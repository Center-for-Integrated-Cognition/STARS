package soargroup.mobilesim.commands.controls;

import java.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class ChangeState extends ControlLaw {
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
			params.add(new TypedParameter("property", TypedValue.TYPE_STRING, true));
			params.add(new TypedParameter("value", TypedValue.TYPE_STRING, true));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

	public final Integer objectId;
	public final String property;
	public final String value;

    public ChangeState(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, ChangeState.getParameters());

		objectId = parameters.get("object-id").getInt();
		property = parameters.get("property").toString();
		value = parameters.get("value").toString();
    }

    public String getName() { return "ChangeState"; }

	public String toString(){
		return String.format("ChangeState(%d, %s=%s)", objectId, property, value);
	}
}
