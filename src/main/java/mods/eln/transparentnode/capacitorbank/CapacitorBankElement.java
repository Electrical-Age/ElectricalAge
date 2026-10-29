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
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.nbt.NbtThermalLoad;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * The capacitor bank (CapacitorBankDescriptor): a capacitor behind an equivalent series resistance (ESR) between its +
 * (front's left) and - (front's right) terminals, a leakage resistor across it (R = leakTauS / C), and the Power
 * Capacitor's over-voltage watchdog (1.3 x the rating). The ESR's I^2 R and the leak heat it (a thermal load, cooled to
 * ambient); past WARM it fails as past its voltage does: it explodes, by the energy it held (Baughn 2026-10-28: at
 * this size, realistic). The anvil's bursts warm it by watts; a stiff line's inrush puts most of the bank's 115 kJ into
 * the ESR in a moment. No inrush limit: an empty bank straight on a stiff 800 V line draws hundreds of amps and burns its cable
 * (Baughn 2026-10-28: "inrush current is supposed to be a problem"); charge it through a resistor or from a soft plant.
 * Its charge is the loads' voltages, saved with them (NbtElectricalLoad), so it keeps it over a restart.
 */
public class CapacitorBankElement extends TransparentNodeElement {
    public final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    public final NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");
    public final NbtElectricalLoad cellLoad = new NbtElectricalLoad("cellLoad");
    public final Capacitor capacitor = new Capacitor(cellLoad, negativeLoad);
    final Resistor esr = new Resistor(positiveLoad, cellLoad);
    final Resistor leak = new Resistor(cellLoad, negativeLoad);
    final BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    /** ESR (ohm); heat capacity (J/K) and resistance to ambient (K/W); it fails WARM above ambient (~95 C). */
    public static final double ESR = 0.1, HEAT_CAPACITY = 2000, TO_AMBIENT = 0.3, WARM = 75;

    public CapacitorBankElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalLoadList.add(cellLoad);
        electricalComponentList.add(capacitor);
        electricalComponentList.add(esr);
        electricalComponentList.add(leak);
        thermalLoadList.add(thermal);
        thermalFastProcessList.add(t -> thermal.movePowerTo(Math.abs(esr.getP()) + Math.abs(leak.getP())));
        Failure failure = new Failure(this).capacitorBank(this::energy);
        slowProcessList.add(watchdog);
        watchdog.set(failure);
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(WARM).set(failure));
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
        esr.setR(ESR);
        thermal.set(0.1, TO_AMBIENT, HEAT_CAPACITY);
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
        return Utils.plotCelsius("T:", thermal.Tc);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", voltage()));
        info.put(I18N.tr("Stored"), Utils.plotEnergy("", energy()));
        info.put(I18N.tr("Capacitance"), String.format("%.2f F", CapacitorBankDescriptor.farads));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        return info;
    }
}
