package mods.eln.oil;

import mods.eln.Eln;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Oil worldgen registrations: the lake structure and its piece, and tar sand. */
public final class OilWorldgen {
    private OilWorldgen() {
    }

    static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Eln.MODID);
    static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Eln.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<OilLakeStructure>> LAKE_TYPE =
        STRUCTURE_TYPES.register("oil_lake", () -> () -> OilLakeStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LAKE_PIECE =
        PIECES.register("oil_lake", () -> (StructurePieceType.ContextlessType) OilLakePiece::new);

    public static DeferredBlock<Block> TAR_SAND;
    public static DeferredItem<Item> TAR_SAND_ITEM;

    static void register(IEventBus modBus) {
        TAR_SAND = Oil.BLOCKS.registerSimpleBlock("tar_sand", p -> p.mapColor(MapColor.TERRACOTTA_BLACK).strength(0.9f).sound(SoundType.SAND));
        TAR_SAND_ITEM = Oil.ITEMS.registerItem("tar_sand", p -> new BlockItem(TAR_SAND.get(), p.useBlockDescriptionPrefix()));
        STRUCTURE_TYPES.register(modBus);
        PIECES.register(modBus);
    }
}
