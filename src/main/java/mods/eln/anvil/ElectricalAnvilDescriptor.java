package mods.eln.anvil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The electrical anvil (Baughn 2026-10-28): a workshop desk with an 800 V arc welder. It repairs a tool with its repair
 * material for no XP, never raising the tool's anvil penalty and stepping an old one down; it engraves a new name for a
 * small pulse. While it works it draws its full rating off the 800 V line, more than a small plant gives: it waits for
 * the voltage to come up and pauses when it sags, so a capacitor bank (or an inductor) carries the pulses. Late-midgame
 * gear. Model: mods.eln.cad.models.ElectricalAnvil.
 */
public class ElectricalAnvilDescriptor extends TransparentNodeDescriptor {
    /** Its rating: 800 V at the HV cable's nominal power (config [anvil] power). */
    public static double powerW = Eln.HVP();
    /** Energy for a unit of material's worth of repair (config [anvil] joulesPerUnit), and for engraving a name. */
    public static double joulesPerUnit = 40_000, joulesPerName = 2_000;
    /**
     * It starts a pulse at RESUME of 800 V, or, on a line that has levelled off (rising under PLATEAU_V_PER_S for
     * PLATEAU_S) above PLATEAU of 800 V, there; it pauses below SAG (config [anvil]). 98% was too high for a real bank
     * on a weak plant: the bank's own leak held the line at ~780 V and the desk waited forever (AdventurAgent).
     */
    public static double resume = 0.95, sag = 0.9, plateau = 0.93;
    public static final double PLATEAU_V_PER_S = 1, PLATEAU_S = 2;
    /**
     * A unit's weld comes in this many bursts, with at least burstGapS between them and the line back at RESUME
     * (Baughn: keep the 40 kJ, in two bursts, so a bank has a chance to recover). Config [anvil].
     */
    public static int bursts = 2;
    public static double burstGapS = 3;
    final Obj3D obj;

    public ElectricalAnvilDescriptor(String name, String model) {
        super(name, ElectricalAnvilElement.class, ElectricalAnvilRender.class);
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(mods.eln.registry.ElnDeviceRegistry.highVoltageCableDescriptor);
        cableTerminals = mods.eln.cable.CableTerminals.BACK;   // the 800 V plate low on the back
    }

    /** Energy in one burst. */
    public static double burstJoules() {
        return joulesPerUnit / Math.max(1, bursts);
    }

    /** The welder's resistance: its rating at 800 V. */
    public static double resistance() {
        return Eln.HVU * Eln.HVU / Math.max(1, powerW);
    }

    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
    }

    /** A point from the model's .txt (block units, centred, front XN): the tool's place or the torch tip. */
    float[] point(String prefix) {
        var p = obj == null ? null : obj.getPart("main");
        return p == null ? null : new float[]{p.getFloat(prefix + "X"), p.getFloat(prefix + "Y"), p.getFloat(prefix + "Z")};
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
        Collections.addAll(list, tr("Repairs a tool with its repair material, for no XP.\nIt never raises the tool's anvil cost,\nand lowers an old one a step each repair.\nTakes 800V at %1$ while it welds: feed it\nfrom a capacitor bank.",
            mods.eln.misc.Utils.plotPower(powerW)).split("\n"));
    }
}
