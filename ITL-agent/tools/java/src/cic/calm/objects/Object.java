/**
 * This class represents an individual object in an experiment.
 * 
 */
package cic.calm.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * @author p
 *
 */
public class Object {
	//	A private helper class
	private class Predicate {
		String name;
		String value;

		public Predicate(String name, String value) {
			this.name = name; this.value = value;
		}
		
		@Override
		public String toString() {
			return String.format("%s=%s", this.name, this.value);
		}
	}
	
	//	Private Members
	private int id;
	private String handle;
	private List<Predicate> predicates;
	private String category;
	private ObjectType type;
	private boolean openable;
	private Object initialLocation;
	private List<String> destinations;
	private List<String> goalNames;
	private List<Object> objectsHere;
	private String gold;
	
	//	Public Properties
	public int getId() { return id; }
	public String getHandle() { return this.handle; }
	public String getCategory() { return this.category; }
	public void setType(ObjectType type) { this.type = type; }
	public ObjectType getType() { return this.type; }
	public boolean hasAffordance(String a) { return this.type.getAffordances().contains(a); }
	public boolean isOpenable() { return this.openable; }
	public void setInitialLocation(Object location) { this.initialLocation = location; }
	public Object getInitialLocation() { return this.initialLocation; }
	public boolean isLocation() { return type.isImmovable(); }
	public void setDestinations(List<String> destinations) { this.destinations = destinations; }
	public List<String> getDestinations() { return this.destinations; }
	public void setGoalNames(List<String> goalNames) { this.goalNames = goalNames; }
	public List<String> getGoalNames() { return this.goalNames; }
	public List<Object> getObjectsHere() { return this.objectsHere; }
	public void setGold(String gold) { this.gold = gold;}
	public String getGold() { return this.gold; }
	
	//	Public Constructor
	public Object(String line) {
		//	Initialize lists
		this.predicates = new ArrayList<Predicate>();
		this.objectsHere = new ArrayList<Object>();
		//	Parse the line to get the data
		String[] fields = line.split(" ");
		this.id = Integer.parseInt(fields[0].replaceFirst(".+_(\\d+)", "$1"));
		this.handle = fields[0];
		String predName = null;
		for (int i = 1; i < fields.length; i++) {
			String field = fields[i];
			if (field.startsWith("^")) {
				//	This is the name of a property
				predName = field.substring(1);
			} else {
				//	This is the value of the property
				//	Assume they alternate
				Predicate pred = new Predicate(predName, field);
				this.predicates.add(pred);
				//	^category is special
				if (predName.equals("category")) {
					this.category = field;
				}
				//	Check for openable
				if (pred.name.equals("is-open1")) {
					this.openable = true;
				}
			}
		}
	}
	
	//	Public Methods
	public void addObjectThere(Object object) {
		this.objectsHere.add(object);
	}

	public String buildWorldString() {
		String worldObject = "";
		worldObject += String.format("   (<objs> ^object <obj%d>)\r\n", this.id);
		worldObject += String.format(
				"   (<obj%d> ^handle %s ^perc-id |%d| ",
				this.id, this.handle, this.id);
		worldObject += String.format(
				"^waypoint wp04 ^predicates <obj%d-preds>)\r\n",
				this.id);
		worldObject += String.format("   (<obj%d-preds>", this.id);
		for (Predicate pred: this.predicates) {
			worldObject += String.format(" ^%s %s", pred.name, pred.value);
		}
		worldObject += ")\r\n\r\n";
		return worldObject;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.handle);
		builder.append("(" + this.category + ")");
		if (this.predicates.size() > 0) {
			builder.append(" has (");
			for (Predicate p: this.predicates) {
				if (!builder.toString().endsWith("("))
					builder.append(",");
				builder.append(p);
			}
			builder.append(")");
		}
		return builder.toString();
	}

}
