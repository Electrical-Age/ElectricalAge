package mods.eln.electricalwindsensor;


import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;

public class ElectricalWindSensorSlowProcess implements IProcess ,INBTTReady{
	ElectricalWindSensorElement element;
	public ElectricalWindSensorSlowProcess(ElectricalWindSensorElement element) {
		this.element = element;
	}
	double timeCounter = 0;
	static final double refreshPeriode = 0.2;
	RcInterpolator rc = new RcInterpolator(3f);
	final float premonitionTime = 120;
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		timeCounter += time;
		if(timeCounter > refreshPeriode)
		{
			timeCounter -= refreshPeriode;
			Coordonate coord = element.sixNode.coordonate;


			element.outputGateProcess.setOutputNormalized(Utils.getWind(coord.world(), coord.y)/element.descriptor.windMax);
			
		}

	}
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
	
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {

	}

}
