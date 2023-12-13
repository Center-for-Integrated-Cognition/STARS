package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.rosie.RosieConstants;

import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.attributes.*;

public class SimShelves extends RosieSimObject {
	private static final double SHELF_SPACING = 0.50; // vertical spacing between shelves
	private static final double OBJ_SPACING = 0.30;   // how far apart objects are placed on a shelf

	// Attributes
	protected Openable openable = null;
	protected Receptacle receptacle = null;

	public SimShelves(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		// Setup points to put objects on the shelves
		receptacle = new Receptacle(this, false);
		int nshelves = (int)Math.ceil(scale_xyz[2]/SHELF_SPACING)-1;
		double sz = -(nshelves-1)/2.0;
		for(int i = 0; i < nshelves; i += 1){
			receptacle.addPoints(scale_xyz[0], scale_xyz[1], (sz + i)*SHELF_SPACING, OBJ_SPACING);
		}
		if(getProperty("category").equals("cupboard1")){
			receptacle.addPoints(scale_xyz[0], scale_xyz[1], -sz/2, OBJ_SPACING);
		}
		addAttribute(receptacle);

		// If there is a door property, then make it openable
		if(properties.containsKey(RosieConstants.DOOR)){
			boolean isOpen = properties.get(RosieConstants.DOOR).equals(RosieConstants.DOOR_OPEN);
			openable = new Openable(this, isOpen);
			addAttribute(openable);
		}

		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject(){
		boolean open = (openable == null || openable.isOpen());
		if(open){
			return ObjectModels.createShelfModel(scale_xyz, color);
		} else {
			return new VisChain(new VzBox(scale_xyz, new VzMesh.Style(color)));
		}
	}
}
