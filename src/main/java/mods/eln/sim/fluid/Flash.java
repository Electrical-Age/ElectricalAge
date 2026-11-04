package mods.eln.sim.fluid;

/**
 * The state of a closed volume from its contents: given each species' mass (SpeciesRegistry ids; water 0, air 1), the
 * internal energy and the volume, find the temperature at which they are in equilibrium, how much of each species is
 * liquid, and the pressure. Liquids sit at the bottom in layers, one per miscibility group, the densest lowest; vapours
 * and gases share the space above them. A condensable species' vapour pressure is its saturation pressure times its
 * mole fraction in its group, from the node's whole composition (Raoult; Baughn 2026-11-03, by the total composition
 * after Argon's review: non-iterative, so the split stays one linear solve). When liquid fills the volume, the pressure
 * rises steeply (a softened bulk modulus, Water.K_LIQUID), so a full node stays solvable. Water and air alone give
 * today's numbers.
 */
public final class Flash {
    /** Smallest gas space, as a fraction of the volume (below it the liquid is "full"). */
    static final double GAS_MIN = 0.02;
    public static final double T_MIN = 80, T_MAX = 6000;   // (gas expanding fast cools a lot; ice is not modelled)

    public double T, P, mLiquid, mVapour, vGas;
    /** Partial pressures (Pa) of water vapour and air in the gas space, and the liquid's extra pressure when full. */
    public double pVapour, pAir, pFull;
    /** Per species (by id, up to n): mass in, liquid and vapour (or gas) mass out; total liquid volume (m^3). */
    double[] m = new double[2], liq = new double[2], vap = new double[2];
    int n = 2;
    public double vLiquid;
    /**
     * Liquid layers from the bottom up: count, and per layer its group's first species id, volume (m^3), mass (kg) and
     * its top as a fraction of the node's volume (cumulative).
     */
    public int layers;
    int[] layerGroup = new int[1];
    public double[] layerVolume = new double[1], layerMass = new double[1], layerTop = new double[1], layerRho = new double[1];
    private int[] layerSpecies = new int[1];
    /** Scratch for the split: Raoult factors, breakpoints and an order. */
    private double[] a = new double[2], b = new double[2], groupMoles = new double[2];
    private int[] order = new int[2];

    public Flash() {
    }

    public Flash copy() {
        Flash f = new Flash();
        f.T = T; f.P = P; f.mLiquid = mLiquid; f.mVapour = mVapour; f.vGas = vGas;
        f.pVapour = pVapour; f.pAir = pAir; f.pFull = pFull;
        f.n = n;
        f.m = m.clone(); f.liq = liq.clone(); f.vap = vap.clone();
        f.vLiquid = vLiquid;
        f.layers = layers;
        f.layerGroup = layerGroup.clone(); f.layerVolume = layerVolume.clone(); f.layerMass = layerMass.clone(); f.layerTop = layerTop.clone();
        f.layerRho = layerRho.clone(); f.layerSpecies = layerSpecies.clone();
        return f;
    }

    /** Fraction of the volume taken by liquid (the liquid level, 0 bottom to 1 top). */
    public double level(double V) {
        return V <= 0 ? 0 : Math.min(1, vLiquid / V);
    }

    /** Liquid mass of species id (kg). */
    public double liquid(int id) {
        return id < n ? liq[id] : 0;
    }

    /** Vapour (or gas) mass of species id (kg). */
    public double vapour(int id) {
        return id < n ? vap[id] : 0;
    }

    /** Size the per-species arrays for species ids below count (keeps nothing: the masses are set after). */
    void size(int count) {
        if (m.length < count) {
            m = new double[count]; liq = new double[count]; vap = new double[count];
            a = new double[count]; b = new double[count]; groupMoles = new double[count];
            order = new int[count];
            layerGroup = new int[count]; layerVolume = new double[count]; layerMass = new double[count]; layerTop = new double[count];
            layerRho = new double[count]; layerSpecies = new int[count];
        }
        n = count;
    }

    /** Set the masses in: water, air, and the others (extra[i] is species i + 2; may be null). */
    void load(double mw, double ma, double[] extra) {
        int count = 2;
        if (extra != null) for (int i = extra.length - 1; i >= 0; i--) if (extra[i] != 0) { count = i + 3; break; }
        size(count);
        m[0] = mw;
        m[1] = ma;
        for (int i = 2; i < count; i++) m[i] = extra[i - 2];
    }

    /** Equilibrium split of mw water and ma air only at temperature T into this (no energy check). */
    void split(double mw, double ma, double V, double T) {
        load(mw, ma, null);
        split(V, T);
    }

    /** The energy of the split last made (ma: unused, the air is in the split). */
    double energy(double ma) {
        return energy();
    }

    /** Equilibrium split of the loaded masses at temperature T into this (no energy check). */
    void split(double V, double T) {
        splitMasses(V, T);
        finish(V);
    }

    /** The phase split only (what energy() needs): each species' liquid and vapour at T. */
    private void splitMasses(double V, double T) {
        if (n == 2) {
            splitWaterAir(V, T);
            return;
        }
        SpeciesRegistry reg = SpeciesRegistry.get();
        double dMin = GAS_MIN * V;
        // Raoult within each group: mole fractions from the group's whole mass (its condensable and non-volatile species)
        java.util.Arrays.fill(groupMoles, 0, n, 0);
        for (int i = 0; i < n; i++) {
            if (m[i] <= 0) continue;
            Species s = reg.byId(i);
            if (!s.gas()) groupMoles[groupOf(reg, i)] += m[i] / s.molarMass;
        }
        // what is liquid whatever the gas space (non-volatile), what is gas (gases, supercritical), and each condensable
        // species' share of its saturation pressure (Raoult: a[i] = x_i psat_i(T), Pa)
        double vFixed = 0;
        int k = 0;
        for (int i = 0; i < n; i++) {
            liq[i] = 0;
            vap[i] = 0;
            double mi = m[i];
            if (mi <= 0) continue;
            Species s = reg.byId(i);
            if (s.gas() || T >= s.critical()) vap[i] = mi;
            else if (!s.condensable()) {
                liq[i] = mi;
                vFixed += mi / s.rhoL;
            } else {
                double x = (mi / s.molarMass) / groupMoles[groupOf(reg, i)];
                a[i] = x * s.psat(T);
                // the gas space above which it is all vapour
                b[i] = a[i] > 0 ? mi * s.r * T / a[i] : Double.MAX_VALUE;
                order[k++] = i;
            }
        }
        if (k > 0) splitCondensables(reg, V - vFixed, dMin, T, k);
        this.T = T;
    }

    /** Water and air only (most nodes): the general split's arithmetic, without its bookkeeping. */
    private void splitWaterAir(double V, double T) {
        double mw = m[0];
        liq[1] = 0;
        vap[1] = m[1];
        double mv;
        if (mw <= 0) mv = 0;
        else if (T >= Water.TC) mv = mw;                                   // supercritical: one gas phase
        else {
            double ps = Water.psat(T);
            if (mw * Water.R_V * T / V <= ps) mv = mw;                    // all vapour (superheated)
            else {
                double dMin = GAS_MIN * V;
                double denom = 1 - ps / (Water.RHO_L * Water.R_V * T);
                mv = ps * (V - mw / Water.RHO_L) / (Water.R_V * T) / denom;
                if (mv < 0 || V - (mw - mv) / Water.RHO_L < dMin) mv = Math.min(mw, ps * dMin / (Water.R_V * T));
                mv = Math.max(0, Math.min(mw, mv));
            }
        }
        vap[0] = mv;
        liq[0] = mw - mv;
        this.T = T;
    }

    /** From the split: volumes, partial pressures, the pressure, the layers. */
    private void finish(double V) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double dMin = GAS_MIN * V;
        vLiquid = 0;
        double pv = 0, pg = 0;
        for (int i = 0; i < n; i++) if (liq[i] != 0) vLiquid += liq[i] / reg.byId(i).rhoL;
        vGas = Math.max(V - vLiquid, dMin);
        pVapour = 0;
        pAir = 0;
        for (int i = 0; i < n; i++) {
            if (vap[i] <= 0) continue;
            double p = vap[i] * reg.byId(i).r * T / vGas;
            if (i == SpeciesRegistry.WATER) pVapour = p;
            else if (i == SpeciesRegistry.AIR) pAir = p;
            else pg += p;
        }
        pv = pVapour + pAir + pg;
        mLiquid = liq[0];
        mVapour = vap[0];
        pFull = Math.max(0, vLiquid - (V - dMin)) * Water.K_LIQUID / V;
        P = pv + pFull;
        buildLayers(reg, V);
    }

    /**
     * The condensable species' split at T, in vAvail (m^3) of room beside the non-volatile liquids. While species j has
     * liquid, its vapour is a_j V' / (r_j T) in a gas space V'; V' is the root of V' - vAvail + sum (m_j - vapour_j) /
     * rho_j over those with liquid: increasing (while the vapours are lighter than their liquids) and linear between
     * the gas spaces where each species turns all vapour (sorted), so one segment holds it. With water alone this is
     * today's formula, operation for operation.
     */
    private void splitCondensables(SpeciesRegistry reg, double vAvail, double dMin, double T, int k) {
        // all vapour in the whole room?
        boolean allVapour = true;
        for (int q = 0; q < k; q++) {
            int i = order[q];
            if (!(m[i] * reg.byId(i).r * T / vAvail <= a[i])) { allVapour = false; break; }
        }
        if (allVapour) {
            for (int q = 0; q < k; q++) vap[order[q]] = m[order[q]];
            return;
        }
        sortByBreakpoint(k);
        int first = 0;   // the species from here on in the order still have liquid
        for (int seg = 0; seg < k; seg++) {
            double num = vAvail, den = 1;
            for (int q = seg; q < k; q++) {
                int i = order[q];
                Species s = reg.byId(i);
                num -= m[i] / s.rhoL;
                den -= a[i] / (s.rhoL * s.r * T);
            }
            first = seg;
            if (den > 0 && num / den <= b[order[seg]]) break;
            first = seg + 1;
        }
        double num = vAvail, den = 1;
        for (int q = first; q < k; q++) {
            int i = order[q];
            Species s = reg.byId(i);
            num -= m[i] / s.rhoL;
            den -= a[i] / (s.rhoL * s.r * T);
        }
        double gas = vAvail;
        for (int q = 0; q < k; q++) {
            int i = order[q];
            if (q < first) vap[i] = m[i];
            else vap[i] = a[i] * num / (reg.byId(i).r * T) / den;
        }
        boolean clamp = false;
        for (int q = 0; q < k; q++) {
            int i = order[q];
            if (vap[i] < 0) clamp = true;
            gas -= (m[i] - vap[i]) / reg.byId(i).rhoL;
        }
        if (clamp || gas < dMin) {
            // (the liquid fills the space: each species' vapour as in the smallest gas space)
            for (int q = 0; q < k; q++) {
                int i = order[q];
                vap[i] = Math.min(m[i], a[i] * dMin / (reg.byId(i).r * T));
            }
        }
        for (int q = 0; q < k; q++) {
            int i = order[q];
            vap[i] = Math.max(0, Math.min(m[i], vap[i]));
            liq[i] = m[i] - vap[i];
        }
    }

    /** The group's representative id: the lowest species id in it (ids carry the group's moles and its layer). */
    private static int groupOf(SpeciesRegistry reg, int id) {
        return reg.groupReps()[id];
    }

    private void sortByBreakpoint(int k) {
        for (int i = 1; i < k; i++) {   // (insertion sort: a handful of species)
            int v = order[i];
            int j = i - 1;
            while (j >= 0 && b[order[j]] > b[v]) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = v;
        }
    }

    /** Liquid layers, one per group with liquid, the densest at the bottom; tops as fractions of the volume. */
    private void buildLayers(SpeciesRegistry reg, double V) {
        layers = 0;
        for (int i = 0; i < n; i++) {
            if (liq[i] <= 0) continue;
            int g = groupOf(reg, i);
            int l = 0;
            while (l < layers && layerGroup[l] != g) l++;
            if (l == layers) {
                layerGroup[l] = g;
                layerVolume[l] = 0;
                layerMass[l] = 0;
                layerSpecies[l] = i;
                layers++;
            } else layerSpecies[l] = -1;   // (more than one species: its density by mass over volume)
            layerVolume[l] += liq[i] / reg.byId(i).rhoL;
            layerMass[l] += liq[i];
        }
        for (int l = 0; l < layers; l++)
            layerRho[l] = layerSpecies[l] >= 0 ? reg.byId(layerSpecies[l]).rhoL : layerMass[l] / layerVolume[l];
        // densest first (insertion sort; ties by group id, so the order is deterministic)
        for (int i = 1; i < layers; i++) {
            int g = layerGroup[i], sp = layerSpecies[i];
            double v = layerVolume[i], ms = layerMass[i], rho = layerRho[i];
            int j = i - 1;
            while (j >= 0 && (layerRho[j] < rho || layerRho[j] == rho && layerGroup[j] > g)) {
                layerGroup[j + 1] = layerGroup[j];
                layerVolume[j + 1] = layerVolume[j];
                layerMass[j + 1] = layerMass[j];
                layerRho[j + 1] = layerRho[j];
                layerSpecies[j + 1] = layerSpecies[j];
                j--;
            }
            layerGroup[j + 1] = g;
            layerVolume[j + 1] = v;
            layerMass[j + 1] = ms;
            layerRho[j + 1] = rho;
            layerSpecies[j + 1] = sp;
        }
        double top = 0;
        for (int l = 0; l < layers; l++) {
            top += V <= 0 ? 0 : layerVolume[l] / V;
            layerTop[l] = Math.min(1, top);
        }
    }

    /** Is species id in layer l (its group's). */
    boolean inLayer(int id, int l) {
        return groupOf(SpeciesRegistry.get(), id) == layerGroup[l];
    }

    double energy() {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double u = 0;
        for (int i = 0; i < n; i++) {
            if (liq[i] == 0 && vap[i] == 0) continue;
            Species s = reg.byId(i);
            if (liq[i] != 0) u += liq[i] * s.uLiquid(T);
            if (vap[i] != 0) u += vap[i] * s.uVapour(T);
        }
        return u;
    }

    /**
     * Solve for the masses already loaded, the energy U (J) in V (m^3). An empty volume keeps tHint and has no pressure.
     * Bisection on T (the energy rises with T at fixed contents: warmer, and more of it vapour).
     */
    Flash solveLoaded(double U, double V, double tHint) {
        double total = 0;
        for (int i = 0; i < n; i++) total += m[i];
        if (total <= 1e-15) {
            split(V, tHint);
            return this;
        }
        double lo = T_MIN, hi = T_MAX;
        splitMasses(V, lo);
        if (energy() >= U) {
            finish(V);
            return this;
        }
        splitMasses(V, hi);
        if (energy() <= U) {
            finish(V);
            return this;
        }
        for (int i = 0; i < 60 && hi - lo > 1e-7; i++) {
            double mid = 0.5 * (lo + hi);
            splitMasses(V, mid);
            if (energy() < U) lo = mid;
            else hi = mid;
        }
        split(V, 0.5 * (lo + hi));
        return this;
    }

    /** Solve for water mw (kg) and air ma (kg) only, U (J) in V (m^3). */
    public Flash solve(double mw, double ma, double U, double V, double tHint) {
        load(mw, ma, null);
        return solveLoaded(U, V, tHint);
    }

    /** Solve for water, air and the other species (extra[i]: species i + 2, may be null). */
    public Flash solve(double mw, double ma, double[] extra, double U, double V, double tHint) {
        load(mw, ma, extra);
        return solveLoaded(U, V, tHint);
    }

    /** The internal energy (J) of mw water and ma air at temperature T in V, in equilibrium (for setting up a state). */
    public static double energyAt(double mw, double ma, double V, double T) {
        return energyAt(mw, ma, null, V, T);
    }

    /** The same with other species (extra[i]: species i + 2, may be null). */
    public static double energyAt(double mw, double ma, double[] extra, double V, double T) {
        Flash f = new Flash();
        f.load(mw, ma, extra);
        f.split(V, T);
        return f.energy();
    }
}
