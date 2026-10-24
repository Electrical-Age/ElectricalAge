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
 * Which ores a scanner shows (Baughn 2026-10-11): each of the 4 filter slots holds an ore block or what an ore drops.
 * Everything goes by item tags, so other mods' ores work too (Baughn 2026-10-24: tags, not special cases). The ore
 * materials are the suffixes of the `c:ores/*` and `eln:ores/*` tags (EA's tungsten is EA-private); a block in the world
 * matches a slot when its item carries one of the slot's. An item's materials are the suffixes of its `ores/`,
 * `raw_materials/`, `gems/`, `dusts/`, `nuggets/` and `ore_drops/` tags in those namespaces that name an ore material
 * (iron ore, deepslate iron ore and raw iron give "iron"; lapis, redstone dust and nether gold ore's nuggets work; coal
 * has no convention tag, so EA tags it `eln:ore_drops/coal`).
 */
public final class XRayFilter {
    private XRayFilter() {
    }

    static final List<String> NAMESPACES = List.of("c", "eln");
    static final List<String> DROP_KINDS = List.of("ores/", "raw_materials/", "gems/", "dusts/", "nuggets/", "ore_drops/");

    public static Set<String> materials(ItemStack stack) {
        Set<String> out = new HashSet<>();
        if (stack == null || stack.isEmpty()) return out;
        Set<String> known = ores().keySet();
        stack.getItem().builtInRegistryHolder().tags().forEach(t -> {
            String m = suffix(t, DROP_KINDS);
            if (m != null && known.contains(m)) out.add(m);
        });
        return out;
    }

    /** The material of tag t when it is `<ns>:<kind><material>` for one of kinds, else null. */
    static String suffix(TagKey<Item> t, List<String> kinds) {
        if (!NAMESPACES.contains(t.location().getNamespace())) return null;
        String p = t.location().getPath();
        for (String k : kinds)
            if (p.startsWith(k) && p.length() > k.length()) return p.substring(k.length());
        return null;
    }

    /** A filter slot takes it: an ore block or an ore's drop. */
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
                    String mat = suffix(t, List.of("ores/"));
                    if (mat != null) m.computeIfAbsent(mat, k -> new HashSet<>()).add(b);
                });
            }
            ores = m;
        }
        return ores;
    }

    /** Every ore block (an item in some `c:ores/*` or `eln:ores/*` tag). */
    public static Set<Block> allOres() {
        Set<Block> out = new HashSet<>();
        ores().values().forEach(out::addAll);
        return out;
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
