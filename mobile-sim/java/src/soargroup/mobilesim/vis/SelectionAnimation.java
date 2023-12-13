package soargroup.mobilesim.vis;

import java.awt.*;

import april.jmat.*;
import april.vis.*;
import soargroup.mobilesim.sim.RosieSimObject;
import soargroup.mobilesim.util.*;

public class SelectionAnimation implements VisObject
{
    BoundingBox bbox;

    // Animation
    int topColor = 0x33ffffff;    // translucent white
    int botColor = 0x00ffffff;
    int color = botColor;
    static final double TARGET_TIME = 1;    // [s]
    double AD; // deltas
    int RD;
    int GD;
    int BD;
    int sign = 1;
    double accum = 0;

    public SelectionAnimation(RosieSimObject obj)
    {
        bbox = obj.getBoundingBox();

        AD = (topColor >> 24) - (botColor >> 24);
        RD = ((topColor >> 16) & 0xff) - ((botColor >> 16) & 0xff);
        GD = ((topColor >> 8) & 0xff) - ((botColor >> 8) & 0xff);
        BD = ((topColor >> 0) & 0xff) - ((botColor >> 0) & 0xff);
    }

    /** Take a step forward dt seconds in the animation */
    public void step(double dt)
    {
        accum += dt;
        if (accum > TARGET_TIME) {
            sign *= -1;
            accum -= TARGET_TIME;
        }

        int diff = ((int)(AD*accum/TARGET_TIME) << 24 |
                    (int)(RD*accum/TARGET_TIME) << 16 |
                    (int)(GD*accum/TARGET_TIME) << 8  |
                    (int)(BD*accum/TARGET_TIME) << 0);
        if (sign > 0) {
            color = botColor + diff;
        } else {
            color = topColor - diff;
        }
    }

    public void render(VisCanvas vc, VisLayer vl, VisCanvas.RenderInfo rinfo, GL gl)
    {
        VzBox box = new VzBox(bbox.lenxyz[0]+0.01,
                              bbox.lenxyz[1]+0.01,
                              bbox.lenxyz[2]+0.01,
                              new VzMesh.Style(new Color(color, true)));
        VisChain vch = new VisChain(LinAlg.xyzrpyToMatrix(bbox.xyzrpy),
                                    box);
        vch.render(vc, vl, rinfo, gl);
    }
}
