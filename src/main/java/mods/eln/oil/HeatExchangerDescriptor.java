package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Counterflow Heat Exchanger (Baughn 2026-10-29, the shaft redo): two pipe streams kept apart, heat from the hotter
 * to the colder through UA (W/K), by effectiveness-NTU for counterflow when both flow (as a shell and a stagnant stream
 * when one doesn't). Either side may condense or boil. Side A in on its left, out on its right; side B in at its back,
 * out at its front (from its top half: a steam raiser gives steam). An economizer (gas exhaust to feedwater), a reheater
 * between the HP and LP turbines, a steam raiser for a combined cycle. Model: the Steam Condenser's until its own.
 */
public class HeatExchangerDescriptor extends TransparentNodeDescriptor {
    public final double ua, sideVolume;
    final Obj3D obj;

    public HeatExchangerDescriptor(String name, double ua, double sideVolume, String model) {
        super(name, HeatExchangerElement.class, HeatExchangerRender.class);
        this.ua = ua;
        this.sideVolume = sideVolume;
        this.obj = Eln.obj.getObj(model);
    }

    void draw() {
        var p = obj == null ? null : obj.getPart("main");
        if (p != null) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Moves heat between two pipe streams kept apart: %.1f kW per 1 K between them.", ua / 1000));
        list.add("Stream A: in on its left, out on its right. Stream B: in at its back, out at its front.");
        list.add("Either side may condense steam or boil water: an economizer, a reheater, a steam raiser.");
    }
}
