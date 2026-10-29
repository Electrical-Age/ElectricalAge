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
 * is a quarter slice per block. Ports on its two end faces: A at their centres, B up high (its top row gives a steam
 * raiser's steam). An economizer (gas exhaust to feedwater), a reheater between the HP and LP turbines, a steam raiser
 * for a combined cycle; separate jobs are separate exchangers piped in series. Model: tools/models/heatexchanger.py.
 */
public class HeatExchangerDescriptor extends TransparentNodeDescriptor {
    /** A 2 x 2 slice's UA (W/K; Baughn 2026-10-29). */
    public static final double SLICE_UA = 50;
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
        list.add(String.format("Moves heat between two pipe streams kept apart: %.0f W per 1 K for each 2 x 2 slice.", 4 * ua));
        list.add("Stack 2 x 2 across and up to 8 long into one exchanger; a lone block is a quarter slice.");
        list.add("Ports on its two ends: stream A at their centres, B up high. For counterflow,");
        list.add("A in at the back end and out at the front; B in at the front and out at the back.");
        list.add("Either side may condense steam or boil water: an economizer, a reheater, a steam raiser.");
    }
}
