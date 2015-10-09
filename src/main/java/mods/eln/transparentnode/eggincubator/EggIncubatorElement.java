package mods.eln.transparentnode.eggincubator;

import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;

public class EggIncubatorElement extends TransparentNodeElement {
	
	public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
	public Resistor powerResistor = new Resistor(powerLoad, null);
	TransparentNodeElementInventory inventory = new EggIncubatorInventory(1, 64, this);
	EggIncubatorProcess slowProcess = new EggIncubatorProcess();
	EggIncubatorDescriptor descriptor;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    double lastVoltagePublish;

    public EggIncubatorElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
	   	electricalLoadList.add(powerLoad);
	   	electricalComponentList.add(powerResistor);
	   	slowProcessList.add(slowProcess);
        
	   	this.descriptor = (EggIncubatorDescriptor) descriptor;
	   	
		WorldExplosion exp = new WorldExplosion(this).machineExplosion();
		slowProcessList.add(voltageWatchdog.set(powerLoad).setUNominal(this.descriptor.nominalVoltage).set(exp));
	}

	class EggIncubatorProcess implements IProcess, INBTTReady {

		double energy = 5000;
		public EggIncubatorProcess() {
			resetEnergy();
		}
		
		void resetEnergy() {
			energy = 10000 + Math.random() * 10000;
		}
		
		@Override
		public void process(double time) {
			energy -= powerResistor.getP() * time;
			if (inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) != null) {
				descriptor.setState(powerResistor, true);
				if (energy <= 0) {
					inventory.decrStackSize(EggIncubatorContainer.EggSlotId, 1);
					EntityChicken chicken = new EntityChicken(node.coordonate.world());
					chicken.setGrowingAge(-24000);
					EntityLiving entityliving = (EntityLiving)chicken;
					entityliving.setLocationAndAngles(node.coordonate.x + 0.5, node.coordonate.y + 0.5, node.coordonate.z + 0.5, MathHelper.wrapAngleTo180_float(node.coordonate.world().rand.nextFloat() * 360.0F), 0.0F);
                    entityliving.rotationYawHead = entityliving.rotationYaw;
                    entityliving.renderYawOffset = entityliving.rotationYaw;
                    //entityliving.func_110161_a((EntityLivingData)null); 1.6.4
                    node.coordonate.world().spawnEntityInWorld(entityliving);
                    entityliving.playLivingSound();
					//node.coordonate.world().spawnEntityInWorld());
                    resetEnergy();

                	needPublish();
				}
			} else {
				descriptor.setState(powerResistor, false);
				resetEnergy();
			}
			if (Math.abs(powerLoad.getU() - lastVoltagePublish) / descriptor.nominalVoltage > 0.1) needPublish();
		}

		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			energy = nbt.getDouble(str + "energyCounter");
		}

		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			nbt.setDouble(str + "energyCounter", energy);
		}
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if (lrdu != LRDU.Down) return null;
		return powerLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if (lrdu == lrdu.Down) {
			return NodeBase.maskElectricalPower;	
		}
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
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

    public void inventoryChange(IInventory inventory) {
    	needPublish();
    }
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new EggIncubatorContainer(player, inventory,node);
	}

	public float getLightOpacity() {
		return 1.0f;
	}
	
	@Override
	public IInventory getInventory() {
		return inventory;
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			if (inventory.getStackInSlot(EggIncubatorContainer.EggSlotId) == null) stream.writeByte(0);
			else stream.writeByte(inventory.getStackInSlot(EggIncubatorContainer.EggSlotId).stackSize);

			node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
			
			stream.writeFloat((float) powerLoad.getU());
		} catch (IOException e) {
			e.printStackTrace();
		}
		lastVoltagePublish = powerLoad.getU();
	}
}
