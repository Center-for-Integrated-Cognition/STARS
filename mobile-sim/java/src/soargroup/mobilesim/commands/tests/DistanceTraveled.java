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

/** Detects whether the robot has traveled a certain distance from the start. 
 **/
public class DistanceTraveled implements ConditionTest, LCMSubscriber
{
    LCM lcm = LCM.getSingleton();

	double[] firstPose = null;
	double curDistance = 0.0;
	double threshold = 1.0;

    public DistanceTraveled() {}

    public DistanceTraveled(HashMap<String, TypedValue> parameters)
    {
    	assert (parameters.containsKey("meters"));
		threshold = parameters.get("meters").getDouble();
    }


    /** Activate or turn off this condition test */
    public void setRunning(boolean run)
    {
        if (run) {
            // XXX LCM Subscription explosion
            lcm.subscribe("POSE", this);
			firstPose = null;
			curDistance = 0.0;
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
        double[] xyt = LinAlg.quatPosToXYT(pose.orientation, pose.pos);
        if (firstPose == null) {
			firstPose = xyt;
        }

		curDistance = LinAlg.distance(xyt, firstPose, 2);
    }

    /** Query whether or not the condition being tested for is currently true.
     *
     *  @return True if condition test is currently satisfied, else false
     **/
    public boolean conditionMet()
    {
		if(firstPose == null){
			return false;
		}
		return curDistance >= threshold;
    }

    /** Get the parameters that can be set for this condition test.
     *
     *  @return An iterable collection of all possible parameters.
     **/
    public Collection<TypedParameter> getParameters()
    {
        ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
        params.add(new TypedParameter("meters",
                                      TypedValue.TYPE_DOUBLE,
                                      true));

        return params;
    }

    public String toString()
    {
        return "DISTANCE_TRAVELED";
    }

    public ConditionTest copyCondition()
    {
        DistanceTraveled test =  new DistanceTraveled();
        test.lcm.subscribe("POSE", test); // XXX

        return test;
    }

    public condition_test_t getLCM()
    {
        condition_test_t ct = new condition_test_t();
        ct.name = "distance-traveled";
        ct.num_params = 1;
        ct.param_names = new String[ct.num_params];
        ct.param_values = new typed_value_t[ct.num_params];
        ct.param_names[0] = "meters";
        ct.param_values[0] = new TypedValue(threshold).toLCM();

        // Not used
        ct.compare_type = condition_test_t.CMP_GT;
        ct.compared_value = new TypedValue(0).toLCM();

        return ct;
    }
}
