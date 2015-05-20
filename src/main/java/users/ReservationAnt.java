package users;

import java.util.LinkedList;
import java.util.ListIterator;

import model.road.Move;
import model.road.Pheromone;
import model.road.PheromoneFactory;
import model.road.PheromoneVirtualGraphRoadModel;
import model.road.VirtualRoadModel;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class ReservationAnt implements VirtualUser, TickListener {

	private Optional<PheromoneVirtualGraphRoadModel> roadModel;
	private LinkedList<Point> path;
	
	public ReservationAnt(LinkedList<Point> path) {
		this.path = path;
		roadModel = Optional.absent();
	}
	


	@Override
	public void tick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = Optional.of((PheromoneVirtualGraphRoadModel) model);		
	}

}
