#!/bin/bash
# Assumes that the environment variable $MOBILE_SIM_HOME is set to this directory

### Setup Environment Variables

export CLASSPATH=$CLASSPATH:$MOBILE_SIM_HOME/java/mobilesim.jar

export PYTHONPATH=$PYTHONPATH:$MOBILE_SIM_HOME/python


### APRIL Environment Variables
export APRIL_HOME=$MOBILE_SIM_HOME/april
export CLASSPATH=$CLASSPATH:$APRIL_HOME/java/april.jar
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$APRIL_HOME/lib:$APRIL_HOME/java/jni/jgl
alias java='java -ea -server'


### Setup Helper Scripts

source $MOBILE_SIM_HOME/scripts/custom_completions.sh

alias build_mobile_sim="$MOBILE_SIM_HOME/scripts/build_mobile_sim.sh"

alias run_mobile_sim="$MOBILE_SIM_HOME/scripts/run_mobile_sim.sh"
complete -F _mobile_worlds_completion run_mobile_sim

export PROBCOG_CONFIG=$MOBILE_SIM_HOME/config/robot.config.local

# Will print a description of each visible object in the lcm observations being sent to Rosie
alias print_perceived_objects="python3 -m mobilesim.tools.print_perceived_objects"


