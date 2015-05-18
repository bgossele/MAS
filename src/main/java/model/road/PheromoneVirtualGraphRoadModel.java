package model.road;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.road.VirtualGraphRoadModel.Loc;
import users.VirtualUser;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;

public class PheromoneVirtualGraphRoadModel extends VirtualGraphRoadModel{
	
	protected volatile Map<Loc,List<Pheromone>> pheromones = new HashMap<Loc, List<Pheromone>>();

	public PheromoneVirtualGraphRoadModel(Graph<? extends ConnectionData> pGraph) {
		super(pGraph);
	}
	
	public void dropPheromone(VirtualUser user, Pheromone pheromone) {
		pheromones.get(objLocs.get(user));
	}

}
