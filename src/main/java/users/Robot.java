package users;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MoveProgress;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import communication.ExplorationReport;

public class Robot implements TickListener, MovingRoadUser, CommUser,
		SimulatorUser {

	private final RandomGenerator rng;
	private Optional<CollisionGraphRoadModel> roadModel;
	private Optional<Point> destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private Optional<CommDevice> device;
	public static final int DEFAULT_HOPLIMIT = 10;

	private SimulatorAPI simulator;

	public Robot(RandomGenerator r) {
		rng = r;
		roadModel = Optional.absent();
		destination = Optional.absent();
		path = new LinkedList<>();
		device = Optional.absent();
	}

	@Override
	public void initRoadUser(RoadModel model) {
		roadModel = Optional.of((CollisionGraphRoadModel) model);
		Point p;
		do {
			p = model.getRandomPosition(rng);
		} while (roadModel.get().isOccupied(p));
		roadModel.get().addObjectAt(this, p);
		lastHop = p;
		started = true;
	}

	@Override
	public double getSpeed() {
		return 1;
	}

	void nextDestination() {
		destination = Optional.of(roadModel.get().getRandomPosition(rng));
		path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
				destination.get()));
	}

	private boolean started = false;

	@Override
	public void tick(TimeLapse timeLapse) {
		if (started) {
			ExplorationAnt ant = new ExplorationAnt(lastHop, this, DEFAULT_HOPLIMIT, 1);
			simulator.register(ant);
			started = false;
		}

		if (!destination.isPresent()) {
			nextDestination();
		}

		MoveProgress mp = roadModel.get().followPath(this, path, timeLapse);
		if (mp.travelledNodes().size() > 0) {
			lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
		}

		if (roadModel.get().getPosition(this).equals(destination.get())) {
			nextDestination();
		}
		
		readMessages();
	}
	
	private void readMessages() {
		Collection<Message> messages = device.get().getUnreadMessages();
		for (Message message: messages) {
			MessageContents content = message.getContents();
			if (content instanceof ExplorationReport) {
				ExplorationReport rep = (ExplorationReport) content;
				System.out.println("received exploration report for pos " + rep.pos.toString());
			}
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
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

}
