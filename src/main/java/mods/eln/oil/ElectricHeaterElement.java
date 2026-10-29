package mods.eln.oil;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.heater.ResistorHeatThermalLoad;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.RegulatorThermalLoadToElectricalResistor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** ElectricHeaterDescriptor: power on the sides, heat out of the back. */
public class ElectricHeaterElement extends TransparentNodeElement {
    final ElectricHeaterDescriptor desc;
    final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor resistor = new Resistor(load, null);
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    final mods.eln.misc.Incandescence.Light glow = new mods.eln.misc.Incandescence.Light();
    /** Baughn: with no regulator in its slot it heats at full power until something gives; a regulator holds a set point. */
    final RegulatorThermalLoadToElectricalResistor regulator = new RegulatorThermalLoadToElectricalResistor("regulator", thermal, resistor);
    final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 1, this);
    public static final int REGULATOR_SLOT = 0;
    public static final byte SET_TARGET_EVENT = 1;

    public ElectricHeaterElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (ElectricHeaterDescriptor) d;
        electricalLoadList.add(load);
        electricalComponentList.add(resistor);
        thermalLoadList.add(thermal);
        thermalFastProcessList.add(new ResistorHeatThermalLoad(resistor, thermal));
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new VoltageStateWatchDog().set(load).setUNominal(desc.nominalU).set(new Failure(this).machine()));
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(800).set(new Failure(this).machine()));
        // red-hot coils light the room (Incandescence; Baughn 2026-10-28)
        slowProcessList.add(t -> {
            if (glow.update(node.coordonate.world(), mods.eln.misc.Incandescence.celsius(thermal.Tc))) node.setLightValue(glow.level);
        });
        thermalFastProcessList.add(regulator);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return load;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return side == front.back() ? thermal : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front.back()) return NodeBase.maskThermal;
        if (side == front || side == Direction.YN || side == Direction.YP) return 0;
        return NodeBase.maskElectricalPower;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc) + Utils.plotPower("P: ", resistor.getP());
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        resistor.setR(desc.resistorR);
        regulator.setRmin(desc.resistorR);
        inventoryChange(inventory);
        // insulated except its back: Rs lets ~P through to a boiler at a few tens of degrees' difference; with nothing
        // to heat and no regulator it passes 800 degrees and burns
        thermal.set(0.01, 2000.0 / desc.nominalP, 250.0 * desc.nominalP / 1000.0);   // ~5% lost to the air at 100 K
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public void inventoryChange(Container inv) {
        super.inventoryChange(inv);
        ItemStack stack = inventory.getItem(REGULATOR_SLOT);
        if (Utils.isEmpty(stack)) {
            regulator.setNone();
        } else {
            ((IRegulatorDescriptor) Utils.getItemObject(stack)).applyTo(regulator, 500.0, 10.0, 0.1, 0.1);
        }
        needPublish();
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) regulator.getTarget());
            stream.writeFloat((float) thermal.Tc);
            stream.writeFloat((float) resistor.getP());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte packetType = super.networkUnserialize(stream);
        try {
            if (packetType == SET_TARGET_EVENT) {
                regulator.setTarget(Math.max(0, Math.min(780, stream.readFloat())));
                needPublish();
                return unserializeNulldId;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packetType;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        regulator.writeToNBT(nbt, "");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        regulator.readFromNBT(nbt, "");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Power"), Utils.plotPower("", resistor.getP()));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        return info;
    }
}
