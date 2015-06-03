package model.road;

public class PathPheromone extends Pheromone{

	private int timeStamp;

	private Move origin;

	private Move goal;

	PathPheromone() {
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
	
	@Override
	public String toString(){
		String s = "Pheromone <" +  timeStamp + " ; " +  origin + " ; " + goal + ">";
		return s;
	}
}
