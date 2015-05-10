package users;
import model.road.VirtualRoadModel;

public interface VirtualUser {

	/**
	 * This is called when a road user can initialize itself.
	 * 
	 * @param model
	 *            The model on which this RoadUser is registered.
	 */
	void initVirtualUser(VirtualRoadModel model);

}
