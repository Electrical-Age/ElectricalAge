package mods.eln.windturbine;

import mods.eln.INBTTReady;
import mods.eln.item.DynamoDescriptor;
import mods.eln.item.WindRotorDescriptor;
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
	static final double environementTimeCounterReset = 2.0;	
	
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
		return Math.abs(localWind + Utils.getWind(turbine.node.coordonate.world())) * environementWindFactor;
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

		ItemStack rotorStack = turbine.getInventory().getStackInSlot(WindTurbineContainer.windRotorSlotId);
		ItemStack dynamoStack = turbine.getInventory().getStackInSlot(WindTurbineContainer.dynamoSlotId);
		
		if(turbine.inventoryChangeFlag)
		{
			turbine.inventoryChangeFlag = false;
			turbine.setPhysicalValue(false);
			
			environementTimeCounter = 0.0;
			rotorStack = turbine.getInventory().getStackInSlot(WindTurbineContainer.windRotorSlotId);
			dynamoStack = turbine.getInventory().getStackInSlot(WindTurbineContainer.dynamoSlotId);

		}
		
		
		environementTimeCounter -= time;
		if(environementTimeCounter < 0.0)
		{
			environementTimeCounter += environementTimeCounterReset;

			if(rotorStack == null)
			{
				environementWindFactor = 0.0;
			}
			else
			{
				WindRotorDescriptor rotor = (WindRotorDescriptor) WindRotorDescriptor.getDescriptor(rotorStack);

				int x1 = 0,x2 = 0,y1 = 0,y2 = 0,z1 = 0,z2 = 0;
				y1 = rotor.environnementalHeightStart;
				y2 = rotor.environnementalHeightEnd;
				
				switch (rotor.axe) {

				case horizontal:
					switch (turbine.front) {
					case XN:
						z1 = rotor.environnementalWidthStart;
						z2 = rotor.environnementalWidthEnd;
						x1 = rotor.environnementalDepthStart;
						x2 = rotor.environnementalDepthEnd;
						break;
					case XP:
						z2 = -rotor.environnementalWidthStart;
						z1 = -rotor.environnementalWidthEnd;
						x2 = -rotor.environnementalDepthStart;
						x1 = -rotor.environnementalDepthEnd;
						break;
					case ZN:
						x2 = -rotor.environnementalWidthStart;
						x1 = -rotor.environnementalWidthEnd;
						z1 = rotor.environnementalDepthStart;
						z2 = rotor.environnementalDepthEnd;
						break;
					case ZP:
						x1 = rotor.environnementalWidthStart;
						x2 = rotor.environnementalWidthEnd;
						z2 = -rotor.environnementalDepthStart;
						z1 = -rotor.environnementalDepthEnd;
						break;
					default:
					case YN:
					case YP:
						break;

				
					}
					break;
				case vertical:
					z2 = -rotor.environnementalWidthStart;
					z1 = -rotor.environnementalWidthEnd;
					x2 = -rotor.environnementalDepthStart;
					x1 = -rotor.environnementalDepthEnd;
				
					break;
				default:
					break;

				}
				
				Coordonate coord = turbine.node.coordonate;
				
				x1 += coord.x;
				x2 += coord.x;
				y1 += coord.y;
				y2 += coord.y;
				z1 += coord.z;
				z2 += coord.z;
				
				int blockFreeCount = 0;
				World world = turbine.node.coordonate.world();
				IChunkProvider chunk = world.getChunkProvider();
				boolean notInCache = false;
				for(int x = x1;x<=x2;x++)
				{
					for(int y = y1;y<=y2;y++)
					{
						for(int z = z1;z<=z2;z++)
						{
							if(chunk.chunkExists(x>>4, z>>4) == false) 
							{
								notInCache = true;
								break;
							}
							if(world.getBlockId(x, y, z) != 0) continue;
								
							blockFreeCount++;
						}		
						if(notInCache) break;
					}	
					if(notInCache) break;
				}
				if(! notInCache)
				{
					double ratio = blockFreeCount / ((double)((x2-x1+1)*(y2-y1+1)*(z2-z1+1) - rotor.environementalOffsetBlock));
					if(ratio > 1.0) ratio = 1.0;
					environementWindFactor = rotor.environnementalFunction.getValue(ratio);
			//		System.out.println("Ratio : " + ratio + "   environementWindFactor : " + environementWindFactor);
				}
				
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

		
		

		
		double P = 0;
		if(rotorStack != null)
		{
			WindRotorDescriptor rotor = (WindRotorDescriptor) WindRotorDescriptor.getDescriptor(rotorStack);
			double wind = getWind();
			if(wind > rotor.maximalWind)
			{
				if(Math.random() <  (wind - rotor.maximalWind) * 0.02);
			}
			else
			{
				P = rotor.PfW.getValue(wind);
			}
		}
		double Umax = 0;
		if(dynamoStack != null)
		{
			DynamoDescriptor rotor = (DynamoDescriptor) WindRotorDescriptor.getDescriptor(dynamoStack);
			P = rotor.PoutfPin.getValue(P);
			Umax = rotor.UfPout.getValue(P);
		}
		else
		{
			P = 0;
		}
		
		
		turbine.powerSource.setP(P);	
		turbine.powerSource.setUmax(Umax);
		
		
		
		
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
