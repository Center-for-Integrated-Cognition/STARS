package soargroup.mobilesim.sim.attributes;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Lockable extends Attribute {
	private boolean _isLocked = false;

	public Lockable(RosieSimObject baseObject, boolean isLocked){
		super(baseObject);
		this.setLocked(isLocked);
	}

	public boolean isLocked(){
		return _isLocked;
	}

	public void setLocked(boolean isLocked){
		this._isLocked = isLocked;
		baseObject.setProperty(RosieConstants.LOCK, 
				isLocked ? RosieConstants.LOCKED : RosieConstants.UNLOCKED);
	}
	
	// Registering Action Handling Rules
	static {
		// SetProp.Lock: Valid if the object is Lockable and unlocked
		ActionHandler.addValidateRule(SetProp.Lock.class, new ValidateRule<SetProp.Lock>() {
			public IsValid validate(SetProp.Lock lock){
				Lockable lockable = lock.object.as(Lockable.class);
				if(lockable == null){
					return IsValid.False("Lockable: Object is not lockable");
				}
				if(lockable.isLocked()){
					return IsValid.False("Lockable: Object is already locked");
				}
				return IsValid.True();
			}
		});
		// SetProp.Lock Apply: Update isLocked flag on object
		ActionHandler.addApplyRule(SetProp.Lock.class, new ApplyRule<SetProp.Lock>() {
			public Result apply(SetProp.Lock lock){
				Lockable lockable = lock.object.as(Lockable.class);
				if(lockable == null){
					return Result.Err("Lockable: Object is not lockable");
				}
				lockable.setLocked(true);
				lock.object.recreateVisObject();
				return Result.Ok();
			}
		});

		// SetProp.Unlock: Valid if the object is Lockable and locked
		ActionHandler.addValidateRule(SetProp.Unlock.class, new ValidateRule<SetProp.Unlock>() {
			public IsValid validate(SetProp.Unlock unlock){
				Lockable lockable = unlock.object.as(Lockable.class);
				if(lockable == null){
					return IsValid.False("Lockable: Object is not lockable");
				}
				if(!lockable.isLocked()){
					return IsValid.False("Lockable: Object is not locked");
				}
				return IsValid.True();
			}
		});
		// SetProp.Unlock Apply: Update isLocked flag on object
		ActionHandler.addApplyRule(SetProp.Unlock.class, new ApplyRule<SetProp.Unlock>() {
			public Result apply(SetProp.Unlock unlock){
				Lockable lockable = unlock.object.as(Lockable.class);
				if(lockable == null){
					return Result.Err("Lockable: Object is not lockable");
				}
				lockable.setLocked(false);
				unlock.object.recreateVisObject();
				return Result.Ok();
			}
		});
	}
}
