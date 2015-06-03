package users;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
import communication.ParcelOffer;

public class Robot implements TickListener, MovingRoadUser, CommUser,
		SimulatorUser {

	public static final int DEFAULT_HOP_LIMIT = 10;

	public static final int MAX_SEARCH_DEPTH = 500;

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
	private boolean checkedPath;

	public Robot(Point start) {
		roadModel = null;
		// Robot will be placed at destination on initialization.
		destination = start;
		path = null;
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
					path = null;
				} else {
					System.out.println("Deliver");
					parcel.dropAndDeliver(getPosition().get());
					destination = null;
					parcel = null;
					pickedUpParcel = false;
					acceptedParcel = false;
					path = null;
				}
			} else if (path != null && path.get(1).equals(getPosition().get())) {
				lastHop = path.get(1);
				path = null;
				System.out.println("Hop reached");
			} else if (checkedPath) {
				MoveProgress mp = roadModel
						.moveTo(this, path.get(1), timeLapse);
				System.out.println("Moving");
			}
			if (path != null) {
				sendReservationAnts();
				System.out.println("Sending reservation ants");
			}
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		pheromones.clear();
		readMessages();
		if (path == null && destination != null) {
			checkedPath = false;
			path = getShortestPathTo(lastHop, destination);
		} else if (!checkedPath) {
			checkedPath();
		}
	}

	private void checkedPath() {
		// TODO checkPathForCollisions
		checkedPath = true;
	}

	private void sendReservationAnts() {
		@SuppressWarnings("unchecked")
		List<Point> path_with_origin = (List<Point>) path.clone();
		if (path.getFirst() != getPosition().get()) {
			path_with_origin.add(0, lastHop);
		}

		List<PathPheromone> pheromones = getPheromones(path_with_origin);
		for (int i = 0; i < path_with_origin.size(); i++) {
			ReservationAntFactory.build(path_with_origin.get(i),
					pheromones.get(i), simulator);
		}
	}

	public static List<PathPheromone> getPheromones(List<Point> path) {
		ArrayList<PathPheromone> res = new ArrayList<PathPheromone>();
		Move previousMove = Move.WAIT;
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
				res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(),
						move, -5));
				previousMove = move;

			} else {
				move = Move.WAIT;
				res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(),
						move, -5));
			}
		}
		return res;
	}

	public static List<PathPheromone> getPheromonesMul(List<PointMul> path) {
		ArrayList<PathPheromone> res = new ArrayList<PathPheromone>();
		int i = 0;
		int end = getLengthPointMulList(path);
		Move move = null;
		Move previousMove = Move.WAIT;
		for (i = 0; i < end - 1; i++) {
			Point current = getFromPointMulList(path, i).getPoint();
			Point next = getFromPointMulList(path, i + 1).getPoint();
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
			res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(), move,
					-5));
			previousMove = move;
		}
		move = Move.WAIT;
		res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(), move, -5));
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
		List<PointMul> pointMuls = doGetShortestPathTo(nodesToExpand, fromTree,
				to);
		LinkedList<Point> path = constructListFromPointMuls(pointMuls);
		return path;
	}

	private List<PointMul> doGetShortestPathTo(Queue<PointTree> nodesToExpand,
			PointTree fromTree, Point to) {
		Graph<? extends ConnectionData> graph = roadModel.getGraph();
		int shortestPathLength = Integer.MAX_VALUE;
		List<PointMul> shortesPath = null;
		while (true) {
			PointTree currentNode = nodesToExpand.poll();
			for (Point nextPoint : graph.getOutgoingConnections(currentNode
					.getPoint())) {
				PointTree nextNode = new PointTree(currentNode, nextPoint);
				currentNode.addChild(nextNode);
				if (nextPoint.equals(to)) {
					List<PointMul> possiblePath = conflictAvoidance(nextNode);
					int possiblelength;
					if (possiblePath == null) {
						possiblelength = Integer.MAX_VALUE;
					} else {
						possiblelength = getLengthPointMulList(possiblePath);
					}
					if (possiblelength < shortestPathLength) {
						shortesPath = possiblePath;
						shortestPathLength = possiblelength;
					}
				}
				nodesToExpand.add(nextNode);
			}
			if (currentNode.getDepth() == shortestPathLength
					|| currentNode.getDepth() > MAX_SEARCH_DEPTH) {
				break;
			}
		}
		return shortesPath;
	}

	private List<PointMul> conflictAvoidance(PointTree nextHopTree) {
		List<PointMul> pointMuls = constructPointMuls(nextHopTree);
		List<PathPheromone> pheremoneList = getPheromonesMul(pointMuls);
		int step = 0;
		while (step < getLengthPointMulList(pointMuls)) {
			PointMul pointMul = getFromPointMulList(pointMuls, step);
			Point point = pointMul.getPoint();
			PathPheromone pheromone = pheremoneList.get(step);
			List<PathPheromone> otherPheromonesOnPoint = pheromones.get(point);
			if (otherPheromonesOnPoint != null) {
				for (PathPheromone otherPheromone : otherPheromonesOnPoint) {
					if (otherPheromone.getTimeStamp() <= step + 1
							|| otherPheromone.getTimeStamp() > step - 1) {
						if (point.equals(pointMuls.get(0).getPoint())) {
							return null;
						}
						if (otherPheromone.getGoal().equals(
								pheromone.getOrigin())) {
							int robotId = otherPheromone.getRobot();
							pointMuls = findBacktrackPoint(pointMuls, robotId,
									pheremoneList, step);
							if(pointMuls == null) {
								return null;
							}
						} else {
							insertWaitingSpot(step, pointMuls);
						}
						pheremoneList = getPheromonesMul(pointMuls);
						step = -1;
						break;
					}
				}
			}
			step++;
		}
		return pointMuls;
	}

	private List<PointMul> findBacktrackPoint(List<PointMul> pointMuls, int robotId,
			List<PathPheromone> pheromoneList, int step) {
		while (true) {
			PathPheromone pheromone = pheromoneList.get(step);
			Point point = getFromPointMulList(pointMuls, step).getPoint();
			PathPheromone otherPheromone = getPheremoneWithRobotId(
					pheromones.get(point), robotId);
			if(point.equals(pointMuls.get(0))) {
				return null;
			} else if(otherPheromone.getGoal().equals(pheromone.getOrigin())) {
				step--;
			} else {
				int waitingTime = otherPheromone.getTimeStamp();
				PointMul waitingSpot = getFromPointMulList(pointMuls, step-1);
				boolean waitingInserted = false;
				for(PointMul pointMul : pointMuls) {
					if(waitingInserted) {
						pointMul.setMul(1);
					} else if(pointMul.equals(waitingSpot)){
						pointMul.setMul(waitingTime+1);
					} else {
						waitingTime -= pointMul.getMul();
					}
				}
				break;
			}
		}
		return pointMuls;
	}

	private static PathPheromone getPheremoneWithRobotId(
			List<PathPheromone> pheromoneList, int robotId) {
		for (PathPheromone pheromone : pheromoneList) {
			if (pheromone.getRobot() == robotId) {
				return pheromone;
			}
		}
		return null;
	}

	private static PointMul getFromPointMulList(List<PointMul> pointMuls,
			int step) {
		int i = 0;
		while (true) {
			PointMul pointMul = pointMuls.get(i);
			step -= pointMul.getMul();
			if (0 > step) {
				return pointMul;
			}
			i++;
		}
	}

	private static int getLengthPointMulList(List<PointMul> pointMuls) {
		int length = 0;
		for (PointMul pointMul : pointMuls) {
			length += pointMul.getMul();
		}
		return length;
	}

	private static List<PointMul> constructPointMuls(PointTree nextHopTree) {
		int depth = nextHopTree.getDepth();
		PointMul[] pointMuls = new PointMul[depth + 1];
		PointTree previousHopTree = nextHopTree;
		for (int i = depth; i >= 0; i--) {
			pointMuls[i] = new PointMul(previousHopTree.getPoint(), 1);
			previousHopTree = previousHopTree.getParent();
		}
		return Arrays.asList(pointMuls);
	}

	private static LinkedList<Point> constructListFromPointMuls(
			List<PointMul> pointMuls) {
		LinkedList<Point> points = new LinkedList<Point>();
		int end = getLengthPointMulList(pointMuls);
		for (int i = 0; i < end; i++) {
			PointMul pointMul = getFromPointMulList(pointMuls, i);
			points.add(pointMul.getPoint());
		}
		return points;
	}

	private static void insertWaitingSpot(int step, List<PointMul> pointMuls) {
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

	private static class PointMul {

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
