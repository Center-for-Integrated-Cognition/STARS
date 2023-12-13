package soargroup.mobilesim.commands;

import java.util.*;

import april.jmat.*;
import april.jmat.geom.*;
import april.util.*;

import soargroup.mobilesim.lcmtypes.diff_drive_t;

public class PathControl
{
    // Ported from RobotDriver
    public final static int LEFT = 0;
    public final static int RIGHT = 1;

    public final static double BASELINE_METERS = 0.46;  // Should be in config

    static diff_drive_t last_drive = new diff_drive_t();

    // Get diff drive command to drive the CENTER of the robot along path
    public static diff_drive_t getDiffDrive(double [] pos_center,
                                            double [] orientation,
                                            ArrayList<double[]> path,
                                            PathParams params,
                                            double requestedSpeedLimit,
                                            double dt)
    {
        diff_drive_t diff_drive = new diff_drive_t();

        //by default, use both
        diff_drive.left_enabled = diff_drive.right_enabled = true;
        diff_drive.left = 0;
        diff_drive.right = 0;

        // Stopping conditions:
        //   1) If no poses have come
        //   2) If the path is malformed
        if (pos_center == null || orientation == null || path == null ||
            path.size() < 2 ||
            params.slowdownDist_m < 0.01 ||
            params.minLookahead_m < 0.0001) {
            return diff_drive;
        }

        // Outline
        // 1) Find closest segment to pose, closes point 'nearest'
        // 2) Find 'lookahead' point on path which intersects with a
        //    circle of radius lookahead dist centered at closestPoint
        // 3) Find min speed constraint over speedPreview_m after closestPoint
        // 4) Drive to 'lookahead'


        // 1)
        GLineSegment2D closestSegment = null;
        double closestDist = Double.MAX_VALUE;
        int closestIndex = -1;
        double[] pos = new double[] {pos_center[0], pos_center[1]};
        for (int i = 0; i < path.size()-1; i++) {
            GLineSegment2D segment = new GLineSegment2D(path.get(i), path.get(i+1));
            double dist = segment.distanceTo(pos);
            if (dist < closestDist) {
                closestDist = dist;
                closestSegment = segment;
                closestIndex = i;
            }
        }

        if (closestSegment == null) {
            System.err.println("WRN: Closest segment on path is null. Path likely malformed.");
            return diff_drive;
        }

        double [] closestPoint = closestSegment.closestPoint(pos);

        double lookahead = params.minLookahead_m;
        if (false) { //scoping
            // 1b)  Find the appropriate lookahead distance
            //double prev[] = path.get(closestIndex);
            //double next[] = path.get(closestIndex+1);
            // prev[2] = Math.min(prev[2], 100*params.maxLookahead_m); //Enable doing math on radii
            // next[2] = Math.min(next[2], 100*params.maxLookahead_m);
            //double pDist = LinAlg.distance(prev, closestPoint);
            //double nDist = LinAlg.distance(next, closestPoint);

            //double linear = pDist / (pDist + nDist); //Interpolate linearly

            // lookahead = prev[2]*linear + next[2]*(1.0-linear);
            //lookahead = MathUtil.clamp(lookahead,params.minLookahead_m, params.maxLookahead_m);
        }

        // 2) Find lookahead point. We always use the minLookahead, as our paths are
        // not encoding distances to obstacles at each point
        GCircle2D lookaheadCircle = new GCircle2D(closestPoint, lookahead);
        double[] lookaheadPoint = path.get(path.size()-1); // Default to last point
        for (int i = closestIndex; i < path.size()-1; i++) {
            double[] nextPt = path.get(i+1);
            GLineSegment2D segment = new GLineSegment2D(path.get(i), nextPt);

            ArrayList<double[]> isects = lookaheadCircle.intersect(segment);
            if (isects.size() == 0) {
                continue;
            } else if (isects.size() == 1) {
                lookaheadPoint = isects.get(0);
                if (i == path.size()-2 && LinAlg.distance(closestPoint, nextPt) <= lookahead) {
                    // Corner case for the end of the path
                    lookaheadPoint = nextPt;
                }
            } else if (isects.size() == 2) {
                double[] isect1 = isects.get(0);
                double[] isect2 = isects.get(1);
                if (LinAlg.squaredDistance(isect1, nextPt) < LinAlg.squaredDistance(isect2, nextPt)) {
                    lookaheadPoint = isect1;
                } else {
                    lookaheadPoint = isect2;
                }
            }
        }

        // 2b) find distance to the end of the path, and the combined speed limit
        double[] end = path.get(path.size()-1);
        double endDist = LinAlg.distance(pos, end);
        double endProp = MathUtil.clamp((endDist / params.slowdownDist_m), 0.0, 1.0);
        double endSpeedLimit = endProp*params.maxSpeed_p + (1.0 - endProp)*params.minMotorSpeed_p;

        if (endDist < params.destTolerance_m) //return here?
            endSpeedLimit = 0.0;

        // 3) not used w/o distance to obstacle stuff

        // 4)
        double combinedSpeedLimit = Math.min(endSpeedLimit, requestedSpeedLimit);

        double[] cur_pos = {pos[0], pos[1],
                            LinAlg.quatToRollPitchYaw(orientation)[2]};

        double[] drive = getKProp(cur_pos, lookaheadPoint, params, combinedSpeedLimit, dt);

        diff_drive.left  = drive[LEFT];
        diff_drive.right = drive[RIGHT];
        last_drive = diff_drive;
        return diff_drive;
    }

    /**
     * Local path follower using proportional constants for angle
     * errors and distance.
     */
    private static double[] getKProp(double pos[], double target[],
                                     PathParams params,
                                     double maxSpeed,
                                     double dt)
    {
        assert(maxSpeed <= 1 && maxSpeed >= 0);

        double dx = target[0] - pos[0];
        double dy = target[1] - pos[1];
        double heading = pos[2];

        double angleToTarget = Math.atan2(dy, dx);

        // Compute how far we have to go:
        // 1. Distance to travel
        double rho   = MathUtil.clamp(Math.sqrt(dx*dx + dy*dy),
                                      0.0, params.slowdownDist_m);
        // 2. Heading difference to destination
        double alpha = MathUtil.mod2pi(angleToTarget - heading);

        // 3. Heading different to target heading
        double beta = MathUtil.mod2pi((-alpha) - heading);

        // Compute the forward and turning components of the trajectory separately
        double turn = params.turnRatio_p * alpha;
        double speed = 1.0;

        // When not facing the right direction, turn in place
        if (Math.abs(alpha) > params.turnInPlaceThresh_rad) {
            speed = 0;
            maxSpeed = Math.min(maxSpeed, .4);
        }

        double targetSpeeds[] = {speed - turn*BASELINE_METERS,  // LEFT
                                 speed + turn*BASELINE_METERS}; // RIGHT

        // Compute a norm [1,infty) to make sure we aren't exceeding a speed limit
        double norm = MathUtil.clamp(LinAlg.max(LinAlg.abs(targetSpeeds)),
                                     1.0, Double.MAX_VALUE);

        // Compute a speed scale based on distance to goal, max speed
        // and use norm to ensure that it is feasible
        double scale = maxSpeed / norm;

        double speeds[] = LinAlg.scale(targetSpeeds, scale);
        double max = Math.max(Math.abs(speeds[0]), Math.abs(speeds[1]));
        double r0, r1;
        r0 = r1 = 0;
        if (max != 0) {
            r0 = Math.abs(speeds[0]/max);
            r1 = Math.abs(speeds[1]/max);
        }

        // Don't let speeds change more than .5/second. Make sure to increase
        // speeds proportionally!
        double left = last_drive.left;
        double right = last_drive.right;
        double DELTA = 0.5;
        //speeds[RobotDriver.LEFT] = r0*MathUtil.clamp(speeds[0], left-dt*DELTA, left+dt*DELTA);
        //speeds[RobotDriver.RIGHT] = r1*MathUtil.clamp(speeds[1], right-dt*DELTA, right+dt*DELTA);

        // ensures we don't overheat the motors
        if (Math.abs(speeds[0]) < params.minMotorSpeed_p && Math.abs(speeds[1]) < params.minMotorSpeed_p)
            speeds[0] = speeds[1] = 0.0;

        return speeds;
    }
}
