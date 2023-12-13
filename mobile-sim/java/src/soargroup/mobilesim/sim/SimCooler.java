package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.ObjectModels;
import soargroup.mobilesim.sim.attributes.*;
import soargroup.mobilesim.sim.actions.UseObject;
import soargroup.mobilesim.sim.actions.ActionHandler;
import soargroup.mobilesim.sim.actions.ActionHandler.*;

public class SimCooler extends RosieSimObject {
	private final double TOP_H = 0.25; // Height of top box
	private final double BOT_H = 0.50; // Height of bottom box
	
	private Receptacle receptacle;
	private boolean auto = false;

	public SimCooler(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		// Add 1 anchor point for where to put a mug/cup
		receptacle = new Receptacle(this, false);
		receptacle.addPoint(scale_xyz[0]/10.0, 0.0, (-0.5 + BOT_H)*scale_xyz[2]);
		addAttribute(receptacle);

		Triggerable triggerable = new Triggerable(this, new Triggerable.Callback(){
			public void trigger(RosieSimObject source){
				dispense();
			}
		});
		addAttribute(triggerable);

		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject(){
		VisChain vc = new VisChain();

		VzMesh.Style gray = new VzMesh.Style(Color.gray);
		VzMesh.Style black = new VzMesh.Style(Color.black);
		// Bottom + Top Boxes
		ObjectModels.addBox(vc, 0.05, 0.0, -0.5 + BOT_H/2, 0.9, 0.8, BOT_H, gray); 
		ObjectModels.addBox(vc, 0.05, 0.0, 0.5 - TOP_H/2, 0.9, 0.8, TOP_H, gray); 
		// 3 Sides
		ObjectModels.addBox(vc, 0.0, 0.45, 0.0, 1.0, 0.1, 1.0, black); 
		ObjectModels.addBox(vc, 0.0, -0.45, 0.0, 1.0, 0.1, 1.0, black); 
		ObjectModels.addBox(vc, -0.45, 0.0, 0.0, 0.1, 1.0, 1.0, black); 

		return new VisChain(LinAlg.scale(scale_xyz[0], scale_xyz[1], scale_xyz[2]), vc);
	}

	public void dispense(){
		// Fill any objects in the cooler
		ArrayList<RosieSimObject> objs = receptacle.getObjects();
		for(RosieSimObject obj : objs){
			if(obj.is(Fillable.class)){
				obj.as(Fillable.class).setContents(RosieConstants.WATER);
			}
		}
	}

	@Override
	public void update(double dt, ArrayList<SimObject> worldObjects){
		super.update(dt, worldObjects);
		if(auto){
			dispense();
		}
	}

	public void read(StructureReader ins) throws IOException {
		super.read(ins);

		// [Str] either auto or manual
		auto = ins.readString().toLowerCase().equals("auto");
	}

	public void write(StructureWriter outs) throws IOException {
		super.write(outs);
		outs.writeString(auto ? "auto" : "manual");
	}

}
