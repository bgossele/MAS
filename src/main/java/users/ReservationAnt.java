package users;

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
	private Pheromone pheromone;
	private Point start;
	private SimulatorAPI simulator;
	
	public void set(Point start, Pheromone pheromone, SimulatorAPI sim) {
		this.start = start;
		this.pheromone = pheromone;
		this.simulator = sim;
	}
	
	void reset(){
		this.start = null;
		this.pheromone = null;
		this.roadModel = null;
	}
	
	ReservationAnt(){}

	@Override
	public void tick(TimeLapse timeLapse) {
			roadModel.dropPheromone(this, pheromone);
			simulator.unregister(this);
			ReservationAntFactory.returnAnt(this);		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}

	@Override
	public void initVirtualUser(VirtualRoadModel model) {
		roadModel = (PheromoneVirtualGraphRoadModel) model;	
		roadModel.addObjectAt(this, start);
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		simulator = api;		
	}

}
