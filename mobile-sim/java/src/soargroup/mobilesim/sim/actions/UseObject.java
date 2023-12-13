package soargroup.mobilesim.sim.actions;

import soargroup.mobilesim.sim.*;

public class UseObject extends Action {
	public final RosieSimObject object;
	public final RosieSimObject target;
	public UseObject(RosieSimObject object, RosieSimObject target){
		this.object = object;
		this.target = target;
	}

	public String toString(){
		return "Use(" + object + ", " + target + ")";
	}
}
