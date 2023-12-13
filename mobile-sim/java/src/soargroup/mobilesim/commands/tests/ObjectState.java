package soargroup.mobilesim.commands.tests;

import java.io.*;
import java.util.*;

import april.jmat.*;

import soargroup.mobilesim.commands.*;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.condition_test_t;
import soargroup.mobilesim.lcmtypes.object_data_list_t;
import soargroup.mobilesim.lcmtypes.object_data_t;
import soargroup.mobilesim.lcmtypes.classification_t;

public class ObjectState implements ConditionTest, LCMSubscriber
{
    static final double DEFAULT_STOPPING_DISTANCE = 0.25;

    LCM lcm = LCM.getSingleton();

    int objectId = -1;
	String property = "";
	String value = "";

	boolean satisfied = false;

    public ObjectState()
    {
    }

    public ObjectState(HashMap<String, TypedValue> parameters)
    {
    	assert (parameters.containsKey("object-id"));
    	objectId = parameters.get("object-id").getInt();

    	assert (parameters.containsKey("property"));
    	property = parameters.get("property").toString();

    	assert (parameters.containsKey("value"));
    	value = parameters.get("value").toString();
    }

    public ConditionTest copyCondition()
    {
        ObjectState objState = new ObjectState();
        objState.objectId = objectId;
        objState.property = property;
        objState.value = value;
        return objState;
    }

    public void processDetectedObjects(object_data_list_t objDatas){
		for(object_data_t obj : objDatas.objects){
			if(obj.id == objectId){
				for(classification_t cls : obj.classifications){
					if(cls.category.equals(property) && cls.name.equals(value)){
						satisfied = true;
					}
				}
			}
		}
    }

    public void setRunning(boolean run)
    {
        if (run) {
            lcm.subscribe("DETECTED_OBJECTS", this);
        } else {
            lcm.unsubscribe("DETECTED_OBJECTS", this);
        }
    }


    public void messageReceived(LCM lcm, String channel, LCMDataInputStream ins)
    {
        try {
            if (channel.startsWith("DETECTED_OBJECTS")) {
            	object_data_list_t objDatas = new object_data_list_t(ins);
            	processDetectedObjects(objDatas);
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
		return satisfied;
    }

    /** Get the parameters that can be set for this condition test.
     *
     *  @return An iterable collection of all possible parameters.
     **/
    public Collection<TypedParameter> getParameters()
    {
        ArrayList<TypedParameter> params = new ArrayList<TypedParameter>();
        params.add(new TypedParameter("object-id", TypedValue.TYPE_INT, true));
        params.add(new TypedParameter("property", TypedValue.TYPE_STRING, true));
        params.add(new TypedParameter("value", TypedValue.TYPE_STRING, true));

        return params;
    }

    public String toString()
    {
        return String.format("PREDICATE_%s(%d)", value, objectId);
    }

    public condition_test_t getLCM()
    {
        condition_test_t ct = new condition_test_t();
        ct.name = "object-state";
        ct.num_params = 3;
        ct.param_names = new String[ct.num_params];
        ct.param_values = new typed_value_t[ct.num_params];
        ct.param_names[0] = "object-id";
        ct.param_values[0] = new TypedValue(objectId).toLCM();
        ct.param_names[1] = "property";
        ct.param_values[1] = new TypedValue(property).toLCM();
        ct.param_names[2] = "value";
        ct.param_values[2] = new TypedValue(value).toLCM();

        // Not used
        ct.compare_type = condition_test_t.CMP_GT;
        ct.compared_value = new TypedValue(0).toLCM();

        return ct;
    }
}
