package model.road;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import users.ExplorationAntFactory;
import users.ExplorationAnt;
import users.Robot;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public class PheromoneVirtualGraphRoadModelTest {

	Point point;
	Robot robot;
	ExplorationAnt ant;

	@Test
	public void test() {
		ListenableGraph<LengthData> g = createSimpleGraph();

		PheromoneVirtualGraphRoadModel pheromoneVirualModel = new PheromoneVirtualGraphRoadModel(
				g);

		Simulator sim = Simulator
				.builder()
				.addModel(
						CollisionGraphRoadModel.builder(g).setVehicleLength(2d)
								.build()).addModel(pheromoneVirualModel)
				.addModel(CommModel.builder().build()).build();

		sim.addTickListener(pheromoneVirualModel);

		PheromoneTesterTicketListner listner = new PheromoneTesterTicketListner(
				pheromoneVirualModel, sim);

		sim.addTickListener(listner);

		point = new Point(16, 16);
		robot = new Robot(sim.getRandomGenerator());
		sim.register(robot);
		ant = ExplorationAntFactory.build(point, robot, 5, 5, sim);

		sim.start();
	}

	static ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
			int rows, Point offset) {
		final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
				.builder();
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				builder.put(r, c, new Point(offset.x + c * 2d * 2, offset.y + r
						* 2d * 2));
			}
		}
		return builder.build();
	}

	static ListenableGraph<LengthData> createSimpleGraph() {
		final Graph<LengthData> g = new TableGraph<>();

		final Table<Integer, Integer, Point> matrix = createMatrix(8, 6,
				new Point(0, 0));

		for (int i = 0; i < matrix.columnMap().size(); i++) {

			Iterable<Point> path;
			if (i % 2 == 0) {
				path = Lists.reverse(newArrayList(matrix.column(i).values()));
			} else {
				path = matrix.column(i).values();
			}
			Graphs.addPath(g, path);
		}

		Graphs.addPath(g, matrix.row(0).values());
		Graphs.addPath(
				g,
				Lists.reverse(newArrayList(matrix.row(
						matrix.rowKeySet().size() - 1).values())));

		return new ListenableGraph<>(g);
	}

	private class PheromoneTesterTicketListner implements TickListener {

		private PheromoneVirtualGraphRoadModel pheromoneVirtualModel;

		private Simulator simulator;

		private int ticks = 0;

		public PheromoneTesterTicketListner(
				PheromoneVirtualGraphRoadModel pheromoneVirtualModel,
				Simulator simulator) {
			this.pheromoneVirtualModel = pheromoneVirtualModel;
			this.simulator = simulator;
		}

		@Override
		public void tick(TimeLapse timeLapse) {
			ticks++;
			switch (ticks) {
			case 2:
				Pheromone pheromone1 = PheromoneFactory.build(0, Move.WAIT,
						Move.WAIT, 0);
				Pheromone pheromone2 = PheromoneFactory.build(5, Move.NORTH,
						Move.SOUTH, 10);
				pheromoneVirtualModel.dropPheromone(ant, pheromone1);
				pheromoneVirtualModel.dropPheromone(ant, pheromone2);
				break;
			case 3:
				List<Pheromone> list = pheromoneVirtualModel
						.readPheromones(ant);
				boolean pheromone1Present = false;
				boolean pheromone2Present = false;
				for (Pheromone pheromone : list) {
					if (pheromone.getTimeStamp() == 0) {
						pheromone1Present = true;
						assert (pheromone.getLifeTime() == 1);
					} else if (pheromone.getTimeStamp() == 5) {
						pheromone2Present = true;
						assert (pheromone.getLifeTime() == 1);
					}
				}
				if (pheromone1Present == false || pheromone2Present == false) {
					fail("Pheromone missing");
				}
				break;
			case 4:
				List<Pheromone> list2 = pheromoneVirtualModel
						.readPheromones(ant);
				boolean pheromone1Present2 = false;
				boolean pheromone2Present2 = false;
				for (Pheromone pheromone : list2) {
					if (pheromone.getTimeStamp() == 0) {
						pheromone1Present2 = true;
					} else if (pheromone.getTimeStamp() == 5) {
						pheromone2Present2 = true;
					}
				}
				if (pheromone1Present2 == true || pheromone2Present2 == true) {
					fail("Pheromone not Removed");
				}
				simulator.stop();
				break;
			}
		}

		@Override
		public void afterTick(TimeLapse timeLapse) {
		}

	}

}
