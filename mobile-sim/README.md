# Installing

Instructions for installing the mobile simulator and its prerequisites. Note that this simulator has only been tested with Ubuntu linux distros, and is not likely to work on a non-linux OS.

#LCM

The first major dependency is lcm, below is a summary of the build instructions

#Required Packages:
    build-essential
    libglib2.0-dev
    cmake

#Clone lcm
git clone https://github.com/lcm-proj/lcm

#Build lcm

mkdir build
cd build
cmake ..
make
sudo make install

#Environment Variables

export CLASSPATH=$CLASSPATH:/usr/local/share/java/lcm.jar

#Soar

To use Rosie, you first need to have Soar installed. Note that currently the only way to get python3 SML support is building from source (only required if you want to use Rosie's python interface).

Option 1: Direct Download
You can download a copy from here: https://soar.eecs.umich.edu/Downloads
Extract it to the desired location, then set up the following environment variables:

# (Make sure that SOAR_HOME is set to the bin directory)
export SOAR_HOME=$HOME/path/to/soar/bin
export PYTHONPATH=$PYTHONPATH:$SOAR_HOME
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$SOAR_HOME
export CLASSPATH=$CLASSPATH:$SOAR_HOME/java/sml.jar:$SOAR_HOME/SoarJavaDebugger.jar

Option 2: Build from Source
You need java and swig installed

git clone https://github.com/soargroup/soar
cd soar
python3 scons/scons.py all

Set the same environment variables as above, except that SOAR_HOME will be set to soar/out

#Rosie

The mobile simulator also depends on Rosie. See the installation instructions in ITL-agent/README.md. Make sure that rosie/java/rosie.jar is on the $CLASSPATH.
Pysoarlib

If you want to use the python SML agent, you will need the pysoarlib library. Simply clone https://github.com/amininger/pysoarlib and add the parent directory to the $PYTHONPATH. Note that it requires the python3 SML library and SOAR_HOME to be included on the PYTHONPATH.

#April

The april robotics toolkit contains the libraries used to create the mobile simulator. There is a copy of it included in the mobile-sim project.

#Prerequisites:

    java (openjdk8 might be more stable than later versions)
    ant
    libglib2.0-dev
    libgl1-mesa-dev
    libdc1394-22-dev
    libusb-1.0-0-dev
    libpng-dev

Building: To build, simply run ant inside the mobile-sim/april/java directory.

#Troubleshooting

    Error: jni_md.h: No such file or directory

Solution: java is looking for the jni_md.h file in the wrong place. If openjdk is installed in a place such as /usr/lib/jvm/java-8-openjdk-amd64 then you want to create a symbolic link include/jni_md.h pointing to include/linux/jni_md.h:

sudo ln -s /usr/lib/jvm/java-8-openjdk-amd64/include/linux/jni_md.h /usr/lib/jvm/java-8-openjdk-amd64/include/jni_md.h

#Environment Variables:
These are the environment variables recommended by the april documentation. Note that if you source source-mobile-sim-env.sh it will set these variables automatically

export APRIL_HOME=$HOME/path/to/april
export CLASSPATH=$CLASSPATH:/usr/share/java/gluegen-rt.jar:/usr/local/share/java/lcm.jar:$APRIL_HOME/java/april.jar:./
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APRIL_HOME/lib
alias java='java -ea -server'

#mobile-sim

git clone https://github.com/amininger/mobile-sim
cd mobile-sim/java
ant

#!!! IMPORTANT !!!
Go to mobile-sim/config and cp robot.config robot.config.local

Environment Variables:
If you set the $MOBILE_SIM_HOME environment variable to the mobile-sim directory and have your bash profile source the source-mobile-sim-env.sh file, it will set up classpath and pythonpath variables and adds several commands to make working with the mobile simulator easier.

export MOBILE_SIM_HOME=$HOME/path/to/mobile-sim
source $MOBILE_SIM_HOME/source-mobile-sim-env.sh

#Sourcing this file adds the following terminal commands:

build_mobile_sim - will run the ant build script to build the jar
run_mobile_sim [world-file] - will launch the simulator with the given world file in $MOBILE_SIM_HOME/worlds (will autocomplete)
print_perceived_objects - If the simulator is running, prints out the id's of all currently visible objects (-h for more options)
