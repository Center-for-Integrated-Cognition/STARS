package soargroup.mobilesim.sim.attributes;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Activatable extends Attribute {
	private boolean _isOn = false;

	public Activatable(RosieSimObject baseObject, boolean isOn){
		super(baseObject);
		this.setOn(isOn);
	}

	public boolean isOn(){
		return _isOn;
	}

	public void setOn(boolean isOn){
		this._isOn = isOn;
		baseObject.setProperty(RosieConstants.ACTIVATION, 
				isOn ? RosieConstants.ACT_ON : RosieConstants.ACT_OFF);
	}
	
	// Registering Action Handling Rules
	static {
		// SetProp.TurnOn: Valid if the object is Activatable and off
		ActionHandler.addValidateRule(SetProp.TurnOn.class, new ValidateRule<SetProp.TurnOn>() {
			public IsValid validate(SetProp.TurnOn turnon){
				Activatable activatable = turnon.object.as(Activatable.class);
				if(activatable == null){
					return IsValid.False("Activatable: Object is not activatable");
				}
				if(activatable.isOn()){
					return IsValid.False("Activatable: Object is already on");
				}
				return IsValid.True();
			}
		});
		// SetProp.TurnOn Apply: Update isOn flag on object
		ActionHandler.addApplyRule(SetProp.TurnOn.class, new ApplyRule<SetProp.TurnOn>() {
			public Result apply(SetProp.TurnOn turnon){
				Activatable activatable = turnon.object.as(Activatable.class);
				if(activatable == null){
					return Result.Err("Activatable: Object is not activatable");
				}
				activatable.setOn(true);
				turnon.object.recreateVisObject();
				return Result.Ok();
			}
		});

		// SetProp.TurnOff: Valid if the object is Activatable and on
		ActionHandler.addValidateRule(SetProp.TurnOff.class, new ValidateRule<SetProp.TurnOff>() {
			public IsValid validate(SetProp.TurnOff turnoff){
				Activatable activatable = turnoff.object.as(Activatable.class);
				if(activatable == null){
					return IsValid.False("Activatable: Object is not activatable");
				}
				if(!activatable.isOn()){
					return IsValid.False("Activatable: Object is not on");
				}
				return IsValid.True();
			}
		});
		// SetProp.TurnOff Apply: Update isOn flag on object
		ActionHandler.addApplyRule(SetProp.TurnOff.class, new ApplyRule<SetProp.TurnOff>() {
			public Result apply(SetProp.TurnOff turnoff){
				Activatable activatable = turnoff.object.as(Activatable.class);
				if(activatable == null){
					return Result.Err("Activatable: Object is not activatable");
				}
				activatable.setOn(false);
				turnoff.object.recreateVisObject();
				return Result.Ok();
			}
		});
	}
}
