package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.rosie.RosieConstants;
import soargroup.mobilesim.util.ResultTypes.*;

import soargroup.mobilesim.sim.actions.*;
import soargroup.mobilesim.sim.attributes.*;

public class SimLightSwitch extends RosieSimObject {
	private String regionHandle = null;
	private SimRegion region = null;

	protected Activatable activatable;
	
	public SimLightSwitch(SimWorld sw){
		super(sw);
	}

	@Override
	public void init(ArrayList<SimObject> worldObjects){
		boolean isOn = properties.getOrDefault(RosieConstants.ACTIVATION, RosieConstants.ACT_ON)
			.equals(RosieConstants.ACT_ON);

		activatable = new Activatable(this, isOn);
		addAttribute(activatable);

		super.init(worldObjects);

		for(SimObject obj : worldObjects){
			if(!(obj instanceof SimRegion)){
				continue;
			}
			SimRegion r = (SimRegion)obj;
			if(r.getHandle().equals(regionHandle)){
				region = (SimRegion)obj;
				region.setLights(activatable.isOn());
				return;
			}
		}
		if(region == null){
			System.err.println("ERROR: LightSwitch cannot find region " + regionHandle);
		}
	}

	@Override
	public VisChain createVisObject() {
		return new VisChain(
			new VzBox(scale_xyz, new VzMesh.Style(activatable.isOn() ? Color.green : Color.red))
		);
	}

	// Action Handling Rules
	static {
		// SetProp.TurnOn Apply: Set the region lights to on
		ActionHandler.addApplyRule(SetProp.TurnOn.class, new ActionHandler.ApplyRule<SetProp.TurnOn>() {
			public Result apply(SetProp.TurnOn turnon){
				if(turnon.object instanceof SimLightSwitch){
					((SimLightSwitch)turnon.object).region.setLights(true);
				}
				return Result.Ok();
			}
		});
		// SetProp.TurnOff Apply: Set the region lights to off
		ActionHandler.addApplyRule(SetProp.TurnOff.class, new ActionHandler.ApplyRule<SetProp.TurnOff>() {
			public Result apply(SetProp.TurnOff turnoff){
				if(turnoff.object instanceof SimLightSwitch){
					((SimLightSwitch)turnoff.object).region.setLights(false);
				}
				return Result.Ok();
			}
		});
	}

	@Override
    public void read(StructureReader ins) throws IOException {
		super.read(ins);
		// [String] regionHandle
		regionHandle = ins.readString();
	}

	@Override
	public void write(StructureWriter outs) throws IOException {
		super.write(outs);
		outs.writeString(regionHandle);
	}
}
