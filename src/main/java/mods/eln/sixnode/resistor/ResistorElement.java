package mods.eln.sixnode.resistor;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ResistorProcess;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ResistorHeatThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

import javax.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ResistorElement extends SixNodeElement {

    ResistorDescriptor descriptor;
    NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    Resistor r = new Resistor(aLoad, bLoad);

    public NbtElectricalGateInput control;

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    ResistorHeatThermalLoad heater = new ResistorHeatThermalLoad(r, thermalLoad);
    ResistorProcess resistorProcess;

    public double nominalRs = 1;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public ResistorElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
        super(SixNode, side, descriptor);
        this.descriptor = (ResistorDescriptor) descriptor;

        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        aLoad.setRs(MnaConst.noImpedance);
        bLoad.setRs(MnaConst.noImpedance);
        electricalComponentList.add(r);
        if (this.descriptor.isRheostat) {
            control = new NbtElectricalGateInput("control");
            electricalLoadList.add(control);
        }

        thermalLoadList.add(thermalLoad);
        thermalSlowProcessList.add(heater);
        thermalLoad.setAsSlow();
        double thermalC = this.descriptor.thermalMaximalPowerDissipated * this.descriptor.thermalNominalHeatTime / (this.descriptor.thermalWarmLimit);
        double thermalRp = this.descriptor.thermalWarmLimit / this.descriptor.thermalMaximalPowerDissipated;
        double thermalRs = this.descriptor.thermalConductivityTao / thermalC / 2;
        thermalLoad.set(thermalRs, thermalRp, thermalC);
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
                .set(new WorldExplosion(this).cableExplosion());

        resistorProcess = new ResistorProcess(this, r, thermalLoad, this.descriptor);
        if (this.descriptor.tempCoef != 0 || this.descriptor.isRheostat) {
            slowProcessList.add(resistorProcess);
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            if (descriptor.isRheostat)
                stream.writeFloat((float) control.getNormalized());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front.right()) return aLoad;
        if (lrdu == front.left()) return bLoad;
        if (lrdu == front) return control;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front.right() || lrdu == front.left()) return NodeBase.maskElectricalPower;
        if (lrdu == front && descriptor.isRheostat) return NodeBase.maskElectricalInputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        double u = -Math.abs(aLoad.getU() - bLoad.getU());
        double i = Math.abs(r.getI());
        return Utils.plotOhm(Utils.plotUIP(u, i), r.getR()) +
                (control != null ? Utils.plotPercent("C", control.getNormalized()) : "");
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.TR("Resistance"), Utils.plotValue(r.getR(), "Ω"));
        info.put(I18N.TR("Voltage drop"), Utils.plotVolt("", Math.abs(r.getU())));
        if (Eln.wailaEasyMode) {
            info.put(I18N.TR("Current"), Utils.plotAmpere("", Math.abs(r.getI())));

        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return Utils.plotCelsius("T", thermalLoad.Tc);
    }

    @Override
    public void initialize() {
        setupPhysical();
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        setupPhysical();
    }

    public void setupPhysical() {
        nominalRs = descriptor.getRsValue(inventory);
        resistorProcess.process(0);
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        return onBlockActivatedRotate(entityPlayer);
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ResistorContainer(player, inventory);
    }
}
