from tkinter import *
import tkinter.font

import sys

from rosie import RosieGUI
from mobilesim.rosie import MobileSimClient

def agent_factory(config_file):
    return MobileSimClient(config_file=config_file)

def main():
    if len(sys.argv) == 1:
        print("ERROR: MobileRosieGUI takes 1 argument - a rosie config file")
        sys.exit(1)
    rosie_config = sys.argv[1]

    root = Tk()
    rosie_client = MobileSimClient(config_filename=rosie_config)
    rosie_gui = RosieGUI(rosie_client, master=root)
    rosie_gui.run()

if __name__ == "__main__":
    main()

