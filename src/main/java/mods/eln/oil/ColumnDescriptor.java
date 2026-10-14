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
public class ColumnDescriptor extends TransparentNodeDescriptor {
    /** Crude to ~350 C and boiled off: ~1 MJ/kg x 0.85 kg/L x 6.75e-5 L/mB (litresPerMilliBucket). */
    public static final double J_PER_MB = 60, DISTIL_C = 350;
    public static final int CRUDE_MB = 8000, RESIDUE_MB = 4000, SEGMENT_MB = 4000, MAX_SEGMENTS = 4;

    final boolean base;
    final Obj3D obj;

    public ColumnDescriptor(String name, boolean base, String model) {
        super(name, base ? ColumnBaseElement.class : ColumnSegmentElement.class, ColumnRender.class);
        this.base = base;
        this.obj = Eln.obj.getObj(model);
    }

    /** Segments sit on the base or on another segment, not on a solid floor. */
    @Override
    public boolean mustHaveFloor() {
        return base;
    }

    void draw() {
        if (obj != null) for (var p : obj.parts()) p.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        if (base) {
            list.add("Crude in, heat in on a side; distils above 350 °C");
            list.add("Stack 1-4 column segments on top for the cuts; residue stays here");
        } else {
            list.add("Stack on a fractionating column base. 4 segments, top to bottom:");
            list.add("natural gas, gasoline, diesel, heavy oil (fewer: the lighter cuts fold down)");
        }
    }
}
