package mods.eln.waterturbine;

import mods.eln.INBTTReady;
import mods.eln.item.DynamoDescriptor;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public class WaterTurbineSlowProcess implements IProcess {

	WaterTurbineElement turbine;

	public WaterTurbineSlowProcess(WaterTurbineElement turbine) {
		this.turbine = turbine;
	}

	double refreshTimeout = 0;
	double refreshPeriode = 0.2;
	@Override
	public void process(double time) 
	{
		WaterTurbineDescriptor d = turbine.descriptor;

		refreshTimeout -=time;
		if(refreshTimeout < 0){
			refreshTimeout = refreshPeriode;
		}
	}

	double getWaterFactor(){
		
		return 1.0;
	}
}
