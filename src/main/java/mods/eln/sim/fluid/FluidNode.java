package mods.eln.sim.fluid;

/**
 * A volume in a fluid network (notes/pipes/DESIGN.md section 5): a device's vessel, a lumped piece of pipe, a junction,
 * or a fixed reservoir (the atmosphere, a lake) whose state never changes. Contents: each species' mass (water and air
 * in their own fields, the others in extra: SpeciesRegistry ids from 2) and the internal energy. Heights: z is the node's bottom (m), height its inside height (m); a port's height
 * on a node is a fraction of that (0 bottom, 1 top).
 */
public final class FluidNode {
    public final double volume, z, height;
    public final boolean fixed;
    public double mWater, mAir, energy;
    /** Masses of the other species (kg): extra[i] is species i + 2; null while there are none. */
    public double[] extra;
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
        n.energy = n.energyAt(T);
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
        extra = null;
        double vGas = Math.max(volume - mLiquid / Water.RHO_L, Flash.GAS_MIN * volume);
        double pv = mLiquid > 0 ? Math.min(Water.psat(T), p) : 0;
        mWater += mLiquid > 0 ? pv * vGas / (Water.R_V * T) : 0;
        mAir = Math.max(0, p - pv) * vGas / (Water.R_AIR * T);
        energy = energyAt(T);
        update();
        return this;
    }

    public void update() {
        if (CoreStep.ON) {
            coreUpdate();
            return;
        }
        state.solve(mWater, mAir, extra, energy, volume, state.T);
    }

    /** The fluid core's node for this one (CoreStep.ON): its thermodynamics behind every read; and its species' ids (null: water and air). */
    private mods.eln.fluidcore.Network.Node core;
    private int[] coreIds;

    /** The core node, (re)built for the species this one holds, its masses and energy set from this one's (no solve). */
    private mods.eln.fluidcore.Network.Node loadCore() {
        int[] ids = null;
        if (extra != null) {
            int count = 0;
            for (double e : extra) if (e > 0) count++;
            if (count > 0) {
                ids = new int[2 + count];
                ids[0] = SpeciesRegistry.WATER;
                ids[1] = SpeciesRegistry.AIR;
                int j = 2;
                for (int i = 0; i < extra.length; i++) if (extra[i] > 0) ids[j++] = i + 2;
            }
        }
        if (core == null || !java.util.Arrays.equals(ids, coreIds)) {
            mods.eln.fluidcore.Species[] sp = null;
            if (ids != null) {
                sp = new mods.eln.fluidcore.Species[ids.length];
                for (int j = 0; j < ids.length; j++) sp[j] = CoreStep.toCore(SpeciesRegistry.get().byId(ids[j]));
            }
            core = new mods.eln.fluidcore.Network.Node(volume, z, height, fixed, sp);
            coreIds = ids;
        }
        core.m[0] = mWater;
        core.m[1] = mAir;
        for (int j = 2; j < core.m.length; j++) core.m[j] = extra[coreIds[j] - 2];
        core.U = energy;
        return core;
    }

    private void coreUpdate() {
        loadCore().update();
        state.fromCore(core, coreIds, volume);
    }

    /** Mass of species id (kg), both phases. */
    public double mass(int id) {
        if (id == SpeciesRegistry.WATER) return mWater;
        if (id == SpeciesRegistry.AIR) return mAir;
        return extra != null && id - 2 < extra.length ? extra[id - 2] : 0;
    }

    /** Add m kg of species id (no energy: the caller books it). */
    public void addMass(int id, double m) {
        if (id == SpeciesRegistry.WATER) mWater += m;
        else if (id == SpeciesRegistry.AIR) mAir += m;
        else {
            if (m == 0 && (extra == null || id - 2 >= extra.length)) return;
            if (extra == null) extra = new double[Math.max(id - 1, SpeciesRegistry.get().size() - 2)];
            else if (id - 2 >= extra.length) extra = java.util.Arrays.copyOf(extra, Math.max(id - 1, SpeciesRegistry.get().size() - 2));
            extra[id - 2] += m;
        }
    }

    /** Ids past water and air it may hold (extra's length + 2), for loops over all its species. */
    public int speciesBound() {
        return extra == null ? 2 : extra.length + 2;
    }

    /** Liquid mass of species id (kg). */
    public double liquid(int id) {
        return state.liquid(id);
    }

    /** Vapour (or gas) mass of species id (kg). */
    public double vapour(int id) {
        return state.vapour(id);
    }

    /** The liquid layer species id is in (0 the bottom), or -1 if it has no liquid here. */
    public int layerOf(int id) {
        if (state.liquid(id) <= 0) return -1;
        for (int l = 0; l < state.layers; l++) if (state.inLayer(id, l)) return l;
        return -1;
    }

    /** How many liquid layers it holds (one per miscibility group with liquid), and the top of layer l (share of its volume). */
    public int layers() {
        return state.layers;
    }

    public double layerTop(int l) {
        return state.layerTop[l];
    }

    /** Volume of all its liquid (m^3). */
    public double liquidVolume() {
        return state.vLiquid;
    }

    /**
     * Specific enthalpy of water vapour at T (J/kg) as this node's thermodynamics has it: with the fluid core on, at the
     * node's vapour pressure (its vapour is not ideal); else Water's. What a device books for steam it takes or puts.
     */
    public double hVapour(double T) {
        if (CoreStep.ON) return mods.eln.fluidcore.WaterAir.hVapour(T, Math.max(state.pVapour, 1));
        return Water.hVapour(T);
    }

    /** The internal energy (J) its contents would have at T, in equilibrium. */
    public double energyAt(double T) {
        if (CoreStep.ON) return loadCore().energyAt(T);
        return Flash.energyAt(mWater, mAir, extra, volume, T);
    }

    /** Add share of src's contents (masses and energy) to this one (lumping pieces of a run; no update). */
    public void addShareOf(FluidNode src, double share) {
        mWater += src.mWater * share;
        mAir += src.mAir * share;
        energy += src.energy * share;
        if (src.extra != null) for (int i = 0; i < src.extra.length; i++) if (src.extra[i] != 0) addMass(i + 2, src.extra[i] * share);
    }

    /** Set this one's contents to share of src's (writing a lumped run back to its pieces; no update). */
    public void setShareOf(FluidNode src, double share) {
        mWater = src.mWater * share;
        mAir = src.mAir * share;
        energy = src.energy * share;
        double[] from = src.extra;   // (src may be this node: read before replacing)
        if (from == null) extra = null;
        else {
            double[] to = new double[from.length];
            for (int i = 0; i < to.length; i++) to[i] = from[i] * share;
            extra = to;
        }
    }

    public double T() {
        return state.T;
    }

    public double P() {
        return state.P;
    }

    /** The water vapour's partial pressure (Pa). */
    public double pVapour() {
        return state.pVapour;
    }

    /** Liquid water (kg). */
    public double mLiquid() {
        return state.mLiquid;
    }

    /** Water vapour (kg). */
    public double mVapour() {
        return state.mVapour;
    }

    /** The top of all its liquid, as a fraction of its height. */
    public double level() {
        if (CoreStep.ON && core != null) return core.level();
        return state.level(volume);
    }

    public double mass() {
        double m = mWater + mAir;
        if (extra != null) for (double e : extra) m += e;
        return m;
    }

    /** Pressure at a port at height fraction f: the gas pressure plus the liquid layers' columns above the port. */
    public double pressureAt(double f) {
        if (CoreStep.ON && core != null) return core.portP(f);
        if (state.layers <= 1) {
            double depth = Math.max(0, level() - f) * height;
            return state.P + (state.layers == 1 ? state.layerRho[0] : Water.RHO_L) * FluidNetwork.G * depth;
        }
        double p = state.P, bottom = 0;
        for (int l = 0; l < state.layers; l++) {
            double top = state.layerTop[l];
            if (top > f) p += state.layerRho[l] * FluidNetwork.G * (top - Math.max(f, bottom)) * height;
            bottom = top;
        }
        return p;
    }

    /**
     * The mean pressure over an opening span tall (a share of its height) centred at f: what pushes through a pipe's open
     * end, wet as far as the liquid reaches up it. span 0: pressureAt(f).
     */
    public double pressureOver(double f, double span) {
        if (span <= 0) return pressureAt(f);
        double lo = Math.max(0, f - span / 2), hi = Math.min(1, f + span / 2);
        if (hi <= lo) return pressureAt(f);
        if (state.layers <= 1 && !(CoreStep.ON && core != null)) {
            double rho = state.layers == 1 ? state.layerRho[0] : Water.RHO_L, L = level();
            double mean;   // (the mean depth under the level over lo..hi, in shares of the height)
            if (L <= lo) mean = 0;
            else if (L >= hi) mean = L - (lo + hi) / 2;
            else mean = (L - lo) * (L - lo) / (2 * (hi - lo));
            return state.P + rho * FluidNetwork.G * mean * height;
        }
        double sum = 0;
        for (int i = 0; i < 8; i++) sum += pressureAt(lo + (hi - lo) * (i + 0.5) / 8);
        return sum / 8;
    }

    /** Split off a fraction of this node's contents into a new node of volume fraction*volume (for a run being cut). */
    public FluidNode splitOff(double fraction, double z, double height) {
        FluidNode n = new FluidNode(volume * fraction, z, height, false);
        n.setShareOf(this, fraction);
        n.state.T = state.T;
        n.update();
        return n;
    }
}
