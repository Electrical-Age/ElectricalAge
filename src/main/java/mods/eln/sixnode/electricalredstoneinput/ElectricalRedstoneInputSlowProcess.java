package mods.eln.sixnode.electricalredstoneinput;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public class ElectricalRedstoneInputSlowProcess implements IProcess {

	ElectricalRedstoneInputElement element;

    double sleepCounter = 0;
    static final double sleepDuration = 0.15;
    public int oldSignal = 0;
	
	public ElectricalRedstoneInputSlowProcess(ElectricalRedstoneInputElement element) {
		this.element = element;
	}

	@Override
	public void process(double time) {
		if (sleepCounter == 0.0) {
			Coordonate coord = element.sixNode.coordonate;

			// TODO: Ignore the value of a signal to redstone converter if it is connected to this converted directly.
			int signal = Utils.getRedstoneLevelAround(coord);
			//Utils.println("Light : " + light);
			element.outputGateProcess.setOutputNormalized((signal) / 15.0 );
			if (signal != oldSignal) {
				element.needPublish();
			}
				
			sleepCounter = sleepDuration + Math.random() * sleepDuration;
			oldSignal = signal;		
		} else {
			sleepCounter -= time;
			if (sleepCounter < 0.0) sleepCounter = 0.0;
		}
	}
}
