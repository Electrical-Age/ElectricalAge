package mods.eln.transparentnode.wiredrawing;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.element.Machines;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.voxel.CableSpec;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * A wire-drawing machine (Baughn 2026-11-06, Wires stage 4): draws copper into wire and sheathes it in rubber, making a
 * cable kit or a transformer winding to any spec within its limits (the 48 V machine up to 6 mm² and 230 V insulation,
 * the 230 V machine everything). It is about 50% more efficient than the hand recipes (Baughn): the copper of a preset
 * cable costs two thirds of its hand recipe's, the rubber two thirds per insulation layer, scaled by the wire's girth.
 * Power on the back. Model: mods.eln.cad.models.WireDrawer.
 */
public class WireDrawingDescriptor extends TransparentNodeDescriptor {
    /** Its supply and rating. */
    public final double nominalU, nominalP;
    /** The thickest wire (CableSpec.MM2 index) and the best insulation (power tier) it makes. */
    public final int maxMm2, maxTier;
    /** The colour its screen starts on (Baughn: brown on the 48 V machine, black on the 230 V one). */
    public final int defaultColour;
    final Obj3D obj;

    /** Energy to draw a block of 2.5 mm² wire, J (more for thicker wire, by its girth). */
    public static final double JOULES_PER_BLOCK = 1000;
    /** Longest kit, blocks (a kit holds 1024 voxels), and most turns of a winding. */
    public static final int MAX_BLOCKS = 64;

    public WireDrawingDescriptor(String name, String model, double nominalU, double nominalP, ElectricalCableDescriptor cable,
                                 int maxMm2, int maxTier, int defaultColour) {
        super(name, WireDrawingElement.class, WireDrawingRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        this.maxMm2 = maxMm2;
        this.maxTier = maxTier;
        this.defaultColour = defaultColour;
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.BACK;
    }

    /** Its machine: a consumer at nominalU drawing nominalP from the back face, slower on a sagging line. */
    public MachineSpec spec() {
        return MachineSpec.consumer(nominalU).ratedWatts(nominalP).onBrownout(Brownout.SLOW)
            .burnsOnOvervoltage(Machines.BURNS_OUT).power(RelativeFace.BACK).build();
    }

    public boolean allows(int mm2, int tier) {
        return mm2 >= 0 && mm2 <= maxMm2 && tier >= 1 && tier <= maxTier;
    }

    // ---- costs (per block of wire; a winding's turn is WindingItem.BLOCKS_PER_TURN of a block)

    static double girth(int mm2) {
        return Math.sqrt(CableSpec.MM2[mm2] / 2.5);
    }

    /** Copper ingots a block (hand: half an ingot a block of 2.5 mm²; two thirds of that, by cross-section). */
    public static double copperPerBlock(int mm2) {
        return 0.5 * 2 / 3 * CableSpec.MM2[mm2] / 2.5;
    }

    /** Rubber a block: two thirds of one a layer (hand: one a layer) x the insulation's thickness x the girth. */
    public static double rubberPerBlock(int tier, int mm2) {
        double layers = switch (tier) {
            case 1 -> 1;
            case 2 -> 1.5;
            case 3 -> 2;
            default -> 4;
        };
        return 2.0 / 3 * layers * girth(mm2);
    }

    public static double energyPerBlock(int mm2) {
        return JOULES_PER_BLOCK * girth(mm2);
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
        String volts = CableSpec.volts(maxTier) >= 1000 ? (int) (CableSpec.volts(maxTier) / 1000) + " kV" : (int) CableSpec.volts(maxTier) + " V";
        Collections.addAll(list, tr("Draws copper and rubber into cable kits\nand transformer windings to any spec:\nup to %1$ mm², %2$ insulation.\nNominal power: %3$W",
            fmt(CableSpec.MM2[maxMm2]), volts, mods.eln.misc.Utils.plotValue(nominalP)).split("\n"));
    }

    static String fmt(double m) {
        return m == Math.floor(m) ? String.valueOf((int) m) : String.valueOf(m);
    }
}
