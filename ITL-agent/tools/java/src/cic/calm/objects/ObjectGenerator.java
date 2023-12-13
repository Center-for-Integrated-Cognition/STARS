/**
 * This class reads in these files:
 * 	locations.txt		This lists the locations where objects can be.
 * 	object-types.txt	This lists types of objects with affordances.
 * 
 * The class produces several output files: (<nn> is the number of objects)
 * 	internal-world-<nn>.soar	A file defining both locations and movable
 * 								objects in the internal world.
 * 	spatial-relations<nn>.soar	Rules that add spatial relations to the world.
 * 	gold-standard-<nn>.csv		A list of gold standards for expected results.
 * 	planning-script-<nn>.txt	An instruction script with goals for planning mode
 * 	action-script-<nn>.txt		A script with both goals and how to reach them
 * 
 * The number of objects to generate, <nn>, is a command line parameter
 * which defaults to 10.
 * 
 */
package cic.calm.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author p
 *
 */
public class ObjectGenerator {
	//	Static Constants
	public final static Charset DEFAULT_ENCODING = StandardCharsets.ISO_8859_1;
	//	Default values for arguments
	public static int nObjects = 10;
	public static String directory = "./examples/teaching-clean/objects/";
	
	//	Private Members
	private List<String> objectCategories;
	private Map<String,ObjectType> objectTypes;
	private List<String> objectHandles;
	private Map<String,Object> objectMap;
	private Map<String,Object> locationsByName;
	private int nLocations;
	
	//	Public Constructor
	public ObjectGenerator() {
		this.objectCategories = new ArrayList<String>();
		this.objectTypes = new HashMap<String,ObjectType>();
		this.objectHandles = new ArrayList<String>();
		this.objectMap = new HashMap<String,Object>();
		this.locationsByName = new HashMap<String,Object>();
		this.nLocations = 0;
	}
	
	//	Public Methods
	public void loadObjectTypes() {
		//	Get all the types of objects from the object-types file
		System.out.println("#	ObjectTypes in object-types.txt");
		List<String> lines = getFileLines("object-types.txt");
		for (String line: lines) {
			ObjectType type = new ObjectType(line);
			String category = type.getCategory();
			this.objectCategories.add(category);
			this.objectTypes.put(category, type);
			System.out.println(type);
		}
	}
	
	public void loadObjects(String fileName) {
		//	Get all the specific objects from the named file
		System.out.println("#	Objects in " + fileName);
		//	Process the file
		List<String> lines = getFileLines(fileName);
		for (String line: lines) {
			Object object = new Object(line);
			//	Find and set its type
			String category = object.getCategory();
			ObjectType type = this.objectTypes.get(category);
			object.setType(type);
			type.addInstance(object);
			//	Record the object by its handle
			String handle = object.getHandle();
			this.objectHandles.add(handle);
			this.objectMap.put(handle, object);
			System.out.println(object);
			//	Record locations by their category (name)
			if (object.isLocation()) {
				String name = object.getType().getName();
				this.nLocations++;
				//	AN UGLY HACK!!!
				//	Make cabinet and cupboard equivalent
				if (name.equals("cabinet") || name.equals("cupboard")) {
					this.locationsByName.put("cabinet", object);
					System.out.println(String.format("%s is location %s", "cabinet", object));
					this.locationsByName.put("cupboard", object);
					System.out.println(String.format("%s is location %s", "cupboard", object));
				} else {
					this.locationsByName.put(name, object);
					System.out.println(String.format("%s is location %s", name, object));
				}
			}
		}
	}
	
	public void createObjects(String fileName) {
		//	Create the desired number of objects based on the types
		int id = 1;
		int objectsMade = 0;
		int loops = 0;
		StringBuilder builder = new StringBuilder();
		while (objectsMade < nObjects + this.nLocations) {
			//	Loop through the categories as many times as it takes
			int nextCategoryIndex = (loops++) % this.objectCategories.size();
			String category = this.objectCategories.get(nextCategoryIndex);
			ObjectType type = this.objectTypes.get(category);
			//	Create an object for this category, but only one per location
			List<Object> instances = type.getInstances();
			if (type.isImmovable() && instances.size() > 0) {
				continue;
			}
			//	Create a line of text for this object
			String text = type.createInstanceString(id++);
			builder.append(text + "\r\n");
			//	Create another object
			Object newObject = new Object(text);
			newObject.setType(type);
			type.addInstance(newObject);
			//	Record the object by its handle
			String handle = newObject.getHandle();
			this.objectHandles.add(handle);
			this.objectMap.put(handle, newObject);
			//	If it is a location, remember it
			if (newObject.isLocation()) {
				//	ASSUMPTION: ONLY ONE LOCATION FOR A LOCATION CATEGORY
				String name = newObject.getType().getName();
				this.locationsByName.put(name, newObject);
			} else {
				objectsMade++;
			}
		}
		//	Output a file with the text for all the objects
		writeFile(fileName, builder.toString());
		System.out.println("#   Objects generated in " + fileName);
		System.out.print(builder.toString());
	}
	
	public void loadInitialLocations(String fileName) {
		System.out.println("#	The initial locations of objects:");
		//	Process the file
		List<String> lines = getFileLines(fileName);
		//	Assume a line sequence by groups,
		//	where each group starts with a location handle
		//	and that is followed by object handles.
		//	Groups are separated by a blank line.
		boolean firstLine = true;
		String locHandle = null;
		Object location = null;
		for (String line: lines) {
			if (line.isBlank()) {
				firstLine = true;
				continue;
			}
			if (firstLine) {
				firstLine = false;
				//	Find the location for the group
				locHandle = line.trim();
				location = this.objectMap.get(locHandle);
			} else {
				//	Add an object on this location
				String handle = line.trim();
				Object object = this.objectMap.get(handle);
				object.setInitialLocation(location);
				//	Assume there is only one location of a given
				//	category, so use category instead of handle
				System.out.println(String.format("Object %s is at %s.", handle,
													location.getCategory()));
			}
		}
	}
	
	public void assignInitialLocations(String fileName) {
		//	Go through all the movable objects and give each a location
		System.out.println("#   Assigning initial locations");
		for (String handle: this.objectHandles) {
			Object object = this.objectMap.get(handle);
			//	Ignore locations
			if (object.isLocation()) {
				continue;
			}
			//	Calculate the possible initial locations
			List<String> targetNames = new ArrayList<String>();
			targetNames.add("counter");
			targetNames.add("table");
			//	Special processing for washable things
			if (object.hasAffordance("washable")) {
				//	Decide if the dishrack is -ok, -always, or -never
				if (object.hasAffordance("rack-always")) {
					targetNames = new ArrayList<String>();
					targetNames.add("rack");
				} else if (!object.hasAffordance("rack-never")) {
					targetNames.add("rack");
				}
			}
			//	Now pick from whatever options are left
			int index = object.getId() % targetNames.size();
			String target = targetNames.get(index);
			Object initialLocation = this.locationsByName.get(target);
			//	Put the object there
			object.setInitialLocation(initialLocation);
			initialLocation.addObjectThere(object);
			System.out.println("  " + object.getHandle() + " starts at "
									+ initialLocation.getHandle());
		}
		//	Accumulate the lines needed
		StringBuilder builder = new StringBuilder();
		for (String handle: this.objectHandles) {
			Object location = this.objectMap.get(handle);
			if (location.isLocation()) {
				//	Add a group of objects at this location
				List<Object> objectsThere = location.getObjectsHere();
				if (objectsThere.size() > 0) {
					builder.append(location.getHandle() + "\r\n");
					for (Object object: objectsThere) {
						builder.append(object.getHandle() + "\r\n");
					}
					builder.append("\r\n");
				}
			}
		}
		//	Output a file with all the initial locations
		writeFile(fileName, builder.toString());
		System.out.println("#   Initial locations in " + fileName);
		System.out.print(builder.toString());
		
	}

	public void buildGoldStandard() {
		//	Start accumulating the data string
		StringBuilder builder = new StringBuilder();

		//	Add line(s) for each object
		for (String handle: this.objectHandles) {
			//	Get the objects in order
			Object obj = this.objectMap.get(handle);
			builder.append(findGold(obj));
		}
		System.out.println("#	Gold standard");
		System.out.println(builder);
		
		//	Write this out to a file
		String fileName = String.format("gold-standard-%d.txt", nObjects);
		writeFile(fileName, builder.toString());
	}

	public void findDestinations() {
		//	Scan through all the objects
		System.out.println("#   Finding destinations");
		for (String handle: this.objectHandles) {
			Object object = this.objectMap.get(handle);
			if (!object.isLocation()) {
				//	Find the possible destinations for this object
				//	Sometimes we need the category and sometimes the name
				List<String> destinations = new ArrayList<String>();
				List<String> goalNames = new ArrayList<String>();
				//	Find its ObjectType
				String category = object.getCategory();
				ObjectType type = this.objectTypes.get(category);
				//	Get the affordances for that type
				List<String> affordances = type.getAffordances();
				//	The main class of objects should be the first affordance
				String objectClass = affordances.get(0);
				String target = (affordances.size() > 1)? affordances.get(1) :  null;
				Object location = (target != null)? this.locationsByName.get(target) : null;
				Object initial = object.getInitialLocation();
				switch (objectClass) {
				case "recyclable":
					location = this.locationsByName.get("bin");
					destinations.add(location.getCategory());
					goalNames.add(location.getType().getName());
					break;
				case "trashable":
					location = this.locationsByName.get("garbage");
					destinations.add(location.getCategory());
					goalNames.add(location.getType().getName());
					break;
				case "storable":
					//	Just put it in the designated target
					destinations.add(location.getCategory());
					goalNames.add(location.getType().getName());
					break;
				case "washable":
					//	If it has not been washed, put it in either
					//	the sink or the dishwasher, else in the target.
					if (!initial.getType().getName().equals("rack")) {
						Object sink = this.locationsByName.get("sink");
						destinations.add(sink.getCategory());
						goalNames.add(sink.getType().getName());
						Object dishwasher = this.locationsByName.get("dishwasher");
						destinations.add(dishwasher.getCategory());
						goalNames.add(dishwasher.getType().getName());
					} else {
						destinations.add(location.getCategory());
						goalNames.add(location.getType().getName());
					}
					break;
				default:
					System.out.println(String.format(
							"*** OBJECT %s HAS NO OBJECT CLASS!!! ***",
							object.getCategory()));
				}
				object.setDestinations(destinations);
				object.setGoalNames(goalNames);
				System.out.println(String.format("  %s destinations: %s",
						object.getHandle(), destinations));
			}
		}
	}


	//	Private Methods
	private String findGold(Object object) {
		String gold = "";
		//	Treat locations and movable objects differently
		if (object.isLocation()) {
			//	If it is openable, it should be closed
			//	Otherwise, ignore it
			if (object.isOpenable()) {
				gold = String.format("%s,is-open1,not-open1\r\n", object.getHandle());
			}
		} else {
			//	Build gold lines according to the destinations
			//	Assume all destination are "in" for movable objects
			List<String> destinations = object.getDestinations();
			//	RESTORE THE FOLLOWING LOOP IF ALLOWING MULTIPLE GOALS IN THE GOLD
//			for (String d: destinations) {
//				gold += String.format("%s,%s,%s\r\n", object.getHandle(), "in", d);
//			}
			//	If multiple destinations, pick one
			//	This is because Bob uses equivalences
			int index = object.getId() % destinations.size();
			String destination = destinations.get(index);
			gold += String.format("%s,%s,%s\r\n", object.getHandle(), "in", destination);
			//	Remember the name of the destination location
			String goalName = object.getGoalNames().get(index);
			object.setGold(goalName);
		}
		return gold;
	}

	public static List<String> getFileLines(String fileName) {
		return getFileLines(fileName, false);
	}
	
	public static List<String> getFileLines(String fileName, boolean keepComments) {
		List<String> lines = new ArrayList<String>();
        try (BufferedReader reader =
        		Files.newBufferedReader(Paths.get(directory, fileName),
        				DEFAULT_ENCODING)) {
            String line = null;
            while ((line = reader.readLine()) != null) {
            	//	Ignore comments
            	if (line.trim().startsWith("#") && !keepComments) {
            		continue;
            	}
            	//	Process each line
            	lines.add(line);
            }
        } catch (Exception e) {
        	System.out.println(e);
        }
		return lines;
	}

	public static void writeFile(String fileName, String content) {
		Path filePath = Paths.get(directory, fileName);
        try {
			Files.write(filePath, content.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//	The Main Program
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//	Get arguments to override defaults
		if (args.length > 0) {
			nObjects = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			directory = args[1];
		}

		//	Create and run an instance
		ObjectGenerator objectGen = new ObjectGenerator();
		//	Load the object types
		objectGen.loadObjectTypes();
		//	Load or generate the objects
		String objectsFileName = String.format("objects-%d.txt", nObjects);
		if (Files.exists(Paths.get(directory, objectsFileName))) {
			objectGen.loadObjects(objectsFileName);
		} else {
			objectGen.createObjects(objectsFileName);
		}
		//	Put them in alphanumeric order
		Collections.sort(objectGen.objectHandles);
		//	Load or generate the initial locations
		String initialsFileName = String.format("initial-locations-%d.txt", nObjects);
		if (Files.exists(Paths.get(directory, initialsFileName))) {
			objectGen.loadInitialLocations(initialsFileName);
		} else {
			objectGen.assignInitialLocations(initialsFileName);
		}
		//	Find their destinations
		objectGen.findDestinations();
		objectGen.buildGoldStandard();
		//	Generate various output files
		WorldGenerator worldGen = new WorldGenerator();
		String worldFileName = String.format("internal-world-%d.soar", nObjects);
		worldGen.buildIternalWorld(objectGen.objectMap, worldFileName);
		String relationsFileName = String.format("spatial-relations-%d.soar", nObjects);
		worldGen.buildSpatialRelations(relationsFileName);
		//	Generate various output files
		String scriptFileName = String.format("planning-script-%d.soar", nObjects);
		ScriptGenerator scriptGen = new ScriptGenerator();
		scriptGen.buildPlanningScript(objectGen.objectMap, scriptFileName);
//		scriptGen.buildActionScript();
	}

}
