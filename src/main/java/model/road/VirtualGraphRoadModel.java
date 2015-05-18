package model.road;

import static com.github.rinde.rinsim.geom.Graphs.unmodifiableGraph;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;


import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

import javax.annotation.Nullable;

import model.road.VirtualGraphRoadModel.Loc;

import org.apache.commons.math3.random.RandomGenerator;

import users.VirtualUser;

import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.base.VerifyException;

public class VirtualGraphRoadModel extends AbstractVirtualRoadModel<Loc> {

	/**
	 * A mapping of {@link VirtualUser} to location.
	 */
	protected volatile Map<VirtualUser, Loc> objLocs = new HashMap<VirtualUser, Loc>();

	/**
	 * Precision.
	 */
	protected static final double DELTA = 0.000001;

	/**
	 * The graph that is used as road structure.
	 */
	protected final Graph<? extends ConnectionData> graph;

	/**
	 * Creates a new instance using the specified {@link Graph} as road
	 * structure.
	 * 
	 * @param pGraph
	 *            The graph which will be used as road structure.
	 */
	public VirtualGraphRoadModel(Graph<? extends ConnectionData> pGraph) {
		super();
		graph = pGraph;
	}

	public void addObjectAt(VirtualUser newObj, Point pos) {
		checkArgument(graph.containsNode(pos),
				"Object must be initiated on a crossroad.");
		super.addObjectAt(newObj, asLoc(pos));
	}
	
	@Override
	public void moveTo(VirtualUser user, Point destination) {
		checkArgument(super.containsObject(user),
				"User must be on graph before being moved.");
		super.removeObject(user);
		super.addObjectAt(user, destination);
	}

	/**
	 * Checks if the point is on a connection.
	 * 
	 * @param p
	 *            The point to check.
	 * @return <code>true</code> if the point is on a connection,
	 *         <code>false</code> otherwise.
	 */
	protected static boolean isOnConnection(Point p) {
		return p instanceof Loc && ((Loc) p).isOnConnection();
	}

	/**
	 * Checks whether the specified location is valid.
	 * 
	 * @param l
	 *            The location to check.
	 * @return The location if it is valid.
	 * @throws VerifyException
	 *             if the location is not valid.
	 */
	protected Loc verifyLocation(Loc l) {
		verify(l.isOnConnection() || graph.containsNode(l),
				"Location points to non-existing vertex: %s.", l);
		verify(!l.isOnConnection()
				|| graph.hasConnection(l.conn.get().from(), l.conn.get().to()),
				"Location points to non-existing connection: %s.", l.conn);
		return l;
	}

	/**
	 * Precondition: the specified {@link Point}s are part of a
	 * {@link Connection} which exists in the {@link Graph}. This method figures
	 * out which {@link Connection} the two {@link Point}s share.
	 * 
	 * @param from
	 *            The start point.
	 * @param to
	 *            The end point.
	 * @return The {@link Connection} shared by the points.
	 */
	protected Connection<?> getConnection(Point from, Point to) {
		final boolean fromIsOnConn = isOnConnection(from);
		final boolean toIsOnConn = isOnConnection(to);
		Connection<?> conn;
		final String errorMsg = "The specified points must be part of the same connection.";
		if (fromIsOnConn) {
			final Loc start = (Loc) from;
			if (toIsOnConn) {
				checkArgument(start.isOnSameConnection((Loc) to), errorMsg);
			} else {
				checkArgument(start.conn.get().to().equals(to), errorMsg);
			}
			conn = start.conn.get();

		} else if (toIsOnConn) {
			final Loc end = (Loc) to;
			checkArgument(end.conn.get().from().equals(from), errorMsg);
			conn = end.conn.get();
		} else {
			checkArgument(
					graph.hasConnection(from, to),
					"The specified points (%s and %s) must be part of an existing connection in the graph.",
					from, to);
			conn = graph.getConnection(from, to);
		}
		return conn;
	}

	/**
	 * @return An unmodifiable view on the graph.
	 */
	public Graph<? extends ConnectionData> getGraph() {
		return unmodifiableGraph(graph);
	}

	/**
	 * Retrieves the connection which the specified {@link RoadUser} is at. If
	 * the road user is at a vertex {@link Optional#absent()} is returned
	 * instead.
	 * 
	 * @param obj
	 *            The object which position is checked.
	 * @return A {@link Connection} if <code>obj</code> is on one,
	 *         {@link Optional#absent()} otherwise.
	 */
	public Optional<? extends Connection<?>> getConnection(VirtualUser obj) {
		final Loc point = objLocs.get(obj);
		if (isOnConnection(point)) {
			return Optional.of(graph.getConnection(point.conn.get().from(),
					point.conn.get().to()));
		}
		return Optional.absent();
	}
	
	public Map<VirtualUser, Loc> getObjectLocations() {
		return objLocs;
	}

	public Map<VirtualUser, Loc> getObjectLocations() {
		return objLocs;
	}

	/**
	 * Creates a new {@link Loc} based on the provided {@link Point}.
	 * 
	 * @param p
	 *            The point used as input.
	 * @return A {@link Loc} with identical position as the specified
	 *         {@link Point}.
	 */
	protected static Loc asLoc(Point p) {
		if (p instanceof Loc) {
			return (Loc) p;
		}
		return new Loc(p.x, p.y, null, -1, 0);
	}

	/**
	 * Creates a new {@link Loc} based on the provided {@link Connection} and
	 * the relative position. The new {@link Loc} will be placed on the
	 * connection with a distance of <code>relativePos</code> to the start of
	 * the connection.
	 * 
	 * @param conn
	 *            The {@link Connection} to use.
	 * @param relativePos
	 *            The relative position measured from the start of the
	 *            {@link Connection}.
	 * @return A new {@link Loc}
	 */
	protected static Loc newLoc(Connection<? extends ConnectionData> conn,
			double relativePos) {
		final Point diff = Point.diff(conn.to(), conn.from());
		final double roadLength = conn.getLength();

		final double perc = relativePos / roadLength;
		if (perc + DELTA >= 1) {
			return new Loc(conn.to().x, conn.to().y, null, -1, 0);
		}
		return new Loc(conn.from().x + perc * diff.x, conn.from().y + perc
				* diff.y, conn, roadLength, relativePos);
	}

	protected Point locObj2point(Loc locObj) {
		return locObj;
	}

	protected Loc point2LocObj(Point point) {
		return asLoc(point);
	}

	public Point getRandomPosition(RandomGenerator rnd) {
		return graph.getRandomNode(rnd);
	}
	
	public ArrayList<Point> getNeighbors(Point point){
		//TODO implement
		return new ArrayList<Point>();
	}

	/**
	 * Location representation in a {@link Graph} for the {@link GraphRoadModel}
	 * .
	 * 
	 * @author Rinde van Lon
	 */
	protected static final class Loc extends Point {
		private static final long serialVersionUID = 7070585967590832300L;
		/**
		 * The length of the current connection.
		 */
		public final double connLength;
		/**
		 * The relative position of this instance compared to the start of the
		 * connection.
		 */
		public final double relativePos;
		/**
		 * The {@link Connection} which this position is on if present.
		 */
		public final Optional<? extends Connection<?>> conn;

		Loc(double pX, double pY,
				@Nullable Connection<? extends ConnectionData> pConn,
				double pConnLength, double pRelativePos) {
			super(pX, pY);
			connLength = pConnLength;
			relativePos = pRelativePos;
			conn = Optional.fromNullable(pConn);
		}

		/**
		 * @return <code>true</code> if the position is on a connection.
		 */
		public boolean isOnConnection() {
			return conn.isPresent();
		}

		/**
		 * Check if this position is on the same connection as the provided
		 * location.
		 * 
		 * @param l
		 *            The location to compare with.
		 * @return <code>true</code> if both {@link Loc}s are on the same
		 *         connection, <code>false</code> otherwise.
		 */
		public boolean isOnSameConnection(Loc l) {
			return conn.equals(l.conn);
		}
	}

}
