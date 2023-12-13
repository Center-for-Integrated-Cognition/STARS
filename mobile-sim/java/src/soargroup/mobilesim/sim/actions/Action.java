package soargroup.mobilesim.sim.actions;

import soargroup.mobilesim.sim.SimRobot;

public abstract class Action {
	public SimRobot robot = null;
	public void setRobot(SimRobot robot){
		this.robot = robot;
	}
}
