package communication;

import java.util.List;

import model.road.PathPheromone;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

public class ExplorationReport implements MessageContents {

	public final Point pos;

	public final List<PathPheromone> pheromones;

	public ExplorationReport(Point pos, List<PathPheromone> pheromones) {
		this.pos = pos;
		this.pheromones = pheromones;
	}

}
