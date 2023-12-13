package soargroup.mobilesim.sim.attributes;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Drain extends Attribute {
	/* When there is a command of Use(Fillable, Drain) then the fillable object will be emptied */
	public Drain(RosieSimObject baseObject){
		super(baseObject);
	}

	static {
		// UseObject Validate: Invalid if target is a Drain and object is not fillable
		ActionHandler.addValidateRule(UseObject.class, new ValidateRule<UseObject>(){
			public IsValid validate(UseObject use){
				if(!use.object.is(Fillable.class) && use.target.is(Drain.class)){
					return IsValid.False("Drain: the object " + use.object + " is not Fillable");
				}
				return IsValid.True();
			}
		});

		// UseObject Apply: If the target is a Drain then empty the object
		ActionHandler.addApplyRule(UseObject.class, new ApplyRule<UseObject>(){
			public Result apply(UseObject use){
				if(use.target.is(Drain.class)){
					Fillable fillable = use.object.as(Fillable.class);
					fillable.setContents(RosieConstants.EMPTY);
					use.target.recreateVisObject();
				}
				return Result.Ok();
			}
		});
	}
}
