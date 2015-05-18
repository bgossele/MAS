package users;
import java.util.LinkedList;

import model.road.VirtualGraphRoadModel;

import org.apache.commons.math3.random.RandomGenerator;

import warehouse.Warehouse;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class ExplorationAnt implements TickListener, VirtualUser, CommUser {
	private Optional<VirtualGraphRoadModel> roadModel;
	private Optional<Point> previousPosition;
	private Optional<Point> position;
	private Optional<Point> destination;
	private LinkedList<Point> path;
	private Optional<CommDevice> device;

	ExplorationAnt(Point start) {
		roadModel = Optional.absent();
		previousPosition = Optional.absent();
		position = Optional.of(start);
		destination = Optional.absent();
		path = new LinkedList<>();
		device = Optional.absent();
	}

	ExplorationAnt(Point start, Point destination) {
		roadModel = Optional.absent();
		previousPosition = Optional.absent();
		position = Optional.of(start);
		this.destination = Optional.of(destination);
		path = new LinkedList<>();
		device = Optional.absent();
	}

	@Override
	public void initVirtualUser(VirtualGraphRoadModel model) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void tick(TimeLapse timeLapse) {
		if (destination.equals(Optional.absent())
				|| destination.get().equals(roadModel.get().getPosition(this))) {
			destination = Optional.absent();
			for (Point des : roadModel.get().getNeighbors(position.get())) {
				if(des.equals(previousPosition.get())) {
					continue;
				} else if (destination.equals(Optional.absent())) {
					destination = Optional.of(des);
				} else{
					ExplorationAnt ant = new ExplorationAnt(position.get(), des);
					Warehouse.getSimulator().register(ant);
				}
			}
		} else {
			roadModel.get().moveTo(this, destination.get(), timeLapse);
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
	}

	@Override
	public Optional<Point> getPosition() {
		if (roadModel.get().containsObject(this)) {
			return Optional.of(roadModel.get().getPosition(this));
		}
		return Optional.absent();
	}

	@Override
	public void setCommDevice(CommDeviceBuilder builder) {
		device = Optional.of(builder.build());
	}


}