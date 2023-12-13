package soargroup.mobilesim;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import april.config.Config;
import april.sim.SimObject;
import april.sim.SimWorld;
import april.sim.Simulator;
import april.util.EnvUtil;
import april.util.GetOpt;
import april.util.TimeUtil;
import april.vis.VisCanvas;
import april.vis.VisConsole;
import april.vis.VisLayer;
import april.vis.VisWorld;

import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.*;
import soargroup.rosie.RosieConstants;

import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.attributes.ObjectHolder;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.control_law_t;

public class MobileSimulator implements LCMSubscriber
{
	/// Global settings for the simulator, should probably put these in a config file 
	public static class Settings {
		// TELEPORT_ROBOT: If true, when a move-to-xy command is sent, the robot will instantly teleport there
		public static boolean TELEPORT_ROBOT = false;

		// COLLIDE_WALLS: If true, the robot will collide with walls (not be able to drive through them)
		public static boolean COLLIDE_WALLS = false;

		// COLLIDE_OBJECTS: If true, the robot will collide with objects (not be able to drive through them)
		public static boolean COLLIDE_OBJECTS = false;

		// ANCHOR_SPACING: Sets the distance in meters between anchors in receptacles/surfaces (smaller values = closer together)
		public final static double ANCHOR_SPACING = 0.3;
	}

	// Sim stuff
    SimWorld world;
    Simulator sim;

	protected SimRobot robot = null;
	protected ArrayList<RosieSimObject> rosieObjs;
	protected ArrayList<BaseSimObject> baseObjs;

    private Timer simulateDynamicsTimer;
    private static final int DYNAMICS_RATE = 10; // FPS to simulate dynamics at

	private int lastHandledCommand = -1;

    public MobileSimulator(GetOpt opts, VisWorld vw, VisLayer vl, VisCanvas vc, VisConsole console) {
        loadWorld(opts);
        sim = new Simulator(vw, vl, console, world);

		rosieObjs = new ArrayList<RosieSimObject>();
		baseObjs = new ArrayList<BaseSimObject>();
		synchronized(world){
			for(SimObject obj : world.objects){
				if(obj instanceof SimRobot){
					robot = (SimRobot)obj;
				}
				if(obj instanceof RosieSimObject){
					rosieObjs.add((RosieSimObject)obj);
				}
				if(obj instanceof BaseSimObject){
					baseObjs.add((BaseSimObject)obj);
				}
			}
			robot.setFullyObservable(opts.getBoolean("fully"));
			robot.setupActionRules();
			for(BaseSimObject baseObj : baseObjs){
				baseObj.init(world.objects);
			}
		}
		if(robot == null){
			System.err.println("WARNING: No SimRobot defined in the world file");
		}

	    simulateDynamicsTimer = new Timer();
	    simulateDynamicsTimer.schedule(new SimulateDynamicsTask(world), 1000, 1000/DYNAMICS_RATE);

		// Listen for rosie commands
		LCM.getSingleton().subscribe("SOAR_COMMAND.*", this);
	}

    public SimWorld getWorld()
    {
    	return world;
    }

	@Override
	public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins) {
        try {
        	if (channel.startsWith("SOAR_COMMAND") && !channel.startsWith("SOAR_COMMAND_STATUS")){
		  		control_law_t controlLaw = new control_law_t(ins);
				if(controlLaw.id == lastHandledCommand){
					return;
				}
				lastHandledCommand = controlLaw.id;

				Action action = null;
				if(controlLaw.name.equals("pick-up")){
					action = parsePickUp(controlLaw);
				} else if(controlLaw.name.equals("put-down")){
					action = parsePutDown(controlLaw);
				} else if(controlLaw.name.equals("put-at-xyz")){
					action = parsePutAtXYZ(controlLaw);
				} else if(controlLaw.name.equals("put-on-object")){
					action = parsePutOnObject(controlLaw);
				} else if(controlLaw.name.equals("change-state")){
					action = parseChangeState(controlLaw);
				} else if(controlLaw.name.equals("use-object")){
					action = parseUseObject(controlLaw);
				} else if(controlLaw.name.equals("press")){
					action = parsePress(controlLaw);
				} else {
					// Unrecognized name - ignore control law
					return;
				}
				if(action == null){
					System.err.println("ERROR: Could not parse action of type " + controlLaw.name);
					return;
				}
				Result result = ActionHandler.handle(action, robot);
				if(result instanceof Err){
					System.err.println(((Err)result).reason);
				} else {
					System.out.println("Performed: " + action);
				}

	      	}
        } catch (IOException ex) {
            System.out.println("WRN: "+ex);
        }
    }

	// Will have every BaseSimObject recompute its VisObject model
	public void recomputeModels(){
		synchronized(world.objects){
			for(SimObject obj : world.objects){
				if(obj instanceof BaseSimObject){
					((BaseSimObject)obj).recreateVisObject();
				}
			}
		}
	}

	private RosieSimObject getRosieObject(control_law_t controlLaw, String param_name){
		String objectIdStr = getParam(controlLaw, param_name, null);
		if(objectIdStr == null){
			return null;
		}
		Integer objectId = new Integer(objectIdStr);

		synchronized(world.objects){
			for(SimObject obj : world.objects){
				if(obj instanceof RosieSimObject){
					RosieSimObject simObj = (RosieSimObject)obj;
					if(simObj.getID().equals(objectId)){
						return simObj;
					}
				}
			}
		}
		System.err.println("MobileSimulator: parsing " + controlLaw.name);
		System.err.println("  The rosie object given by " + objectIdStr + " is does not exist");
		return null;
	}

	private String getParam(control_law_t controlLaw, String param_name, String default_value){
		for(int p = 0; p < controlLaw.num_params; p++){
			if(controlLaw.param_names[p].equals(param_name)){
				return controlLaw.param_values[p].value;
			}
		}
		if(default_value != null){
			return default_value;
		}
		return null;
	}

	private Press parsePress(control_law_t controlLaw){
		RosieSimObject obj = getRosieObject(controlLaw, "object-id");
		if(obj == null){ return null; }
		return new Press(obj);
	}

	private PickUp parsePickUp(control_law_t controlLaw){
		RosieSimObject obj = getRosieObject(controlLaw, "object-id");
		if(obj == null){ return null; }
		return new PickUp(obj);
	}

	private PutDown.Floor parsePutDown(control_law_t controlLaw){
		RosieSimObject obj = robot.getGrabbedObject();
		if(obj == null){
			System.err.println("MobileSimulator: parsing " + controlLaw.name);
			System.err.println("   SimRobot's grabbedObject is null");
			return null;
		}
		return new PutDown.Floor(obj);
	}

	private PutDown.XYZ parsePutAtXYZ(control_law_t controlLaw){
		RosieSimObject movingObj = getRosieObject(controlLaw, "object-id");
		if(movingObj != null){
			removeObjectFromHolders(movingObj);
		} else {
			movingObj = robot.getGrabbedObject();
			if(movingObj == null){
				System.err.println("MobileSimulator: parsing " + controlLaw.name);
				System.err.println("   SimRobot's grabbedObject is null");
				return null;
			}
		}

		double[] xyz = new double[]{ 0, 0, 0 };
		for(int p = 0; p < controlLaw.num_params; p++){
			if(controlLaw.param_names[p].equals("x")){
				xyz[0] = Double.parseDouble(controlLaw.param_values[p].value);
			} else if(controlLaw.param_names[p].equals("y")){
				xyz[1] = Double.parseDouble(controlLaw.param_values[p].value);
			} else if(controlLaw.param_names[p].equals("z")){
				xyz[2] = Double.parseDouble(controlLaw.param_values[p].value);
			} 
		}

		Boolean teleport = getParam(controlLaw, "teleport", "false").toLowerCase().equals("true");

		return new PutDown.XYZ(movingObj, xyz, teleport);
	}

	private PutDown.Target parsePutOnObject(control_law_t controlLaw){
		RosieSimObject movingObj = getRosieObject(controlLaw, "object-id");
		if(movingObj != null){
			removeObjectFromHolders(movingObj);
		} else {
			movingObj = robot.getGrabbedObject();
			if(movingObj == null){
				System.err.println("MobileSimulator: parsing " + controlLaw.name);
				System.err.println("   SimRobot's grabbedObject is null");
				return null;
			}
		}

		RosieSimObject target = getRosieObject(controlLaw, "destination-id");
		if(target == null){ return null; }

		String relation = getParam(controlLaw, "relation", RosieConstants.REL_ON);
		Boolean teleport = getParam(controlLaw, "teleport", "false").toLowerCase().equals("true");

		return new PutDown.Target(movingObj, relation, target, teleport);
	}

	private SetProp parseChangeState(control_law_t controlLaw){
		RosieSimObject obj = getRosieObject(controlLaw, "object-id");
		if(obj == null){ return null; }

		String prop = getParam(controlLaw, "property", null);
		if(prop == null){ return null; }

		String val = getParam(controlLaw, "value", null);
		if(val == null){ return null; }

		return SetProp.construct(obj, prop, val);
	}

	private UseObject parseUseObject(control_law_t controlLaw){
		RosieSimObject obj = getRosieObject(controlLaw, "object-id");
		if(obj == null){ return null; }

		RosieSimObject target = getRosieObject(controlLaw, "target-id");
		if(target == null){ return null; }

		return new UseObject(obj, target);
	}

	private void removeObjectFromHolders(RosieSimObject object){
		// Remove the object from any ObjectHolders it is currently in
		synchronized(world){
			for(SimObject obj : world.objects){
				if(!(obj instanceof RosieSimObject)){
					continue;
				}
				ObjectHolder holder = ((RosieSimObject)obj).as(ObjectHolder.class);
				if(holder != null){
					holder.removeObject(object);
				}
			}
			object.setVisible(true);
		}
	}

    private void loadWorld(GetOpt opts)
    {
    	try {
            Config config = new Config();
            //if (opts.wasSpecified("sim-config"))
            //    config = new ConfigFile(EnvUtil.expandVariables(opts.getString("sim-config")));

            if (opts.getString("world").length() > 0) {
                String worldFilePath = EnvUtil.expandVariables(opts.getString("world"));
                world = new SimWorld(worldFilePath, config);
            } else {
                world = new SimWorld(config);
            }

        } catch (IOException ex) {
            System.err.println("ERR: Error loading sim world.");
            ex.printStackTrace();
            return;
        }
        world.setRunning(true);
    }

    class SimulateDynamicsTask extends TimerTask
    {
		private SimWorld world;
		private long lastUpdate;
		public SimulateDynamicsTask(SimWorld world){
			this.world = world;
			this.lastUpdate = TimeUtil.utime();
		}
		@Override
		public void run() {
			long time = TimeUtil.utime();
			double dt = (double)(time - lastUpdate)/1000000.0;
			synchronized(world){
				for(SimObject simObj : world.objects){
					if(simObj instanceof BaseSimObject){
						((BaseSimObject)simObj).update(dt, world.objects);
					}
				}
			}
			lastUpdate = time;
		}
    }

}
