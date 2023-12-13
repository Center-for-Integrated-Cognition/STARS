import math

from pysoarlib import *


VIEW_DIST = 8.0
VIEW_ANGLE = math.pi/2 * .6
VIEW_HEIGHT = 2.0

# Returns vertices for a 3D triangular region representing the view area
def get_view_region_verts():
    verts = []

    dx = VIEW_DIST/2
    dy = VIEW_DIST * math.sin(VIEW_ANGLE/2)
    dz = VIEW_HEIGHT/2
    # Top triangle
    verts.append("%f %f %f" % (-dx, 0.0, dz))
    verts.append("%f %f %f" % (dx, -dy, dz))
    verts.append("%f %f %f" % (dx, dy, dz))
    # Bottom triangle
    verts.append("%f %f %f" % (-dx, 0.0, -dz))
    verts.append("%f %f %f" % (dx, -dy, -dz))
    verts.append("%f %f %f" % (dx, dy, -dz))

    return " ".join(verts)

class Robot:
    def __init__(self):
        self.pos = [ 0.0, 0.0, 0.0 ]
        self.rot = [ 0.0, 0.0, 0.0 ]

        self.dims = [ 0.5, 0.5, 2.0 ]

        self.pose_id = None
        self.pose_wmes = [ SoarWME(d, 0.0) for d in ['x', 'y', 'z', 'roll', 'pitch', 'yaw' ] ]
        self.update_pose = True

        self.self_id = None
        self.arm_id = None

        self.moving_status = SoarWME("moving-status", "stopped")
        self.held_object = SoarWME("holding-object", "none")
        self.current_wp = SoarWME("current-waypoint", "none")

    def get_held_object(self):
        return self.held_object.get_value()
    
    def set_moving_status(self, status):
        self.moving_status.set_value(status)

    def update(self, robot_info):
        self.held_object.set_value("none" if robot_info.held_object is None else str(robot_info.held_object))
        self.current_wp.set_value(robot_info.current_waypoint)

        for d in range(3):
            if abs(self.pos[d] - robot_info.xyzrpy[d]) > 0.02:
                self.pos[d] = robot_info.xyzrpy[d]
                self.pose_wmes[d].set_value(robot_info.xyzrpy[d])
                self.update_pose = True

            if abs(self.rot[d] - robot_info.xyzrpy[3+d]) > 0.05:
                self.rot[d] = robot_info.xyzrpy[3+d]
                self.pose_wmes[3+d].set_value(robot_info.xyzrpy[3+d])
                self.update_pose = True
    
    ############# Soar Interface Functions #############

    def is_added(self):
        return self.self_id is not None

    def add_to_wm(self, parent_id, svs_commands):
        if self.self_id is not None:
            self.update_wm(parent_id, svs_commands)
            return

        self.self_id = parent_id.CreateIdWME("self")
        self.moving_status.add_to_wm(self.self_id)
        self.current_wp.add_to_wm(self.self_id)

        self.pose_id = self.self_id.CreateIdWME("pose")
        for wme in self.pose_wmes:
            wme.add_to_wm(self.pose_id)

        self.arm_id = self.self_id.CreateIdWME("arm")
        self.arm_id.CreateStringWME("moving-status", "wait")
        self.held_object.add_to_wm(self.arm_id)

        svs_commands.append(SVSCommands.add_node("robot", parent="world", pos=self.pos, rot=self.rot))
        svs_commands.append(SVSCommands.add_node("robot_pos", parent="robot"))
        svs_commands.append(SVSCommands.add_box("robot_body", parent="robot", pos=[0.2, 0.0, 1.0], scl=self.dims))
        svs_commands.append("add robot_view robot v %s p %f %f %f" % \
                ( get_view_region_verts(), VIEW_DIST/2 + .5, 0.0, VIEW_HEIGHT/2-self.pos[2] - 0.1))
    
    def update_wm(self, parent_id, svs_commands):
        if self.self_id is None:
            self.add_to_wm(parent_id, svs_commands)
            return

        self.moving_status.update_wm()
        self.held_object.update_wm()
        self.current_wp.update_wm()

        if self.update_pose:
            for wme in self.pose_wmes:
                wme.update_wm()
            svs_commands.append(SVSCommands.change_pos("robot", self.pos))
            svs_commands.append(SVSCommands.change_rot("robot", self.rot))
            self.update_pose = False
    
    def remove_from_wm(self, svs_commands):
        if self.self_id is None: return

        self.moving_status.remove_from_wm()
        self.held_object.remove_from_wm()
        self.current_wp.remove_from_wm()

        for wme in self.pose_wmes:
            wme.remove_from_wm()
        
        self.self_id.DestroyWME()
        self.self_id = None
        self.arm_id = None
        self.pose_id = None
        
        svs_commands.append("delete robot")
    


