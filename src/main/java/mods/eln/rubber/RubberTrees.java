package mods.eln.rubber;

import mods.eln.Eln;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Rubber trees (Baughn 2026-11-07, the rubber chain): a log, leaves and a sapling. They grow wild in small groves in
 * jungles (worldgen data eln:rubber_grove), and a sapling is crafted from four others, one of them jungle. A Tree Resin
 * Collector on a rubber trunk collects three times what it gets from any other tree (TreeResinCollectorElement).
 * Assets and data are hand-written in src/main/resources.
 */
public final class RubberTrees {
    private RubberTrees() {
    }

    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eln.MODID);
    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eln.MODID);

    /** The tree the sapling grows (data: worldgen/feature/rubber_tree.json). */
    public static final ResourceKey<Feature> TREE = ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(Eln.MODID, "rubber_tree"));
    public static final TreeGrower GROWER = new TreeGrower("eln_rubber", WeightedList.of(TREE), WeightedList.of(), WeightedList.of(), TREE);

    /** A log that burns as vanilla's do. */
    public static final class RubberLogBlock extends RotatedPillarBlock {
        public RubberLogBlock(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return 5;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return 5;
        }
    }

    /** Leaves that burn as vanilla's do. */
    public static final class RubberLeavesBlock extends TintedParticleLeavesBlock {
        public RubberLeavesBlock(BlockBehaviour.Properties p) {
            super(0.01f, p);
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return 60;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
            return 30;
        }
    }

    public static final DeferredBlock<RubberLogBlock> LOG = BLOCKS.registerBlock("rubber_log", RubberLogBlock::new,
        p -> p.mapColor(s -> s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? MapColor.SAND : MapColor.COLOR_LIGHT_GRAY)
            .instrument(NoteBlockInstrument.BASS).strength(2f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RubberLeavesBlock> LEAVES = BLOCKS.registerBlock("rubber_leaves", RubberLeavesBlock::new,
        p -> p.mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion()
            .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos, box) -> false)
            .isRedstoneConductor((s, l, pos) -> false).ignitedByLava().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.registerBlock("rubber_sapling", p -> new SaplingBlock(GROWER, p),
        p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));

    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerItem("rubber_log", p -> new BlockItem(LOG.get(), p.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> LEAVES_ITEM = ITEMS.registerItem("rubber_leaves", p -> new BlockItem(LEAVES.get(), p.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerItem("rubber_sapling", p -> new BlockItem(SAPLING.get(), p.useBlockDescriptionPrefix()));

    /** Is this block a rubber tree's log? */
    public static boolean isRubberLog(Block b) {
        return b == LOG.get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(SAPLING_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(LOG_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(LEAVES_ITEM);
    }

    /** (for the JEI audit and tests) */
    public static Item sapling() {
        return SAPLING_ITEM.get();
    }
}
