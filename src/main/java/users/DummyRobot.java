package users;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.road.Move;
import model.road.PathPheromone;
import model.road.PathPheromoneFactory;

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

import communication.ParcelAccept;
import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelCancellation;
import communication.ParcelOffer;

public class DummyRobot implements TickListener, MovingRoadUser, CommUser{

	public static final int DEFAULT_HOP_LIMIT = 10;

	private CollisionGraphRoadModel roadModel;
	private Point destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private CommDevice device;
	private Map<Point, List<PathPheromone>> pheromones;
	private Parcel parcel;
	private boolean acceptedParcel;
	private boolean pickedUpParcel;
	private final int id;

	public DummyRobot(int id, Point start) {
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
	
	public String toString(){
		return "<DummyRobot " + id + ">";
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		
		if(destination != null) {
			if(destination.equals(getPosition().get())) {
				//parcel reached
				if(!pickedUpParcel) {
					parcel.pickUp();
					destination = parcel.getDestination();
					pickedUpParcel = true;
				} else {
					parcel.dropAndDeliver(getPosition().get());
					acceptedParcel = false;System.out.println("Delivered " + parcel);
					destination = null;
					parcel = null;
					pickedUpParcel = false;
					logParcelDelivery(timeLapse.getTime());
				}
			} else {
				MoveProgress mp = roadModel.followPath(this, path, timeLapse);
				if (mp.travelledNodes().size() > 0) {
					lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
				}
			}
		}

		readMessages();
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
			if (!pickedUpParcel) {
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
	
	private void logParcelDelivery(long time) {
		String s = id + ":" + time / 1000 + "\n";
		byte data[] = s.getBytes();
		Path p = Paths.get("parcel_delivery_log.txt");
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(
				p, CREATE, APPEND))) {
			out.write(data, 0, data.length);
			out.close();
		} catch (IOException x) {
			System.err.println(x);
		}
	}

	private void logDistanceTraveled(long time, double distance,
			int deliveringPacket) {
		String s = id + ":" + time / 1000 + ";" + distance + ";"
				+ deliveringPacket + "\n";
		byte data[] = s.getBytes();
		Path p = Paths.get("distance_traveled_log.txt");
		try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(
				p, CREATE, APPEND))) {
			out.write(data, 0, data.length);
			out.close();
		} catch (IOException x) {
			System.err.println(x);
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
		System.out.println("accepted " + parcel + " at distance " + min_cost);
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

}
