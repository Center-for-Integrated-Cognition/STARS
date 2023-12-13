package soargroup.mobilesim.commands;

import april.config.*;

import soargroup.mobilesim.util.Util;

public class PathParams
{
    public double minSpeed_p;
    public double maxSpeed_p;

    public double speedPreview_m;
    public double previewDistThresh_m;

    public double minLookahead_m;
    public double maxLookahead_m;

    public double turnRatio_p;

    public double turnInPlaceThresh_rad;
    public double slowdownDist_m;
    public double destTolerance_m;

    public double minMotorSpeed_p;

    public static PathParams makeDefault()
    {
        Config config = Util.getConfig();

        PathParams params = new PathParams();
        params.minSpeed_p = config.requireDouble("pathControl.minSpeed_p");
        params.maxSpeed_p = config.requireDouble("pathControl.maxSpeed_p");

        params.speedPreview_m = config.requireDouble("pathControl.speedPreview_m");
        params.previewDistThresh_m = config.requireDouble("pathControl.previewDistThresh_m");

        params.minLookahead_m = config.requireDouble("pathControl.minLookahead_m");
        params.maxLookahead_m = config.requireDouble("pathControl.maxLookahead_m");

        params.turnRatio_p = config.requireDouble("pathControl.turnRatio_p");

        params.turnInPlaceThresh_rad = Math.toRadians(config.requireDouble("pathControl.turnInPlaceThresh_deg"));
        params.slowdownDist_m = config.requireDouble("pathControl.slowdownDist_m");
        params.destTolerance_m = config.requireDouble("pathControl.destTolerance_m");

        params.minMotorSpeed_p = config.requireDouble("pathControl.minMotorSpeed_p");

        return params;
    }
}
