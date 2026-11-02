package mods.eln.api.lowlevel;

import mods.eln.api.spi.Implementations;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A set of EA circuit parts that join and leave EA's simulation together. Make it, add its parts, then host it at
 * your block entity (hostAt): from the next tick its parts are in EA's simulation, its faces connect to EA cables
 * beside the block, and its links reach EA ports elsewhere. When the block entity goes (broken, replaced, its chunk
 * unloaded) the circuit leaves the simulation for good; the block entity that comes back on a reload builds a new one.
 * In a JUnit test, attach it to a Bench (ElnTestKit) instead.
 * <p>
 * Parts can be added while hosted. Each part belongs to its circuit: wiring a part of another circuit throws
 * IllegalArgumentException. Calls off the server thread throw IllegalStateException.
 */
public interface Circuit {
    /** An empty circuit; name labels it in messages and the debug trace. Throws IllegalStateException without EA. */
    static Circuit create(String name) {
        return Implementations.api().circuit(name);
    }

    String name();

    /** A power node: what cables join at a face, sources feed and resistors draw from. */
    Load load(String name);

    /** A signal node (EA's signal cables, 0..50 V, read as a level 0..1): what signal cables join at a face. */
    SignalLoad signalLoad(String name);

    /** A signal node with a gate output that drives it (0..1), floating (high impedance) until first set. */
    SignalSource signalSource(String name);

    /** A resistor of ohms (positive; infinite: open) between a and b. */
    ResistorLoad resistor(String name, Load a, Load b, double ohms);

    /** A resistor of ohms (positive; infinite: open) from a to ground. */
    ResistorLoad resistorToGround(String name, Load a, double ohms);

    /**
     * A source of volts (finite) at out, against ground, behind its internal resistance of ohms (positive and
     * finite: never an ideal source). It is a private node of the source's EMF and a resistor from it to out.
     */
    VoltageSource voltageSource(String name, Load out, double volts, double ohms);

    /** Watchdogs on this circuit's parts: limits checked after every step, with a trip callback. */
    Watchdogs watchdogs();

    /**
     * Links load to an existing EA port (see PortRef), as a cable would: resolved before each step in the level the
     * circuit is hosted in, re-resolved when the port's block changes, and its status says why it isn't connected.
     */
    PortLink connect(Load load, PortRef port);

    /**
     * Hosts the circuit at be (a server block entity, of a type not registered with ElnApi.machines()): EA puts an
     * unsaved node at its block, with faces (each a face relative to the block's facing and the load cables join
     * there; faces not given connect nothing). It joins the simulation at the start of the next tick (safe to call
     * from the block entity's onLoad) and leaves it when be is removed. Once per circuit and per block entity, else
     * IllegalStateException.
     */
    HostedNode hostAt(BlockEntity be, FaceConnection... faces);

    /** Are the circuit's parts in a simulation now (hosted and joined, or on a Bench)? */
    boolean attached();
}
