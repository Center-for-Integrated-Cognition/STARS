package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.actions.PutDown;
import soargroup.mobilesim.sim.actions.ActionHandler;
import soargroup.mobilesim.sim.attributes.ObjectHolder;

public class SimPerson extends RosieSimObject {
	protected ObjectHolder objHolder;

	// Body measurements
	double head_r;
	double shoulder_h; 
	double torso_h;
	double legs_h;
	double limb_w;

	public SimPerson(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects){
		// Default size
		scale_xyz[0] *= 0.25;
		scale_xyz[1] *= 0.5;
		scale_xyz[2] *= 1.8;

		// measurements
		head_r = scale_xyz[0]*0.8;
		shoulder_h = scale_xyz[2]/2 - head_r*2;
		torso_h = (scale_xyz[2] - head_r*2)*0.5;
		legs_h = (scale_xyz[2] - head_r*2 - torso_h);
		limb_w = 0.12;

		objHolder = new ObjectHolder(this);
		objHolder.addPoint(limb_w, -scale_xyz[1]/2+limb_w/2, shoulder_h - torso_h);
		addAttribute(objHolder);

		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject() {
		VisChain vc = new VisChain();

		Color legColor = new Color(50, 20, 20);

		// Head
		vc.add(new VisChain(LinAlg.translate(0.0, 0.0, scale_xyz[2]/2 - head_r), 
					new VzSphere(head_r, new VzMesh.Style(new Color(224, 172, 105)))));

		// Torso
		vc.add(new VisChain(LinAlg.translate(0.0, 0.0, shoulder_h - torso_h/2), 
					new VzBox(scale_xyz[0], scale_xyz[1]-2*limb_w, torso_h, new VzMesh.Style(color))));

		// Arms
		vc.add(new VisChain(LinAlg.translate(0.0, scale_xyz[1]/2-limb_w/2, shoulder_h - limb_w/2 - torso_h/2), 
					new VzBox(limb_w, limb_w, torso_h+limb_w, new VzMesh.Style(color))));

		vc.add(new VisChain(LinAlg.translate(0.0, -scale_xyz[1]/2+limb_w/2, shoulder_h - limb_w/2 - torso_h/2), 
					new VzBox(limb_w, limb_w, torso_h+limb_w, new VzMesh.Style(color))));

		// Legs
		vc.add(new VisChain(LinAlg.translate(0.0, -limb_w*0.6, -scale_xyz[2]/2 + legs_h/2),
					new VzBox(limb_w, limb_w, legs_h, new VzMesh.Style(legColor))));

		vc.add(new VisChain(LinAlg.translate(0.0, limb_w*0.6, -scale_xyz[2]/2 + legs_h/2),
					new VzBox(limb_w, limb_w, legs_h, new VzMesh.Style(legColor))));

		return vc;
	}

	static {
		//  PutDown.Target: NotValid if target is a Person and relation != TO
		ActionHandler.addValidateRule(PutDown.Target.class, new ActionHandler.ValidateRule<PutDown.Target>() {
			public IsValid validate(PutDown.Target putdown){
				if(putdown.target instanceof SimPerson && !putdown.relation.equals(RosieConstants.REL_TO)){
					return IsValid.False("SimPerson: relation must be to");
				}
				return IsValid.True();
			}
		});
	}
}
