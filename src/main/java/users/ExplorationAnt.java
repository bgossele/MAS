package users;

import java.util.Collection;

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

public class ExplorationAnt implements TickListener, VirtualUser, CommUser, SimulatorUser {

	private Optional<PheromoneVirtualGraphRoadModel> roadModel;
	private Optional<Point> previousPosition;
	private Optional<Point> position;
	private Optional<Point> destination;
	private Optional<CommDevice> device;
	private SimulatorAPI simulator;
	private Robot mothership;
	private final int hopLimit;
	private int hopCounter;
	public final int id;

	public ExplorationAnt(Point start, Robot mothership, int hopLimit, int id) {
		roadModel = Optional.absent();
		previousPosition = Optional.absent();
		position = Optional.of(start);
		destination = Optional.absent();
		device = Optional.absent();
		this.mothership = mothership;
		this.hopLimit = hopLimit;
		hopCounter = 0;
		this.id = id;
	}

	public ExplorationAnt(Point start, Point destination, Robot mothership, int hopLimit, int id) {
		roadModel = Optional.absent();
		previousPosition = Optional.absent();
		position = Optional.of(start);
		this.destination = Optional.of(destination);
		device = Optional.absent();
		this.mothership = mothership;
		this.hopLimit = hopLimit;
		hopCounter = 0;
		this.id = id;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (hopCounter < hopLimit) {
			if (destination.equals(Optional.absent())
					|| destination.get().equals(
							roadModel.get().getPosition(this))) {
				destination = Optional.absent();
				Collection<Point> neighbours = roadModel.get().getNeighbours(roadModel.get().getPosition(this));
				if(neighbours.size() > 1)
					System.out.println("Ant neighbours = " + neighbours.toString());
				int childnr = 1;
				for (Point des : neighbours) {
					if (des.equals(previousPosition.orNull())) {
						continue;
					} else if (destination.equals(Optional.absent())) {
						destination = Optional.of(des);
					} else {
						int new_id = 10 * id + childnr;
						childnr++;
						ExplorationAnt ant = new ExplorationAnt(roadModel.get().getPosition(this),
								des, mothership, hopLimit - hopCounter, new_id);
						simulator.register(ant);
					}
				}
			} else {
				roadModel.get().moveTo(this, destination.get());
				hopCounter += 1;
			}
			System.out.println("Ant " + id + " reporting from " + roadModel.get().getPosition(this));
			ExplorationReport message = new ExplorationReport(roadModel.get().getPosition(this), roadModel.get().readPheromones(this));
			device.get().send(message, mothership);
		} else {
			System.out.println("Hoplimit reached");
		}

	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
	}

	@Override
	public Optional<Point> getPosition() {
		if (roadModel.get().containsObject(this)) {
			return Optional.of(roadModel.get().getPosition(this));
		}
		return Optional.absent();
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = Optional.of(builder.build());
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = Optional.of((PheromoneVirtualGraphRoadModel) model);
		roadModel.get().addObjectAt(this, position.get());
		System.out.println("Initialised ant " + id + " at = " + roadModel.get().getPosition(this).toString());
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
		
	}

}
