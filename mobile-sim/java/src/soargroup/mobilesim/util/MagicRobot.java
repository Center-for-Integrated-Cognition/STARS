package soargroup.mobilesim.util;

/**
 * This class defines various physical constants for the MAGIC 2010
 * robots.  These values are used by various modules in order to unify
 * the constants in a single location. Previously each module used its
 * own constants and many of these were wrong.  The origin of the
 * robot is the center of the rear axle and all pos(ition) constants
 * are w.r.t. to this location.  Units are all SI (specifically meters
 * and radians).
 **/

public class MagicRobot
{
    public static final double ORIGIN_Z_OFFSET_GROUND = 0.1;
    public static final double CENTER_X_OFFSET = 0.2; //0.1333;

    // worst case radius of physical robot (from center)
    public static final double RADIUS = 0.46;

    // main chassis is comprised of 2 boxes (main cavity and battery cavity)
    public static final double[] CHASSIS_MAIN_POS     = new double[]{CENTER_X_OFFSET, 0, 0.15125};
    public static final double[] CHASSIS_BATTERY_POS  = new double[]{CENTER_X_OFFSET, 0, 0.03};
    public static final double[] CHASSIS_MAIN_SIZE    = new double[]{0.52,   0.37, 0.1675};
    public static final double[] CHASSIS_BATTERY_SIZE = new double[]{0.45,   0.14, 0.1};

    // handles
    public static final double[] HANDLE_FRONT_POS        = new double[]{CENTER_X_OFFSET+CHASSIS_MAIN_SIZE[0]/2,   0, 0.0606};
    public static final double[] HANDLE_FRONT_TOP_POS    = new double[]{CENTER_X_OFFSET+CHASSIS_MAIN_SIZE[0]/2 - .013125, 0, 0.08820};
    public static final double[] HANDLE_FRONT_BOTTOM_POS = new double[]{CENTER_X_OFFSET+CHASSIS_MAIN_SIZE[0]/2 - .013135, 0, 0.03304};
    public static final double[] HANDLE_REAR_POS         = new double[]{CENTER_X_OFFSET-CHASSIS_MAIN_SIZE[0]/2,  0, 0.0606};
    public static final double[] HANDLE_REAR_TOP_POS     = new double[]{CENTER_X_OFFSET-CHASSIS_MAIN_SIZE[0]/2 + .013135,  0, 0.08820};
    public static final double[] HANDLE_REAR_BOTTOM_POS  = new double[]{CENTER_X_OFFSET-CHASSIS_MAIN_SIZE[0]/2 + .013135,  0, 0.03304};

    public static final double[] HANDLE_CENTER_SIZE = new double[]{0.105,    0.14, 0.04};
    public static final double[] HANDLE_OTHER_SIZE  = new double[]{0.12124,  0.14, 0.0525};

    // sensor head box
    public static final double[] SENSOR_HEAD_SIZE = new double[]{0.1775, 0.1775, 0.1775};
    public static final double[] SENSOR_HEAD_POS  = new double[]{CENTER_X_OFFSET+CHASSIS_MAIN_SIZE[0]/2-SENSOR_HEAD_SIZE[0]/2-.025, 0.005,  0.3125};

    // wheels/axles
    public static final double WHEEL_RADIUS  = 0.125;
    public static final double WHEEL_WIDTH   = 0.10;
    public static final double AXLE_REAR_X   = 0.0;
    public static final double AXLE_FRONT_X  = 0.4;
    public static final double AXLE_CENTER_Y = 0.168275;
    public static final double[] WHEEL_POS_R_FRONT = new double[]{AXLE_FRONT_X, -CHASSIS_MAIN_SIZE[1]/2 - WHEEL_WIDTH/2 - 0.01, 0};
    public static final double[] WHEEL_POS_L_FRONT = new double[]{AXLE_FRONT_X,  -WHEEL_POS_R_FRONT[1], 0};
    public static final double[] WHEEL_POS_R_REAR  = new double[]{0,             WHEEL_POS_R_FRONT[1], 0};
    public static final double[] WHEEL_POS_L_REAR  = new double[]{0,             -WHEEL_POS_R_FRONT[1], 0};

    // motor mounts
    public static final double[] MOTOR_MOUNT_POS_FR = new double[]{AXLE_FRONT_X - 0.0127, -0.14, 0.02065};
    public static final double[] MOTOR_MOUNT_POS_FL = new double[]{AXLE_FRONT_X - 0.0127,  0.14, 0.02065};
    public static final double[] MOTOR_MOUNT_POS_BR = new double[]{0.0127, -0.14, 0.02065};
    public static final double[] MOTOR_MOUNT_POS_BL = new double[]{0.0127,  0.14, 0.02065};
    public static final double[] MOTOR_MOUNT_SIZE  = new double[]{0.1524, 0.0762, 0.0762};

    // estop
    public static final double[] ESTOP_POS = new double[]{0.031, 0.1525, 0.2425};

    // Coarse size as box
    public static final double[] COARSE_SIZE = new double[]{CHASSIS_MAIN_SIZE[0]+2*HANDLE_OTHER_SIZE[0], WHEEL_WIDTH+Math.abs(WHEEL_POS_R_FRONT[1]-WHEEL_POS_L_FRONT[1]), 0.5125}; // from ground

}
