package mods.eln.sim.process.destruct;

import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.state.VoltageState;

public abstract class ValueWatchdog implements IProcess {

	IDestructable destructable;
	double perOverflowStrenght = 1;
	double min;
	double max;

	double timeoutReset = 2;

	double timeout = 0;
	boolean boot = true;
	boolean joker = true;
	
	double rand = Utils.rand(0.5, 1.5);
	
	@Override
	public void process(double time) {
		if(boot) {
			boot = false;
			timeout = timeoutReset;
		}
		double value = getValue();
		double overflow = Math.max(value - max, min - value);
		if(overflow > 0){
			if(joker){
				joker = false;
				overflow = 0;
			}
		}else{
			joker = true;
		}
		
		timeout -= time * overflow * rand;
		if(timeout > timeoutReset) {
			timeout = timeoutReset;
		}
		if(timeout < 0) {
			destructable.destructImpl();
		}

	}
	
	public ValueWatchdog set(IDestructable d){
		this.destructable = d;
		return this;
	}

	abstract double getValue();

}
