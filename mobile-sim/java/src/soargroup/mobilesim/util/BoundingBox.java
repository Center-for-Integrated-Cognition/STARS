package soargroup.mobilesim.util;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import april.jmat.*;
//import april.jmat.geom.*;
import april.jmat.geom.GLine2D;
import april.util.*;
import april.vis.*;

public class BoundingBox
{
    static final double DMAX = Double.POSITIVE_INFINITY;
    static final double DMIN = Double.NEGATIVE_INFINITY;

    // Parameters of the box
    public double[] lenxyz;
    public double[] xyzrpy;

    public BoundingBox()
    {
        lenxyz = new double[3];     // Axis-aligned side lengths for the box
        xyzrpy = new double[6];     // Transforms box centered at origin
    }

    public BoundingBox(double[] lenxyz_, double[] xyzrpy_)
    {
        lenxyz = lenxyz_;
        xyzrpy = xyzrpy_;
    }

    /** Compute the volume of the bounding box */
    public double volume()
    {
        return lenxyz[0]*lenxyz[1]*lenxyz[2];
    }

    // XXX DEBUG
    public void print()
    {
        System.out.printf("[%f x %f x %f]\n", lenxyz[0], lenxyz[1], lenxyz[2]);
        System.out.printf("[%f, %f, %f, %f]\n", xyzrpy[0],
                                                xyzrpy[1],
                                                xyzrpy[2],
                                                xyzrpy[5]);
    }

    public VisObject getVis(Style ... styles)
    {
        VzBox box = new VzBox(lenxyz, styles);
        return new VisChain(LinAlg.xyzrpyToMatrix(xyzrpy), box);
    }

	/** Returns true if the given point is inside the bounding box **/
	public boolean containsPoint(double[] xyz){
		// vector from the bbox center to the given point
		double[] to_pt = new double[]{ xyz[0] - xyzrpy[0], xyz[1] - xyzrpy[1], xyz[2] - xyzrpy[2] };

		// invert the bbox rotation and apply it to to_pt to
		// get the point in the bounding box's coordinate frame
		double[] quat = LinAlg.rollPitchYawToQuat(LinAlg.copy(xyzrpy, 3, 3));
		double[] invQuat = LinAlg.quatInverse(quat);
		double[] local_xyz = LinAlg.quatRotate(invQuat, to_pt);

		// once we have the local point, just compare it against the three dimensions
		return 2*Math.abs(local_xyz[0]) <= lenxyz[0] 
			&& 2*Math.abs(local_xyz[1]) <= lenxyz[1] 
			&& 2*Math.abs(local_xyz[2]) <= lenxyz[2];
	}

    /** Return the axis-aligned bounding box for a supplied set of points.
     *  The box is defined by 2 opposition corners.
     */
    static BoundingBox getAxisAligned(ArrayList<double[]> points)
    {
        double[] min = new double[] {DMAX, DMAX, DMAX};
        double[] max = new double[] {DMIN, DMIN, DMIN};

        for (double[] p: points) {
            min[0] = Math.min(min[0], p[0]);
            min[1] = Math.min(min[1], p[1]);
            min[2] = Math.min(min[2], p[2]);
            max[0] = Math.max(max[0], p[0]);
            max[1] = Math.max(max[1], p[1]);
            max[2] = Math.max(max[2], p[2]);
        }

        // XXX TEST ME
        BoundingBox bbox = new BoundingBox();
        bbox.lenxyz = LinAlg.subtract(max, min);
        bbox.xyzrpy = LinAlg.resize(LinAlg.add(LinAlg.scale(bbox.lenxyz, .5),
                                               min),
                                    6);
        return bbox;
    }

    /** Assumes in the XY plane. Returns xlen, ylen, xcenter, ycenter, yaw */
    static private double[] getBoundingRectangle(ArrayList<double[]> hull2D)
    {
        double area = Double.POSITIVE_INFINITY;
        double[] best = new double[5];
        for (int i = 0; i < hull2D.size(); i++) {
            double[] p0 = hull2D.get(i);
            double[] p1 = hull2D.get((i+1)%hull2D.size());
            GLine2D lx = new GLine2D(p0, p1);
            GLine2D ly = lx.perpendicularLine();

            double[] min = new double[] {DMAX, DMAX};
            double[] max = new double[] {DMIN, DMIN};
            for (double[] p: hull2D) {
                double xcoord = lx.getLineCoordinate(p);
                double ycoord = ly.getLineCoordinate(p);
                min[0] = Math.min(min[0], xcoord);
                max[0] = Math.max(max[0], xcoord);
                min[1] = Math.min(min[1], ycoord);
                max[1] = Math.max(max[1], ycoord);
            }

            // Project coordinates back into space to find centroid
            GLine2D ly0 = lx.perpendicularLineThrough(lx.getPointOfCoordinate(min[0]));
            GLine2D ly1 = lx.perpendicularLineThrough(lx.getPointOfCoordinate(max[0]));
            GLine2D lx0 = ly.perpendicularLineThrough(ly.getPointOfCoordinate(min[1]));
            GLine2D lx1 = ly.perpendicularLineThrough(ly.getPointOfCoordinate(max[1]));

            double[] min0 = lx0.intersectionWith(ly0);
            double[] max0 = lx1.intersectionWith(ly1);

            double[] lenxy = LinAlg.subtract(max, min);

            if (area < lenxy[0] * lenxy[1])
                continue;

            area = lenxy[0] * lenxy[1];
            best[0] = lenxy[0];
            best[1] = lenxy[1];

            double[] center = LinAlg.add(min0,
                                         LinAlg.scale(LinAlg.subtract(max0, min0), 0.5));

            best[2] = center[0];
            best[3] = center[1];
            best[4] = lx.getTheta();
        }

        return best;
    }

    /** Return the minimal bounding box for a supplied set of points such that
     *  one side of the box is constrained to be parallel to the XY plane.
     *
     *
     */
    static public BoundingBox getMinimalXY(ArrayList<double[]> points)
    {
        // The minimal bounding box constrained to have a box with a face
        // parallel to the XY plane is roughly equivalent to finding the 2D
        // minimal bounding rectangle. This may be found by first computing
        // the convex hull of the the points without considering Z dimensions.
        // Then, the box max be found by considering all boxes that are
        // co-linear with at least one edge of the hull.
        ArrayList<double[]> hull2D = ConvexHull.getHull2D(points);

        // Run through all of the points to find the spread in Z
        double zmin = DMAX;
        double zmax = DMIN;
        for (double[] p: points) {
//        	System.out.println(String.format("%+.10f,  %+.10f, %+.10f", p[0], p[1], p[2]));
            zmin = Math.min(zmin, p[2]);
            zmax = Math.max(zmax, p[2]);
        }
//        System.out.println("Z MIN: " + zmin + ", ZMAX: " + zmax);
//        System.out.println("DIFF: " + (zmax - zmin));

        // Consider each edge of the polygon forming the hull of our box in
        // turn and compute the minimal bounding box with an edge aligned with
        // said edge.
        double[] rect = getBoundingRectangle(hull2D);

        BoundingBox minBBox = new BoundingBox(new double[] {rect[0],
                                                            rect[1],
                                                            zmax-zmin},
                                              new double[] {rect[2],
                                                            rect[3],
                                                            zmin + (zmax-zmin)*0.5,
                                                            0,
                                                            0,
                                                            rect[4]});
        return minBBox;
    }

    /** Get an approximate minimal bounding box for a set of points.
     *  Exact minimal bounding boxes can be found in O(n^3) time for
     *  a convex hull bounding the points of size n. However, a bounding
     *  box no more than two times this volume may be found with an
     *  incredibly straightforward algorithm in O(n^2). This is an
     *  implementation of the latter.
     **/
    static public BoundingBox getFastMinimal(ArrayList<double[]> points)
    {
        Polyhedron3D hull = ConvexHull.getHull3D(points);
        BoundingBox minBBox = new BoundingBox();

        // Iterate through the faces of the hull. Find the minimal bounding box
        // with a face flush with a face of the hull.
        ArrayList<double[]> vertices = hull.getVertices();
        ArrayList<int[]> faces = hull.getFaces();
        ArrayList<double[]> normals = hull.getNormals();
        double minVolume = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < faces.size(); i++) {
            double[] n = normals.get(i);
            double[] p0 = vertices.get(faces.get(i)[0]);
            double[] p1 = vertices.get(faces.get(i)[1]);
            double[] xaxis = LinAlg.normalize(LinAlg.subtract(p1, p0));
            double[] yaxis = LinAlg.normalize(LinAlg.crossProduct(n, xaxis));

            ArrayList<double[]> projectedPoints = new ArrayList<double[]>();
            double dist = 0;
            for (double[] q: hull.getVertices()) {
                // Find opposite plane face
                double d = Math.abs(LinAlg.dotProduct(LinAlg.subtract(q, p0), n));
                dist = Math.max(dist, d);

                // Project points onto the 2D plane defined by this face
                double[] xy = new double[2];
                xy[0] = LinAlg.dotProduct(LinAlg.subtract(q, p0), xaxis);
                xy[1] = LinAlg.dotProduct(LinAlg.subtract(q, p0), yaxis);
                projectedPoints.add(xy);
            }

            // Find the minimal bounding rectangle for the 2D points.
            double[] rect = getBoundingRectangle(projectedPoints);
            double[] lenxyz = new double[] {rect[0],
                                            rect[1],
                                            dist};
            //double[] center = LinAlg.add(p0, LinAlg.scale(xaxis, rect[2]));
            //LinAlg.plusEquals(center, LinAlg.scale(yaxis, rect[3]));
            //LinAlg.plusEquals(center, LinAlg.scale(n, dist*0.5));
            //double[] xyzrpy = LinAlg.resize(center, 6);
            double[] xyzrpy = new double[6];

            // XXX This is wrong. QuatCompute certainly.
            //double[] quat0 = LinAlg.quatCompute(new double[] {0,0,-1}, n);
            //double[] quat1 = LinAlg.angleAxisToQuat(-rect[4], n);
            //double[] rpy = LinAlg.quatToRollPitchYaw(LinAlg.quatMultiply(quat0,
            //                                                             quat1));
            //xyzrpy[3] = rpy[0];
            //xyzrpy[4] = rpy[1];
            //xyzrpy[5] = rpy[2];


            BoundingBox bbox = new BoundingBox(lenxyz,
                                               xyzrpy);
            

            if (minBBox.volume() <= 0 || minBBox.volume() > bbox.volume())
                minBBox = bbox;
        }

        return minBBox;
    }

    // Utility for random point generation.
    // It would be cool if this took parameters
    static public MultiGaussian makeRandomGaussian3D(Random r)
    {
        double[][] P = new double[3][3];
        double[] u = new double[3];

        // Generate mean XXX
        //u[0] = r.nextGaussian()*5;
        //u[1] = r.nextGaussian()*5;
        //u[2] = r.nextGaussian()*5;

        // Generate diagonals
        P[0][0] = Math.abs(r.nextGaussian()*10);
        P[1][1] = Math.abs(r.nextGaussian()*10);
        P[2][2] = Math.abs(r.nextGaussian()*10);

        System.out.printf("u: [%f, %f, %f]\n", u[0], u[1], u[2]);
        System.out.printf("P: [%f, %f, %f]\n"+
                          "   [%f, %f, %f]\n"+
                          "   [%f, %f, %f]\n", P[0][0], P[0][1], P[0][2],
                                               P[1][0], P[1][1], P[1][2],
                                               P[2][0], P[2][1], P[2][2]);

        return new MultiGaussian(P, u);
    }
    
    static public double estimateIntersectionVolume(BoundingBox a, BoundingBox b){
    	return estimateIntersectionVolume(a, b, 8);
    }
    
    static public double estimateIntersectionVolume(BoundingBox a, BoundingBox b, int samplesPerDim){
    	// One of the volumes is 0, return 0
    	if(LinAlg.min(a.lenxyz) < .000000001){
    		return 0;
    	}
    	if(LinAlg.min(b.lenxyz) < .000000001){
    		return 0;
    	}
    	// quick test to see if there's even a possibility of intersection
    	
    	// Bounding sphere radius of each box, squared
    	// (Largest edge = c, radius = sqrt(c^2 + c^2 + c^2) = sqrt(3c^2)
    	double ra2 = 3*Math.pow(LinAlg.max(a.lenxyz)/2, 2);
    	double rb2 = 3*Math.pow(LinAlg.max(b.lenxyz)/2, 2);

    	// Squared Distance between the centers
    	double dx = a.xyzrpy[0] - b.xyzrpy[0];
    	double dy = a.xyzrpy[1] - b.xyzrpy[1];
    	double dz = a.xyzrpy[2] - b.xyzrpy[2];
    	double d2 = dx*dx + dy*dy + dz*dz;
    	
    	if(d2 > ra2 + rb2){
    		// No possible intersection, return 0;
    		return 0;
    	}
    	
    	// Caculate the transformation matrix from b into a's coordinate frame
    	double[][] m = LinAlg.identity(4);
    	
    	// First, transform from box b into world coords
    	m = LinAlg.matrixAB(LinAlg.scale(b.lenxyz[0], b.lenxyz[1], b.lenxyz[2]), m);
    	m = LinAlg.matrixAB(LinAlg.xyzrpyToMatrix(b.xyzrpy), m);
    	
    	// Then transform from world into box a
    	m = LinAlg.matrixAB(LinAlg.translate(-a.xyzrpy[0], -a.xyzrpy[1], -a.xyzrpy[2]), m);
    	double[] qa = LinAlg.rollPitchYawToQuat(new double[]{a.xyzrpy[3], a.xyzrpy[4], a.xyzrpy[5]});
    	m = LinAlg.matrixAB(LinAlg.quatToMatrix(LinAlg.quatInverse(qa)), m);
    	m = LinAlg.matrixAB(LinAlg.scale(1/a.lenxyz[0], 1/a.lenxyz[1], 1/a.lenxyz[2]), m);
    	
    	double numSamples = 0;
    	double numContained = 0;
    	
    	double sampleWidth = 1.0/samplesPerDim;
    	double sampleStart = -.5 + sampleWidth/2;
    	double[] pb = new double[]{0, 0, 0, 1};
    	for(int i = 0; i < samplesPerDim; i++){
    		pb[0] = sampleStart + i * sampleWidth;
    		for(int j = 0; j < samplesPerDim; j++){
    			pb[1] = sampleStart + j * sampleWidth;
    			for(int k = 0; k < samplesPerDim; k++){
    				pb[2] = sampleStart + k * sampleWidth;
    				double[] pa = LinAlg.matrixAB(m, pb);
    				//System.out.println("Sample " + numSamples);
    				//System.out.println("  B: " + pb[0] + ", " + pb[1] + ", " + pb[2]);
    				//System.out.println("  A: " + pa[0] + ", " + pa[1] + ", " + pa[2]);
    				if(pa[0] >= -.5 && pa[0] <= .5 && 
    					pa[1] >= -.5 && pa[1] <= .5 && 
    					pa[2] >= -.5 && pa[2] <= .5){
    					numContained++;
    				}
    				numSamples++;
    			}

    		}
    	}
    	
    	return (numContained/numSamples) * b.lenxyz[0] * b.lenxyz[1] * b.lenxyz[2];
    }

    /** Test bounding boxes */
    static public void main(String[] args)
    {
        JFrame jf = new JFrame("Bounding Box Test");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        jf.setSize(800,600);

        final Random r = new Random(1);
        final VisWorld vw = new VisWorld();
        VzGrid.addGrid(vw);
        VisLayer vl = new VisLayer(vw);
        VisCanvas vc = new VisCanvas(vl);
        jf.add(vc, BorderLayout.CENTER);

        ParameterGUI pg = new ParameterGUI();
        // Covariance stuff
        pg.addIntSlider("num", "Num Points", 4, 10000, 100);
        pg.addButtons("generate", "Generate Box");
        pg.addBoolean("box_align", "Axis-aligned", true);
        pg.addBoolean("box_minz", "Minimal XY", false);
        pg.addBoolean("box_min", "Minimal all", false);
        pg.addListener(new ParameterListener() {
            public void parameterChanged(ParameterGUI pg, String name) {
                if (name.contains("box_")) {
                    pg.sb("box_align", name.equals("box_align"));
                    pg.sb("box_minz", name.equals("box_minz"));
                    pg.sb("box_min", name.equals("box_min"));
                }

                if (name.equals("generate")) {
                    // Make points and box them
                    MultiGaussian mg = makeRandomGaussian3D(r);
                    ArrayList<double[]> points = mg.sampleMany(r,
                                                               pg.gi("num"));

                    // Render points
                    VisWorld.Buffer vb = vw.getBuffer("points");
                    vb.addBack(new VzPoints(new VisVertexData(points),
                                            new VzPoints.Style(Color.red, 2)));
                    vb.swap();

                    // Create the bound box
                    BoundingBox bbox = new BoundingBox();
                    Tic tic = new Tic();
                    if (pg.gb("box_align"))
                        bbox = getAxisAligned(points);
                    else if (pg.gb("box_minz"))
                        bbox = getMinimalXY(points);
                    else if (pg.gb("box_min"))
                        bbox = getFastMinimal(points);
                    else
                        System.err.println("ERR: unrecognized bounding box type");
                    //bbox.print();
                    double time = tic.toc();
                    System.out.printf("Computed bounding box in %f s\n", time);

                    vb = vw.getBuffer("bbox");
                    vb.addBack(bbox.getVis(new VzLines.Style(Color.yellow, 1)));
                    vb.swap();

                }
            }
        });
        jf.add(pg, BorderLayout.SOUTH);

        jf.setVisible(true);
    }
}
