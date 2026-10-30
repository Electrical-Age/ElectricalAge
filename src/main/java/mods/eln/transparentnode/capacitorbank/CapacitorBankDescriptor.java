package mods.eln.transparentnode.capacitorbank;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The capacitor bank (Baughn 2026-10-28): six capacitor cans in two layers of three, one large capacitor inside, for
 * peak loads like the electrical anvil's welding bursts. Sized so one bank carries one 20 kJ burst from 800 V down to
 * the anvil's 720 V: C >= 20 kJ / (0.5 (800^2 - 720^2)) = 0.33 F; it has 0.36 F. Rated 1000 V: it pops above 1.3 times
 * that, as the Power Capacitor does. It leaks (an RC time constant of half an hour), so it's no bulk store: lead-acid
 * batteries stay the way to keep energy (Baughn). Polar: + on the left of the front, - on the right (the model's
 * coloured plates). Model: mods.eln.cad.models.CapacitorBank.
 */
public class CapacitorBankDescriptor extends TransparentNodeDescriptor {
    /** Its capacitance (F), rating (V) and leakage time constant R C (s); config [capacitorBank]. */
    public static double farads = 0.36, ratedVolts = 1000, leakTauS = 1800;
    final Obj3D obj;

    public CapacitorBankDescriptor(String name, String model) {
        super(name, CapacitorBankElement.class, CapacitorBankRender.class);
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(mods.eln.registry.ElnDeviceRegistry.highVoltageCableDescriptor);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;
    }

    /** Energy it holds at this voltage (J). */
    public static double energyAt(double u) {
        return 0.5 * farads * u * u;
    }

    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("%1$F, rated %2$V: %3$ at 800V.\n+ on the left of its front, - on the right.\nEmpty, it draws a huge inrush: charge it\nthrough a resistor or from a soft plant.\nFor bursts, not for keeping: it leaks\nmost of its charge within the hour.",
            String.format("%.2f", farads), String.format("%.0f", ratedVolts), Utils.plotEnergy(energyAt(800))).split("\n"));
    }
}
