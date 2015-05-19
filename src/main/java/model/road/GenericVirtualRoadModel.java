package model.road;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;

import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import users.VirtualUser;

import com.github.rinde.rinsim.core.model.AbstractModel;
import com.github.rinde.rinsim.core.model.road.GenericRoadModel.RoadEventType;
import com.github.rinde.rinsim.event.EventDispatcher;

public abstract class GenericVirtualRoadModel extends AbstractModel<VirtualUser> 
	implements VirtualRoadModel {

  /**
   * The logger of the model.
   */
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(GenericVirtualRoadModel.class);

  /**
   * Reference to the outermost decorator of this road model, or to
   * <code>this</code> if there are no decorators.
   */
  protected GenericVirtualRoadModel self = this;
  private boolean initialized = false;
  protected final EventDispatcher eventDispatcher;

  protected GenericVirtualRoadModel() {
	  
	    final Set<Enum<?>> events = new LinkedHashSet<>();
	    events.addAll(asList(RoadEventType.values()));
	    eventDispatcher = new EventDispatcher(events);
  }

  /**
   * Method which should only be called by a decorator of this instance.
   * @param rm The decorator to set as 'self'.
   */
  protected void setSelf(GenericVirtualRoadModel rm) {
    LOGGER.info("setSelf {}", rm);
    checkState(
        !initialized,
        "This road model is already initialized, it can only be decorated before objects are registered.");
    self = rm;
  }

  @Override
  public final boolean register(VirtualUser object) {
    initialized = true;
    return doRegister(object);
  }

  /**
   * Actual implementation of {@link #register(RoadUser)}.
   * @param object The object to register.
   * @return <code>true</code> when registration succeeded, <code>false</code>
   *         otherwise.
   */
  protected abstract boolean doRegister(VirtualUser object);

}

