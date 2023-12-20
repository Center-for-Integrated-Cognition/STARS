package april.tag;

import april.jmat.*;

public class CameraUtil
{
    /** Given a 3x3 homography matrix and the focal lengths of the
     * camera, compute the pose of the tag. The focal lengths should
     * be given in pixels. For example, if the camera's focal length
     * is twice the width of the sensor, and the sensor is 600 pixels
     * across, the focal length in pixels is 2*600. Note that the
     * focal lengths in the fx and fy direction will be approximately
     * equal for most lenses, and is not a function of aspect ratio.
     *
     * Theory: The homography matrix is the product of the camera
     * projection matrix and the tag's pose matrix (the matrix that
     * projects points from the tag's local coordinate system to the
     * camera's coordinate frame).
     *
    * [ h00 h01 h02 h03] = [ fx   0     cx 0 ] [ R00 R01 R02 TX ]
     * [ h10 h11 h12 h13] = [ 0    fy    cy 0 ] [ R10 R11 R12 TY ]
     * [ h20 h21 h22 h23] = [ 0    0      s 0 ] [ R20 R21 R22 TZ ]
     *                                          [ 0   0   0   1  ]
     *
     * fx is the focal length in the x direction of the camera
     * (typically measured in pixels), fy is the focal length. cx and
     * cy give the focal center (usually the middle of the image), and
     * s is either +1 or -1, depending on the conventions you use. (We
     * use 1.)

     * When observing a tag, the points we project in world space all
     * have z=0, so we can form a 3x3 matrix by eliminating the 3rd
     * column of the pose matrix.
     *
     * [ h00 h01 h02 ] = [ fx   0    cx 0 ] [ R00 R01 TX ]
     * [ h10 h11 h12 ] = [ 0    fy   cy 0 ] [ R10 R11 TY ]
     * [ h20 h21 h22 ] = [ 0    0     s 0 ] [ R20 R21 TZ ]
     *                                      [ 0   0   1  ]
     *
     * (note that these h's are different from the ones above.)
     *
     * We can multiply the right-hand side to yield a set of equations
     * relating the values of h to the values of the pose matrix.
     *
     * There are two wrinkles. The first is that the homography matrix
     * is known only up to scale. We recover the unknown scale by
     * constraining the magnitude of the first two columns of the pose
     * matrix to be 1. We use the geometric average scale. The sign of
     * the scale factor is recovered by constraining the observed tag
     * to be in front of the camera. Once scaled, we recover the first
     * two colmuns of the rotation matrix. The third column is the
     * cross product of these.
     *
     * The second wrinkle is that the computed rotation matrix might
     * not be exactly orthogonal, so we perform a polar decomposition
     * to find a good pure rotation approximation.
     *
     * Tagsize is the size of the tag in your desired units. I.e., if
     * your tag measures 0.25m along the side, your tag size is
     * 0.25. (The homography is computed in terms of *half* the tag
     * size, i.e., that a tag is 2 units wide as it spans from -1 to
     * +1, but this code makes the appropriate adjustment.)
     *
     * A note on signs:
     *
     * The code below incorporates no additional negative signs, but
     * respects the sign of any parameters tha tyou pass in. Flipping
     * the signs allows you to modify the projection to suit a wide
     * variety of conditions.
     *
     * In the "pure geometry" projection matrix, the image appears
     * upside down; i.e., the x and y coordinates on the left hand
     * side are the opposite of those on the right of the camera
     * projection matrix. This would happen for all parameters
     * positive: recall that points in front of the camera have
     * negative Z values, which will cause the sign of all points to
     * flip.
     *
     * However, most cameras flip things so that the image appears
     * "right side up" as though you were looking through the lens
     * directly. This means that the projected points should have the
     * same sign as the points on the right of the camera projection
     * matrix. To achieve this, flip fx and fy.
     *
     * One further complication: cameras typically put y=0 at the top
     * of the image, instead of the bottom. Thus you generally want to
     * flip y yet again (so it's now positive again).
     *
     * General advice: you probably want fx negative, fy positive, cx
     * and cy positive, and s=1.
     **/
    public static double[][] homographyToPose(double fx, double fy, double tagSize, double h[][])
    {
        System.out.println("WARNING: this homographyToPose method is broken!");
        System.exit(-1);

        // flip the homography along the Y axis to align the
        // conventional image coordinate system (y=0 at the top) with
        // the conventional camera coordinate system (y=0 at the
        // bottom).
        double F[][] = LinAlg.identity(3);
        F[1][1] = -1;
        F[2][2] = -1;

        h = LinAlg.matrixAB(F, h);

        double M[][] = new double[4][4];
        M[0][0] =  h[0][0] / fx;
        M[0][1] =  h[0][1] / fx;
        M[0][3] =  h[0][2] / fx;
        M[1][0] =  h[1][0] / fy;
        M[1][1] =  h[1][1] / fy;
        M[1][3] =  h[1][2] / fy;
        M[2][0] =  h[2][0];
        M[2][1] =  h[2][1];
        M[2][3] =  h[2][2];

        // Compute the scale. The columns of M should be made to be
        // unit vectors. This is over-determined, so we take the
        // geometric average.
        double scale0 = Math.sqrt(sq(M[0][0]) + sq(M[1][0]) + sq(M[2][0]));
        double scale1 = Math.sqrt(sq(M[0][1]) + sq(M[1][1]) + sq(M[2][1]));
        double scale = Math.sqrt(scale0*scale1);

        M = LinAlg.scale(M, 1.0/scale);

        // recover sign of scale factor by noting that observations must occur in front of the camera.
        if (M[2][3] > 0)
            M = LinAlg.scale(M, -1);

        // The bottom row should always be [0 0 0 1].  We reset the
        // first three elements, even though they must be zero, in
        // order to make sure that they are +0. (We could have -0 due
        // to the sign flip above. This is theoretically harmless but
        // annoying in practice.)
        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = 0;
        M[3][3] = 1;

        // recover third rotation vector by crossproduct of the other two rotation vectors.
        double a[] = new double[] { M[0][0], M[1][0], M[2][0] };
        double b[] = new double[] { M[0][1], M[1][1], M[2][1] };
        double ab[] = LinAlg.crossProduct(a,b);

        M[0][2] = ab[0];
        M[1][2] = ab[1];
        M[2][2] = ab[2];

        // pull out just the rotation component so we can normalize it.
        double R[][] = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                R[i][j] = M[i][j];

        SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(R));
        // polar decomposition, R = (UV')(VSV')
        Matrix MR = svd.getU().times(svd.getV().transpose());
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                M[i][j] = MR.get(i,j);

        // Scale the results based on the scale in the homography. The
        // homography assumes that tags span from -1 to +1, i.e., that
        // they are two units wide (and tall).
        for (int i = 0; i < 3; i++)
            M[i][3] *= tagSize / 2;

        return M;
    }


    /** Given the parameters of the perspective projection and the
     * homography, determine the pose matrix. The pose will be correct
     * when the scale of the model and real-world are the same. Note
     * that in most AprilTag applications, the scale of each tag is
     * fixed to 2.0 m, whereas the user's actual (printed) tag is some
     * other size; use the scalePose method to correct this.
     **/
    public static double[][] homographyToPose(double fx, double fy, double cx, double cy, double h[][])
    {
        double s = 1;

        double M[][] = new double[4][4];
        M[0][0] =  (h[0][0]-cx*h[2][0]) / fx;
        M[0][1] =  (h[0][1]-cx*h[2][1]) / fx;
        M[0][3] =  (h[0][2]-cx*h[2][2]) / fx;
        M[1][0] =  (h[1][0]-cy*h[2][0]) / fy;
        M[1][1] =  (h[1][1]-cy*h[2][1]) / fy;
        M[1][3] =  (h[1][2]-cy*h[2][2]) / fy;
        M[2][0] =  s*h[2][0];
        M[2][1] =  s*h[2][1];
        M[2][3] =  s*h[2][2];
//        M[3][3] =  1; // will get overwritten anyway.

        // Compute the scale. The columns of M should be made to be
        // unit vectors. This is over-determined, so we take the
        // geometric average.
        double scale0 = Math.sqrt(sq(M[0][0]) + sq(M[1][0]) + sq(M[2][0]));
        double scale1 = Math.sqrt(sq(M[0][1]) + sq(M[1][1]) + sq(M[2][1]));
        double scale = Math.sqrt(scale0*scale1);

        M = LinAlg.scale(M, 1.0/scale);

        // recover sign of scale factor by noting that observations
        // must occur in front of the camera. (which is z < 0).
        if (M[2][3] > 0)
            M = LinAlg.scale(M, -1);

        // The bottom row should always be [0 0 0 1].  We reset the
        // first three elements, even though they must be zero, in
        // order to make sure that they are +0. (We could have -0 due
        // to the sign flip above. This is theoretically harmless but
        // annoying in practice.)
        M[3][0] = 0;
        M[3][1] = 0;
        M[3][2] = 0;
        M[3][3] = 1;

        // recover third rotation vector by crossproduct of the other two rotation vectors.
        double a[] = new double[] { M[0][0], M[1][0], M[2][0] };
        double b[] = new double[] { M[0][1], M[1][1], M[2][1] };
        double ab[] = LinAlg.crossProduct(a,b);

        M[0][2] = ab[0];
        M[1][2] = ab[1];
        M[2][2] = ab[2];

        // pull out just the rotation component so we can normalize it.
        double R[][] = new double[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                R[i][j] = M[i][j];

        SingularValueDecomposition svd = new SingularValueDecomposition(new Matrix(R));
        // polar decomposition, R = (UV')(VSV')
        Matrix MR = svd.getU().times(svd.getV().transpose());
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                M[i][j] = MR.get(i,j);

        return M;
    }

    public static double[][] scalePose(double M[][], double modelSize, double realWorldSize)
    {
        double M2[][] = LinAlg.copy(M);
        for (int i = 0; i < 3; i++)
            M2[i][3] *= realWorldSize /  modelSize;

        return M2;
    }

    static final double sq(double v)
    {
        return v*v;
    }
}
