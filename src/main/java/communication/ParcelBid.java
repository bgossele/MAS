package communication;

import com.github.rinde.rinsim.core.model.comm.MessageContents;

public class ParcelBid implements MessageContents {

	private final int cost;

	public ParcelBid(int cost) {
		this.cost = cost;
	}

	public int getCost() {
		return this.cost;
	}

	public String toString() {
		return "ParcelBid; cost = " + cost;
	}
}
