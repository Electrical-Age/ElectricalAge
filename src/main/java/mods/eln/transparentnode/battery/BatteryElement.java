package mods.eln.transparentnode.battery;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.OverHeatingProtectionDescriptor;
import mods.eln.item.OverVoltageProtectionDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.NodeVoltageState;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.ResistorSwitch;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.nbt.NbtBatteryProcess;
import mods.eln.sim.nbt.NbtBatterySlowProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BatteryElement extends TransparentNodeElement {

    public NbtElectricalLoad cutLoad = new NbtElectricalLoad("cutLoad");
    public NodeVoltageState positiveLoad = new NodeVoltageState("positiveLoad");
    public NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    public VoltageSource voltageSource = new VoltageSource("volSrc", positiveLoad, negativeLoad);

    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    public ElectricalLoadHeatThermalLoad negativeETProcess = new ElectricalLoadHeatThermalLoad(negativeLoad, thermalLoad);
    public ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();

    public NbtBatteryProcess batteryProcess = new NbtBatteryProcess(positiveLoad, negativeLoad, null, 0, voltageSource);

    public Resistor dischargeResistor = new Resistor(positiveLoad, negativeLoad);
    public ResistorSwitch cutSwitch = new ResistorSwitch("cutSwitch", cutLoad, positiveLoad);

    public BatteryInventoryProcess inventoryProcess = new BatteryInventoryProcess(this);

    double syncronizedPositiveUc, syncronizedNegativeUc, syncronizedCurrent, syncronizedTc;

    NbtBatterySlowProcess batterySlowProcess = new NbtBatterySlowProcess(node, batteryProcess, thermalLoad);
    private final Failure failure = new Failure(this).battery(() -> batteryProcess.getEnergy());

    AutoAcceptInventoryProxy inventory =
        (new AutoAcceptInventoryProxy(new TransparentNodeElementInventory(2, 64, this)))
            .acceptIfEmpty(0, OverVoltageProtectionDescriptor.class)
            .acceptIfEmpty(1, OverHeatingProtectionDescriptor.class);

    boolean fromNBT = false;

    public BatteryDescriptor descriptor;

    //static int UUIDCounter = 0;
    //int UUID = 0;
    boolean fromItemStack = false;
    double fromItemStack_charge;
    double fromItemStack_life;

    public BatteryElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        this.descriptor = (BatteryDescriptor) descriptor;

        electricalLoadList.add(cutLoad);
        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);

        electricalComponentList.add(new Resistor(positiveLoad, null));
        electricalComponentList.add(new Resistor(negativeLoad, null));
        //electricalComponentList.add(new Resistor(cutLoad, null).setR(1000));
        //	electricalComponentList.add(new Resistor(positiveLoad, null).setR(1000));
        //electricalComponentList.add(new Resistor(negativeLoad, null).setR(1000));
        electricalComponentList.add(dischargeResistor);
        electricalComponentList.add(voltageSource);
        electricalComponentList.add(cutSwitch);
        thermalLoadList.add(thermalLoad);
        electricalProcessList.add(batteryProcess);
        thermalFastProcessList.add(negativeETProcess);

        slowProcessList.add(batterySlowProcess);
        slowProcessList.add(inventoryProcess);

        grounded = false;
        batteryProcess.setIMax(this.descriptor.IMax);

        slowProcessList.add(thermalWatchdog);

        thermalWatchdog
            .set(thermalLoad)
            .setTMax(this.descriptor.thermalWarmLimit)
            .set(failure);
        batterySlowProcess.failure = failure;
    }

    @Override
    public Container getInventory() {
        return inventory.getInventory();
    }

    public boolean hasOverVoltageProtection() {
        return !Utils.isEmpty(getInventory().getItem(0));
    }

    public boolean hasOverHeatingProtection() {
        return !Utils.isEmpty(getInventory().getItem(1));
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front.left()) return cutLoad;
        if (side == front.right() && !grounded) return negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
    /*	if(side == front.left()) return thermalLoad;
		if(side == front.right() && ! grounded) return thermalLoad;*/
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front.left()) return node.maskElectricalPower;
        if (side == front.right() && !grounded) return node.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        //	if (side == front)return  Utils.plotVolt("U+", positiveLoad.Uc );
        //	if (side == front.back() && ! grounded)return  Utils.plotVolt("U-", negativeLoad.Uc );
        return Utils.plotVolt("Ubat:", batteryProcess.getU()) + Utils.plotAmpere("Current Output:", batteryProcess.getDischargeCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("Tbat:", thermalLoad.Tc);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            double U = batteryProcess.getU();//(positiveLoad.Uc - negativeLoad.Uc);
            stream.writeFloat((float) (U * batteryProcess.getDischargeCurrent()));
            stream.writeFloat((float) batteryProcess.getEnergy());
            stream.writeShort((short) (batteryProcess.life * 1000));

            node.lrduCubeMask.getTranslate(Direction.YN).serialize(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        initPhysicalValue();
        connect();
    }

    public void initPhysicalValue() {
        descriptor.applyTo(batteryProcess);
        descriptor.applyTo(thermalLoad);
        descriptor.applyTo(dischargeResistor);
        descriptor.applyTo(batterySlowProcess);
        cutSwitch.setR(descriptor.electricalRs / 2);
        cutLoad.setRs(descriptor.electricalRs / 2);
        negativeLoad.setRs(descriptor.electricalRs);
        if (fromItemStack) {
            batteryProcess.life = fromItemStack_life;
            batteryProcess.setCharge(fromItemStack_charge);
            fromItemStack = false;
        }
    }

    @Override
    public void inventoryChange(Container inventory) {
        //	initPhysicalValue();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return inventory.take(entityPlayer.getMainHandItem());
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        //inventory.writeToNBT(nbt, str + "inv");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        fromNBT = true;
        //inventory.readFromNBT(nbt, str + "inv");
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void onGroundedChangedByClient() {
        super.onGroundedChangedByClient();

        disconnect();
        initPhysicalValue();
        reconnect();
    }

    @Override
    public void readItemStackNBT(CompoundTag nbt) {
        super.readItemStackNBT(nbt);

        if (nbt == null) nbt = descriptor.getDefaultNBT();
        fromItemStack_charge = nbt.getDoubleOr("charge", 0.0);
        fromItemStack_life = nbt.getDoubleOr("life", 0.0);

        fromItemStack = true;
    }

    @Override
    public CompoundTag getItemStackNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putDouble("charge", batteryProcess.getCharge());
        nbt.putDouble("life", batteryProcess.life);
        return nbt;
    }

	/*
	public static CompoundTag newItemStackNBT() {
		CompoundTag nbt = new CompoundTag("itemStackNBT");
		nbt.putDouble("charge", 0.5);
		nbt.putDouble("life", 1.0);
		return nbt;
	}*/

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> wailaList = new HashMap<String, String>();
        wailaList.put(I18N.tr("Charge"), Utils.plotPercent("", batteryProcess.getCharge()));
        wailaList.put(I18N.tr("Energy"), Utils.plotEnergy("", batteryProcess.getEnergy()));
        wailaList.put(I18N.tr("Life"), Utils.plotPercent("", batteryProcess.life));
        if (Eln.wailaEasyMode) {
            wailaList.put(I18N.tr("Voltage"), Utils.plotVolt("", batteryProcess.getU()));
            wailaList.put(I18N.tr("Current"), Utils.plotAmpere("", batteryProcess.getDischargeCurrent()));
        }
        return wailaList;
    }
}
