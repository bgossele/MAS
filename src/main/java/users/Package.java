package users;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Optional;

public class Package implements CommUser, TickListener{
	
	private Point position;
	private Point destination;
	private CommDevice device;
	private boolean sold;

	Package(Point position, Point destination) {
		this.position = position;
		this.destination = destination;
		this.sold = false;
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
		// TODO Auto-generated method stub		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}
	
	@Override
	public String toString(){
		return "Package @ " + position + " ; destination = " + destination + "; " + (sold? "" : " not" ) + " sold";
	}
	
}
