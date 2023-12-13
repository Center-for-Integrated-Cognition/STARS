package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.sim.attributes.Openable;

public class SimDoor extends RosieSimObject {
	// Attributes
	private Openable openable;

	// The 2 regions connected by the door
	private String reg1;
	private String reg2;

	public SimDoor(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects) { 
		boolean isOpen = properties.get(RosieConstants.DOOR).equals(RosieConstants.DOOR_OPEN);
		this.collide = !isOpen;

		openable = new Openable(this, isOpen);
		addAttribute(openable);

		super.init(worldObjects);
	}

	@Override
	public void setProperty(String property, String value){
		super.setProperty(property, value);
		if(property.equals(RosieConstants.DOOR)){
			this.collide = value.equals(RosieConstants.DOOR_CLOSED);
		}
	}

	public boolean connectsRegion(SimRegion region){
		if(region == null){
			return false;
		}
		return reg1.equals(region.getHandle()) || reg2.equals(region.getHandle());
	}

	@Override
	public VisChain createVisObject() {
		VisChain c = new VisChain();

		if(openable.isOpen()){
			// Draw two side doorjams
			final double thickness = 0.1;
			c.add(new VisChain(LinAlg.translate(0.0, scale_xyz[1]/2-thickness/2, 0.0), 
						new VzBox(scale_xyz[0], thickness, scale_xyz[1], new VzMesh.Style(color))));
			c.add(new VisChain(LinAlg.translate(0.0, -scale_xyz[1]/2+thickness/2, 0.0), 
						new VzBox(scale_xyz[0], thickness, scale_xyz[1], new VzMesh.Style(color))));
		} else {
			c.add(new VzBox(scale_xyz, new VzMesh.Style(color)));
		}

		return c;
	}
	
	/** Restore state that was previously written **/
	public void read(StructureReader ins) throws IOException
	{
		super.read(ins); 
		reg1 = ins.readString();
		reg2 = ins.readString();
	}

	/** Write one or more lines that serialize this instance. No line
	 * is allowed to consist of just an asterisk. **/
	public void write(StructureWriter outs) throws IOException
	{
		super.write(outs);
		outs.writeString(reg1);
		outs.writeString(reg2);
	}
}

