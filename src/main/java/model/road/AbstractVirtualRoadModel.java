package model.road;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verifyNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import users.VirtualUser;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public abstract class AbstractVirtualRoadModel<T> extends
		GenericVirtualRoadModel {

	/**
	 * A mapping of {@link VirtualUser} to location.
	 */
	protected volatile Map<VirtualUser, T> objLocs;

	protected AbstractVirtualRoadModel() {
		super();
		objLocs = Collections
				.synchronizedMap(new LinkedHashMap<VirtualUser, T>());
	}

	/**
	 * A function for converting the location representation to a {@link Point}.
	 * 
	 * @param locObj
	 *            The location to be converted.
	 * @return A {@link Point} indicating the position as represented by the
	 *         specified location.
	 */
	protected abstract Point locObj2point(T locObj);

	/**
	 * A function for converting a {@link Point} to the location representation
	 * of this model.
	 * 
	 * @param point
	 *            The {@link Point} to be converted.
	 * @return The location.
	 */
	protected abstract T point2LocObj(Point point);

	public void addObjectAt(VirtualUser newObj, Point pos) {
		checkArgument(!objLocs.containsKey(newObj),
				"Object is already added: %s.", newObj);
		objLocs.put(newObj, point2LocObj(pos));
	}

	public void addObjectAtSamePosition(VirtualUser newObj,
			VirtualUser existingObj) {
		checkArgument(!objLocs.containsKey(newObj),
				"Object %s is already added.", newObj);
		checkArgument(objLocs.containsKey(existingObj),
				"Object %s does not exist.", existingObj);
		objLocs.put(newObj, objLocs.get(existingObj));
	}

	public void removeObject(VirtualUser virtualUser) {
		checkArgument(objLocs.containsKey(virtualUser),
				"RoadUser: %s does not exist.", virtualUser);
		objLocs.remove(virtualUser);
	}

	public void clear() {
		objLocs.clear();
	}
	
	public boolean containsObject(VirtualUser obj) {
	    return objLocs.containsKey(obj);
	  }

	  public boolean containsObjectAt(VirtualUser obj, Point p) {
	    if (containsObject(obj)) {
	      return objLocs.get(obj).equals(p);
	    }
	    return false;
	  }

	  public boolean equalPosition(VirtualUser obj1, VirtualUser obj2) {
	    return containsObject(obj1) && containsObject(obj2)
	        && getPosition(obj1).equals(getPosition(obj2));
	  }

	  public Map<VirtualUser, Point> getObjectsAndPositions() {
	    Map<VirtualUser, T> copiedMap;
	    synchronized (objLocs) {
	      copiedMap = new LinkedHashMap<>();
	      copiedMap.putAll(objLocs);
	      // it is save to release the lock now
	    }

	    final Map<VirtualUser, Point> theMap = new LinkedHashMap<>();
	    for (final java.util.Map.Entry<VirtualUser, T> entry : copiedMap.entrySet()) {
	      theMap.put(entry.getKey(), locObj2point(entry.getValue()));
	    }
	    return theMap;
	  }

	  public Point getPosition(VirtualUser virtualUser) {
	    checkArgument(containsObject(virtualUser), "RoadUser does not exist: %s.",
	    		virtualUser);
	    return locObj2point(objLocs.get(virtualUser));
	  }

	  public Collection<Point> getObjectPositions() {
	    return getObjectsAndPositions().values();
	  }

	  public Set<VirtualUser> getObjects() {
	    synchronized (objLocs) {
	      final Set<VirtualUser> copy = new LinkedHashSet<>();
	      copy.addAll(objLocs.keySet());
	      return copy;
	    }
	  }

	  public Set<VirtualUser> getObjects(Predicate<VirtualUser> predicate) {
	    return Sets.filter(getObjects(), predicate);
	  }

	  @SuppressWarnings("unchecked")
	  public <Y extends VirtualUser> Set<Y> getObjectsAt(VirtualUser virtualUser,
	      Class<Y> type) {
	    final Set<Y> result = new HashSet<>();
	    //TODO SameLocation predicate afmaken zie private class onderaan deze klasse.
//	    for (final VirtualUser ru : getObjects(new SameLocationPredicate(virtualUser,
//	        type, self))) {
//	      result.add((Y) ru);
//	    }
	    return result;
	  }

	  @SuppressWarnings("unchecked")
	  public <Y extends VirtualUser> Set<Y> getObjectsOfType(final Class<Y> type) {
	    return (Set<Y>) getObjects(new Predicate<VirtualUser>() {
	      @Override
	      public boolean apply(@Nullable VirtualUser input) {
	        return type.isInstance(input);
	      }
	    });
	  }

	  @Override
	  public boolean doRegister(VirtualUser virtualUser) {
	    LOGGER.info("register {}", virtualUser);
	    //TODO VirtualUser methode moet VirtualRoadModel aanvaarden (zie VirtualRoadModel comment onderaan))
	    //virtualUser.initRoadUser(self);
	    return true;
	  }

	  @Override
	  public boolean unregister(VirtualUser virtualUser) {
	    final boolean contains = containsObject(virtualUser);
	    LOGGER.info("unregister {} succes: {}", virtualUser, contains);
	    if (contains) {
	      removeObject(virtualUser);
	      return true;
	    }
	    return false;
	  }

/*	 
 * TODO eerst VirtualRoadModel interface maken vergelijkbaar met RoadModel
 * private static class SameLocationPredicate implements Predicate<VirtualUser> {
		    private final VirtualUser reference;
		    private final AbstractVirtualRoadModel<T> model;
		    private final Class<?> type;

		    SameLocationPredicate(final VirtualUser pReference, final Class<?> pType,
		        final RoadModel pModel) {
		      reference = pReference;
		      type = pType;
		      model = pModel;
		    }

		    @Override
		    public boolean apply(@Nullable VirtualUser input) {
		      return type.isInstance(input)
		          && model.equalPosition(verifyNotNull(input), reference);
		    }
		  }*/
}
