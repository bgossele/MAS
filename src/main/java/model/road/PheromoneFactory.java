package model.road;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PheromoneFactory {

	private static final int MAX_UNUSED_PHEROMONE_INSTANCES = 200;

	private static Queue<Pheromone> unusedPheremoneInstances = new ArrayBlockingQueue<Pheromone>(
			MAX_UNUSED_PHEROMONE_INSTANCES);

	public static Pheromone build(int timeStamp, Move origin, Move goal,
			int robot) {
		Pheromone pheromone = unusedPheremoneInstances.poll();
		if (pheromone == null) {
			pheromone = new Pheromone();
		}
		pheromone.setTimeStamp(timeStamp);
		pheromone.setOrigin(origin);
		pheromone.setGoal(goal);
		pheromone.setRobot(robot);
		return pheromone;
	}

	static void returnPheromone(Pheromone pheromone) {
		unusedPheremoneInstances.add(pheromone);
	}
}
