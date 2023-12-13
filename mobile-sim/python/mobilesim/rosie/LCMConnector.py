import threading
import time
import select

import lcm

from pysoarlib import *

class LCMConnector(AgentConnector):
    def __init__(self, agent):
        super().__init__(agent)

        self.lcm = lcm.LCM()
        self.lcm_thread = None
        self.stop_lcm = True

    def connect(self):
        super().connect()
        if self.lcm_thread is None:
            self.stop_lcm = False
            self.lcm_thread = threading.Thread(target=LCMConnector.lcm_handle_thread, args=(self,))
            self.lcm_thread.start()

    def disconnect(self):
        if self.lcm_thread is not None:
            self.stop_lcm = True
            self.lcm_thread.join()
            self.lcm_thread = None
        super().disconnect()
    
    def lcm_handle_thread(self):
        while not self.stop_lcm:
            rfds, wfds, efds = select.select([self.lcm.fileno()], [], [], 0.2)
            if rfds:
                self.lcm.handle()

    def on_input_phase(self, input_link):
        pass

    def on_init_soar(self):
        pass


