package soargroup.mobilesim.sim.attributes;

import java.util.List;
import soargroup.mobilesim.sim.*;


public class InRegion extends Attribute {
	private SimRegion curRegion = null;
	private boolean staleRegion = true;

	public InRegion(RosieSimObject baseObject){
		super(baseObject);
	}

	// Return the closest region from the list that contains the position of the object
	//   It will cache this result in curRegion and not recompute until its position changes
	public SimRegion getRegion(List<SimRegion> regions){
		if(staleRegion){
			Double bestDist = Double.MAX_VALUE;
			curRegion = null;
			for(SimRegion region : regions){
				double[] xyzrpy = baseObject.getXYZRPY();
				if(region.contains(xyzrpy)){
					double dist2 = region.getDistanceSq(xyzrpy);
					if(dist2 < bestDist){
						bestDist = dist2;
						curRegion = region;
					}
				}
			}
			staleRegion = false;
		}
		return curRegion;
	}

	public void moveHandler(double[] xyzrpy){ 
		staleRegion = true;
	}


}
