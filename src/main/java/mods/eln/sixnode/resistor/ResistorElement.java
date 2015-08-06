package mods.eln.sixnode.resistor;

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
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sim.process.heater.ResistorHeatThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;

public class ResistorElement extends SixNodeElement {

    ResistorDescriptor descriptor;
    NbtElectricalLoad aLoad = new NbtElectricalLoad("aLoad");
    NbtElectricalLoad bLoad = new NbtElectricalLoad("bLoad");
    Resistor r = new Resistor(aLoad, bLoad);

    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    ResistorHeatThermalLoad heater = new ResistorHeatThermalLoad(r, thermalLoad);

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public ResistorElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
        super(SixNode, side, descriptor);
        this.descriptor = (ResistorDescriptor) descriptor;

        electricalLoadList.add(aLoad);
        electricalLoadList.add(bLoad);
        aLoad.setRs(MnaConst.noImpedance);
        bLoad.setRs(MnaConst.noImpedance);
        electricalComponentList.add(r);

        thermalLoadList.add(thermalLoad);
        thermalSlowProcessList.add(heater);
        thermalLoad.setAsSlow();
        double thermalC = this.descriptor.thermalMaximalPowerDissipated * this.descriptor.thermalNominalHeatTime / (this.descriptor.thermalWarmLimit);
        double thermalRp = this.descriptor.thermalWarmLimit / this.descriptor.thermalMaximalPowerDissipated;
        double thermalRs = 0; //thermalConductivityTao / thermalC / 2;
        thermalLoad.set(thermalRs, thermalRp, thermalC);
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog
                .set(thermalLoad)
                .setLimit(this.descriptor.thermalWarmLimit, this.descriptor.thermalCoolLimit)
                .set(new WorldExplosion(this).cableExplosion());
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front.right()) return aLoad;
        if (lrdu == front.left()) return bLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front.right() || lrdu == front.left()) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString() {
        double u = -Math.abs(aLoad.getU() - bLoad.getU());
        double i = Math.abs(r.getI());
        return Utils.plotOhm(Utils.plotUIP(u, i), r.getR());
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
        double rs = descriptor.getRsValue(inventory);
        r.setR(rs);
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
