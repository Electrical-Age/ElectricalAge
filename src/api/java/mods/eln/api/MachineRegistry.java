package mods.eln.api;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Function;

/**
 * Machines at other mods' block entities. Register the type at common setup, then in the block entity's server tick
 * ask of(this) for its Machine and take (or give) energy:
 * <pre>{@code
 * // FMLCommonSetupEvent
 * ElnApi.machines().register(MY_MILL.get(), be -> MILL_SPEC);
 * // MillBlockEntity, server tick
 * Machine m = ElnApi.machines().of(this);
 * if (hasInput() && m.tryTakeJoules(JOULES_PER_STEP)) step();
 * }</pre>
 * Lifecycle, all EA's to handle:
 * <ul>
 * <li>When a registered block entity joins a server level (placed, or its chunk loaded), EA puts a node of its own at
 * its position (not saved: it lives as long as the block entity), connects it to EA cables and devices beside it and
 * reconnects them. of() does the same at once if it comes first.</li>
 * <li>The node lasts as long as the block entity object: block state changes that keep it (a furnace's LIT) change
 * nothing, while turning the block (its facing) reconnects it to its new faces. When the block entity goes (broken,
 * replaced, its chunk unloaded), the node goes the next tick and the Machine it had reads DETACHED.</li>
 * <li>Faces are relative to the block's facing: a property named facing or horizontal_facing (FACING,
 * HORIZONTAL_FACING); without one the block counts as facing north. LEFT and RIGHT are as seen facing the front;
 * UP and DOWN on a block facing up or down are EA's frame (east and west).</li>
 * <li>Every face is a leaf: each power face has its own terminal, and the machine draws (or feeds) through one of
 * them at a time, the first connected face with a live line, until it loses its cable or its line. Power never
 * passes through the block from one face to another, so unloading it never cuts an EA line.</li>
 * <li>The machine's state (buffer, temperature, a trip, that it was running) is saved with the block entity, in a
 * data attachment (eln:machine), and restored when the node is made again; nothing to save by hand.</li>
 * <li>EA can't burn another mod's block in place, so a spec that burns says what a burn does:
 * burnsOnOvervoltage(handler), called once on the server thread; the machine stays OVERVOLTAGE_TRIPPED for good.
 * The builder has no burn without a handler. Baughn: "a callback. Let the API user choose how to handle it".</li>
 * </ul>
 * Server thread only on the server (other threads throw IllegalStateException); on the client of() is a read-only
 * view, synced from the server (state at once, the numbers at most once a second).
 */
public interface MachineRegistry {
    /**
     * Hosts machines at block entities of type: spec makes a block entity's spec (called when its machine is made,
     * on server and client, with the block entity as loaded). Call it at common setup (FMLCommonSetupEvent); after
     * capabilities are registered it throws IllegalStateException. Registering a type again with the same function
     * does nothing; with another function it throws IllegalStateException.
     */
    <T extends BlockEntity> void register(BlockEntityType<T> type, Function<? super T, MachineSpec> spec);

    /** Is type registered? */
    boolean registered(BlockEntityType<?> type);

    /**
     * The machine of be: on the server the live machine (the same object for as long as the block entity lasts; it
     * reads DETACHED once the block entity has gone), on the client a read-only view (its takes, gives, signals, bus
     * and respec throw IllegalStateException; state, volts, power, buffer and temperature are synced). A block entity
     * of a type never registered gets an inert DETACHED machine, and a warning in the log once per type.
     */
    Machine of(BlockEntity be);
}
