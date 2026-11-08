package mods.eln.transparentnode.resincollector;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.element.Machines;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Industrial Resin Collector (Baughn 2026-11-07, the rubber chain): a powered tapping rig set against a tree trunk
 * by its back. It taps the trunk ten times as fast as a hand Tree Resin Collector (per log, by the same tree factors:
 * a rubber tree three times any other), as latex, into its tank; its OUT flange on top pushes the latex into an EA
 * pipe, or into whatever takes fluid above it. 230 V on its two side faces. Model mods.eln.cad.models.ResinCollector.
 */
public class IndustrialResinCollectorDescriptor extends TransparentNodeDescriptor {
    public final double nominalU, nominalP;
    final Obj3D obj;
    /** Its tank (mB), and its outlet's rate and head, as the oil devices'. */
    public static final int TANK_MB = 4000;
    public static final double OUT_MB_PER_S = 200, OUT_HEAD_PA = 6e5;
    /** How much faster than the hand collector it taps (Baughn: ~10x). */
    public static final double SPEED_UP = 10;
    /** The most logs of a trunk it counts (Baughn 2026-11-07: a rubber fig's whole trunk made far too much). */
    public static final int MAX_LOGS = 6;
    /** The OUT flange's height (px, top face centre). */
    public static final int FLANGE_Y = 16;

    public IndustrialResinCollectorDescriptor(String name, String model, double nominalU, double nominalP, ElectricalCableDescriptor cable) {
        super(name, IndustrialResinCollectorElement.class, IndustrialResinCollectorRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;
    }

    /** Its machine: a consumer at nominalU drawing nominalP from its side faces while it taps. */
    public MachineSpec spec() {
        return MachineSpec.consumer(nominalU).ratedWatts(nominalP).onBrownout(Brownout.SLOW)
            .burnsOnOvervoltage(Machines.BURNS_OUT).power(RelativeFace.LEFT, RelativeFace.RIGHT).build();
    }

    void draw(boolean powered, boolean running) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        mods.eln.anim.Led.draw(obj.getPart("ledpower"), powered);
        mods.eln.anim.Led.draw(obj.getPart("ledrun"), running);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(false, false);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Set it against a tree trunk by its back.\nTaps latex ten times as fast as a\nTree Resin Collector; a rubber tree\ngives three times as much.\nLatex out of the flange on top.\nNominal power: %1$W",
            mods.eln.misc.Utils.plotValue(nominalP)).split("\n"));
    }
}
