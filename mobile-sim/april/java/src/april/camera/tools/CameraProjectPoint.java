package april.camera.tools;

import april.config.*;
import april.camera.*;

import java.io.*;

public class CameraProjectPoint
{
    public static void main(String args[]) throws IOException
    {
        if (args.length != 4) {
            System.out.printf("Usage: java april.camera.tools.CameraProjectPoint <config> <x> <y> <z>\n"+
                              " Projects [x y z] into pixels using calibrated camera config\n");
            System.exit(1);
        }

        Config config = new ConfigFile(args[0]);

        CameraSet camSet = new CameraSet(config.getChild("aprilCameraCalibration"));

        assert(camSet.size() > 0);
        Calibration cal = camSet.getCalibration(0);

        // Read XYZ point in camera frame from cmdline
        double xyz_camera[] = {Double.parseDouble(args[1]),
                               Double.parseDouble(args[2]),
                               Double.parseDouble(args[3])};

        // use calibration to project to pixels. Pass null for camera
        // frame matrix (assumed to be identity)
        double pix_xy[] = CameraMath.project(cal, null, xyz_camera);
        assert(pix_xy.length == 2);

        System.out.printf("Converted xyz [%f, %f, %f] to pixels [%f, %f] using camera %s\n",
                          xyz_camera[0], xyz_camera[1], xyz_camera[2],
                          pix_xy[0], pix_xy[1],
                          camSet.getName(0));
    }
}