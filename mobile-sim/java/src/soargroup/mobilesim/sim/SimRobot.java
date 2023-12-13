package soargroup.mobilesim.sim;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.event.*;
import java.awt.*;

import javax.swing.*;

import april.config.Config;
import april.jmat.LinAlg;
import april.jmat.MathUtil;
import april.jmat.geom.*;
import april.vis.*;
import april.lcm.*;
import april.util.*;
import april.sim.*;
import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.commands.CommandInterpreter;
import soargroup.mobilesim.commands.CommandCoordinator.Status;
import soargroup.mobilesim.util.*;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.vis.*;

import soargroup.mobilesim.sim.attributes.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.Action.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.laser_t;
import soargroup.mobilesim.lcmtypes.diff_drive_t;
import soargroup.mobilesim.lcmtypes.grid_map_t;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.lcmdoubles_t;
import soargroup.mobilesim.lcmtypes.robot_info_t;
import soargroup.mobilesim.lcmtypes.robot_map_data_t;

public class SimRobot implements SimObject, LCMSubscriber
{
	static final double OBJECT_VIEW_DIST = 20.0;  // max distant it can see objects at
	static final double OBJECT_VIEW_DIST_SQ = OBJECT_VIEW_DIST * OBJECT_VIEW_DIST;
	static final double OBJECT_VIEW_ANGLE = Math.PI/2;  // max angle it can see objects at
	static final double OBJECT_VIEW_ANGLE_COS = Math.cos(OBJECT_VIEW_ANGLE/2);

    static final int ROBOT_MAP_DATA_HZ = 10;
    long lastMapData = 0;

    int ROBOT_ID = 3;
    SimWorld sw;
    DifferentialDrive drive;

    int robotID;

    CommandInterpreter ci;
    LCM lcm = LCM.getSingleton();

    SimObjectDetector objDetector;

    CompoundShape shape;
    VisObject visObj;

    ExpiringMessageCache<diff_drive_t> diffdriveCache = new ExpiringMessageCache<diff_drive_t>(0.25);

    PeriodicTasks tasks = new PeriodicTasks(2);

    // static variables ..
    static Random r = new Random();
    static Model4 model4 = new Model4();

    private RosieSimObject grabbedObject = null;
	protected SimRegion curRegion = null;

    public SimRobot(SimWorld sw)
    {
        this.sw = sw;
        this.robotID = ROBOT_ID;

        shape = makeShape();

		visObj = model4;

        // visObj = new VzSphere(.5, new VzMesh.Style(Color.RED));

        boolean sim = true;
        ci = new CommandInterpreter(sim);

        // Reproduce this in monte-carlo bot
        drive = new DifferentialDrive(sw, this, new double[3]);
        drive.centerOfRotation = new double[] { 0.20, 0, 0 };
        drive.voltageScale = 24.0;
        drive.wheelDiameter = 0.25;
        drive.baseline = 0.46;  // As measured to wheel centers
        drive.translation_noise = 0.1;
        drive.rotation_noise = 0.05;

        // Motor setup
        double K_t = 0.7914*2;    // torque constant in [Nm / A] * multiplier to speed us up
        drive.leftMotor.torque_constant = K_t;
        drive.rightMotor.torque_constant = K_t;
        double K_emf = 1.406; // emf constant [V/(rad/s)]
        drive.leftMotor.emf_constant = K_emf;
        drive.rightMotor.emf_constant = K_emf;
        double K_wr = 2.5;  // Winding resistance [ohms]
        drive.leftMotor.winding_resistance = K_wr;
        drive.rightMotor.winding_resistance = K_wr;
        double K_inertia = 0.5; // XXX Hand-picked inertia [kg m^2]
        drive.leftMotor.inertia = K_inertia;
        drive.rightMotor.inertia = K_inertia;
        double K_drag = 1.0;    // XXX Hand-picked drag [Nm / (rad/s)], always >= 0
        drive.leftMotor.drag_constant = K_drag;
        drive.rightMotor.drag_constant = K_drag;

        objDetector = new SimObjectDetector(this, sw);

        lcm.subscribe("DIFF_DRIVE", this);
        lcm.subscribe("SOAR_COMMAND.*", this);

        tasks.addFixedDelay(new ImageTask(sw.objects), 0.04);
        tasks.addFixedDelay(new PoseTask(), 0.04);
        tasks.addFixedDelay(new ControlTask(), 0.01);
//        tasks.addFixedDelay(new ClassifyTask(), 0.04);

	}

    public april.sim.Shape getShape()
    {
        return shape;
    }

    private CompoundShape makeShape()
    {
        CompoundShape shape = new CompoundShape();
		shape.add(
			LinAlg.translate(0, 0, MagicRobot.ORIGIN_Z_OFFSET_GROUND),

			// chassis
			LinAlg.translate(MagicRobot.CHASSIS_MAIN_POS),
			new BoxShape(MagicRobot.CHASSIS_MAIN_SIZE),
			LinAlg.inverse(LinAlg.translate(MagicRobot.CHASSIS_MAIN_POS)),

			LinAlg.translate(MagicRobot.CHASSIS_BATTERY_POS),
			new BoxShape(MagicRobot.CHASSIS_BATTERY_SIZE),
			LinAlg.inverse(LinAlg.translate(MagicRobot.CHASSIS_BATTERY_POS)),

			// front handle
			LinAlg.translate(MagicRobot.HANDLE_FRONT_POS),
			new BoxShape(MagicRobot.HANDLE_CENTER_SIZE),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_FRONT_POS)),

			LinAlg.translate(MagicRobot.HANDLE_FRONT_TOP_POS),
			LinAlg.rotateY(Math.PI/6),
			new BoxShape(MagicRobot.HANDLE_OTHER_SIZE),
			LinAlg.rotateY(-Math.PI/6),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_FRONT_TOP_POS)),

			LinAlg.translate(MagicRobot.HANDLE_FRONT_BOTTOM_POS),
			LinAlg.rotateY(-Math.PI/6),
			new BoxShape(MagicRobot.HANDLE_OTHER_SIZE),
			LinAlg.rotateY(Math.PI/6),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_FRONT_BOTTOM_POS)),

			// rear handle
			LinAlg.translate(MagicRobot.HANDLE_REAR_POS),
			new BoxShape(MagicRobot.HANDLE_CENTER_SIZE),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_REAR_POS)),

			LinAlg.translate(MagicRobot.HANDLE_REAR_TOP_POS),
			LinAlg.rotateY(-Math.PI/6),
			new BoxShape(MagicRobot.HANDLE_OTHER_SIZE),
			LinAlg.rotateY(Math.PI/6),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_REAR_TOP_POS)),

			LinAlg.translate(MagicRobot.HANDLE_REAR_BOTTOM_POS),
			LinAlg.rotateY(Math.PI/6),
			new BoxShape(MagicRobot.HANDLE_OTHER_SIZE),
			LinAlg.rotateY(-Math.PI/6),
			LinAlg.inverse(LinAlg.translate(MagicRobot.HANDLE_REAR_BOTTOM_POS)),

			// sensor head
			LinAlg.translate(MagicRobot.SENSOR_HEAD_POS),
			new BoxShape(MagicRobot.SENSOR_HEAD_SIZE),
			LinAlg.inverse(LinAlg.translate(MagicRobot.SENSOR_HEAD_POS))
			);
        return shape;
    }

    public VisObject getVisObject()
    {
        return visObj;
    }

    public double[][] getPose()
    {
        return LinAlg.quatPosToMatrix(drive.poseTruth.orientation,
                                      drive.poseTruth.pos);
    }

	public double[] getXYZRPY(){
        return LinAlg.matrixToXyzrpy(LinAlg.quatPosToMatrix(
					drive.poseTruth.orientation, drive.poseTruth.pos));
	}

    public double[][] getNoisyPose(double[] L2G)
    {
        double[][] M = LinAlg.quatPosToMatrix(drive.poseOdom.orientation,
                                              drive.poseOdom.pos);
        if (L2G == null)
            return M;

        double[] M_xyt = LinAlg.matrixToXYT(M);
        M_xyt = LinAlg.xytMultiply(L2G, M_xyt);
        return LinAlg.xytToMatrix(M_xyt);
    }

    /* Get a transformation that will convert the current local pose into the
     * current global pose. Returns an XYT
     */
    public double[] getL2G()
    {
        double[] gxyt = LinAlg.matrixToXYT(getPose());
        double[] lxyt = LinAlg.matrixToXYT(getNoisyPose(null));
        double[] L2G = new double[3];

        // Angle is easy to compute
        L2G[2] = gxyt[2] - lxyt[2];
        double s = Math.sin(L2G[2]);
        double c = Math.cos(L2G[2]);

        L2G[0] = gxyt[0] - c*lxyt[0] + s*lxyt[1];
        L2G[1] = gxyt[1] - s*lxyt[0] - c*lxyt[1];

        return L2G;
    }

    public void setPose(double T[][])
    {
        drive.poseTruth.orientation = LinAlg.matrixToQuat(T);
        drive.poseTruth.pos = new double[] { T[0][3], T[1][3], 0 };
    }

	// Return the closest region from the list that contains the position of the object
	public SimRegion getRegion(){
		Double bestDist = Double.MAX_VALUE;
		curRegion = null;
		synchronized(sw.objects){
			for(SimObject obj : sw.objects){
				if(!(obj instanceof SimRegion)){
					continue;
				}
				SimRegion region = (SimRegion)obj;
				if(region.contains(drive.poseTruth.pos)){
					double dist2 = region.getDistanceSq(drive.poseTruth.pos);
					if(dist2 < bestDist){
						bestDist = dist2;
						curRegion = region;
					}
				}
			}
		}
		return curRegion;
	}

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            messageReceivedEx(lcm, channel, ins);
        } catch (IOException ex) {
            System.out.println("WRN: "+ex);
        }
    }

    private void messageReceivedEx(LCM lcm, String channel, LCMDataInputStream ins) throws IOException
    {
        if (channel.equals("DIFF_DRIVE")) {
            diff_drive_t msg = new diff_drive_t(ins);
            diffdriveCache.put(msg, msg.utime);
        }

        // AM: Added so that simulated drive-xy commands teleport the robot
		if (channel.startsWith("SOAR_COMMAND")) {
			control_law_t controlLaw = new control_law_t(ins);
			if(controlLaw.name.equals("drive-xy")){
				double newx = 0.0;
				double newy = 0.0;
				for(int p = 0; p < controlLaw.num_params; p++){
					if(controlLaw.param_names[p].equals("x")){
						newx = Double.parseDouble(controlLaw.param_values[p].value);
					} else if(controlLaw.param_names[p].equals("y")){
						newy = Double.parseDouble(controlLaw.param_values[p].value);
					}
				}
				if(soargroup.mobilesim.MobileSimulator.Settings.TELEPORT_ROBOT){
					drive.poseTruth.pos[0] = newx;
					drive.poseTruth.pos[1] = newy;
				}
			} else if (controlLaw.name.toLowerCase().equals("pause")){
				this.setRunning(false);
			} else if (controlLaw.name.toLowerCase().equals("resume")){
				this.setRunning(true);
			}
		}
    }

	public void setFullyObservable(boolean b){
		objDetector.setFullyObservable(b);
	}

    public RosieSimObject getGrabbedObject(){
    	return grabbedObject;
    }

	public void setupActionRules(){
		// PickUp: Valid if not holding anything
		ActionHandler.addValidateRule(PickUp.class, new ValidateRule<PickUp>(){
			public IsValid validate(PickUp pickup){
				if(grabbedObject != null)
					return IsValid.False("SimRobot: Already holding object " + grabbedObject);
				return IsValid.True();
			}
		});

		// PickUp Apply: Set grabbedObject
		ActionHandler.addApplyRule(PickUp.class, new ApplyRule<PickUp>(){
			public Result apply(PickUp pickup){
				grabbedObject = pickup.object;
				return Result.Ok();
			}
		});

		// PutDown: Valid if currently held (and not teleporting)
		ActionHandler.addValidateRule(PutDown.class, new ValidateRule<PutDown>(){
			public IsValid validate(PutDown putdown){
				if(!putdown.teleport && putdown.object != grabbedObject){
					return IsValid.False("SimRobot: Object " + grabbedObject + " is not the held object");
				}
				return IsValid.True();
			}
		});

		// PutDown Apply: Set grabbedObject
		ActionHandler.addApplyRule(PutDown.class, new ApplyRule<PutDown>(){
			public Result apply(PutDown putdown){
				if(putdown.object == grabbedObject){
					grabbedObject = null;
				}
				return Result.Ok();
			}
		});

		// PutDownFloor Apply: Set the position of the object to in front of the robot
		ActionHandler.addApplyRule(PutDown.Floor.class, new ApplyRule<PutDown.Floor>(){
			public Result apply(PutDown.Floor putdown){
				double[] robotPos = LinAlg.copy(drive.poseTruth.pos);
				double[] forward = LinAlg.matrixAB(LinAlg.quatToMatrix(drive.poseTruth.orientation), new double[]{1.0, 0.0, 0.0, 0.0});
				forward = LinAlg.scale(forward, 0.6);
				double[] newPos = LinAlg.add(robotPos, forward);
				double[] xyzrpy = new double[]{ newPos[0], newPos[1], 0.2, 0, 0, 0 };
				putdown.object.setPose(LinAlg.xyzrpyToMatrix(xyzrpy));
				return Result.Ok();
			}
		});

		// PutDown.XYZ Apply: Set the position of the object to the given coordinates
		ActionHandler.addApplyRule(PutDown.XYZ.class, new ApplyRule<PutDown.XYZ>(){
			public Result apply(PutDown.XYZ putdown){
				putdown.object.setPose(LinAlg.xyzrpyToMatrix(new double[]{ putdown.x, putdown.y, putdown.z, 0, 0, 0 }));
				return Result.Ok();
			}
		});

		// UseObject: Valid if the actor object is held
		ActionHandler.addValidateRule(UseObject.class, new ValidateRule<UseObject>(){
			public IsValid validate(UseObject use){
				if(grabbedObject != use.object){
					return IsValid.False("SimRobot: The object " + use.object + " is not grabbed");
				}
				return IsValid.True();
			}
		});
	}

    public boolean inViewRange(double[] xyz){
    	double[] robotPos  = LinAlg.copy(this.drive.poseTruth.pos);
    	double[] toPoint = LinAlg.subtract(LinAlg.copy(xyz, 3), robotPos);
    	toPoint[2] = 0.0;

    	// Check if point is within view distance
    	double sqDist = toPoint[0]*toPoint[0] + toPoint[1]*toPoint[1] + toPoint[2]*toPoint[2];
    	if(sqDist > OBJECT_VIEW_DIST_SQ){
    		return false;
    	}
    	if(sqDist < 0.5){
    		// Within 50 cm of object, report as seen
    		return true;
    	}

    	double[] forward = LinAlg.matrixAB(LinAlg.quatToMatrix(this.drive.poseTruth.orientation), new double[]{1.0, 0.0, 0.0, 0.0});
    	forward = LinAlg.copy(forward, 3);
    	double dp = LinAlg.dotProduct(forward, toPoint);
    	double lengthProduct = LinAlg.magnitude(forward) * LinAlg.magnitude(toPoint);
    	if(dp/lengthProduct > OBJECT_VIEW_ANGLE_COS){
    		return true;
    	}
    	return false;
    }

    class ImageTask implements PeriodicTasks.Task
    {
        double gridmap_range = 10;
        double gridmap_meters_per_pixel = 0.1;

        HashSet<SimObject> ignore = null;

        public ImageTask(Collection<SimObject> objects)
        {
        }

        public void run(double dt)
        {
        	if(ignore == null){
        		ignore = new HashSet<SimObject>();
        		ignore.add(SimRobot.this);
        		for(SimObject obj : sw.objects){
					// Ignore all objects (dont collide with anything)
					ignore.add(obj);
        			//if(obj instanceof RosieSimObject){
        			//	ignore.add(obj);
        			//}
        		}
        	}
            double radstep = Math.atan2(gridmap_meters_per_pixel, gridmap_range);
            double minDeg = -135;
            double maxDeg = 135;
            double maxRange = 29.9;
            double rad0 = Math.toRadians(minDeg);
            double rad1 = Math.toRadians(maxDeg);

            double T_truth[][] = LinAlg.matrixAB(LinAlg.quatPosToMatrix(drive.poseTruth.orientation,
                                                                        drive.poseTruth.pos),
                                                 LinAlg.translate(0.3, 0, 0.25));

            double T_odom[][] = LinAlg.matrixAB(LinAlg.quatPosToMatrix(drive.poseOdom.orientation,
                                                                       drive.poseOdom.pos),
                                                LinAlg.translate(0.3, 0, 0.25));


            double ranges[] = Sensors.laser(sw, ignore, T_truth, (int) ((rad1-rad0)/radstep),
                                            rad0, radstep, maxRange);

            // XXX Config file
            double mean = 0;
            double stddev = 0.01;

            laser_t laser = new laser_t();
            laser.utime = TimeUtil.utime();
            laser.nranges = ranges.length;
            laser.ranges = LinAlg.copyFloats(ranges);
            laser.rad0 = (float) rad0;
            laser.radstep = (float) radstep;

            lcm.publish(Util.getConfig().getString("robot.lcm.laser_channel", "HOKUYO_LIDAR"), laser);

            // Make gzipped grid maps periodically
            if (laser.utime - lastMapData > 1000000L/ROBOT_MAP_DATA_HZ) {
                grid_map_t gm = new grid_map_t();
                gm.utime = laser.utime;

                double[] xyt = LinAlg.matrixToXYT(T_truth);
                double x0 = xyt[0] - 5;
                double y0 = xyt[1] - 5;

                // Project data into map
                GridMap map = GridMap.makeMeters(x0, y0, 10.0, 10.0, 0.1, 0);
                for (int i = 0; i < laser.nranges; i++) {
                    double r = laser.ranges[i];
                    if (r < 0)
                        continue;
                    double t = laser.rad0 + i*laser.radstep + xyt[2];
                    double x = xyt[0] + r*Math.cos(t);
                    double y = xyt[1] + r*Math.sin(t);

                    map.setValue(x, y, (byte)1);
                }

                gm.x0 = map.x0;
                gm.y0 = map.y0;
                gm.width = map.width;
                gm.height = map.height;
                gm.meters_per_pixel = map.metersPerPixel;

                // DEBUG
                //gm.encoding = 0;
                //gm.datalen = map.data.length;
                //gm.data = map.data;
                //lcm.publish("GRID_MAP_DEBUG", gm);

                gm.encoding = grid_map_t.ENCODING_GZIP;

                if (gm.encoding == grid_map_t.ENCODING_GZIP) {
                    try {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        GZIPOutputStream gzos = new GZIPOutputStream(bytes);
                        gzos.write(map.data, 0, map.data.length);
                        gzos.finish();

                        byte[] data = bytes.toByteArray();
                        gm.datalen = data.length;
                        gm.data = data;

                        gzos.close();
                    } catch (IOException ex) {
                        System.out.println("ERR: Could not GZIP grid map data");
                        ex.printStackTrace();
                        return;
                    }
                }

                robot_map_data_t rmd = new robot_map_data_t();
                rmd.utime = laser.utime;
                rmd.gridmap = gm;
                rmd.latlon_deg = new double[] {Double.NaN, Double.NaN};
                rmd.xyt_local = xyt;

                lcm.publish(Util.getConfig().getString("robot.lcm.map_channel", "ROBOT_MAP_DATA")+"_"+ROBOT_ID, rmd);

                lastMapData = laser.utime;
            }

        }
    }

    // Implementaton of this task moved to robot.perception.SimObjectDetector
    // class ClassifyTask implements PeriodicTasks.Task

    class PoseTask implements PeriodicTasks.Task
    {
        public PoseTask()
        {
        }

        public void run(double dt)
        {
            drive.poseOdom.utime = TimeUtil.utime();
            lcm.publish("POSE", drive.poseOdom);

            // Compute L2G
            double[] gxyt = LinAlg.matrixToXYT(LinAlg.quatPosToMatrix(drive.poseTruth.orientation,
                                                                      drive.poseTruth.pos));
            double[] lxyt = LinAlg.matrixToXYT(LinAlg.quatPosToMatrix(drive.poseOdom.orientation,
                                                                      drive.poseOdom.pos));
            double[] l2g_ = getL2G();
            lcmdoubles_t l2g = new lcmdoubles_t();
            l2g.utime = drive.poseOdom.utime;
            l2g.ndata = 3;
            l2g.data = l2g_;
            lcm.publish("L2G", l2g);

            double[] xyzrpy = LinAlg.quatPosToXyzrpy(drive.poseTruth.orientation, drive.poseTruth.pos);
            if(grabbedObject != null){
				double[] xyzrpy2 = LinAlg.copy(xyzrpy);
				xyzrpy2[2] = 0.5;
            	double[][] robPose = LinAlg.xyzrpyToMatrix(xyzrpy2);
    		    grabbedObject.setPose(robPose);
            }

            robot_info_t robotInfo = new robot_info_t();
            robotInfo.utime = TimeUtil.utime();
            robotInfo.xyzrpy = xyzrpy;
			SimRegion region = getRegion();
			robotInfo.current_waypoint = (region == null ? "none" : region.getHandle());
            robotInfo.held_object = (grabbedObject == null ? -1 : grabbedObject.getID());
            lcm.publish("ROBOT_INFO", robotInfo);
        }
    }

    class ControlTask implements PeriodicTasks.Task
    {
        PathParams params;

        public ControlTask()
        {
            params = PathParams.makeDefault();
        }

        public void run(double dt) {
            double[] mcmd = new double[2];

            double center_xyz[] = LinAlg.add(drive.poseOdom.pos, LinAlg.quatRotate(drive.poseOdom.orientation, drive.centerOfRotation));

            double q[] = drive.poseOdom.orientation;

            diff_drive_t dd = diffdriveCache.get();
            if (dd != null) {
                mcmd = new double[] { dd.left, dd.right };
            }

            // Add in some resistance to turning. Let's call it the
            // "tank-driviness" factor. In general, our commands
            // regress towards the mean of the two when trying to turn
            double tankFactor = 0.3;
            double avg = mcmd[0]+mcmd[1]/2;
            double dl = avg-mcmd[0];
            double dr = avg-mcmd[1];
            mcmd[0] += tankFactor*dl;
            mcmd[1] += tankFactor*dr;

            drive.motorCommands = mcmd;
        }
    }

    /** Restore state that was previously written **/
    public void read(StructureReader ins) throws IOException
    {
        this.robotID = ins.readInt();

        double Ttruth[][] = LinAlg.xyzrpyToMatrix(ins.readDoubles());
        double Todom[][] = LinAlg.xyzrpyToMatrix(ins.readDoubles());

        synchronized(drive) {
            drive.poseTruth.orientation = LinAlg.matrixToQuat(Ttruth);
            drive.poseTruth.pos = new double[] { Ttruth[0][3], Ttruth[1][3], Ttruth[2][3] };

            drive.poseOdom.orientation = LinAlg.matrixToQuat(Todom);
            drive.poseOdom.pos = new double[] { Todom[0][3], Todom[1][3], Todom[2][3] };
        }
    }

    /** Write one or more lines that serialize this instance. No line
     * is allowed to consist of just an asterisk. **/
    public void write(StructureWriter outs) throws IOException
    {
        double Ttruth[][] = LinAlg.quatPosToMatrix(drive.poseTruth.orientation, drive.poseTruth.pos);
        double Todom[][] = LinAlg.quatPosToMatrix(drive.poseOdom.orientation, drive.poseOdom.pos);

        outs.writeComment("Robot ID");
        outs.writeInt(robotID);
        outs.writeComment("XYZRPY Truth");
        outs.writeDoubles(LinAlg.matrixToXyzrpy(Ttruth));
        outs.writeComment("XYZRPY Odom");
        outs.writeDoubles(LinAlg.matrixToXyzrpy(Todom));
    }

    public synchronized void setRunning(boolean b)
    {
        drive.setRunning(b);
        tasks.setRunning(b);
        ci.setRunning(b);
        objDetector.setRunning(b);
    }
}
