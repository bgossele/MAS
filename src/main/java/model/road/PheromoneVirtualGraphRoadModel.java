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

	private static final int PHEROMONE_LIFETIME = 2;

	protected volatile Map<Loc, List<Pheromone>> pheromones = new HashMap<Loc, List<Pheromone>>();

	public PheromoneVirtualGraphRoadModel(Graph<? extends ConnectionData> pGraph) {
		super(pGraph);
	}

	public void dropPheromone(VirtualUser user, Pheromone pheromone) {
		Loc location = objLocs.get(user);
		List<Pheromone> list = pheromones.get(location);
		if (list == null) {
			list = new LinkedList<Pheromone>();
			pheromones.put(location, list);
		}
		list.add(pheromone);
	}

	public List<Pheromone> readPheromones(VirtualUser user) {
		return pheromones.get(objLocs.get(user));
	}

	@Override
	public void tick(TimeLapse timeLapse) {
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// Update the life of pheromones and remove pheromones if they are
		// expired.
		Iterator<List<Pheromone>> pheromoneListIterator = pheromones.values()
				.iterator();
		while (pheromoneListIterator.hasNext()) {
			Iterator<Pheromone> pheromoneIterator = pheromoneListIterator
					.next().iterator();
			while (pheromoneIterator.hasNext()) {
				Pheromone pheromone = pheromoneIterator.next();
				pheromone.addTickToLife();
//				System.out.println("Pheromone life : " +pheromone.getLifeTime());
				if (pheromone.getLifeTime() >= PHEROMONE_LIFETIME) {
					pheromoneIterator.remove();
					PheromoneFactory.returnPheromone(pheromone);
//					System.out.println("Removing pheromone");
				}
			}
		}
	}
}
