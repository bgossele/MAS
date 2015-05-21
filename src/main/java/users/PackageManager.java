package users;

import model.road.VirtualGraphRoadModel;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.SimulatorAPI;
import com.github.rinde.rinsim.core.TickListener;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;

public class PackageManager implements TickListener {
	
	private VirtualGraphRoadModel model;
	private final RandomGenerator rng;
	private SimulatorAPI sim;
	
	public PackageManager(VirtualGraphRoadModel m, RandomGenerator rng, Simulator sim){
		this.model = m;
		this.rng = rng;
		this.sim = sim;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		while(rng.nextBoolean()){			
			Point start = model.getRandomPosition(rng);
			Point destination;
			do {
				destination = model.getRandomPosition(rng);
			} while (destination.equals(start));
			Package p = new Package(start, destination);
			sim.register(p);
			System.out.println("Spawned " + p);
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}
	
}
