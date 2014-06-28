package mods.eln.electricalweathersensor;

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

public class ElectricalWeatherSensorSlowProcess implements IProcess, INBTTReady {
	
	ElectricalWeatherSensorElement element;
	
	public ElectricalWeatherSensorSlowProcess(ElectricalWeatherSensorElement element) {
		this.element = element;
	}
	
	double timeCounter = 0;
	static final double refreshPeriode = 0.2;
	RcInterpolator rc = new RcInterpolator(3f);
	final float premonitionTime = 120;
	
	@Override
	public void process(double time) {
		timeCounter += time;
		if(timeCounter > refreshPeriode) {
			timeCounter -= refreshPeriode;
			Coordonate coord = element.sixNode.coordonate;

			World world = coord.world();
	    	float target = 0f;
	    	if(world.isRaining()) {
	    		//float f = Math.max(0f, (float)((premonitionTime - rain * time) / premonitionTime));
	    		target = 0.5f;
	    	}
	    	if(world.isThundering()) {
	    		target = 1.0f;
	    	}
	    	
	    /*	int rain = world.getWorldInfo().getRainTime();
	    	int thunder = world.getWorldInfo().getThunderTime();
	    	
	    	if(rain < thunder) {
	    		target = target * (1 - f) + f * 0.5f;
	    	} else {
	    		target = target * (1 - f) + f * 1f;
	    	}
	    	*/
	    	//Utils.println(target);
			rc.setTarget(target);
			rc.step((float) time);
			element.outputGateProcess.setOutputNormalized(rc.get());
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		rc.setValue(nbt.getFloat(str + "rc"));
		
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setFloat(str + "rc", rc.get());
	}
}
