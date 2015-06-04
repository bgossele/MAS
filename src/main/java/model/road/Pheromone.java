package model.road;

public abstract class Pheromone {

	private int robot;

	private int lifeTime;

	public int getRobot() {
		return robot;
	}

	void setRobot(int robot) {
		this.robot = robot;
	}

	public int getLifeTime() {
		return lifeTime;
	}

	public void addTickToLife() {
		lifeTime++;
	}

	void resetLife() {
		lifeTime = 0;
	}

}
