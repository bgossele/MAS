package model.road;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class ExploredPheromoneFactory {

	private static final int MAX_UNUSED_PHEROMONE_INSTANCES = 200;

	private static Queue<ExploredPheromone> unusedPheromoneInstances = new ArrayBlockingQueue<ExploredPheromone>(
			MAX_UNUSED_PHEROMONE_INSTANCES);

	public static ExploredPheromone build(int tick, int robot) {
		ExploredPheromone pheromone = unusedPheromoneInstances.poll();
		if (pheromone == null) {
			pheromone = new ExploredPheromone();
		}
		pheromone.setTick(tick);
		pheromone.setRobot(robot);
		return pheromone;
	}

	static void release(ExploredPheromone pheromone) {
		unusedPheromoneInstances.add(pheromone);
	}
}
