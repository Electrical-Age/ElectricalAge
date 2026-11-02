package mods.eln.api.docs;

import mods.eln.api.ElnApi;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.function.Supplier;

/** docs/api.md's soft dependency: a mod that runs with or without EA, its mill on EA's power when EA is there. */
public final class OptionalEa {
    private OptionalEa() {
    }

    // doc:begin guard
    /** Is EA installed? Ask before anything that reaches mods.eln.api. */
    public static final boolean ELN = ModList.get().isLoaded("eln");

    /** In the mod's constructor. This class names no mods.eln.api type, so it loads with or without EA. */
    public static void init(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> mill) {
        if (ELN) EaCompat.register(modBus, mill);   // EaCompat, and the API with it, loads only here
    }

    /** The mill's energy this tick: from EA's line when EA is there, else none (it runs on its own fuel). */
    public static double joulesFor(BlockEntity mill, double wanted) {
        return ELN ? EaCompat.take(mill, wanted) : 0;
    }

    /** Every mention of mods.eln.api is in classes like this one, reached only behind ELN. */
    static final class EaCompat {
        static void register(IEventBus modBus, Supplier<BlockEntityType<MillBlockEntity>> mill) {
            Mill.register(modBus, mill);
        }

        static double take(BlockEntity mill, double wanted) {
            return ElnApi.machines().of(mill).takeJoules(wanted);
        }
    }
    // doc:end guard
}
