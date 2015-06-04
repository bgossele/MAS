package users;

import java.util.ArrayList;
import java.util.List;

import model.road.VirtualGraphRoadModel;
import model.road.VirtualRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import communication.ParcelAccept;
import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelCancellation;
import communication.ParcelOffer;

public class Parcel implements CommUser, TickListener, VirtualUser, SimulatorUser {
	
	private Point position;
	private Point destination;
	private CommDevice device;
	private boolean sold;
	private boolean forSale;
	private final int parcel_id;
	private VirtualGraphRoadModel model;
	private SimulatorAPI sim;
	private boolean delivered = false;
	private boolean pickedUp = false;
	private boolean waitingForAcceptance = false;
	private int counter = 0;

	Parcel(int parcel_id, Point position, Point destination) {
		this.position = position;
		this.destination = destination;
		this.sold = false;
		this.forSale = true;
		this.parcel_id = parcel_id;
	}
	
	public int getId(){
		return parcel_id;
	}
	
	@Override
	public Optional<Point> getPosition() {
		return Optional.of(position);
	}
	
	public Point getDestination() {
		return destination;
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = builder.build();		
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if(forSale){ // Start auction
			System.out.println("Parcel " + parcel_id + " starting auction");
			device.broadcast(new ParcelOffer(position, destination));
		}
		
		List<Message> messages = device.getUnreadMessages();
		
		if (forSale) { // Get bids
			
			ArrayList<Message> bids = new ArrayList<Message>();	
			
			for(Message m: messages) {
				MessageContents content = m.getContents();
				if(content instanceof ParcelBid){
					bids.add(m);
					System.out.println("Parcel " + parcel_id + " received bid from " + m.getSender() + " at " + timeLapse.getTime()/1000);
				}
			}
			if(bids.size() > 0) {
				CommUser winner = getBestBidder(bids);
				this.forSale = false;
				System.out.println("Parcel " + parcel_id + " awarded to " + winner + " at " + timeLapse.getTime()/1000);
				this.waitingForAcceptance = true;
				device.send(new ParcelAllocation(), winner);
			} else {
				System.out.println("Parcel " + parcel_id + " received no bids.");
			}
		}
		
		if (waitingForAcceptance) {
			for(Message m: messages) {
				MessageContents content = m.getContents();
				if(content instanceof ParcelAccept){
					this.sold = true;
					this.forSale = false;
					System.out.println("Parcel " + parcel_id + " accepted by " + m.getSender());
					break;
				}
			}
			System.out.println("Parcel " + parcel_id + " not accepted.");
		}
		
		this.waitingForAcceptance = false;
		
		if (sold && !pickedUp) {
			for(Message m: messages) {
				MessageContents c = m.getContents();
				if(c instanceof ParcelCancellation) {
					this.sold = false;
					this.forSale = true;
					System.out.println("Parcel " + parcel_id + " cancelled by " + m.getSender() + " at " + timeLapse.getTime()/1000);
				}
			}
		}
		
		if(delivered) {
			if(counter >= 50) {
				sim.unregister(this);
			} else {
				counter++;
			}
		}
	}

	private CommUser getBestBidder(ArrayList<Message> bids) {
		int min_cost = Integer.MAX_VALUE;
		CommUser winner = null;
		for(Message m: bids) {
			ParcelBid bid = (ParcelBid) m.getContents();
			if(bid.getCost() < min_cost) {
				min_cost = bid.getCost();
				winner = m.getSender();
			}
		}
		return winner;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}
	
	@Override
	public String toString(){
		return "Parcel " + parcel_id + " @ " + position + " ; destination = " + destination + "; " + (sold? "" : " not" ) + " sold";
	}
	
	public void pickUp(){
		this.model.removeObject(this);
		this.pickedUp = true;
	}
	
	public void dropAndDeliver(Point pos) {
		this.model.addObjectAt(this, pos);
		delivered = true;
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		this.model = (VirtualGraphRoadModel) model;
		this.model.addObjectAt(this, position);
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.sim = api;		
	}
	
}
