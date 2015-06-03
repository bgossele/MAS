package model.road;

public enum Move {
	WAIT, NORTH, EAST, SOUTH, WEST;

	private Move opposite;

	static {
		WAIT.opposite = WAIT;
		NORTH.opposite = SOUTH;
		EAST.opposite = WEST;
		SOUTH.opposite = SOUTH;
		WEST.opposite = EAST;
	}

	public Move getOpposite() {
		return opposite;
	}
}
