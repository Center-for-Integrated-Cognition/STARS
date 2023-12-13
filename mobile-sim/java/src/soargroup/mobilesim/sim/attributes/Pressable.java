package soargroup.mobilesim.sim.attributes;

import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Pressable extends Attribute {
	private RosieSimObject target;
	public Pressable(RosieSimObject baseObject, RosieSimObject target){
		super(baseObject);
		this.target = target;
	}

	public void press(){
		if(target != null && target.is(Triggerable.class)){
			target.as(Triggerable.class).trigger(baseObject);
		}
	}

	// Registering Action Handling Rules
	static {
		// Press: Valid if the object is Pressable
		ActionHandler.addValidateRule(Press.class, new ValidateRule<Press>() {
			public IsValid validate(Press press){
				if(press.object.is(Pressable.class)){
					return IsValid.True();
				}
				return IsValid.False("Pressable: Object is not pressable");
			}
		});
		// Press Apply: Trigger the target object
		ActionHandler.addApplyRule(Press.class, new ApplyRule<Press>() {
			public Result apply(Press press){
				Pressable pressable = press.object.as(Pressable.class);
				if(pressable == null){
					return Result.Err("Pressable: Object is not pressable");
				}
				pressable.press();
				return Result.Ok();
			}
		});
	}
}
