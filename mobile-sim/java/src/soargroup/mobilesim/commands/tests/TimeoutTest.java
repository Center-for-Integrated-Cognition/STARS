package soargroup.mobilesim.commands.tests;

import java.util.*;

import april.util.*;

import soargroup.mobilesim.commands.*;

// LCM Types
import soargroup.mobilesim.lcmtypes.typed_value_t;
import soargroup.mobilesim.lcmtypes.condition_test_t;


// XXX IS this broken when not executed immediately? It would seem so, since
// elapsed is initialized on creation.
public class TimeoutTest implements ConditionTest
{
    double timeSofar = 0;
    double timeout;
    Tic elapsed;

    /** Strictly for use with parameter checking */
    public TimeoutTest()
    {
    }

    public TimeoutTest(HashMap<String, TypedValue> parameters)
    {
        assert (parameters.containsKey("timeout"));
        timeout = parameters.get("timeout").getDouble();
        elapsed = new Tic();
    }

    public void setRunning(boolean run)
    {
        if (run) {
            elapsed = new Tic();
        } else {
            timeSofar += elapsed.toctic();
        }
    }

    /** Query whether or not the condition being tested for is currently true.
     *
     *  @return True if condition test is currently satisfied, else false
     **/
    public boolean conditionMet()
    {
        return timeSofar+elapsed.toc() > timeout;
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
                                      true)); // Timeout [s]

        return params;
    }

    public ConditionTest copyCondition()
    {
        TimeoutTest tt = new TimeoutTest();
        tt.timeout = timeout;
        tt.elapsed = new Tic();

        return tt;
    }

    public condition_test_t getLCM()
    {
        condition_test_t ct = new condition_test_t();
        ct.name = "timeout";
        ct.num_params = 1;
        ct.param_names = new String[ct.num_params];
        ct.param_values = new typed_value_t[ct.num_params];
        ct.param_names[0] = "timeout";
        ct.param_values[0] = new TypedValue(timeout).toLCM();

        // Not used
        ct.compare_type = condition_test_t.CMP_GT;
        ct.compared_value = new TypedValue(0).toLCM();

        return ct;
    }
}
