package users;

import java.util.LinkedList;

import model.road.Pheromone;
import model.road.PheromoneVirtualGraphRoadModel;
import model.road.VirtualRoadModel;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.SimulatorUser;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

public class ReservationAnt implements VirtualUser, TickListener, SimulatorUser {

	private PheromoneVirtualGraphRoadModel roadModel;
	private LinkedList<Point> path;
	private LinkedList<Pheromone> pheromones;
	private Point start;
	private SimulatorAPI simulator;
	
	public void set(Point start, LinkedList<Point> path, LinkedList<Pheromone> pheromones, SimulatorAPI sim) {
		this.path = path;
		this.start = start;
		this.pheromones = pheromones;
		this.simulator = sim;
	}
	
	void reset(){
		this.path = null;
		this.start = null;
		this.pheromones = null;
		this.roadModel = null;
	}
	
	ReservationAnt(){}

	@Override
	public void tick(TimeLapse timeLapse) {
		if(path.isEmpty() == false){
			Point nextHop = path.pop();
			Pheromone p = pheromones.pop();
			roadModel.moveTo(this, nextHop);
			roadModel.dropPheromone(this, p);
			System.out.println("Dropped " + p + " at " + nextHop);
		} else {
			simulator.unregister(this);
			ReservationAntFactory.returnAnt(this);
		}
		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = (PheromoneVirtualGraphRoadModel) model;	
		roadModel.addObjectAt(this, start);
		System.out.println("Reservation ant initialised");
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;		
	}

}
