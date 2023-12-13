package soargroup.mobilesim.commands;

import java.util.*;
import java.io.IOException;

import april.util.*;

import soargroup.mobilesim.commands.CommandCoordinator.Status;
import soargroup.mobilesim.commands.controls.*;
import soargroup.mobilesim.commands.tests.*;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.control_law_status_t;
import soargroup.mobilesim.lcmtypes.control_law_status_list_t;
import soargroup.mobilesim.lcmtypes.condition_test_t;

public class CommandInterpreter
{
	protected class CommandInfo{
		public int lawID;
		public int commandID;
        public String name;
		public CommandInfo(int lawID_, int commandID_, String name_){
			lawID = lawID_;
			commandID = commandID_;
            name = name_;
		}
	}

    private boolean running;
	protected static LCM lcm = LCM.getSingleton();
	private static int LCM_FPS = 50;
	private static int CMD_FPS = 20;

	CommandCoordinator coordinator = new CommandCoordinator();
	ControlLawFactory clfactory = ControlLawFactory.getSingleton();
	ConditionTestFactory ctfactory = ConditionTestFactory.getSingleton();

	protected Object commandLock = new Object();
	protected CommandInfo activeCommand = null;
	protected int highestCommandID = -1;

	private HashMap<Integer, Status> statuses = new HashMap<Integer, Status>();

    // Is this in a sim robot?
    boolean sim;

	// Handle stale/repeat control law messages
	//protected int lastCommandID = -1;
	//protected Queue<control_law_t> waitingCommands;

	//protected ExpiringMessageCache<control_law_status_list_t> statusCache =
	//	new ExpiringMessageCache<control_law_status_list_t>(.25);

    public CommandInterpreter()
    {
        this(false);
    }

	public CommandInterpreter(boolean sim)
	{
		//waitingCommands = new LinkedList<control_law_t>();
        this.sim = sim;

        running = true;
		new ListenerThread().start();
		//new CommandThread().start();
	}

    public void setRunning(boolean run)
    {
        running = run;
        coordinator.setRunning(run);
    }

	protected void setCommandStatus(Integer commandId, Status status){
		synchronized(statuses){
			statuses.put(commandId, status);
		}
	}

	protected void interpretCommand(control_law_t controlLaw)
	{
		synchronized(commandLock){
			String clName = controlLaw.name.toLowerCase();
			
			// If command == restart, stop the current command and reset the counter
			if(clName.equals("restart")){
				stopActiveCommand(Status.INTERRUPTED);
				highestCommandID = controlLaw.id;
				return;
			}

			// If the id is not greater than the highest we've seen, we can ignore it (already handled)
			if(controlLaw.id <= highestCommandID){
				return;
			}
			highestCommandID = controlLaw.id;

			stopActiveCommand(Status.INTERRUPTED);

			setCommandStatus(controlLaw.id, Status.RECEIVED);
			if(clName.equals("none") || clName.equals("stop")){
				// If the command is none or stop, we don't need to do anything
				setCommandStatus(controlLaw.id, Status.SUCCESS);
			} else {
				executeControlLaw(controlLaw);
			}
		}
	}

	// Given a controlLaw, collects the parameters from the lcm type into a map
	protected Map<String, TypedValue> collectParams(control_law_t controlLaw){
		Map<String, TypedValue> params = new HashMap<String, TypedValue>();
		for(int i = 0; i < controlLaw.num_params; i++) {
			String name = controlLaw.param_names[i];
			TypedValue value = new TypedValue(controlLaw.param_values[i]);
			params.put(name, value);
		}
		return params;
	}

	// Given a conditionTest, collects the parameters from the lcm type into a map
	protected Map<String, TypedValue> collectParams(condition_test_t conditionTest){
		Map<String, TypedValue> params = new HashMap<String, TypedValue>();
		for(int i = 0; i < conditionTest.num_params; i++) {
			String name = conditionTest.param_names[i];
			TypedValue value = new TypedValue(conditionTest.param_values[i]);
			params.put(name, value);
		}
		return params;
	}

	protected void executeControlLaw(control_law_t controlLaw){
		synchronized(commandLock){
			// Get control law parameters
			Map<String, TypedValue> params = collectParams(controlLaw);
            if (sim) {
                params.put("sim", new TypedValue(1));
            }

			// Get termination condition parameters
			Map<String, TypedValue> termParams = collectParams(controlLaw.termination_condition);

			// Attempt to create and register control law and test condition
			CommandInfo commandInfo;
			try {
				ControlLaw law = clfactory.construct(controlLaw.name, params);
				ConditionTest test = ctfactory.construct(controlLaw.termination_condition.name, termParams);
				if (law != null && test != null) {
					int lawID = coordinator.executeControlLaw(law, test);
					activeCommand = new CommandInfo(lawID, controlLaw.id, law.getName());
				} else {
					System.err.println("WRN: Error constructing law/test");
					setCommandStatus(controlLaw.id, Status.ERROR);
				}
			} catch (ClassNotFoundException ex) {
				System.err.println("ERR: "+ex);
				ex.printStackTrace();
				setCommandStatus(controlLaw.id, Status.ERROR);
			}
		}
	}

	protected void stopActiveCommand(Status finalStatus){
		synchronized(commandLock){
			if (activeCommand == null){
				return;
			}
			setCommandStatus(activeCommand.commandID, finalStatus);
			coordinator.destroyControlLaw(activeCommand.lawID);
			activeCommand = null;
		}
	}

	protected void sendCommandStatus(control_law_t controlLaw)
	{
		String clName = controlLaw.name.toLowerCase();
		control_law_status_t status = new control_law_status_t();
		status.utime = TimeUtil.utime();
		status.id = controlLaw.id;
		synchronized(statuses){
			status.status = statuses.getOrDefault(controlLaw.id, Status.UNKNOWN).toString();
		}
		status.name = clName;

		lcm.publish("STATUS__SOAR_COMMAND_TX", status);
	}

	protected void updateControlLawStatus(control_law_status_list_t status_list){
		synchronized(commandLock){
			for(control_law_status_t cl_status : status_list.statuses){
				if (activeCommand != null && cl_status.id == activeCommand.lawID){
					Status status = Status.valueOf(cl_status.status);
					if(status == Status.EXECUTING){
						setCommandStatus(activeCommand.commandID, status);
					} else {
						stopActiveCommand(status);
					}
				}
			}
		}
	}

	class ListenerThread extends Thread implements LCMSubscriber
	{
		LCM lcm = LCM.getSingleton();

		public ListenerThread()
		{
			lcm.subscribe("SOAR_COMMAND.*", this);
			lcm.subscribe("CONTROL_LAW_STATUS.*", this);
		}

		public void run()
		{
			while(true) {
				TimeUtil.sleep(1000/LCM_FPS);
			}
		}

		public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
		{
			//System.out.println("CommandInterpreter::messageReceived(" + channel + ") [" + TimeUtil.utime() + "]");
            if (!running){
                return;
			}

			try {
				messageReceivedEx(lcm, channel, ins);
			} catch (IOException ioex) {
				System.out.println("ERR: LCM channel " + channel);
				ioex.printStackTrace();
			}
		}

		public void messageReceivedEx(LCM lcm, String channel, LCMDataInputStream ins)
			throws IOException
		{
			if (channel.startsWith("SOAR_COMMAND")) {
				control_law_t controlLaw = new control_law_t(ins);
				interpretCommand(controlLaw);
				sendCommandStatus(controlLaw);
			} else if (channel.startsWith("CONTROL_LAW_STATUS")) {
				control_law_status_list_t sl = new control_law_status_list_t(ins);
				updateControlLawStatus(sl);
			}
		}
	}

	public static void main(String[] args)
	{
		CommandInterpreter ci = new CommandInterpreter();
	}
}
