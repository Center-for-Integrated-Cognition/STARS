package soargroup.mobilesim.sim.attributes;

import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Grabbable extends Attribute {
	private boolean _isGrabbed = false;

	public Grabbable(RosieSimObject baseObject){
		super(baseObject);
	}

	public boolean isGrabbed(){
		return _isGrabbed;
	}

	public void setGrabbed(boolean isGrabbed){
		this._isGrabbed = isGrabbed;
	}
	
	// Registering Action Handling Rules
	static {
		// PickUp: Valid if the object is Grabbable
		ActionHandler.addValidateRule(PickUp.class, new ValidateRule<PickUp>() {
			public IsValid validate(PickUp pickup){
				if(pickup.object.is(Grabbable.class)){
					return IsValid.True();
				}
				return IsValid.False("Grabbable: Object is not grabbable");
			}
		});
		// PickUp Apply: Update isGrabbed flag on object
		ActionHandler.addApplyRule(PickUp.class, new ApplyRule<PickUp>() {
			public Result apply(PickUp pickup){
				Grabbable grabbable = pickup.object.as(Grabbable.class);
				if(grabbable == null){
					return Result.Err("Grabbable: Object is not grabbable");
				}
				grabbable.setGrabbed(true);
				return Result.Ok();
			}
		});

		// PutDown Apply: Update isGrabbed flag on object
		ActionHandler.addApplyRule(PutDown.class, new ApplyRule<PutDown>() {
			public Result apply(PutDown putdown){
				Grabbable grabbable = putdown.object.as(Grabbable.class);
				if(grabbable != null){
					grabbable.setGrabbed(false);
				}
				return Result.Ok();
			}
		});
	}
}
