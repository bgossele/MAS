package users;

import model.road.VirtualGraphRoadModel;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

public class ParcelManager implements TickListener {

	private VirtualGraphRoadModel model;
	private final RandomGenerator rng;
	private SimulatorAPI sim;
	private int parcelCounter = 1;

	public ParcelManager(VirtualGraphRoadModel m, RandomGenerator rng,
			Simulator sim) {
		this.model = m;
		this.rng = rng;
		this.sim = sim;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		if (parcelCounter <= 25) {
			while ((rng.nextInt() % 67) == 0) {
				Point start = model.getRandomPosition(rng);
				Point destination;
				do {
					destination = model.getRandomPosition(rng);
				} while (destination.equals(start));
				Parcel p = new Parcel(parcelCounter, start, destination);
				parcelCounter++;
				sim.register(p);
//				System.out.println("Spawned " + p);
			}
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub

	}

}
