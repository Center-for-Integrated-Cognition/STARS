package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

/** Tells the robot to use one object with another
 *  The specific effect depends on the objects **/
public class UseObject extends ControlLaw {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			params.add(new TypedParameter("object-id", TypedValue.TYPE_INT, true));
			params.add(new TypedParameter("target-id", TypedValue.TYPE_INT, true));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

	public final Integer objectId; // The actor
	public final Integer targetId; // The target

    public UseObject(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, UseObject.getParameters());

		objectId = parameters.get("object-id").getInt();
		targetId = parameters.get("target-id").getInt();
    }

	@Override
    public String getName() { return "UseObject"; }

	@Override
	public String toString() { 
		return String.format("UseObject(%d,%d)", objectId, targetId); 
	}
}
