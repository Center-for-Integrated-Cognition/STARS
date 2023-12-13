package soargroup.rosie.perception;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.umich.rosie.soar.SoarUtil;
import edu.umich.rosie.soar.ISVSObject;

import sml.Identifier;

// LCM Types
import lcm.lcm.*;
import soargroup.mobilesim.lcmtypes.object_data_t;
import soargroup.mobilesim.lcmtypes.object_data_list_t;

public class WorldObjectManager implements ISVSObject {
	private HashMap<Integer, WorldObject> objects;
	private HashMap<Integer, WorldObject> objsToRemove;
	private boolean gotUpdate = true;

    private Identifier objectsId = null;

    public WorldObjectManager(){
    	objects = new HashMap<Integer, WorldObject>();
    	objsToRemove = new HashMap<Integer, WorldObject>();
    }

	public WorldObject getObject(Integer id){
		return objects.get(id);
	}

	public void update(object_data_list_t newObjectData){
    	// Set of objects that didn't appear in the new update
    	// (remove ids as we see them)
    	HashSet<Integer> oldIds = new HashSet<Integer>();
    	oldIds.addAll(objects.keySet());

		for(object_data_t objData : newObjectData.objects){
			if(!objData.visible){
				// Ignore objects that aren't visible
				continue;
			}
			Integer objID = objData.id;
			if(oldIds.contains(objID)){
				oldIds.remove(objID);
			}

			WorldObject obj = objects.get(objID);
			if(obj == null){
				if(objsToRemove.containsKey(objID)){
    				// Object was going to be removed
    				// Transfer it to the normal list and update
					obj = objsToRemove.get(objID);
					objsToRemove.remove(objID);
				} else {
    				// It's a new object, add it to the map
					obj = new WorldObject(objData);
					objects.put(objID, obj);
				}
			}

    		obj.update(objData);
    	}

    	for(Integer oldID : oldIds){
    		WorldObject oldObj = objects.get(oldID);
    		objects.remove(oldID);
    		objsToRemove.put(oldID, oldObj);
    	}

		gotUpdate = true;
    }

	// Returns true if the object is currently in working memory
	public boolean isAdded(){
		return (objectsId != null);
	}

	// Add the object to working memory, on the given identifier
	public void addToWM(Identifier parentId, StringBuilder svsCommands){
		if (objectsId != null){ return; }

		objectsId = parentId.CreateIdWME("objects");
		for(WorldObject obj : objects.values()){
			obj.addToWM(objectsId, svsCommands);
		}
	}
	
	// Update is called before input phase, changes can be made to WM
	public void updateWM(StringBuilder svsCommands){
		if (objectsId == null){ return; }
		if (!gotUpdate){ return; }

		for(WorldObject obj : objects.values()){
			if(obj.isAdded()){
				obj.updateWM(svsCommands);
			} else {
				obj.addToWM(objectsId, svsCommands);
			}
		}
    	for(WorldObject obj : objsToRemove.values()){
    		obj.removeFromWM(svsCommands);
    	}
    	objsToRemove.clear();
		gotUpdate = false;
	}
	
	// Release all references to SML objects and remove from WM
	public void removeFromWM(StringBuilder svsCommands){
		if (objectsId == null){ return; }

		for(WorldObject obj : objects.values()){
    		obj.removeFromWM(svsCommands);
		}
    	for(WorldObject obj : objsToRemove.values()){
    		obj.removeFromWM(svsCommands);
    	}
		objsToRemove.clear();

		objectsId.DestroyWME();
		objectsId = null;
	}
}
