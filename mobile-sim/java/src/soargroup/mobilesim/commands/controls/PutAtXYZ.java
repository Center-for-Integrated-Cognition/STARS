package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class PutAtXYZ extends ControlLaw {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			params.add(new TypedParameter("x", TypedValue.TYPE_DOUBLE, true));
			params.add(new TypedParameter("y", TypedValue.TYPE_DOUBLE, true));
			params.add(new TypedParameter("z", TypedValue.TYPE_DOUBLE, true));
			params.add(new TypedParameter("teleport", TypedValue.TYPE_BOOLEAN, false));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

	// The coordinate to put the object at
	public final double x;
	public final double y;
	public final double z;

	// If true, don't worry about the object in relation to the robot
	public final boolean teleport;

    public PutAtXYZ(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, PutAtXYZ.getParameters());

		x = parameters.get("x").getDouble();
		y = parameters.get("y").getDouble();
		z = parameters.get("z").getDouble();
		teleport = parameters.getOrDefault("teleport", new TypedValue(false)).getBoolean();
    }

	@Override
    public String getName() { return "PutAtXYZ"; }

	@Override
	public String toString() { 
		return String.format("PutAtXYZ(%.2f, %.2f, %.2f)", x, y, z); 
	}
}
