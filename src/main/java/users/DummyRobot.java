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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.measure.Measure;

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
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import communication.ParcelAccept;
import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelCancellation;
import communication.ParcelOffer;

public class DummyRobot implements TickListener, MovingRoadUser, CommUser{

	public static final int DEFAULT_HOP_LIMIT = 10;
	private static final boolean PRINT = false;

	private CollisionGraphRoadModel roadModel;
	private Point destination;
	private LinkedList<Point> path;
	private Point lastHop;
	private CommDevice device;
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
	}
	
	public Point getLastHop() {
		return lastHop;
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
				if(!isNextHopOccupied()) {
					print(id + ": heading for " + path.get(1));
					MoveProgress mp = roadModel.followPath(this, path, timeLapse);
					if (mp.travelledNodes().size() > 0) {
						lastHop = mp.travelledNodes().get(mp.travelledNodes().size() - 1);
						logDistanceTraveled(timeLapse.getTime(), mp.distance().getValue(), 1);
					}
				} else {
					print(id + ": road block; waiting on " + getPosition().get() + "; lastHop " + lastHop);
				}
			}
		}

		readMessages();
	}
	
	private boolean isNextHopOccupied() {
		for(RoadUser u: roadModel.getObjects()) {
			DummyRobot d = (DummyRobot) u;
			if(! d.equals(this) && (d.getLastHop().equals(path.get(1)) || d.getPosition().get().equals(path.get(1)))) {
				print(id + ": road blocked by " + d);
				return true;
			}
		}
		return false;
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
				print("Changed my mind from " + parcel.getId() + " to " + winner.getId());
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
	
	private void print(String s) {
		if(PRINT)
			System.out.println(s);
	}

}
