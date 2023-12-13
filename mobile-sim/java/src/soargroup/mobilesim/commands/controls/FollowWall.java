package soargroup.mobilesim.commands.controls;

import java.io.*;
import java.util.zip.*;
import java.util.*;

import april.jmat.*;
import april.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.util.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import april.lcmtypes.laser_t;
import soargroup.mobilesim.lcmtypes.grid_map_t;
import soargroup.mobilesim.lcmtypes.diff_drive_t;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.robot_map_data_t;

/** A not-very-principaled wall follower. Should be revisited later. Note:
 *  when it reaches a wall in front of it that forces a turn, the follower
 *  should STOP. This is more "conforming" in that the robot will 1) not turn
 *  around because a person got in the way and 2) will typically converge to
 *  the same place as time approaches infinity.
 **/
public class FollowWall extends ControlLaw implements LCMSubscriber {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters() {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			ArrayList<TypedValue> options = new ArrayList<TypedValue>();
			options.add(new TypedValue(-1));
			options.add(new TypedValue(1));
			params.add(new TypedParameter("side",
										  TypedValue.TYPE_INT,
										  options,
										  true));

			params.add(new TypedParameter("distance",
										  TypedValue.TYPE_DOUBLE,
										  false));  // Could have range limits...

			params.add(new TypedParameter("heading",
										  TypedValue.TYPE_DOUBLE,
										  new TypedValue(-Math.PI),
										  new TypedValue(Math.PI),
										  false));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    private static final double FW_HZ = 40;
    private static final double HEADING_THRESH = Math.toRadians(5.0);
    private static final double ROBOT_RAD = Util.getConfig().requireDouble("robot.geometry.radius");
    private double BACK_THETA = 16*Math.PI/36;
    private double FRONT_THETA = 10*Math.PI/36;
    private static final double MAX_RANGE_M = 10.0;
    private static final double MAX_V = 0.6;
    private static final double MIN_V = 0.4;

    LCM lcm = LCM.getSingleton();
    String laserChannel = Util.getConfig().getString("robot.lcm.laser_channel", "LASER");
    String poseChannel = Util.getConfig().getString("robot.lcm.pose_channel", "POSE");
    String mapChannel = Util.getConfig().getString("robot.lcm.map_channel", "ROBOT_MAP_DATA");
    String driveChannel = Util.getConfig().getString("robot.lcm.drive_channel", "DIFF_DRIVE");

    private PeriodicTasks tasks = new PeriodicTasks(1);
    private ExpiringMessageCache<laser_t> laserCache =
        new ExpiringMessageCache<laser_t>(.2);
    private ExpiringMessageCache<pose_t> poseCache =
        new ExpiringMessageCache<pose_t>(.2);
    private ExpiringMessageCache<grid_map_t> gmCache =
        new ExpiringMessageCache<grid_map_t>(1.5);

    protected final Direction dir;
    private enum Direction { LEFT, RIGHT }
    private double goalDistance = 0.6;
    private double targetHeading = Double.MAX_VALUE;

    // Task State
    boolean sim = false;
    boolean oriented = false;
    int startIdx = -1;
    int finIdx = -1;

    // State for PID
    double K_d = 0.05;
    //double K_d = 0.001;
    double lastRange = -1;
    double deriv = 0;

    public FollowWall(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, FollowWall.getParameters());

        //System.out.println("FOLLOW WALL");
        String rid = System.getenv("ROBOT_ID");
        if (rid == null)
            rid = "3";
        mapChannel += "_"+rid;

        if (parameters.get("side").getInt() < 0)
            dir = Direction.RIGHT;
        else
            dir = Direction.LEFT;

        if (parameters.containsKey("distance"))
            goalDistance = Math.abs(parameters.get("distance").getDouble());

        if (parameters.containsKey("heading"))
            targetHeading = parameters.get("heading").getDouble();

        if (parameters.containsKey("sim"))
            sim = true;

        if (sim) {
            FRONT_THETA = 8*Math.PI/36;
        }
        tasks.addFixedRate(new UpdateTask(), 1.0/FW_HZ);
    }

	@Override
    public String getName() { return "FollowWall"; }

    public String getSide() {
		return dir == Direction.LEFT ? "LEFT" : "RIGHT"; 
	}

    // Ignore heading for now
    public int hashCode() {
        return dir.hashCode() ^ new Double(goalDistance).hashCode();
    }

    public boolean equals(Object o) {
        if (o == null)
            return false;
        else if (!(o instanceof FollowWall))
            return false;
        FollowWall fw = (FollowWall)o;
        return dir == fw.dir && goalDistance == fw.goalDistance;
    }

	@Override
    public String toString() {
        return String.format("Follow %s wall @ %2.3f [m]", getSide(), goalDistance);
    }

    /** Start/stop the execution of the control law.
     *
     *  @param run  True causes the control law to begin execution, false stops it
     **/
	@Override
    public void setRunning(boolean run) {
		if(run == is_running){ return; }
		super.setRunning(run);

        if (run) {
            lcm.subscribe(laserChannel, this);
            lcm.subscribe(poseChannel, this);
            lcm.subscribe(mapChannel, this);
        } else {
            lcm.unsubscribe(laserChannel, this);
            lcm.unsubscribe(poseChannel, this);
            lcm.unsubscribe(mapChannel, this);
        }
        tasks.setRunning(run);
    }

    private class UpdateTask implements PeriodicTasks.Task
    {
        public void run(double dt)
        {
            laser_t laser = laserCache.get();
            //if (laser == null) {
            //    return;
            //}

            grid_map_t gm = gmCache.get();
            if (gm == null) {
                return;
            }

            pose_t pose = poseCache.get();
            if (pose == null) {
                return;
            }

            // Initialization step.
            // Find the laser beam we will observe for distance. If we are using
            // a 180 or greater degree sensor, this should be 90 degrees CW or
            // 90 degress CCW from the front of the robot, in all likelihood.
            // Otherwise, we choose the furthest beam out to that side and correct
            // our range measurements accordingly.
            if (startIdx < 0)
                init(laser);

            // Update the movement of the robot. Basically, swing towards wall
            // when range > desired, else swing back the other way. Some extra
            // logic to prevent running into walls.
            update(laser, pose, gm, dt);
        }
    }

    // Publicly exposed initializer, used in monte-carlo sim
    public void init(laser_t laser)
    {
        // Find indices at which to start/stop scanning
        switch (dir) {
            case LEFT:
                finIdx = MathUtil.clamp((int)((BACK_THETA - laser.rad0)/laser.radstep), 0, laser.nranges-1);
                startIdx = MathUtil.clamp((int)((FRONT_THETA - laser.rad0)/laser.radstep), 0, laser.nranges-1);
                break;
            case RIGHT:
                startIdx = MathUtil.clamp((int)((-BACK_THETA - laser.rad0)/laser.radstep), 0, laser.nranges-1);
                finIdx = MathUtil.clamp((int)((-FRONT_THETA - laser.rad0)/laser.radstep), 0, laser.nranges-1);
                break;
            default:
                assert (false);
                break;
        }

        assert (startIdx <= finIdx);
    }

    // P controller to do wall avoidance. More logic could be added...
    private void update(laser_t laser, pose_t pose, grid_map_t gm, double dt)
    {
        diff_drive_t dd;

        if (targetHeading == Double.MAX_VALUE)
            oriented = true;

        if (!oriented) {
            dd = orient(pose, targetHeading);
        } else {
            DriveParams params = new DriveParams();
            params.dt = dt;
            params.laser = laser;
            params.pose = pose;
            params.gm = gm;
            dd = drive(params);
        }

        dd.utime = TimeUtil.utime();
        lcm.publish(driveChannel, dd);
    }

    private diff_drive_t orient(pose_t pose, double heading)
    {
        double[] rpy = LinAlg.quatToRollPitchYaw(pose.orientation);

        diff_drive_t dd = new diff_drive_t();
        dd.utime = TimeUtil.utime();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = 0;
        dd.right = 0;

        double delta = MathUtil.mod2pi(rpy[2] - heading);

        if (Math.abs(delta) < HEADING_THRESH) {
            oriented = true;
        } else {
            dd.left = 0.7 * delta / Math.PI;
            dd.right = 0.7 * delta / -Math.PI;
        }

        // Friction sucks
        if (dd.left > 0) {
            dd.left = Math.max(dd.left, 0.1);
            dd.right = Math.min(dd.right, -0.1);
        } else {
            dd.left = Math.min(dd.left, -0.1);
            dd.right = Math.max(dd.right, 0.1);
        }

        return dd;
    }

    // Publicly exposed diff drive fn (can be used repeatedly as if static obj)
    // after initialized once
	@Override
    public synchronized diff_drive_t drive(DriveParams params)
    {
        if (false) {
            return handTunedFollow(params);
        } else {
            return xyFollow(params);
        }
    }

    private diff_drive_t xyFollow(DriveParams params)
    {
        diff_drive_t dd = new diff_drive_t();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = dd.right = 0;

        // Decode map data if necessary XXX To util
        byte[] gmdata;
        if (params.gm.encoding == grid_map_t.ENCODING_GZIP) {
            try {
                ByteArrayInputStream bytes = new ByteArrayInputStream(params.gm.data);
                GZIPInputStream gzis = new GZIPInputStream(bytes);

                gmdata = new byte[params.gm.width*params.gm.height];
                int readSoFar = 0;
                while (readSoFar < gmdata.length) {
                    int read = gzis.read(gmdata, readSoFar, gmdata.length-readSoFar);
                    if (read < 0)
                        break;
                    readSoFar += read;
                }
                gzis.close();
            } catch (ZipException ex) {
                System.err.println("ERR: GZIP'd map data is corrupted");
                ex.printStackTrace();
                return dd;
            }
            catch (IOException ex) {
                System.err.println("ERR: Could not extract GZIP'd map");
                ex.printStackTrace();
                return dd;
            }
        } else {
            gmdata = params.gm.data;
        }

        GridMap gm = GridMap.makePixels(params.gm.x0,
                                        params.gm.y0,
                                        params.gm.width,
                                        params.gm.height,
                                        params.gm.meters_per_pixel,
                                        0,
                                        gmdata);

        double[] poseXYT = LinAlg.matrixToXYT(LinAlg.quatPosToMatrix(params.pose.orientation,
                                                                     params.pose.pos));

        // Look for points in front of the robot
        double rWidth = 0.225;
        double hazardDist = Math.max(goalDistance, 0.5);
        if (sim)
            hazardDist = 1.25;
        double rc = Math.cos(poseXYT[2]);
        double rs = Math.sin(poseXYT[2]);
        for (double y = -rWidth; y < rWidth; y += gm.metersPerPixel/2) {
            for (double x = 0; x < hazardDist; x += gm.metersPerPixel/2) {
                double xbot = poseXYT[0] + rc*x - rs*y;
                double ybot = poseXYT[1] + rs*x + rc*y;
                int v = gm.getValue(xbot, ybot);
                if (v > 0) {
                    return dd;
                }
            }
        }

        // Ray casting to obstacles
        int sign = dir == Direction.LEFT ? 1 : -1;
        double t0 = Math.min(sign*BACK_THETA, sign*FRONT_THETA);
        double t1 = Math.max(sign*BACK_THETA, sign*FRONT_THETA);
        double tstep = Math.toRadians(0.25);
        double rSide = MAX_RANGE_M;
        double dt = params.dt;

        for (double t = t0; t < t1; t += tstep) {
            for (double r = 0; r < MAX_RANGE_M; r += gm.metersPerPixel/2) {
                double x = poseXYT[0] + r*Math.cos(t+poseXYT[2]);
                double y = poseXYT[1] + r*Math.sin(t+poseXYT[2]);
                int v = gm.getValue(x, y);
                // Support for new map values.
                if (v > 0) {
                    double w = MathUtil.clamp(Math.abs(Math.sin(t)),
                                              Math.sin(Math.PI/6),
                                              1.0);
                    rSide = Math.min(w*r, rSide);
                }
            }
        }

        double alpha0 = 0.0;
        if (lastRange > 0)
            rSide = alpha0*lastRange + (1.0-alpha0)*rSide;
        double alpha1 = 0.5;
        if (lastRange > 0)
            deriv = deriv*alpha1 + (1.0-alpha1)*(rSide - lastRange)/dt;
        lastRange = rSide;

        // Hand tuned controller
        // Normalize distance
        double G_weight = Math.pow(goalDistance, 1.0);
        double rSideNorm = rSide-G_weight;
        int goalSign = rSideNorm > 0 ? 1 : -1;
        double K_p = -MathUtil.clamp(goalSign*Math.pow(Math.abs(rSideNorm)/G_weight, 1.0/2), -1, 1);
        //double K_p = 1.0 - Math.pow(rSide/G_weight, 0.5);
        if (!sim) {
            //K_p *= 5.0;
            K_p *= 1.0;
            K_d = 0.1;
        } else {
            K_d = 0.5;
        }
        double prop = MathUtil.clamp(0.5 + K_p, -1.0, 1.0);//0.65);
        //double prop = 0.5 + K_p;

        double farSpeed = 0.3 - (prop - K_d*deriv);
        double nearSpeed = 0.3 + (prop - K_d*deriv);
        //double nearSpeed = MathUtil.clamp(prop - K_d*deriv, -1.0, 1.0);
        //double nearSpeed = MathUtil.clamp(prop - K_d*deriv, 0, 1.0);
        //double nearSpeed = prop - K_d*deriv;
        double max = Math.max(Math.abs(nearSpeed), Math.abs(farSpeed));
        if (max < 0.01) {
            nearSpeed = farSpeed = 0;
        } else {
            nearSpeed = MAX_V*nearSpeed/max;
            farSpeed = MAX_V*farSpeed/max;
        }

        switch (dir) {
            case LEFT:
                dd.left = nearSpeed;
                dd.right = farSpeed;
                break;
            case RIGHT:
                dd.right = nearSpeed;
                dd.left = farSpeed;
                break;
        }

        //System.out.printf("{%f %f}\n", K_p, K_d*deriv);
        //System.out.printf("[%f %f] [%f %f]\n", nearSpeed, farSpeed, dd.left, dd.right);
        return dd;
    }

    private diff_drive_t handTunedFollow(DriveParams params)
    {
        laser_t laser = params.laser;
        double dt = params.dt;

        if (startIdx < 0)
            init(laser);

        double[] xAxis = new double[] {1.0, 0};
        double[] yAxis = new double[] {0, 1.0};

        // Initialize no-speed diff drive
        diff_drive_t dd = new diff_drive_t();
        dd.utime = TimeUtil.utime();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = 0;
        dd.right = 0;

        double r = Double.MAX_VALUE;
        double rFront = Double.MAX_VALUE;
        for (int i = startIdx; i <= finIdx; i++) {
            // Handle error states
            if (laser.ranges[i] < 0)
                continue;

            double t = laser.rad0 + laser.radstep*i;
            double w = MathUtil.clamp(Math.abs(Math.sin(t)),
                                      Math.sin(Math.PI/6),
                                      1.0);
            r = Math.min(r, w*laser.ranges[i]);
        }

        for (int i = 0; i < laser.nranges; i++) {
            // Handle error states
            if (laser.ranges[i] < 0)
                continue;

            double t = laser.rad0 + laser.radstep*i;

            // Points in front
            double[] xy = new double[] {laser.ranges[i]*Math.cos(t),
                                        laser.ranges[i]*Math.sin(t)};
            double width = Math.abs(LinAlg.dotProduct(xy, yAxis));
            if (width > 0.3)
                continue;

            double dist = LinAlg.dotProduct(xy, xAxis);
            if (dist > 0.1) {
                rFront = Math.min(rFront, dist);
            }
        }

        double deriv = 0;
        if (lastRange > 0)
            deriv = (r - lastRange)/dt; // [m/s] of change
        lastRange = r;

        // XXX
        double G_weight = Math.pow(goalDistance, .5);
        double K_p = (1.0 - r/G_weight);
        if (sim)
            K_p *= 4.0;
        double prop = MathUtil.clamp(0.5 + K_p, -1.0, 1.0);//0.65);

        //double nearSpeed = 0.5;
        //double farSpeed = MathUtil.clamp(prop + K_d*deriv, -1.0, 1.0);
        double farSpeed = 0.5;
        double nearSpeed = MathUtil.clamp(prop - K_d*deriv, 0.0, 1.0);    // XXX
        double max = Math.max(Math.abs(nearSpeed), Math.abs(farSpeed));
        if (max < 0.01) {
            nearSpeed = farSpeed = 0;
        } else {
            nearSpeed = MAX_V*nearSpeed/max;
            farSpeed = MAX_V*farSpeed/max;
        }

        // Special case turn-in-place when near a dead end in front
        // XXX Now this is a STOP case
        //System.out.printf("%f\n", rFront);
        if (rFront < goalDistance + 0.25) {
            //nearSpeed = MIN_V;
            //farSpeed = -MIN_V;
            nearSpeed = farSpeed = 0;
        }


        switch (dir) {
            case LEFT:
                dd.left = nearSpeed;
                dd.right = farSpeed;
                break;
            case RIGHT:
                dd.right = nearSpeed;
                dd.left = farSpeed;
                break;
        }

        return dd;
    }

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            messageReceivedEx(lcm, channel, ins);
        } catch (IOException ex) {
            System.err.println("WRN: Error receving message from channel " + channel + ": "+ex);
        }
    }

    synchronized void messageReceivedEx(LCM lcm, String channel,
            LCMDataInputStream ins) throws IOException
    {
        if (laserChannel.equals(channel)) {
            laser_t laser = new laser_t(ins);
            laserCache.put(laser, laser.utime);
        } else if (poseChannel.equals(channel)) {
            pose_t pose = new pose_t(ins);
            poseCache.put(pose, pose.utime);
        } else if (mapChannel.equals(channel)) {
            robot_map_data_t rmd = new robot_map_data_t(ins);
            gmCache.put(rmd.gridmap, rmd.utime);
        }
    }

    public control_law_t getLCM()
    {
        control_law_t cl = new control_law_t();
        cl.name = "follow-wall";
        cl.num_params = 3;
        cl.param_names = new String[cl.num_params];
        cl.param_values = new typed_value_t[cl.num_params];
        cl.param_names[0] = "side";
        cl.param_values[0] = new TypedValue((dir == Direction.LEFT ? 1 : -1)).toLCM();
        cl.param_names[1] = "distance";
        cl.param_values[1] = new TypedValue(goalDistance).toLCM();
        cl.param_names[2] = "heading";
        cl.param_values[2] = new TypedValue(targetHeading).toLCM();

        return cl;
    }
}
