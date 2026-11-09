package mods.eln.transparentnode.polymeriser;

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
 * The Polymeriser (the rubber chain's oil route, 2026-11-07): a stirred reactor that makes the Steam Cracker's butadiene
 * into Raw Rubber (the same item latex gives; it vulcanises the same). Butadiene comes in at the blue flange on its back,
 * as liquid or vapour; Soul Dust is the initiator (Baughn: macerated soul sand instead of the real activator), one for
 * every 8 Raw Rubber, fed by a hopper or pipe from above or the sides, or by hand. The Raw Rubber goes into a hopper
 * under it, or a right-click takes it. 230 V on its side faces, 1 kW: 20 mB of butadiene and 5 kJ a Raw Rubber, so it
 * keeps up with one Steam Cracker. Model mods.eln.cad.models.Polymeriser.
 */
public class PolymeriserDescriptor extends TransparentNodeDescriptor {
    public final double nominalU, nominalP;
    final Obj3D obj;
    /** Butadiene (liquid mB, or its mass as vapour) and energy a Raw Rubber; Raw Rubber a Soul Dust. */
    public static final double MB_PER_RUBBER = 20, JOULES_PER_RUBBER = 5000;
    public static final int RUBBER_PER_SOUL_DUST = 8, TANK_MB = 2000;
    public static final double PORT_MB_PER_S = 200;
    /** Its flange (px, model frame): on the back, 4 px up, centred. */
    public static final int FLANGE_Y = 4, FLANGE_Z = 8;

    public PolymeriserDescriptor(String name, String model, double nominalU, double nominalP, ElectricalCableDescriptor cable) {
        super(name, PolymeriserElement.class, PolymeriserRender.class);
        this.nominalU = nominalU;
        this.nominalP = nominalP;
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(cable);
        cableTerminals = mods.eln.cable.CableTerminals.FLANKS;
    }

    public MachineSpec spec() {
        return MachineSpec.consumer(nominalU).ratedWatts(nominalP).onBrownout(Brownout.SLOW)
            .burnsOnOvervoltage(Machines.BURNS_OUT).power(RelativeFace.LEFT, RelativeFace.RIGHT).build();
    }

    void draw(boolean powered, boolean running, float lever, float stirDeg) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        var s = obj.getPart("stirrer");   // (the agitator's drive turns while it runs, about its origin)
        if (s != null) s.draw(stirDeg, 0, 1, 0);
        mods.eln.anim.Led.draw(obj.getPart("ledpower"), powered);
        mods.eln.anim.Led.draw(obj.getPart("ledrun"), running);
        mods.eln.node.transparent.MachineSwitch.drawToggle(obj.getPart("toggle"), lever);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    /** The on/off lever (MachineSwitch), or null. */
    public mods.eln.misc.Obj3D.Obj3DPart toggle() {
        return obj == null ? null : obj.getPart("toggle");
    }

    @Override
    public void drawItem(boolean entity) {
        draw(false, false, 1, 0);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Makes butadiene into Raw Rubber.\nButadiene into the blue flange on its back;\nSoul Dust from above or the sides\n(one for every 8 Raw Rubber).\nRaw Rubber into a hopper under it.\nNominal power: %1$W",
            mods.eln.misc.Utils.plotValue(nominalP)).split("\n"));
    }
}
