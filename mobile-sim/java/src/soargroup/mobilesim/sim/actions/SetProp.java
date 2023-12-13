package soargroup.mobilesim.sim.actions;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;

public class SetProp extends Action {
	public final RosieSimObject object;
	public final String property;
	public final String value;

	public SetProp(RosieSimObject object, String property, String value){
		this.object = object;
		this.property = property;
		this.value = value;
	}

	// Constructs the SetProp action, using the appropriate subclass when needed
	//   (e.g. if property==activation1 and value=on1 will construct TurnOn)
	public static SetProp construct(RosieSimObject object, String property, String value){
		if(property.equals(RosieConstants.ACTIVATION) && value.equals(RosieConstants.ACT_ON)){
			return new TurnOn(object);
		} 
		else if(property.equals(RosieConstants.ACTIVATION) && value.equals(RosieConstants.ACT_OFF)){
			return new TurnOff(object);
		} 
		else if(property.equals(RosieConstants.DOOR) && value.equals(RosieConstants.DOOR_OPEN)){
			return new Open(object);
		} 
		else if(property.equals(RosieConstants.DOOR) && value.equals(RosieConstants.DOOR_CLOSED)){
			return new Close(object);
		} 
		else if(property.equals(RosieConstants.LOCK) && value.equals(RosieConstants.LOCKED)){
			return new Lock(object);
		} 
		else if(property.equals(RosieConstants.LOCK) && value.equals(RosieConstants.UNLOCKED)){
			return new Unlock(object);
		} 
		return new SetProp(object, property, value);
	}

	public String toString(){
		return "SetProp(" + object + ", " + property + "=" + value + ")";
	}

	// SetProp.TurnOn -> Turns on the object (e.g. a lightswitch)
	public static class TurnOn extends SetProp {
		public TurnOn(RosieSimObject object){
			super(object, RosieConstants.ACTIVATION, RosieConstants.ACT_ON);
		}

		public String toString(){
			return "SetProp.TurnOn(" + object + ")";
		}
	}

	// SetProp.TurnOff -> Turns off the object (e.g. a lightswitch)
	public static class TurnOff extends SetProp {
		public TurnOff(RosieSimObject object){
			super(object, RosieConstants.ACTIVATION, RosieConstants.ACT_OFF);
		}

		public String toString(){
			return "SetProp.TurnOff(" + object + ")";
		}
	}

	// SetProp.Open -> Opens an object with a door
	public static class Open extends SetProp {
		public Open(RosieSimObject object){
			super(object, RosieConstants.DOOR, RosieConstants.DOOR_OPEN);
		}

		public String toString(){
			return "SetProp.Open(" + object + ")";
		}
	}

	// SetProp.Close -> Closes an object with a door
	public static class Close extends SetProp {
		public Close(RosieSimObject object){
			super(object, RosieConstants.DOOR, RosieConstants.DOOR_CLOSED);
		}

		public String toString(){
			return "SetProp.Close(" + object + ")";
		}
	}

	// SetProp.Lock -> Locks an object with a lock
	public static class Lock extends SetProp {
		public Lock(RosieSimObject object){
			super(object, RosieConstants.LOCK, RosieConstants.LOCKED);
		}

		public String toString(){
			return "SetProp.Lock(" + object + ")";
		}
	}

	// SetProp.Unlock -> Unlocks an object with a lock
	public static class Unlock extends SetProp {
		public Unlock(RosieSimObject object){
			super(object, RosieConstants.LOCK, RosieConstants.UNLOCKED);
		}

		public String toString(){
			return "SetProp.Unlock(" + object + ")";
		}
	}


}
