from tkinter import *
import tkinter.font

import time
current_time_us = lambda: int(time.time() * 1000000)
import threading

import lcm
from mobilesim.lcmtypes import rosie_agent_command_t
import select

class SendAgentCommands(Frame):
    """ A window with a single button that when clicked will send an lcm message to tell Rosie to interrupt """
    def create_widgets(self):
        self.grid(row=0, column=0, sticky=N+S+E+W)

        self.interrupt_button = Button(self, text="INTERRUPT", font=("Times", "24"))
        self.interrupt_button["command"] = self.on_interrupt_click
        self.interrupt_button.grid(row=0, column=0, sticky=N+S+E+W)

        self.continue_button = Button(self, text="CONTINUE", font=("Times", "24"))
        self.continue_button["command"] = self.on_continue_click
        self.continue_button.grid(row=1, column=0, sticky=N+S+E+W)

    def on_interrupt_click(self):
        interrupt = rosie_agent_command_t()
        interrupt.utime = current_time_us()
        interrupt.command_type = rosie_agent_command_t.INTERRUPT
        self.lcm.publish("ROSIE_AGENT_COMMAND", interrupt.encode())

    def on_continue_click(self):
        contin = rosie_agent_command_t()
        contin.utime = current_time_us()
        contin.command_type = rosie_agent_command_t.CONTINUE
        self.lcm.publish("ROSIE_AGENT_COMMAND", contin.encode())

    def on_exit(self):
        if self.master:
            self.master.destroy()

    def __init__(self, master=None):
        Frame.__init__(self, master, width=400, height=200)
        self.master = master

        master.columnconfigure(0, weight=1)
        master.rowconfigure(0, weight=1)
        self.create_widgets()

        self.lcm = lcm.LCM()

root = Tk()
send_cmd = SendAgentCommands(master=root)
root.protocol("WM_DELETE_WINDOW", send_cmd.on_exit)
root.mainloop()
