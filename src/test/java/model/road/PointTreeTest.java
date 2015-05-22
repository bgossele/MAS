package model.road;

import org.junit.Before;
import org.junit.Test;

import com.github.rinde.rinsim.geom.Point;

public class PointTreeTest {

	private PointTree pointTree1;
	private PointTree pointTree2;
	private PointTree pointTree3;

	@Before
	public void setUp() {
		pointTree1 = new PointTree(new Point(1, 1));
		pointTree2 = new PointTree(pointTree1, new Point(2, 2));
		pointTree3 = new PointTree(pointTree2, new Point(3, 3));
		pointTree1.addChild(pointTree2);
		pointTree2.addChild(pointTree3);
	}
	
	@Test
	public void getParentTest() {
		assert (pointTree1.getParent() == null);
		assert (pointTree2.getParent().equals(pointTree1));
		assert (pointTree3.getParent().equals(pointTree2));
	}

	@Test
	public void getPointTest() {
		assert (pointTree1.getPoint().equals(new Point(1, 1)));
		assert (pointTree2.getPoint().equals(new Point(2, 2)));
	}

	@Test
	public void childTest() {
		assert (pointTree1.getChildren().contains(pointTree2));
		assert (pointTree2.getChildren().contains(pointTree3));
	}

	@Test
	public void depthTest() {
		assert (pointTree1.getDepth() == 0);
		assert (pointTree2.getDepth() == 1);
		assert (pointTree3.getDepth() == 3);
	}

}
