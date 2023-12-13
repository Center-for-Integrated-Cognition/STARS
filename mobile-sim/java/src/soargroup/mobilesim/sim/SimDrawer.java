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
import soargroup.mobilesim.util.ResultTypes.*;

import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.attributes.*;

public class SimDrawer extends RosieSimObject {
	// Attributes
	private Openable openable;
	private Receptacle receptacle;
        private static final double OBJ_SPACING = 0.30;
	public SimDrawer(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		openable = new Openable(this, false);
		addAttribute(openable);

		// only have a single point to put things
		receptacle = new Receptacle(this, false);
		//receptacle.addPoint(new double[]{ scale_xyz[0]*0.2, 0.0, -scale_xyz[2]*0.5 + 0.001 });
		receptacle.addPoints(scale_xyz[0], scale_xyz[1], -scale_xyz[2]*0.5 + 0.001, OBJ_SPACING);
		
		addAttribute(receptacle);

		super.init(worldObjects);
	}

	@Override
	public VisChain createVisObject() {
		VisChain c = new VisChain();

		if(openable.isOpen()){
			// Draw open drawer
			c.add(new VzOpenBox(scale_xyz, new VzMesh.Style(color)));
			// Draw body of drawer
			c.add(new VisChain(LinAlg.translate(-scale_xyz[0], 0.0, 0.0), 
						new VzBox(scale_xyz, new VzMesh.Style(color))));
		} else {
			c.add(new VzBox(scale_xyz, new VzMesh.Style(color)));
		}

		return c;
	}

	private void setDoorPosition(String doorValue){
		boolean open = doorValue.equals(RosieConstants.DOOR_OPEN);
		// Move drawer either forward or backward
		double[][] pose = this.getPose();
		double[][] translate = LinAlg.translate(open ? scale_xyz[0] : -scale_xyz[0], 0.0, 0.0);
		pose = LinAlg.matrixAB(pose, translate);
		this.setPose(pose);
	}

	// Action Handling Rules
	static {
		// SetProp.Open Apply: Move the drawer forward
		ActionHandler.addApplyRule(SetProp.Open.class, new ActionHandler.ApplyRule<SetProp.Open>() {
			public Result apply(SetProp.Open open){
				if(open.object instanceof SimDrawer){
					((SimDrawer)open.object).setDoorPosition(open.value);
				}
				return Result.Ok();
			}
		});
		// SetProp.Close Apply: Move the drawer backward
		ActionHandler.addApplyRule(SetProp.Close.class, new ActionHandler.ApplyRule<SetProp.Close>() {
			public Result apply(SetProp.Close close){
				if(close.object instanceof SimDrawer){
					((SimDrawer)close.object).setDoorPosition(close.value);
				}
				return Result.Ok();
			}
		});
	}
}
