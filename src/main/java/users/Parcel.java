package users;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import communication.ParcelAllocation;
import communication.ParcelBid;
import communication.ParcelOffer;

public class Parcel implements CommUser, TickListener{
	
	private Point position;
	private Point destination;
	private CommDevice device;
	private boolean sold;
	private boolean forSale;
	private final int parcel_id;

	Parcel(int parcel_id, Point position, Point destination) {
		this.position = position;
		this.destination = destination;
		this.sold = false;
		this.forSale = false;
		this.parcel_id = parcel_id;
	}
	
	@Override
	public Optional<Point> getPosition() {
		return Optional.of(position);
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = builder.build();		
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if(!sold && !forSale){ // STart auction
			device.broadcast(new ParcelOffer(position, destination));
			this.forSale = true;
		} else if (forSale) {
			ArrayList<Message> bids = new ArrayList<Message>();
			List<Message> messages = device.getUnreadMessages();
			for(Message m: messages) {
				MessageContents content = m.getContents();
				if(content instanceof ParcelBid){
					bids.add(m);
				}
			}
			if(bids.size() > 0) {
				CommUser winner = getBestBidder(bids);
				device.send(new ParcelAllocation(), winner);
				this.sold = true;
			} else {
				System.out.println("Parcel " + parcel_id + " received no bids.");
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
	
}
