package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;
import com.github.rinde.rinsim.geom.Point;

public class ParcelOffer implements MessageContents {

	private final Point position;
	private final Point destination;

	public ParcelOffer(Point pos, Point des) {
		this.position = pos;
		this.destination = des;
	}

	public Point getPosition() {
		return this.position;
	}

	public Point getDestination() {
		return this.destination;
	}

	public String toString() {
		return "Parcel from " + position + " to " + destination;
	}
}