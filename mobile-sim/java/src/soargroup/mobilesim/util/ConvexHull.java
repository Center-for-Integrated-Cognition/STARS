package soargroup.mobilesim.util;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import april.jmat.*;
import april.jmat.geom.*;
import april.vis.*;
import april.util.*;

public class ConvexHull
{
    static final double DMAX = Double.POSITIVE_INFINITY;
    static final double DMIN = Double.NEGATIVE_INFINITY;

    static class GrahamComparator implements Comparator<double[]>
    {
        double[] o;

        public GrahamComparator(double[] o_)
        {
            o = o_;
        }

        public int compare(double[] a, double[] b)
        {
            // If one of the points is the origin, it is always the lower
            if (Arrays.equals(a, o))
                return -1;
            else if (Arrays.equals(b, o))
                return 1;

            // Compare angles to the x-axis based on origin o. Lower is less.
            double ta = Math.atan2(a[1]-o[1], a[0]-o[0]);
            double tb = Math.atan2(b[1]-o[1], b[0]-o[0]);
            if (ta < tb)
                return -1;
            else if (ta > tb)
                return 1;
            return 0;
        }

        public boolean equals(Object obj)
        {
            if (obj == null)
                return false;
            if (!(obj instanceof GrahamComparator))
                return false;
            GrahamComparator gc = (GrahamComparator)obj;
            return Arrays.equals(o, gc.o);
        }
    }

    /** Compute the convex hull for a set of 2D points. There must be at
     *  least 3 non-co-linear points to form a hull. In no such points
     *  exist, then an empty hull will be returned.
     *
     *  XXX Should return a polygon
     */
    static public ArrayList<double[]> getHull2D(ArrayList<double[]> points)
    {
        if (points == null || points.size() < 3)
            return new ArrayList<double[]>();

        // Step 0: Filter points. Find the most extreme points in X and Y and
        // create a polygon from those. Use this polygon to discard all points
        // inside the polygon (which is, itself, a convex hull to THOSE points)
        double[] minx = new double[] {DMAX, 0, 0};
        double[] maxx = new double[] {DMIN, 0, 0};
        double[] miny = new double[] {0, DMAX, 0};
        double[] maxy = new double[] {0, DMIN, 0};
        for (double[] p: points) {
            minx = min(minx, p, 0);
            maxx = max(maxx, p, 0);
            miny = min(miny, p, 1);
            maxy = max(maxy, p, 1);
        }
        Set<double[]> extrema = new LinkedHashSet<double[]>();
        extrema.add(minx);
        extrema.add(maxy);
        extrema.add(maxy);
        extrema.add(miny);

        ArrayList<double[]> valid = new ArrayList<double[]>();

        // Filter out points, if appropriate. This hugely helps performance for
        // large numbers of points.
        if (extrema.size() > 2) {
            // Filter points
            april.jmat.geom.Polygon filter = new april.jmat.geom.Polygon(new ArrayList<double[]>(extrema));
            for (double[] p: points) {
                if (extrema.contains(p) || !filter.contains(p)) {
                    valid.add(p);
                }
            }
        } else {
            valid = points;
        }

        // Construct hull from valid points
        return grahamScan(valid);
    }

    /** Use Graham's scan algorithm to find the 2D convex hull of a set of points.
     *  This algorithm runs in O(n log n) for the number of points scanned. The
     *  cost is dominated by an initial sorting operation.
     *
     *  Returns a set of points that make the convex hull.
     */
    static public ArrayList<double[]> grahamScan(ArrayList<double[]> points)
    {
        assert (points.size() > 2);

        ArrayList<double[]> hull = new ArrayList<double[]>();

        // Find the point with the minimum y coordinate, ties broken by minimum
        // x coordinates.
        double y = DMAX, x = DMAX;
        double[] o = null;
        for (double[] p: points) {
            if (p[1] < y || (p[1] == y && p[0] < x)) {
                y = p[1];
                x = p[0];
                o = p;
            }
        }

        Collections.sort(points, new GrahamComparator(o));
        hull.add(points.get(0));
        hull.add(points.get(1));
        hull.add(points.get(2));
        for (int i = 3; i < points.size();) {
            int size = hull.size();
            double[] p0 = hull.get(size-2);
            double[] p1 = hull.get(size-1);
            double[] q = points.get(i);

            if (LinAlg.pointLeftOf(p0, p1, q)) {
                hull.add(q);
                i++;
            } else {
                hull.remove(size-1);
            }
        }

        return hull;
    }

    /** Compute the convex hull of a set of 3D points. There must be at least
     *  4 non-co-planar points to form a hull. If no such points exist, then
     *  an empty hull will be returned.
     *
     *  Returns a Polyhedron representing the hull
     */
    static public Polyhedron3D getHull3D(ArrayList<double[]> points)
    {
        if (points == null || points.size() < 4)
            return new Polyhedron3D();

        return Quickhull.getHull(points);
    }

    /** Return the minimum double array based on the value at index i. */
    static public double[] min(double[] a, double[] b, int i)
    {
        assert (a.length > i);
        assert (b.length > i);

        if (a[i] > b[i])
            return b;
        else
            return a;

    }

    /** Return the maximum double array based on the value at index i. */
    static public double[] max(double[] a, double[] b, int i)
    {
        assert (a.length > i);
        assert (b.length > i);

        if (a[i] < b[i])
            return b;
        else
            return a;
    }

    static public void main(String[] args)
    {
        JFrame jf = new JFrame("Convex Hull Test");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        jf.setSize(800,600);

        final VisWorld vw = new VisWorld();
        VisLayer vl = new VisLayer(vw);
        VisCanvas vc = new VisCanvas(vl);
        jf.add(vc, BorderLayout.CENTER);

        ParameterGUI pg = new ParameterGUI();
        // Covariance stuff
        pg.addIntSlider("num", "Num Points", 4, 10000, 6);
        pg.addDoubleSlider("xy", "Cov_xy", -1.9, 1.9, 0);
        pg.addDoubleSlider("xz", "Cov_xz", -2.9, 2.9, 0);
        pg.addDoubleSlider("yz", "Cov_yz", -1.9, 1.9, 0);
        pg.addButtons("generate", "Generate Hulls");
        pg.addListener(new ParameterListener() {
            public void parameterChanged(ParameterGUI pg, String name) {
                if (name.equals("generate")) {
                    // Make points and box them
                    double[][] P = new double[3][3];
                    P[0][0] = 5;
                    P[1][1] = 2;
                    P[2][2] = 3;
                    P[0][1] = pg.gd("xy");
                    P[0][2] = pg.gd("xz");
                    P[1][0] = pg.gd("xy");
                    P[1][2] = pg.gd("yz");
                    P[2][0] = pg.gd("xz");
                    P[2][1] = pg.gd("yz");

                    double[] u = new double[3];
                    MultiGaussian mg = new MultiGaussian(P, u);
                    ArrayList<double[]> points = mg.sampleMany(new Random(),
                                                               pg.gi("num"));

                    // Render points
                    VisWorld.Buffer vb = vw.getBuffer("points");
                    vb.addBack(new VzPoints(new VisVertexData(points),
                                            new VzPoints.Style(Color.red, 2)));
                    vb.swap();

                    // Create the 2D hull
                    ArrayList<double[]> hull2D = getHull2D(points);
                    vb = vw.getBuffer("hull2D");
                    vb.addBack(new VzLines(new VisVertexData(hull2D),
                                           VzLines.LINE_LOOP,
                                           new VzLines.Style(Color.yellow, 2)));
                    vb.swap();

                    vb = vw.getBuffer("hull3D");
                    Polyhedron3D hull3D = getHull3D(points);
                    vb.addBack(hull3D.getVis(new VzLines.Style(Color.cyan, 1)));

                    vb.swap();

                }
            }
        });
        jf.add(pg, BorderLayout.SOUTH);

        jf.setVisible(true);
    }
}
