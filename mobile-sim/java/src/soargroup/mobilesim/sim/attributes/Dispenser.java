package soargroup.mobilesim.sim.attributes;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Dispenser extends Attribute {
	/* When there is a command of Use(Dispenser, Fillable) then the fillable object will be filled with the contents*/
	public Dispenser(RosieSimObject baseObject){
		super(baseObject);
	}

	public void fill(Fillable fillable){
		String contents = baseObject.getProperty(RosieConstants.CONTENTS);
		fillable.setContents(contents);
	}

	static {
		ActionHandler.addValidateRule(UseObject.class, new ValidateRule<UseObject>(){
			public IsValid validate(UseObject use){
				// If the object is a dispenser then it must have a valid contents
				if(use.object.getProperty(RosieConstants.CONTENTS) == null){
					return IsValid.False("Dispenser: The dispenser " + use.object + " has no contents");
				}
				// If the object is a dispenser then the target must be fillable
				if(use.object.is(Dispenser.class) && !use.target.is(Fillable.class)){
					return IsValid.False("Dispenser: The target object " + use.target + " is not fillable");
				}
				return IsValid.True();
			}
		});

		// UseObject Apply: If the actor object is a dispenser then fill the target
		ActionHandler.addApplyRule(UseObject.class, new ApplyRule<UseObject>(){
			public Result apply(UseObject use){
				if(use.object.is(Dispenser.class)){
					Fillable fillable = use.target.as(Fillable.class);
					fillable.setContents(use.object.getProperty(RosieConstants.CONTENTS));
				}
				return Result.Ok();
			}
		});
	}
}
