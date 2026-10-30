package mods.eln.transparentnode.smelter;

import mods.eln.Eln;
import mods.eln.misc.IFunction;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The smelter (Baughn 2026-10-23): a furnace heated through a thermal cable on its back, with a little 50 V for its
 * controls. It smelts as the electrical oven does at the same temperature (the oven's PfT), taking that heat from what the
 * cable brings; hoppers feed its top and empty its bottom. Model: mods.eln.cad.models.Smelter.
 */
public class SmelterDescriptor extends TransparentNodeDescriptor {
    public final IFunction PfT, thermalPlostfT;
    public final double thermalC;
    /** Its controls (W at 50 V), and the share of 50 V they need to run. */
    public static final double CONTROL_W = 20, CONTROL_MIN = 0.7;
    /** Over this (°C) it cracks (a thermal failure). */
    public static final double T_MAX = 1200;
    final Obj3D obj;

    public SmelterDescriptor(String name, IFunction PfT, IFunction thermalPlostfT, double thermalC, String model) {
        super(name, SmelterElement.class, SmelterRender.class);
        this.PfT = PfT;
        this.thermalPlostfT = thermalPlostfT;
        this.thermalC = thermalC;
        this.obj = Eln.obj.getObj(model);
    }

    public void applyTo(ThermalLoad load) {
        load.set(0.02, thermalPlostfT.getValue(0), thermalC);
    }

    /** Heat leaks to the room as the oven's (by temperature). */
    public void refreshTo(ThermalLoad load) {
        double t = Math.abs(load.Tc);
        double rp = t < 1 ? thermalPlostfT.getValue(0) : t / Math.max(1e-6, thermalPlostfT.getValue(t));
        load.setRp(Math.max(0.1, rp));
    }

    /** The model's body (the glowing firebox window is drawn by SmelterRender, unlit). */
    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        var g = obj.getPart("glow");
        if (g != null) g.draw();
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
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Smelts with heat from a thermal cable\non its back, as hot as the cable brings.\nHoppers feed the top and empty the bottom.\nNeeds a little 50V for its controls.").split("\n"));
    }
}
