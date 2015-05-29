package users;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.analysis.function.Power;

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
import communication.ParcelAccept;
import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelOffer;

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
	private Parcel parcel;
	private boolean acceptedParcel;
	private boolean pickedUpParcel;

	public Robot(Point start) {
		roadModel = null;
		// Robot will be placed at destination on initialization.
		destination = start;
		path = new LinkedList<>();
		device = null;
		parcel = null;
		pheromones = new HashMap<Point, List<Pheromone>>();
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
		ExplorationAntFactory.build(lastHop, this, DEFAULT_HOP_LIMIT, 1,
				simulator);
		if (destination != null) {
			if (destination.equals(getPosition().get())) {
				// parcel reached
				if (!pickedUpParcel) {
					System.out.println("Pickup");
					parcel.pickUp();
					destination = parcel.getDestination();
					pickedUpParcel = true;
				} else {
					System.out.println("Deliver");
					parcel.drop(getPosition().get());
					destination = null;
					parcel = null;
					pickedUpParcel = false;
					acceptedParcel = false;
				}
			} else {
				MoveProgress mp = roadModel.followPath(this, path, timeLapse);
				if (mp.travelledNodes().size() > 0) {
					lastHop = mp.travelledNodes().get(
							mp.travelledNodes().size() - 1);
				}
				if (!lastHop.equals(destination))
					sendReservationAnts();
			}
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		pheromones.clear();
		readMessages();
		path = getShortestPathTo(roadModel.getPosition(this), destination);
	}

	private void sendReservationAnts() {
		@SuppressWarnings("unchecked")
		List<Point> path_with_origin = (List<Point>) path.clone();
		if (path.getFirst() != getPosition().get()) {
			path_with_origin.add(0, lastHop);
		}

		List<Pheromone> pheromones = getPheromones(path_with_origin);
		for (int i = 0; i < path_with_origin.size(); i++) {
			ReservationAntFactory.build(path_with_origin.get(i),
					pheromones.get(i), simulator);
		}
	}

	public static List<Pheromone> getPheromones(List<Point> path) {
		ArrayList<Pheromone> res = new ArrayList<Pheromone>();
		for (int i = 0; i < path.size(); i++) {
			Point current = path.get(i);
			Move move = null;
			if (i < path.size() - 1) {
				Point next = path.get(i + 1);
				double d_x = next.x - current.x;
				double d_y = next.y - current.y;
				if (d_x == 0 && d_y == 0) {
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

	public static List<Pheromone> getPheromonesMul(List<PointMul> path) {
		ArrayList<Pheromone> res = new ArrayList<Pheromone>();
		int i = 0;
		Move move = null;
		while (true) {
			PointMul pointMulCurrent = path.get(i);
			PointMul pointMulNext = path.get(i + 1);
			if (pointMulCurrent == null || pointMulNext == null) {
				move = Move.WAIT;
				res.add(PheromoneFactory.build(i, null, move, -5));
				break;
			}
			Point current = pointMulCurrent.getPoint();
			Point next = pointMulCurrent.getPoint();
			double d_x = next.x - current.x;
			double d_y = next.y - current.y;
			if (d_x == 0 && d_y == 0) {
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
			i++;
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
			} else if (!acceptedParcel) {
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
		if (awardedParcels.size() > 0) {
			acceptClosestPackage(awardedParcels);
		}
	}

	private void acceptClosestPackage(ArrayList<Parcel> awardedParcels) {
		int min_cost = Integer.MAX_VALUE;
		Parcel winner = null;
		for (Parcel p : awardedParcels) {
			int cost = getShortestPathTo(roadModel.getPosition(this),
					p.getPosition().get()).size();
			if (cost < min_cost) {
				winner = p;
				min_cost = cost;
			}
		}
		device.send(new ParcelAccept(), winner);
		parcel = winner;
		acceptedParcel = true;
		destination = winner.getPosition().get();
	}

	private LinkedList<Point> getShortestPathTo(Point from, Point to) {
		Queue<PointTree> nodesToExpand = new ArrayDeque<PointTree>();
		PointTree fromTree = new PointTree(from);
		nodesToExpand.add(fromTree);
		List<PointMul> pointMuls = expandNodes(nodesToExpand, fromTree, to);
		LinkedList<Point> path = constructListFromPointMuls(pointMuls);
		return path;
	}

	private List<PointMul> expandNodes(Queue<PointTree> nodesToExpand,
			PointTree fromTree, Point to) {
		Graph<? extends ConnectionData> graph = roadModel.getGraph();
		int shortestPathLength = Integer.MAX_VALUE;
		List<PointMul> shortesPath = null;
		while (true) {
			PointTree nextExpand = nodesToExpand.poll();
			for (Point nextHop : graph.getOutgoingConnections(nextExpand
					.getPoint())) {
				PointTree nextHopTree = new PointTree(nextExpand, nextHop);
				nextExpand.addChild(nextHopTree);
				if (nextHop.equals(to)) {
					List<PointMul> path = conflictAvoidance(nextHopTree);
					int length = getLengthPointMulList(path);
					if (length < shortestPathLength) {
						shortesPath = path;
						shortestPathLength = length;
					}
				}
				nodesToExpand.add(nextHopTree);
			}
			if (nextExpand.getDepth() >= shortestPathLength) {
				break;
			}
		}
		return shortesPath;
	}

	private List<PointMul> conflictAvoidance(PointTree nextHopTree) {
		List<PointMul> pointMuls = constructPointMuls(nextHopTree);
		List<Pheromone> pheremoneList = getPheromonesMul(pointMuls);
		int step = 1;
		while (true) {
			Point point = getFromPointMulList(pointMuls, step).getPoint();
			if (point == null) {
				break;
			}
			Pheromone pheromone = pheremoneList.get(step);
			List<Pheromone> otherPheromonesOnPoint = pheromones.get(point);
			for (Pheromone otherPheromone : otherPheromonesOnPoint) {
				if (otherPheromone.getTimeStamp() <= step + 1
						|| otherPheromone.getTimeStamp() > step - 1) {
					if (otherPheromone.getGoal().equals(pheromone.getOrigin())) {
						// Colliding.
						// TODO backtrack until no more collision. Currently
						// only one way road systems are supported.
					} else {
						insertWaitingSpot(step, pointMuls);
						step = 0;
						break;
					}
				}
			}
			step++;
		}
		return pointMuls;
	}

	private PointMul getFromPointMulList(List<PointMul> pointMuls, int step) {
		int i = 1;
		while (true) {
			PointMul pointMul = pointMuls.get(i);
			step -= pointMul.getMul();
			if (step <= 0) {
				return pointMul;
			}
		}
	}

	private int getLengthPointMulList(List<PointMul> pointMuls) {
		int length = 0;
		for (PointMul pointMul : pointMuls) {
			length += pointMul.getMul();
		}
		return length;
	}

	private List<PointMul> constructPointMuls(PointTree nextHopTree) {
		int depth = nextHopTree.getDepth();
		List<PointMul> path = new ArrayList<PointMul>(depth + 1);
		PointTree previousHopTree = nextHopTree;
		for (int i = depth; i == 0; i--) {
			path.add(i, new PointMul(previousHopTree.getPoint(), 1));
			previousHopTree = previousHopTree.getParent();
		}
		path.add(depth, new PointMul(nextHopTree.getPoint(), 1));
		return path;
	}

	private LinkedList<Point> constructListFromPointMuls(
			List<PointMul> pointMuls) {
		int i = 1;
		LinkedList<Point> points = new LinkedList<Point>();
		while (true) {
			PointMul pointMul = getFromPointMulList(pointMuls, i);
			if (pointMul == null) {
				break;
			} else {
				points.add(pointMul.getPoint());
			}
		}
		return points;
	}

	private void insertWaitingSpot(int step, List<PointMul> pointMuls) {
		PointMul pointMul = getFromPointMulList(pointMuls, step);
		if (pointMul.getMul() > 1) {
			pointMul.setMul(pointMul.getMul() - 1);
		}
		int i = pointMuls.indexOf(pointMul) - 1;
		pointMuls.get(i).setMul(pointMuls.get(i).getMul() + 1);
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

	private class PointMul {

		private final Point point;
		private int mul;

		public PointMul(Point point, int mul) {
			this.point = point;
			this.mul = mul;
		}

		public final Point getPoint() {
			return point;
		}

		public int getMul() {
			return mul;
		}

		public void setMul(int mul) {
			this.mul = mul;
		}

	}

}
