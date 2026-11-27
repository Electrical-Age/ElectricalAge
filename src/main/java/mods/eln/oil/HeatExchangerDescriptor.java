package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Counterflow Heat Exchanger (Baughn 2026-10-29, the shaft redo): two pipe streams kept apart, heat from the hotter
 * to the colder through UA (W/K), by effectiveness-NTU for counterflow when both flow (as a shell and a stagnant stream
 * when one doesn't). Either side may condense or boil. Blocks stack into one exchanger (HeatExchangerGroup): 2 x 2 across,
 * up to 8 long, SLICE_UA per 2 x 2 slice (Baughn 2026-10-29: 50 W/K, 400 at the most); a lone block or any other shape
 * is a quarter slice per block. Each side holds 1 B a block (SIDE_VOLUME). Ports on its two end faces: A at their centres, B up high (its top row gives a steam
 * raiser's steam). An economizer (gas exhaust to feedwater), a reheater between the HP and LP turbines, a steam raiser
 * for a combined cycle; separate jobs are separate exchangers piped in series. Model: mods.eln.cad.models.HeatExchanger.
 */
public class HeatExchangerDescriptor extends TransparentNodeDescriptor {
    /** A 2 x 2 slice's UA (W/K; Baughn 2026-10-29). */
    public static final double SLICE_UA = 50;
    /**
     * Each side's volume in a block (m^3): 1 B of water (Baughn 2026-11-15: "1B a side seems sensible"; it was 8 L, 114 B,
     * 14 x a boiler, and an economizer soaked up minutes of feed). UA is the tubes' area, not their volume: it stays.
     */
    public static final double SIDE_VOLUME = 1000 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / mods.eln.sim.fluid.Water.RHO_L;
    /** What a side held a block before (m^3; saves without a volume key): HeatExchangerElement spills the excess. */
    public static final double OLD_SIDE_VOLUME = 0.008;
    /** A block's UA (W/K) and each side's volume in it (m^3). */
    public final double ua, sideVolume;
    final Obj3D obj;

    public HeatExchangerDescriptor(String name, double ua, double sideVolume, String model) {
        super(name, HeatExchangerElement.class, HeatExchangerRender.class);
        this.ua = ua;
        this.sideVolume = sideVolume;
        this.obj = Eln.obj.getObj(model);
    }

    void drawPart(String name) {
        var p = obj == null ? null : obj.getPart(name);
        if (p != null) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Transfers heat between two isolated pipe streams (%.0f W/K per 2×2 slice).", 4 * ua));
        list.add(String.format("Multiblock: up to 2×2×8 (%.0f B per side per block).",
            sideVolume * mods.eln.sim.fluid.Water.RHO_L / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / 1000));
        mods.eln.client.ItemTooltips.detail(list,
            "Both streams must flow; either side may boil or condense.");
    }
}
