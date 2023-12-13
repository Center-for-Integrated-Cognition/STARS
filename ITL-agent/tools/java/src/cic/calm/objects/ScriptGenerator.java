/**
 * This class uses data already assembled by the ObjectGenerator
 * to create a script to simulate human instruction for
 * the "Tidy kitchen." task.
 * 
 */
package cic.calm.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author p
 *
 */
public class ScriptGenerator {
	//	Private Members
	private List<String> objectHandles;
	private Map<String,Object> objectMap;
	private Map<String,List<Object>> objectsByPlace;
	private Map<String,Object> locationsByName;
	
	//	Public Constructor
	
	//	Public Methods
	public void buildPlanningScript(Map<String, Object> objectMap, String scriptFileName) {
		//	Start building the script
		StringBuilder builder = new StringBuilder();
		builder.append("Tidy kitchen.\r\n");
		//	Get the map and build the list of handles
		this.objectMap = objectMap;
		Set<String> keys = this.objectMap.keySet();
		List<String> handles = new ArrayList<String>(keys);
		Collections.sort(handles);
		this.objectHandles = handles;
		this.locationsByName = new HashMap<String,Object>();
		//	Find the objects in each initial location
		this.objectsByPlace = new HashMap<String,List<Object>>();
		for (String handle: this.objectHandles) {
			Object object = this.objectMap.get(handle);
			if (!object.isLocation()) {
				Object location = object.getInitialLocation();
				String locationName = location.getType().getName();
				if (!this.objectsByPlace.containsKey(locationName)) {
					this.objectsByPlace.put(locationName, new ArrayList<Object>());
				}
				this.objectsByPlace.get(locationName).add(object);
			} else {
				//	Remember the locations for later
				String objectName = object.getType().getName();
				this.locationsByName.put(objectName, object);
				//	A cabinet is the same as a cupboard
				if (objectName.equals("cupboard")) {
					this.locationsByName.put("cabinet", object);
				}
			}
		}
		//	Add a section for each initial location
		List<String> placesToTidy = Arrays.asList(new String[]
										{"table", "counter", "rack"});
		for (String place: placesToTidy) {
			addContent(place, builder);
		}
		//	Add the postamble
		builder.append("You are done.\r\n");
		//	Write out the script
		ObjectGenerator.writeFile(scriptFileName, builder.toString());
		System.out.println(builder.toString());
	}

	//	Private Methods
	private void addContent(String place, StringBuilder builder) {
		//	Find parameters of the section introduction
		String placePP = null;
		String mainVerb = null;
		switch (place) {
		case "table":	placePP = "on the table";		mainVerb = "Clear";	break;
		case "counter":	placePP = "on the counter";		mainVerb = "Store";	break;
		case "rack":	placePP = "in the dish rack";	mainVerb = "Unload";break;
		}
		//	Build the section introduction
		String repeatLine = String.format(
				"Repeat the following tasks while an object is %s.\r\n", placePP);
		String actionLine = String.format(
				"%s an object that is %s.\r\n", mainVerb, placePP);
		builder.append(repeatLine);
		builder.append(actionLine);
		//	Collect the list of object types for this place
		List<Object> objectsHere = this.objectsByPlace.get(place);
		List<ObjectType> typesHere = new ArrayList<ObjectType>();
		for (Object object: objectsHere) {
			ObjectType type = object.getType();
			if (!typesHere.contains(type)) {
				typesHere.add(type);
			}
		}
		//	Build the goal statements
		for (ObjectType type: typesHere) {
			String typeName = type.getName();
			//	Build the if part
			String ifPart = String.format("If the object is a %s ", typeName);
			//	Build the then part
			//	The goal location depends on the type, unless the object
			//	is washable. In that case it is the gold destination.
			List<Object> instances = type.getInstances();
			if (instances.size() == 0) {
				//	There are no objects of this type in this place, so ignore it
				//	This should never happen here
				continue;
			}
			String goalPlace = (type.isWashable())? instances.get(0).getGold()
										: type.getGoalPlace();
			String goal = String.format("in the %s", goalPlace);
			//	Add closed condition if needed
			Object thisPlace = this.locationsByName.get(goalPlace);
			if (thisPlace.isOpenable()) {
				goal += String.format(" and the %s is closed", goalPlace);
			}
			String thenPart =
					String.format("then the goal is that the object is %s.\r\n", goal);
			//	Build the whole conditional goal statement
			builder.append(ifPart + thenPart);
		}
		//	Build the section conclusion
		builder.append("You are done.\r\n");
		builder.append("Repeat.\r\n");
	}

}
