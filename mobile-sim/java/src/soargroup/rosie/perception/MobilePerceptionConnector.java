package soargroup.rosie.perception;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Properties;

import java.nio.ByteBuffer;

import edu.umich.rosie.soar.AgentConnector;
import edu.umich.rosie.soar.SoarClient;
import edu.umich.rosie.soar.SoarUtil;
import edu.umich.rosie.soarobjects.Pose;
import april.jmat.LinAlg;
import april.lcmtypes.pose_t;
import april.util.*;

import sml.Identifier;
import soargroup.rosie.actuation.MobileActuationConnector;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.object_data_list_t;
import soargroup.mobilesim.lcmtypes.robot_info_t;

public class MobilePerceptionConnector extends AgentConnector implements LCMSubscriber{
	private Integer nextID = 1;

    private LCM lcm;

    private Identifier objectsId = null;

	private WorldObjectManager objectManager;

    private Robot robot;

    public MobilePerceptionConnector(SoarClient client, Properties props){
    	super(client);

		objectManager = new WorldObjectManager();

    	robot = new Robot(props);

    	// Setup LCM events
        lcm = LCM.getSingleton();
    }

    @Override
    public void connect(){
    	super.connect();
        lcm.subscribe("ROBOT_INFO", this);
        lcm.subscribe("DETECTED_OBJECTS", this);
    }

    @Override
    public void disconnect(){
    	super.disconnect();
        lcm.unsubscribe("ROBOT_INFO", this);
        lcm.unsubscribe("DETECTED_OBJECTS", this);
    }

	/***************************
	 *
	 * LCM HANDLING
	 *
	 **************************/


    @Override
    public synchronized void messageReceived(LCM lcm, String channel, LCMDataInputStream ins){
		try {
			if (channel.startsWith("ROBOT_INFO")){
				robot_info_t robotInfo = new robot_info_t(ins);
				robot.update(robotInfo);
				WorldObject obj = objectManager.getObject(robotInfo.held_object);
				robot.setHeldObject(obj == null ? null : obj.getHandle());
			} else if(channel.startsWith("DETECTED_OBJECTS")){
				object_data_list_t newObjs = new object_data_list_t(ins);
				objectManager.update(newObjs);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	/***************************
	 *
	 * INPUT PHASE HANDLING
	 *
	 **************************/

    protected synchronized void onInputPhase(Identifier inputLink){
    	// Update the information about the robot
    	updateRobot();

    	// Update information about objects
    	updateObjects();
    }

    private void updateRobot(){
    	robot.updateMovingState(soarClient.getConnector(MobileActuationConnector.class).getMovingState());
    	if(!robot.isAdded()){
    		robot.addToWM(soarClient.getAgent().GetInputLink());
    	} else {
    		robot.updateWM();
    	}
    }

    private void updateObjects(){
    	StringBuilder svsCommands = new StringBuilder();
		if(objectManager.isAdded()){
			objectManager.updateWM(svsCommands);
		} else {
			objectManager.addToWM(soarClient.getAgent().GetInputLink(), svsCommands);
		}

    	if(svsCommands.length() > 0){
    		soarClient.getAgent().SendSVSInput(svsCommands.toString());
    	}
    }

	@Override
	protected void onInitSoar() {
    	StringBuilder svsCommands = new StringBuilder();
		objectManager.removeFromWM(svsCommands);
    	if(svsCommands.length() > 0){
    		soarClient.getAgent().SendSVSInput(svsCommands.toString());
    	}

		robot.removeFromWM();
	}

    // Currently no commands relevant to perception
	protected void onOutputEvent(String attName, Identifier id) { }
}
