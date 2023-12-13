package soargroup.rosie.actuation;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;

import edu.umich.rosie.soar.AgentConnector;
import edu.umich.rosie.soar.SoarClient;
import edu.umich.rosie.soar.SoarUtil;
import april.jmat.LinAlg;
import april.util.TimeUtil;

import sml.Identifier;
import soargroup.mobilesim.commands.CommandCoordinator.Status;
import soargroup.mobilesim.commands.TypedValue;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.control_law_status_t;

public class MobileActuationConnector extends AgentConnector implements LCMSubscriber{
	private static int CL_FPS = 10;

	private Object commandLock = new Object();
	private control_law_t activeCommand = null;
	private Identifier activeCommandId = null;
	private String commandStatus = "none";
	private int nextControlLawId = 1;

	private String movingState = "stopped";

    private LCM lcm;
    
    private boolean killThread = false;
    private ControlLawThread sendCommandThread = null;
    
    public MobileActuationConnector(SoarClient client, Properties props){
    	super(client);

        lcm = LCM.getSingleton();
        
        // Setup Output Link Events
        String[] outputHandlerStrings = { "do-control-law", "stop" };
        this.setOutputHandlerNames(outputHandlerStrings);

        activeCommand = SoarCommandParser.createEmptyControlLaw("RESTART");
        activeCommand.id = nextControlLawId++;
    }
    
    @Override
    public void connect(){
    	super.connect();
        lcm.subscribe("STATUS__SOAR_COMMAND.*", this);
        killThread = false;
        sendCommandThread = new ControlLawThread();
        sendCommandThread.start();
    }
    
    @Override
    public void disconnect(){
    	super.disconnect();
        lcm.unsubscribe("STATUS__SOAR_COMMAND.*", this);
        if(sendCommandThread != null){
        	killThread = true;
        	try {
				sendCommandThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	sendCommandThread = null;
        }
    }
    
    public String getMovingState(){
    	return movingState;
    }

    @Override
    public synchronized void messageReceived(LCM lcm, String channel, LCMDataInputStream ins){
		try {
			if(channel.startsWith("STATUS__SOAR_COMMAND")){
				control_law_status_t status = new control_law_status_t(ins);
				synchronized(commandLock){
					if(activeCommand != null && activeCommand.id == status.id){
						commandStatus = Status.valueOf(status.status).toString().toLowerCase();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    
    /*****************************************************
     * 
     * ControlLawThread
     *   Thread for periodically sending the activeCommand
     *   over LCM to the robot
     *
     *****************************************************/
	class ControlLawThread extends Thread{
		public ControlLawThread(){}

		public void run(){
			while(!killThread) {
				synchronized(commandLock){
					if(activeCommand != null){
						activeCommand.utime = TimeUtil.utime();
						lcm.publish("SOAR_COMMAND_TX", activeCommand);
					}
				}

				TimeUtil.sleep(1000/CL_FPS);
			}
		}
	}

    /*******************************************************
     * Methods for updating the input link
     ******************************************************/
    
	protected void onInputPhase(Identifier inputLink) {
		synchronized(commandLock){
			if(activeCommandId != null){
				SoarUtil.updateStringWME(activeCommandId, "status", commandStatus);
				if(commandStatus.equals("success") || commandStatus.equals("failure") || commandStatus.equals("unknown")){
					activeCommandId = null;
					activeCommand = null;
					movingState = "stopped";
				}
			}
		}
	}
	
	protected void onInitSoar() {
		synchronized(commandLock){
			activeCommand = null;
			activeCommandId = null;
			commandStatus =  "none";
		}
	}
	
	/*************************************************************
	 * 
	 * Handling Commands on the Soar Output-Link
	 ************************************************************/

	protected void onOutputEvent(String attName, Identifier id) {
		if(attName.equals("do-control-law")){
			processDoControlLawCommand(id);
		} else if(attName.equals("stop")){
			processStopCommand(id);
		}
	}

    public void processDoControlLawCommand(Identifier id){
    	control_law_t controlLaw = SoarCommandParser.parseControlLaw(id);
    	if(controlLaw == null){
    		id.CreateStringWME("status", "error");
    		id.CreateStringWME("error-type", "syntax-error");
    		return;
    	}
    	
    	controlLaw.id = nextControlLawId++;
    	id.CreateStringWME("status", "sent");
    	synchronized(commandLock){
    		if (activeCommandId != null){
    			SoarUtil.updateStringWME(activeCommandId, "status", "interrupted");
    		}
    		activeCommand = controlLaw;
    		activeCommandId = id;
    		
			commandStatus = "sent";
    		movingState = "moving";
    	}
    }

    public void processStopCommand(Identifier id){
		synchronized(commandLock){
			if(activeCommandId != null){
				SoarUtil.updateStringWME(activeCommandId, "status", "interrupted");
			}
			activeCommand = SoarCommandParser.createEmptyControlLaw("STOP");
			activeCommand.id = nextControlLawId++;
			activeCommandId = id;
			
			commandStatus = "sent";
			movingState = "stopped";
		}
    }
}
