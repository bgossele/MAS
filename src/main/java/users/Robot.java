package users;

import java.io.BufferedOutputStream;
import static java.nio.file.StandardOpenOption.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	
	public static final int RESERVATION_SPEED = 2;

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
	private int reservationTime;
	private int tickCounter = 0;
	private final int id;
	int waitingTime = 0;
	int nbRobots;

	public Robot(int id, Point start, int nbRobots) {
		roadModel = null;
		// Robot will be placed at destination on initialization.
		destination = start;
		path = null;
		device = null;
		parcel = null;
		this.id = id;
		this.nbRobots = nbRobots;
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

	private int getTicksToWait() {
		return (int) Math.round(15 / getSpeed());
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		tickCounter++;
		ExplorationAntFactory.build(lastHop, this, id, tickCounter,
				DEFAULT_HOP_LIMIT, simulator);
		if (destination != null) {
			if (destination.equals(getPosition().get())) {
				// parcel reached
				if (!pickedUpParcel) {
					parcel.pickUp();
					destination = parcel.getDestination();
					pickedUpParcel = true;
					path = null;
					lastHop = getPosition().get();
					System.out.println(id + ": Pickup - " + lastHop);
				} else {
					parcel.dropAndDeliver(getPosition().get());
					destination = null;
					parcel = null;
					pickedUpParcel = false;
					acceptedParcel = false;
					path = null;
					lastHop = getPosition().get();
					System.out.println(id + ": Deliver - " + lastHop);
					logParcelDelivery(timeLapse.getTime());
				}
			} else if (path != null && path.get(1).equals(getPosition().get())) {
				if (waitingTime > 0) {
					waitingTime--;
				} else {
					lastHop = getPosition().get();
					path = null;
//					System.out.println(id + ": Hop reached - " + lastHop);
				}
			} else if (checkedPath) {
				roadModel.moveTo(this, path.get(1), timeLapse);
			}
		}
		sendReservationAnts();
	}
	
	private void logParcelDelivery(long time){
		String s = id + ":" + time/1000 + "\n";
	    byte data[] = s.getBytes();
	    Path p = Paths.get("parcel_delivery_log.txt");

	    try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(p, CREATE, APPEND))) {
	      out.write(data, 0, data.length);
	      out.close();
	    } catch (IOException x) {
	      System.err.println(x);
	    }
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		pheromones.clear();
		readMessages();
		if (path == null && destination != null) {
			checkedPath = false;
			try {
				path = getShortestPathTo(lastHop, destination);
				reservationTime = 0;
				System.out.println(id + ": path reserved at " + tickCounter);
			} catch (PathNotFoundException e) {
				e.printStackTrace();
			}
			if (path != null && path.get(0).equals(path.get(1))) {
				waitingTime = getTicksToWait();
			}
		} else if (path != null && !checkedPath) {
			if(reservationTime != RESERVATION_SPEED) {
				reservationTime ++;
			} else {
				checkedPath = checkPath(timeLapse);
				if(checkedPath == false) {
					path = null;
				}
			}
		}
	}

	private Boolean checkPath(TimeLapse timeLapse) {
		int priorityShift = (int) ((timeLapse.getTime() / 1000) % nbRobots);
		int priority = (id + priorityShift) % nbRobots;
		for (int step = 0; step < path.size(); step++) {
			Point point = path.get(step);
			List<PathPheromone> otherPheromonesOnPoint = pheromones.get(point);
			if (otherPheromonesOnPoint != null) {
				for (PathPheromone otherPheromone : otherPheromonesOnPoint) {
					if (otherPheromone.getRobot() != id
							&& otherPheromone.getTimeStamp() <= step + 1
							&& otherPheromone.getTimeStamp() >= step - 1) {
						if(step == 1) {
							return false;
						}
						int otherPriority = (otherPheromone.getRobot() + priorityShift) % nbRobots;
						if(priority< otherPriority) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private void sendReservationAnts() {
		List<Point> resPath;
		if (path == null) {
			resPath = new LinkedList<Point>();
			for (int i = 0; i < 30; i++) {
				resPath.add(lastHop);
			}
		} else {
			resPath = path;
			for (int i = path.size(); i < 30; i++) {
				resPath.add(path.getLast());
			}
		}
		List<PathPheromone> pheromones = getPheromones(resPath);
		for (int i = 0; i < resPath.size(); i++) {
			ReservationAntFactory.build(resPath.get(i), pheromones.get(i),
					simulator, id);
		}

	}

	public List<PathPheromone> getPheromones(List<Point> path) {
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
				res.add(PathPheromoneFactory.build(i,
						previousMove.getOpposite(), move, id));
				previousMove = move;

			} else {
				move = Move.WAIT;
				res.add(PathPheromoneFactory.build(i,
						previousMove.getOpposite(), move, id));
			}
		}
		return res;
	}

	public List<PathPheromone> getPheromonesMul(List<PointMul> path) {
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
			res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(),
					move, id));
			previousMove = move;
		}
		move = Move.WAIT;
		res.add(PathPheromoneFactory.build(i, previousMove.getOpposite(), move,
				id));
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
			int cost;
			try {
				List<Point> candidate_path = getShortestPathTo(
						roadModel.getPosition(this), p.getPosition().get());
				if (candidate_path != null) {
					cost = candidate_path.size();
					if (cost < min_cost) {
						winner = p;
						min_cost = cost;
					}
				} else {
					winner = p;
					min_cost = 0;
					break;
				}
			} catch (PathNotFoundException e) {
				System.err
						.println("PathNotFoundException in acceptClosestPackage");
			}
		}
		if (winner != null) {
			device.send(new ParcelAccept(), winner);
			parcel = winner;
			acceptedParcel = true;
			destination = winner.getPosition().get();
			System.out.println(id + ": packet accepted - " + destination);
		}
	}

	private LinkedList<Point> getShortestPathTo(Point from, Point to)
			throws PathNotFoundException {
		System.out.println(id + ": searching from " + from + " to " + to);
		if (from.equals(to)) {
			return null;
		}
		Queue<PointTree> nodesToExpand = new ArrayDeque<PointTree>();
		PointTree fromTree = new PointTree(from);
		nodesToExpand.add(fromTree);
		List<PointMul> pointMuls = doGetShortestPathTo(nodesToExpand, fromTree,
				to);
		if (pointMuls == null) {
			throw new PathNotFoundException();
		}
		LinkedList<Point> path = constructListFromPointMuls(pointMuls);
		System.out.println(id + ": path:" + path);
		return path;
	}

	private List<PointMul> doGetShortestPathTo(Queue<PointTree> nodesToExpand,
			PointTree fromTree, Point to) {
		Graph<? extends ConnectionData> graph = roadModel.getGraph();
		int shortestPathLength = Integer.MAX_VALUE;
		List<PointMul> shortesPath = null;
		while (true) {
			PointTree currentNode = nodesToExpand.poll();
			if (currentNode == null) {
				break;
			}
			for (Point nextPoint : graph.getOutgoingConnections(currentNode
					.getPoint())) {
				PointTree nextNode = new PointTree(currentNode, nextPoint);
				if (!containsPoint(currentNode, nextNode.getPoint())) {
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
			}
			if (currentNode.getDepth() == shortestPathLength
					|| currentNode.getDepth() > MAX_SEARCH_DEPTH) {
				break;
			}
			// if (currentNode.getDepth() != searchDepth) {
			// searchDepth = currentNode.getDepth();
			// System.out.println(id + ": searchdepth - " + searchDepth);
			// }
		}
		System.out.println(id + ": shortest path length:" + shortestPathLength);
		return shortesPath;
	}

	private static boolean containsPoint(PointTree tree, Point point) {
		if (tree.getPoint().equals(point)) {
			return true;
		} else if (tree.getDepth() != 0) {
			return containsPoint(tree.getParent(), point);
		} else {
			return false;
		}
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
					if (otherPheromone.getRobot() != id
							&& otherPheromone.getTimeStamp() <= step + 1
							&& otherPheromone.getTimeStamp() >= step - 1) {
						if (point.equals(pointMuls.get(0).getPoint())) {
							return null;
						}
						if (otherPheromone.getGoal().equals( // Head-on
																// collision
								pheromone.getOrigin())) {
							int robotId = otherPheromone.getRobot();
							pointMuls = findBacktrackPoint(pointMuls, robotId,
									pheremoneList, step);
							if (pointMuls == null) {
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

	private List<PointMul> findBacktrackPoint(List<PointMul> pointMuls,
			int robotId, List<PathPheromone> pheromoneList, int step) {
		int waitingTime = 0;
		Boolean collissionEndFound = false;
		while (!collissionEndFound) {
			PathPheromone pheromone = pheromoneList.get(step);
			Point point = getFromPointMulList(pointMuls, step).getPoint();
			for (PathPheromone otherPheromone : pheromones.get(point)) {
				if (otherPheromone.getRobot() == robotId) {
					if (point.equals(pointMuls.get(0).getPoint())) {
						return null;
					} else if (otherPheromone.getGoal().equals(
							pheromone.getOrigin())) {
						step--;
						break;
					} else if (otherPheromone.getGoal().equals(Move.WAIT)) {
						// Do nothing
					} else {
						collissionEndFound = true;
						waitingTime = otherPheromone.getTimeStamp();
						break;
					}
				}
			}
		}
		PointMul waitingSpot = getFromPointMulList(pointMuls, step - 1);
		boolean waitingInserted = false;
		for (PointMul pointMul : pointMuls) {
			if (waitingInserted) {
				pointMul.setMul(1);
			} else if (pointMul.equals(waitingSpot)) {
				pointMul.setMul(waitingTime + 1);
				waitingInserted = true;
			} else {
				waitingTime -= pointMul.getMul();
			}
		}
		System.out.println(pointMuls);
		return pointMuls;
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

		public String toString() {
			return "< " + point.toString() + " " + getMul() + " >";
		}

	}

}
