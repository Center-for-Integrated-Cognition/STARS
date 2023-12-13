
package soargroup.mobilesim.sim.attributes;

import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class Triggerable extends Attribute {
	public interface Callback {
		void trigger(RosieSimObject source);
	}

	private Callback callback;
	public Triggerable(RosieSimObject baseObject, Callback callback){
		super(baseObject);
		this.callback = callback;
	}

	public void trigger(RosieSimObject source){
		callback.trigger(source);
	}
}
