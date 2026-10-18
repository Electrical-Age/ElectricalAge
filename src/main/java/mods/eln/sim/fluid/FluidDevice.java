package mods.eln.sim.fluid;

import java.util.List;

/**
 * Something with fluid volumes in the world (a boiler, a turbine, later pipes and tanks): its nodes, the ports where
 * they meet a neighbour, and vents to the air. The world (mods.eln.fluid.FluidWorld) joins devices whose ports face
 * each other into networks and steps them; a device is in the world while its EA node is in the simulation.
 */
public interface FluidDevice {
    /** Devices couple only within one level (any key that tells levels apart). */
    Object fluidLevel();

    List<FluidPort> fluidPorts();

    default List<Vent> fluidVents() {
        return List.of();
    }

    /** Before each step of its network (heat exchange, valve settings), with the step's length (s). */
    default void beforeFluidStep(double dt) {
    }

    /** After each step (meters, power from what flowed). */
    default void afterFluidStep(double dt) {
    }

    /**
     * A port: node is the device's volume behind it; pos and face say where it is (a block position as a long and the
     * face it opens on, as net.minecraft.core.Direction's 3D data value: 0 down, 1 up, 2 north, 3 south, 4 west,
     * 5 east); draw is where on the node it draws from (0 bottom .. 1 top: a dome draws steam whatever face it opens
     * on); k its conductance (m^3; two coupled ports use the smaller).
     */
    record FluidPort(FluidNode node, long pos, int face, double draw, double k) {
    }

    /** A vent to the air from a node at height fraction draw; the device sets open (0..1) before each step. */
    final class Vent {
        public final FluidNode node;
        public final double draw, k;
        public double open;
        /** This step's edge (set by the world when it builds the network). */
        public FluidEdge edge;

        public Vent(FluidNode node, double draw, double k) {
            this.node = node;
            this.draw = draw;
            this.k = k;
        }
    }
}
