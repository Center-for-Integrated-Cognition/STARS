package soargroup.mobilesim.sim.attributes;

import java.util.*;
import april.sim.SimObject;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.util.BoundingBox;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.RosieSimObject;
import soargroup.mobilesim.sim.actions.PutDown;
import soargroup.mobilesim.sim.actions.ActionHandler;

public class Surface extends ObjectHolder {
	// Setup anchors with default spacing
	public Surface(RosieSimObject object, boolean useDefaultSpacing){
		super(object);
		if(useDefaultSpacing){
			// Default uses whole width/height and puts anchors at the top
			double[] scale = object.getScale();
			addPoints(scale[0], scale[1], scale[2]/2+0.001, soargroup.mobilesim.MobileSimulator.Settings.ANCHOR_SPACING);
		}
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects){
		// Check to see if there are any objects that start off on this
		// surface and add each one to an anchor point
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
			// Check to see if the bottom of the object (center_z-height/2-episilon)
			// is inside the surface, if so, add it as a held object
			double[] obj_pose = rosieObj.getXYZRPY();
			obj_pose[2] -= rosieObj.getScale()[2] + 0.005;
			if(bbox.containsPoint(obj_pose)){
				this.addObject(rosieObj);
			}
		}
		super.init(worldObjects);
	}

	static {
		//  PutDown.Target: NotValid if target is a Surface and relation != ON
		ActionHandler.addValidateRule(PutDown.Target.class, new ActionHandler.ValidateRule<PutDown.Target>() {
			public IsValid validate(PutDown.Target putdown){
				if(putdown.target.is(Surface.class) && !putdown.relation.equals(RosieConstants.REL_ON)){
					return IsValid.False("Surface: relation must be on");
				}
				return IsValid.True();
			}
		});
	}
}
