package soargroup.mobilesim.sim.actions;

import soargroup.mobilesim.sim.*;

public class PickUp extends Action {
	public final RosieSimObject object;
	public PickUp(RosieSimObject object){
		this.object = object;
	}

	public String toString(){
		return "PickUp(" + object + ")";
	}
}
