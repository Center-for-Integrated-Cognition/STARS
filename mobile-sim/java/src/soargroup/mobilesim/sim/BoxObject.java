package soargroup.mobilesim.sim;

import java.awt.Color;
import java.io.IOException;
import java.util.*;

import april.jmat.LinAlg;
import april.util.*;
import april.sim.*;
import april.vis.*;

// This is not a Rosie Object, will not be reported by perception
// It is just a box
public class BoxObject extends BaseSimObject{
	protected Color  color = Color.gray;

	public BoxObject(SimWorld sw){
		super(sw);
	}

	@Override
	public void setXYZRPY(double[] newpose){
		this.xyzrpy = newpose;
	}

	@Override
	public VisChain createVisObject() {
		return new VisChain(new VzBox(scale_xyz, new VzMesh.Style(color)));
	}

	/** Restore state that was previously written **/
	public void read(StructureReader ins) throws IOException {
		super.read(ins); // xyz rot scale_xyz

		// [Int]x3 rgb color
		int rgb[] = ins.readInts();
		color = new Color(rgb[0], rgb[1], rgb[2]);
	}

	/** Write one or more lines that serialize this instance. No line
	 * is allowed to consist of just an asterisk. **/
	public void write(StructureWriter outs) throws IOException {
		super.write(outs);

		int rgb[] = new int[]{ color.getRed(), color.getGreen(), color.getBlue() };
    	outs.writeInts(rgb);
	}
}
