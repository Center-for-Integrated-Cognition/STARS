package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class Press extends ControlLaw {
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

    public Press(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, Press.getParameters());

		objectId = parameters.get("object-id").getInt();
    }

	@Override
    public String getName() { return "Press"; }

	@Override
	public String toString() { 
		return String.format("Press(%d)", objectId); 
	}
}
