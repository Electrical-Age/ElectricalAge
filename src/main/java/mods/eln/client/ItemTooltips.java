package mods.eln.client;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.IDescriptorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Item tooltips: the descriptor's 1.12 addInformation lines under the item name (1.12 GenericItemUsingDamage and
 * GenericItemBlockUsingDamage.addInformation). Lines may hold "\n" (1.12 split some with tr(...).split("\n")).
 */
public final class ItemTooltips {
    private ItemTooltips() {
    }

    /** Descriptors whose addInformation threw once: logged once, then skipped. */
    private static final Set<Object> broken = new HashSet<>();

    public static void onTooltip(ItemTooltipEvent e) {
        ItemStack stack = e.getItemStack();
        if (stack.getItem() instanceof mods.eln.node.voxel.PipeSensorItem ps) {
            for (String l : ps.tooltip()) e.getToolTip().add(Component.literal(l));
            return;
        }
        if (stack.getItem() instanceof mods.eln.node.voxel.ValveItem v && v.tooltip() != null) {
            for (String l : v.tooltip()) e.getToolTip().add(Component.literal(l));
            return;
        }
        if (!(stack.getItem() instanceof IDescriptorItem di)) return;
        Object d = di.getEaDescriptor();
        if (d == null || broken.contains(d)) return;
        List<String> lines = new ArrayList<>();
        boolean advanced = e.getFlags().isAdvanced();
        try {
            if (d instanceof GenericItemUsingDamageDescriptor g) g.addInformation(stack, e.getEntity(), lines, advanced);
            else if (d instanceof GenericItemBlockUsingDamageDescriptor b) b.addInformation(stack, e.getEntity(), lines, advanced);
        } catch (RuntimeException ex) {
            broken.add(d);
            Eln.LOG.warn("tooltip of {} failed", d, ex);
            return;
        }
        for (String l : lines) for (String part : l.split("\n")) e.getToolTip().add(Component.literal(part));
    }
}
