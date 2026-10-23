package mods.eln.sixnode.electricalswitch;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sound.SoundCommand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalSwitchElement extends SixNodeElement {

    public ElectricalSwitchDescriptor descriptor;
    public NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    public NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    public ResistorSwitch switchResistor = new ResistorSwitch("switchRes", aLoad, bLoad);

    VoltageStateWatchDog voltageWatchDogA = new VoltageStateWatchDog();
    VoltageStateWatchDog voltageWatchDogB = new VoltageStateWatchDog();
//	ResistorCurrentWatchdog currentWatchDog = new ResistorCurrentWatchdog();

    boolean switchState = false;

    public ElectricalSwitchElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        switchResistor.mustUseUltraImpedance();
        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        electricalComponentList.add(switchResistor);
        electricalComponentList.add(new Resistor(bLoad, null).pullDown());
        electricalComponentList.add(new Resistor(aLoad, null).pullDown());

        this.descriptor = (ElectricalSwitchDescriptor) descriptor;

        Failure exp = new Failure(this).electronics();

        //	slowProcessList.add(currentWatchDog);
        slowProcessList.add(voltageWatchDogA);
        slowProcessList.add(voltageWatchDogB);

        //currentWatchDog.set(switchResistor).setIAbsMax(this.descriptor.maximalPower/this.descriptor.nominalVoltage).set(exp);
        voltageWatchDogA.set(aLoad).setUNominalMirror(this.descriptor.nominalVoltage).set(exp);
        voltageWatchDogB.set(bLoad).setUNominalMirror(this.descriptor.nominalVoltage).set(exp);
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByteOr("front", (byte) 0);
        front = LRDU.fromInt((value >> 0) & 0x3);
        switchState = nbt.getBooleanOr("switchState", false);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("front", (byte) (front.toInt() << 0));
        nbt.putBoolean("switchState", switchState);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front == lrdu) return aLoad;
        if (front.inverse() == lrdu) return bLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        //return thermalLoad;
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return descriptor.getNodeMask();
        if (front.inverse() == lrdu) return descriptor.getNodeMask();

        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("Ua:", aLoad.getU()) + Utils.plotVolt("Ub:", bLoad.getU()) + Utils.plotAmpere("I:", aLoad.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Position"), switchState ? I18N.tr("Closed") : I18N.tr("Open"));
        info.put(I18N.tr("Current"), Utils.plotAmpere("", aLoad.getCurrent()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Voltages"), Utils.plotVolt("", aLoad.getU()) + Utils.plotVolt(" ", bLoad.getU()));
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        //return Utils.plotCelsius("T:",thermalLoad.Tc);
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(switchState);
            stream.writeShort((short) (aLoad.getU() * NodeBase.networkSerializeUFactor));
            stream.writeShort((short) (bLoad.getU() * NodeBase.networkSerializeUFactor));
            stream.writeShort((short) (aLoad.getCurrent() * NodeBase.networkSerializeIFactor));
            //stream.writeShort((short)(thermalLoad.Tc * NodeBase.networkSerializeTFactor));
            stream.writeShort(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSwitchState(boolean state) {
        switchState = state;
        switchResistor.setState(state);
        needPublish();
    }

    @Override
    public void initialize() {
        //descriptor.thermal.applyTo(thermalLoad);

        descriptor.applyTo(aLoad);
        descriptor.applyTo(bLoad);

        switchResistor.setR(descriptor.electricalRs);

        setSwitchState(switchState);
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;
        ItemStack currentItemStack = entityPlayer.getMainHandItem();

        if (mods.eln.node.MeterItems.isMultiMeter(entityPlayer.getMainHandItem())) {
            return false;
        }
        if (mods.eln.node.MeterItems.isThermometer(entityPlayer.getMainHandItem())) {
            return false;
        }
        if (mods.eln.node.MeterItems.isAllMeter(entityPlayer.getMainHandItem())) {
            return false;
        } else {
            setSwitchState(!switchState);
            //playSoundEffect("random.click", 0.3F, 0.6F);
            play(new SoundCommand("random.click").mulVolume(0.3F, 0.6f).smallRange());
            return true;
        }
        //front = LRDU.from((front.toInt()+1)&3);
    }
}
