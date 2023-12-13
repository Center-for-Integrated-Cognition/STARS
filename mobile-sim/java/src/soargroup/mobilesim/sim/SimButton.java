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

public class SimButton extends RosieSimObject {
	private String targetId;
	private RosieSimObject target = null;

	private Pressable pressable;

	public SimButton(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		// Try to find a world object with the target id
		for(SimObject simObj : worldObjects){
			if(simObj instanceof RosieSimObject){
				RosieSimObject obj = (RosieSimObject)simObj;
				if(obj.getTempId() != null && obj.getTempId().equals(targetId)){
					target = obj;
				}
			}
		}

		pressable = new Pressable(this, target);
		this.addAttribute(pressable);

		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject(){
		VisChain vc = new VisChain();
		ObjectModels.addSphere(vc, 0.0, 0.0, 0.0, 0.5, new VzMesh.Style(color));
		return new VisChain(LinAlg.scale(scale_xyz[0], scale_xyz[1], scale_xyz[2]), vc);
	}

	@Override
	public void read(StructureReader ins) throws IOException {
		super.read(ins);
		// [Str] target id (matches temp_id)
		targetId = ins.readString();
	}

	@Override
	public void write(StructureWriter outs) throws IOException {
		super.write(outs);
		outs.writeString(targetId);
	}

}
