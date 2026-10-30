package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The steam oven (Baughn 2026-10-23): steam from a pipe on top, a 10 W fan on 50 V, and food in. All its heat is the
 * steam's: it cooks at up to twice the electrical oven's best food speed (the oven just under charring, PfT ~140 W:
 * here MAX_COOK_W), but only EFFICIENCY of the steam's heat reaches the food; the rest goes out of its vent. Model:
 * mods.eln.cad.models.SteamOven.
 */
public class SteamOvenDescriptor extends TransparentNodeDescriptor {
    public static final double MAX_COOK_W = 280, EFFICIENCY = 0.25, FAN_W = 10, FAN_MIN = 0.7;
    final Obj3D obj;

    public SteamOvenDescriptor(String name, String model) {
        super(name, SteamOvenElement.class, SteamOvenRender.class);
        obj = Eln.obj.getObj(model);
    }

    /** Steam (kg/s) it condenses at full speed. */
    public static double maxSteamKgPerS() {
        return MAX_COOK_W / EFFICIENCY / mods.eln.sim.fluid.Water.L_TB;
    }

    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
    }

    /** The vent's outlet (model coordinates, front XN), from the model's .txt. */
    float[] vent() {
        var p = obj == null ? null : obj.getPart("main");
        return p == null ? null : new float[]{p.getFloat("ventX"), p.getFloat("ventY"), p.getFloat("ventZ")};
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
        Collections.addAll(list, tr("Cooks food with steam from a pipe on top:\nup to twice as fast as the electrical oven,\nbut it vents three quarters of the heat.\nNeeds a little 50V for its fan.").split("\n"));
    }
}
