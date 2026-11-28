package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 26.x (notes/oil/MACHINES-PLAN.md section 4): the fractionating column. The base takes crude (pipe or bucket) and heat
 * on its sides (an electric heater with a regulator, or a fuel heat furnace); above {@link #DISTIL_C} the heat above it
 * distils crude at {@link #J_PER_MB} per mB. Segments stacked on the base collect the cuts, top to bottom; the residue
 * stays in the base. With fewer than four segments the missing cuts fold into the next heavier one. Placeholder models.
 */
public class ColumnDescriptor extends TransparentNodeDescriptor implements SightGlassRender.Glazed {
    /** Crude to ~350 C and boiled off: ~1 MJ/kg x 0.85 kg/L x 6.75e-5 L/mB (litresPerMilliBucket). */
    public static final double J_PER_MB = 60, DISTIL_C = 350;
    /** On EA pipes (its nozzles; Baughn 2026-11-03): what moves through a port each second (mB) and the head it pushes against (Pa). */
    public static final double PIPE_MB_PER_S = 200, PIPE_HEAD_PA = 6e5;
    public static final int CRUDE_MB = 8000, RESIDUE_MB = 4000, SEGMENT_MB = 4000, MAX_SEGMENTS = 4;

    final boolean base;
    final Obj3D obj;

    public ColumnDescriptor(String name, boolean base, String model) {
        // (the base has a level gauge; a segment is a static model)
        super(name, base ? ColumnBaseElement.class : ColumnSegmentElement.class, base ? SightGlassRender.class : mods.eln.node.transparent.FrontDrawnRender.class);
        this.base = base;
        this.obj = Eln.obj.getObj(model);
    }

    /** Segments sit on the base or on another segment, not on a solid floor. */
    @Override
    public boolean mustHaveFloor() {
        return base;
    }

    @Override
    public void draw() {
        var main = obj == null ? null : obj.getPart("main");
        if (main != null) main.draw();
    }

    /**
     * The liquid in the base's level gauge (Lashtear 2026-11-28, Baughn: do it properly): residue under crude, of the
     * base's two tanks together. A segment has no gauge (its model no "level" part).
     */
    @Override
    public void drawLevel(SightGlass glass) {
        glass.draw(obj);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        if (base) {
            list.add("Distils crude oil above 350 °C (residue remains in the base).");
            list.add("Stack 1–4 column segments on top to collect cuts.");
        } else {
            list.add("Fractionating column stage (stack 1–4 on a base).");
            mods.eln.client.ItemTooltips.detail(list,
                "4 stages, top to bottom: natural gas, gasoline, diesel, heavy oil (fewer stages fold lighter cuts down).");
        }
    }
}
