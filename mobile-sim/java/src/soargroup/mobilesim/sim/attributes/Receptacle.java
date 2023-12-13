package soargroup.mobilesim.sim.attributes;

import java.util.ArrayList;

import april.sim.SimObject;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.util.BoundingBox;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.RosieSimObject;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;

public class Receptacle extends ObjectHolder {
	// Setup anchors with default spacing
	public Receptacle(RosieSimObject object, boolean useDefaultSpacing){
		super(object);
		if(useDefaultSpacing){
			// Default uses whole width/height and puts anchors at the bottom
			double[] scale = object.getScale();
			addPoints(scale[0], scale[1], -scale[2]/2+0.001, soargroup.mobilesim.MobileSimulator.Settings.ANCHOR_SPACING);
		}
	}

	// Receptacle with custom custom surface boundaries and point spacing
	public Receptacle(RosieSimObject object, double dx, double dy, double z, double spacing){
		super(object);
		addPoints(dx, dy, z, spacing);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects){
		Openable openable = baseObject.as(Openable.class);
		boolean doorClosed = (openable != null) && !openable.isOpen();

		// Check to see if there are any objects that start off inside this
		// receptacle and add each one to an anchor point
		BoundingBox bbox = baseObject.getBoundingBox();
		for(SimObject simObj : worldObjects){
			if(!(simObj instanceof RosieSimObject)){
				continue;
			}
			RosieSimObject rosieObj = (RosieSimObject)simObj;
			if(rosieObj == baseObject){
				continue;
			}
			if(!rosieObj.is(Grabbable.class)){
				continue;
			}
			// Check if our bounding box contains the center of the sim object
			if(bbox.containsPoint(rosieObj.getXYZRPY())){
				this.addObject(rosieObj);
				rosieObj.setVisible(!doorClosed);
			}
		}
		super.init(worldObjects);
	}


	static {
		//  PutDown.Target: NotValid if target is a Receptacle and relation != IN
		ActionHandler.addValidateRule(PutDown.Target.class, new ValidateRule<PutDown.Target>() {
			public IsValid validate(PutDown.Target putdown){
				if(putdown.target.is(Receptacle.class) && !putdown.relation.equals(RosieConstants.REL_IN)){
					return IsValid.False("Receptacle: relation must be in");
				}
				return IsValid.True();
			}
		});

		// SetProp.Open Apply: If the target is a Receptacle, make all objects visible
		ActionHandler.addApplyRule(SetProp.Open.class, new ApplyRule<SetProp.Open>() {
			public Result apply(SetProp.Open open){
				Receptacle receptacle = open.object.as(Receptacle.class);
				if(receptacle != null){
					ArrayList<RosieSimObject> objs = receptacle.getObjects();
					for(RosieSimObject obj : objs){
						obj.setVisible(true);
					}
				}
				return Result.Ok();
			}
		});

		// SetProp.Close Apply: If the target is a Receptacle, make all objects not visible
		ActionHandler.addApplyRule(SetProp.Close.class, new ApplyRule<SetProp.Close>() {
			public Result apply(SetProp.Close close){
				Receptacle receptacle = close.object.as(Receptacle.class);
				if(receptacle != null){
					ArrayList<RosieSimObject> objs = receptacle.getObjects();
					for(RosieSimObject obj : objs){
						obj.setVisible(false);
					}
				}
				return Result.Ok();
			}
		});
	}
}
