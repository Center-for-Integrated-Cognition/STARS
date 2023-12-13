package soargroup.mobilesim.vis;

import javax.swing.*;

import java.util.ArrayList;
import java.awt.*;

import april.vis.*;
import april.jmat.LinAlg;
import april.config.*;

import soargroup.mobilesim.util.*;

public class Model4 extends VisChain
{
    final static int ROBOT_ID = 6; // XXX - Pull this from config?

    // Single storage for flag triangles
    static VisVertexData flagVd = new VisVertexData(new double[][]{{0, 0, 0},
                                                                   {-0.25, 0.075, 0.1},
                                                                   {-0.25, -0.075, 0.1},
                                                                   {0, 0, 0.2}});
    static VisIndexData flagIDat = new VisIndexData(new int[]{0,1,3,
                                                              0,2,3,
                                                              0,1,2,
                                                              1,2,3});
    static VzMesh.Style flagStyle = new VzMesh.Style(Color.blue);

    static final double in2m = 2.54/100;  // convert inches to meters

    public Model4()
    {
        this(null, Color.black, ROBOT_ID);
    }

    public Model4(Config config, Color chassisColor, double opacity)
    {
        this(config, -1, chassisColor, opacity);
    }

    public Model4(Config config, int robotID, Color chassisColor, double opacity)
    {
        // colors
        int alpha = (int) Math.min(255, Math.max(0, (opacity*255)));
        Color gray  = new Color(120, 120, 120, alpha);
        Color cyan  = new Color(  0, 255, 255, alpha);
        Color green = new Color(  0, 255,   0, alpha);
        Color red   = new Color(255,   0,   0, alpha);
        Color black = new Color(  0,   0,   0, alpha);

        if (chassisColor == null)
            chassisColor = new Color(45, 45, 45, alpha);
        else
            chassisColor = new Color(chassisColor.getRed(), chassisColor.getGreen(),
                                     chassisColor.getBlue(), alpha);


        Color textBG =  new Color(255,255,255,0);//chassisColor;//new Color(120,120,120, alpha);
        String label = String.format("<<monospaced-36,scale=.006,dropshadow=%s>>%d",
                                     ColorUtil.toHexString(textBG),robotID);


        Color wheelColor     = chassisColor;
        Color absColor       = new Color(230, 215, 170, alpha);
        VisObject wheel      = new VzCylinder(MagicRobot.WHEEL_RADIUS, MagicRobot.WHEEL_WIDTH,
                                               new VzMesh.Style(wheelColor));
        VisObject axle       = new VzCylinder(0.25*in2m, 7.05*in2m, new VzMesh.Style(gray));
        VisObject motorMount = new VzBox(MagicRobot.MOTOR_MOUNT_SIZE, new VzMesh.Style(gray));

        double cfgRadius  = 0.001;
        double center[] = new double[] {MagicRobot.CENTER_X_OFFSET, 0, 0};

        // double hfscale = 8*in2m / hf.getMaxCharacterHeight();

        if (config != null)
        {
            cfgRadius = config.getDouble("robot.geometry.radius", 0.001);
            center[0] = config.getDoubles("robot.geometry.circles_x", new double[] {center[0]})[0];
            center[1] = config.getDoubles("robot.geometry.circles_y", new double[] {center[1]})[0];
        }
        VzCircle circleFwd = new VzCircle(cfgRadius, new VzMesh.Style(new Color(  0, 255,   0, 128)));
        VzCircle circleBck1 = new VzCircle(cfgRadius, new VzMesh.Style(new Color(  0,   0,   0, 128)));
        VzCircle circleBck2 = new VzCircle(cfgRadius, new VzMesh.Style(new Color(  0,   0,   0, 128)));
        // vis2
        // circleFwd.setThetaRange( -Math.PI/4,  Math.PI/4);
        // circleBck1.setThetaRange( Math.PI/4,    Math.PI);
        // circleBck2.setThetaRange(  -Math.PI, -Math.PI/4);
		
		// AM: Optional - view region parameters
		//double view_angle = Math.PI/2;
		//double view_dist = 3.0;


        add(
            // pose origin
            // new VzCylinder(0.25*in2m, 4*in2m, new VzMesh.Style(cyan)),
            // new VisChain(LinAlg.translate(center[0], center[1], center[2]),
            //              new VzCylinder(0.25*in2m, 4*in2m, new VzMesh.Style(green))),

            // configuration space circles
            new VisChain(LinAlg.translate(MagicRobot.CENTER_X_OFFSET, 0, 0.3*in2m),
                         circleBck1, circleBck2, circleFwd),

            // Translate to robot origin
            LinAlg.translate(0, 0, MagicRobot.ORIGIN_Z_OFFSET_GROUND),

			// AM: Optional 
			// Draw rectangles representing the boundaries of the view region (where the robot can see)
			//new VisChain(
			//	LinAlg.translate(Math.cos(view_angle/2)*view_dist/2, Math.sin(view_angle/2)*view_dist/2, 0.5), 
			//	LinAlg.rotateX(Math.PI/2), LinAlg.rotateY(view_angle/2), 
			//	new VzRectangle(view_dist, 1.0, new VzLines.Style(Color.black, 1.0))),

			//new VisChain(
			//	LinAlg.translate(Math.cos(view_angle/2)*view_dist/2, -Math.sin(view_angle/2)*view_dist/2, 0.5), 
			//	LinAlg.rotateX(Math.PI/2), LinAlg.rotateY(-view_angle/2), 
			//	new VzRectangle(view_dist, 1.0, new VzLines.Style(Color.black, 1.0))),

            // chassis
            new VisChain(LinAlg.translate(MagicRobot.CHASSIS_MAIN_POS),
                         new VzBox(MagicRobot.CHASSIS_MAIN_SIZE, new VzMesh.Style(chassisColor))),
            new VisChain(LinAlg.translate(MagicRobot.CHASSIS_BATTERY_POS),
                         new VzBox(MagicRobot.CHASSIS_BATTERY_SIZE, new VzMesh.Style(chassisColor))),

            // front handle
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_FRONT_POS),
                         new VzBox(MagicRobot.HANDLE_CENTER_SIZE, new VzMesh.Style(chassisColor))),
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_FRONT_TOP_POS),
                         LinAlg.rotateY(Math.PI/6),
                         new VzBox(MagicRobot.HANDLE_OTHER_SIZE, new VzMesh.Style(chassisColor))),
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_FRONT_BOTTOM_POS),
                         LinAlg.rotateY(-Math.PI/6),
                         new VzBox(MagicRobot.HANDLE_OTHER_SIZE, new VzMesh.Style(chassisColor))),

            // rear handle
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_REAR_POS),
                         new VzBox(MagicRobot.HANDLE_CENTER_SIZE, new VzMesh.Style(chassisColor))),
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_REAR_TOP_POS),
                         LinAlg.rotateY(-Math.PI/6),
                         new VzBox(MagicRobot.HANDLE_OTHER_SIZE, new VzMesh.Style(chassisColor))),
            new VisChain(LinAlg.translate(MagicRobot.HANDLE_REAR_BOTTOM_POS),
                         LinAlg.rotateY(Math.PI/6),
                         new VzBox(MagicRobot.HANDLE_OTHER_SIZE, new VzMesh.Style(chassisColor))),

            // wheels
            new VisChain(LinAlg.translate(MagicRobot.WHEEL_POS_R_FRONT),
                         LinAlg.rotateX(Math.PI/2), wheel),
            new VisChain(LinAlg.translate(MagicRobot.WHEEL_POS_L_FRONT),
                         LinAlg.rotateX(Math.PI/2), wheel),
            new VisChain(LinAlg.translate(MagicRobot.WHEEL_POS_R_REAR),
                         LinAlg.rotateX(Math.PI/2), wheel),
            new VisChain(LinAlg.translate(MagicRobot.WHEEL_POS_L_REAR),
                         LinAlg.rotateX(Math.PI/2), wheel),

            // axles
            new VisChain(LinAlg.translate(MagicRobot.AXLE_REAR_X,  MagicRobot.AXLE_CENTER_Y, 0),
                         LinAlg.rotateX(Math.PI/2), axle),
            new VisChain(LinAlg.translate(MagicRobot.AXLE_REAR_X, -MagicRobot.AXLE_CENTER_Y, 0),
                         LinAlg.rotateX(Math.PI/2), axle),
            new VisChain(LinAlg.translate(MagicRobot.AXLE_FRONT_X,  MagicRobot.AXLE_CENTER_Y, 0),
                         LinAlg.rotateX(Math.PI/2), axle),
            new VisChain(LinAlg.translate(MagicRobot.AXLE_FRONT_X, -MagicRobot.AXLE_CENTER_Y, 0),
                         LinAlg.rotateX(Math.PI/2), axle),

            // motor mounts
            new VisChain(LinAlg.translate(MagicRobot.MOTOR_MOUNT_POS_FR),
                         motorMount),
            new VisChain(LinAlg.translate(MagicRobot.MOTOR_MOUNT_POS_FL),
                         motorMount),
            new VisChain(LinAlg.translate(MagicRobot.MOTOR_MOUNT_POS_BR),
                         motorMount),
            new VisChain(LinAlg.translate(MagicRobot.MOTOR_MOUNT_POS_BL),
                         motorMount),

            // sensor head
            new VisChain(LinAlg.translate(MagicRobot.SENSOR_HEAD_POS),
                         new VzBox(MagicRobot.SENSOR_HEAD_SIZE, new VzMesh.Style(chassisColor)),
                         LinAlg.translate(0, 0, MagicRobot.SENSOR_HEAD_SIZE[2] / 2 + 0.0015),
                         new VzBox(6.5*in2m, 6*in2m, 0.125*in2m, new VzMesh.Style(absColor))),

            // estop
            new VisChain(LinAlg.translate(MagicRobot.ESTOP_POS),
                         new VzCylinder(0.75*in2m, 1*in2m, new VzMesh.Style(red))),

            // label
            robotID >=0 ? new VisChain(LinAlg.translate(.03, 0, MagicRobot.ESTOP_POS[2]),
                                       LinAlg.rotateZ(-Math.PI/2),
                                       new VisDepthTest(false,
                                                        new VzText(VzText.ANCHOR.CENTER,label))) : null


            );
    }

    // translucent factory methods
    public static VisObject makeTranslucent()
    {
        return new Model4(null, Color.black, 0.5);
    }

    public static VisObject makeTranslucent(Config config)
    {
        return new Model4(config, Color.black, 0.5);
    }

    public static VisObject makeTranslucent(Config config, Color chassisColor)
    {
        return new Model4(config, chassisColor, 0.5);
    }

    // opaque factory methods
    public static VisObject make()
    {
        return new Model4(null, null, 1);
    }

    public static VisObject make(Config config)
    {
        return new Model4(config, null, 1);
    }

    public static VisObject Model4Head(Config config, double camPan, double camTilt, double hok)
    {
        return Model4Head(config, camPan, camTilt, hok, false);
    }

    public static VisObject Model4Head(Config config, double camPan, double camTilt, double hok, boolean laserEn)
    {
        double poseToPan[][] = ConfigUtil.getRigidBodyTransform(config, "cameraCalibration.extrinsics.poseToPan");
        double panToTilt[][] = ConfigUtil.getRigidBodyTransform(config, "cameraCalibration.extrinsics.panToTilt");
        double tiltToCam[][] = ConfigUtil.getRigidBodyTransform(config, "cameraCalibration.extrinsics.tiltToCam");

        VisObject servo  = new VisChain(LinAlg.translate( 0.00000,  0.01325, -0.01875),
                                        new VzBox(0.02600,  0.04850,  0.03750,
                                                  new VzMesh.Style(new Color(40, 40, 40))));

        VisObject camera = new VisChain(new VzBox(0.043, 0.034, 0.020,
                                                  new VzMesh.Style(new Color(40,40,40))),
                                        LinAlg.translate(0.000, 0.000, 0.017),
                                        new VzCylinder(0.0165,
                                                       0.0340,
                                                       new VzMesh.Style(new Color(40,40,40))));

        VisObject laser  = new VisChain(LinAlg.translate(0.5, 0, 0),
                                        LinAlg.rotateY(Math.PI/2),
                                        new VzCylinder(0.005, 1, new VzMesh.Style(Color.red)));

        VisObject hokuyo = new VisChain(new VzBox(0.060, 0.060, 0.038,
                                                  new VzMesh.Style(new Color(40, 40, 40))),
                                        LinAlg.translate(0.000, 0.000, 0.0215),
                                        new VzBox(0.052, 0.052, 0.005,
                                                  new VzMesh.Style(new Color(50, 50, 50))),
                                        LinAlg.translate(0.000, 0.000, 0.0135),
                                        new VzCylinder(0.0230, 0.030, new VzMesh.Style(new Color(40, 40, 40))),
                                        LinAlg.translate(0.000, 0.000, 0.0195),
                                        new VzBox(0.045, 0.045, 0.009,
                                                  new VzMesh.Style(new Color(235,135,40))),
                                        LinAlg.translate(0.000, 0.000, 0.0090),
                                        new VzBox(0.045, 0.045, 0.009,
                                                  new VzMesh.Style(new Color(40,40,40)))
                                        );

        double cam[][] = LinAlg.multiplyMany(poseToPan,
                                             LinAlg.rotateZ(camPan),
                                             panToTilt,
                                             LinAlg.rotateZ(camTilt),
                                             tiltToCam);
        VisChain c = new VisChain();
        // camera setup
        c.add(new VisChain( poseToPan,
                            servo));
                            //new VzAxes()));

        c.add(new VisChain( poseToPan,
                            LinAlg.rotateZ(camPan),
                            panToTilt,
                            servo));
                            //new VzAxes()));

        c.add(new VisChain( poseToPan,
                            LinAlg.rotateZ(camPan),
                            panToTilt,
                            LinAlg.rotateZ(camTilt),
                            tiltToCam,
                            camera));

        // laser
        if (laserEn)
            c.add(new VisChain( poseToPan,
                                LinAlg.rotateZ(camPan),
                                panToTilt,
                                LinAlg.rotateZ(camTilt),
                                tiltToCam,
                                LinAlg.rotateX(Math.PI/2),
                                LinAlg.rotateZ(Math.PI/2),
                                LinAlg.translate(0, 0, 0.022),
                                laser));

        // hokuyo setup
        c.add(new VisChain( poseToPan,
                            LinAlg.translate( 0.0930, 0.0340,-0.0410),
                            LinAlg.rotateZ(Math.PI),
                            LinAlg.rotateX(-Math.PI/2),
                            servo));

        c.add(new VisChain( poseToPan,
                            LinAlg.translate( 0.0930, 0.0340,-0.0410),
                            LinAlg.rotateZ(Math.PI),
                            LinAlg.rotateX(-Math.PI/2),
                            LinAlg.translate( 0.0000, 0.0000, 0.0340),
                            LinAlg.rotateX(Math.PI/2),
                            LinAlg.rotateZ(-Math.PI),
                            LinAlg.translate( 0.0000, 0.0000,-0.0090),
                            LinAlg.rotateY(-hok),
                            hokuyo));
        return c;
    }


    public static VisObject Model4Flag(boolean displayFlag)
    {
        if (displayFlag)
            return new VisChain(LinAlg.translate(0, 0, (13+18)*in2m),
                                new VzCylinder(0.005, 36*in2m, new VzMesh.Style(Color.lightGray)),
                                           LinAlg.translate(0, 0, 18*in2m),
                                new VzMesh(flagVd,flagVd,flagIDat, VzMesh.TRIANGLES, flagStyle));
        else
            return new VisChain();
    }

    public static void main(String args[])
    {
        JFrame jf = new JFrame();
        VisWorld vw = new VisWorld();
        VisLayer vl = new VisLayer(vw);
VisCanvas vc = new VisCanvas(vl);
        jf.setLayout(new BorderLayout());
        jf.add(vc, BorderLayout.CENTER);
        jf.setSize(200,200);
        jf.setVisible(true);

        VisWorld.Buffer vb = vw.getBuffer("model");
        Config config = Util.getConfig();
//        vb.addBack(new Model4(null, new Color(0, 150, 20), 1));
//        vb.addBack(new Model4(null, new Color(120, 0, 0), 1));
        vb.addBack(new Model4(null, 17, new Color(32, 32, 32), 1));
//        vb.addBack(new Model4(null, new Color(200, 200, 0), 1));

        vb.addBack(Model4Head(config, 0, 0, 0));
        vb.swap();

        vl.cameraManager.uiLookAt(new double[] {1, -.35, 0.64 },
                                  new double[] {-.58, .265, 0 },
                                  new double[] {-.32, .14, .94 }, true);

        vc.setBackground(Color.black);
    }
}
