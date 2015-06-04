package model.road;

public enum Move {
	WAIT, NORTH, EAST, SOUTH, WEST, SLEEP;

	private Move opposite;

	static {
		WAIT.opposite = WAIT;
		NORTH.opposite = SOUTH;
		EAST.opposite = WEST;
		SOUTH.opposite = NORTH;
		WEST.opposite = EAST;
		SLEEP.opposite = SLEEP;
	}

	public Move getOpposite() {
		return opposite;
	}
}
