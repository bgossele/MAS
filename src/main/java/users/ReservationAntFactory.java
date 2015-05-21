package users;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;

import model.road.Pheromone;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;

public class ReservationAntFactory {

	private static Queue<ReservationAnt> unusedAntInstances = new ArrayDeque<ReservationAnt>();

	public static ReservationAnt build(Point start, LinkedList<Point> path, LinkedList<Pheromone> pheromones, SimulatorAPI sim) {
		ReservationAnt ant = unusedAntInstances.poll();
		if (ant == null) {
			ant = new ReservationAnt();
		} else {
			ant.reset();
		}
		ant.set(start, path, pheromones, sim);
		sim.register(ant);
		return ant;
	}

	static void returnAnt(ReservationAnt ant) {
		ant.reset();
		unusedAntInstances.add(ant);
	}

}
