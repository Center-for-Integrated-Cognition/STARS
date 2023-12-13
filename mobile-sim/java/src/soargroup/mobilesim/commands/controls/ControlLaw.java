package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;

import soargroup.mobilesim.lcmtypes.diff_drive_t;

public abstract class ControlLaw {

	protected final Map<String, TypedValue> inputParams;
	protected boolean is_running = false;
    public ControlLaw(Map<String, TypedValue> parameters) {
		inputParams = parameters;
	}

	/** Will assert that each parameter condition is met in the given map of params
	 *  (Checks that each required parameter is present and the types match)
	 *
	 * Will throw an assertion error if a condition is violated
	 **/
	public static void validateParameters(Map<String, TypedValue> params, Collection<TypedParameter> conditions){
		for(TypedParameter cond : conditions){
			// If there is a required parameter, assert that it is present
			if(cond.isRequired()){
				assert params.containsKey(cond.getName()) : "Missing parameter " + cond.getName();
			}
			// Assert that a given parameter has the correct type
			if(params.containsKey(cond.getName())){
				assert params.get(cond.getName()).getType() == cond.getType() : "Invalid type for parameter " + cond.getName();
			}
		}
	}

    /** Get the name of this control law. Mostly useful for debugging purposes.
     *
     *  @return The name of the control law
     **/
    public abstract String getName();

    public abstract String toString();

    /** Start/stop the execution of the control law.
     *
     *  @param run  True causes the control law to begin execution, false stops it
     **/
    public void setRunning(boolean run) {
		is_running = run;
	}

    /** Get a drive command from the CL. */
    public diff_drive_t drive(DriveParams params) {
		return null;
	}
}
