package soargroup.mobilesim.commands.tests;

import java.util.*;
import java.lang.reflect.*;

import soargroup.mobilesim.commands.*;

public class ConditionTestFactory
{
    HashMap<String, String> testMap = new HashMap<String, String>();

    static ConditionTestFactory singleton = null;
    static public ConditionTestFactory getSingleton()
    {
        if (singleton == null)
            singleton = new ConditionTestFactory();
        return singleton;
    }

    private ConditionTestFactory()
    {
        init();
    }

    // XXX Could move this to a config
    /** Initializes the factory on first use. This is where the default
     *  registrations are added.
     **/
    private void init()
    {
        registerConditionTest("timeout", TimeoutTest.class.getName());
        registerConditionTest("stabilized", Stabilized.class.getName());
        registerConditionTest("holding", Holding.class.getName());
        registerConditionTest("object-state", ObjectState.class.getName());
        registerConditionTest("distance-traveled", DistanceTraveled.class.getName());
    }

    /** Register condition tests with the factory. It is the job of the
     *  writer to ensure that provided classes implement the appropriate
     *  interface. Reflection is used to construct an instance of the
     *  appropriate class.
     *
     *  @param name         The name provided by the external system
     *  @param classname    The given name of the class.
     *
     **/
    synchronized public void registerConditionTest(String name, String classname)
    {
        testMap.put(name, classname);
    }

    /** Unregister a condition test. Returns true/false if test was
     *  successfully removed/no matching key was found.
     *
     *  @param name         The name of the test to be removed
     *
     *  @return True if test successfully removed, else false
     **/
    synchronized public boolean unregisterControllaw(String name)
    {
        String val = testMap.remove(name);
        return (val != null);
    }

    /** Construct a condition test from an associated set of parameters.
     *
     *  @param name         Registered name of test
     *  @param parameters   Input parameters for construction
     *
     *  @return A ConditionTest object that a robot may execute
     **/
	synchronized public ConditionTest construct(String name, Map<String, TypedValue> parameters) throws ClassNotFoundException
    {
        // Ensure class existence
        if (!testMap.containsKey(name)) {
            throw new ClassNotFoundException("No class registered to " + name);
        }

        // Instantiate the appropriate control law
        try {
            String classname = testMap.get(name);
            Constructor[] ctors = Class.forName(classname).getDeclaredConstructors();
            Constructor ctor = null;
            for (Constructor c: ctors) {
                if (c.getGenericParameterTypes().length > 0) {
                    ctor = c;
                    break;
                }
            }
            assert (ctor != null);
            Object obj = ctor.newInstance(parameters);
            assert (obj instanceof ConditionTest);

            return (ConditionTest) obj;
        } catch (InvocationTargetException ex) {
            System.err.printf("ERR: %s (%s)\n", ex, ex.getTargetException());
        } catch (Exception ex) {
            System.err.printf("ERR: %s\n", ex);
            ex.printStackTrace();
        }
        // XXX Should fail more gracefully
        assert (false); // Tried to instantiate non-existent conition test.
        return null;    // XXX
	}

    /** Get collections of parameters for all known condition tests. These lists of
     *  parameters specify information like which parameters are required to be
     *  set (vs. ones that will have reasonable default values), the type and
     *  range of values parameters may assume, etc.
     **/
    synchronized public Map<String, Collection<TypedParameter> > getParameters()
    {
        HashMap<String, Collection<TypedParameter> > map =
            new HashMap<String, Collection<TypedParameter> >();
        try {
            for (String name: testMap.keySet()) {
                String classname = testMap.get(name);
                Class klass = Class.forName(classname);
                Object obj = klass.newInstance();
                map.put(name, (Collection<TypedParameter>)klass.getDeclaredMethod("getParameters").invoke(obj));
            }
        } catch (Exception ex) {
            System.err.printf("ERR: %s\n", ex);
            ex.printStackTrace();
            assert (false);
        }

        return map;
    }

    public static void main(String[] args)
    {
        ConditionTestFactory factory = ConditionTestFactory.getSingleton();

        try {
            Map<String, Collection<TypedParameter> > map = factory.getParameters();
            for (String key: map.keySet()) {
                System.out.println("Found condition test " + key);
            }

            // Try constructing an instance of a control law based on parameters
            // XXX


        } catch (Exception ex) {
        //catch (ClassNotFoundException ex) {
            System.err.println("ERR: Could not construct condition test" + ex);
            ex.printStackTrace();
        }
    }
}
