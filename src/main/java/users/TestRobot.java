package users;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import model.road.Move;
import model.road.PathPheromone;
import model.road.PathPheromoneFactory;
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
import communication.ParcelAccept;
import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelCancellation;
import communication.ParcelOffer;

public class TestRobot implements TickListener, MovingRoadUser, CommUser,
		SimulatorUser {

	public static final int DEFAULT_HOP_LIMIT = 2;

	private CollisionGraphRoadModel roadModel;
	private Point destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private CommDevice device;
	private Map<Point, List<PathPheromone>> pheromones;
	private SimulatorAPI simulator;
	private Parcel parcel;
	private boolean acceptedParcel;
	private boolean pickedUpParcel;
	private int tickCount = 0;
	private final int id;

	public TestRobot(int id, Point start) {
		roadModel = null;
		// Robot will be placed at destination on initialization.
		destination = start;
		path = new LinkedList<>();
		this.id = id;
		device = null;
		parcel = null;
		pheromones = new HashMap<Point, List<PathPheromone>>();
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
		if(tickCount % 11 == 0){
//			ExplorationAntFactory.build(lastHop, this, id, tickCount, DEFAULT_HOP_LIMIT, simulator);
		}
		tickCount ++;
		
		if(destination != null) {
			if(destination.equals(getPosition().get())) {
				//parcel reached
				if(!pickedUpParcel) {
					parcel.pickUp();
					destination = parcel.getDestination();
					pickedUpParcel = true;
				} else {
					parcel.dropAndDeliver(getPosition().get());
					destination = null;
					parcel = null;
					pickedUpParcel = false;
					acceptedParcel = false;
				}
			} else {
				MoveProgress mp = roadModel.followPath(this, path, timeLapse);
				if (mp.travelledNodes().size() > 0) {
					lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
				}
				if(!lastHop.equals(destination))
					sendReservationAnts();
			}
		}

		readMessages();
	}

	private void sendReservationAnts() {
		@SuppressWarnings("unchecked")
		List<Point> path_with_origin = (List<Point>) path.clone();
		if(path.getFirst() != getPosition().get()){
			path_with_origin.add(0, lastHop);
		}
		
		List<PathPheromone> pheromones = getPheromones(path_with_origin);
		for(int i = 0; i < Math.min(path_with_origin.size(), DEFAULT_HOP_LIMIT); i++){
			ReservationAntFactory.build(path_with_origin.get(i), pheromones.get(i), simulator);
		}
	}
	
	public static List<PathPheromone> getPheromones(List<Point> path){
		ArrayList<PathPheromone> res = new ArrayList<PathPheromone>();
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
				res.add(PathPheromoneFactory.build(i, null, move, -5));
				
			} else {
				move = Move.WAIT;
				res.add(PathPheromoneFactory.build(i, null, move, -5));
			}
		}
		return res;			
	}
	
	private void readMessages() {
		Collection<Message> messages = device.getUnreadMessages();
		ArrayList<Parcel> awardedParcels = new ArrayList<Parcel>();
		for (Message message : messages) {
			MessageContents content = message.getContents();
			if (content instanceof ExplorationReport) {
				ExplorationReport rep = (ExplorationReport) content;
				pheromones.put(rep.pos, rep.pheromones);
			} else if (!pickedUpParcel) {
				if (content instanceof ParcelOffer) {
					ParcelOffer offer = (ParcelOffer) content;
					Point des = offer.getPosition();
					int cost = roadModel.getShortestPathTo(this, des).size();
					ParcelBid reply = new ParcelBid(cost);
					device.send(reply, message.getSender());
				} else if (content instanceof ParcelAllocation) {
					awardedParcels.add((Parcel) message.getSender());
				}
			}
		}
		if(awardedParcels.size() > 0) {
			acceptClosestPackage(awardedParcels);
		}
	}

	private void acceptClosestPackage(ArrayList<Parcel> awardedParcels) {
		int min_cost = Integer.MAX_VALUE;
		Parcel winner = null;
		for(Parcel p: awardedParcels) {
			int cost = roadModel.getShortestPathTo(this, p.getPosition().get()).size();
			if(cost < min_cost) {
				winner = p;
				min_cost = cost;
			}
		}
		if(acceptedParcel) {
			int remainingCost = roadModel.getShortestPathTo(this, parcel.getPosition().get()).size();
			if (min_cost < remainingCost) {
				System.out.println("Changed my mind from " + parcel.getId() + " to " + winner.getId());
				device.send(new ParcelCancellation(), parcel);
			} else {
				return;
			}
		}
		device.send(new ParcelAccept(), winner);
		parcel = winner;
		System.out.println("accepted parcel at distance " + min_cost);
		acceptedParcel = true;
		destination = winner.getPosition().get();
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		pheromones.clear();
		readMessages();
		if(destination != null)
			path = new LinkedList<>(roadModel.getShortestPathTo(roadModel.getPosition(this), destination));
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
			List<PathPheromone> pheremonesOnPoint = pheromones.get(point);
			for (PathPheromone pheromone : pheremonesOnPoint) {
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
			previousHopTree = previousHopTree.getParent();
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
