package soargroup.mobilesim.commands.tests;

import java.io.*;
import java.util.*;

import april.jmat.*;

import soargroup.mobilesim.commands.*;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.robot_info_t;
import soargroup.mobilesim.lcmtypes.condition_test_t;

public class Holding implements ConditionTest, LCMSubscriber
{
    static final double DEFAULT_STOPPING_DISTANCE = 0.25;

    LCM lcm = LCM.getSingleton();

    int objectId = -1;
    boolean held = false;
    
    boolean negated = false;

    public Holding()
    {
    }

    public Holding(HashMap<String, TypedValue> parameters)
    {
    	assert (parameters.containsKey("object-id"));
    	objectId = parameters.get("object-id").getInt();
    	
    	if(parameters.containsKey("negated")){
    		held = true;
    		negated = parameters.get("negated").getBoolean();
    	}
    }

    public ConditionTest copyCondition()
    {
        Holding hold = new Holding();
        hold.objectId = objectId;
        hold.negated = negated;
        return hold;
    }

    public void processRobotInfo(robot_info_t info)
    {
    	if(objectId == info.held_object){
    		held = true;
    	} else {
    		held = false;
    	}
    }

    public void setRunning(boolean run)
    {
        if (run) {
            lcm.subscribe("ROBOT_INFO", this);
        } else {
            lcm.unsubscribe("ROBOT_INFO", this);
        }
    }


    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            if (channel.startsWith("ROBOT_INFO")) {
            	robot_info_t info = new robot_info_t(ins);
            	processRobotInfo(info);
            }
        } catch (IOException ex) {
            System.err.println("ERR: Could not handle message on channel - "+channel);
            ex.printStackTrace();
        }
    }

    /** Query whether or not the condition being tested for is currently true.
     *
     *  @return True if condition test is currently satisfied, else false
     **/
    public boolean conditionMet()
    {
    	if(negated){
    		return !held;
    	} else {
    		return held;
    	}
    }

    /** Get the parameters that can be set for this condition test.
     *
     *  @return An iterable collection of all possible parameters.
     **/
    public Collection<TypedParameter> getParameters()
    {
        ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
        params.add(new TypedParameter("object-id",
                                      TypedValue.TYPE_INT,
                                      true));
        params.add(new TypedParameter("negated",
                                      TypedValue.TYPE_BOOLEAN,
                                      false));

        return params;
    }

    public String toString()
    {
        return String.format("HOLDING(%d)", objectId);
    }

    public condition_test_t getLCM()
    {
        condition_test_t ct = new condition_test_t();
        ct.name = "holding";
        ct.num_params = 2;
        ct.param_names = new String[ct.num_params];
        ct.param_values = new typed_value_t[ct.num_params];
        ct.param_names[0] = "object-id";
        ct.param_values[0] = new TypedValue(objectId).toLCM();
        ct.param_names[1] = "negated-handle";
        ct.param_values[1] = new TypedValue(negated).toLCM();

        // Not used
        ct.compare_type = condition_test_t.CMP_GT;
        ct.compared_value = new TypedValue(0).toLCM();

        return ct;
    }
}
