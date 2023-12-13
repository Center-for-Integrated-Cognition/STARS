from pysoarlib import *

from .WorldObject import WorldObject
from string import digits

class WorldObjectManager:
    def __init__(self):
        self.objects = {}
        self.objs_to_remove = {}

        self.objects_id = None
        self.wm_dirty = False

    def get_object(self, handle):
        return self.objects.get(handle, None)

    def get_object_by_cat(self, cat):
        for obj in self.objects.values():
            if cat in obj.get_property("category"):
                return obj
        return None

    def get_object_container(self, obj):
        if obj is None:
            return None
        for container in self.objects.values():
            if obj.id in container.contents:
                return container
        return None

    def update(self, new_object_data):
        new_ids = set( od.id for od in new_object_data.objects )
        for obj_data in new_object_data.objects:
            obj_id = obj_data.id

            obj = self.objects.get(obj_id, None)
            if obj is None:
                if obj in self.objs_to_remove:
                    # Object was going to be removed
                    # Transfer it to the normal list and update
                    obj = self.objs_to_remove[obj_id]
                    del self.objs_to_remove[obj_id]
                else:
                    # It's a new object, add it to the map
                    obj = WorldObject(obj_data);
                self.objects[obj_id] = obj

            obj.update(obj_data)

        old_ids = [ obj_id for obj_id in self.objects if obj_id not in new_ids ]
        for old_id in old_ids:
            self.objs_to_remove[old_id] = self.objects[old_id]
            del self.objects[old_id]

        self.wm_dirty = True


    #### METHODS TO UPDATE WORKING MEMORY ####

    def is_added(self):
        return self.objects_id is not None

    def add_to_wm(self, parent_id, svs_commands):
        if self.objects_id is not None:
            self.update_wm(parent_id, svs_commands)
            return

        self.objects_id = parent_id.CreateIdWME("objects")
        for obj in self.objects.values():
            obj.update_wm(self.objects_id, svs_commands)

        self.wm_dirty = False

    def update_wm(self, parent_id, svs_commands):
        if self.objects_id is None:
            self.add_to_wm(parent_id, svs_commands)
            return

        for obj in self.objects.values():
            obj.update_wm(self.objects_id, svs_commands)

        for obj in self.objs_to_remove.values():
            obj.remove_from_wm(svs_commands)
        self.objs_to_remove.clear()

        self.wm_dirty = False

    def remove_from_wm(self, svs_commands):
        if self.objects_id is None: return

        for obj in self.objects.values():
            obj.remove_from_wm(svs_commands)

        for obj in self.objs_to_remove.values():
            obj.remove_from_wm(svs_commands)
        self.objs_to_remove.clear()

        self.objects_id.DestroyWME()
        self.objects_id = None
