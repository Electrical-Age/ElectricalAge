package mods.eln.sim.fluid;

/**
 * A connection between two nodes (a pipe piece, a valve, a device port pair, a turbine, a vent). Flow is volumetric:
 * Q = conductance / viscosity x (pressure difference + gravity head), carried at the upstream node's port: liquid if
 * the port is below that node's liquid level, else its gas (vapour and air in proportion).
 */
public final class FluidEdge {
    public enum Kind { PIPE, CHECK, TURBINE }

    public final FluidNode a, b;
    /** Port heights on a and b (fractions of each node's height). */
    public final double portA, portB;
    /** Geometric conductance (m^3; Q = k / mu x dP). */
    public double k;
    /** 0 closed .. 1 open (a valve). */
    public double open = 1;
    public Kind kind = Kind.PIPE;
    /** TURBINE: electric power made this step (W), at EFFICIENCY of the steam's heat above liquid at 100 C. */
    public double power;
    public static final double TURBINE_EFFICIENCY = 0.40;

    /** This step's flow a -> b (kg/s; negative: b -> a), and what it carried: liquid, vapour, air (kg/s), enthalpy (W). */
    public double flow, fLiquid, fVapour, fAir, fEnthalpy;
    // the solve's coefficient and upstream properties (per step)
    double g, rho, head, sA, sB, xl;
    boolean fromA;

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
