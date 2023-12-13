/**
 * This class reads in these files:
 * 	object-types.txt	This lists types of objects with affordances.
 * 
 * The class produces several output files: (<nn> is the number of objects)
 * 	internal-world-<nn>.soar	A file defining both locations and movable
 * 								objects in the internal world.
 * 	spatial-relations<nn>.soar	Rules that add spatial relations to the world.
 * 
 * The number of objects to generate, <nn>, is a command line parameter
 * which defaults to 10.
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
public class WorldGenerator {
	//	Private Members
	private List<String> objectHandles;
	private Map<String,Object> objectMap;
	
	//	Public Constructor
	
	//	Public Methods
	public void buildIternalWorld(Map<String,Object> objectMap, String worldFileName) {
		//	Get the map and build the list of handles
		this.objectMap = objectMap;
		Set<String> keys = this.objectMap.keySet();
		List<String> handles = new ArrayList<String>(keys);
		Collections.sort(handles);
		this.objectHandles = handles;
		//	Copy the template with data filled in
		StringBuilder builder = new StringBuilder();
		List<String> template =
				ObjectGenerator.getFileLines("internal-world-template.soar", true);
		for (String line: template) {
			if (line.trim().equals("##OBJECT-LIST##")) {
				builder.append("\r\n   ###  LOCATION OBJECTS\r\n");
				//	Fill in the locations
				for (String handle: this.objectHandles) {
					Object location = this.objectMap.get(handle);
					if (location.isLocation()) {
						builder.append(location.buildWorldString());
					}
				}
				//	Fill in the movable objects
				builder.append("\r\n   ###  MOVABLE OBJECTS\r\n");

				String location_check = "table1";
				for (String handle: this.objectHandles) {
					Object object = this.objectMap.get(handle);
					if (!object.isLocation())
					{
						String obj_location = object.getInitialLocation().getHandle();
						String obj_location_name = obj_location.split("_")[0];

					 	if (obj_location_name.equals(location_check)) {
							builder.append(object.buildWorldString());
						 }
					}
				}
				location_check = "counter1";
				for (String handle: this.objectHandles) {
					Object object = this.objectMap.get(handle);
					if (!object.isLocation())
					{
						String obj_location = object.getInitialLocation().getHandle();
						String obj_location_name = obj_location.split("_")[0];

					 	if (obj_location_name.equals(location_check)) {
							builder.append(object.buildWorldString());
						 }
					}
				}
				location_check = "rack1";
				for (String handle: this.objectHandles) {
					Object object = this.objectMap.get(handle);
					if (!object.isLocation())
					{
						String obj_location = object.getInitialLocation().getHandle();
						String obj_location_name = obj_location.split("_")[0];

					 	if (obj_location_name.equals(location_check)) {
							builder.append(object.buildWorldString());
						 }
					}
				}
			} else {
				builder.append(line + "\r\n");
			}
		}
		//	Finish off the data and write it to the file
		ObjectGenerator.writeFile(worldFileName, builder.toString());
		System.out.println(builder.toString());
	}

	public void buildSpatialRelations(String relationsFileName) {
		//	Assume the object data was already filled in for the internal world
		//	Gather all the initial location
		List<String> locations = new ArrayList<String>();
		Map<String,List<String>> locationMap = new HashMap<String,List<String>>();
		for (String handle: this.objectHandles) {
			Object object = this.objectMap.get(handle);
			if (!object.isLocation()) {
				//	Get the object initial location
				String location = object.getInitialLocation().getHandle();
				//	Register this location if not already there
				if (!locationMap.containsKey(location)) {
					locations.add(location);
					locationMap.put(location, new ArrayList<String>());
				}
				//	Add this object to the list
				locationMap.get(location).add(handle);
			}
		}
		//	Initialize output string
		StringBuilder builder = new StringBuilder();
		//	Pass through the template and organize it
		List<String> template =
				ObjectGenerator.getFileLines("spatial-relations-template.soar", true);
		List<String> operator = new ArrayList<String>();
		boolean inOperator = false;
		for (String line: template) {
			if (!inOperator && !line.startsWith("### START")) {
				//	Just copy these initial lines
				builder.append(line + "\r\n");
			} else if (line.startsWith("### START")) {
				//	Now start gathering the operator lines
				inOperator = true;
			} else if (inOperator && !line.startsWith("### END")) {
				operator.add(line);
			}
		}
		
		//	Build a list for each occupied location
		for (String location: locations) {
			//	Get the list of object handles for this location
			List<String> objects = locationMap.get(location);
			//	Find the category and relation for this location
			List<String> onLocations = Arrays.asList(
					new String[] { "counter1", "table1" });
			String locCategory = this.objectMap.get(location).getCategory();
			String relation = (onLocations.contains(locCategory))? "on" : "in";
			//	Build ##OBJECT-NAMES##
			String objectNames = "";
			for (String handle: objects) {
				//	Turn handles int Soar variables to handle multiples
				objectNames += String.format(" <%s>", handle);
			}
			objectNames = reformatObjects(objectNames.trim());
			//	Build ##OBJECT-LIST##
			String objectList = String.format("   (<%s> ^handle %s)\r\n",
												location, location);
			for (String handle: locationMap.get(location)) {
				objectList += String.format("   (<%s> ^handle %s)\r\n",
											handle, handle);
			}
			objectList = objectList.substring(0, objectList.length() - 2);
			//	Build ##INSTANCE-IDS##
			String instanceIds = "";
			for (int i = 1; i <= objects.size(); i++) {
				instanceIds += String.format(" <i%d>", i);
			}
			instanceIds = reformatInstances(instanceIds.trim());
			//	Build ##INSTANCE-LIST##
			String instanceList = "";
			int i = 1;
			for (String handle: locationMap.get(location)) {
				instanceList += String.format("   (<i%d> ^1 <%s> ^2 <%s>)\r\n",
													i++, handle, location);
			}
			instanceList = instanceList.substring(0, instanceList.length() - 2);
			//	Copy the operator template with data filled in
			for (String line: operator) {
				String newLine = line;
				newLine = rebuildLine(newLine, "##LOCATION##", location);
				newLine = rebuildLine(newLine, "##RELATION##", relation);
				newLine = rebuildLine(newLine, "##OBJECT-NAMES##", objectNames);
				newLine = rebuildLine(newLine, "##OBJECT-LIST##", objectList);
				newLine = rebuildLine(newLine, "##INSTANCE-IDS##", instanceIds);
				newLine = rebuildLine(newLine, "##INSTANCE-LIST##", instanceList);
				builder.append(newLine + "\r\n");
			}
		}
		//	Finish off the data and write it to the file
		ObjectGenerator.writeFile(relationsFileName, builder.toString());
		System.out.println(builder.toString());
	}

	//	Private Methods
	private String rebuildLine(String source, String pattern, String substitution) {
		String newLine = source;
		while (newLine.contains(pattern)) {
			int start = newLine.indexOf(pattern);
			String prefix = newLine.substring(0, start);
			String postfix = newLine.substring(start + pattern.length());
			newLine = String.format("%s%s%s", prefix, substitution, postfix);
		}
		return newLine;
	}
	
	private String reformatObjects(String list) {
		String[] items = list.split(" ");
		String result = "";
		for (int i = 0; i < items.length; i++) {
			if ((i % 4) == 0) {
				result += "\r\n          ";
			}
			result += " " + items[i];
		}
		return result;
	}

	private String reformatInstances(String list) {
		String[] items = list.split(" ");
		String result = "";
		for (int i = 0; i < items.length; i++) {
			if (i > 0 && (i % 8) == 0) {
				result += "\r\n                  ";
			}
			result += " " + items[i];
		}
		return result;
	}

}
