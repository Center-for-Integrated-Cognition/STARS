from pysoarlib import *


class ObjectProperty(WMInterface):
    def __init__(self, name):
        super().__init__()
        self.name = name
        self.root_id = None
        self.values_id = None

        self.last_update = {}
        self.values = {}
    
    def get_name(self):
        return self.name

    def clear_values(self):
        self.last_update = {}
    
    def add_value(self, value, conf):
        self.last_update[value] = conf

    def get_best_value(self):
        best_conf = 0.0
        best_val = None
        for val, conf in self.last_update.items():
            if conf > best_conf:
                best_conf = conf
                best_val = val
        return best_val

    ### Methods for managing working memory structures ###

    def _add_to_wm_impl(self, parent_id):
        self.root_id = parent_id.CreateIdWME("property")
        self.root_id.CreateStringWME("property-handle", self.name)

        self.values_id = self.root_id.CreateIdWME("values")
        for val, conf in self.last_update.items():
            self.values[val] = SoarWME(val, conf)
            self.values[val].add_to_wm(self.values_id)
        
    def _update_wm_impl(self):
        for val, conf in self.last_update.items():
            wme = self.values.get(val, None)
            if wme is None:
                wme = SoarWME(val, conf)
                wme.add_to_wm(self.values_id)
                self.values[val] = wme
            elif abs(wme.get_value() - conf) > 0.02:
                wme.set_value(conf)
                wme.update_wm()

        old_vals = [ val for val in self.values if val not in self.last_update ]
        for val in old_vals:
            self.values[val].remove_from_wm()
            del self.values[val]

    def _remove_from_wm_impl(self):
        for val, wme in self.values.items():
            wme.remove_from_wm()
        self.root_id.DestroyWME()
        self.root_id = None
        self.values_id = None
