package users;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import model.road.Move;
import model.road.PathPheromone;

import org.junit.Before;
import org.junit.Test;

import warehouse.Warehouse;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Point;

public class RobotTest {
	
	CollisionGraphRoadModel model;
	double VEHICLE_LENGTH = 2d;
	Robot robot;
	
	@Before
	public void SetUp(){
		model = CollisionGraphRoadModel.builder(Warehouse.createSimpleGraph()).setVehicleLength(VEHICLE_LENGTH).build();
		robot = new Robot(0, new Point(0,0), 0);
	}

	/**@Test
	public void test() {
		fail("Not yet implemented");
	}**/
	
	@Test
	public void test_get_pheromones_0_0_to_8_0() {
		LinkedList<Point> path = new LinkedList<>(model.getShortestPathTo(new Point(0.0, 0.0), new Point(8.0, 0.0)));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(3, pheromones.size());
		assertEquals(Move.EAST, pheromones.get(0).getGoal());
		assertEquals(Move.EAST, pheromones.get(1).getGoal());
		assertEquals(Move.WAIT, pheromones.get(2).getGoal());
		
	}
	
	@Test
	public void test_get_pheromones_4_0_to_4_12() {
		LinkedList<Point> path = new LinkedList<>(model.getShortestPathTo(new Point(4.0, 0.0), new Point(4.0, 8.0)));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(3, pheromones.size());
		assertEquals(Move.SOUTH, pheromones.get(0).getGoal());
		assertEquals(Move.SOUTH, pheromones.get(1).getGoal());
		assertEquals(Move.WAIT, pheromones.get(2).getGoal());
	}
	
	@Test
	public void test_get_pheromones_0_0_to_0_0() {
		LinkedList<Point> path = new LinkedList<>(model.getShortestPathTo(new Point(0.0, 0.0), new Point(0.0, 0.0)));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(1, pheromones.size());
		assertEquals(Move.WAIT, pheromones.get(0).getGoal());
	}
	
	@Test
	public void test_get_pheromones_0_8_to_0_0() {
		LinkedList<Point> path = new LinkedList<>(model.getShortestPathTo(new Point(0.0, 8.0), new Point(0.0, 0.0)));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(3, pheromones.size());
		assertEquals(Move.NORTH, pheromones.get(0).getGoal());
		assertEquals(Move.NORTH, pheromones.get(1).getGoal());
		assertEquals(Move.WAIT, pheromones.get(2).getGoal());
	}
	
	@Test
	public void test_timestamps(){
		LinkedList<Point> path = new LinkedList<>(model.getShortestPathTo(new Point(0.0, 8.0), new Point(0.0, 0.0)));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(0, pheromones.get(0).getTimeStamp());
		assertEquals(1, pheromones.get(1).getTimeStamp());
		assertEquals(2, pheromones.get(2).getTimeStamp());
	}
	
	@Test
	public void test_timestamps_wait(){
		LinkedList<Point> path = new LinkedList<Point>();
		path.add(new Point(0, 0));
		path.add(new Point(4, 0));
		path.add(new Point(4, 0));
		path.add(new Point(8, 0));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(0, pheromones.get(0).getTimeStamp());
		assertEquals(1, pheromones.get(1).getTimeStamp());
		assertEquals(2, pheromones.get(2).getTimeStamp());
		assertEquals(3, pheromones.get(3).getTimeStamp());
		
	}
	
	@Test
	public void test_moves_wait(){
		LinkedList<Point> path = new LinkedList<Point>();
		path.add(new Point(0, 0));
		path.add(new Point(4, 0));
		path.add(new Point(4, 0));
		path.add(new Point(8, 0));
		List<PathPheromone> pheromones = robot.getPheromones(path);
		assertEquals(Move.EAST, pheromones.get(0).getGoal());
		assertEquals(Move.WAIT, pheromones.get(1).getGoal());
		assertEquals(Move.EAST, pheromones.get(2).getGoal());
		assertEquals(Move.WAIT, pheromones.get(3).getGoal());
	}

}
