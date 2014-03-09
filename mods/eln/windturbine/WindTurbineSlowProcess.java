package mods.eln.windturbine;

import mods.eln.INBTTReady;
import mods.eln.item.DynamoDescriptor;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public class WindTurbineSlowProcess implements IProcess,INBTTReady {

	WindTurbineElement turbine;
	
	double environementWindFactor = 0.0;
	double environementTimeCounter = 0;
	static final double environementTimeCounterReset = 10.0;	
	
	double localWind = 0;
	double localWindDerive = 0;
	double localWindTimeCounter = 0;
	static final double localWindTimeCounterReset = 1.0;
	static final double localWindMax = 3.0;
	static final double localWinDeriveMax = 0.1;
	static final double localWinDeriveLostFactor = 0.3;
	static final double localWinDeriveDeriveMax = 0.1;
	
	String name;
	
	double getWind()
	{
		return Math.abs(localWind + Utils.getWind(turbine.node.coordonate.world(),turbine.node.coordonate.y + turbine.descriptor.offY)) * environementWindFactor;
	}
	void setWind(double wind)
	{
		this.localWind = wind;
	}

	public WindTurbineSlowProcess(String name,WindTurbineElement turbine) {
		this.turbine = turbine;
		this.name = name;
	}
	
	int counter = 0;
	@Override
	public void process(double time) 
	{
		WindTurbineDescriptor d = turbine.descriptor;
		environementTimeCounter -= time;
		if(environementTimeCounter < 0.0)
		{
			environementTimeCounter += environementTimeCounterReset*(0.75 + Math.random()*0.5);
			
			int x1 = 0,x2 = 0,y1 = 0,y2 = 0,z1 = 0,z2 = 0;

			
			Coordonate coord = new Coordonate(turbine.node.coordonate);
			
			x1 = coord.x - d.rayX;
			x2 = coord.x + d.rayX;
			y1 = coord.y - d.rayY + d.offY;
			y2 = coord.y + d.rayY + d.offY;
			z1 = coord.z - d.rayZ;
			z2 = coord.z + d.rayZ;
			
			int blockBusyCount = -d.blockMalusSubCount;
			World world = turbine.node.coordonate.world();
			IChunkProvider chunk = world.getChunkProvider();
			boolean notInCache = false;
			for(int x = x1;x<=x2;x++)
			{
				for(int y = y1;y<=y2;y++)
				{
					for(int z = z1;z<=z2;z++)
					{
						if(world.blockExists(x, y, z) == false) 
						{
							notInCache = true;
							break;
						}
						if(world.getBlockId(x, y, z) != 0){
							blockBusyCount++;
						}
					}		
					if(notInCache) break;
				}	
				if(notInCache) break;
			}
			if(! notInCache)
			{
				environementWindFactor = Math.max(0.0,Math.min(1.0,1.0 - blockBusyCount*d.blockMalus));
				
				System.out.println("EnvironementWindFactor : " + environementWindFactor);
			}
		
		}
		
		
		localWindTimeCounter -= time;
		if(localWindTimeCounter < 0)
		{
			localWindTimeCounter += localWindTimeCounterReset;
			
			localWindDerive *= 1 - (localWinDeriveLostFactor * localWindTimeCounterReset);
			localWindDerive += (Math.random()*2.0 - 1.0) * localWinDeriveDeriveMax * localWindTimeCounterReset;					
		}
		
		localWind += localWindDerive*time;
		
		if(localWind > localWindMax)
		{
			localWind = localWindMax;
			localWindDerive = 0.0;
		}
		if(localWind < -localWindMax)
		{
			localWind = -localWindMax;
			localWindDerive = 0.0;
		}

		
		

		localWind = 0;
		double P = 0;
		double wind = getWind();
		
		if(wind > d.maxWind)
		{
			if(Math.random() <  (wind - d.maxWind) * 0.02){
			//	turbine.selfDestroy();
			}
		}

		P = d.PfW.getValue(wind);
		
		
		
		turbine.powerSource.setP(P);	
		turbine.powerSource.setUmax(d.maxVoltage);
		
		
		
		
		counter++;
		if(counter%20 == 0)
		{
			System.out.println("Wind : " + getWind() + "  Derivate : " + localWindDerive + " EPmax : " + P);
		}
		
	}


	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		localWind = nbt.getDouble(str + name + "localWind");
		environementWindFactor = nbt.getDouble(str + name + "environementWindFactor");
				
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + name + "localWind", localWind);
		nbt.setDouble(str + name + "environementWindFactor", environementWindFactor);
		
	}
}
