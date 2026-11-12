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

        /**
         * Openings (as FluidPort.span: a share of the node's height, 0 a point) at each end: when exactly one end is an
         * opening, the world gives the link the couplings' counter-flow pair of one-way edges, so liquid can run out
         * under it while gas comes back over it (a sealed part-full pipe arm into a junction drains: LabAgent's review,
         * 2026-11-12). Valves leave both 0 (one edge: their opening and regulating act on it).
         */
        public double spanA, spanB;

        public Link spans(double a, double b) {
            spanA = a;
            spanB = b;
            return this;
        }

        public Link kind(FluidEdge.Kind kind) {
            this.kind = kind;
            return this;
        }

        /** Heat conductance between the two nodes (W/K; a heat exchanger's plates), set by the device each step. */
        public double conductance;
        /** Its own stream band (Pa; NaN: the core's): narrower mixes its two ends less at rest (Network.Edge.band). */
        public double band = Double.NaN;

        /** TURBINE: its nozzle coefficient (FluidEdge.nozzle); the device sets open and drop before each step. */
        public double nozzle, drop;

        /** This link as a steam turbine (Network.Kind.TURBINE): nozzle coefficient, efficiency. */
        public Link turbine(double nozzle, double eta) {
            this.kind = FluidEdge.Kind.TURBINE;
            this.nozzle = nozzle;
            this.eta = eta;
            return this;
        }

        /** PUMP: its curve (FluidEdge.qMax, h0, eta); the device sets speed before each step. */
        public double qMax, h0, speed = 1, eta = 0.7;

        /** PUMP as a gas pump (Network.Edge.gas): qMax a volume (m^3/s), h0 the pressure it makes whatever it draws. */
        public boolean gas;

        /** This link as a gas pump (an ejector): free flow qMax (m^3/s at full speed), shut-off pressure rise h0 (Pa). */
        public Link gasPump(double qMax, double h0) {
            pump(qMax, h0);
            gas = true;
            return this;
        }

        /** This link as a centrifugal pump: free flow qMax (kg/s at full speed), shut-off head h0 (Pa). */
        public Link pump(double qMax, double h0) {
            this.kind = FluidEdge.Kind.PUMP;
            this.qMax = qMax;
            this.h0 = h0;
            return this;
        }

        /** A valve's flow area fully open (m^2): it then meters by the orifice law (FluidEdge.area). */
        public double area;
        /** REGULATOR: its set point (Pa) and mode (relief: holds a at it; else reducing: holds b), set before each step. */
        public double setPa;
        public boolean relief;

        public Link area(double area) {
            this.area = area;
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
     * couple only at the same spot. span: how tall the opening is, as a share of the node's height, centred on draw (0: a
     * point, as a device's flange on its vessel; a pipe's open end on a side face is as tall as its bore: it passes
     * liquid as far as it is wet, Baughn 2026-11-03).
     */
    record FluidPort(FluidNode node, long pos, int face, double draw, double k, int at, double span) {
        public static final int CENTRE = 8 * 17 + 8;

        public FluidPort(FluidNode node, long pos, int face, double draw, double k) {
            this(node, pos, face, draw, k, CENTRE);
        }

        public FluidPort(FluidNode node, long pos, int face, double draw, double k, int at) {
            this(node, pos, face, draw, k, at, 0);
        }
    }

    /** A vent to the air from a node at height fraction draw; the device sets open (0..1) before each step. */
    final class Vent {
        public final FluidNode node;
        public final double draw, k;
        public double open;
        /** Gas only, never liquid, and nothing back in (a purge valve: Network.Edge.gasOnly). */
        public boolean gasOnly;
        /** The other way: only air in from outside, nothing out (a vacuum breaker: a check valve from the air). */
        public boolean inward;
        /**
         * A relief vent (a safety valve, Baughn 2026-11-12: it sirened): above 0, a self-acting relief regulator to the air
         * that holds its node at this pressure (Pa) inside the core's solve, its conductance k wide open. Set before each step.
         */
        public double setPa;
        /** This step's edge (set by the world when it builds the network). */
        public FluidEdge edge;

        public Vent(FluidNode node, double draw, double k) {
            this.node = node;
            this.draw = draw;
            this.k = k;
        }
    }
}
