
import java.util.LinkedList;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class Robot implements TickListener, WarehouseAgent, CommUser {
	private final RandomGenerator rng;
	private Optional<CollisionGraphRoadModel> roadModel;
	private Optional<Point> destination;
	private LinkedList<Point> path;
	private Optional<CommDevice> device;

	Robot(RandomGenerator r) {
		rng = r;
		roadModel = Optional.absent();
		destination = Optional.absent();
		path = new LinkedList<>();
		device = Optional.absent();
	}

	@Override
	public void initRoadUser(RoadModel model) {
		roadModel = Optional.of((CustomCollisionGraphRoadModel) model);
		Point p;
		do {
			p = model.getRandomPosition(rng);
		} while (roadModel.get().isOccupied(p));
		roadModel.get().addObjectAt(this, p);

	}

	@Override
	public double getSpeed() {
		return 1;
	}

	void nextDestination() {
		destination = Optional.of(roadModel.get().getRandomPosition(rng));
		path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
				destination.get()));
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (!destination.isPresent()) {
			nextDestination();
		}

		roadModel.get().followPath(this, path, timeLapse);

		if (roadModel.get().getPosition(this).equals(destination.get())) {
			nextDestination();
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
	}

	@Override
	public boolean isPhysical() {
		return true;
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
