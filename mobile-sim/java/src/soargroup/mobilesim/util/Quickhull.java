package soargroup.mobilesim.util;

import java.util.*;

import april.jmat.*;
import april.jmat.geom.*;
import april.util.*;

/** An implementation of 3D convex hull using the Quickhull algorithm */
public class Quickhull
{
    // XXX NEEDS
    // 1) Plane formulas
    // 2) Distance to plane
    // 3) Ability to isolate boundary edges

    /** A face of the polyhedron */
    static class Facet implements Comparable<Facet>
    {
        public int v0, v1, v2; // Vertices
        HashSet<Integer> vertices = new HashSet<Integer>();

        HashSet<Ridge> ridges = new HashSet<Ridge>();
        double[] p0;    // point on plane
        double[] n;     // normal

        // Bookkeeping
        HashSet<Integer> outsideSet = new HashSet<Integer>();

        /** Specify vertices in CCW order */
        public Facet(int v0_, int v1_, int v2_, ArrayList<double[]> points)
        {
            v0 = v0_;
            v1 = v1_;
            v2 = v2_;

            p0 = points.get(v0);
            double[] t0 = LinAlg.subtract(points.get(v1), p0);
            double[] t1 = LinAlg.subtract(points.get(v2), p0);
            n = LinAlg.normalize(LinAlg.crossProduct(t0, t1));

            vertices.add(v0);
            vertices.add(v1);
            vertices.add(v2);

            ridges.add(new Ridge(v0, v1));
            ridges.add(new Ridge(v1, v2));
            ridges.add(new Ridge(v2, v0));
        }

        /** Add outside point p to our outside set */
        public void addOutsidePoint(int p)
        {
            outsideSet.add(p);
        }

        /** Return the outside set */
        public Set<Integer> getOutsideSet()
        {
            return outsideSet;
        }

        /** Empty the outside set */
        public void clearOutsideSet()
        {
            outsideSet.clear();
        }

        /** Find the most distance point in the outside set, returning
         *  null if no such point exists
         */
        public double[] getFurthestPoint(ArrayList<double[]> points)
        {
            if (outsideSet.size() < 1)
                return null;

            double[] best = null;
            double bestDist = 0;
            for (int i: outsideSet) {
                double d = distance(i, points);
                if (d > bestDist) {
                    best = points.get(i);
                    bestDist = d;
                }
            }

            return best;
        }

        public Set<Ridge> getRidges()
        {
            return ridges;
        }

        public Set<Integer> getVertices()
        {
            return vertices;
        }

        /** Return the distance to the plane specified by the facet to
         *  the query point q. Positive values are "above" the plane
         *  while negative values are "below."
         */
        public double distance(int i, ArrayList<double[]> points)
        {
            if (v0 == i || v1 == i || v2 == i)
                return 0;

            return LinAlg.dotProduct(n, LinAlg.subtract(points.get(i), p0));
        }


        public int compareTo(Facet f)
        {
            if (equals(f))
                return 0;

            if (v0 < f.v0)
                return -1;
            else if (v0 > f.v0)
                return 1;
            else if (v1 < f.v1)
                return -1;
            else if (v1 > f.v1)
                return 1;
            else if (v2 < f.v2)
                return -1;
            return 1;
        }

        public int hashCode()
        {
            Integer h0 = v0;
            Integer h1 = v1;
            Integer h2 = v2;
            return h0.hashCode() ^ h1.hashCode() ^ h2.hashCode();
        }

        public boolean equals(Object o)
        {
            if (o == null)
                return false;
            if (!(o instanceof Facet))
                return false;
            Facet f = (Facet)o;
            return (v0 == f.v0 && v1 == f.v1 && v2 == f.v2) ||
                   (v0 == f.v1 && v1 == f.v2 && v2 == f.v0) ||
                   (v0 == f.v2 && v1 == f.v0 && v2 == f.v1);
        }

        public void print()
        {
            System.out.printf("(%d, %d, %d)\n", v0, v1, v2);
        }
    }

    /** An edge of the polyhedron */
    static class Ridge implements Comparable<Ridge>
    {
        public int v0, v1;

        /** Maintains CCW ordering from Facet */
        public Ridge(int v0_, int v1_)
        {
            v0 = v0_;
            v1 = v1_;
        }

        public int compareTo(Ridge r)
        {
            if (equals(r))
                return 0;
            else if (v0 < r.v0)
                return -1;
            else if (v0 > r.v0)
                return 1;
            else if (v1 < r.v1)
                return -1;
            return 1;
        }

        public int hashCode()
        {
            Integer h0 = v0;
            Integer h1 = v1;
            return h0.hashCode() ^ h1.hashCode();
        }

        public boolean equals(Object o)
        {
            if (o == null)
                return false;
            if (!(o instanceof Ridge))
                return false;
            Ridge r = (Ridge)o;
            return (v0 == r.v0 && v1 == r.v1) ||
                   (v0 == r.v1 && v1 == r.v0);
        }

        public void print()
        {
            System.out.printf("[%d, %d]\n", v0, v1);
        }
    }

    /** Construct a simplex from a set of points. This is the inital shape
     *  that we will build our convex hull around. A good simplex will
     *  contain a large portion of the points, removing them from consideration
     *  as vertices in the convex hull.
     */
    private static HashSet<Facet> constructSimplex(ArrayList<double[]> points)
    {
        HashSet<Integer> pointSet = new HashSet<Integer>();
        for (int i = 0; i < 4; i++) {
            pointSet.add(i);    // Guarantees 4 points to choose from
        }
        Integer[] ex = new Integer[6];
        for (int i = 0; i < ex.length; i++) {
            ex[i] = new Integer(0);
        }
        for (int i = 0; i < points.size(); i++) {
            double[] p = points.get(i);
            if (points.get(ex[0])[0] > p[0])
                ex[0] = i;
            if (points.get(ex[1])[0] < p[0])
                ex[1] = i;
            if (points.get(ex[2])[1] > p[1])
                ex[2] = i;
            if (points.get(ex[3])[1] < p[1])
                ex[3] = i;
            if (points.get(ex[4])[2] > p[2])
                ex[4] = i;
            if (points.get(ex[5])[2] < p[2])
                ex[5] = i;
        }
        for (int i = 0; i < ex.length; i++) {
            pointSet.add(ex[i]);
        }
        ex = pointSet.toArray(new Integer[pointSet.size()]);

        // Find points to make initial baseline. These
        // are the most distant two points.
        int v0 = 0, v1 = 1;
        double dist = 0;
        for (int i = 0; i < ex.length; i++) {
            for (int j = i+1; j < ex.length; j++) {
                double d = LinAlg.distance(points.get(ex[i]),
                                           points.get(ex[j]));
                if (d > dist) {
                    v0 = ex[i];
                    v1 = ex[j];
                    dist = d;
                }
            }
        }

        // Expand the baseline to make a face by finding the point
        // that is the farthest away from the baseline.
        GLine3D line = new GLine3D(points.get(v0), points.get(v1));
        int v2 = -1;
        dist = 0;
        for (int i = 0; i < ex.length; i++) {
            double[] c = line.pointOnLineClosestTo(points.get(ex[i]));
            double d = LinAlg.distance(c, points.get(ex[i]));
            if (d > dist) {
                v2 = ex[i];
                dist = d;
            }
        }

        // Make simplex (order doesn't matter for now, since we don't know
        // which way this facet will face, yet)
        Facet face = new Facet(v0, v1, v2, points);
        dist = 0;
        int v3 = -1;
        int dir = 1;
        for (int i = 0; i < ex.length; i++) {
            double d = face.distance(ex[i], points);
            if (Math.abs(d) > dist) {
                v3 = ex[i];
                dist = Math.abs(d);
                dir = d > 0 ? 1 : -1;
            }
        }


        //System.out.printf("%d %d %d %d\n", v0, v1, v2, v3);
        //assert (v3 != -1);

        // Build the initial hull (in this case, a simplex)
        HashSet<Facet> facets = new HashSet<Facet>();
        if (dir > 0) {
            facets.add(new Facet(v2, v1, v0, points));  // Flip around face
            facets.add(new Facet(v0, v1, v3, points));
            facets.add(new Facet(v1, v2, v3, points));
            facets.add(new Facet(v2, v0, v3, points));
        } else {
            facets.add(face);
            facets.add(new Facet(v2, v1, v3, points));
            facets.add(new Facet(v1, v0, v3, points));
            facets.add(new Facet(v0, v2, v3, points));
        }

        // Initialize outside sets
        HashSet<Integer> unassigned = new HashSet<Integer>();
        for (int i = 0; i < points.size(); i++) {
            unassigned.add(i);
        }
        for (Facet facet: facets) {
            for (int i: unassigned) {
                if (i == v0 || i == v1 || i == v2 || i == v3)
                    continue;   // XXX hack
                if (facet.distance(i, points) > 0)
                    facet.addOutsidePoint(i);
            }
            unassigned.removeAll(facet.getOutsideSet());
        }

        return facets;
    }

    /** Construct the convex hull of a set of 3D points. Must supply a minimum
     *  of 4 non-coplanar points.
     */
    public static Polyhedron3D getHull(ArrayList<double[]> points)
    {
        if (points.size() < 4)
            return new Polyhedron3D();  // XXX Error message?

        HashSet<Facet> facets = constructSimplex(points);

        // Expand hull. For each facet with a non-empty outside set,
        // find the furthest point in its outside set and add it to the hull,
        // updating facets accordingly.
        boolean nonempty = true;
        while (nonempty) {
            nonempty = false;
            //System.out.println("TOTAL FACETS: " + facets.size());
            //for (Facet facet: facets) {
            //    facet.print();
            //}
            for (Facet facet: facets) {
                if (facet.getOutsideSet().size() < 1)
                    continue;
                nonempty = true;

                // Find furthest point from facet
                int furthest = -1;
                double dist = 0;
                for (int i: facet.getOutsideSet()) {
                    double d = facet.distance(i, points);
                    if (d > dist) {
                        furthest = i;
                        dist = d;
                    }
                }
                assert (furthest > -1);
                //System.out.println("CHOSE POINT: "+furthest);

                // Find all visible facets
                // XXX This should be made more efficient by visiting
                // NEIGHBORS of facets that have yet to be visited, but
                // this will get things working
                HashSet<Facet> visibleSet = new HashSet<Facet>();
                for (Facet visible: facets) {
                    if (visible.distance(furthest, points) > 0)
                        visibleSet.add(visible);
                }

                // Find horizon ridges (XXX which could also probably
                // be made more efficient with optimization to the previous
                // step taking locality into account)
                HashSet<Ridge> horizonRidges = new HashSet<Ridge>();
                HashSet<Ridge> otherRidges = new HashSet<Ridge>();
                for (Facet visible: visibleSet) {
                    Set<Ridge> ridges = visible.getRidges();
                    for (Ridge ridge: ridges) {
                        if (horizonRidges.contains(ridge)) {
                            otherRidges.add(ridge);
                        } else {
                            horizonRidges.add(ridge);
                        }
                    }
                }
                //System.out.println(otherRidges.size() + " " + horizonRidges.size());
                horizonRidges.removeAll(otherRidges);
                //System.out.println(otherRidges.size() + " " + horizonRidges.size());

                // Construct new facets
                HashSet<Facet> newFacets = new HashSet<Facet>();
                for (Ridge ridge: horizonRidges) {
                    newFacets.add(new Facet(ridge.v0, ridge.v1, furthest, points));
                }

                // Take points from visible outside sets and
                // add them to new facets' outside sets if applicable
                int size = 0;
                int count = 0;
                for (Facet visible: visibleSet) {
                    size += visible.getOutsideSet().size();
                    for (int i: visible.getOutsideSet()) {
                        for (Facet newFacet: newFacets) {
                            if (newFacet.distance(i, points) > 0) {
                                count++;
                                newFacet.addOutsidePoint(i);
                                break;
                            }
                        }
                    }
                }

                facets.removeAll(visibleSet);
                facets.addAll(newFacets);

                break;
            }
        }

        HashMap<Integer, Integer> vertexMap = new HashMap<Integer, Integer>();
        ArrayList<double[]> vertices = new ArrayList<double[]>();
        ArrayList<int[]> faces = new ArrayList<int[]>();
        for (Facet facet: facets) {
            for (Integer i: facet.getVertices())
                vertexMap.put(i, 0);
        }
        for (Integer i: vertexMap.keySet()) {
            vertexMap.put(i, vertices.size());
            vertices.add(points.get(i));
        }
        for (Facet facet: facets) {
            faces.add(new int[] {vertexMap.get(facet.v0),
                                 vertexMap.get(facet.v1),
                                 vertexMap.get(facet.v2)});
        }

        return new Polyhedron3D(vertices, faces);
    }
}
