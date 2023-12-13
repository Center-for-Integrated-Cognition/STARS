package soargroup.mobilesim.commands.controls;

import java.io.*;
import java.util.*;

import april.jmat.*;
import april.jmat.geom.*;
import april.util.*;

import soargroup.mobilesim.commands.*;
import soargroup.mobilesim.util.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class DriveForward extends ControlLaw implements LCMSubscriber
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
			parameters = Collections.unmodifiableList(params);
		}
		return parameters;
    }

    static final int DB_HZ = 100;
    static final double VERY_FAR = 3671000;     // Earth's radius [m]

    PathParams pathParams = PathParams.makeDefault();
    //GLineSegment2D path;
    ArrayList<double[]> path = null;

    // XXX This needs to change
    double centerOffsetX_m = Util.getConfig().requireDouble("robot.geometry.centerOffsetX_m");
    private ExpiringMessageCache<pose_t> poseCache = new ExpiringMessageCache<pose_t>(0.2);

    LCM lcm = LCM.getSingleton();

    PeriodicTasks tasks = new PeriodicTasks(1);

    public DriveForward(Map<String, TypedValue> parameters)
    {
		super(parameters);
		ControlLaw.validateParameters(parameters, DriveForward.getParameters());

        tasks.addFixedRate(new DriveTask(), 1.0/DB_HZ);
    }

	@Override
    public String getName() { return "DriveForward"; }

	@Override
	public String toString() { 
		return "DriveForward()";
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
            lcm.subscribe("POSE", this);
        } else {
            lcm.unsubscribe("POSE", this);
        }
        tasks.setRunning(run);
    }

    private class DriveTask implements PeriodicTasks.Task
    {

        public DriveTask()
        {
        }

        // Uses MAGIC path controller to issue drive commands to robot. These
        // seem to be stateless in this form, which means that if the
        // behavior is non-ideal, there's nothing done to correct it, but it
        // should result in approximately straight forward driving for now.
        public void run(double dt)
        {
            // Get the most recent position
            DriveParams params = new DriveParams();
            params.pose = poseCache.get();
            params.dt = dt;
            diff_drive_t dd = drive(params);

            publishDiff(dd);
        }
    }

	@Override
    public diff_drive_t drive(DriveParams params)
    {
        pose_t pose = params.pose;
        double dt = params.dt;

        if (path == null)
            init(pose);

        diff_drive_t dd = new diff_drive_t();
        dd.left_enabled = true;
        dd.right_enabled = true;
        dd.left = 0;
        dd.right = 0;

        // Get the most recent position
        if(pose == null)
            return dd;
        double offset[] = LinAlg.matrixAB(LinAlg.quatToMatrix(pose.orientation),
                                          new double[] {centerOffsetX_m, 0 , 0, 1});
        double center_pos[] = new double[]{pose.pos[0] + offset[0],
            pose.pos[1] + offset[1] };

        // Create and publish controls used by RobotDriver
        dd = PathControl.getDiffDrive(center_pos,
                                      pose.orientation,
                                      path,
                                      pathParams,
                                      0.5,
                                      dt);

        return dd;
    }

    private void init(pose_t initialPose)
    {
        if (initialPose == null)
            return;

        double[] rpy = LinAlg.quatToRollPitchYaw(initialPose.orientation);
        double goalX = VERY_FAR*Math.cos(rpy[2]);
        double goalY = VERY_FAR*Math.sin(rpy[2]);

        double[] start2D = LinAlg.resize(initialPose.pos, 2);
        double[] goal2D = new double[] {start2D[0]+goalX,
            start2D[1]+goalY};

        //path = new GLineSegment2D(start2D, goal2D); // XXX - update?
        path = new ArrayList<double[]>();
        path.add(start2D);
        path.add(goal2D);
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
        if (channel.equals("POSE")) {
            pose_t msg = new pose_t(ins);
            poseCache.put(msg, msg.utime);
        }
    }

    private static void publishDiff(diff_drive_t diff_drive)
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
