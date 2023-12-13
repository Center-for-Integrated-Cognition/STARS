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
import april.vis.VzMesh;
import april.vis.VzRectangle;
import april.vis.VzText;
import april.vis.VzLines;

public class SimRegion extends BaseSimObject
{
	protected static Color onColor = new Color(255, 225, 180);
	protected static Color offColor = new Color(100, 100, 100);
	protected boolean isOn = true;
	
	protected double width = 1.0;
	protected double length = 1.0;
	protected double dist_threshold_sq = 1.0; // If a dist^2 to a point is greater than this, no way it is in the region
	
	protected String handle = null;
	private String label = null;

    public SimRegion(SimWorld sw)
    {
    	super(sw);
		this.collide = false;
    }
    
    public String getHandle(){
    	return handle;
    }
    public void setHandle(String handle){
    	this.handle = handle;
    }

	public String getLabel(){
		return label;
	}
    
	// Given a x,y point, returns the distance between the point and the 
	public double getDistanceSq(double[] p){
		double dx = p[0] - xyzrpy[0];
		double dy = p[1] - xyzrpy[1];
		return (dx*dx + dy*dy);
	}

	// Given a point p with x,y coordinates, 
	//   returns true if the point falls within the 2D region 
	public boolean contains(double[] p){
		double dx = p[0] - xyzrpy[0];
		double dy = p[1] - xyzrpy[1];
		double dist_sq = dx*dx + dy*dy;
		// Early exit case, the point is way too far away to possibly be inside
		if(dist_sq > dist_threshold_sq){ return false; }
		
		// Project the point into the region's coordinate frame (undo rotation)
		double dist = Math.sqrt(dist_sq);
		double theta = Math.atan2(dy, dx);
		double localTheta = theta - xyzrpy[5];
		double xproj = dist * Math.cos(localTheta);
		double yproj = dist * Math.sin(localTheta);
		return (Math.abs(xproj) < width/2 && Math.abs(yproj) < length/2);
	}
	
    public VisChain createVisObject(){
		VisChain vc = new VisChain();

		// Center Rectangle
		vc.add(new VisChain(LinAlg.translate(0, 0, -0.002), new VzRectangle(width, length, new VzMesh.Style(isOn ? onColor : offColor))));

		if(soargroup.mobilesim.MobileGUI.Settings.DRAW_REGION_IDS){
			// Black Border
			vc.add(new VisChain(LinAlg.translate(0, 0, -0.004), new VzRectangle(width + 0.002, length + 0.002, new VzLines.Style(Color.black, 2))));

			// The handle of the location
			vc.add(new VisChain(LinAlg.scale(0.05),
								  new VzText(VzText.ANCHOR.CENTER, String.format("<<black>> %s", handle.replace("wp", "")))));
		}

		return vc;
    }

	public void setLights(boolean isOn){
		this.isOn = isOn;
		this.recreateVisObject();
	}

    public Shape getShape()
    {
    	return new april.sim.SphereShape(0.0);
    }

    /** Restore state that was previously written **/
    public void read(StructureReader ins) throws IOException
    {
    	// handle for the location
    	handle = ins.readString();

		// Center of the region [x, y] and rotation (yaw)
    	xyzrpy = ins.readDoubles();
    	
		// width (dX) and length (dY) of the region 
    	width = ins.readDouble();
    	length = ins.readDouble();
		dist_threshold_sq = (width*width + length*length)/4;
    }

    /** Write one or more lines that serialize this instance. No line
     * is allowed to consist of just an asterisk. **/
    public void write(StructureWriter outs) throws IOException
    {
    	outs.writeString(handle);
    	outs.writeDoubles(xyzrpy);
    	outs.writeDouble(width);
    	outs.writeDouble(length);
    }

    public void setRunning(boolean b){
    	
    }
}
