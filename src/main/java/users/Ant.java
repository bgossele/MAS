package users;

import model.road.PheromoneVirtualGraphRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public abstract class Ant implements VirtualUser, TickListener, SimulatorUser{
	
	protected int robotId;
	protected SimulatorAPI simulator;
	protected Optional<PheromoneVirtualGraphRoadModel> roadModel;
	
	public Optional<Point> getPosition() {
		if (roadModel.get().containsObject(this)) {
			return Optional.of(roadModel.get().getPosition(this));
		}
		return Optional.absent();
	}
	
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
