package model.road;

import model.road.Move;
import model.road.Pheromone;
import model.road.PheromoneFactory;

import org.junit.After;
import org.junit.Test;

public class PheremoneTest {

	private Pheromone pheremone1;
	private Pheromone pheremone2;

	@After
	public void tearDown() {
		pheremone1 = null;
		pheremone2 = null;
	}

	@Test
	public void testPheremoneConstruction() {
		pheremone1 = PheromoneFactory.build(1, Move.NORTH, Move.SOUTH, 2);
		assert (pheremone1.getOrigin().equals(Move.NORTH));
		assert (pheremone1.getGoal().equals(Move.SOUTH));
		assert (pheremone1.getTimeStamp() == 1);
		assert (pheremone1.getRobot() == 2);
	}

	@Test
	public void testPheremoneConstructionReturnAndReconstruction() {
		pheremone1 = PheromoneFactory.build(1, Move.NORTH, Move.SOUTH, 2);
		PheromoneFactory.returnPheromone(pheremone1);
		pheremone2 = PheromoneFactory.build(11, Move.EAST, Move.WEST, 22);
		assert (pheremone1.getOrigin().equals(Move.NORTH));
		assert (pheremone1.getGoal().equals(Move.SOUTH));
		assert (pheremone1.getTimeStamp() == 1);
		assert (pheremone1.getRobot() == 2);
		assert (pheremone2.getOrigin().equals(Move.EAST));
		assert (pheremone2.getGoal().equals(Move.WEST));
		assert (pheremone2.getTimeStamp() == 11);
		assert (pheremone2.getRobot() == 22);
		assert (pheremone1 == pheremone2);
	}

}
