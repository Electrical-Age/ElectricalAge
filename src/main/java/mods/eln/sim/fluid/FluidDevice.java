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

    /** Edges inside the device between its own nodes (a valve between two pipes); the device sets open before each
     *  step. */
    default List<Link> fluidLinks() {
        return List.of();
    }

    final class Link {
        public final FluidNode a, b;
        public final double drawA, drawB, k;
        public double open = 1;
        /** CHECK: a check valve, passing flow only from a to b. */
        public FluidEdge.Kind kind = FluidEdge.Kind.PIPE;
        /** A pump's head (Pa) pushing a -> b; the device sets it with open before each step. */
        public double boost;
        /** This step's edge (set by the world). */
        public FluidEdge edge;

        public Link(FluidNode a, double drawA, FluidNode b, double drawB, double k) {
            this.a = a;
            this.drawA = drawA;
            this.b = b;
            this.drawB = drawB;
            this.k = k;
        }

        public Link kind(FluidEdge.Kind kind) {
            this.kind = kind;
            return this;
        }
    }

    /** Nodes joined inside the device (pipes meeting at a junction): each group is one volume (lumped pipes). */
    default List<List<FluidNode>> fluidJoins() {
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
     * on); k its conductance (m^3; two coupled ports are in series); at where on the face its centre is, in voxels
     * (0..16 along the face's two other axes in x, y, z order, packed u * 17 + v; the face centre is CENTRE): two ports
     * couple only at the same spot.
     */
    record FluidPort(FluidNode node, long pos, int face, double draw, double k, int at) {
        public static final int CENTRE = 8 * 17 + 8;

        public FluidPort(FluidNode node, long pos, int face, double draw, double k) {
            this(node, pos, face, draw, k, CENTRE);
        }
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
