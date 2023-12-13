package soargroup.rosie.perception;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Arrays;

import java.nio.ByteBuffer;

import april.jmat.LinAlg;
import sml.Identifier;
import edu.umich.rosie.soar.ISVSObject;
import edu.umich.rosie.soar.IntWME;
import edu.umich.rosie.soar.SVSCommands;
import edu.umich.rosie.soar.StringWME;

import soargroup.mobilesim.lcmtypes.object_data_t;
import soargroup.mobilesim.lcmtypes.classification_t;

public class WorldObject implements ISVSObject {
	private Integer id;
	private StringWME handle;

	private HashMap<String, ObjectProperty> properties;

	private boolean updatePos = true;
	private double[] pos = new double[3];

	private boolean updateRot = true;
	private double[] rot = new double[3];

	private boolean updateScale = true;
	private double[] scale = new double[3];

	public WorldObject(object_data_t objData){
		this.id = objData.id;
		this.handle = new StringWME("object-handle", id.toString());
		this.properties = new HashMap<String, ObjectProperty>();

		this.update(objData);
	}

	public String getHandle(){
		return handle.getValue();
	}

	public synchronized void setHandle(String handle){
		this.handle.setValue(handle);
	}

	public void setPos(double[] pos){
		this.pos = pos;
		updatePos = true;
	}

	public double[] getPos(){
		return pos;
	}

	public synchronized void update(object_data_t objData){
		for(int d = 0; d < 3; d++){
			// Only update pos if it has changed by a significant amount
			if(Math.abs(pos[d] - objData.xyzrpy[d]) > 0.01){
				pos[d] = objData.xyzrpy[d];
				updatePos = true;
			}
			// Only update rot if it has changed by a significant amount
			if(Math.abs(rot[d] - objData.xyzrpy[3+d]) > 0.05){
				rot[d] = objData.xyzrpy[3+d];
				updateRot = true;
			}
			// Only update scale if it was changed by a significant amount
			if(Math.abs(scale[d] - objData.lenxyz[d]) > 0.01){
				scale[d] = objData.lenxyz[d];
				updateScale = true;
			}
		}

		// Group classifications by category and update the properties
		for(ObjectProperty prop : properties.values()){
			prop.clearValues();
		}
		for(classification_t cls : objData.classifications){
			ObjectProperty prop = properties.get(cls.category);
			if(prop == null){
				prop = new ObjectProperty(cls.category);
				properties.put(cls.category, prop);
			}
			prop.addValue(cls.name, cls.confidence);
		}
	}

	 /******************************************************************
     * Methods for Modifying Working Memory
     *****************************************************************/
	private Identifier rootID = null;

    public boolean isAdded(){
		return (rootID != null);
    }

    public synchronized void addToWM(Identifier parentID, StringBuilder svsCommands){
		if (rootID != null){ return; }

    	rootID = parentID.CreateIdWME("object");
    	handle.addToWM(rootID);
    	for (ObjectProperty prop : properties.values()){
			prop.addToWM(rootID);
    	}

    	svsCommands.append(SVSCommands.addBox(handle.getValue(), pos, rot, scale));
    	svsCommands.append(SVSCommands.addTag(handle.getValue(), "object-source", "perception"));
    }

    public synchronized void updateWM(StringBuilder svsCommands){
		if (rootID == null){ return; }

        // Update the pose in SVS
    	if(updatePos){
   			svsCommands.append(SVSCommands.changePos(handle.getValue(), pos));
   			updatePos = false;
    	}
   		if(updateRot){
   			svsCommands.append(SVSCommands.changeRot(handle.getValue(), rot));
   			updateRot = false;
   		}
   		if(updateScale){
   			svsCommands.append(SVSCommands.changeScale(handle.getValue(), scale));
   			updateScale = false;
   		}

		handle.updateWM();

		for(ObjectProperty prop : properties.values()){
			if(!prop.isAdded()){
				prop.addToWM(rootID);
			} else {
				prop.updateWM();
			}
		}
    }

    // Remove the object from the input link and SVS
    public synchronized void removeFromWM(StringBuilder svsCommands){
		if (rootID == null){ return; }
    	for(ObjectProperty prop : properties.values()){
    		prop.removeFromWM();
    	}
    	handle.removeFromWM();
    	rootID.DestroyWME();
    	rootID = null;

    	svsCommands.append(SVSCommands.delete(handle.getValue()));
    }
}
