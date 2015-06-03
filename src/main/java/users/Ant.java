package users;

import model.road.PheromoneVirtualGraphRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.google.common.base.Optional;

public abstract class Ant implements VirtualUser, TickListener, SimulatorUser{
	
	protected int robotId;
	protected SimulatorAPI simulator;
	protected Optional<PheromoneVirtualGraphRoadModel> roadModel;

	public int getRobotId() {
		return robotId;
	}
	
	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
	}
	
	@Override
	public void afterTick(TimeLapse timeLapse) {}

}
