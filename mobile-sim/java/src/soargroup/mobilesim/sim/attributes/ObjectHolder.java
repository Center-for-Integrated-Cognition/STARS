package soargroup.mobilesim.sim.attributes;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import april.jmat.LinAlg;
import april.vis.*;
import april.sim.SimObject;

import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;
import soargroup.mobilesim.util.ResultTypes.*;

public class ObjectHolder extends Attribute {
	// Anchors are locations where objects can be placed
	protected ArrayList<AnchorPoint> anchors;
	public ObjectHolder(RosieSimObject baseObject){
		super(baseObject);
		this.anchors = new ArrayList<AnchorPoint>();
	}

	public void init(ArrayList<SimObject> worldObjects) {
		setupRules();
	}

	// Creates a set of points centered at 0, 0 that fit onto a rectange of the given dx and dy size
	//   at the given z height and spacing
	public void addPoints(double dx, double dy, double z, double spacing){
		int nrows = (int)Math.ceil(dy/spacing)-1;
		int ncols = (int)Math.ceil(dx/spacing)-1;
		double srow = -(nrows-1)/2.0;
		double scol = -(ncols-1)/2.0;
		for(int r = 0; r < nrows; r += 1){
			for(int c = 0; c < ncols; c += 1){
				anchors.add(new AnchorPoint((scol+c)*spacing, (srow+r)*spacing, z));
			}
		}
	}

	public void addPoint(double[] xyz){
		anchors.add(new AnchorPoint(xyz[0], xyz[1], xyz[2]));
	}

	public void addPoint(double x, double y, double z){
		anchors.add(new AnchorPoint(x, y, z));
	}

	public List<RosieSimObject> getHeldObjects(){
		List<RosieSimObject> objs = new ArrayList<RosieSimObject>();
		for(AnchorPoint pt : anchors){
			RosieSimObject obj = pt.getObject();
			if(obj != null){
				objs.add(obj);
			}
		}
		return objs;
	}

	@Override
	public void moveHandler(double[] xyzrpy){
		double[][] newPose = LinAlg.xyzrpyToMatrix(xyzrpy);
		for(AnchorPoint pt : this.anchors){
			pt.updateObject(newPose);
		}
	}

	@Override
	public void render(VisChain vc){
		if(soargroup.mobilesim.MobileGUI.Settings.DRAW_ANCHORS){
			// Add a small wireframe box for each anchor
			for(AnchorPoint anchor : anchors){
				vc.add(new VisChain(LinAlg.translate(anchor.xyz),
							new VzBox(new double[]{ 0.04, 0.04, 0.04}, new VzLines.Style(Color.black, 0.01))));
			}
		}
	}

	public boolean hasOpenPoint(){
		for(AnchorPoint pt : anchors){
			if(!pt.hasObject()){
				return true;
			}
		}
		return false;
	}

	public Result addObject(RosieSimObject object){
		return addObject(object, object.getXYZRPY());
	}

	public Result addObject(RosieSimObject object, double[] robotPos){
		double[][] parentPose = baseObject.getPose();
		AnchorPoint closestPt = null;
		double closestDistance = Double.MAX_VALUE;
		for(AnchorPoint pt : anchors){
			if(!pt.hasObject()){
				double dist = pt.getDistanceSq(robotPos, parentPose);
				if(dist < closestDistance){
					closestDistance = dist;
					closestPt = pt;
				}
			}
		}
		if(closestPt == null){
			return Result.Err("ObjectHolder: No open points");
		}
		closestPt.setObject(object);
		return Result.Ok();
	}

	public void removeObject(RosieSimObject object){
		for(AnchorPoint pt : anchors){
			if(pt.getObject() == object){
				pt.setObject(null);
			}
		}
	}

	public ArrayList<RosieSimObject> getObjects(){
		ArrayList<RosieSimObject> objs = new ArrayList<RosieSimObject>();
		for(AnchorPoint pt : anchors){
			RosieSimObject obj = pt.getObject();
			if(obj != null){
				objs.add(obj);
			}
		}
		return objs;
	}

	// Registering Action Handling Rules
	protected void setupRules(){
		// PickUp Apply: Remove the object from any anchors
		ActionHandler.addApplyRule(PickUp.class, new ApplyRule<PickUp>() {
			public Result apply(PickUp pickup){
				removeObject(pickup.object);
				return Result.Ok();
			}
		});
	}

	static {
		//  PutDown.Target: Valid if there is a free anchor available
		ActionHandler.addValidateRule(PutDown.Target.class, new ValidateRule<PutDown.Target>() {
			public IsValid validate(PutDown.Target putdown){
				ObjectHolder holder = putdown.target.as(ObjectHolder.class);
				if(holder == null){
					return IsValid.False("Target is not an ObjectHolder");
				}
				return holder.hasOpenPoint() ? IsValid.True() : IsValid.False("ObjectHolder: No open points");
			}
		});

		// PutDown.Target Apply: Add the object to the anchors
		ActionHandler.addApplyRule(PutDown.Target.class, new ApplyRule<PutDown.Target>() {
			public Result apply(PutDown.Target putdown){
				ObjectHolder holder = putdown.target.as(ObjectHolder.class);
				if(holder == null){
					return Result.Err("Target is not an ObjectHolder");
				}
				return holder.addObject(putdown.object, putdown.robot.getXYZRPY());
			}
		});
	}

	// An anchor is a point on an object where another object can be placed
	// Used so that something like a table can have multiple objects not all at the same place
	protected class AnchorPoint {
		public AnchorPoint(double x, double y, double z){
			this.xyz = new double[]{ x, y, z };
		}
		public final double[] xyz;
		private RosieSimObject heldObj = null;

		public RosieSimObject getObject(){
			checkObject();
			return heldObj;
		}

		public void setObject(RosieSimObject obj){
			heldObj = obj;
			if(heldObj != null){
				heldObj.setPose(calcObjectPose(baseObject.getPose()));
			}
		}

		public boolean hasObject(){
			checkObject();
			return (heldObj != null);
		}

		// Checks the held object, then updates its pose
		public void updateObject(double[][] newPose){
			checkObject();
			if(heldObj != null){
				heldObj.setPose(calcObjectPose(newPose));
			}
		}

		// Returns the squared distance from the anchor point to the given pos (in world space)
		public double getDistanceSq(double[] pos, double[][] parentPose){
			double[][] local_translate = LinAlg.translate(xyz[0], xyz[1], xyz[2]);
			double[][] world_pose = LinAlg.matrixAB(parentPose, local_translate);
			double[] world_xyz = LinAlg.matrixToXyzrpy(world_pose);
			return LinAlg.squaredDistance(world_xyz, pos, 3);

		}

		// Makes sure the heldObject is still at the anchor point (hasn't been moved somewhere else)
		private void checkObject(){
			if(heldObj == null){ return; }
			double[][] obj_pose = calcObjectPose(baseObject.getPose());
			double[] obj_pos = LinAlg.matrixToXyzrpy(obj_pose);
			if(LinAlg.squaredDistance(obj_pos, heldObj.getXYZRPY(), 2) > 0.01){
				// Object must have been moved
				heldObj = null;
			}
		}

		// Gets the pose of the held object at the anchor point (in world coordinates, apply base transform)
		private double[][] calcObjectPose(double[][] parentPose){
			double[][] local_translate = LinAlg.translate(xyz[0], xyz[1], xyz[2] + heldObj.getScale()[2]/2 + 0.001);
			return LinAlg.matrixAB(parentPose, local_translate);
		}
	}
}

