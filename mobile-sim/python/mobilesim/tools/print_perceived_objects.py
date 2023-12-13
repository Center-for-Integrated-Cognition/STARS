# If you call mobilesim.tools.print_perceived_objects
#   It will listen for an lcm message from perception 
#   and print out a list of the visible objects

import threading
import time
import select

import lcm
lcm = lcm.LCM()

import sys

from mobilesim.lcmtypes import object_data_list_t

### -h | --help : print help message
if "-h" in sys.argv or "--help" in sys.argv:
    print("Listens for a DETECTED_OBJECTS LCM message")
    print("and prints out all the objects that the robot currently perceives")
    print("  -a | --all will print out all objects, not just visible ones")
    print("  -r | --repeat will continually keep printing out the objects")
    print("  -p | --props will print out object properties as well")
    sys.exit(0)

### -a | --all : Print out all objects in the message (default is only visible ones)
print_all = ("-a" in sys.argv or "--all" in sys.argv)

### -r | --repeat : Continually print out updates
repeat = ("-r" in sys.argv or "--repeat" in sys.argv)

### -p | --props : Include the properties
print_props = ("-p" in sys.argv or "--props" in sys.argv)

# Data shared across threads
class SharedState:
    def __init__(self, repeat):
        self.repeat = repeat
        self.exit = False
shared = SharedState(repeat)
    
# Thread to continually check for new lcm messages until told to exit
def lcm_handle_thread(shared_state):
    while not shared_state.exit:
        rfds, wfds, efds = select.select([lcm.fileno()], [], [], 0.2)
        if rfds:
            lcm.handle()

lcm_thread = threading.Thread(target=lcm_handle_thread, args=(shared,))
lcm_thread.start()

# Listen for a DETECTED_OBJECTS message and print each visible object
def handle_message(channel, data):
    obj_list = object_data_list_t.decode(data)
    objs_to_print = [ obj for obj in obj_list.objects if obj.visible or print_all ]
    print(", ".join(str(obj.id) for obj in objs_to_print))
    if print_props:
        for obj in objs_to_print:
            props = dict( (cls.category, cls.name) for cls in obj.classifications )
            cat = props["category"]
            print("   " + str(obj.id) + " (" + cat + ") " + " ".join(k + "=" + v for (k, v) in props.items() if k != "category"))
            if len(obj.contained_objects) > 0:
                print("   -> contains: " + str(obj.contained_objects))

    if not shared.repeat:
        shared.exit = True

lcm.subscribe("DETECTED_OBJECTS", handle_message)

while not shared.exit:
    time.sleep(0.1)

lcm_thread.join()

