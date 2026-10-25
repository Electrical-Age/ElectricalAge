package mods.eln.misc;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The macerator on an ore block with no recipe of its own: what the block drops to a Fortune II pickaxe, from its loot
 * table, rolled for every operation (Baughn: the macerator acts as a Fortune II pick on all ore blocks). Any block in
 * c:ores whose drops aren't the block itself (ancient debris, an ore that only drops itself, gets no recipe).
 */
public class OreLootRecipe extends Recipe {
    public static final TagKey<Item> ORES = TagKey.create(Registries.ITEM, net.minecraft.resources.Identifier.parse("c:ores"));
    /** The energy of an operation (J): as an iron ore block's. */
    public static final double ENERGY = 6000;
    private static final int SAMPLES = 64;

    private final BlockItem block;
    private ItemStack[] max;

    private OreLootRecipe(BlockItem block, ItemStack[] max) {
        super(new ItemStack(block), max, ENERGY);
        this.block = block;
        this.max = max;
    }

    /** Without a server (a client): an ore block, which the server may well take. */
    public static boolean maybe(ItemStack stack) {
        return level() == null && !Utils.isEmpty(stack) && stack.is(ORES) && stack.getItem() instanceof BlockItem;
    }

    /** Per ore block item, once there is a level (empty: no recipe). */
    private static final Map<Item, java.util.Optional<OreLootRecipe>> CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The recipe for an ore block item, or null (not an ore block, no level yet, or it drops itself). */
    public static OreLootRecipe of(ItemStack stack) {
        if (Utils.isEmpty(stack) || !stack.is(ORES) || !(stack.getItem() instanceof BlockItem bi)) return null;
        ServerLevel level = level();
        if (level == null) return null;
        return CACHE.computeIfAbsent(bi, k -> java.util.Optional.ofNullable(make(level, bi))).orElse(null);
    }

    private static OreLootRecipe make(ServerLevel level, BlockItem bi) {
        // the most of each item in many rolls: the output space the machine must have
        Map<Item, Integer> most = new LinkedHashMap<>();
        for (int k = 0; k < SAMPLES; k++) {
            Map<Item, Integer> one = new LinkedHashMap<>();
            for (ItemStack s : roll(level, bi)) one.merge(s.getItem(), s.getCount(), Integer::sum);
            for (var e : one.entrySet()) most.merge(e.getKey(), e.getValue(), Math::max);
        }
        if (most.isEmpty() || most.containsKey(bi)) return null;
        List<ItemStack> m = new ArrayList<>();
        for (var e : most.entrySet()) m.add(new ItemStack(e.getKey(), e.getValue()));
        return new OreLootRecipe(bi, m.toArray(new ItemStack[0]));
    }

    @Override
    public boolean canBeCraftedBy(ItemStack stack) {
        return !Utils.isEmpty(stack) && stack.getItem() == block;
    }

    @Override
    public ItemStack[] maxOutput() {
        ItemStack[] o = new ItemStack[max.length];
        for (int i = 0; i < max.length; i++) o[i] = max[i].copy();
        return o;
    }

    @Override
    public ItemStack[] getOutputCopy(java.util.Random random) {
        ServerLevel level = level();
        if (level == null) return new ItemStack[0];
        return roll(level, block).toArray(new ItemStack[0]);
    }

    static List<ItemStack> roll(ServerLevel level, BlockItem block) {
        ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
        pick.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), 2);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
            .withParameter(LootContextParams.TOOL, pick);
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : block.getBlock().defaultBlockState().getDrops(params)) if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static ServerLevel level() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.overworld();
    }
}
