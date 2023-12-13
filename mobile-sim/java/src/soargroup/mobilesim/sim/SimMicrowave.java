package soargroup.mobilesim.sim;

import java.awt.Color;
import java.util.ArrayList;
import java.io.IOException;

import april.sim.*;
import april.vis.*;
import april.util.*;
import april.jmat.LinAlg;

import soargroup.mobilesim.vis.VzOpenBox;
import soargroup.rosie.RosieConstants;

public class SimMicrowave extends SimShelves {
	public static final double TEMPERATURE = 150.0;

	public SimMicrowave(SimWorld sw){
		super(sw);
	}

	//// Children can override to implement any dynamics, this is called multiple times/second
	//public void performDynamics(double dt) {
	//	//for(AnchorPoint pt : anchors){
	//	//	if(!pt.hasObject()){
	//	//		continue;
	//	//	}
	//	//	pt.checkObject();
	//	//	if(pt.object != null){
	//	//		pt.object.changeTemperature(TEMPERATURE);
	//	//	}
	//	//}
	//}
}
