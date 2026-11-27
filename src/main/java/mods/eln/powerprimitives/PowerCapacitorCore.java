package mods.eln.powerprimitives;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.DielectricItem;
import mods.eln.misc.Utils;
import mods.eln.misc.series.ISerie;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.IProcess;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Power Capacitor's circuit and state, shared by its six-node (sixnode/powercapacitorsix) and transparent-node
 * (transparentnode/powercapacitor) elements: the capacitor between the two loads, its discharge resistor and the
 * process that holds that resistor low until the energy a bigger C took in has been legalised, the over-voltage
 * watchdog, C and Unom from the slots. The element maps its pins to the loads and owns the inventory.
 * Saved: the loads in the element's lists (keys positiveLoad, negativeLoad) and punkELeft in the element's compound.
 */
public final class PowerCapacitorCore {
    /** The descriptor's ratings: C and Unom from the slots (PowerCapacitorContainer), the leak's time constant. */
    public interface Spec {
        ISerie capacitanceSerie();

        /** C with no redstone or no dielectric in the slots. */
        double emptyCapacitance();

        /** The discharge resistor is dischargeTao / C. Read on every setup: the config sets the six-node's late. */
        double dischargeTao();

        default double getCValue(int cableCount, double nominalDielVoltage) {
            if (cableCount == 0) return emptyCapacitance();
            double uTemp = nominalDielVoltage / Eln.LVU;
            return capacitanceSerie().getValue(cableCount - 1) / uTemp / uTemp;
        }

        default double getCValue(Container inventory) {
            ItemStack core = inventory.getItem(PowerCapacitorContainer.redId);
            ItemStack diel = inventory.getItem(PowerCapacitorContainer.dielectricId);
            if (Utils.isEmpty(core) || Utils.isEmpty(diel))
                return getCValue(0, 0);
            else {
                return getCValue(core.getCount(), getUNominalValue(inventory));
            }
        }

        default double getUNominalValue(Container inventory) {
            ItemStack diel = inventory.getItem(PowerCapacitorContainer.dielectricId);
            if (Utils.isEmpty(diel))
                return 10000;
            else {
                DielectricItem desc = (DielectricItem) DielectricItem.getDescriptor(diel);
                return desc.uNominal * diel.getCount();
            }
        }
    }

    public final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    public final NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

    public final Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
    public final Resistor dischargeResistor = new Resistor(positiveLoad, negativeLoad);
    private final PunkProcess punkProcess = new PunkProcess();
    private final BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    private final Spec spec;
    private final Container inventory;
    private double stdDischargeResistor;
    private boolean fromNbt = false;

    /** failure: the element's capacitor failure (new Failure(element).capacitor()). */
    public PowerCapacitorCore(Spec spec, Container inventory, Failure failure) {
        this.spec = spec;
        this.inventory = inventory;
        watchdog.set(failure);
        positiveLoad.setAsMustBeFarFromInterSystem();
    }

    /** Adds its loads, components and processes to the element's lists, in the order the elements always had. */
    public void addTo(List<? super NbtElectricalLoad> electricalLoads, List<? super Component> electricalComponents,
                      List<? super IProcess> electricalProcesses, List<? super IProcess> slowProcesses) {
        electricalLoads.add(positiveLoad);
        electricalLoads.add(negativeLoad);
        electricalComponents.add(capacitor);
        electricalComponents.add(dischargeResistor);
        electricalProcesses.add(punkProcess);
        slowProcesses.add(watchdog);
    }

    private class PunkProcess implements IProcess {
        double eLeft = 0;
        double eLegaliseResistor;

        @Override
        public void process(double time) {
            if (eLeft <= 0) {
                eLeft = 0;
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                eLeft -= dischargeResistor.getP() * time;
                dischargeResistor.setR(eLegaliseResistor);
            }
        }
    }

    /** The element's initialize(), before it connects. */
    public void initialize() {
        ElnDeviceRegistry.applySmallRs(positiveLoad);
        ElnDeviceRegistry.applySmallRs(negativeLoad);

        setupPhysical();
    }

    /** C, the discharge resistor and the watchdog from the slots; run on initialize and on every inventory change. */
    public void setupPhysical() {
        double eOld = capacitor.getE();
        capacitor.setC(spec.getCValue(inventory));
        stdDischargeResistor = spec.dischargeTao() / capacitor.getC();

        watchdog.setUNominal(spec.getUNominalValue(inventory));
        punkProcess.eLegaliseResistor = Math.pow(spec.getUNominalValue(inventory), 2) / 400;

        if (fromNbt) {
            dischargeResistor.setR(stdDischargeResistor);
            fromNbt = false;
        } else {
            double deltaE = capacitor.getE() - eOld;
            punkProcess.eLeft += deltaE;
            if (deltaE < 0) {
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                dischargeResistor.setR(punkProcess.eLegaliseResistor);
            }
        }
    }

    /** After the element's super.writeToNBT. */
    public void writeToNBT(CompoundTag nbt) {
        nbt.putDouble("punkELeft", punkProcess.eLeft);
    }

    /** After the element's super.readFromNBT. */
    public void readFromNBT(CompoundTag nbt) {
        punkProcess.eLeft = nbt.getDoubleOr("punkELeft", 0.0);
        if (Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0;
        fromNbt = true;
    }

    /** The multimeter line: the current, with the voltage first if withVoltage. */
    public String multiMeterString(boolean withVoltage) {
        String current = Utils.plotAmpere("I", capacitor.getCurrent());
        return withVoltage ? Utils.plotVolt("U", Math.abs(capacitor.getU())) + current : current;
    }

    /** The Jade lines: capacity and charge, and the voltage and current in easy mode. */
    public Map<String, String> waila() {
        return mods.eln.compat.jade.ProbeLines.of()
            .headline(I18N.tr("Capacity"), Utils.plotValue(capacitor.getC(), "F"))
            .headline(I18N.tr("Charge"), Utils.plotEnergy("", capacitor.getE()))
            .detail(I18N.tr("Voltage drop"), Utils.plotVolt("", Math.abs(capacitor.getU())))
            .detail(I18N.tr("Current"), Utils.plotAmpere("", Math.abs(capacitor.getCurrent())));
    }
}
