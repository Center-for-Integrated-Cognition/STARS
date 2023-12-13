package april.sim;

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

import lcm.lcm.*;

import april.lcmtypes.*;
import april.jmat.*;
import april.jmat.geom.*;
import april.util.*;
import april.vis.*;
import april.lcm.*;
import april.vis.*;

public class Sensors
{
    static Object vocObj = new Object();
    static VisCanvas voc;
    static VisLayer vol;

    public static BufferedImage camera(VisWorld vw,
                                       double eye[], double lookAt[], double up[],
                                       double fovy_degrees,
                                       int width, int height)
    {
        synchronized(vocObj) {
            if (vol == null) {
                vol = new VisLayer(vw);
                vol.backgroundColor = Color.white;
                ((DefaultCameraManager)vol.cameraManager).interfaceMode = 3.0;
                // The following seems to result in a 2.5 cm backward translation of the camera.
                ((DefaultCameraManager)vol.cameraManager).zclip_near = 0.025;
                ((DefaultCameraManager)vol.cameraManager).UI_ANIMATE_MS = 0;
            }
            if (voc == null) {
                voc = new VisCanvas(vol);
                voc.showSizeChanges = false;
            }

            voc.setSize(width, height);
            vol.world = vw;
            ((DefaultCameraManager)vol.cameraManager).perspective_fovy_degrees = fovy_degrees;
            vol.cameraManager.uiLookAt(eye,lookAt,up, false);

            voc.drawSync();
            return ImageUtil.flipVertical(voc.getLatestFrame());
        }
    }

    public static double[] laser(SimWorld sw,
                                 HashSet<SimObject> ignore,
                                 double T[][],
                                 int nranges,
                                 double rad0,
                                 double radstep,
                                 double maxrange)
    {
        double ranges[] = new double[nranges];

        double eye[] = new double[] { T[0][3], T[1][3], T[2][3] };
        double R[][] = LinAlg.copy(T);
        R[0][3] = 0;
        R[1][3] = 0;
        R[2][3] = 0;

        synchronized(sw) {
            for (int i = 0; i < ranges.length; i++) {
                double dir[] = LinAlg.transform(R, new double[] { Math.cos(rad0 + i*radstep),
                                                                  Math.sin(rad0 + i*radstep),
                                                                  0 });

                ranges[i] = collisionDistance(sw, eye, dir, maxrange, ignore);
            }
        }

        return ranges;
    }

    public static double[] variableLaser(SimWorld sw,
                                         HashSet<SimObject> ignore,
                                         double[][] T,
                                         double maxRange,
                                         RayGenerator rg)
    {
        double[] ranges = new double[rg.numRanges()];
        double[] eye = new double[] { T[0][3], T[1][3], T[2][3] };
        double[][] R = LinAlg.copy(T);
        R[0][3] = 0;
        R[1][3] = 0;
        R[2][3] = 0;

        synchronized (sw) {
            for (int i = 0; i < ranges.length; i++) {
                double[] dir = rg.getRay(i);
                dir = LinAlg.transform(R, dir);

                ranges[i] = collisionDistance(sw, eye, dir, maxRange, ignore);
            }
        }

        return ranges;
    }


    public static double[] hoopskirt(SimWorld sw,
                                     HashSet<SimObject> ignore,
                                     double[][] T,
                                     int nranges,
                                     double pitch,
                                     double rad0,
                                     double radstep,
                                     double maxrange)
    {
        double ranges[] = new double[nranges];

        double[] eye = new double[] { T[0][3], T[1][3], T[2][3] };
        double[][] R = LinAlg.copy(T);
        R[0][3] = 0;
        R[1][3] = 0;
        R[2][3] = 0;

        synchronized (sw) {
            for (int i = 0; i < ranges.length; i++) {
                double[] rpy = new double[] {0,0,0,0,pitch,rad0+i*radstep};
                double[] dir = LinAlg.transform(LinAlg.xyzrpyToMatrix(rpy), new double[] {1, 0, 0});
                dir = LinAlg.transform(R, dir);

                ranges[i] = collisionDistance(sw, eye, dir, maxrange, ignore);
            }
        }

        return ranges;
    }



    private static double collisionDistance(SimWorld sw,
                                            double[] eye,
                                            double[] dir,
                                            double maxrange,
                                            HashSet<SimObject> ignore)
    {
        // More efficient version of collisions that takes max range into
        // account to avoid unnecessary math.
        double dist = maxrange;

        // Ground collision check
        if (dir[2] < 0) {
            GRay3D ray = new GRay3D(eye, dir);
            double[] xy0 = ray.intersectPlaneXY();
            dist = Math.min(dist, LinAlg.distance(eye, xy0));
        }

        for (SimObject so: sw.objects) {
            if (ignore != null && ignore.contains(so))
                continue;

            // Non-collidable check
            Shape s = so.getShape();
            if (s.getBoundingRadius() < 0)
                continue;

            // Range check
            double[][] T = so.getPose();
            double[] xyzrpy = LinAlg.matrixToXyzrpy(T);
            double d = LinAlg.distance(xyzrpy, eye, 3) - s.getBoundingRadius();
            if (d > dist)
                continue;

            dist = Math.min(dist, Collisions.collisionDistance(eye, dir, s, T));
        }

        return dist;
    }
}
