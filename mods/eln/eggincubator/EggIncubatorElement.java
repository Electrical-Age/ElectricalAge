package mods.eln.eggincubator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.management.Descriptor;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.heatfurnace.HeatFurnaceContainer;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalLoadHeatThermalLoadProcess;
import mods.eln.sim.ElectricalSourceRefGroundProcess;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.TransformerProcess;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemBlockDamageDualLoad;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingData;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

public class EggIncubatorElement extends TransparentNodeElement{
	
	
	public NodeElectricalLoad powerLoad = new NodeElectricalLoad("powerLoad");

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);
	EggIncubatorProcess slowProcess = new EggIncubatorProcess();
	EggIncubatorDescriptor descriptor;
	public EggIncubatorElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
	   	electricalLoadList.add(powerLoad);
	   	slowProcessList.add(slowProcess);
	   	
	   	this.descriptor = (EggIncubatorDescriptor) descriptor;
	   	
	}


	class EggIncubatorProcess implements IProcess,INBTTReady
	{

		double energy = 5000;
		public EggIncubatorProcess() {

			resetEnergy();
		}
		void resetEnergy()
		{
			energy = 10000 + Math.random()*10000;
		}
		
		@Override
		public void process(double time) {
			energy -= powerLoad.getRpPower()*time;
			if(inventory.getStackInSlot(EggIncubatorContainer.EggSlotId)!=null){
				descriptor.setState(powerLoad, true);
				if(energy <= 0){
					inventory.decrStackSize(EggIncubatorContainer.EggSlotId, 1);
					EntityChicken chicken = new EntityChicken(node.coordonate.world());
					chicken.setGrowingAge(-24000);
					EntityLiving entityliving = (EntityLiving)chicken;
					entityliving.setLocationAndAngles(node.coordonate.x+0.5, node.coordonate.y+0.5, node.coordonate.z+0.5, MathHelper.wrapAngleTo180_float(node.coordonate.world().rand.nextFloat() * 360.0F), 0.0F);
                    entityliving.rotationYawHead = entityliving.rotationYaw;
                    entityliving.renderYawOffset = entityliving.rotationYaw;
                    entityliving.func_110161_a((EntityLivingData)null);
                    node.coordonate.world().spawnEntityInWorld(entityliving);
                    entityliving.playLivingSound();
					//node.coordonate.world().spawnEntityInWorld());
                    resetEnergy();

                	needPublish();
				}
			}
			else{
				descriptor.setState(powerLoad, false);
				resetEnergy();
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			energy = nbt.getDouble(str + "energyCounter");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "energyCounter", energy);
		}
	}
	
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(lrdu != LRDU.Down) return null;
		return powerLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(lrdu == lrdu.Down)
		{
			return NodeBase.maskElectricalPower;	
		}
		return 0;
	}


	
	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(powerLoad.Uc, powerLoad.getCurrent());

	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return  null;
	}

	
	@Override
	public void initialize() {

		descriptor.applyTo(powerLoad);
		
		connect();
    			
	}

    public void inventoryChange(IInventory inventory)
    {
 
    	needPublish();
    }
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new EggIncubatorContainer(player, inventory,node);
	}

	

	public float getLightOpacity() {
		// TODO Auto-generated method stub
		return 1.0f;
	}
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	
	


	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			if(inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) == null) stream.writeByte(0);
			else stream.writeByte(inventory.getStackInSlot(EggIncubatorContainer.EggSlotId).stackSize);

			
			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
			
			stream.writeFloat((float) powerLoad.Uc);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
