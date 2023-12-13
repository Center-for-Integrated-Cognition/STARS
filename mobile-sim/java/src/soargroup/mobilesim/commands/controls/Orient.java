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

/** Orient the robot to face along a particular heading. Automatically turns
 *  the most efficient direction.
 */
public class Orient extends ControlLaw implements LCMSubscriber
{
    /** Get the parameters that can be set for this control law.
     *
     *  @return An iterable, immutable collection of all possible parameters
     **/
	private static List<TypedParameter> parameters = null;
    public static Collection<TypedParameter> getParameters()
    {
		if(parameters == null){
			ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
			params.add(new TypedParameter("yaw",
										  TypedValue.TYPE_DOUBLE,
										  new TypedValue(-Math.PI),
										  new TypedValue(Math.PI),
										  true));
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    LCM lcm = LCM.getSingleton();

    static final double SPEED_THRESH_RANGE_RAD = Math.PI/2;
    static final double MAX_SPEED = 0.30;
    static final double MIN_SPEED = 0.45;
    static final double MIN_SIM_SPEED = 0;
    static final int HZ = 20;


    boolean sim = false;
    protected final double goalYaw;

    Object poseLock = new Object();
    pose_t lastPose = null;
    private double lastSpeed = 0;       // Ramp up/down

    private PeriodicTasks tasks = new PeriodicTasks(1);

    public Orient(HashMap<String, TypedValue> parameters) {
		super(parameters);
		ControlLaw.validateParameters(parameters, Orient.getParameters());

        goalYaw = parameters.get("yaw").getDouble();

        if (parameters.containsKey("sim"))
            sim = true;

        tasks.addFixedDelay(new OrientTask(), 1.0/HZ);
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
            // No-lcm?
            lcm.subscribe("POSE", this);
        } else {
            lcm.unsubscribe("POSE", this);
        }
        tasks.setRunning(run);
    }

	@Override
    public String getName() { return "Orient"; }

	@Override
    public String toString() {
        return String.format("Orient(%.2f rad)", goalYaw);
    }

    private class OrientTask implements PeriodicTasks.Task {
        public OrientTask() { }

        public void run(double dt)
        {
            DriveParams params = new DriveParams();
            params.dt = dt;
            synchronized (poseLock) {
                params.pose = lastPose;
            }
            diff_drive_t dd = drive(params);
            dd.utime = TimeUtil.utime();

            lcm.publish("DIFF_DRIVE", dd);
        }
    }

    /** Get a drive command from the CL. */
	@Override
    public diff_drive_t drive(DriveParams params)
    {
        diff_drive_t dd = new diff_drive_t();
        dd.left_enabled = dd.right_enabled = true;
        dd.left = 0;
        dd.right = 0;

        if (params.pose == null){
            return dd;
		}

        double yaw = LinAlg.quatPosToXYT(params.pose.orientation, params.pose.pos)[2];
		//System.out.println("CURRENT YAW: " + yaw);

        // We want to rotate toward the goal, but not overshoot.
        double dyaw = MathUtil.mod2pi(goalYaw - yaw);
		//System.out.println("YAW DIFF: " + dyaw);
        double minSpeed = sim ? MIN_SIM_SPEED : MIN_SPEED;
        double speed = MathUtil.clamp(MAX_SPEED*(Math.abs(dyaw) / SPEED_THRESH_RANGE_RAD),
                                      minSpeed,
                                      MAX_SPEED);

        // A little filtering
        speed = Math.min((speed + lastSpeed) / 2, MAX_SPEED);
        if (dyaw > 0) {
            // We need to go LEFT
            dd.left = -speed;
            dd.right = speed;
        } else {
            dd.left = speed;
            dd.right = -speed;
        }

        if (Math.abs(dyaw) < Math.toRadians(2))
            dd.left = dd.right = 0;

        lastSpeed = Math.abs(dd.left);


        return dd;
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
                lastPose = msg;
            }
        }
    }

    public control_law_t getLCM()
    {
        control_law_t cl = new control_law_t();
        cl.name = "orient";
        cl.num_params = 1;
        cl.param_names = new String[cl.num_params];
        cl.param_values = new typed_value_t[cl.num_params];
        cl.param_names[0] = "yaw";
        cl.param_values[0] = new TypedValue(goalYaw).toLCM();

        return cl;
    }
}
