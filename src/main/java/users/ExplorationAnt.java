package users;

import java.util.Collection;
import java.util.List;

import model.road.ExploredPheromone;
import model.road.ExploredPheromoneFactory;
import model.road.PheromoneVirtualGraphRoadModel;
import model.road.VirtualRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import communication.ExplorationReport;

public class ExplorationAnt extends Ant implements TickListener, VirtualUser, CommUser,
		SimulatorUser {

	private boolean active;
	private Optional<PheromoneVirtualGraphRoadModel> roadModel;
	private Optional<Point> previousPosition;
	private Optional<Point> destination;
	private Optional<CommDevice> device;
	private SimulatorAPI simulator;
	private CommUser mothership;
	private int robotId;
	private int tick;
	private int hopLimit;
	private int hopCounter;
	private final int id;

	ExplorationAnt(int id) {
		this.id = id;
	}

	void reset() {
		active = false;
		previousPosition = Optional.absent();
		this.destination = Optional.absent();
		this.mothership = null;
		this.robotId = -1;
		this.tick = -1;
		this.hopLimit = 0;
		hopCounter = 0;
	}

	void set(Point start, CommUser mothership, int robotId, int tick,
			int hopLimit, SimulatorAPI sim) {
		active = true;
		previousPosition = Optional.absent();
		this.destination = Optional.absent();
		this.mothership = mothership;
		this.robotId = robotId;
		this.tick = tick;
		this.hopLimit = hopLimit;
		hopCounter = 0;
		this.simulator = sim;
		roadModel.get().addObjectAt(this, start);
	}

	void set(Point start, Point previous, CommUser mothership, int robotId,
			int tick, int hopLimit, SimulatorAPI sim) {
		set(start, mothership, robotId, tick, hopLimit, sim);
		this.previousPosition = Optional.of(previous);
	}

	public int getId() {
		return id;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (active == false) {
			return;
		}

		ExplorationReport message = new ExplorationReport(getPosition().get(),
				roadModel.get().readPathPheromones(this));
		device.get().send(message, mothership);

		if (hopCounter < hopLimit) {
			List<ExploredPheromone> pheromoneList = roadModel.get()
					.readExploredPheromones(this);
			if (pheromoneList != null) {
				for (ExploredPheromone pheromone : roadModel.get()
						.readExploredPheromones(this)) {
					if (pheromone.getRobot() == robotId
							&& pheromone.getTick() == tick) {
						roadModel.get().removeObject(this);
						ExplorationAntFactory.returnAnt(this);
						return;
					}
				}
			}

			ExploredPheromone pheromone = ExploredPheromoneFactory.build(tick,
					robotId);
			roadModel.get().dropPheromone(this, pheromone);

			destination = Optional.absent();
			Collection<Point> neighbours = roadModel.get().getNeighbours(
					getPosition().get());
			for (Point des : neighbours) {
				if (des.equals(previousPosition.orNull())) {
					continue;
				} else if (destination.equals(Optional.absent())) {
					destination = Optional.of(des);
				} else {
					ExplorationAntFactory.build(des, getPosition().get(),
							mothership, robotId, tick, hopLimit - hopCounter,
							simulator);
				}
			}

			if (destination.isPresent()) {
				roadModel.get().moveTo(this, destination.get());
				hopCounter += 1;
			} else { // dead-end reached, stop exploration
				hopCounter = hopLimit;
			}
		} else {
			roadModel.get().removeObject(this);
			ExplorationAntFactory.returnAnt(this);
		}

	}	

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = Optional.of(builder.build());
	}
	
	@Override
	public Optional<Point> getPosition() {
		if (roadModel.get().containsObject(this)) {
			return Optional.of(roadModel.get().getPosition(this));
		}
		return Optional.absent();
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = Optional.of((PheromoneVirtualGraphRoadModel) model);
	}
}
