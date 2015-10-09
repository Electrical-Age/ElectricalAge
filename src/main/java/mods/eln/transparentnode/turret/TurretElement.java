package mods.eln.transparentnode.turret;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.*;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtResistor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

public class TurretElement extends TransparentNodeElement {

    public static final byte ToggleFilterMeaning = 1;
    public static final byte UnserializeChargePower = 2;

	private final TurretDescriptor descriptor;
	
	private final TurretMechanicsSimulation simulation;

    public double chargePower;
    public boolean filterIsSpare = false;
	public double energyBuffer = 0;

	final NbtElectricalLoad load = new NbtElectricalLoad("load");
	final NbtResistor powerResistor = new NbtResistor("powerResistor", load, null);

    final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);

	public TurretElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode, descriptor);
		this.descriptor = (TurretDescriptor)descriptor;
        chargePower = ((TurretDescriptor) descriptor).getProperties().chargePower;
		slowProcessList.add(new TurretSlowProcess(this));
		simulation = new TurretMechanicsSimulation((TurretDescriptor)descriptor);
		slowProcessList.add(simulation);
		
		Eln.instance.highVoltageCableDescriptor.applyTo(load);
		electricalLoadList.add(load);
		electricalComponentList.add(powerResistor);
	
	}
	
	public TurretDescriptor getDescriptor() {
		return descriptor;
	}

	public float getTurretAngle() {
		return simulation.getTurretAngle();
	}
	
	public void setTurretAngle(float angle) {
		if (simulation.setTurretAngle(angle)) needPublish();
	}
	
	public float getGunPosition() {
		return simulation.getGunPosition();
	}
	
	public void setGunPosition(float position) {
		if (simulation.setGunPosition(position)) needPublish();
	}
	
	public void setGunElevation(float elevation) {
		if (simulation.setGunElevation(elevation)) needPublish();
	}
	
	public void setSeekMode(boolean seekModeEnabled) {
		if (seekModeEnabled != simulation.inSeekMode()) needPublish();
		simulation.setSeekMode(seekModeEnabled);
	}
	
	public void shoot() {
		if (simulation.shoot()) needPublish();
	}
	
	public boolean isTargetReached() {
		return simulation.isTargetReached();
	}
	
	public void setEnabled(boolean armed) {
		if (simulation.setEnabled(armed)) needPublish();
	}
	
	public boolean isEnabled() {
		return simulation.isEnabled();
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		if(side == front.back() && lrdu == LRDU.Down) return load;
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(side == front.back() && lrdu == LRDU.Down) return NodeBase.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(load.getU(), load.getI());
	}

	@Override
	public String thermoMeterString(Direction side) {
		return null;
	}

	@Override
	public void initialize() {
		connect();
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		return false;
	}
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
    	try {
    		stream.writeFloat(simulation.getTurretTargetAngle());
    		stream.writeFloat(simulation.getGunTargetPosition());
    		stream.writeFloat(simulation.getGunTargetElevation());
    		stream.writeBoolean(simulation.inSeekMode());
    		stream.writeBoolean(simulation.isShooting());
    		stream.writeBoolean(simulation.isEnabled());
            Utils.serialiseItemStack(stream, inventory.getStackInSlot(TurretContainer.filterId));
            stream.writeBoolean(filterIsSpare);
            stream.writeFloat((float) chargePower);
 		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
        nbt.setDouble("chargePower", chargePower);
        nbt.setBoolean("filterIsSpare", filterIsSpare);
		nbt.setDouble("energyBuffer", energyBuffer);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
        chargePower = nbt.getDouble("chargePower");
        filterIsSpare = nbt.getBoolean("filterIsSpare");
        energyBuffer = nbt.getDouble("energyBuffer");
	}

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new TurretContainer(player, inventory);
    }

    @Override
    public void inventoryChange(IInventory inventory) {
        super.inventoryChange(inventory);
        needPublish();
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte packetType = super.networkUnserialize(stream);
        try {
            switch (packetType) {
                case ToggleFilterMeaning:
                    filterIsSpare = !filterIsSpare;
                    needPublish();
                    break;

                case UnserializeChargePower:
                    chargePower = stream.readFloat();
                    needPublish();
                    break;
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
        return unserializeNulldId;
    }
}
