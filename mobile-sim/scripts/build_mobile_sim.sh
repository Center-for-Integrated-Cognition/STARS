#!/bin/bash

cd $MOBILE_SIM_HOME/java
if [ "$1" == "--clean" ]; then 
	ant clean
else
	ant
fi
