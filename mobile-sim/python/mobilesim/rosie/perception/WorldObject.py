from pysoarlib import *

from .ObjectProperty import ObjectProperty

class WorldObject:
    def __init__(self, obj_data):
        super().__init__()
        self.root_id = None
        #root_cat = ""
        #for cls in obj_data.classifications:
        #    prop = ObjectProperty(cls.category).get_name()
        #    #print(prop)
        #    if prop in "category":
        #        root_cat = cls.name
        #        #print(root_cat)
        #        break

        #object_handle = str(root_cat) + "_instance_" + str(obj_data.id)

        self.id = obj_data.id
        self.handle = SoarWME("object-handle", str(self.id))

        #print("sed -i 's/;" + str(self.id) + ";/;" + object_handle + ";/g' $1")
        #print("sed -i 's/t;" + str(self.id) + ";/t;" + object_handle + ";/g' $1")
        #print("sed -i 's/|" + str(self.id) + "|/"+ object_handle + "/g' $1")
        #print(object_handle)
        
        self.bbox_pos = [0, 0, 0]
        self.bbox_rot = [0, 0, 0]
        self.bbox_scl = [0.01, 0.01, 0.01]

        self.pos_changed = True
        self.rot_changed = True
        self.scl_changed = True

        self.visible = True
        self.properties = {}
        self.contents = []

        self.update(obj_data)

    def get_handle(self):
        return self.handle.get_value()

    def get_property(self, prop_name):
        if prop_name not in self.properties:
            return None
        return self.properties[prop_name].get_best_value()


    # Pos: The x,y,z world coordinate of the object
    def get_pos(self):
        return tuple(self.bbox_pos)
    def set_pos(self, pos):
        self.bbox_pos = list(pos)
        self.pos_changed = True

    # Rot: The orientation of the world object, in x,y,z axis rotations
    def get_rot(self):
        return tuple(self.bbox_rot)
    def set_rot(self, rot):
        self.bbox_rot = list(rot)
        self.rot_changed = True

    # Scl: The scale of the world object in x,y,z dims, scl=1.0 means width of 1 unit
    def get_scl(self):
        return tuple(self.bbox_scl)
    def set_scl(self, scl):
        self.bbox_scl = list(scl)
        self.scl_changed = True

    def update(self, obj_data):
        self.visible = obj_data.visible
        self.contents = [ int(i) for i in obj_data.contained_objects ]

        for d in range(3):
            # Only update pos if it has changed by a significant amount
            if abs(self.bbox_pos[d] - obj_data.xyzrpy[d]) > 0.02:
                self.bbox_pos[d] = obj_data.xyzrpy[d]
                self.pos_changed = True

            # Only update rot if it has changed by a significant amount
            if abs(self.bbox_rot[d] - obj_data.xyzrpy[3+d]) > 0.05:
                self.bbox_rot[d] = obj_data.xyzrpy[3+d]
                self.rot_changed = True

            # Only update scale if it was changed by a significant amount
            if abs(self.bbox_scl[d] - obj_data.lenxyz[d]) > 0.02:
                self.bbox_scl[d] = obj_data.lenxyz[d]
                self.scl_changed = True

        for prop in self.properties.values():
            prop.clear_values()
        for cls in obj_data.classifications:
            prop = self.properties.get(cls.category, None)
            if prop is None:
                prop = ObjectProperty(cls.category)
                self.properties[cls.category] = prop
            prop.add_value(cls.name, cls.confidence)

    ### Methods for managing working memory structures ###

    def is_added(self):
        return self.root_id is not None

    def update_wm(self, parent_id, svs_commands):
        # Not visible and not in working memory, do nothing
        if not self.visible and self.root_id is None:
            return

        # Visible but not in working memory, add it to WM
        if self.visible and self.root_id is None:
            self.add_to_wm(parent_id, svs_commands)
            return

        # Not visible but in working memory, remove it from WM
        if not self.visible and self.root_id is not None:
            self.remove_from_wm(svs_commands)
            return

        # Both visible and in working memory, update it
        if self.pos_changed:
            svs_commands.append(SVSCommands.change_pos(str(self.id), self.bbox_pos))
            self.pos_changed = False

        if self.rot_changed:
            svs_commands.append(SVSCommands.change_rot(str(self.id), self.bbox_rot))
            self.rot_changed = False

        if self.scl_changed:
            svs_commands.append(SVSCommands.change_scl(str(self.id), self.bbox_scl))
            self.scl_changed = False

        for prop in self.properties.values():
            prop.update_wm(self.root_id)

    def add_to_wm(self, parent_id, svs_commands):
        if self.root_id is not None: return

        self.root_id = parent_id.CreateIdWME("object")
        self.handle.add_to_wm(self.root_id)
        for prop in self.properties.values():
            prop.add_to_wm(self.root_id)

        svs_commands.append(SVSCommands.add_box(str(self.id), self.bbox_pos, self.bbox_rot, self.bbox_scl))
        svs_commands.append(SVSCommands.add_tag(str(self.id), "object-source", "perception"))

    def remove_from_wm(self, svs_commands):
        if self.root_id is None: return

        for prop in self.properties.values():
            prop.remove_from_wm()

        self.handle.remove_from_wm()
        self.root_id.DestroyWME()
        self.root_id = None

        svs_commands.append(SVSCommands.delete(str(self.id)))



