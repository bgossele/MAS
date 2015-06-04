package users;

import java.util.ArrayDeque;
import java.util.Queue;

import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.geom.Point;

public class ExplorationAntFactory {

	private static Queue<ExplorationAnt> unusedAntInstances = new ArrayDeque<ExplorationAnt>();

	private static int counter = 0;

	public static ExplorationAnt build(Point start, CommUser mothership,
			int robotId, int tick, int hopLimit, SimulatorAPI sim) {
		ExplorationAnt ant = unusedAntInstances.poll();
		if (ant == null) {
			ant = new ExplorationAnt(counter);
			counter++;
			sim.register(ant);
		} else {
			// ant.reset();
		}
		ant.set(start, mothership, robotId, tick, hopLimit, sim);
		return ant;
	}

	public static ExplorationAnt build(Point start, Point destination,
			CommUser mothership, int robotId, int tick, int hopLimit,
			SimulatorAPI sim) {
		ExplorationAnt ant = unusedAntInstances.poll();
		if (ant == null) {
			ant = new ExplorationAnt(counter);
			counter++;
			sim.register(ant);
		} else {
			// ant.reset();
		}
		ant.set(start, destination, mothership, robotId, tick, hopLimit, sim);
		return ant;
	}

	static void returnAnt(ExplorationAnt ant) {
		ant.reset();
		unusedAntInstances.add(ant);
	}

}
