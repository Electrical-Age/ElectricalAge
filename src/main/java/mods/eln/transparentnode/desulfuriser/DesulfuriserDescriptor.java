package mods.eln.transparentnode.desulfuriser;

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
 * The Desulfuriser (the rubber chain's oil route, 2026-11-07): a heated vessel in line on a refinery cut's pipe. The cut
 * comes in at the blue IN flange on its back and leaves unchanged by the red OUT flange on its front, up to 50 mB/s at
 * its rated 1 kW (it holds no more than its small tanks); every 2000 mB it treats leaves a Sulfur Dust (real refineries'
 * sulfur comes from hydrodesulfurisation), which it pushes into a hopper under it or a right-click takes. 230 V on its
 * side faces. Model mods.eln.cad.models.Desulfuriser.
 */
public class DesulfuriserDescriptor extends TransparentNodeDescriptor {
    public final double nominalU, nominalP;
    final Obj3D obj;
    public static final int TANK_MB = 1000;
    public static final double TREAT_MB_PER_S = 50, PORT_MB_PER_S = 200, OUT_HEAD_PA = 6e5;
    public static final int MB_PER_SULFUR = 2000, MAX_SULFUR = 64;
    /** Its flanges (px, model frame): IN on the back, OUT on the front, both 4 px up, centred. */
    public static final int FLANGE_Y = 4, FLANGE_Z = 8;

    public DesulfuriserDescriptor(String name, String model, double nominalU, double nominalP, ElectricalCableDescriptor cable) {
        super(name, DesulfuriserElement.class, DesulfuriserRender.class);
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

    void draw(boolean powered, boolean running, float lever) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
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
        draw(false, false, 1);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("In line on a refinery cut's pipe:\nin at the blue flange on its back,\nout at the red one on its front.\nA Sulfur Dust every 2000 mB treated,\ninto a hopper under it.\nNominal power: %1$W",
            mods.eln.misc.Utils.plotValue(nominalP)).split("\n"));
    }
}
