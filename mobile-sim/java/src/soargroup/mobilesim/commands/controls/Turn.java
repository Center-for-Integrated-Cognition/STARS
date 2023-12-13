package soargroup.mobilesim.commands.controls;

import java.io.*;
import java.util.*;

import april.util.*;
import april.jmat.*;

import soargroup.mobilesim.commands.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import soargroup.mobilesim.lcmtypes.diff_drive_t;
import soargroup.mobilesim.lcmtypes.control_law_t;
import soargroup.mobilesim.lcmtypes.typed_value_t;

public class Turn extends ControlLaw implements LCMSubscriber {
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			ArrayList<TypedValue> options = new ArrayList<TypedValue>();
			options.add(new TypedValue(-1));
			options.add(new TypedValue(1));
			params.add(new TypedParameter("direction", TypedValue.TYPE_INT, options, false));
			params.add(new TypedParameter("yaw", TypedValue.TYPE_DOUBLE, false));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    LCM lcm = LCM.getSingleton();

    static final int DD_HZ = 10;
    static final double MAX_SPEED = 0.5;
    static final double MIN_SPEED = 0.3;
    static final double SIM_SPEED = 0.05; 

    Object poseLock = new Object();
    pose_t currPose = null;
    pose_t lastPose = null;

    boolean sim = false;

    private boolean started = false;
    private double startYaw;
    private double prevYaw;
    private double accYaw;

    private double goalYaw = Double.MAX_VALUE;
	enum Direction { LEFT, RIGHT };
	final Direction dir;

    private PeriodicTasks tasks = new PeriodicTasks(1);

    public Turn(Map<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, Turn.getParameters());

        // Needs a direction to turn, currently
        if (parameters.containsKey("direction")){
        	int direction = parameters.get("direction").getInt();
            if (direction > 0) // CW
                dir = Direction.LEFT;
            else // CCW
                dir = Direction.RIGHT;
        } else {
        	dir = Direction.LEFT;
        }

        if (parameters.containsKey("yaw")) {
            int sign = (dir == Direction.LEFT) ? 1 : -1;
            goalYaw = sign*Math.abs(parameters.get("yaw").getDouble());
        }

        if (parameters.containsKey("sim"))
            sim = true;

        tasks.addFixedDelay(new TurnTask(), 1.0/DD_HZ);
    }

	@Override
    public String getName() { return "Turn"; }

	@Override
    public String toString() {
        return String.format("Turn(%s)", dir == Direction.LEFT ? "LEFT" : "RIGHT");
    }

    /** Start/stop the execution of the control law.
     *
     *  @param run  True causes the control law to begin execution, false stops it
     **/
	@Override
    public void setRunning(boolean run) {
		if (run == is_running) return;
		super.setRunning(run);

        if (run) {
            // no-lcm?
            lcm.subscribe("POSE", this);
        } else {
            lcm.unsubscribe("POSE", this);
        }
        tasks.setRunning(run);
    }

    private class TurnTask implements PeriodicTasks.Task
    {
        public TurnTask()
        {
            //System.out.println("Turn ready to execute");
        }

        public void run(double dt)
        {
            synchronized (poseLock) {
                DriveParams params = new DriveParams();
                params.dt = dt;
                params.pose = currPose;
                diff_drive_t dd = drive(params);
                publishDiff(dd);
            }
        }
    }

	@Override
    public diff_drive_t drive(DriveParams params)
    {
        double dt = params.dt;

        // Initialize diff drive with no movement
        diff_drive_t dd = new diff_drive_t();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = 0;
        dd.right = 0;

        if (params.pose == null)
            return dd;

        double[] rpy =  LinAlg.quatToRollPitchYaw(params.pose.orientation);

        if (!started) {
            startYaw = rpy[2];
            prevYaw = rpy[2];
            started = true;
        }

        accYaw += MathUtil.mod2pi(rpy[2] - prevYaw);
        prevYaw = rpy[2];

        // Determine turn strength
        double dtheta = goalYaw - accYaw;
        dtheta = MathUtil.clamp(dtheta, -Math.PI/2, Math.PI/2);

        double st = Math.sin(dtheta);
        int sign = st >= 0 ? 1 : -1;
        double signal = Math.max(MIN_SPEED, Math.abs(st)*MAX_SPEED);
        if (sim)
            signal = SIM_SPEED;

        if (Math.abs(dtheta) > Math.toRadians(3)) {
            dd.left = -sign*signal;
            dd.right = sign*signal;
        }

        return dd;
    }

    public void reset()
    {
        currPose = null;
        lastPose = null;
        accYaw = 0;
    }

    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            messageReceivedEx(lcm, channel, ins);
        } catch (IOException ex) {
            System.err.println("WRN: Error reading channel "+channel+": "+ex);
        }
    }

    synchronized void messageReceivedEx(LCM lcm, String channel,
            LCMDataInputStream ins) throws IOException
    {
        if (channel.equals("POSE")) {
            pose_t msg = new pose_t(ins);
            synchronized (poseLock) {
                currPose = msg;
            }
        }
    }

    public control_law_t getLCM()
    {
        control_law_t cl = new control_law_t();
        cl.name = "turn";
        cl.num_params = 2;
        cl.param_names = new String[cl.num_params];
        cl.param_values = new typed_value_t[cl.num_params];
        cl.param_names[0] = "direction";
        cl.param_values[0] = new TypedValue(dir == Direction.LEFT ? 1 : -1).toLCM();
        cl.param_names[1] = "yaw";
        cl.param_values[1] = new TypedValue(goalYaw).toLCM();

        return cl;
    }

    // XXX publishDiff is in two classes - can we consolidate?
    static private void publishDiff(diff_drive_t diff_drive)
    {
        // We may get a null if there are no poses yet
        // We should throw a WRN elsewhere if that is the case
        if (diff_drive == null)
            return;

        assert(diff_drive.left <= 1 && diff_drive.left >= -1);
        assert(diff_drive.right <= 1 && diff_drive.right >= -1);

        diff_drive.utime = TimeUtil.utime();
        LCM.getSingleton().publish("DIFF_DRIVE", diff_drive);
    }
}
