package soargroup.mobilesim.commands.controls;

import java.io.*;
import java.util.zip.*;
import java.util.*;

import april.jmat.*;
import april.jmat.geom.*;
import april.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.util.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import soargroup.mobilesim.lcmtypes.grid_map_t;
import soargroup.mobilesim.lcmtypes.diff_drive_t;
import soargroup.mobilesim.lcmtypes.robot_map_data_t;
import soargroup.mobilesim.lcmtypes.lcmdoubles_t;
import soargroup.mobilesim.lcmtypes.robot_command_t;
import soargroup.mobilesim.lcmtypes.robot_task_t;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.typed_value_t;

// XXX DEBUG
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import april.vis.*;

public class DriveToXY extends ControlLaw implements LCMSubscriber {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static java.util.List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			params.add(new TypedParameter("x", TypedValue.TYPE_DOUBLE, true));
			params.add(new TypedParameter("y", TypedValue.TYPE_DOUBLE, true));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    VisWorld vw;

    private boolean DEBUG = false;

    // I don't think we can hit this rate. CPU intensive?
    static final double HZ = 100;//10
    static final double LOOKAHEAD_X_M = 0.2;
    static final double TURN_IN_PLACE_RAD = Math.toRadians(90);
    static final double STOP_DIST = 0.25;

    // Stop when we are oscillating
    double alpha = 0.2;
    boolean turning = false;
    int signThen = -1;
    int switches = 0;

    // XXX Get this into config
    double WHEELBASE = 0.46;
    double WHEEL_DIAMETER = 0.25;
    double REAR_AXLE_OFFSET = 0.20;

    private PeriodicTasks tasks = new PeriodicTasks(1);
    private ExpiringMessageCache<pose_t> poseCache =
        new ExpiringMessageCache<pose_t>(.2);
    private ExpiringMessageCache<grid_map_t> gmCache =
        new ExpiringMessageCache<grid_map_t>(1.5);

    LCM lcm = LCM.getSingleton();
    String mapChannel = Util.getConfig().getString("robot.lcm.map_channel", "ROBOT_MAP_DATA");
    String poseChannel = Util.getConfig().getString("robot.lcm.pose_channel", "POSE");
    String l2gChannel = Util.getConfig().getString("robot.lcm.l2g_channel", "L2G");
    String driveChannel = Util.getConfig().getString("robot.lcm.drive_channel", "DIFF_DRIVE");

    boolean sim = false;

    // RobotDrive state tracking
    byte robotid = 3;
    static byte robotDriveID = 0;

    // The goal target as a global coordinate
    double[] globalXYT;
    double[] l2g = new double[3];
	protected final double targetX;
	protected final double targetY;

    // Alpha-beta filtering for history
    diff_drive_t lastDrive = new diff_drive_t();

    public DriveToXY(HashMap<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, DriveToXY.getParameters());

        String rid = System.getenv("ROBOT_ID");
        if (rid == null)
            rid = "3";
        mapChannel += "_"+rid;
        this.robotid = Byte.valueOf(rid);

		targetX = parameters.get("x").getDouble();
		targetY = parameters.get("y").getDouble();
        globalXYT = new double[] { targetX, targetY, 0.0 };

        if (parameters.containsKey("sim"))
            sim = true;

        if (parameters.containsKey("alpha"))
            alpha = parameters.get("alpha").getDouble();

        tasks.addFixedRate(new UpdateTask(), 1.0/HZ);

        if (DEBUG) {
            JFrame jf = new JFrame("Debug DriveXY");
            jf.setSize(400, 400);
            jf.setLayout(new BorderLayout());

            vw = new VisWorld();
            VisLayer vl = new VisLayer(vw);
            VisCanvas vc = new VisCanvas(vl);
            jf.add(vc);

            jf.setVisible(true);
        }
    }

	@Override
    public String getName() { return "DriveToXY"; }

	@Override
    public String toString() {
        return String.format("Drive to (%.2f,%.2f)", targetX, targetY);
    }

    /** Start/stop the execution of the control law.
     *
     *  @param run  True causes the control law to begin execution, false stops it
     **/
	@Override
    public void setRunning(boolean run) {
		if(run == is_running) return;
		super.setRunning(run);

        if (run) {
            lcm.subscribe(mapChannel, this);
            lcm.subscribe(poseChannel, this);
            lcm.subscribe(l2gChannel, this);
        } else {
            lcm.unsubscribe(mapChannel, this);
            lcm.unsubscribe(poseChannel, this);
            lcm.unsubscribe(l2gChannel, this);
        }
        tasks.setRunning(run);
        if (!run) {
            sendNullCmd();
        }
    }


    private class UpdateTask implements PeriodicTasks.Task
    {
        public void run(double dt)
        {
            pose_t pose = poseCache.get();
            if (pose == null) {
                return;
            }

            grid_map_t gm = gmCache.get();
            if (gm == null) {
                return;
            }

            DriveParams params = new DriveParams();
            params.pose = pose;
            params.gm = gm;
            diff_drive_t dd = drive(params);

            if (dd != null) {
                dd.utime = TimeUtil.utime();
                lcm.publish(driveChannel, dd);
            }
        }
    }

	@Override
    public diff_drive_t drive(DriveParams params)
    {
        if (sim) {
            return driveSim(params);
        } else {
            return driveReal(params);
        }
    }

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins) {
        try {
            messageReceivedEx(lcm, channel, ins);
        } catch (IOException ex) {
            System.err.println("WRN: Error receving message from channel " + channel + ": "+ex);
        }
    }

    synchronized void messageReceivedEx(LCM lcm, String channel,
            LCMDataInputStream ins) throws IOException
    {
        if (poseChannel.equals(channel)) {
            pose_t pose = new pose_t(ins);
            poseCache.put(pose, TimeUtil.utime());
        } else if (mapChannel.equals(channel)) {
            robot_map_data_t rmd = new robot_map_data_t(ins);
            gmCache.put((grid_map_t)rmd.gridmap.copy(), TimeUtil.utime());
        } else if (l2gChannel.equals(channel)) {
            lcmdoubles_t l2g_ = new lcmdoubles_t(ins);
            assert (l2g_.ndata == 3);
            synchronized (l2g) {
                System.arraycopy(l2g_.data, 0, l2g, 0, 3);
            }
        }
    }


    // Return true if we see a hazard in the map
    private boolean evaluatePath(GridMap gm, double[] xy0, double[] xy1)
    {
        // we'll microstep at 0.25 pixels. this isn't exact but it's pretty darn close.
        double stepsize = gm.metersPerPixel * 0.25;

        double dist = Math.sqrt(LinAlg.sq(xy0[0]-xy1[0]) +
                                LinAlg.sq(xy0[1]-xy1[1]));

        int nsteps = ((int) (dist / stepsize)) + 1;

        double cost = 0;

        for (int i = 0; i < nsteps; i++) {
            double alpha = ((double) i) / nsteps;
            double x = alpha*xy0[0] + (1-alpha)*xy1[0];
            double y = alpha*xy0[1] + (1-alpha)*xy1[1];

            int v = gm.getValue(x,y);
            if (v > 0)
                return true;
        }

        // normalize correctly for distance.
        return false;
    }

    synchronized public void sendNullCmd()
    {
        robot_command_t msg = new robot_command_t();
        msg.robotid = robotid;
        msg.task = new robot_task_t();
        msg.task.task = robot_task_t.FREEZE;
        msg.taskid = robotDriveID++;

        msg.ndparams = 0;
        msg.niparams = 0;

        lcm.publish("CMDS_STOP", msg);
        lcm.publish("CMDS_STOP", msg);
        lcm.publish("CMDS_STOP", msg);
    }

    /** Marshal data to robotDrive from magic2 */
    synchronized public diff_drive_t driveReal(DriveParams params)
    {
        if (!is_running)
            return null;

        // Convert global goal to robot local goal
        double[] goalXYT;
        synchronized (l2g) {
            goalXYT = LinAlg.xytInvMul31(l2g, globalXYT);
        }

        robot_command_t msg = new robot_command_t();
        //msg.utime = TimeUtil.utime();
        msg.robotid = robotid;
        msg.task = new robot_task_t();
        msg.task.task = robot_task_t.GOTO_GLOBAL;
        msg.taskid = robotDriveID++;

        msg.ndparams = 4;
        msg.dparams = new double[] {goalXYT[0],
                                    goalXYT[1],
                                    goalXYT[2],
                                    STOP_DIST};

        msg.niparams = 1;
        msg.iparams = new byte[] {0};

            lcm.publish("CMDS", msg);

        return null;
    }

    /** Get a drive command from the CL. */
    public diff_drive_t driveSim(DriveParams params)
    {
        diff_drive_t dd = new diff_drive_t();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = dd.right = 0.0;

        // Decode map data if necessary
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
        //grid_map_t map = new grid_map_t();
        //map.x0 = gm.x0;
        //map.y0 = gm.y0;
        //map.width = gm.width;
        //map.height = gm.height;
        //map.meters_per_pixel = gm.metersPerPixel;
        //map.datalen = gm.data.length;
        //map.data = gm.data;
        //lcm.publish("MAP DEBUG", map);

        double[] poseXYT = LinAlg.matrixToXYT(LinAlg.quatPosToMatrix(params.pose.orientation,
                                                                     params.pose.pos));
        poseXYT = LinAlg.xytMultiply(l2g, poseXYT);
        // Convert global goal to robot local goal
        double[] goalXYT = globalXYT;
        //synchronized (l2g) {
        //    goalXYT = LinAlg.xytInvMul31(l2g, globalXYT);
        //}

        // Medium drive speed
        double driveGain = 0.0;
        double turnGain = 3.0;          // 3.0 normally
        double straightGain = 1.2;//0.6; //1.2
        double collisionDistance = 0.4;
        double repulsiveStrength = 1.5;
        double repulsiveDistance = 2.0; // 2.0 normally
        double attractiveStrength = 1.0;
        double attractiveDistance = 1.0;
        double maxSpeed = 0.5;//0.3;
        double lookahead = LOOKAHEAD_X_M;


        if (sim) {
            driveGain = 0.0;
            repulsiveDistance = 2.0;
            turnGain = 2.0;
            maxSpeed = 0.5;//0.3;
            lookahead = 0.4;
        }

        double distToGoal = LinAlg.distance(poseXYT, goalXYT, 2);
        double angleToGoal = Math.atan2(goalXYT[1] - poseXYT[1],
                                        goalXYT[0] - poseXYT[0]);
        angleToGoal = MathUtil.mod2pi(angleToGoal - poseXYT[2]);

        // If sufficiently close to goal, stop
        if (0.8*distToGoal < lookahead)
            return dd;

        // If we need to turn in place, do so
        if (Math.abs(angleToGoal) > TURN_IN_PLACE_RAD) {
            dd.right = dd.left = maxSpeed;
            if (angleToGoal > 0)
                dd.left *= -1;
            else
                dd.right *= -1;
            return dd;
        }

        // Single lookahead point
        double scale = Math.min(1.0, 0.5*distToGoal/lookahead);
        double[] lookaheadTrans = new double[] { poseXYT[0], poseXYT[1] };
        lookaheadTrans[0] += scale*Math.cos(poseXYT[2])*lookahead;
        lookaheadTrans[1] += scale*Math.sin(poseXYT[2])*lookahead;
        //System.out.printf("[%f %f] -- [%f %f]\n", poseXYT[0], poseXYT[1], lookaheadTrans[0], lookaheadTrans[1]);

        // Compute gradients for point
        // First, check line of sign. If we don't have it, then
        // force our gradient back at the robot.
        double[] grad = new double[2];
        boolean flagCollision = false;
        if (evaluatePath(gm, poseXYT, lookaheadTrans)) {
            double angle = Math.atan2(poseXYT[1] - lookaheadTrans[1],
                                      poseXYT[0] - lookaheadTrans[0]);
            grad[0] += repulsiveStrength*Math.cos(angle);
            grad[1] += repulsiveStrength*Math.sin(angle);
        } else {
            // Attractive gradient
            double angle = Math.atan2(goalXYT[1] - lookaheadTrans[1],
                                      goalXYT[0] - lookaheadTrans[0]);
            double dist = LinAlg.distance(goalXYT, lookaheadTrans, 2);
            scale = Math.min(1.0, dist/attractiveDistance);
            grad[0] += scale*Math.cos(angle)*attractiveStrength;
            grad[1] += scale*Math.sin(angle)*attractiveStrength;

            // Repulsive gradient
            double x0 = lookaheadTrans[0] - repulsiveDistance;
            double y0 = lookaheadTrans[1] - repulsiveDistance;
            double x1 = lookaheadTrans[0] + repulsiveDistance;
            double y1 = lookaheadTrans[1] + repulsiveDistance;

            ArrayList<double[]> gradients = new ArrayList<double[]>();
            double[] data = new double[2];
            double esum = 0;
            double escale = 15;     // 15 normally
            if (sim)
                escale = 15;

            for (double y = y0; y <= y1; y += gm.metersPerPixel) {
                for (double x = x0; x <= x1; x += gm.metersPerPixel) {
                    int v = gm.getValue(x, y);
                    if (v == 0)
                        continue;

                    dist = Math.sqrt(LinAlg.sq(lookaheadTrans[0]-x) +
                                     LinAlg.sq(lookaheadTrans[1]-y));
                    data[1] = Math.atan2(lookaheadTrans[1]-y,
                                         lookaheadTrans[0]-x);
                    if (dist <= collisionDistance) {
                        flagCollision = true;
                        data[0] = 1.0;
                        gradients.add(LinAlg.copy(data));
                        esum += Math.exp(escale);
                    } else if (dist > collisionDistance &&
                               dist < repulsiveDistance) {
                        scale = (repulsiveDistance - dist)/(repulsiveDistance-collisionDistance);
                        assert (scale <= 1.0 && scale >= 0.0);
                        data[0] = scale;
                        gradients.add(LinAlg.copy(data));
                        esum += Math.exp(escale*scale);
                    }
                }
            }

            // Remove effects of attractive field
            if (flagCollision) {
                grad[0] = 0;
                grad[1] = 0;
            }

            // Softmax
            for (int i = 0; i < gradients.size(); i++) {
                double[] g = gradients.get(i);
                double w = Math.exp(escale*g[0])/esum;
                grad[0] += w*g[0]*Math.cos(g[1])*repulsiveStrength;
                grad[1] += w*g[0]*Math.sin(g[1])*repulsiveStrength;
            }
        }


        // Use this gradient to do something
        if (LinAlg.magnitude(grad) > 0) {
            double theta = Math.atan2(grad[1], grad[0]);
            double mag = LinAlg.magnitude(grad);

            double dtheta = theta - poseXYT[2];
            double drive = mag*Math.cos(dtheta);
            double turn = mag*Math.sin(dtheta);

            dd.left = driveGain + straightGain*drive - turnGain*turn;
            dd.right = driveGain + straightGain*drive + turnGain*turn;
        }

        // Scale to preserve proportions
        double max = Math.max(Math.abs(dd.left), Math.abs(dd.right));
        if (max > maxSpeed) {
            dd.left = (dd.left/max)*maxSpeed;
            dd.right = (dd.right/max)*maxSpeed;
        }

        // Clamping for safety.
        // XXX Weren't we doing something smarter than this before?
        dd.left = MathUtil.clamp(dd.left, -maxSpeed, maxSpeed);
        dd.right = MathUtil.clamp(dd.right, -maxSpeed, maxSpeed);

        turning = false;
        // Turn in place instead of going backwards
        if (dd.left < 0 && dd.right < 0) {
            if (dd.left > dd.right)
                dd.left = -dd.right;
            else
                dd.right = -dd.left;
            turning = true;
        }

        if (sim) {
            boolean nl = dd.left < 0;
            boolean nr = dd.right < 0;
            double diff = Math.abs(dd.right) - Math.abs(dd.left);
            if (diff > maxSpeed/2) {
                if (nr && Math.abs(dd.right) > Math.abs(dd.left))
                    dd.left = -dd.right;
                else if (nl && Math.abs(dd.left) > Math.abs(dd.right))
                    dd.right = -dd.left;
                turning = true;
            }
        }

        // Try to stop when we are wiggling
        if (turning) {
            int signNow = dd.left < dd.right ? 1 : -1;
            switches += signNow == signThen ? 0 : 1;
            signThen = signNow;
        } else {
            switches = 0;
        }

        if (switches >= 2) {
            dd.left = dd.right = 0;
        }

        // Alpha-beta filtering
        if (sim) {
            alpha = 1.0;
        }
        dd.left = dd.left*alpha + (1.0-alpha)*lastDrive.left;
        dd.right = dd.right*alpha + (1.0-alpha)*lastDrive.right;
        lastDrive = dd;

        return dd;
    }

    public control_law_t getLCM()
    {
        control_law_t cl = new control_law_t();
        cl.name = "drive-xy";
        cl.num_params = 5;
        cl.param_names = new String[cl.num_params];
        cl.param_values = new typed_value_t[cl.num_params];
        cl.param_names[0] = "x";
        cl.param_values[0] = (new TypedValue(globalXYT[0])).toLCM();
        cl.param_names[1] = "y";
        cl.param_values[1] = (new TypedValue(globalXYT[1])).toLCM();

        return cl;
    }

    static public void main(String[] args)
    {
        HashMap<String, TypedValue> params = new HashMap<String, TypedValue>();
        params.put("x", new TypedValue(new Double(args[0])));
        params.put("y", new TypedValue(new Double(args[1])));
        if (args.length > 2)
            params.put("distance", new TypedValue(new Double(args[2])));
        DriveToXY drive = new DriveToXY(params);
        drive.setRunning(true);
    }
}
