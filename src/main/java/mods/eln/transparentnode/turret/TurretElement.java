package mods.eln.transparentnode.turret;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtResistor;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    final AutoAcceptInventoryProxy acceptingInventory =
        (new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(1, 64, this)))
            .acceptAlways(0, 1, new AutoAcceptInventoryProxy.SimpleItemDropper(node), EntitySensorFilterDescriptor.class);

    public TurretElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (TurretDescriptor) descriptor;
        chargePower = ((TurretDescriptor) descriptor).getProperties().chargePower;
        slowProcessList.add(new TurretSlowProcess(this));
        simulation = new TurretMechanicsSimulation((TurretDescriptor) descriptor);
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
        Coordinate lightSourceCoordinate = new Coordinate();
        lightSourceCoordinate.copyFrom(coordonate());
        lightSourceCoordinate.move(front);
        LightBlockEntity.addLight(lightSourceCoordinate, 25, 2);
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
        if (side == front.back() && lrdu == LRDU.Down) return load;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (side == front.back() && lrdu == LRDU.Down) return NodeBase.maskElectricalPower;
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
        return acceptingInventory.take(entityPlayer.getCurrentEquippedItem());
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
            Utils.serialiseItemStack(stream, acceptingInventory.getInventory().getStackInSlot(TurretContainer.filterId));
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
        return acceptingInventory.getInventory();
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new TurretContainer(player, acceptingInventory.getInventory());
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

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Charge power"), Utils.plotPower("", chargePower));

        ItemStack filterStack = acceptingInventory.getInventory().getStackInSlot(TurretContainer.filterId);
        if (filterStack != null) {
            GenericItemUsingDamageDescriptor gen = EntitySensorFilterDescriptor.getDescriptor(filterStack);
            if (gen != null && gen instanceof EntitySensorFilterDescriptor) {
                EntitySensorFilterDescriptor filter = (EntitySensorFilterDescriptor) gen;
                String target = I18N.tr("Shoot ");
                if (filterIsSpare) {
                    target += "not ";
                }
                if (filter.entityClass == EntityPlayer.class) {
                    target += I18N.tr("players");
                } else if (filter.entityClass == IMob.class) {
                    target += I18N.tr("monsters");
                } else if (filter.entityClass == EntityAnimal.class) {
                    target += I18N.tr("animals");
                } else {
                    target += I18N.tr("??");
                }
                info.put(I18N.tr("Target"), target);
            }
        } else {
            if (filterIsSpare) {
                info.put(I18N.tr("Target"), I18N.tr("Shoot everything"));
            } else {
                info.put(I18N.tr("Target"), I18N.tr("Shoot nothing"));
            }
        }

        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Charge level"),
                Utils.plotPercent("", energyBuffer / descriptor.getProperties().impulseEnergy));
        }
        return info;
    }
}
