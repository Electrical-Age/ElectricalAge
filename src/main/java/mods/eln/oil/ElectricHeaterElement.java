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
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** ElectricHeaterDescriptor: power on the sides, heat out of the back. */
public class ElectricHeaterElement extends TransparentNodeElement {
    final ElectricHeaterDescriptor desc;
    final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor resistor = new Resistor(load, null);
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");

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
        // insulated except its back: unloaded it settles near 600 degrees at nominal power; Rs lets ~P through to a
        // boiler at a few tens of degrees' difference
        thermal.set(0.01, 2000.0 / desc.nominalP, 1000.0 * desc.nominalP / 1000.0);   // ~5% lost to the air at 100 K
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Power"), Utils.plotPower("", resistor.getP()));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        return info;
    }
}
