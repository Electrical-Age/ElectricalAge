package mods.eln.powerprimitives;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Utils;
import mods.eln.misc.series.ISerie;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Inductor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Power Inductor's circuit and state, shared by its six-node (sixnode/powerinductorsix) and transparent-node
 * (transparentnode/powerinductor) elements: the inductor between the two loads, L from the copper cables in its slot
 * and the windings' Rs (on both loads) from the core in the other. The element maps its pins to the loads and owns the
 * inventory. Saved: the loads and the inductor in the element's lists (keys positiveLoad, negativeLoad, inductor).
 */
public final class PowerInductorCore {
    /** The descriptor's ratings: L and Rs from the slots (PowerInductorContainer). */
    public interface Spec {
        ISerie inductanceSerie();

        default double getlValue(int cableCount) {
            if (cableCount == 0) return 0;
            return inductanceSerie().getValue(cableCount - 1);
        }

        default double getlValue(Container inventory) {
            ItemStack core = inventory.getItem(PowerInductorContainer.cableId);
            if (Utils.isEmpty(core))
                return getlValue(0);
            else
                return getlValue(core.getCount());
        }

        default double getRsValue(Container inventory) {
            ItemStack core = inventory.getItem(PowerInductorContainer.coreId);

            if (Utils.isEmpty(core)) return MnaConst.highImpedance;
            FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

            double coreFactor = coreDescriptor.cableMultiplicator;

            return ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs * coreFactor;
        }
    }

    public final NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    public final NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

    public final Inductor inductor = new Inductor("inductor", positiveLoad, negativeLoad);

    private final Spec spec;
    private final Container inventory;
    private boolean fromNbt = false;

    public PowerInductorCore(Spec spec, Container inventory) {
        this.spec = spec;
        this.inventory = inventory;
        positiveLoad.setAsMustBeFarFromInterSystem();
    }

    /** Adds its loads and the inductor to the element's lists, in the order the elements always had. */
    public void addTo(List<? super NbtElectricalLoad> electricalLoads, List<? super Component> electricalComponents) {
        electricalLoads.add(positiveLoad);
        electricalLoads.add(negativeLoad);
        electricalComponents.add(inductor);
    }

    /**
     * L and Rs from the slots; run on initialize and on every inventory change. A change empties the inductor's
     * current; the first setup after a load keeps the saved one.
     */
    public void setupPhysical() {
        double rs = spec.getRsValue(inventory);
        inductor.setL(spec.getlValue(inventory));
        positiveLoad.setRs(rs);
        negativeLoad.setRs(rs);

        if (fromNbt) {
            fromNbt = false;
        } else {
            inductor.resetStates();
        }
    }

    /** After the element's super.readFromNBT (the state itself is in the lists). */
    public void readFromNBT() {
        fromNbt = true;
    }

    /** The multimeter line: the current, with the voltage first if withVoltage. */
    public String multiMeterString(boolean withVoltage) {
        String current = Utils.plotAmpere("I", inductor.getCurrent());
        return withVoltage ? Utils.plotVolt("U", Math.abs(inductor.getU())) + current : current;
    }

    /** The Jade lines: inductance and stored energy, and the voltage and current in easy mode. */
    public Map<String, String> waila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Inductance"), Utils.plotValue(inductor.getL(), "H"));
        info.put(I18N.tr("Charge"), Utils.plotEnergy("", inductor.getE()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Voltage drop"), Utils.plotVolt("", Math.abs(inductor.getU())));
            info.put(I18N.tr("Current"), Utils.plotAmpere("", Math.abs(inductor.getCurrent())));
        }
        return info;
    }
}
