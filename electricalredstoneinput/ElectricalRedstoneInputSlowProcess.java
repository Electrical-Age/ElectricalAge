package mods.eln.electricalredstoneinput;

import net.minecraft.world.EnumSkyBlock;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class ElectricalRedstoneInputSlowProcess implements IProcess {
	ElectricalRedstoneInputElement element;
	public ElectricalRedstoneInputSlowProcess(ElectricalRedstoneInputElement element) {
		this.element = element;
	}
	double sleepCounter = 0;
	static final double sleepDuration = 0.2;
	int oldSignal = 0;
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub

		if(sleepCounter == 0.0)
		{
			Coordonate coord = element.sixNode.coordonate;
			int signal = coord.world().getStrongestIndirectPower(coord.x, coord.y, coord.z);
		//	System.out.println("Light : " + light);
			element.outputGateProcess.setOutputNormalized((signal)/15.0 );
			if(signal != oldSignal)
				sleepCounter = sleepDuration;
			oldSignal = signal;		
		}
		else
		{
			sleepCounter -= time;
			if(sleepCounter < 0.0) sleepCounter = 0.0;
		}

	}

}
