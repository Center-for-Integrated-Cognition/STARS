package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class PickUp extends ControlLaw {
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
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

	public final Integer objectId;

    public PickUp(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, PickUp.getParameters());

		objectId = parameters.get("object-id").getInt();
    }

	@Override
    public String getName() { return "PickUp"; }

	@Override
	public String toString() { 
		return String.format("PickUp(%d)", objectId); 
	}
}
