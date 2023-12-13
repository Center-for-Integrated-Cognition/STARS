package soargroup.mobilesim.commands.controls;

import java.util.*;
import java.lang.reflect.*;

import soargroup.mobilesim.commands.*;

public class ControlLawFactory
{
    HashMap<String, String> controlLawMap = new HashMap<String, String>();

    static ControlLawFactory singleton = null;
    static public ControlLawFactory getSingleton()
    {
        if (singleton == null)
            singleton = new ControlLawFactory();
        return singleton;
    }

    private ControlLawFactory()
    {
        init();
    }

    /** Initializes the factory on first use. This is where the default
     *  registrations are added.
     **/
    private void init()
    {
        registerControlLaw("turn", Turn.class.getName());
        registerControlLaw("drive-forward", DriveForward.class.getName());
        registerControlLaw("follow-wall", FollowWall.class.getName());
        registerControlLaw("orient", Orient.class.getName());
        registerControlLaw("drive-xy", DriveToXY.class.getName());
        registerControlLaw("pick-up", PickUp.class.getName());
        registerControlLaw("put-down", PutDown.class.getName());
        registerControlLaw("put-at-xyz", PutAtXYZ.class.getName());
        registerControlLaw("put-on-object", PutOnObject.class.getName());
        registerControlLaw("change-state", ChangeState.class.getName());
        registerControlLaw("use-object", UseObject.class.getName());
        registerControlLaw("press", Press.class.getName());
    }

    /** Register control laws with the factory. It is the job of the
     *  writer to ensure that provided classes implement the appropriate
     *  interface. Reflection is used to construct an instance of the
     *  appropriate class.
     *
     *  @param name         The name provided by the external system
     *  @param classname    The given name of the class.
     *
     **/
    synchronized public void registerControlLaw(String name, String classname)
    {
        controlLawMap.put(name, classname);
    }

    /** Unregister a control law. Returns true/false if control law was
     *  successfully removed/no matching key was found.
     *
     *  @param name         The name of the control law to be removed
     *
     *  @return True if control law successfully removed, else false
     **/
    synchronized public boolean unregisterControllaw(String name)
    {
        String val = controlLawMap.remove(name);
        return (val != null);
    }

    /** Construct a control law from an associated set of parameters.
     *
     *  @param name         Registered name of control law
     *  @param parameters   Input parameters for construction
     *
     *  @return A ControlLaw object that a robot may execute
     **/
	synchronized public ControlLaw construct(String name, Map<String, TypedValue> parameters) throws ClassNotFoundException
    {
        // Ensure class existence
        if (!controlLawMap.containsKey(name)) {
            throw new ClassNotFoundException("No class registered to " + name);
        }

        // Instantiate the appropriate control law
        try {
            String classname = controlLawMap.get(name);
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
            assert (obj instanceof ControlLaw);

            return (ControlLaw) obj;
        } catch (InvocationTargetException ex) {
            System.err.printf("ERR: %s (%s)\n", ex, ex.getTargetException());
        } catch (Exception ex) {
            System.err.printf("ERR: %s\n", ex);
            ex.printStackTrace();
        }
        return null; // XXX Should fail more gracefully
	}

    /** Get collections of parameters for all known control laws. These lists of
     *  parameters specify information like which parameters are required to be
     *  set (vs. ones that will have reasonable default values), the type and
     *  range of values parameters may assume, etc.
     **/
    synchronized public Map<String, Collection<TypedParameter> > getParameters()
    {
        HashMap<String, Collection<TypedParameter> > map =
            new HashMap<String, Collection<TypedParameter> >();
        try {
            for (String name: controlLawMap.keySet()) {
                String classname = controlLawMap.get(name);
                Class klass = Class.forName(classname);
				Method getParams = klass.getMethod("getParameters");
                map.put(name, (Collection<TypedParameter>)klass.getMethod("getParameters").invoke(klass));
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
        ControlLawFactory factory = ControlLawFactory.getSingleton();

        try {
            Map<String, Collection<TypedParameter> > map = factory.getParameters();
            for (String key: map.keySet()) {
                System.out.println("Found control law " + key);
            }

            // Try constructing an instance of a control law based on parameters
            // XXX
        } catch (Exception ex) {
        //catch (ClassNotFoundException ex) {
            System.err.println("ERR: Could not construct control law" + ex);
            ex.printStackTrace();
        }
    }
}
