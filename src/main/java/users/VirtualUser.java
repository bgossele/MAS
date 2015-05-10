package users;
import model.road.VirtualGraphRoadModel;

public interface VirtualUser {

	/**
	 * This is called when an road user can initialize itself.
	 * 
	 * @param model
	 *            The model on which this RoadUser is registered.
	 */
	void initRoadUser(VirtualGraphRoadModel model);

}
