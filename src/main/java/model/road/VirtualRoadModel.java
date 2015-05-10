package model.road;

import users.VirtualUser;

import com.github.rinde.rinsim.core.model.Model;
import com.github.rinde.rinsim.core.model.road.RoadUser;

public interface VirtualRoadModel extends Model<VirtualUser>{
	
	/**
	   * Checks if the positions of the <code>obj1</code> and <code>obj2</code> are
	   * equal.
	   * @param obj1 A {@link RoadUser}.
	   * @param obj2 A {@link RoadUser}.
	   * @return <code>true</code> if the positions are equal, <code>false</code>
	   *         otherwise.
	   */
	  boolean equalPosition(VirtualUser obj1, VirtualUser obj2);
}
