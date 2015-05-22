package users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import model.road.Move;
import model.road.Pheromone;
import model.road.PheromoneFactory;

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

public class TestRobot implements TickListener, MovingRoadUser, CommUser,
		SimulatorUser {

	private final RandomGenerator rng;
	private Optional<CollisionGraphRoadModel> roadModel;
	private Optional<Point> destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private Optional<CommDevice> device;
	public static final int DEFAULT_HOPLIMIT = 10;

	private SimulatorAPI simulator;

	public TestRobot(RandomGenerator r) {
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
		return 0.5;
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
			ExplorationAntFactory.build(lastHop, this, DEFAULT_HOPLIMIT, 1, simulator);
			started = false;
		}

		if (!destination.isPresent()) {
			nextDestination();
		}
		
		sendReservationAnts();

		MoveProgress mp = roadModel.get().followPath(this, path, timeLapse);
		if (mp.travelledNodes().size() > 0) {
			lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
		}

		if (roadModel.get().getPosition(this).equals(destination.get())) {
			nextDestination();
		}
		
		readMessages();
	}

	private void sendReservationAnts() {
		@SuppressWarnings("unchecked")
		List<Point> path_with_origin = (List<Point>) path.clone();
		if(path.getFirst() != getPosition().get()){
			path_with_origin.add(0, lastHop);
		}
		
		List<Pheromone> pheromones = getPheromones(path_with_origin);
		for(int i = 0; i < path_with_origin.size(); i++){
			ReservationAntFactory.build(path_with_origin.get(i), pheromones.get(i), simulator);
		}
	}
	
	public static List<Pheromone> getPheromones(List<Point> path){
		ArrayList<Pheromone> res = new ArrayList<Pheromone>();
		for (int i = 0; i < path.size(); i++) {
			Point current = path.get(i);
			Move move = null;
			if (i < path.size() - 1) {
				Point next = path.get(i + 1);
				double d_x = next.x - current.x;
				double d_y = next.y - current.y;
				if (d_x == 0 && d_y == 0){
					move = Move.WAIT;
				} else if (d_x > 0) {
					move = Move.EAST;
				} else if (d_x < 0) {
					move = Move.WEST;
				} else if (d_y > 0) {
					move = Move.SOUTH;
				} else if (d_y < 0) {
					move = Move.NORTH;
				}
				res.add(PheromoneFactory.build(i, null, move, -5));
				
			} else {
				move = Move.WAIT;
				res.add(PheromoneFactory.build(i, null, move, -5));
			}
		}
		return res;			
	}
	
	private void readMessages() {
		Collection<Message> messages = device.get().getUnreadMessages();
		for (Message message: messages) {
			MessageContents content = message.getContents();
			if (content instanceof ExplorationReport) {
				ExplorationReport rep = (ExplorationReport) content;
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
