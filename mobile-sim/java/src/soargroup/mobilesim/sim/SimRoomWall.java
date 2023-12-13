package soargroup.mobilesim.sim;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import soargroup.mobilesim.util.BoundingBox;
import april.jmat.LinAlg;
import april.sim.BoxShape;
import april.sim.Shape;
import april.sim.SimObject;
import april.sim.SimWorld;
import april.util.StructureReader;
import april.util.StructureWriter;
import april.vis.VisChain;
import april.vis.VisObject;
import april.vis.VzBox;
import april.vis.VzMesh;
import april.vis.VzRectangle;
import april.vis.VzText;

public class SimRoomWall extends BaseSimObject {
	protected static Color  color = new Color(100, 100, 100);//Color.gray;   // default wall color
	protected static double height = 1.5;         // default wall height (meters)
	protected static double thickness = 0.2;	  // default wall thickness (meters)
	
	protected double[] p1 = new double[]{ -1.0,  0.0 }; // first endpoint of the wall
	protected double[] p2 = new double[]{  1.0,  0.0 }; // second endpoint of the wall
	protected double length = 2.0;
	
    public SimRoomWall(SimWorld sw){
    	super(sw);
    }

	@Override
	public Shape getShape(){
		if(!soargroup.mobilesim.MobileSimulator.Settings.COLLIDE_WALLS){
			return new april.sim.SphereShape(0.0);
		} 
		return super.getShape();
	}
    
    public VisChain createVisObject(){
		VisChain vc = new VisChain();
    	vc.add(new VzBox(length, thickness, height, new VzMesh.Style(color)));
		return vc;
    }

    /** Restore state that was previously written **/
    public void read(StructureReader ins) throws IOException {
    	// 1st end point [x, y]
        p1 = ins.readDoubles();
    	
    	// 2nd end point [x, y]
        p2 = ins.readDoubles();
        
        length = LinAlg.distance(p1, p2) + thickness;
        
        double x = (p1[0] + p2[0])/2;
        double y = (p1[1] + p2[1])/2;
        double z = height/2;
        double yaw = Math.atan2(p2[1] - p1[1], p2[0] - p1[0]);
        double[] xyzrpy = new double[]{x, y, z, 0.0, 0.0, yaw};
		this.setXYZRPY(xyzrpy);
    }

    /** Write one or more lines that serialize this instance. No line
     * is allowed to consist of just an asterisk. **/
    public void write(StructureWriter outs) throws IOException
    {
    	outs.writeDoubles(p1);
    	outs.writeDoubles(p2);
    }
}
