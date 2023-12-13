package soargroup.mobilesim.sim.actions;

import soargroup.mobilesim.sim.*;

public class PutDown extends Action {
	public final RosieSimObject object;
	public final Boolean teleport;
	public PutDown(RosieSimObject object, Boolean teleport){
		this.object = object;
		this.teleport = teleport;
	}

	public String toString(){
		return "PutDown(" + object + ")";
	}

	// PutDown.Floor -> puts on the object on the ground in front of the robot
	public static class Floor extends PutDown {
		public Floor(RosieSimObject object){
			super(object, false);
		}

		public String toString(){
			return "PutDown.Floor(" + object + ")";
		}
	}

	// PutDown.XYZ -> puts on the object at a specific coordinate
	public static class XYZ extends PutDown {
		public final Double x, z, y;
		public XYZ(RosieSimObject object, double[] xyz, Boolean teleport){
			super(object, teleport);
			this.x = xyz[0];
			this.y = xyz[1];
			this.z = xyz[2];
		}

		public String toString(){
			return "PutDown.XYZ(" + object + ", " + x + ", " + y + ", " + z + ")";
		}
	}

	// PutDown.Target
	public static class Target extends PutDown {
		public final String relation;
		public final RosieSimObject target;
		public Target(RosieSimObject object, String relation, RosieSimObject target, Boolean teleport){
			super(object, teleport);
			this.relation = relation;
			this.target = target;
		}

		public String toString(){
			return "PutDown.Target(" + object + " " + relation + " " + target + ")";
		}
	}

}
