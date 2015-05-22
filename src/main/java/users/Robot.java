package users;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import model.road.Move;
import model.road.Pheromone;
import model.road.PheromoneFactory;
import model.road.PointTree;

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
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import communication.ExplorationReport;

public class Robot implements TickListener, MovingRoadUser, CommUser,
		SimulatorUser {

	public static final int DEFAULT_HOP_LIMIT = 10;

	private CollisionGraphRoadModel roadModel;
	private Point destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private CommDevice device;
	private Map<Point, List<Pheromone>> pheromones;
	private SimulatorAPI simulator;

	public Robot(Point start) {
		roadModel = null;
		// Robot will be placed at destination on initialization.
		destination = start;
		path = new LinkedList<>();
		device = null;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		roadModel = (CollisionGraphRoadModel) model;
		roadModel.addObjectAt(this, destination);
		lastHop = destination;
		destination = null;
	}

	@Override
	public double getSpeed() {
		return 0.5;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		ExplorationAntFactory.build(lastHop, this, DEFAULT_HOP_LIMIT, 1, simulator);

		MoveProgress mp = roadModel.followPath(this, path, timeLapse);
		if (mp.travelledNodes().size() > 0) {
			lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
		}

		readMessages();
		sendReservationAnts();
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
		Collection<Message> messages = device.getUnreadMessages();
		for (Message message : messages) {
			MessageContents content = message.getContents();
			if (content instanceof ExplorationReport) {
				ExplorationReport rep = (ExplorationReport) content;
				pheromones.put(rep.pos, rep.pheromones);
			}
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		pheromones.clear();
		readMessages();
		roadModel.getShortestPathTo(roadModel.getPosition(this), destination);
	}

	public List<Point> getShortestPathTo(Point from, Point to) {
		Queue<PointTree> nodesToExpand = new ArrayDeque<PointTree>();
		PointTree fromTree = new PointTree(from);
		nodesToExpand.add(fromTree);
		expandNodes(nodesToExpand, fromTree, to);

		return null;
	}

	private void expandNodes(Queue<PointTree> nodesToExpand,
			PointTree fromTree, Point to) {
		Graph<? extends ConnectionData> graph = roadModel.getGraph();
		while (true) {
			PointTree nextExpand = nodesToExpand.poll();
			for (Point nextHop : graph.getOutgoingConnections(nextExpand
					.getPoint())) {
				PointTree nextHopTree = new PointTree(nextExpand, nextHop);
				nextExpand.addChild(nextHopTree);
				if (nextHop.equals(to)) {
					// TODO check for conflict and calculate traversal time.
					conflictAvoidance(nextHopTree);
					
				}
				nodesToExpand.add(nextHopTree);
			}
		}

	}

	private void conflictAvoidance(PointTree nextHopTree) {
		List<Point> path = constructPath(nextHopTree);
		int step = 1;
		while (true) {
			Point point = path.get(step);
			List<Pheromone> pheremonesOnPoint = pheromones.get(point);
			for (Pheromone pheromone : pheremonesOnPoint) {
				if (pheromone.getTimeStamp() == step) {
					//TODO check for kind of conflict.
				}
			}
		}
	}

	private List<Point> constructPath(PointTree nextHopTree) {
		int depth = nextHopTree.getDepth();
		List<Point> path = new ArrayList<Point>(depth + 1);
		PointTree previousHopTree = nextHopTree;
		for (int i = depth; i == 0; i--) {
			path.add(i, previousHopTree.getPoint());
			previousHopTree = previousHopTree.getParrent();
		}
		path.add(depth, nextHopTree.getPoint());
		return path;
	}

	@Override
	public Optional<Point> getPosition() {
		if (roadModel.containsObject(this)) {
			return Optional.of(roadModel.getPosition(this));
		}
		return Optional.absent();
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = builder.build();
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;
	}

}
