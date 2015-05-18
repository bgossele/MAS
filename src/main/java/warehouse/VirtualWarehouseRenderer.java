package warehouse;

import static com.google.common.base.Preconditions.checkArgument;

import model.road.VirtualGraphRoadModel;

import org.eclipse.swt.graphics.GC;

import users.VirtualUser;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.CanvasRendererBuilder;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;

public final class VirtualWarehouseRenderer implements CanvasRenderer {

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
		// TODO Is dit wel nodig? Gebeurt ook al in de WarehouseRenderer, natuurlijk...

	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {
		if (showNodeOccupancy) {
			for (VirtualUser user : model.getObjectLocations().keySet()) {
				Point p = (Point) model.getObjectLocations().get(user);
				gc.setAlpha(SEMI_TRANSPARENT);
				// adapter.setBackgroundSysCol(SWT.COLOR_RED);
				fillCircle(gc, vp, p, 5);
				// gc.setAlpha(OPAQUE);
			}
		}
	}
	
	private void fillCircle(GC gc, ViewPort vp, Point p, double radius) {
		gc.fillOval(
				vp.toCoordX(p.x - radius), 
				vp.toCoordY(p.y - radius),
				vp.scale(radius * 2), 
				vp.scale(radius * 2));
	}
	

	@Override
	public ViewRect getViewRect() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * The dimensions of the head of the arrow relative to the vehicle length.
	 */
	private static final Point ARROW_HEAD_REL_DIM = new Point(.25, .25);

	private static final int OPAQUE = 255;
	private static final int SEMI_TRANSPARENT = 50;

	private final VirtualGraphRoadModel model;
	private final double margin;
	private final double vehicleLength;
	private final double minDistance;
	private final double halfRoadWidth;
	private final Graph<?> graph;
	private final boolean drawOneWayStreetArrows;
	private final boolean showNodeOccupancy;
	private final boolean showNodes;
	private final Point arrowDimensions;

	public VirtualWarehouseRenderer(Builder builder, VirtualGraphRoadModel m) {
		
		model = m;
		graph = model.getGraph();
		//margin = builder.margin + m.getVehicleLength() / 2d;
		margin = builder.margin + 4d;
		drawOneWayStreetArrows = builder.drawOneWayStreetArrows;
		showNodeOccupancy = builder.showNodeOccupancy;
		showNodes = builder.showNodes;
		vehicleLength = 1;
		minDistance = 0;
		final double roadWidth = 8d;
		halfRoadWidth = roadWidth / 2d;
		arrowDimensions = new Point(ARROW_HEAD_REL_DIM.x * roadWidth,
				ARROW_HEAD_REL_DIM.y * roadWidth);
	}

	/**
	 * @return A new {@link Builder} for creating a {@link WarehouseRenderer}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A builder for creating a {@link WarehouseRenderer}.
	 * 
	 * @author Rinde van Lon
	 */
	public static class Builder implements CanvasRendererBuilder {
		boolean drawOneWayStreetArrows;
		double margin;
		boolean showNodeOccupancy;
		boolean showNodes;

		Builder() {
			margin = 0;
			drawOneWayStreetArrows = false;
			showNodeOccupancy = false;
			showNodes = false;
		}

		/**
		 * Defines the margin around the warehouse. The margin is defined in the
		 * unit used by the {@link CollisionGraphRoadModel}. The default value
		 * is <code>0</code>.
		 * 
		 * @param m
		 *            Must be a positive value.
		 * @return This, as per the builder pattern.
		 */
		public Builder setMargin(double m) {
			checkArgument(m >= 0d);
			margin = m;
			return this;
		}

		/**
		 * One way streets will be indicated with an arrow indicating the
		 * allowed driving direction. By default this is not drawn.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder drawOneWayStreetArrows() {
			drawOneWayStreetArrows = true;
			return this;
		}

		/**
		 * Will draw an overlay on occupied nodes. By default this is not shown.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder showNodeOccupancy() {
			showNodeOccupancy = true;
			return this;
		}

		/**
		 * Will draw a small dot for each node. By default this is not shown.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder showNodes() {
			showNodes = true;
			return this;
		}

		@Override
		public CanvasRenderer build(ModelProvider mp) {
			return new VirtualWarehouseRenderer(this,
					mp.getModel(VirtualGraphRoadModel.class));
		}

		@Override
		public CanvasRendererBuilder copy() {
			final Builder b = new Builder();
			b.drawOneWayStreetArrows = drawOneWayStreetArrows;
			b.margin = margin;
			b.showNodeOccupancy = showNodeOccupancy;
			b.showNodes = showNodes;
			return b;
		}
	}

}
