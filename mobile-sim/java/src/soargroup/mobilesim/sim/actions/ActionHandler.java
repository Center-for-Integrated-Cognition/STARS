package soargroup.mobilesim.sim.actions;

import java.util.*;
import soargroup.mobilesim.util.ResultTypes.*;
import soargroup.mobilesim.sim.SimRobot;

public class ActionHandler {

	/************************ Handle Actions *************************/

	// First validates the action by checking it against each validate rule for that action type
	// If it validates, then it applies the action by running each apply rule for the action type
	public static Result handle(Action action, SimRobot robot){
		action.setRobot(robot);
		IsValid isValid = ActionHandler.validate(action);
		if(isValid instanceof NotValid){
			return Result.Err("Action " + action.toString() + " is not valid\n" + 
					"Reason: " + ((NotValid)isValid).reason);
		}
		Result result = ActionHandler.apply(action);
		if(result instanceof Err){
			return Result.Err("Action " + action.toString() + " failed\n" + 
					"Reason: " + ((Err)result).reason);
		}
		return Result.Ok();
	}

	private interface Rule { } 

	/*********************** Validate Actions *************************/
	// Before applying an action, we make sure it is valid
	
	public interface ValidateRule<A extends Action> extends Rule {
		IsValid validate(A action);
	}

	// Any object can add a validation rule that must be checked before doing an action
	public static <A extends Action> void addValidateRule(Class<A> actionType, ValidateRule<A> rule){
		if(!validateRules.containsKey(actionType)){
			// If this is the first validate rule for a certain type, create a new list in the validateRules map
			validateRules.put(actionType, new ArrayList<Rule>());
		}
		validateRules.get(actionType).add(rule);
	}

	// Checks the given action against all the validation rules to make sure it is ok to apply
	private static IsValid validate(Action action) {
		IsValid isValid = IsValid.True();

		// Look for handler rules for the given class and any superclasses that are also Actions
		// Keep traversing up by superclass until no longer an Action
		Class cls = action.getClass();
		while(Action.class.isAssignableFrom(cls)){
			List<Rule> rules = validateRules.get(cls);
			// Loop through every ValidateRule for the given action type and make sure none of them are invalid
			if(rules == null){
				break;
			}
			for(Rule rule : rules){
				ValidateRule<Action> vrule = (ValidateRule<Action>)rule;
				isValid = isValid.AND(vrule.validate(action));
			}
			cls = cls.getSuperclass();
		}
		return isValid;
	}

	// Map from an action class to a list of ValidateRules for that action type
	private static HashMap<Class, List<Rule> >validateRules = new HashMap<Class, List<Rule> >();


	/*********************** Apply Actions *************************/
	// once an action is validated, then apply it via the given handlers

	public interface ApplyRule<A extends Action> extends Rule{
		Result apply(A action);
	}

	public static <A extends Action> void addApplyRule(Class<A> actionType, ApplyRule<A> rule){
		if(!applyRules.containsKey(actionType)){
			// If this is the first apply rule for a certain type, create a new list in the applyRules map
			applyRules.put(actionType, new ArrayList<Rule>());
		}
		applyRules.get(actionType).add(rule);
	}

	// Calls each apply rule involving the given action and reports whether it succeeded or failed
	private static Result apply(Action action) {
		// Look for handler rules for the given class and any superclasses that are also Actions
		// Keep traversing up by superclass until no longer an Action
		Class cls = action.getClass();
		while(Action.class.isAssignableFrom(cls)){
			List<Rule> rules = applyRules.get(cls);
			if(rules == null){
				break;
			}
			// Loop through every ApplyRule for the given action type
			for(Rule rule : rules){
				ApplyRule<Action> arule = (ApplyRule<Action>)rule;
				Result result = arule.apply(action);
				if(result instanceof Err){
					return result;
				}
			}
			cls = cls.getSuperclass();
		}
		return Result.Ok();
	}

	// Map from an action class to a list of ApplyRules for that action type
	private static HashMap<Class, List<Rule> > applyRules = new HashMap<Class, List<Rule> >();
}
