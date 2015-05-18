package model.road;

public class Pheromone {

	private int timeStamp;

	private Move origin;

	private Move goal;

	private int robot;

	private int lifeTime;

	Pheromone() {
	}

	public int getTimeStamp() {
		return timeStamp;
	}

	void setTimeStamp(int timeStamp) {
		this.timeStamp = timeStamp;
	}

	public Move getOrigin() {
		return origin;
	}

	void setOrigin(Move origin) {
		this.origin = origin;
	}

	public Move getGoal() {
		return goal;
	}

	void setGoal(Move goal) {
		this.goal = goal;
	}

	public int getRobot() {
		return robot;
	}

	void setRobot(int robot) {
		this.robot = robot;
	}

	public int live() {
		return lifeTime++;
	}

	void resetLife() {
		lifeTime = 0;
	}
}
