package model.road;

import model.road.Move;
import model.road.PathPheromone;
import model.road.PathPheromoneFactory;

import org.junit.After;
import org.junit.Test;

public class PheromoneTest {

	private PathPheromone pheromone1;
	private PathPheromone pheromone2;

	@After
	public void tearDown() {
		pheromone1 = null;
		pheromone2 = null;
	}

	@Test
	public void testPheromoneConstruction() {
		pheromone1 = PathPheromoneFactory.build(1, Move.NORTH, Move.SOUTH, 2);
		assert (pheromone1.getOrigin().equals(Move.NORTH));
		assert (pheromone1.getGoal().equals(Move.SOUTH));
		assert (pheromone1.getTimeStamp() == 1);
		assert (pheromone1.getRobot() == 2);
	}

	@Test
	public void testPheremoneConstructionReturnAndReconstruction() {
		pheromone1 = PathPheromoneFactory.build(1, Move.NORTH, Move.SOUTH, 2);
		PathPheromoneFactory.release(pheromone1);
		pheromone2 = PathPheromoneFactory.build(11, Move.EAST, Move.WEST, 22);
		assert (pheromone1.getOrigin().equals(Move.NORTH));
		assert (pheromone1.getGoal().equals(Move.SOUTH));
		assert (pheromone1.getTimeStamp() == 1);
		assert (pheromone1.getRobot() == 2);
		assert (pheromone2.getOrigin().equals(Move.EAST));
		assert (pheromone2.getGoal().equals(Move.WEST));
		assert (pheromone2.getTimeStamp() == 11);
		assert (pheromone2.getRobot() == 22);
		assert (pheromone1 == pheromone2);
	}

}
