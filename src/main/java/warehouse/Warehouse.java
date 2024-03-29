package warehouse;

import static com.google.common.collect.Lists.newArrayList;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;

import org.eclipse.swt.graphics.RGB;

import rendering.HybridWarehouseRenderer;
import rendering.VirtualUserRenderer;
import model.road.PheromoneVirtualGraphRoadModel;
import users.ExplorationAnt;
import users.Parcel;
import users.ParcelManager;
import users.ReservationAnt;
import users.Robot;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public final class Warehouse {

	// The number of robots that will be placed in the warehouse
	private static final int N_ROBOTS = 8;

	// The number of parcel that will be spawned in the warehouse
	public static final int N_PARCELS = 25;

	// The warehouse topology that will be used.
	// 0 is a small warehouse with 8 vertical corridors that are connected by 2
	// horizontal corridors.
	// 1 is a larger warhouse with a small bottleneck in the middle
	public static final int WAREHOUSE_TOPOLOGY = 0;

	private static final double VEHICLE_LENGTH = 2d;
	public static final String EXPERIMENT_TAG = "simple_2_ta";
	public static final int EXP_ITERATION = 6;

	private Warehouse() {
	}

	/**
	 * @param args
	 *            - No args.
	 */
	public static void main(String[] args) {

		ListenableGraph<LengthData> g;
		if (WAREHOUSE_TOPOLOGY == 0) {
			g = createSimpleBiGraph();
		} else if (WAREHOUSE_TOPOLOGY == 1) {
			g = createGraph();
		}
		PheromoneVirtualGraphRoadModel pheromoneVirtualModel = new PheromoneVirtualGraphRoadModel(
				g);

		Simulator sim = Simulator
				.builder()
				.addModel(
						CollisionGraphRoadModel.builder(g)
								.setVehicleLength(VEHICLE_LENGTH).build())
				.addModel(CommModel.builder().build())
				.addModel(pheromoneVirtualModel).build();

		Random rand = new Random();
		sim.getRandomGenerator().setSeed(rand.nextInt());
		HashSet<Point> robotPos = new HashSet<Point>();

		for (int i = 0; i < N_ROBOTS; i++) {
			Point start;
			do {
				start = pheromoneVirtualModel.getRandomPosition(sim
						.getRandomGenerator());
			} while (robotPos.contains(start));
			robotPos.add(start);
			sim.register(new Robot(i, start, N_ROBOTS));
			// sim.register(new DummyRobot(i, start));
		}

		sim.addTickListener(new ParcelManager(pheromoneVirtualModel, sim
				.getRandomGenerator(), sim));

		sim.addTickListener(pheromoneVirtualModel);

		View.create(sim)
				.with(HybridWarehouseRenderer.builder().setMargin(
						VEHICLE_LENGTH))
				.with(AGVRenderer.builder().useDifferentColorsForVehicles())
				.with(VirtualUserRenderer
						.builder()
						// .addTypeToRender(ExplorationAnt.class)
						.addTypeToRender(ReservationAnt.class)
						.addTypeToRender(Parcel.class)
						.addColorAssociation(ExplorationAnt.class,
								new RGB(0, 0, 255))
						.addColorAssociation(ReservationAnt.class,
								new RGB(255, 0, 0))
						.addColorAssociation(Parcel.class, new RGB(0, 0, 0)))
				.show();
	}

	public static ImmutableTable<Integer, Integer, Point> createMatrix(
			int cols, int rows, Point offset) {
		final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
				.builder();
		for (int c = 0; c < cols; c++) {
			for (int r = 0; r < rows; r++) {
				builder.put(r, c, new Point(offset.x + c * VEHICLE_LENGTH * 2,
						offset.y + r * VEHICLE_LENGTH * 2));
			}
		}
		return builder.build();
	}

	public static ListenableGraph<LengthData> createSimpleGraph() {
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

	public static ListenableGraph<LengthData> createSimpleBiGraph() {
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
			Graphs.addBiPath(g, path);
		}

		Graphs.addBiPath(g, matrix.row(0).values());
		Graphs.addBiPath(
				g,
				Lists.reverse(newArrayList(matrix.row(
						matrix.rowKeySet().size() - 1).values())));

		return new ListenableGraph<>(g);
	}

	public static ListenableGraph<LengthData> createGraph() {
		final Graph<LengthData> g = new TableGraph<>();

		final Table<Integer, Integer, Point> matrix = createMatrix(5, 10,
				new Point(0, 0));
		for (final Map<Integer, Point> column : matrix.columnMap().values()) {
			Graphs.addBiPath(g, column.values());
		}
		Graphs.addBiPath(g, matrix.row(0).values());
		Graphs.addBiPath(g, matrix.row(5).values());
		Graphs.addBiPath(g, matrix.row(9).values());

		final Table<Integer, Integer, Point> matrix2 = createMatrix(10, 7,
				new Point(20, 8));
		for (final Map<Integer, Point> row : matrix2.rowMap().values()) {
			Graphs.addBiPath(g, row.values());
		}
		Graphs.addBiPath(g, matrix2.column(0).values());
		Graphs.addBiPath(g, matrix2.column(matrix2.columnKeySet().size() - 1)
				.values());

		Graphs.addBiPath(g, matrix2.get(2, 0), matrix.get(4, 4));
		Graphs.addBiPath(g, matrix.get(5, 4), matrix2.get(3, 0));

		return new ListenableGraph<>(g);
	}
}
