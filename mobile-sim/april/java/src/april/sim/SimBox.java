package april.sim;

import java.util.*;
import java.awt.*;
import java.io.*;

import april.vis.*;
import april.jmat.*;
import april.util.*;

public class SimBox implements SimObject
{
    double T[][] = LinAlg.identity(4);  // position
    double last_sxyz[] = new double[3];
    double sxyz[] = new double[3]; // size
    Color  color = Color.gray;
    BoxShape shape = null;
    VisObject obj = null;

    public SimBox(SimWorld sw)
    {
        shape = new BoxShape(sxyz);
        obj = new VzBox(sxyz[0], sxyz[1], sxyz[2], new VzMesh.Style(color));
    }

    public double[][] getPose()
    {
        if (!Arrays.equals(sxyz, last_sxyz))
            return LinAlg.copy(T);
        return T;
    }

    public void setPose(double T[][])
    {
        this.T = LinAlg.copy(T);
    }

    public Shape getShape()
    {
        if (!Arrays.equals(sxyz, last_sxyz))
            shape = new BoxShape(sxyz);
        return shape;
    }

    public VisObject getVisObject()
    {
        if (!Arrays.equals(sxyz, last_sxyz))
            obj = new VzBox(sxyz[0], sxyz[1], sxyz[2], new VzMesh.Style(color));
        return obj;
    }

    /** Restore state that was previously written **/
    public void read(StructureReader ins) throws IOException
    {
        double xyzrpy[] = ins.readDoubles();
        this.T = LinAlg.xyzrpyToMatrix(xyzrpy);

        this.sxyz = ins.readDoubles();
        this.last_sxyz = this.sxyz;
        shape = new BoxShape(sxyz);
        obj = new VzBox(sxyz[0], sxyz[1], sxyz[2], new VzMesh.Style(color));

        double c[] = ins.readDoubles();
        this.color = new Color((float) c[0], (float) c[1], (float) c[2], (float) c[3]);
    }

    /** Write one or more lines that serialize this instance. No line
     * is allowed to consist of just an asterisk. **/
    public void write(StructureWriter outs) throws IOException
    {
        outs.writeDoubles(LinAlg.matrixToXyzrpy(T));
        outs.writeDoubles(sxyz);
        float f[] = color.getRGBComponents(null);
        outs.writeDoubles(LinAlg.copyDoubles(f));
    }

    public void setRunning(boolean b)
    {
    }
}
