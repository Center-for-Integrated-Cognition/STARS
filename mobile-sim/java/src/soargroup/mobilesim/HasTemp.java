package soargroup.mobilesim.sim.attributes;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.*;

public class HasTemp extends Attribute {

	private Double temperature = 70.0;
	public HasTemp(RosieSimObject baseObject){
		super(baseObject);
	}

	// Doesn't directly set temperature, instead will gradually move towards the given value
	public void changeTemperature(double targetTemp){
		temperature += (targetTemp - temperature) * 0.02; 
		baseObject.setProperty(RosieConstants.TEMPERATURE, temperature.toString());
	}

}
