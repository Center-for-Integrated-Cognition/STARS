package soargroup.mobilesim.sim.attributes;

import java.util.ArrayList;
import java.awt.Color;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.util.ResultTypes.*;

import soargroup.mobilesim.sim.*;
import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.actions.ActionHandler.*;

public class Fillable extends Attribute {
	private String contents;

	public Fillable(RosieSimObject baseObject, String contents){
		super(baseObject);
		this.contents = contents;
	}

	public String getContents(){
		return contents;
	}

	public Color getColor(){
		return Fillable.getLiquidColor(contents);
	}

	public void setContents(String contents){
		this.contents = contents;
		baseObject.setProperty(RosieConstants.CONTENTS, contents);
		baseObject.recreateVisObject();
	}

	public static Color getLiquidColor(String liquid){
		if(liquid.equals(RosieConstants.MILK)){   return new Color(255, 255, 255);  }
		if(liquid.equals(RosieConstants.WATER)){  return new Color(200, 200, 255);  }
		return new Color(100, 100, 100);
	}
}
