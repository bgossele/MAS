package communication;

import java.util.List;

import model.road.Pheromone;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

public class ExplorationReport implements MessageContents{
	
	public final Point pos;
	
	public final List<Pheromone> pheromones;
	
	public ExplorationReport(Point pos, List<Pheromone> pheromones){
		this.pos = pos;
		this.pheromones = pheromones;
	}
	
	
	
}
