package mods.eln.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Forge Energy receiver for game tests (a bare server has no FE block): a lodestone at a position a test put here
 * accepts energy on every face. Anywhere else (every lodestone in a real world) the capability is null.
 */
public final class TestEnergySinks {
    private TestEnergySinks() {
    }

    static final Map<BlockPos, SimpleEnergyHandler> SINKS = new ConcurrentHashMap<>();

    public static final net.minecraft.world.level.block.Block BLOCK = Blocks.LODESTONE;

    /** A sink of the given capacity at pos (put a {@link #BLOCK} there). */
    public static SimpleEnergyHandler put(BlockPos pos, int capacity) {
        SimpleEnergyHandler h = new SimpleEnergyHandler(capacity);
        SINKS.put(pos.immutable(), h);
        return h;
    }

    public static void remove(BlockPos pos) {
        SINKS.remove(pos);
    }

    static void register(IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, e -> e.registerBlock(Capabilities.Energy.BLOCK,
            (level, pos, state, be, side) -> (EnergyHandler) SINKS.get(pos), BLOCK));
    }
}
