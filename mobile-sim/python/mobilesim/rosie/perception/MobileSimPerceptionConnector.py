import time
current_time_ms = lambda: int(round(time.time() * 1000))
current_time_us = lambda: int(time.time() * 1000000)

from threading import Lock

from mobilesim.lcmtypes import robot_info_t, object_data_list_t

from pysoarlib import *

from .WorldObjectManager import WorldObjectManager
from .Robot import Robot

class MobileSimPerceptionConnector(AgentConnector):
    # TODO: Implement eye position?
    def __init__(self, client, lcm):
        super().__init__(client)

        self.lcm = lcm
        self.lcm_handler = lambda channel, data: self.message_received(channel, data)
        self.lcm_subscriptions = []
        self.lock = Lock()

        self.objects = WorldObjectManager()
        self.robot = Robot()


    def connect(self):
        super().connect()
        self.lcm_subscriptions.append(self.lcm.subscribe("ROBOT_INFO", self.lcm_handler))
        self.lcm_subscriptions.append(self.lcm.subscribe("DETECTED_OBJECTS", self.lcm_handler))

    def disconnect(self):
        super().disconnect()
        for sub in self.lcm_subscriptions:
            self.lcm.unsubscribe(sub)
        self.lcm_subscriptions = []

    def message_received(self, channel, data):
        self.lock.acquire()
        if channel == "ROBOT_INFO":
            self.robot.update(robot_info_t.decode(data))
        elif channel == "DETECTED_OBJECTS":
            od = object_data_list_t.decode(data)
            self.objects.update(od)
        self.lock.release()

    def on_input_phase(self, input_link):
        svs_commands = []

        self.lock.acquire()

        self.robot.set_moving_status(self.client.connectors["actuation"].get_moving_status())
        self.robot.update_wm(input_link, svs_commands)
        self.objects.update_wm(input_link, svs_commands)
        if len(svs_commands) > 0:
            self.client.agent.SendSVSInput("\n".join(svs_commands))

        self.lock.release()


    def on_init_soar(self):
        svs_commands = []

        self.lock.acquire()

        self.robot.remove_from_wm(svs_commands)
        self.objects.remove_from_wm(svs_commands)
        if len(svs_commands) > 0:
            self.client.agent.SendSVSInput("\n".join(svs_commands))

        self.lock.release()



