import static com.google.common.collect.Lists.newArrayList;

import java.util.Map;

import com.github.rinde.rinsim.core.Simulator;
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

/**
 * Example showcasing the {@link CollisionGraphRoadModel} with an
 * {@link WarehouseRenderer} and {@link AGVRenderer}.
 * @author Rinde van Lon
 */
public final class Warehouse {

  private static final double VEHICLE_LENGTH = 2d;
  
  private static Simulator sim;
  
  public static Simulator getSimulator() {
	  return sim;
  }

  private Warehouse() {}

  /**
   * @param args - No args.
   */
  public static void main(String[] args) {

    sim = Simulator.builder()
        .addModel(CustomCollisionGraphRoadModel.builder(createSimpleGraph())
            .setVehicleLength(VEHICLE_LENGTH)
            .build())
        .build();

    for (int i = 0; i < 1; i++) {
      sim.register(new Robot(sim.getRandomGenerator()));
    }

    View.create(sim)
        .with(CustomWarehouseRenderer.builder()
            .setMargin(VEHICLE_LENGTH)
        )
        .with(AGVRenderer.builder()
            .useDifferentColorsForVehicles()
        )
        .show();
  }

  static ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
      int rows, Point offset) {
    final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
        .builder();
    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        builder.put(r, c, new Point(
            offset.x + c * VEHICLE_LENGTH * 2,
            offset.y + r * VEHICLE_LENGTH * 2));
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
    Graphs.addPath(g, Lists.reverse(newArrayList(matrix.row(
        matrix.rowKeySet().size() - 1).values())));

    return new ListenableGraph<>(g);
  }

  static ListenableGraph<LengthData> createGraph() {
    final Graph<LengthData> g = new TableGraph<>();

    final Table<Integer, Integer, Point> matrix = createMatrix(5, 10,
        new Point(0, 0));
    for (final Map<Integer, Point> column : matrix.columnMap().values()) {
      Graphs.addBiPath(g, column.values());
    }
    Graphs.addBiPath(g, matrix.row(4).values());
    Graphs.addBiPath(g, matrix.row(5).values());

    final Table<Integer, Integer, Point> matrix2 = createMatrix(10, 7,
        new Point(30, 6));
    for (final Map<Integer, Point> row : matrix2.rowMap().values()) {
      Graphs.addBiPath(g, row.values());
    }
    Graphs.addBiPath(g, matrix2.column(0).values());
    Graphs.addBiPath(g, matrix2.column(matrix2.columnKeySet().size() -
        1).values());

    Graphs.addPath(g, matrix2.get(2, 0), matrix.get(4, 4));
    Graphs.addPath(g, matrix.get(5, 4), matrix2.get(4, 0));

    return new ListenableGraph<>(g);
  }
}
