

import java.util.LinkedList;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

class Robot implements TickListener, WarehouseAgent {
  private final RandomGenerator rng;
  private Optional<CollisionGraphRoadModel> roadModel;
  private Optional<Point> destination;
  private LinkedList<Point> path;

  Robot(RandomGenerator r) {
    rng = r;
    roadModel = Optional.absent();
    destination = Optional.absent();
    path = new LinkedList<>();
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
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
  public void afterTick(TimeLapse timeLapse) {}

@Override
public boolean isPhysical() {
	return true;
}

}
