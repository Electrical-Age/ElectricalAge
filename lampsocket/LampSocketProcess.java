package mods.eln.lampsocket;

import cpw.mods.fml.relauncher.Side;
import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.lampsocket.LightBlockEntity.LightBlockObserver;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.node.NodeServer;
import mods.eln.node.SixNode;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class LampSocketProcess implements IProcess , INBTTReady,LightBlockObserver{
	double time = 0;
	double deltaTBase = 0.2;
	double deltaT = deltaTBase;
	public double invulerabilityTimeLeft = 2;
	boolean overPoweredInvulerabilityArmed = true;
	LampSocketElement lamp;
	int light = 0; //0..15
	double alphaZ = 0.0;
	
	double stableProb = 0;
	
	public LampSocketProcess(LampSocketElement lamp) {
		this.lamp = lamp;
		lbCoord = new Coordonate(lamp.sixNode.coordonate);
		LightBlockEntity.addObserver(this);
	}
	
	ItemStack lampStackLast = null;
	boolean boot = true;
	
	double vp[] = new double[3];
	double vv[] = new double[3];
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		this.time += time;
		if(this.time < deltaT) return;
		
		this.time -= deltaT;
		
		lamp.computeElectricalLoad();
		int oldLight = light;
		int newLight = 0;
		
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
			

			if(lightDouble - oldLight > 1.3){
				newLight =  (int) lightDouble;
			}
			else if(lightDouble - oldLight < -0.3){
				newLight =  (int) lightDouble;
			}
			else
			{

				newLight = oldLight;
			}

			if(newLight < 0) newLight = 0;
			if(newLight > 15) newLight = 15;
						
			
			
			
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

		if(invulerabilityTimeLeft != 0)
		{
			invulerabilityTimeLeft -= deltaT;
			if(invulerabilityTimeLeft < 0) invulerabilityTimeLeft = 0;
		}
		deltaT = deltaTBase + deltaTBase*(-0.1 + 0.2*Math.random());
		
		
		lampStackLast  = lampStack;

		placeSpot(newLight);

	}

	void placeSpot(int newLight)
	{
		boolean exit = false;
		myCoord().copyTo(vp);
		vv[0] = 1.0 * Math.cos(alphaZ*Math.PI/180.0);
		vv[1] = 1.0 * Math.sin(alphaZ*Math.PI/180.0);
		vv[2] = 0.0;
		lamp.front.rotateOnXnLeft(vv);
		lamp.side.rotateFromXN(vv);
		Coordonate newCoord = new Coordonate(myCoord());
/*
		for(int idx = 0;idx < lamp.socketDescriptor.range;idx++)
		{
			//newCoord.move(lamp.side.getInverse());
			vp[0] += vv[0];
			vp[1] += vv[1];
			vp[2] += vv[2];
			newCoord.setPosition(vp);
			if(newCoord.getBlockExist() == false)
			{
				exit = true;
				break;
			}
			int block = newCoord.getBlockId();
			if(block != 0)
			{
				if(block != Eln.lightBlockId || lbCoord.equals(newCoord) == false)
				{
					vp[0] -= vv[0];
					vp[1] -= vv[1];
					vp[2] -= vv[2];
					newCoord.setPosition(vp);
					break;
				}
			}
		}*/
		for(int idx = 0;idx < lamp.socketDescriptor.range;idx++)
		{
			//newCoord.move(lamp.side.getInverse());
			vp[0] += vv[0];
			vp[1] += vv[1];
			vp[2] += vv[2];
			newCoord.setPosition(vp);
			if(newCoord.getBlockExist() == false)
			{
				exit = true;
				break;
			}
			if(isOpaque(newCoord))
			{
				vp[0] -= vv[0];
				vp[1] -= vv[1];
				vp[2] -= vv[2];
				newCoord.setPosition(vp);
				break;
			}
		}
		if(!exit)
		{
			int count = 0;
			while(newCoord.equals(myCoord()) == false)
			{
				int block = newCoord.getBlockId();
				if(block == 0 || block == Eln.lightBlockId)
				{
					count++;
					if(count == 2)
						break;
				}
				
				vp[0] -= vv[0];
				vp[1] -= vv[1];
				vp[2] -= vv[2];
				newCoord.setPosition(vp);
			}
			
		}
		if(exit == false)
			setLightAt(newCoord, newLight);
				
	}
	public boolean isOpaque(Coordonate coord)
	{
		int blockId = coord.getBlockId();
		boolean isNotOpaque = blockId == 0 || ! Block.blocksList[blockId].isOpaqueCube();
		return ! isNotOpaque;
	}

	public void publish()
	{
		System.out.print("Light published");
	}
	
	public void setLightAt(Coordonate coord,int value)
	{
		Coordonate oldLbCoord = lbCoord;
		lbCoord = new Coordonate(coord);
		int oldLight = light;
		boolean same = coord.equals(oldLbCoord);
		light = value;
		
		if(same == false)
		{
			if(oldLbCoord.equals(myCoord()))		
				lamp.sixNode.recalculateLightValue();
			else
				LightBlockEntity.removeLight(oldLbCoord, oldLight);
		}
		

		
		if(lbCoord.equals(myCoord()))
		{
			if(light != oldLight || same == false)
				lamp.sixNode.recalculateLightValue();
		}
		else
		{
			if(same)
				LightBlockEntity.remplaceLight(lbCoord, oldLight, light);
			else
				LightBlockEntity.addLight(lbCoord, light);
		}
	
	}
	
	Coordonate myCoord()
	{
		return lamp.sixNode.coordonate;
	}
	public void destructor()
	{
		//if(lbCoord.equals(myCoord()) == false && lbCoord.getBlockId() == Eln.lightBlockId)
		//	lbCoord.setBlock(0,0);
		//TODO
		LightBlockEntity.removeObserver(this);
		if(lbCoord.equals(myCoord()) == false)
			LightBlockEntity.removeLight(lbCoord, light);
		
	}
	
	Coordonate lbCoord;
	
	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		stableProb = nbt.getDouble(str + "LSP" + "stableProb");
		lbCoord.readFromNBT(nbt, str + "lbCoordInst");
		alphaZ = nbt.getFloat(str + "alphaZ");
		light = nbt.getInteger(str + "light");
	}


	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + "LSP" + "stableProb",stableProb);
		lbCoord.writeToNBT(nbt, str + "lbCoordInst");
		nbt.setFloat(str + "alphaZ", (float) alphaZ);
		nbt.setInteger(str + "light", light);
	}
	
	
	public int getBlockLight()
	{
		if(lbCoord.equals(myCoord()))
		{
			return light;
		}
		else
		{
			return 0;
		}
	}


	@Override
	public void lightBlockDestructor(Coordonate coord) {
		if(coord.equals(lbCoord))
		{
			placeSpot(light);
		}
	}
}
