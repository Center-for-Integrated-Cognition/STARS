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
import soargroup.mobilesim.sim.actions.UseObject;
import soargroup.mobilesim.sim.actions.ActionHandler;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.sim.attributes.Fillable;

public class SimKettle extends RosieSimObject {

	public SimKettle(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject(){
		VisChain vc = new VisChain();

		VzMesh.Style style = new VzMesh.Style(color);
		VzMesh.Style black = new VzMesh.Style(Color.black);
		ObjectModels.addCyl(vc, 0.0, 0.0, -0.20, 0.5, 0.6, style); // Body Cylinder
		ObjectModels.addCone(vc, 0.0, 0.0, 0.25, 0.5, 0.3, style); // Top Cone
		ObjectModels.addSphere(vc, 0.0, 0.0, 0.45, 0.1, black); // Top Knob
		ObjectModels.addBox(vc, -0.6, 0.0, -0.2, 0.2, 0.1, 0.4, black); // Handle
		vc.add(new VisChain(LinAlg.rotateY(0.5), LinAlg.translate(0.5, 0.0, 0.2), new VzCylinder(0.1, 0.6, style))); // Spout

		return new VisChain(LinAlg.scale(scale_xyz[0], scale_xyz[1], scale_xyz[2]), vc);
	}

	static {
		// UseObject Validate: If the actor object is a kettle the target must be fillable
		ActionHandler.addValidateRule(UseObject.class, new ValidateRule<UseObject>(){
			public IsValid validate(UseObject use){
				if(use.object instanceof SimKettle && !use.target.is(Fillable.class)){
					return IsValid.False("SimKettle: The target object " + use.target + " is not fillable");
				}
				return IsValid.True();
			}
		});

		// UseObject Apply: If the actor object is a kettle then fill the target
		ActionHandler.addApplyRule(UseObject.class, new ApplyRule<UseObject>(){
			public Result apply(UseObject use){
				if(use.object instanceof SimKettle){
					Fillable fillable = use.target.as(Fillable.class);
					fillable.setContents(RosieConstants.WATER);
					use.target.recreateVisObject();
				}
				return Result.Ok();
			}
		});
	}
}
