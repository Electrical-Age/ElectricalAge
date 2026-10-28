package mods.eln.transparentnode.capacitorbank;

import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * The capacitor bank (CapacitorBankDescriptor): a plain capacitor between its + (front's left) and - (front's right)
 * terminals, a leakage resistor across it (R = leakTauS / C), and the Power Capacitor's over-voltage watchdog (1.3 x the
 * rating). No inrush limit: an empty bank straight on a stiff 800 V line draws hundreds of amps and burns its cable
 * (Baughn 2026-10-28: "inrush current is supposed to be a problem"); charge it through a resistor or from a soft plant.
 * Its charge is the loads' voltages, saved with them (NbtElectricalLoad), so it keeps it over a restart.
 */
public class CapacitorBankElement extends TransparentNodeElement {
    public final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    public final NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    public final Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
    final Resistor leak = new Resistor(positiveLoad, negativeLoad);
    final BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    public CapacitorBankElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalComponentList.add(capacitor);
        electricalComponentList.add(leak);
        slowProcessList.add(watchdog);
        watchdog.set(new Failure(this).capacitor());
        positiveLoad.setAsMustBeFarFromInterSystem();
    }

    /** Voltage across it (V) and the energy it holds (J). */
    public double voltage() {
        return capacitor.getU();
    }

    public double energy() {
        return CapacitorBankDescriptor.energyAt(voltage());
    }

    @Override
    public void initialize() {
        ElnDeviceRegistry.applySmallRs(positiveLoad);
        ElnDeviceRegistry.applySmallRs(negativeLoad);
        capacitor.setC(CapacitorBankDescriptor.farads);
        leak.setR(CapacitorBankDescriptor.leakTauS / CapacitorBankDescriptor.farads);
        watchdog.setUNominal(CapacitorBankDescriptor.ratedVolts);
        connect();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        if (side == front.left()) return positiveLoad;
        if (side == front.right()) return negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front.left() || side == front.right()) ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(voltage(), capacitor.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", voltage()));
        info.put(I18N.tr("Stored"), Utils.plotEnergy("", energy()));
        info.put(I18N.tr("Capacitance"), String.format("%.2f F", CapacitorBankDescriptor.farads));
        return info;
    }
}
