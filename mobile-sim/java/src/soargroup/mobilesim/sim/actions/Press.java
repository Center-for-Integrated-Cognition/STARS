package soargroup.mobilesim.sim.actions;

import soargroup.mobilesim.sim.*;

public class Press extends Action {
	public final RosieSimObject object;
	public Press(RosieSimObject object){
		this.object = object;
	}

	public String toString(){
		return "Press(" + object + ")";
	}
}
