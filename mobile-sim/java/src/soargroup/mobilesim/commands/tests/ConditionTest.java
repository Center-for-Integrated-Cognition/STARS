package soargroup.mobilesim.commands.tests;

import java.util.*;

import soargroup.mobilesim.commands.*;

public interface ConditionTest
{
    /** Activate or turn off this condition test */
    public void setRunning(boolean run);

    /** Query whether or not the condition being tested for is currently true.
     *
     *  @return True if condition test is currently satisfied, else false
     **/
    public boolean conditionMet();

    /** Get the parameters that can be set for this condition test.
     *
     *  @return An iterable collection of all possible parameters.
     **/
    public Collection<TypedParameter> getParameters();

    public String toString();

    public ConditionTest copyCondition();    // Like clone
}
