import sys

import Python_sml_ClientInterface as sml

from rosie import RosieClient

from mobilesim.rosie import LCMConnector, MobileSimCommandConnector, MobileSimPerceptionConnector, MobileSimActuationConnector

from string import digits
strip_digits = lambda s: s.translate(str.maketrans('', '', digits))

class MobileSimClient(RosieClient):
    def __init__(self, config_filename=None, **kwargs):
        RosieClient.__init__(self, config_filename=config_filename, domain="magicbot", custom_command_connector=True, **kwargs)

        self.lcm_conn = LCMConnector(self)
        self.add_connector("lcm", self.lcm_conn)

        self.actuation = MobileSimActuationConnector(self, self.lcm_conn.lcm)
        self.add_connector("actuation", self.actuation)

        self.perception = MobileSimPerceptionConnector(self, self.lcm_conn.lcm)
        self.add_connector("perception", self.perception)

        self.add_connector("commands", MobileSimCommandConnector(self, self.lcm_conn.lcm))

    def connect(self):
        super().connect()
        if self.has_connector("script") and self.find_help == "custom":
            self.get_connector("script").set_find_helper(lambda msg: self.handle_find_request(msg))

    def handle_find_request(self, msg):
        obj_cat = next(w for w in msg.split() if w[-1] == ',')
        obj_cat = obj_cat.replace(',', '')
        obj = self.perception.objects.get_object_by_cat(obj_cat)
        container = self.perception.objects.get_object_container(obj)
        response = "Unknown."
        if container is not None:
            response = "The {} is in the {}.".format(obj.get_property("category"), container.get_property("category"))
            response = strip_digits(response)
        return response

