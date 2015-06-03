package model.road;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class PheromoneFactory {

	private static final int MAX_UNUSED_PHEROMONE_INSTANCES = 200;

	private static Queue<PathPheromone> unusedPheremoneInstances = new ArrayBlockingQueue<PathPheromone>(
			MAX_UNUSED_PHEROMONE_INSTANCES);

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
		return pheromone;
	}
	
//	public static ExploredPheromone build(int timeStamp, Move origin, Move goal,
//			int robot) {
//		Pheromone pheromone = unusedPheremoneInstances.poll();
//		if (pheromone == null) {
//			pheromone = new Pheromone();
//		}
//		pheromone.setTimeStamp(timeStamp);
//		pheromone.setOrigin(origin);
//		pheromone.setGoal(goal);
//		pheromone.setRobot(robot);
//		return pheromone;
//	}

	static void returnPheromone(PathPheromone pheromone) {
		unusedPheremoneInstances.add(pheromone);
	}
}
