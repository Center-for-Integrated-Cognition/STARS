package soargroup.mobilesim.sim;

import java.io.IOException;
import java.util.*;

import java.nio.ByteBuffer;

import april.jmat.LinAlg;
import april.jmat.MathUtil;
import april.sim.SimObject;
import april.sim.SimWorld;
import april.util.PeriodicTasks;
import april.util.TimeUtil;

import soargroup.mobilesim.sim.SimRobot;
import soargroup.mobilesim.sim.RosieSimObject;
import soargroup.mobilesim.sim.SimRegion;
import soargroup.mobilesim.sim.attributes.InRegion;
import soargroup.mobilesim.util.BoundingBox;
import soargroup.mobilesim.util.Util;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import soargroup.mobilesim.lcmtypes.object_data_t;
import soargroup.mobilesim.lcmtypes.object_data_list_t;


public class SimObjectDetector {
	private static double MSG_PER_SEC = 10.0;

	protected SimRobot robot;
	protected SimWorld world;

	protected HashSet<RosieSimObject> currentObjects;
	protected List<SimRegion> regions = null;

    static Random classifierRandom = new Random(3611871);

    PeriodicTasks tasks = new PeriodicTasks(2);

	private boolean fullyObservable = false;

	public SimObjectDetector(SimRobot robot, SimWorld world){
		this.robot = robot;
		this.world = world;
		this.currentObjects = new HashSet<RosieSimObject>();

		this.tasks.addFixedDelay(new DetectorTask(), 1.0/MSG_PER_SEC);

	}

	public void setRunning(boolean b){
		tasks.setRunning(b);
	}

	public void setFullyObservable(boolean b){
		this.fullyObservable = b;
	}

	protected class DetectorTask implements PeriodicTasks.Task {
        public void run(double dt){
            ArrayList<SimObject> simObjects;
            synchronized(world.objects){
            	simObjects = (ArrayList<SimObject>)world.objects.clone();
				if(regions == null){
					regions = new ArrayList<SimRegion>();
					for(SimObject obj : world.objects){
						if(obj instanceof SimRegion){
							regions.add((SimRegion)obj);
						}
					}
				}
            }
			

			updateCurrentObjects(simObjects);
			sendObjectMessage();
        }

		private boolean inCurrentRegion(RosieSimObject obj, SimRobot robot, SimRegion curRegion){
			if(obj instanceof SimDoor){
				// If it's a door, check if it is adjacent to the current region
				return ((SimDoor)obj).connectsRegion(curRegion);
			}
			if(obj == robot.getGrabbedObject()){
				// Grabbed object always in the same region
				return true;
			}
			if(!obj.is(InRegion.class)){
				// We cannot check the region of the object, so we return false
				return false;
			}

			// Return true if object's region is the same as the curRegion
			SimRegion objRegion = obj.as(InRegion.class).getRegion(regions);
			return objRegion != null && objRegion == curRegion;
		}

		private boolean isVisible(RosieSimObject obj, SimRobot robot){
			if(fullyObservable){
				// If we are not using the robot's viewpoint, 
				//    then return true for all objects in current region
				return true;
			}
			if(!obj.isVisible()){
				// Object may report that it is not visible 
				return false;
			}
			if(robot.inViewRange(obj.getBoundingBox().xyzrpy)){
				// Otherwise, check that the object is actually visible from the robot's perspective
				return true;
			}
			return false;
		}

		// Recompute the list of current objects (objects in the current region)
        private void updateCurrentObjects(ArrayList<SimObject> simObjects){
			HashSet<RosieSimObject> rosieObjects = new HashSet<RosieSimObject>();
			for(SimObject obj : simObjects){
				if(obj instanceof RosieSimObject){
					rosieObjects.add((RosieSimObject)obj);
				}
			}

			synchronized(currentObjects){
				currentObjects.clear();
				SimRegion robotRegion = robot.getRegion();
				for(RosieSimObject obj : rosieObjects){
					if(inCurrentRegion(obj, robot, robotRegion)){
						currentObjects.add(obj);
					}
				}
			}
		}

        private void sendObjectMessage(){
			object_data_list_t objectMessage = new object_data_list_t();
			objectMessage.utime = TimeUtil.utime();
			synchronized(currentObjects){
				objectMessage.num_objects = 0;
				objectMessage.objects = new object_data_t[currentObjects.size()];
				for(RosieSimObject obj : currentObjects){
					objectMessage.objects[objectMessage.num_objects] = obj.getObjectData();
					objectMessage.objects[objectMessage.num_objects].visible = isVisible(obj, robot);
					objectMessage.num_objects += 1;
				}
			}
            LCM.getSingleton().publish("DETECTED_OBJECTS", objectMessage);
        }
    }
}
