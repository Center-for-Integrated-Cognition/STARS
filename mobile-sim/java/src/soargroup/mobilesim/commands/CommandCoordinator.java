package soargroup.mobilesim.commands;

import java.util.*;

import april.util.*;

import soargroup.mobilesim.commands.controls.*;
import soargroup.mobilesim.commands.tests.*;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.control_law_status_t;
import soargroup.mobilesim.lcmtypes.control_law_status_list_t;

/** The command coordinator is responsible for the creation of new control
 *  law requests, condition tests, registration of tests as termination
 *  conditions for control law(s), an in general, dealing with requests to the
 *  robotic system to perform some action.
 *
 *  For example, a request to "Turn right after the green painting" could
 *  result in the following events:
 *  -Spawn a control law that keeps the robot driving down a corridor
 *  -Turn on a classifier for identifying green paintings
 *  -Spawn a condition test that indicates when we are "after" a green painting
 *   (which may involve spatial reasoning on Soar or some other classifier,
 *    even!)
 *  -Spawn a turn detecting condition test (specifically for right, or possibly
 *   with more back and forth with Soar, etc.)
 *
 *  Condition Tests could feasibly be compositions of other Condition Tests. For
 *  example, maybe we only care if one of two tests is true. Maybe we need
 *  several conditions to be true simultaneously! In this case, the highest
 *  level condition test is all that's registered with the Coordinator, while
 *  it internally manages its own condition tests.
 **/
public class CommandCoordinator
{
    LCM lcm = LCM.getSingleton();

    public enum Status {
		RECEIVED, ERROR, EXECUTING, SUCCESS, FAILURE, TIMEOUT, INTERRUPTED, UNKNOWN
    }

	// Combines a ConditionTest with the status to report if that test passes
	private static int nextTermCondId = 1;
	private class TerminationCondition {

		public final int id;
		public final ConditionTest test;
		public final Status result;

		public TerminationCondition(ConditionTest test, Status result){
			this.id = nextTermCondId++;
			this.test = test;
			this.result = result;
		}
	}

	private static int nextControlLawId = 1;
    private class ControlLawRecord {

		public final int id;
        public final ControlLaw controlLaw;
		public final Set<TerminationCondition> conditions;
        public Status status;

        public ControlLawRecord(ControlLaw controlLaw) {
			this.id = nextControlLawId++;
            this.controlLaw = controlLaw;
			this.conditions = new HashSet<TerminationCondition>();
            this.status = Status.EXECUTING;
        }

		public void addTerminationCondition(ConditionTest test, Status result){
			this.conditions.add(new TerminationCondition(test, result));
		}

		public void checkTerminationConditions(){
			for(TerminationCondition cond : conditions){
				if(status == Status.EXECUTING && cond.test.conditionMet()){
					status = cond.result;
					setRunning(false);
				}
			}
		}

		private boolean running = false;
		public void setRunning(boolean running){
			if(this.running == running){
				return;
			}
			this.running = running;
			this.controlLaw.setRunning(running);
			for(TerminationCondition cond : conditions){
				cond.test.setRunning(running);
			}
		}
    }

    Map<Integer, ControlLawRecord> controlLaws = new HashMap<Integer, ControlLawRecord>();

    PeriodicTasks tasks = new PeriodicTasks(1);
    private class UpdateTask implements PeriodicTasks.Task
    {
        /** Query control laws for statuses, manage termination conditions, etc. */
        public void run(double dt) {
			checkTerminationConditions();
			publishControlLawStatusList();
        }
    }

    public CommandCoordinator()
    {
        int hz = 40;
        tasks.addFixedRate(new UpdateTask(), 1.0/hz);
        tasks.setRunning(true);
    }

    public synchronized void setRunning(boolean run)
    {
        tasks.setRunning(run);  // Is this coordinator active?
		for(ControlLawRecord clRecord : controlLaws.values()){
			clRecord.setRunning(run);
		}
    }

    /** Register a control law with the Coordinator and start executing it
     *
     *  @param controlLaw       Control law to execute
     *
     *  @return ID assigned to law
     **/
    public synchronized int executeControlLaw(ControlLaw controlLaw, ConditionTest successTest)
    {
		ControlLawRecord clRecord = new ControlLawRecord(controlLaw);
		System.out.printf("Created new control law %s <%d>\n", controlLaw.toString(), clRecord.id);

		// Report success if the termination condition passed
		clRecord.addTerminationCondition(successTest, Status.SUCCESS);

		// Automatically insert a failure condition for getting too close
		// to an obstacle...
		//ConditionTest obstacleTest = new ObstacleTest(new HashMap<String, TypedValue>());
		//clRecord.addTerminationCondition(obstacleTest, Status.FAILURE);
		
		// Report failure if the command timed out after 30 seconds (the robot didn't move)
		HashMap<String, TypedValue> params = new HashMap<String, TypedValue>();
		params.put("timeout", new TypedValue(30.0));
		ConditionTest timeoutTest = new Stabilized(params);
		clRecord.addTerminationCondition(timeoutTest, Status.FAILURE);

		for(TerminationCondition cond : clRecord.conditions){
			// Start each test running
			System.out.printf("Attached TerminationCondition %s <%d> -> %s to Law <%d>\n", 
					cond.test.toString(), cond.id, cond.result.toString(), clRecord.id);
		}

		controlLaws.put(clRecord.id, clRecord);
		clRecord.setRunning(true);
		System.out.printf("Started running control law %s <%d>\n", controlLaw.toString(), clRecord.id);

		return clRecord.id;
    }

    /** Destroy the control law associated with the given ID.
     *
     *  @param id   ID of control law to destroy
     *
     *  @return     True is matching control law was destroyed, else false
     **/
    public synchronized boolean destroyControlLaw(Integer id) {
		System.out.println("Destroying control law " + id);
		ControlLawRecord clRecord = controlLaws.remove(id);
		if (clRecord != null) {
			clRecord.setRunning(false);
		}
		return (clRecord != null);
    }

	// Checks every termination condition and if it has a test with conditions met, 
	//    it will update the corresponding control law's status and stop it running
	protected synchronized void checkTerminationConditions(){
		for(ControlLawRecord clRecord : controlLaws.values()){
			clRecord.checkTerminationConditions();
		}
	}

	// Sends out a message with the status of every control law
	protected synchronized void publishControlLawStatusList(){
		control_law_status_list_t status_list = new control_law_status_list_t();
		status_list.utime = TimeUtil.utime();
		status_list.nstatuses = controlLaws.size();
		status_list.statuses = new control_law_status_t[status_list.nstatuses];
		int idx = 0;
		for (ControlLawRecord clRecord : controlLaws.values()){
			control_law_status_t clStatus = new control_law_status_t();
			clStatus.id = clRecord.id;
			clStatus.name = clRecord.controlLaw.getName();
			clStatus.status = clRecord.status.name();
			status_list.statuses[idx++] = clStatus;
		}
		lcm.publish("CONTROL_LAW_STATUS", status_list);
	}
}

