/**
 * This class represents an object type which can be the source
 * for generating a number of objects.
 * 
 */
package cic.calm.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * @author p
 *
 */
public class ObjectType {
	//	Private Members
	private String category;
	private String name;
	private List<String> affordances;
	private boolean immovable;
	private List<Object> instances;
	private String goalPlace;
	private boolean washable;
	
	//	Public Properties
	public String getCategory() { return this.category;	}
	public String getName() { return this.name;	}
	public List<String> getAffordances() { return this.affordances;	}
	public boolean isImmovable() { return this.immovable; }
	public List<Object> getInstances() { return this.instances;	}
	public boolean isOpenable() { return this.affordances.contains("openable"); }
	public boolean isWashable() { return this.washable; }
	public String getGoalPlace() { return this.goalPlace; }

	
	//	Public Constructor
	public ObjectType(String line) {
		//	Initialize lists
		this.affordances = new ArrayList<String>();
		this.instances = new ArrayList<Object>();
		//	Parse the line to get the data
		String[] fields = line.split(" ");
		this.category = fields[0];
		this.name = fields[1];
		for (int i = 2; i < fields.length; i++) {
			String field = fields[i];
			//	This is an affordance
			this.affordances.add(field);
			//	If this is a location, it should have
			//	either a receptacle1 or a surface1 affordance.
			if (field.equals("receptacle1") || field.equals("surface1")) {
				this.immovable = true;
			}
			//	Remember the goal place
			switch (field) {
			case "trashable":	this.goalPlace = "garbage";		break;
			case "recyclable":	this.goalPlace = "bin";			break;
			case "washable":
				this.washable = true;
			case "storable":	this.goalPlace = fields[i + 1];	break;
			}
		}
	}
	
	//	Public Methods
	public String createInstanceString(int id) {
		//	Build the basic string
		String text = String.format("%s_%d ^category %s", this.category, id, this.category);
		//	Add additional augmentations as needed
		for (String a: this.affordances) {
			//	A property with value in parens
			if (a.matches("[a-z]+\\(.+\\)")) {
				String value = a.replaceFirst("([a-z]+)\\((.+)\\)", " ^$1 $2");
				text += value;
			} else {
				//	Consider other affordances
				switch (a) {
				case "openable1":	text += " ^is-open1 not-open1";
				}
			}
		}
		//	Return the result
		return text;
	}
	
	public void addInstance(Object object) {
		this.instances.add(object);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(this.name);
		builder.append("(" + this.category + ")");
		if (this.affordances.size() > 0) {
			builder.append(" is (");
			for (String a: this.affordances) {
				if (!builder.toString().endsWith("("))
					builder.append(",");
				builder.append(a);
			}
			builder.append(")");
		}
		return builder.toString();
	}

}
