package soargroup.mobilesim.util;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import april.jmat.*;
import april.jmat.geom.GRay3D;
import april.util.*;
import april.vis.*;

/** A 3D enclosed volume bounded by a set of faces. */
public class Polyhedron3D
{
    ArrayList<double[]> vertices;
    ArrayList<double[]> normals;
    ArrayList<double[]> lines;
    ArrayList<int[]> faces;

    private class Line
    {
        public Integer a, b;

        public Line(int a_, int b_)
        {
            a = Math.min(a_, b_);
            b = Math.max(a_, b_);
        }

        public int hashCode()
        {
            return a.hashCode() ^ b.hashCode();
        }

        public boolean equals(Object o)
        {
            if (o == null)
                return false;
            if (!(o instanceof Line))
                return false;
            Line l = (Line)o;
            return l.a == a && l.b == b;
        }
    }

    public Polyhedron3D()
    {
        this(new ArrayList<double[]>(),
             new ArrayList<int[]>());
    }

    /** Specify a polyhedron based on its defining vertices and
     *  faces. Faces are defined by indexing into vertices. It is
     *  expected that faces are triples, and should be considered
     *  analygous to polygons in 3D rendering
     */
    public Polyhedron3D(ArrayList<double[]> vertices_,
                        ArrayList<int[]> faces_)
    {
        vertices = vertices_;
        faces = faces_;
        normals = new ArrayList<double[]>();
        for (int f = 0; f < faces.size(); f++) {
            int[] face = faces.get(f);
            double[] p0 = vertices.get(face[0]);
            double[] p1 = vertices.get(face[1]);
            double[] p2 = vertices.get(face[2]);
            double[] v0 = LinAlg.subtract(p1, p0);
            double[] v1 = LinAlg.subtract(p2, p0);
            double[] v2 = LinAlg.crossProduct(v0, v1);  // Normal

            normals.add(LinAlg.normalize(v2));
        }

        Set<Line> lineSet = new HashSet<Line>();
        for (int[] face: faces) {
            for (int i = 0; i < face.length; i++) {
                Line l = new Line(face[i], face[(i+1)%face.length]);
                if (lineSet.contains(l))
                    continue;
                lineSet.add(l);
            }
        }

        // XXX Very inefficient use of space, sadly. Feels wasteful after
        // all that effort to compress the vertex storage, but VzLines
        // has a suboptimal implementation. Might be worth going and rewriting?
        lines = new ArrayList<double[]>();
        for (Line l: lineSet) {
            lines.add(vertices.get(l.a));
            lines.add(vertices.get(l.b));
        }
    }

    /** Checks if the query point is contained in the polyhedron. */
    public boolean contains(double[] q)
    {
        // Construct a ray from the query point guaranteed to go through at
        // least one face.
        double[] p0 = vertices.get(faces.get(0)[0]);
        double[] p1 = vertices.get(faces.get(0)[1]);
        double[] p2 = vertices.get(faces.get(0)[2]);
        double[] c0 = new double[] {(p0[0]+p1[0]+p2[0])/3,
                                    (p0[1]+p1[1]+p2[1])/3,
                                    (p0[2]+p1[2]+p2[2])/3};
        GRay3D ray = new GRay3D(q, LinAlg.subtract(c0, q));
        double[] l = ray.getDir();
        double[] pl = ray.getSource();

        // Count intersections with the faces
        int intersections = 0;
        for (int i = 0; i < faces.size(); i++) {
            double[] n = normals.get(i);
            int[] f = faces.get(i);
            double[] pp = vertices.get(f[0]);

            // Distance to intersection
            if (LinAlg.dotProduct(l, n) == 0) {
                continue;   // Parallel
            }

            double d = LinAlg.dotProduct(LinAlg.subtract(pp, pl), n)/
                       LinAlg.dotProduct(l, n);

            if (d <= 0) {
                continue;   // Wrong direction or on plane.
            }

            double[] intersect = ray.getPoint(d);

            // Check to see if point is inside face. If so, increment the
            // intersection counter. The point is inside the face if it is
            // to the left of all of the face line segments.
            //
            // Project the point onto the plane and take the cross product of it
            // with the counterclockwise face boundaries. If all cross products
            // are in the same direction as our normal, then the point is
            // INSIDE the face.
            boolean contained = true;
            for (int j = 0; j < f.length; j++) {
                double[] source = vertices.get(f[j]);
                double[] dest = vertices.get(f[(j+1)%f.length]);
                double[] v0 = LinAlg.subtract(dest, source);
                double[] v1 = LinAlg.subtract(intersect, source);

                double[] cross = LinAlg.normalize(LinAlg.crossProduct(v0, v1));

                if (LinAlg.dotProduct(cross, n) <= 0) {
                    contained = false;
                    break;
                }
            }
            if (contained) {
                intersections++;
            }
        }

        // XXX DEBUG
        /*
        double[] debug0 = pl;
        double[] debug1 = ray.getPoint(10);
        VisVertexData vvd = new VisVertexData();
        vvd.add(debug0);
        vvd.add(debug1);
        VisWorld.Buffer vb = vw.getBuffer("debug-rays");
        Color c = Color.red;
        vb.addBack(new VzLines(vvd, VzLines.LINES,
                               new VzLines.Style(c, 1)));
        vb.swap();

        vb = vw.getBuffer("debug-faces");
        for (Integer f: faceHits) {
            int mod = f%4;
            if (mod == 0)
                c = Color.white;
            else if (mod == 1)
                c = Color.yellow;
            else if (mod == 2)
                c = Color.magenta;
            else if (mod == 3)
                c = Color.red;
            VisVertexData vertexData = new VisVertexData(vertices);
            VisVertexData normalData = new VisVertexData();
            VisIndexData indexData = new VisIndexData();
            normalData.add(normals.get(f));
            indexData.add(faces.get(f));
            vb.addBack(new VzMesh(vertexData,
                                  normalData,
                                  indexData,
                                  VzMesh.TRIANGLES,
                                  new VzMesh.Style(c)));
        }
        vb.swap();

        vb = vw.getBuffer("debug-intersections");
        vb.addBack(new VzPoints(new VisVertexData(planePoints),
                                new VzPoints.Style(Color.cyan, 4)));
        vb.swap();

        vb = vw.getBuffer("debug-normals");
        ArrayList<double[]> normalLines = new ArrayList<double[]>();
        ArrayList<double[]> crossLines = new ArrayList<double[]>();
        for (int f = 0; f < faces.size(); f++) {
            int[] face = faces.get(f);
            double[] centroid = new double[3];
            for (int i = 0; i < face.length; i++) {
                LinAlg.plusEquals(centroid, vertices.get(face[i]), 1.0/face.length);
            }
            normalLines.add(centroid);
            normalLines.add(LinAlg.add(centroid, normals.get(f)));

            if (crossNormals[f][0] != 0) {
                crossLines.add(centroid);
                crossLines.add(LinAlg.add(centroid, LinAlg.scale(crossNormals[f], 0.5)));
            }

        }
        vb.addBack(new VzLines(new VisVertexData(normalLines),
                               VzLines.LINES,
                               new VzLines.Style(Color.cyan, 2)));
        vb.addBack(new VzLines(new VisVertexData(crossLines),
                               VzLines.LINES,
                               new VzLines.Style(Color.magenta, 4)));
        vb.swap();
        */


        return (intersections%2) == 1;
    }

    public ArrayList<double[]> getVertices()
    {
        return vertices;
    }

    public ArrayList<int[]> getFaces()
    {
        return faces;
    }

    public ArrayList<double[]> getNormals()
    {
        return normals;
    }

    public ArrayList<double[]> getLines()
    {
        return lines;
    }

    /** Create this as a renderable VisObject */
    public VisObject getVis(Style ... styles)
    {
        VisVertexData vvd = new VisVertexData(vertices);
        VisVertexData vnd = new VisVertexData(normals);
        VisVertexData vld = new VisVertexData(lines);
        VisIndexData vid = new VisIndexData();
        for (int[] idxs: faces) {
            vid.add(idxs);
        }

        VisChain chain = new VisChain();
        for (Style style: styles) {
            if (style instanceof VzMesh.Style)
                chain.add(new VzMesh(vvd,
                                     vnd,
                                     vid,
                                     VzMesh.TRIANGLES,
                                     (VzMesh.Style)style));
            else if (style instanceof VzPoints.Style)
                chain.add(new VzPoints(vvd, (VzPoints.Style)style));
            else if (style instanceof VzLines.Style)
                chain.add(new VzLines(vld, VzLines.LINES, (VzLines.Style)style));
        }

        // Render the normals
        //ArrayList<double[]> normalPoints = new ArrayList<double[]>();
        //for (int i = 0; i < faces.size(); i++) {
        //    double[] centroid = new double[3];
        //    LinAlg.plusEquals(centroid, vertices.get(faces.get(i)[0]), 1.0/3);
        //    LinAlg.plusEquals(centroid, vertices.get(faces.get(i)[1]), 1.0/3);
        //    LinAlg.plusEquals(centroid, vertices.get(faces.get(i)[2]), 1.0/3);
        //    normalPoints.add(centroid);
        //    normalPoints.add(LinAlg.add(centroid, normals.get(i)));
        //}
        //chain.add(new VzLines(new VisVertexData(normalPoints),
        //                      VzLines.LINES,
        //                      new VzLines.Style(Color.magenta, 2)));
        return chain;
    }

    /** Test Polyhedron */
    static public void main(String[] args)
    {
        GetOpt opts = new GetOpt();
        opts.addBoolean('h',"help",false,"Show this help screen");
        opts.addInt('n',"num-points",100,"Number of query points");

        if (!opts.parse(args)) {
            System.err.println("ERR: Could not parse args - "+opts.getReason());
            System.exit(1);
        }

        if (opts.getBoolean("help")) {
            opts.doHelp();
            System.exit(0);
        }

        // Render a tetrahedron. 4 points, 4 faces.
        ArrayList<double[]> points = new ArrayList<double[]>();
        ArrayList<int[]> idxs = new ArrayList<int[]>();
        points.add(new double[3]);
        points.add(new double[] {1, 1, 0});
        points.add(new double[] {1, 0, 1});
        points.add(new double[] {0, 1, 1});

        // Face 0
        idxs.add(new int[] {0,1,2});
        // Face 1
        idxs.add(new int[] {0,3,1});
        // Face 2
        idxs.add(new int[] {0,2,3});
        // Face 3
        idxs.add(new int[] {1,3,2});

        Polyhedron3D poly = new Polyhedron3D(points, idxs);

        JFrame jf = new JFrame("Polyhedron test");
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jf.setLayout(new BorderLayout());
        jf.setSize(800, 600);

        VisWorld vw = new VisWorld();
        VisLayer vl = new VisLayer(vw);
        VisCanvas vc = new VisCanvas(vl);
        jf.add(vc, BorderLayout.CENTER);

        VzGrid.addGrid(vw);

        VisWorld.Buffer vb = vw.getBuffer("polyhedron");
        vb.addBack(poly.getVis(//new VzMesh.Style(Color.red),
                               new VzLines.Style(Color.cyan, 2),
                               new VzPoints.Style(Color.yellow, 4)));
        vb.swap();

        jf.setVisible(true);

        // Containment checks
        Random r = new Random();
        ArrayList<double[]> in = new ArrayList<double[]>();
        ArrayList<double[]> out = new ArrayList<double[]>();
        for (int i = 0; i < opts.getInt("num-points"); i++) {
            double[] q = new double[] {r.nextGaussian()*.5,
                                       r.nextGaussian()*.5,
                                       r.nextGaussian()*.5};
            if (poly.contains(q)) {
                in.add(q);
            } else {
                out.add(q);
            }
            vb = vw.getBuffer("in-poly");
            vb.addBack(new VzPoints(new VisVertexData(in),
                                    new VzPoints.Style(Color.green, 4)));
            vb.swap();

            vb = vw.getBuffer("out-poly");
            vb.addBack(new VzPoints(new VisVertexData(out),
                                    new VzPoints.Style(Color.red, 4)));
            vb.swap();
            //TimeUtil.sleep(2000);
        }



    }
}
