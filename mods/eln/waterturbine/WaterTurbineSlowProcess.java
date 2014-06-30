package mods.eln.waterturbine;

import mods.eln.INBTTReady;
import mods.eln.misc.RcRcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;

public class WaterTurbineSlowProcess implements IProcess,INBTTReady {

	WaterTurbineElement turbine;

	public WaterTurbineSlowProcess(WaterTurbineElement turbine) {
		this.turbine = turbine;
	}

	double refreshTimeout = 0;
	double refreshPeriode = 0.2;
	
	RcRcInterpolator filter = new RcRcInterpolator(2, 2);
	
	@Override
	public void process(double time) 
	{
		WaterTurbineDescriptor d = turbine.descriptor;

		refreshTimeout -=time;
		if(refreshTimeout < 0){
			refreshTimeout = refreshPeriode;
			double waterFactor = getWaterFactor();
			if(waterFactor<0){
				filter.setValue((float) (filter.get()*(1-0.5f*time)));
			}
			else{
				filter.setTarget((float) (waterFactor*d.nominalPower));
				filter.step((float) time);
			}
		
			turbine.powerSource.setP(filter.get());
		}
	}

	double getWaterFactor(){
		//Block b = turbine.waterCoord.getBlock();
		Block block = turbine.waterCoord.getBlock();
		int blockMeta = turbine.waterCoord.getMeta();
		//Utils.println("WATER : " + b + "    " + turbine.waterCoord.getMeta());
		if(block != Blocks.flowing_water && block != Blocks.water) return -1;
		if(blockMeta == 0) return 0;
		
		double time = Utils.getWorldTime(turbine.world());
		double timeFactor = 1+0.2*Math.sin((time-0.20)*Math.PI*2);
		double weatherFactor = 1+Utils.getWeather(turbine.world())*2;
		return timeFactor*weatherFactor;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		filter.readFromNBT(nbt, str + "filter");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		filter.writeToNBT(nbt, str + "filter");
	
	}
}
