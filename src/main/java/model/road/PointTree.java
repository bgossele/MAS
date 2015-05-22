package model.road;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.github.rinde.rinsim.geom.Point;

public class PointTree {

	private PointTree parent;

	private Point point;

	private Set<PointTree> children = new HashSet<PointTree>();

	private int depth;

	public PointTree(Point point) {
		this.point = point;
		this.depth = 0;
	}

	public PointTree(PointTree parrent, Point point) {
		this.parent = parrent;
		this.point = point;
		this.depth = this.parent.getDepth() + 1;
	}

	public PointTree getParent() {
		return parent;
	}
	
	public Point getPoint() {
		return point;
	}

	public Collection<PointTree> getChildren() {
		return children;
	}

	public void addChild(PointTree point) {
		children.add(point);
	}

	public int getDepth() {
		return depth;
	}
}