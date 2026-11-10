package mods.eln.fluidcore;

/**
 * A node's thermodynamics: what Network needs from a node's contents (species masses m[0..S-1] and internal energy U)
 * in its volume (10-species-plan.md). Slopes are against the node's C = S + 1 unknowns (the masses, then U). Water and
 * air: WaterAirFluid.
 */
interface NodeFluid {
    /** S, the number of species masses. */
    int species();

    /** Solve for the state of contents (m, U), warm-started from the last one. */
    void update(double[] m, double U);

    double T();

    /** dT against (m, U). */
    double[] tSlope();

    /** The gas pressure. */
    double P();

    /** The liquid's level (share of the height). */
    double level();

    /** The pressure at a port at share f of the height (the liquid's head over it included), and its slope. */
    double portP(double f);

    double[] portPSlope(double f);

    /** What a kg drawn from the port at f carries: each species' share, then its energy (C entries); and the slopes [C][C]. */
    double[] draw(double f);

    double[][] drawSlope(double f);

    /**
     * For the counter-current exchange: how much the port at f draws liquid (0..1), the density of the liquid it draws
     * and of the gas, and what a kg of that liquid / of the gas carries.
     */
    double liquidWindow(double f);

    double rhoLiquid(double f);

    double rhoGas();

    double[] liquidDraw(double f);

    double[] gasDraw();

    /** Species i's liquid and vapour (or gas) mass (kg) in the solved state, and its partial pressure in the gas (Pa). */
    double liquidMass(int i);

    double vapourMass(int i);

    double partialP(int i);

    /** The internal energy (J) masses m would have at T, in equilibrium in this node (no change to its state). */
    double energyAt(double[] m, double T);

    /** An opaque copy of the solved state, and putting it back (Network.perturbed: a trial without a new solve after). */
    Object saveState();

    void restoreState(Object s);
}
