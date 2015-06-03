package model.road;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PathPheromoneFactory {

	private static Queue<PathPheromone> unusedPheremoneInstances = new ArrayDeque<PathPheromone>();

	public static PathPheromone build(int timeStamp, Move origin, Move goal,
			int robot) {
		PathPheromone pheromone = unusedPheremoneInstances.poll();
		if (pheromone == null) {
			pheromone = new PathPheromone();
		}
		pheromone.setTimeStamp(timeStamp);
		pheromone.setOrigin(origin);
		pheromone.setGoal(goal);
		pheromone.setRobot(robot);
		pheromone.resetLife();
		return pheromone;
	}

	static void release(PathPheromone pheromone) {
		unusedPheremoneInstances.add(pheromone);
	}
}
