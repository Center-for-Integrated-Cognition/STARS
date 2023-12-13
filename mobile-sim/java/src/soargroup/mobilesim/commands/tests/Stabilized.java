package soargroup.mobilesim.commands.tests;

import java.io.*;
import java.util.*;

import april.jmat.*;
import april.util.*;

import soargroup.mobilesim.commands.*;

// LCM Types
import lcm.lcm.*;
import april.lcmtypes.pose_t;
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.condition_test_t;

/** Detects whether or not a system has basically ceased moving. In
 *  other words, it has "stabilized." For now, no parameters, just
 *  built in thresholds!
 **/
public class Stabilized implements ConditionTest, LCMSubscriber
{
    LCM lcm = LCM.getSingleton();
    private double PERIOD_S = 1.0;
    static final double THETA_THRESH_RAD = Math.toRadians(3);
    static final double DIST_THRESH_M = 0.10;

    boolean begun = false;
    long startTime = 0;
    LinkedList<pose_t> poseQueue = new LinkedList<pose_t>();

    public Stabilized() {}

    public Stabilized(HashMap<String, TypedValue> parameters)
    {
        if (parameters.containsKey("timeout"))
            PERIOD_S = Math.abs(parameters.get("timeout").getDouble());
        else
            PERIOD_S = 1.0;
    }

    /** Activate or turn off this condition test */
    public void setRunning(boolean run)
    {
        if (run) {
            // XXX LCM Subscription explosion
            lcm.subscribe("POSE", this);
            begun = false;
        } else {
            lcm.unsubscribe("POSE", this);
        }
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
            update(msg);
        }
    }

    // Add a new pose
    public void update(pose_t pose)
    {
        assert (pose != null);
        if (!begun) {
            begun = true;
            startTime = pose.utime;
        }

        synchronized(poseQueue) {
            while (poseQueue.size() > 1) {
                pose_t front = poseQueue.peek();
                if ((pose.utime - front.utime) < PERIOD_S * 1000000)
                    break;
                poseQueue.poll();
            }
            poseQueue.add(pose);
        }
    }

    /** Query whether or not the condition being tested for is currently true.
     *
     *  @return True if condition test is currently satisfied, else false
     **/
    public boolean conditionMet()
    {
        if (!begun || poseQueue.size() < 2)
            return false;
        synchronized (poseQueue) {
            pose_t last = poseQueue.peekLast();
            if (last.utime - startTime < PERIOD_S * 1000000)
                return false;
        }

        double minx = Double.POSITIVE_INFINITY;
        double maxx = Double.NEGATIVE_INFINITY;
        double miny = Double.POSITIVE_INFINITY;
        double maxy = Double.NEGATIVE_INFINITY;
        double mint = Double.POSITIVE_INFINITY;
        double maxt = Double.NEGATIVE_INFINITY;
        double avgt = 0;

        synchronized (poseQueue) {
            for (pose_t pose: poseQueue) {
                double[] xyt = LinAlg.quatPosToXYT(pose.orientation, pose.pos);
                minx = Math.min(minx, xyt[0]);
                maxx = Math.max(maxx, xyt[0]);
                miny = Math.min(miny, xyt[1]);
                maxy = Math.max(maxy, xyt[1]);
                mint = Math.min(mint, MathUtil.mod2pi(xyt[2]) + Math.PI);
                maxt = Math.max(maxt, MathUtil.mod2pi(xyt[2]) + Math.PI);
            }
        }
        double dx = maxx - minx;
        double dy = maxy - miny;
        double dt = maxt - mint;


        boolean ret = Math.sqrt(dx*dx + dy*dy) < DIST_THRESH_M;
        ret = ret && dt < THETA_THRESH_RAD;

        return ret;
    }

    /** Get the parameters that can be set for this condition test.
     *
     *  @return An iterable collection of all possible parameters.
     **/
    public Collection<TypedParameter> getParameters()
    {
        ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
        params.add(new TypedParameter("timeout",
                                      TypedValue.TYPE_DOUBLE,
                                      false));

        return params;
    }

    public String toString()
    {
        return "STABILIZED";
    }

    public ConditionTest copyCondition()
    {
        Stabilized test =  new Stabilized();
        test.lcm.subscribe("POSE", test); // XXX

        return test;
    }

    public condition_test_t getLCM()
    {
        condition_test_t ct = new condition_test_t();
        ct.name = "stabilized";
        ct.num_params = 1;
        ct.param_names = new String[ct.num_params];
        ct.param_values = new typed_value_t[ct.num_params];
        ct.param_names[0] = "timeout";
        ct.param_values[0] = new TypedValue(PERIOD_S).toLCM();

        // Not used
        ct.compare_type = condition_test_t.CMP_GT;
        ct.compared_value = new TypedValue(0).toLCM();

        return ct;
    }
}
