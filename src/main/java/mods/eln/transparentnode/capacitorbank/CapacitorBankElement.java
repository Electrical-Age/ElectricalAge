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
 * The capacitor bank (CapacitorBankDescriptor): one capacitor between an inner cell node and its - terminal (front's
 * right), a leakage resistor across it (R = leakTauS / C), and the Power Capacitor's over-voltage watchdog (1.3 x the
 * rating). Between the + terminal (front's left) and the cell sits an inrush limiter: discharging it is a near short,
 * charging it lets in at most chargeAmps. Without it an empty 0.36 F bank on a stiff 800 V line drew hundreds of amps
 * and burnt the cable in the first second (the GameTest found it). Its charge is the loads' voltages, saved with them
 * (NbtElectricalLoad), so it keeps it over a restart.
 */
public class CapacitorBankElement extends TransparentNodeElement {
    public final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    public final NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    public final NbtElectricalLoad cellLoad = new NbtElectricalLoad("cellLoad");
    public final Capacitor capacitor = new Capacitor(cellLoad, negativeLoad);
    final Resistor leak = new Resistor(cellLoad, negativeLoad);
    final Resistor limiter = new Resistor(positiveLoad, cellLoad);
    /** The limiter when it isn't limiting (ohm): small beside the anvil's 128. */
    static final double R_PASS = 0.05;
    final BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    public CapacitorBankElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalLoadList.add(cellLoad);
        electricalComponentList.add(capacitor);
        electricalComponentList.add(leak);
        electricalComponentList.add(limiter);
        electricalProcessList.add(t -> {
            // charging (the line above the cell): as much resistance as keeps it at chargeAmps; else pass straight through
            double du = positiveLoad.getU() - cellLoad.getU();
            double want = du > CapacitorBankDescriptor.chargeAmps * R_PASS ? du / CapacitorBankDescriptor.chargeAmps : R_PASS;
            double r = limiter.getR();
            if (Math.abs(want - r) > 0.05 * r) limiter.setR(want);   // (re-solving the network for every small change costs)
        });
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
        ElnDeviceRegistry.applySmallRs(cellLoad);
        // (start limiting, from the cell's saved charge: the first step's inrush alone burnt a cable)
        double du = Math.max(0, mods.eln.Eln.HVU - cellLoad.getU());
        limiter.setR(Math.max(R_PASS, du / CapacitorBankDescriptor.chargeAmps));
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
