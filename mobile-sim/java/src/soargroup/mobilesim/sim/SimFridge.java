package soargroup.mobilesim.sim;

import java.util.List;
import java.util.ArrayList;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.rosie.RosieConstants;

import soargroup.mobilesim.sim.attributes.*;

public class SimFridge extends SimShelves {
	public static final double TEMPERATURE = 35.0;

	public SimFridge(SimWorld sw){
		super(sw);
	}

	// Children can override to implement any dynamics, this is called multiple times/second
	@Override
	public void update(double dt, ArrayList<SimObject> worldObjects){
		List<RosieSimObject> objects = receptacle.getHeldObjects();
		for(RosieSimObject obj : objects){
			if(obj.is(HasTemp.class)){
				obj.as(HasTemp.class).changeTemperature(TEMPERATURE);
			}
		}
	}
}
