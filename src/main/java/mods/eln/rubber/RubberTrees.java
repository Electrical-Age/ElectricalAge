package mods.eln.rubber;

import mods.eln.Eln;
import com.mojang.serialization.MapCodec;
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
 * Rubber trees (Baughn 2026-11-07, the rubber chain): rubber figs (Ficus elastica, RubberFigFeature), with a log, leaves,
 * aerial roots and a sapling. They grow wild in small groves in
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
    /** Bark on all six faces: buttress fins, branch elbows and the pillars aerial roots grow into (AdventurAgent: logs
     *  laid sideways showed their end grain and read as boxes). */
    public static final DeferredBlock<RubberLogBlock> WOOD = BLOCKS.registerBlock("rubber_wood", RubberLogBlock::new,
        p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).instrument(NoteBlockInstrument.BASS).strength(2f).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RubberLeavesBlock> LEAVES = BLOCKS.registerBlock("rubber_leaves", RubberLeavesBlock::new,
        p -> p.mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion()
            .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos, box) -> false)
            .isRedstoneConductor((s, l, pos) -> false).ignitedByLava().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<SaplingBlock> SAPLING = BLOCKS.registerBlock("rubber_sapling", p -> new SaplingBlock(GROWER, p),
        p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<AerialRootBlock> AERIAL_ROOT = BLOCKS.registerBlock("rubber_aerial_root", AerialRootBlock::new,
        p -> p.mapColor(MapColor.COLOR_BROWN).noCollision().instabreak().sound(SoundType.HANGING_ROOTS).ignitedByLava()
            .pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> AERIAL_ROOT_ITEM = ITEMS.registerItem("rubber_aerial_root", p -> new BlockItem(AERIAL_ROOT.get(), p.useBlockDescriptionPrefix()));

    /** The rubber fig's shape, in code (RubberFigFeature); worldgen/feature/rubber_tree.json names it. */
    static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, Eln.MODID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends Feature>, MapCodec<RubberFigFeature>> FIG =
        FEATURE_TYPES.register("rubber_fig", () -> RubberFigFeature.CODEC);

    public static final DeferredItem<BlockItem> LOG_ITEM = ITEMS.registerItem("rubber_log", p -> new BlockItem(LOG.get(), p.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> WOOD_ITEM = ITEMS.registerItem("rubber_wood", p -> new BlockItem(WOOD.get(), p.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> LEAVES_ITEM = ITEMS.registerItem("rubber_leaves", p -> new BlockItem(LEAVES.get(), p.useBlockDescriptionPrefix()));
    public static final DeferredItem<BlockItem> SAPLING_ITEM = ITEMS.registerItem("rubber_sapling", p -> new BlockItem(SAPLING.get(), p.useBlockDescriptionPrefix()));

    /** Is this block a rubber tree's log? */
    public static boolean isRubberLog(Block b) {
        return b == LOG.get() || b == WOOD.get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURE_TYPES.register(modBus);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(SAPLING_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(LOG_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(WOOD_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(LEAVES_ITEM);
        mods.eln.registry.ElnRegistry.TAB_EXTRAS.add(AERIAL_ROOT_ITEM);
    }

    /** (for the JEI audit and tests) */
    public static Item sapling() {
        return SAPLING_ITEM.get();
    }
}
