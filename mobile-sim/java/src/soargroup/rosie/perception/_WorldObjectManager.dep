package soargroup.rosie.perception;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import april.util.StringUtil;

public class WorldObjectManager {
	
	private HashMap<Integer, WorldObject> objects;
	
	public WorldObjectManager(Properties props){
		objects = new HashMap<Integer, WorldObject>();
	}
	
	public WorldObject getObject(Integer tagID){
		return objects.get(tagID);
	}
	
	public HashSet<WorldObject> getObjects(){
		return new HashSet<WorldObject>(objects.values());
	}
	
	private void readObjectInfoFile(String filename){
        // This will reference one line at a time
        String line = null;

        try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = new FileReader(filename);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) {
            	parseObjectInfo(line);
            }   

            System.out.println("WorldObjectManager: Loaded " + objects.size() + " objects\n");

            // Always close files.
            bufferedReader.close();         
        }
        catch(FileNotFoundException ex) {
            System.err.println("WorldObjectManager: unable to open file " + filename);
        }
        catch(IOException ex) {
        	System.err.println("WorldObjectManager: error reading file " + filename);
        }
	}
	
	private void parseObjectInfo(String info){
		String[] params = info.split(" ");
		if(params.length < 8){
			return;
		}
		// Param 1 : id
		Integer tagID = new Integer(params[0]);

		if(objects.containsKey(tagID)){
			System.err.println("Trying to add multiple objects for tag id " + tagID.toString());
			return;
		}

		// Param 2-4: size
		double[] pos = new double[]{
				new Double(params[1]), new Double(params[2]), new Double(params[3])
		};

		// Param 5-7: size
		double[] size = new double[]{
				new Double(params[4]), new Double(params[5]), new Double(params[6])
		};
		
		// Param 8: num classes
		Integer numClasses = new Integer(params[7]);
		HashMap<String, String> classifications = new HashMap<String, String>();
		for(int i = 0; i < numClasses; i++){
			String name = params[2*i+8];
			String value = params[2*i+9];
			classifications.put(name, value);
		}
		objects.put(tagID, new WorldObject(tagID, pos, size, classifications));
		//System.out.println("Added object " + tagID.toString());
		//System.out.println("  " + classifications.toString());
	}
}
