package model.road;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import users.ExplorationAnt;

public class ExploredPheromoneFactory {

	private static Queue<ExploredPheromone> unusedPheromoneInstances = new ArrayDeque<ExploredPheromone>();

	public static ExploredPheromone build(int tick, int robot) {
		ExploredPheromone pheromone = unusedPheromoneInstances.poll();
		if (pheromone == null) {
			pheromone = new ExploredPheromone();
		}
		pheromone.setTick(tick);
		pheromone.setRobot(robot);
		pheromone.resetLife();
		return pheromone;
	}

	static void release(ExploredPheromone pheromone) {
		unusedPheromoneInstances.add(pheromone);
	}
}
