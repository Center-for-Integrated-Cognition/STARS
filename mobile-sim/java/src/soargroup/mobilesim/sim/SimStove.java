package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.rosie.RosieConstants;

import soargroup.mobilesim.sim.attributes.*;

public class SimStove extends RosieSimObject {
	public static final double TEMPERATURE = 200.0;
	public final static double BURNER_H = 0.05;

	protected Activatable activatable;
	protected Surface surface;

	public SimStove(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects){
		boolean isOn = properties.get(RosieConstants.ACTIVATION).equals(RosieConstants.ACT_ON);
		activatable = new Activatable(this, isOn);
		addAttribute(activatable);

		surface = new Surface(this, false);
		double burner_dx = scale_xyz[0]*0.25;
		double burner_dy = scale_xyz[1]*0.25;
		double burner_z  = (scale_xyz[2] - BURNER_H)*0.5;
		surface.addPoint( burner_dx,  burner_dy, burner_z);
		surface.addPoint( burner_dx, -burner_dy, burner_z);
		surface.addPoint(-burner_dx,  burner_dy, burner_z);
		surface.addPoint(-burner_dx, -burner_dy, burner_z);
		addAttribute(surface);

		super.init(worldObjects);
	}

	// Children can override to implement any dynamics, this is called multiple times/second
	@Override
	public void update(double dt, ArrayList<SimObject> worldObjects){
		List<RosieSimObject> objects = surface.getHeldObjects();
		for(RosieSimObject obj : objects){
			if(obj.is(HasTemp.class)){
				obj.as(HasTemp.class).changeTemperature(TEMPERATURE);
			}
		}
	}

	@Override
	public VisChain createVisObject(){
		VisChain vc = new VisChain();
		VzMesh.Style style = new VzMesh.Style(color);
		VzMesh.Style burnerStyle = new VzMesh.Style(activatable.isOn() ? Color.red : Color.black);
	
		// Stove Base
		ObjectModels.addBox(vc, 0.0, 0.0, -BURNER_H*0.5, scale_xyz[0], scale_xyz[1], scale_xyz[2]-BURNER_H, style);

		// Burners
		double burner_dx = scale_xyz[0]*0.25;
		double burner_dy = scale_xyz[1]*0.25;
		double burner_z  = (scale_xyz[2] - BURNER_H)*0.5;
		double burner_rad = Math.min(scale_xyz[0], scale_xyz[1])*0.2;
		ObjectModels.addCyl(vc,  burner_dx,  burner_dy, burner_z, burner_rad, BURNER_H, burnerStyle);
		ObjectModels.addCyl(vc, -burner_dx,  burner_dy, burner_z, burner_rad, BURNER_H, burnerStyle);
		ObjectModels.addCyl(vc, -burner_dx, -burner_dy, burner_z, burner_rad, BURNER_H, burnerStyle);
		ObjectModels.addCyl(vc,  burner_dx, -burner_dy, burner_z, burner_rad, BURNER_H, burnerStyle);

		return vc;
	}

}
