package model.road;

public class ExploredPheromone extends Pheromone{
	
	int tick;
	
	ExploredPheromone() {	
	}

	public int getTick() {
		return tick;
	}
	
	public void setTick(int tick) {
		this.tick = tick;
	}
	
}
