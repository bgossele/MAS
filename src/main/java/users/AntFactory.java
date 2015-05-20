package users;

import java.util.ArrayDeque;
import java.util.Queue;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.geom.Point;

public class AntFactory {

	private static Queue<ExplorationAnt> unusedAntInstances = new ArrayDeque<ExplorationAnt>();

	public static ExplorationAnt build(Point start, Robot mothership,
			int hopLimit, int id, SimulatorAPI sim) {
		ExplorationAnt ant = unusedAntInstances.poll();
		if (ant == null) {
			ant = new ExplorationAnt();
			sim.register(ant);
		} else {
			ant.reset();
		}
		ant.set(start, mothership, hopLimit, id, sim);
		return ant;
	}

	public static ExplorationAnt build(Point start, Point destination,
			Robot mothership, int hopLimit, int id, SimulatorAPI sim) {
		ExplorationAnt ant = unusedAntInstances.poll();
		if (ant == null) {
			ant = new ExplorationAnt();
			sim.register(ant);
		} else {
			ant.reset();
		}
		ant.set(start, destination, mothership, hopLimit, id, sim);
		return ant;
	}

	static void returnAnt(ExplorationAnt ant) {
		ant.reset();
		unusedAntInstances.add(ant);
	}

}
