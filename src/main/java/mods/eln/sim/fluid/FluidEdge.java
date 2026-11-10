package mods.eln.sim.fluid;

/**
 * A connection between two nodes (a pipe piece, a valve, a device port pair, a turbine, a vent). Flow is volumetric:
 * Q = conductance / viscosity x (pressure difference + gravity head), carried at the upstream node's port: liquid if
 * the port is below that node's liquid level, else its gas (vapour and air in proportion).
 */
public final class FluidEdge {
    public enum Kind { PIPE, CHECK, TURBINE, PUMP }

    public final FluidNode a, b;
    /** Port heights on a and b (fractions of each node's height). */
    public final double portA, portB;
    /** How tall each end's opening is (a share of its node's height, centred on the port; 0: a point): FluidPort.span. */
    public double spanA, spanB;
    /**
     * An entry edge into an opening (FluidWorld: the second of an opening's two edges, b the opening's node, entrySpan its
     * height): liquid coming in meets the opening as the liquid going out does (its wet mean), gas meets it at its
     * height as a point (it bubbles in over the liquid); so liquid can't circulate out and back in through the pair.
     */
    public double entrySpan;
    /** Geometric conductance (m^3; Q = k / mu x dP). */
    public double k;
    /** 0 closed .. 1 open (a valve). */
    public double open = 1;
    /**
     * A valve's flow area when fully open (m^2), or 0: a metering edge, by the orifice law m = CD x area x open x
     * sqrt(2 rho dP) (both phases), rather than the lossless pipes' linear conductance (FluidNetwork.props).
     */
    public double area;
    /** The orifice law's discharge coefficient, and the drop under which it turns linear (Pa: g stays finite). */
    public static final double CD = 0.6, DP_LINEAR = 1000;
    /** A pump's head (Pa), pushing a -> b: added to the drive (a pump: its shutoff pressure above its sump). */
    public double boost;
    public Kind kind = Kind.PIPE;
    /**
     * PUMP: a centrifugal pump a -> b in the fluid core's solve (its flow speed qMax sqrt(1 - dp / h0) against the
     * back-pressure dp; Network.Kind.PUMP): free flow qMax (kg/s), shut-off head h0 (Pa), speed (0..), efficiency; and the
     * power it put into the fluid last step (W).
     */
    public double qMax, h0, speed = 1, eta = 0.7, work;
    /**
     * TURBINE (in the fluid core's solve, Network.Kind.TURBINE): its nozzle coefficient and the isentropic drop (J/kg)
     * from its inlet to its exhaust's pressure, set each step; it takes eta x drop a kg of water out as shaft work (work,
     * W over the last step; workJ, J).
     */
    public double nozzle, drop, workJ;
    /** TURBINE: electric power made this step (W), at EFFICIENCY of the steam's heat above liquid at 100 C. */
    public double power;
    public static final double TURBINE_EFFICIENCY = 0.40;

    /**
     * This step's flow a -> b (kg/s; negative: b -> a), and what it carried: liquid water, water vapour, air (kg/s),
     * enthalpy (W); fSpecies[id]: each species, both phases (kg/s, a -> b; null until it has carried something).
     */
    public double flow, fLiquid, fVapour, fAir, fEnthalpy;
    public double[] fSpecies;
    // the solve's coefficient and upstream properties (per step)
    double g, rho, head, sA, sB, xl;
    /** How each end's port pressure answers a kg more liquid in its node (Pa/kg: the weight over the port; FluidNetwork.headResponse). */
    double hA, hB;
    boolean fromA;
    /** The solve's per-step coefficients, for /elnlab fluidtrace. */
    public String debug() {
        return String.format("g=%.4g head=%.4g sA=%.4g sB=%.4g xl=%.3f fromA=%s", g, head, sA, sB, xl, fromA);
    }

    /** This step's packets each end gives (FluidNetwork.packet; cached: node states hold still until the step ends). */
    FluidNetwork.Packet pkA, pkB;

    public FluidEdge(FluidNode a, double portA, FluidNode b, double portB, double k) {
        this.a = a;
        this.b = b;
        this.portA = portA;
        this.portB = portB;
        this.k = k;
    }

    public FluidEdge kind(Kind kind) {
        this.kind = kind;
        return this;
    }

    /** Check valves and turbines pass flow only from a to b. */
    public boolean oneWay() {
        return kind == Kind.CHECK || kind == Kind.TURBINE;
    }

    double zA() {
        return a.z + portA * a.height;
    }

    double zB() {
        return b.z + portB * b.height;
    }
}
