package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

/** Tells the robot to set the held object onto the floor **/
public class PutDown extends ControlLaw {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    public PutDown(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, PutDown.getParameters());
    }

	@Override
    public String getName() { return "PutDown"; }

	@Override
	public String toString() { return "PutDown()"; }
}
