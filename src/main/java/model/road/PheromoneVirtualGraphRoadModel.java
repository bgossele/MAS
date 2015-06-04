package model.road;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import users.VirtualUser;

import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;

public class PheromoneVirtualGraphRoadModel extends VirtualGraphRoadModel
		implements TickListener {

	private static final int PATH_PHEROMONE_LIFETIME = 2;

	protected volatile Map<Loc, List<PathPheromone>> pathPheromones = new HashMap<Loc, List<PathPheromone>>();

	protected volatile Map<Loc, List<ExploredPheromone>> exploredPheromones = new HashMap<Loc, List<ExploredPheromone>>();

	public PheromoneVirtualGraphRoadModel(Graph<? extends ConnectionData> pGraph) {
		super(pGraph);
	}

	public void dropPheromone(VirtualUser user, Pheromone pheromone) {
		Loc location = objLocs.get(user);
		if (pheromone instanceof PathPheromone) {
			List<PathPheromone> list = pathPheromones.get(location);
			if (list == null) {
				list = new LinkedList<PathPheromone>();
				pathPheromones.put(location, list);
			}
			list.add((PathPheromone) pheromone);
		} else if (pheromone instanceof ExploredPheromone) {
			List<ExploredPheromone> list = exploredPheromones.get(location);
			if (list == null) {
				list = new LinkedList<ExploredPheromone>();
				exploredPheromones.put(location, list);
			}
			list.add((ExploredPheromone) pheromone);
		}
	}

	public List<PathPheromone> readPathPheromones(VirtualUser user) {
		return pathPheromones.get(objLocs.get(user));
	}

	public List<ExploredPheromone> readExploredPheromones(VirtualUser user) {
		return exploredPheromones.get(objLocs.get(user));
	}

	@Override
	public void tick(TimeLapse timeLapse) {
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// Update the life of pheromones and remove pheromones if they are
		// expired.
		checkExploredPheromoneLifeTime();
		checkPathPheromoneLifeTime();
	}

	private void checkPathPheromoneLifeTime() {
		Iterator<List<PathPheromone>> pheromoneListIterator = pathPheromones
				.values().iterator();
		while (pheromoneListIterator.hasNext()) {
			Iterator<PathPheromone> pheromoneIterator = pheromoneListIterator
					.next().iterator();
			while (pheromoneIterator.hasNext()) {
				PathPheromone pheromone = pheromoneIterator.next();
				pheromone.addTickToLife();
				if (pheromone.getLifeTime() >= PATH_PHEROMONE_LIFETIME) {
					pheromoneIterator.remove();
					PathPheromoneFactory.release(pheromone);
				}
			}
		}
	}

	private void checkExploredPheromoneLifeTime() {
		Iterator<List<ExploredPheromone>> pheromoneListIterator = exploredPheromones
				.values().iterator();
		while (pheromoneListIterator.hasNext()) {
			Iterator<ExploredPheromone> pheromoneIterator = pheromoneListIterator
					.next().iterator();
			while (pheromoneIterator.hasNext()) {
				ExploredPheromone pheromone = pheromoneIterator.next();
				pheromone.addTickToLife();
				if (pheromone.getLifeTime() >= PATH_PHEROMONE_LIFETIME) {
					pheromoneIterator.remove();
					ExploredPheromoneFactory.release(pheromone);
				}
			}
		}
	}

}
