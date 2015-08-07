package mods.eln.sixnode.electricalwindsensor;

import mods.eln.misc.Coordinate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalWindSensorSlowProcess implements IProcess, INBTTReady {

	ElectricalWindSensorElement element;

    double timeCounter = 0;
    static final double refreshPeriode = 0.2;
    RcInterpolator rc = new RcInterpolator(3f);
    final float premonitionTime = 120;

    public ElectricalWindSensorSlowProcess(ElectricalWindSensorElement element) {
		this.element = element;
	}

	@Override
	public void process(double time) {
		timeCounter += time;
		if (timeCounter > refreshPeriode) {
			timeCounter -= refreshPeriode;
			Coordinate coord = element.sixNode.coordinate;

			element.outputGateProcess.setOutputNormalized(Utils.getWind(coord.dimention, coord.y) / element.descriptor.windMax);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
	}
}
