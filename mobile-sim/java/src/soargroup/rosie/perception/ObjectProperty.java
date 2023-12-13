package soargroup.rosie.perception;

import java.util.*;
import java.util.HashMap;

import sml.*;
import edu.umich.rosie.soar.FloatWME;
import edu.umich.rosie.soar.ISoarObject;

/**
 * A category for each object, which contains several possible labels and their confidences
 * 
  * 
 */
public class ObjectProperty implements ISoarObject
{   
    protected String name;
	protected HashMap<String, Double> lastUpdate;
    protected HashMap<String, FloatWME> values;
    
    public ObjectProperty(String propName){
		name = propName;
		lastUpdate = new HashMap<String, Double>();
		values = new HashMap<String, FloatWME>();
    }
    
    public String getPropertyName(){
    	return name;
    }

	public void clearValues(){
		lastUpdate.clear();
	}

	public void addValue(String value, Double confidence){
		lastUpdate.put(value, confidence);
	}

    /**************************************************************
     * Methods for adding to working memory
     **************************************************************/

	protected Identifier propId = null;
    protected Identifier valuesId = null;
    
    public boolean isAdded(){
		return (propId != null);
    }
    
    public void addToWM(Identifier parentId){
		if (propId != null){ return; }

		propId = parentId.CreateIdWME("property");
		propId.CreateStringWME("property-handle", name);

		valuesId = propId.CreateIdWME("values");
		for(Map.Entry<String, Double> e : lastUpdate.entrySet()){
			FloatWME wme = new FloatWME(e.getKey(), e.getValue());
			wme.addToWM(valuesId);
			values.put(e.getKey(), wme);
		}
    }
	
	public void updateWM(){
		if(propId == null){ return; }

		for(Map.Entry<String, Double> e : lastUpdate.entrySet()){
			FloatWME wme = values.get(e.getKey());
			if (wme == null){
				wme = new FloatWME(e.getKey(), e.getValue());
				wme.addToWM(valuesId);
				values.put(e.getKey(), wme);
			} else if(Math.abs(wme.getValue() - e.getValue()) > 0.02){
				wme.setValue(e.getValue());
				wme.updateWM();
			}
		}

    	HashSet<String> oldVals = new HashSet<String>();
    	oldVals.addAll(values.keySet());
		oldVals.removeAll(lastUpdate.keySet());
		for(String val : oldVals){
			values.get(val).removeFromWM();
			values.remove(val);
		}
    }
    
    public void removeFromWM(){
		if(propId != null){ return; }

    	for(FloatWME wme : values.values()){
    		wme.removeFromWM();
    	}
		values.clear();
    	valuesId.DestroyWME();
    	valuesId = null;

    	propId.DestroyWME();
    	propId = null;
    }
}
