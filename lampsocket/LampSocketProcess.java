package mods.eln.lampsocket;

import mods.eln.INBTTReady;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.misc.Utils;
import mods.eln.node.NodeServer;
import mods.eln.sim.IProcess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class LampSocketProcess implements IProcess , INBTTReady{
	double time = 0;
	double deltaTBase = 0.2;
	double deltaT = deltaTBase;
	public double invulerabilityTimeLeft = 2;
	boolean overPoweredInvulerabilityArmed = true;
	LampSocketElement lamp;
	int light = 0; //0..15
	
	double stableProb = 0;
	
	public LampSocketProcess(LampSocketElement lamp) {
		this.lamp = lamp;
	}
	
	ItemStack lampStackLast = null;
	boolean boot = true;
	
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		this.time += time;
		if(this.time < deltaT) return;
		
		this.time -= deltaT;
		
		lamp.computeElectricalLoad();
		int oldLight = light;
		light = 0;
		
		ItemStack lampStack = lamp.inventory.getStackInSlot(0);
		
		if(boot == false && (lampStack != lampStackLast || lampStack == null))
		{
			stableProb = 0;
			
		}
		
		if(lampStack != null)
		{
			LampDescriptor lampDescriptor = (LampDescriptor) ((GenericItemUsingDamage<GenericItemUsingDamageDescriptor>)lampStack.getItem()).getDescriptor(lampStack);
			
			if(stableProb < 0) stableProb = 0;
			
			double lightDouble = 0;
			switch(lampDescriptor.type)
			{
				case Incandescent:
				{
					lightDouble = 	  lampDescriptor.nominalLight 
					*(Math.abs(lamp.lampResistor.getU())-lampDescriptor.minimalU)
						/(lampDescriptor.nominalU-lampDescriptor.minimalU);
					lightDouble =  (lightDouble*16);
				}
				break;
				case eco:
					double U = Math.abs(lamp.lampResistor.getU());
					if(U < lampDescriptor.minimalU)
					{
						stableProb = 0;
						lightDouble = 0;
					}
					else
					{
						double powerFactor = U / lampDescriptor.nominalU;
						stableProb += U / lampDescriptor.stableU * deltaT / lampDescriptor.stableTime * lampDescriptor.stableUNormalised;
						
						if(stableProb > U / lampDescriptor.stableU) stableProb = U / lampDescriptor.stableU;
						if(Math.random() > stableProb)
						{
							lightDouble = 0;
						}
						else
						{
							lightDouble = lampDescriptor.nominalLight * powerFactor;
							lightDouble =  (lightDouble*16);
						}
					}
				break;
				
				default:
				break;
				
			}
			

		//	lamp.sixNode.setLightValue(lightInt);
			
		//	System.out.println("Light : " + lightInt);
			
			if(lightDouble - oldLight > 1.3){
				light =  (int) lightDouble;
			}
			else if(lightDouble - oldLight < -0.3){
				light =  (int) lightDouble;
			}
			else
			{
				//if((int)lightDouble != oldLight)
			//		System.out.println("light filtred : D");
				light = oldLight;
			}
			//light = (int) lightDouble;
			if(light < 0) light = 0;
			if(light > 15) light = 15;
						
			
			
			
			/*double overFactor =  (lamp.electricalLoad.Uc-lampDescriptor.minimalU)
								/(lampDescriptor.nominalU-lampDescriptor.minimalU);*/
			double overFactor =  (lamp.lampResistor.getP())/(lampDescriptor.nominalP);
			if(overFactor < 0) overFactor = 0;
			
			if(overFactor < 1.3) overPoweredInvulerabilityArmed = true;
			
			if(overFactor > 1.5 && overPoweredInvulerabilityArmed) 
			{
				invulerabilityTimeLeft = 2;
				overPoweredInvulerabilityArmed = false;
			}
			
			if(invulerabilityTimeLeft != 0 && overFactor > 1.5 ) overFactor = 1.5;			
		
			double lifeLost = deltaT / lampDescriptor.nominalLife;
			
			lifeLost = Utils.voltageMargeFactorSub(lifeLost);
			
			lifeLost *= overFactor;
			lifeLost *= overFactor;
			lifeLost *= overFactor;
			
			NBTTagCompound lampNbt = lampStack.getTagCompound();
			
			double life = lampNbt.getDouble("life") - lifeLost;
			lampNbt.setDouble("life", life);
			
			if(life<0)
			{
				lamp.inventory.setInventorySlotContents(0, null);
				light = 0;
			}
			

			boot = false;
			
		}
		
		lamp.sixNode.recalculateLightValue();
		
		if(invulerabilityTimeLeft != 0)
		{
			invulerabilityTimeLeft -= deltaT;
			if(invulerabilityTimeLeft < 0) invulerabilityTimeLeft = 0;
		}
		deltaT = deltaTBase + deltaTBase*(-0.1 + 0.2*Math.random());
		
		
		lampStackLast  = lampStack;
		

	}

	
	

	public void publish()
	{
		System.out.print("Light published");
	}


	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		stableProb = nbt.getDouble(str + "LSP" + "stableProb");
	}


	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "LSP" + "stableProb",stableProb);
	}
}
