package mods.eln.item.xray;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Which ores a scanner shows (Baughn 2026-10-11): each of the 4 filter slots holds an ore block or a raw chunk; its
 * materials are the suffixes of its `c:ores/*` and `c:raw_materials/*` item tags (iron ore, deepslate iron ore and
 * raw iron all give "iron"). A block in the world matches a slot when its item is tagged `c:ores/<material>`.
 */
public final class XRayFilter {
    private XRayFilter() {
    }

    public static Set<String> materials(ItemStack stack) {
        Set<String> out = new HashSet<>();
        if (stack == null || stack.isEmpty()) return out;
        stack.getItem().builtInRegistryHolder().tags().forEach(t -> material(t, out));
        return out;
    }

    static void material(TagKey<Item> t, Set<String> out) {
        if (!t.location().getNamespace().equals("c")) return;
        String p = t.location().getPath();
        if (p.startsWith("ores/")) out.add(p.substring(5));
        else if (p.startsWith("raw_materials/")) out.add(p.substring(14));
    }

    /** A filter slot takes it: an ore block or raw chunk of some material. */
    public static boolean accepts(ItemStack stack) {
        return !materials(stack).isEmpty();
    }

    /** Ore blocks per material (from the block's item tags), built once per tag reload. */
    private static Map<String, Set<Block>> ores;

    public static synchronized void invalidate() {
        ores = null;
    }

    static synchronized Map<String, Set<Block>> ores() {
        if (ores == null) {
            Map<String, Set<Block>> m = new HashMap<>();
            for (Block b : BuiltInRegistries.BLOCK) {
                Item i = b.asItem();
                if (i == net.minecraft.world.item.Items.AIR) continue;
                i.builtInRegistryHolder().tags().forEach(t -> {
                    if (t.location().getNamespace().equals("c") && t.location().getPath().startsWith("ores/"))
                        m.computeIfAbsent(t.location().getPath().substring(5), k -> new HashSet<>()).add(b);
                });
            }
            ores = m;
        }
        return ores;
    }

    /** Block -> filter slot (first slot wins) for these filter stacks; blocks not in the map match no slot. */
    public static Map<Block, Integer> blockSlots(List<ItemStack> filters) {
        Map<Block, Integer> out = new HashMap<>();
        Map<String, Set<Block>> all = ores();
        for (int s = 0; s < filters.size(); s++)
            for (String mat : materials(filters.get(s)))
                for (Block b : all.getOrDefault(mat, Set.of())) out.putIfAbsent(b, s);
        return out;
    }

    /** The 4 filters stored in a scanner stack (DataComponents.CONTAINER), empty stacks for empty slots. */
    public static List<ItemStack> filters(ItemStack scanner) {
        List<ItemStack> out = new ArrayList<>();
        var c = scanner.get(net.minecraft.core.component.DataComponents.CONTAINER);
        for (int i = 0; i < 4; i++) out.add(ItemStack.EMPTY);
        if (c != null) {
            var list = net.minecraft.core.NonNullList.withSize(4, ItemStack.EMPTY);
            c.copyInto(list);
            for (int i = 0; i < 4; i++) out.set(i, list.get(i));
        }
        return out;
    }

    public static void setFilters(ItemStack scanner, List<ItemStack> filters) {
        scanner.set(net.minecraft.core.component.DataComponents.CONTAINER,
            net.minecraft.world.item.component.ItemContainerContents.fromItems(filters));
    }
}
