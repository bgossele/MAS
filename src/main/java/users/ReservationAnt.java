package users;

import model.road.PathPheromone;
import model.road.PheromoneVirtualGraphRoadModel;
import model.road.VirtualRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class ReservationAnt extends Ant {

	private PathPheromone pheromone;
	private Point start;
	private SimulatorAPI simulator;

	public void set(Point start, PathPheromone pheromone, SimulatorAPI sim,
			int robotId) {
		this.start = start;
		this.pheromone = pheromone;
		this.simulator = sim;
		this.robotId = robotId;
	}

	void reset() {
		this.start = null;
		this.pheromone = null;
		this.roadModel = null;
	}

	ReservationAnt() {
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		roadModel.get().dropPheromone(this, pheromone);
		simulator.unregister(this);
		ReservationAntFactory.returnAnt(this);
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = Optional.of((PheromoneVirtualGraphRoadModel) model);
		roadModel.get().addObjectAt(this, start);
	}

}
