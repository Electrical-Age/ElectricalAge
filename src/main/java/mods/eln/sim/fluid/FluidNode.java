package mods.eln.sim.fluid;

/**
 * A volume in a fluid network (notes/pipes/DESIGN.md section 5): a device's vessel, a lumped piece of pipe, a junction,
 * or a fixed reservoir (the atmosphere, a lake) whose state never changes. Contents: water (liquid + vapour) and air
 * by mass, and the internal energy. Heights: z is the node's bottom (m), height its inside height (m); a port's height
 * on a node is a fraction of that (0 bottom, 1 top).
 */
public final class FluidNode {
    public final double volume, z, height;
    public final boolean fixed;
    public double mWater, mAir, energy;
    /** Heat into the contents this step (W; devices add to it, the network clears it after each step). */
    public double heat;
    /** A piece of pipe: pieces coupled to each other are lumped into one node per run (FluidWorld); resistance is the
     *  piece's own (1/conductance, along its length), so the run's ends get half the run's. */
    public boolean lumpable;
    public double resistance;
    /** Game tick this piece burst (not saved; a run that just burst is venting: its other pieces wait). */
    public long burstTick = Long.MIN_VALUE / 2;
    /** Heat loss to the surroundings (W/K) towards Water.T_AMBIENT; 0 = insulated. */
    public double lossWPerK;
    final Flash state = new Flash();

    public FluidNode(double volume, double z, double height, boolean fixed) {
        this.volume = volume;
        this.z = z;
        this.height = height;
        this.fixed = fixed;
        state.T = Water.T_AMBIENT;
    }

    /** A volume of air at ambient pressure and temperature (a new, empty pipe). */
    public static FluidNode air(double volume, double z, double height) {
        FluidNode n = new FluidNode(volume, z, height, false);
        n.fill(0, Water.P_ATM, Water.T_AMBIENT);
        return n;
    }

    /** The atmosphere: air at 1 atm and ambient temperature, never changing. */
    public static FluidNode atmosphere(double z) {
        FluidNode n = new FluidNode(1e6, z, 1, true);
        n.fill(0, Water.P_ATM, Water.T_AMBIENT);
        return n;
    }

    /** A fixed body of liquid water at temperature T (a lake or the well): what it gives is always liquid. */
    public static FluidNode lake(double z, double T) {
        FluidNode n = new FluidNode(1e6, z, 1, true);
        n.mWater = 0.999 * n.volume * Water.RHO_L;   // (a sliver of gas space: its surface)
        n.mAir = 0;
        n.energy = Flash.energyAt(n.mWater, 0, n.volume, T);
        n.update();
        return n;
    }

    /** A fixed body of liquid water at ambient temperature open to the air (1 atm on it): a pump's sump. */
    public static FluidNode sump(double z) {
        return sump(z, Water.T_AMBIENT);
    }

    /** The same at T (K; the local ambient), at most just under boiling (it is open to 1 atm). */
    public static FluidNode sump(double z, double T) {
        FluidNode n = new FluidNode(1e6, z, 1, true);
        n.fill(0.9 * n.volume * Water.RHO_L, Water.P_ATM, Math.min(T, Water.TB - 1));   // (fuller would compress the liquid: fill keeps GAS_MIN of gas)
        return n;
    }

    /** Set the contents: liquid water mLiquid (kg), and air to make up the pressure p (Pa) above it, at T (K). */
    public FluidNode fill(double mLiquid, double p, double T) {
        mWater = mLiquid;
        double vGas = Math.max(volume - mLiquid / Water.RHO_L, Flash.GAS_MIN * volume);
        double pv = mLiquid > 0 ? Math.min(Water.psat(T), p) : 0;
        mWater += mLiquid > 0 ? pv * vGas / (Water.R_V * T) : 0;
        mAir = Math.max(0, p - pv) * vGas / (Water.R_AIR * T);
        energy = Flash.energyAt(mWater, mAir, volume, T);
        update();
        return this;
    }

    public void update() {
        state.solve(mWater, mAir, energy, volume, state.T);
    }

    public double T() {
        return state.T;
    }

    public double P() {
        return state.P;
    }

    /** The vapour's partial pressure (Pa). */
    public double pVapour() {
        return state.pVapour;
    }

    public double mLiquid() {
        return state.mLiquid;
    }

    public double mVapour() {
        return state.mVapour;
    }

    public double level() {
        return state.level(volume);
    }

    public double mass() {
        return mWater + mAir;
    }

    /** Pressure at a port at height fraction f: the gas pressure plus the liquid column above the port. */
    public double pressureAt(double f) {
        double depth = Math.max(0, level() - f) * height;
        return state.P + Water.RHO_L * FluidNetwork.G * depth;
    }

    /** Split off a fraction of this node's contents into a new node of volume fraction*volume (for a run being cut). */
    public FluidNode splitOff(double fraction, double z, double height) {
        FluidNode n = new FluidNode(volume * fraction, z, height, false);
        n.mWater = mWater * fraction;
        n.mAir = mAir * fraction;
        n.energy = energy * fraction;
        n.state.T = state.T;
        n.update();
        return n;
    }
}
