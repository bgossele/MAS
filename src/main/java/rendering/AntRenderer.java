/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rendering;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import model.road.VirtualGraphRoadModel;
import model.road.VirtualRoadModelEvent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Transform;

import users.VirtualUser;

import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.geom.Connection;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.CanvasRenderer;
import com.github.rinde.rinsim.ui.renderers.CanvasRendererBuilder;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

/**
 * Renders vehicles as AGVs. Instances can be obtained via
 * {@link AGVRenderer#builder()}.
 * <p>
 * <b>Requires:</b> a {@link CollisionGraphRoadModel} in the
 * {@link com.github.rinde.rinsim.core.Simulator}.
 * 
 * @author Rinde van Lon
 */
public final class AntRenderer implements CanvasRenderer, Listener {
	private static final int DEFAULT_COLOR = SWT.COLOR_BLACK;
	private final VirtualGraphRoadModel model;
	private final RenderHelper helper;
	private final Map<VirtualUser, VehicleUI> vehicles;
	private int vehicleCounter;

	enum VizOptions {
		COORDINATES, CREATION_NUMBER, VEHICLE_ORIGIN, USE_DIFFERENT_COLORS;
	}

	private final ImmutableSet<VizOptions> vizOptions;

	private final Iterator<Integer> colors = Iterators.cycle(SWT.COLOR_BLUE,
			SWT.COLOR_RED, SWT.COLOR_GREEN, SWT.COLOR_CYAN, SWT.COLOR_MAGENTA,
			SWT.COLOR_YELLOW, SWT.COLOR_DARK_BLUE, SWT.COLOR_DARK_RED,
			SWT.COLOR_DARK_GREEN, SWT.COLOR_DARK_CYAN, SWT.COLOR_DARK_MAGENTA,
			SWT.COLOR_DARK_YELLOW);

	AntRenderer(VirtualGraphRoadModel m, Builder factory) {
		model = m;
		helper = new RenderHelper();
		vehicles = new LinkedHashMap<>();
		vehicleCounter = 0;
		vizOptions = Sets.immutableEnumSet(factory.vizOptions);

		final Set<VirtualUser> obs = model.getObjects();
		for (final VirtualUser ru : obs) {
			addVehicleUI((VirtualUser) ru);
		}

		model.getEventAPI().addListener(this, RoadEventType.ADD_ROAD_USER,
				RoadEventType.REMOVE_ROAD_USER);
	}

	void addVehicleUI(VirtualUser mru) {
		final int color = vizOptions.contains(VizOptions.USE_DIFFERENT_COLORS) ? colors
				.next() : DEFAULT_COLOR;
		final VehicleUI v = new VehicleUI(mru, model, color, vizOptions,
				vehicleCounter++);

		verify(vehicles.put(mru, v) == null);
	}

	@Override
	public void handleEvent(Event e) {
		verify(e instanceof VirtualRoadModelEvent);
		final VirtualRoadModelEvent rme = (VirtualRoadModelEvent) e;
		if (rme.roadUser instanceof VirtualUser) {
			if (e.getEventType() == RoadEventType.ADD_ROAD_USER) {
				addVehicleUI((VirtualUser) rme.roadUser);
			} else if (e.getEventType() == RoadEventType.REMOVE_ROAD_USER) {
				verifyNotNull(vehicles.remove(rme.roadUser)).dispose();
			} else {
				verify(false);
			}
		}
	}

	@Override
	public void renderStatic(GC gc, ViewPort vp) {
	}

	@Override
	public void renderDynamic(GC gc, ViewPort vp, long time) {
		helper.adapt(gc, vp);
		HashSet<VehicleUI> v_uis;
			v_uis = new HashSet<VehicleUI>(vehicles.values());

		for (final VehicleUI v : v_uis) {
			v.update(gc, vp, helper);
		}

	}

	@Override
	@Nullable
	public ViewRect getViewRect() {
		return null;
	}

	/**
	 * @return A {@link Builder} for creating an {@link AGVRenderer}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A builder for creating {@link AGVRenderer}s.
	 * 
	 * @author Rinde van Lon
	 */
	public static class Builder implements CanvasRendererBuilder {
		Set<VizOptions> vizOptions;

		Builder() {
			vizOptions = EnumSet.noneOf(VizOptions.class);
		}

		/**
		 * Draws a number on each vehicle. The number indicates the creation
		 * order of the vehicle.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder showVehicleCreationNumber() {
			vizOptions.add(VizOptions.CREATION_NUMBER);
			return this;
		}

		/**
		 * Displays the coordinates of each vehicle next to it.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder showVehicleCoordinates() {
			vizOptions.add(VizOptions.COORDINATES);
			return this;
		}

		/**
		 * Vehicles are drawn with different colors to ease debugging.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder useDifferentColorsForVehicles() {
			vizOptions.add(VizOptions.USE_DIFFERENT_COLORS);
			return this;
		}

		/**
		 * Vehicles are drawn with a small half circle on top, the center of
		 * this half circle indicates the vehicle origin. The origin is the
		 * actual position as returned by
		 * {@link CollisionGraphRoadModel#getPosition(RoadUser)}.
		 * 
		 * @return This, as per the builder pattern.
		 */
		public Builder showVehicleOrigin() {
			vizOptions.add(VizOptions.VEHICLE_ORIGIN);
			return this;
		}

		@Override
		public CanvasRenderer build(ModelProvider mp) {
			return new AntRenderer(mp.getModel(VirtualGraphRoadModel.class),
					this);
		}

		@Override
		public CanvasRendererBuilder copy() {
			final Builder copy = new Builder();
			copy.vizOptions.addAll(vizOptions);
			return copy;
		}
	}

	static class VehicleUI {
		final VirtualGraphRoadModel model;
		final VirtualUser vehicle;
		Point position;
		double angle;
		final int color;
		final Set<VizOptions> vizOptions;
		final int creationNumber;
		double scale = 1;
		Optional<Image> image;

		VehicleUI(VirtualUser mru, VirtualGraphRoadModel m, int c,
				Set<VizOptions> t, int num) {
			vehicle = mru;
			model = m;
			angle = 0;
			color = c;
			vizOptions = t;
			creationNumber = num;
			position = new Point(0, 0);
			image = Optional.absent();
		}

		void dispose() {
			if (image.isPresent()) {
				image.get().dispose();
			}
		}

		Image createImage(GC gc, ViewPort vp) {
			if (image.isPresent()) {
				image.get().dispose();
			}
			final int length = (int) (1 * vp.scale);
			final int width = length / 2;
			final int frontSize = length / 8;
			final Image img = new Image(gc.getDevice(), width, length);
			final GC igc = new GC(img);

			igc.setBackground(gc.getDevice().getSystemColor(color));
			igc.fillPolygon(new int[] { frontSize, 0, width - frontSize, 0,
					width, frontSize, 0, frontSize });
			igc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
			igc.fillRectangle(0, frontSize, width, length - frontSize);

			if (vizOptions.contains(VizOptions.CREATION_NUMBER)) {
				final String string = Integer.toString(creationNumber);
				final double factor = width
						/ (double) igc.stringExtent(string).x;
				final Font initialFont = igc.getFont();
				final FontData[] fontData = initialFont.getFontData();
				for (int i = 0; i < fontData.length; i++) {
					fontData[i]
							.setHeight((int) (fontData[i].getHeight() * factor));
				}
				final Font newFont = new Font(gc.getDevice(), fontData);
				igc.setFont(newFont);

				final org.eclipse.swt.graphics.Point finalTextSize = igc
						.stringExtent(string);

				final int xOffset = (int) ((width - finalTextSize.x) / 2d);
				final int yOffset = frontSize
						+ (int) ((length - frontSize - finalTextSize.y) / 2d);
				igc.drawText(string, xOffset, yOffset, true);
				newFont.dispose();
			}

			if (vizOptions.contains(VizOptions.VEHICLE_ORIGIN)) {
				igc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_RED));
				igc.setForeground(gc.getDevice()
						.getSystemColor(SWT.COLOR_WHITE));
				igc.fillOval(width / 2 - 2, length / 2 - 2, 4, 4);
				igc.drawOval(width / 2 - 2, length / 2 - 2, 4, 4);
			}

			igc.dispose();
			return img;
		}

		void update(GC gc, ViewPort vp, RenderHelper helper) {
			position = model.getPosition(vehicle);
			final Optional<? extends Connection<?>> conn = model
					.getConnection(vehicle);

			if (!image.isPresent() || scale != vp.scale) {
				scale = vp.scale;
				image = Optional.of(createImage(gc, vp));
			}

			if (conn.isPresent()) {
				angle = angle(conn.get());
			}

			final int x = vp.toCoordX(position.x);
			final int y = vp.toCoordY(position.y);

			final Transform transform = new Transform(gc.getDevice());
			transform.translate(x, y);
			transform.rotate((float) (90 + angle * 180 / Math.PI));
			transform.translate(-(x + image.get().getBounds().width / 2),
					-(y + image.get().getBounds().height / 2));
			gc.setTransform(transform);
			gc.drawImage(image.get(), x, y);
			gc.setTransform(null);
			transform.dispose();

			if (vizOptions.contains(VizOptions.COORDINATES)) {
				helper.setBackgroundSysCol(SWT.COLOR_YELLOW);
				helper.setForegroundSysCol(SWT.COLOR_BLACK);
				gc.drawString(
						String.format("%1.2f,%1.2f", position.x, position.y),
						vp.toCoordX(position.x), vp.toCoordY(position.y));
			}
		}
	}

	static double angle(Point p1, Point p2) {
		final double dx = p2.x - p1.x;
		final double dy = p2.y - p1.y;
		return Math.PI + Math.atan2(-dy, -dx);
	}

	static double angle(Connection<?> connection) {
		return angle(connection.from(), connection.to());
	}
}
