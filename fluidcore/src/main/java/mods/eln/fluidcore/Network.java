package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;

/**
 * A network of fluid volumes and the flows between them, stepped fully implicitly (board #61, 08-core-plan.md M2).
 * <p>
 * Each free node holds a mass per species (kg; water and air for now) and internal energy U (J), its thermodynamics
 * behind NodeFluid; a fixed node is a boundary (atmosphere,
 * reservoir) whose contents never change. A step solves for the end-of-step contents Y' of every free node:
 * <pre>  Y' = Y + dt (sum of fluxes into it at Y') + dt (heat in)</pre>
 * Each edge's mass flow is q = g * (port pressure at a - port pressure at b + boost) at the end state, and it carries
 * what the upstream end's port draws at the end state (strictly upwind: the liquid by the port's window on the level,
 * the gas by its composition), so a node can pass many times its contents in a step and still end non-negative. The
 * fluxes are antisymmetric, so mass per species and energy are conserved exactly; no caps, limiters or passes.
 * Solved by Newton with the exact Jacobian (each flux's slopes chained through its end nodes' thermodynamic slopes),
 * fraction-to-the-boundary damping per component, a backtracking line search, and sub-steps only when a step won't
 * converge.
 */
public final class Network {
    public static final double G = 9.81;
    /** Port window half-width (share of the node's height) over which a port goes from drawing gas to liquid. */
    public static double BAND = 0.02;

    public static final class Node {
        public final double V, z, H;
        public final boolean fixed;
        /** Species masses (kg; water and air: m[0], m[1]) and internal energy (J). */
        public final double[] m;
        public double U;
        /** Heat into it (W), applied each step (free nodes). */
        public double heat;
        /** Its thermodynamics: the last solved state, what its ports draw, and their slopes. */
        final NodeFluid fluid;

        /** Its species (null: water and air, the WaterAir fast path). */
        final Species[] species;

        /** A node of water and air. */
        public Node(double V, double z, double H, boolean fixed) {
            this(V, z, H, fixed, null);
        }

        /** A node of the given species (MixFluid; null: water and air through WaterAirFluid). */
        public Node(double V, double z, double H, boolean fixed, Species[] species) {
            this.V = V;
            this.z = z;
            this.H = H;
            this.fixed = fixed;
            this.species = species;
            this.fluid = species == null ? new WaterAirFluid(V, H, fixed) : new MixFluid(species, V, H, fixed);
            this.m = new double[fluid.species()];
        }

        /** Fill a node of any species: masses (kg, its species' order), all at temperature T. */
        public Node fillMasses(double[] masses, double T) {
            System.arraycopy(masses, 0, m, 0, m.length);
            if (fluid instanceof MixFluid mf) {
                mf.T = T;
                U = mf.energyOf(m, T);
            } else {
                U = WaterAir.energyAt(m[0], m[1], V, T);
                waterAir().tHint = T;
            }
            update();
            return this;
        }

        /** Fill to water mass mw (all of it at temperature T) and air ma (a water/air node). */
        public Node fill(double mw, double ma, double T) {
            return fillMasses(new double[]{mw, ma}, T);
        }

        void update() {
            fluid.update(m, U);
        }

        /** Its water/air state (tests, a water/air node). */
        WaterAirFluid waterAir() {
            return (WaterAirFluid) fluid;
        }

        public double mw() {
            return m[0];
        }

        public double ma() {
            return m[1];
        }

        /** All its masses (kg). */
        public double mass() {
            double t = 0;
            for (double x : m) t += x;
            return t;
        }

        public double P() {
            return fluid.P();
        }

        public double T() {
            return fluid.T();
        }

        public double level() {
            return fluid.level();
        }

        public double liquid() {
            return waterAir().liquid();
        }
    }

    public enum Kind { PIPE, CHECK }

    public static final class Edge {
        public final Node a, b;
        public final double fa, fb;
        public double g, boost;
        public Kind kind = Kind.PIPE;
        /**
         * Counter-current exchange (m^3 s / kg, 0 for none): where a's port is at its floor and b's at its ceiling (a over
         * b), liquid at a's port and gas at b's swap places, equal volumes Q = exchange x (rho_liquid - rho_gas) g x
         * (liquid at a's port) x (gas at b's port) a second. Volume-neutral, so the pressure flow doesn't see it; it lets a
         * stack of tanks drain liquid down while gas comes up through one coupling (one stream an edge can't).
         */
        public double exchange;
        /**
         * Heat conductance between the two nodes (W/K, 0 for none): a pipe wall, a heat exchanger's plates. It acts in full
         * between nodes holding at least about M_CONTACT kg/m^3, fading to nothing for an empty one (wetted(): with
         * nothing to hold it, a near-empty node's temperature answers a joule by thousands of kelvin, and Newton failed).
         */
        public double conductance;
        /** The last step's flow (kg/s, a to b positive). */
        public double flow;

        Edge(Node a, double fa, Node b, double fb, double g) {
            this.a = a;
            this.fa = fa;
            this.b = b;
            this.fb = fb;
            this.g = g;
        }
    }

    public final List<Node> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();

    public Node add(Node n) {
        if (!nodes.isEmpty() && !java.util.Arrays.equals(nodes.get(0).species, n.species))
            throw new IllegalArgumentException("one network, one species list (and one implementation)");
        nodes.add(n);
        return n;
    }

    /** Join node a's port at share fa of its height to b's at fb. The two must be at one height (no head on an edge). */
    public Edge connect(Node a, double fa, Node b, double fb, double g) {
        double za = a.z + fa * a.H, zb = b.z + fb * b.H;
        if (Math.abs(za - zb) > 1e-6) throw new IllegalArgumentException("ports at " + za + " and " + zb + ": they must meet at one height");
        Edge e = new Edge(a, fa, b, fb, g);
        edges.add(e);
        return e;
    }

    // ---- smooth pieces

    /** The contents (kg/m^3) at which conduction is half on: about air at 1 bar. */
    public static double M_CONTACT = 1;

    /** How much of an edge's conductance a node's contents carry: m / (m + M_CONTACT V). */
    static double wetted(Node n) {
        double m = 0, m0 = M_CONTACT * n.V;
        for (double x : n.m) m += Math.max(0, x);
        return m / (m + m0);
    }

    /** d wetted / d mass. */
    static double wettedSlope(Node n) {
        double m = 0, m0 = M_CONTACT * n.V;
        for (double x : n.m) m += Math.max(0, x);
        return m0 / ((m + m0) * (m + m0));
    }

    /** Heat from a to b (W). */
    static double conduction(Edge e) {
        return e.conductance * wetted(e.a) * wetted(e.b) * (e.a.T() - e.b.T());
    }

    /** A C1 step from 0 (x <= -1) to 1 (x >= 1). */
    static double smoothStep(double x) {
        if (x <= -1) return 0;
        if (x >= 1) return 1;
        double t = 0.5 * (x + 1);
        return t * t * (3 - 2 * t);
    }

    static double stepSlope(double x) {
        if (x <= -1 || x >= 1) return 0;
        double t = 0.5 * (x + 1);
        return 0.5 * 6 * t * (1 - t);
    }

    /** Port pressure of node n at share f. */
    static double portP(Node n, double f) {
        return n.fluid.portP(f);
    }

    /** The exchange's fluxes (each component, a to b; negative: b to a) at the current states (Edge.exchange). */
    static double[] exchangeFlux(Edge e) {
        int C = e.a.m.length + 1;
        double[] f = new double[C];
        if (e.exchange <= 0) return f;
        NodeFluid a = e.a.fluid, b = e.b.fluid;
        double wl = a.liquidWindow(e.fa), wg = 1 - b.liquidWindow(e.fb);
        double rhoG = b.rhoGas(), rhoL = a.rhoLiquid(e.fa);
        double Q = e.exchange * Math.max(0, rhoL - rhoG) * G * wl * wg;
        if (Q <= 0) return f;
        // liquid down: Q rhoL of a's liquid; gas up: Q of b's gas
        double gasM = Q * rhoG;
        double[] liq = a.liquidDraw(e.fa), gas = b.gasDraw();
        for (int k = 0; k < C; k++) f[k] = Q * rhoL * liq[k] - gasM * gas[k];
        return f;
    }

    /** The edge's mass flow at the current states. */
    static double flow(Edge e) {
        double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost;
        return e.kind == Kind.CHECK ? e.g * Math.max(0, dp) : e.g * dp;
    }

    /**
     * A pipe's flow as two streams (0: strictly upwind instead), q+ = g SPLIT_DP softplus(dp / SPLIT_DP) from a
     * carrying a's draw and q- = q+ - q from b carrying b's, so the net flow is exactly q but what it carries changes
     * smoothly through q = 0 (strictly upwind, a pipe at rest between a liquid port and a gas port flips between carrying
     * water and air, and Newton stalled there). At rest the streams are g SPLIT_DP ln 2 each way: equal masses, so
     * nothing moves where both ports draw the same; where one draws liquid and the other gas, they swap slowly (liquid
     * runs into a dry branch below the surface, its gas bubbles out), as they would. 100 random networks x 3000 steps,
     * failed steps without / with conduction on every edge: strict upwind 2 / 44, 1 Pa 2 / 32 (once the slopes were
     * exact), 10 Pa 0 / 4, 100 Pa 0 / 1. Narrower bands fail because a dry node's pressure moves ~170 Pa per microgram
     * of vapour, so Newton's steps jump across a 1 Pa band (09-k-check.md).
     */
    public static double SPLIT_DP = 10;

    /** The two streams {q+ from a, q- from b} and their slopes in dp. */
    static double[] streams(Edge e) {
        double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost;
        if (e.kind == Kind.CHECK) return dp > 0 ? new double[]{e.g * dp, 0, e.g, 0} : new double[4];
        if (SPLIT_DP > 0) {
            double x = dp / SPLIT_DP;
            double sp = x > 30 ? x : x < -30 ? Math.exp(x) : Math.log1p(Math.exp(x));
            double sig = x > 30 ? 1 : x < -30 ? Math.exp(x) : 1 / (1 + Math.exp(-x));
            double qp = e.g * SPLIT_DP * sp;
            return new double[]{qp, qp - e.g * dp, e.g * sig, e.g * sig - e.g};
        }
        return dp >= 0 ? new double[]{e.g * dp, 0, e.g, 0} : new double[]{0, -e.g * dp, 0, -e.g};
    }

    // ---- the step

    /** Counters for tests and measurements. */
    public static final class Stats {
        public long steps, iterations, substeps, failures, solves, stalls;
        /** The last failed solve's residual norms by iteration (tests). */
        public final List<Double> lastFail = new ArrayList<>();
        /** The last failed solve's scaled residual by row (tests). */
        public double[] lastRows = new double[0];
        final List<Double> norms = new ArrayList<>();
    }

    public final Stats stats = new Stats();
    public static int MAX_ITERS = 25, MAX_DEPTH = 5;

    /** One step of dt seconds. */
    public void step(double dt) {
        stats.steps++;
        List<Node> free = new ArrayList<>();
        for (Node n : nodes) if (!n.fixed) free.add(n);
        double[] y0 = pack(free);
        if (!solve(free, y0, dt)) {
            // (no convergence: two half steps, each from where the last ended)
            restore(free, y0);
            substep(free, dt, 1);
        }
        for (Edge e : edges) e.flow = flow(e);
    }

    private void substep(List<Node> free, double dt, int depth) {
        stats.substeps++;
        for (int half = 0; half < 2; half++) {
            double[] y = pack(free);
            if (!solve(free, y, dt / 2)) {
                restore(free, y);
                if (depth < MAX_DEPTH) substep(free, dt / 2, depth + 1);
                else {
                    stats.failures++;
                    solve(free, y, dt / 2);   // (its best iterate: still conservative)
                }
            }
        }
    }

    /** Components per node: the species masses, then the energy (one count for the whole network). */
    private int C() {
        return nodes.isEmpty() ? 3 : nodes.get(0).m.length + 1;
    }

    private double[] pack(List<Node> free) {
        int C = C(), S = C - 1;
        double[] y = new double[C * free.size()];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            for (int k = 0; k < S; k++) y[C * i + k] = n.m[k];
            y[C * i + S] = n.U;
        }
        return y;
    }

    private void restore(List<Node> free, double[] y) {
        set(free, y);
    }

    private void set(List<Node> free, double[] y) {
        int C = C(), S = C - 1;
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            for (int k = 0; k < S; k++) n.m[k] = y[C * i + k];
            n.U = y[C * i + S];
            n.update();
        }
    }

    /** The residual R = Y' - Y0 - dt (fluxes in + heat) at the nodes' current states (each node's C rows). */
    private double[] residual(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        int C = C(), S = C - 1;
        double[] r = new double[C * free.size()];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            for (int k = 0; k < S; k++) r[C * i + k] = n.m[k] - y0[C * i + k];
            r[C * i + S] = n.U - y0[C * i + S] - dt * n.heat;
        }
        for (Edge e : edges) {
            if (e.conductance > 0) {
                double h = conduction(e);
                Integer ia = idx.get(e.a), ib = idx.get(e.b);
                if (ia != null) r[C * ia + S] += dt * h;
                if (ib != null) r[C * ib + S] -= dt * h;
            }
            if (e.exchange > 0) {
                double[] x = exchangeFlux(e);
                Integer ia = idx.get(e.a), ib = idx.get(e.b);
                for (int k = 0; k < C; k++) {
                    if (ia != null) r[C * ia + k] += dt * x[k];
                    if (ib != null) r[C * ib + k] -= dt * x[k];
                }
            }
            double[] qs = streams(e);
            if (qs[0] == 0 && qs[1] == 0) continue;
            double[] ca = qs[0] != 0 ? e.a.fluid.draw(e.fa) : null, cb = qs[1] != 0 ? e.b.fluid.draw(e.fb) : null;
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            for (int k = 0; k < C; k++) {
                double f = (ca != null ? qs[0] * ca[k] : 0) - (cb != null ? qs[1] * cb[k] : 0);
                if (ia != null) r[C * ia + k] += dt * f;   // (a loses q c)
                if (ib != null) r[C * ib + k] -= dt * f;
            }
        }
        return r;
    }

    /** dR/dY' at the current states. */
    private Sparse jacobian(List<Node> free, java.util.Map<Node, Integer> idx, double dt) {
        int C = C(), S = C - 1;
        int M = C * free.size();
        Sparse J = new Sparse(M);
        for (int i = 0; i < M; i++) J.add(i, i, 1);
        for (Edge e : edges) {
            // (every entry an edge can touch is added, zeros too: skipping zeros let entries come and go, and each new
            // one rebuilt the sparse LU's symbolic factorisation, most steps at 2,000 nodes)
            // F_k = q+ ca_k - q- cb_k; dF_k = (dq+ ca_k - dq- cb_k) (dPa - dPb) + q+ dca_k (a) - q- dcb_k (b)
            double[] qs = streams(e);
            double[] ca = e.a.fluid.draw(e.fa), cb = e.b.fluid.draw(e.fb);
            double[][] dca = qs[0] != 0 ? e.a.fluid.drawSlope(e.fa) : null, dcb = qs[1] != 0 ? e.b.fluid.drawSlope(e.fb) : null;
            double[] dpa = e.a.fluid.portPSlope(e.fa), dpb = e.b.fluid.portPSlope(e.fb);
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            for (int k = 0; k < C; k++) {
                double dFdp = qs[2] * ca[k] - qs[3] * cb[k];
                for (int side = 0; side < 2; side++) {
                    Integer col = side == 0 ? ia : ib;
                    if (col == null) continue;
                    double[] dpx = side == 0 ? dpa : dpb;
                    double sign = side == 0 ? 1 : -1;
                    double[][] dc = side == 0 ? dca : dcb;
                    double qn = side == 0 ? qs[0] : -qs[1];
                    for (int j2 = 0; j2 < C; j2++) {
                        double dF = dFdp * sign * dpx[j2] + (dc != null ? qn * dc[k][j2] : 0);
                        if (ia != null) J.add(C * ia + k, C * col + j2, dt * dF);
                        if (ib != null) J.add(C * ib + k, C * col + j2, -dt * dF);
                    }
                }
            }
        }
        // heat conduction: H = C wa wb (Ta - Tb), through each node's temperature slope and its contents
        for (Edge e : edges) {
            if (e.conductance <= 0) continue;
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            double wa = wetted(e.a), wb = wetted(e.b), dTab = e.a.T() - e.b.T();
            for (int side = 0; side < 2; side++) {
                Integer col = side == 0 ? ia : ib;
                if (col == null) continue;
                Node n = side == 0 ? e.a : e.b;
                double[] dT = n.fluid.tSlope();
                double sign = side == 0 ? 1 : -1, wOther = side == 0 ? wb : wa, dw = wettedSlope(n);
                for (int j = 0; j < C; j++) {
                    double dm = j < S && n.m[j] > 0 ? dw : 0;
                    double dH = e.conductance * (wa * wb * sign * dT[j] + wOther * dm * dTab);
                    if (ia != null) J.add(C * ia + S, C * col + j, dt * dH);
                    if (ib != null) J.add(C * ib + S, C * col + j, -dt * dH);
                }
            }
        }
        // the exchange edges: their slopes by central differences over their two nodes' contents (local and cheap)
        for (Edge e : edges) {
            if (e.exchange <= 0) continue;
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            for (int side = 0; side < 2; side++) {
                Node n = side == 0 ? e.a : e.b;
                Integer col = side == 0 ? ia : ib;
                if (col == null) continue;
                double[] y = new double[C];
                for (int k = 0; k < S; k++) y[k] = n.m[k];
                y[S] = n.U;
                for (int j = 0; j < C; j++) {
                    double h = j == S ? 1e-6 * Math.max(1, Math.abs(y[S])) : Math.max(1e-12, 1e-6 * y[j]);
                    if (j < S && y[j] - h < 0) h = Math.max(y[j], 1e-15);
                    double[] fp = perturbed(n, j, h, e), fm = perturbed(n, j, -h, e);
                    for (int k = 0; k < C; k++) {
                        double dF = (fp[k] - fm[k]) / (2 * h);
                        if (ia != null) J.add(C * ia + k, C * col + j, dt * dF);
                        if (ib != null) J.add(C * ib + k, C * col + j, -dt * dF);
                    }
                }
            }
        }
        return J;
    }

    /** exchangeFlux(e) with node n's component j moved by d (n restored after). */
    private static double[] perturbed(Node n, int j, double d, Edge e) {
        int S = n.m.length;
        double keep = j < S ? n.m[j] : n.U;
        Object state = n.fluid.saveState();
        if (j < S) n.m[j] += d;
        else n.U += d;
        n.update();
        double[] f = exchangeFlux(e);
        if (j < S) n.m[j] = keep;
        else n.U = keep;
        n.fluid.restoreState(state);
        return f;
    }

    private final java.util.Map<Long, SparseLU> luCache = new java.util.HashMap<>();

    /** Row scales: a node's masses by its contents, its energy by its contents times a typical specific energy. */
    private double[] scales(List<Node> free, double[] y0) {
        int C = C(), S = C - 1;
        double[] s = new double[y0.length];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            double m = Math.max(1e-6 * n.V, massOf(y0, C * i, S));
            for (int k = 0; k < S; k++) s[C * i + k] = 1 / m;
            s[C * i + S] = 1 / (m * 1e5);
        }
        return s;
    }

    /** The masses of the node whose rows start at at. */
    private static double massOf(double[] y, int at, int S) {
        double m = 0;
        for (int k = 0; k < S; k++) m += y[at + k];
        return m;
    }

    private static double[] scaled(double[] r, double[] s) {
        double[] o = new double[r.length];
        for (int i = 0; i < r.length; i++) o[i] = Math.round(r[i] * s[i] * 1e6) / 1e6;
        return o;
    }

    private static double norm(double[] r, double[] s) {
        double x = 0;
        for (int i = 0; i < r.length; i++) x = Math.max(x, Math.abs(r[i] * s[i]));
        return x;
    }

    public static double TOL = 1e-10, STALL_OK = 1e-6;
    /** Fraction-to-the-boundary floor (share of the node's contents): components under it are damped on their own. */
    public static double FLOOR = 1e-4;
    /** Tests: a trace of each Newton iteration (null: off). */
    public static StringBuilder DEBUG = null;

    /**
     * End the step conservatively whatever Newton reached: the contents become the start's plus dt times the fluxes
     * at the final iterate (Y' = Y0 + dt F(Y'_iter)), so mass and energy balance exactly even when the residual isn't
     * zero; converged, this moves Y' by no more than the residual.
     */
    private void finish(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        int C = C(), S = C - 1;
        // each transfer: {from, to (indices into free, -1 for a fixed node), component, amount moved over dt}
        List<double[]> moves = new ArrayList<>();
        for (Edge e : edges) {
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            int a = ia == null ? -1 : ia, b = ib == null ? -1 : ib;
            if (e.conductance > 0) {
                double h = dt * conduction(e);
                if (h != 0) moves.add(h > 0 ? new double[]{a, b, S, h} : new double[]{b, a, S, -h});
            }
            if (e.exchange > 0) {
                double[] x = exchangeFlux(e);
                for (int k = 0; k < C; k++) if (x[k] != 0) moves.add(x[k] > 0 ? new double[]{a, b, k, dt * x[k]} : new double[]{b, a, k, -dt * x[k]});
            }
            double[] qs = streams(e);
            for (int side = 0; side < 2; side++) {
                if (qs[side] == 0) continue;
                double[] c = side == 0 ? e.a.fluid.draw(e.fa) : e.b.fluid.draw(e.fb);
                for (int k = 0; k < C; k++) {
                    double f = dt * qs[side] * c[k];   // (from a to b for side 0, from b to a for side 1)
                    if (side == 1) f = -f;
                    if (f != 0) moves.add(f > 0 ? new double[]{a, b, k, f} : new double[]{b, a, k, -f});
                }
            }
        }
        // the limiter: a node can't send more of a mass than it started with plus what it receives (Newton's tolerance
        // is relative to the node's whole contents, so a dry node's water could end 1e-10 of its air below zero);
        // its sends of that mass are scaled down, which keeps every transfer in flux form and so conserves exactly
        int M = y0.length;
        double[] scale = new double[M];
        java.util.Arrays.fill(scale, 1);
        double[] y = new double[M];
        for (int pass = 0; pass < 4; pass++) {
            double[] in = new double[M], out = new double[M];
            for (double[] mv : moves) {
                int from = (int) mv[0], to = (int) mv[1], k = (int) mv[2];
                double s = from >= 0 && k < S ? scale[C * from + k] : 1;
                if (from >= 0) out[C * from + k] += mv[3] * s;
                if (to >= 0) in[C * to + k] += mv[3] * s;
            }
            boolean changed = false;
            for (int i = 0; i < M; i++) {
                if (i % C == S || out[i] == 0) continue;
                double have = Math.max(0, y0[i]) + in[i];
                if (out[i] > have * (1 + 1e-15)) {
                    scale[i] *= have / out[i];
                    changed = true;
                }
            }
            if (!changed && pass > 0) break;
        }
        for (int i = 0; i < free.size(); i++) {
            for (int k = 0; k < S; k++) y[C * i + k] = y0[C * i + k];
            y[C * i + S] = y0[C * i + S] + dt * free.get(i).heat;
        }
        for (double[] mv : moves) {
            int from = (int) mv[0], to = (int) mv[1], k = (int) mv[2];
            double amt = mv[3] * (from >= 0 && k < S ? scale[C * from + k] : 1);
            if (from >= 0) y[C * from + k] -= amt;
            if (to >= 0) y[C * to + k] += amt;
        }
        set(free, y);
    }

    /**
     * Newton's starting point: an explicit predictor, Y* = Y0 + dt F(Y0), each node's change scaled down as far as keeps
     * its masses non-negative (to 90 % of the way). Started from Y0, an empty riser a pump fed was linearised as a node of
     * air (its pressure answering heat as air's does), so Newton refused the water while admitting its energy and
     * deadlocked whatever the step; from the predicted state the riser holds water and Newton converges.
     */
    private void predict(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        int C = C(), S = C - 1;
        double[] r = residual(free, idx, y0, dt);   // (at Y = Y0: R = -dt F(Y0))
        double[] y = y0.clone();
        for (int i = 0; i < free.size(); i++) {
            double f = 1;
            for (int k = 0; k < S; k++) {
                double m = y0[C * i + k], dm = -r[C * i + k];
                if (dm < 0 && m + dm < 0) f = Math.min(f, m > 0 ? 0.9 * m / -dm : 0);
            }
            for (int k = 0; k < C; k++) y[C * i + k] = y0[C * i + k] - f * r[C * i + k];
        }
        set(free, y);
    }

    /** Newton from the nodes' current states (y0 = the step's start); true if it converged (the nodes then hold Y'). */
    private boolean solve(List<Node> free, double[] y0, double dt) {
        java.util.Map<Node, Integer> idx0 = new java.util.IdentityHashMap<>();
        for (int i = 0; i < free.size(); i++) idx0.put(free.get(i), i);
        predict(free, idx0, y0, dt);
        boolean ok = newton(free, y0, dt);
        finish(free, idx0, y0, dt);
        return ok;
    }

    private boolean newton(List<Node> free, double[] y0, double dt) {
        int C = C(), S = C - 1;
        java.util.Map<Node, Integer> idx = new java.util.IdentityHashMap<>();
        for (int i = 0; i < free.size(); i++) idx.put(free.get(i), i);
        double[] s = scales(free, y0);
        double[] y = pack(free);
        double[] r = residual(free, idx, y0, dt);
        double nr = norm(r, s);
        stats.norms.clear();
        stats.norms.add(nr);
        for (int it = 0; it < MAX_ITERS; it++) {
            if (nr < TOL) return true;
            stats.iterations++;
            Sparse J = jacobian(free, idx, dt);
            double[] minus = new double[r.length];
            for (int i = 0; i < r.length; i++) minus[i] = -r[i];
            stats.solves++;
            double[] d = J.solve(minus, luCache);
            // fraction to the boundary: no mass goes below zero in a trial
            // (a component already at or near zero that the step would take lower is held where it is: projecting it,
            // rather than shrinking the whole step to nothing for it, as a node with no air at all did)
            // (held components are taken out of the system and the rest solved again without them: zeroing them in the
            // step alone left a direction the residual didn't fall along, and an empty riser's first step stalled)
            // (repeated until no further component at zero would go lower: holding some can turn others)
            for (int round = 0; round < 6; round++) {
                boolean held = false;
                for (int i = 0; i < free.size(); i++)
                    for (int k = 0; k < S; k++) {
                        double tiny = 1e-12 * Math.max(1e-3, massOf(y, C * i, S));
                        if (d[C * i + k] < 0 && y[C * i + k] <= tiny) {
                            if (r[C * i + k] < 0) {
                                // (its own balance wants it up: not held but stepped along its diagonal, -r)
                                d[C * i + k] = -r[C * i + k];
                                continue;
                            }
                            J.unitRow(C * i + k);
                            minus[C * i + k] = 0;
                            held = true;
                        }
                    }
                if (!held) break;
                stats.solves++;
                d = J.solve(minus, luCache);
            }
            // fraction to the boundary with a floor: a component holding more than 1e-4 of its node's contents
            // limits the whole step to 99 % of the way to zero (keeping Newton's direction); one with less is damped on
            // its own (shrinking the whole step for 3 mg of water in a tank stalled a pump's first step into an empty
            // riser; damping every component on its own broke the direction elsewhere and the scenarios crawled)
            double alpha = 1;
            for (int i = 0; i < free.size(); i++)
                for (int k = 0; k < S; k++) {
                    double m = y[C * i + k], dm = d[C * i + k];
                    if (!(dm < 0 && m + dm < 0)) continue;
                    double floor = FLOOR * Math.max(1e-9, massOf(y, C * i, S));
                    if (m > floor) alpha = Math.min(alpha, 0.99 * m / -dm);
                    else d[C * i + k] = -0.99 * Math.max(0, m);
                }
            if (DEBUG != null) {
                DEBUG.append(String.format("it %d nr %.3e alpha0 %.3e%n  y %s%n  r*s %s%n  d %s%n", it, nr, alpha, java.util.Arrays.toString(y),
                    java.util.Arrays.toString(scaled(r, s)), java.util.Arrays.toString(d)));
            }
            boolean accepted = false;
            for (int ls = 0; ls < 12; ls++, alpha *= 0.5) {
                double[] yt = y.clone();
                for (int i = 0; i < y.length; i++) yt[i] += alpha * d[i];
                set(free, yt);
                double[] rt = residual(free, idx, y0, dt);
                double nt = norm(rt, s);
                if (DEBUG != null && ls < 3) DEBUG.append(String.format("    ls %d alpha %.3e nt %.3e r*s %s%n", ls, alpha, nt, java.util.Arrays.toString(scaled(rt, s))));
                if (nt < (1 - 1e-4 * alpha) * nr || nt < TOL) {
                    y = yt;
                    r = rt;
                    nr = nt;
                    accepted = true;
                    break;
                }
            }
            stats.norms.add(nr);
            if (!accepted) {
                set(free, y);
                if (DEBUG != null) DEBUG.append("  stalled; scaled Jacobian errors: ").append(scaledJacobianErrors(free, idx, y0, dt, s)).append("\n");
                // (stalled at the precision floor: a liquid-full node turns a rounding error in its mass into a pressure,
                // so the residual can't go lower; the step still ends conservatively)
                if (nr < STALL_OK) {
                    stats.stalls++;
                    return true;
                }
                if (nr >= 1e3 * TOL) {
                    stats.lastRows = new double[r.length];
                    for (int i = 0; i < r.length; i++) stats.lastRows[i] = r[i] * s[i];
                    stats.lastFail.clear();
                    stats.lastFail.addAll(stats.norms);
                    stats.lastFail.add(-alpha);
                }
                return nr < 1e3 * TOL;
            }
        }
        if (nr >= TOL) {
            stats.lastFail.clear();
            stats.lastFail.addAll(stats.norms);
        }
        return nr < TOL;
    }

    /** Tests: the Jacobian at the nodes' current states against central differences of the residual; the worst row-relative error and where. */
    String checkJacobian(double dt) {
        List<Node> free = new ArrayList<>();
        for (Node n : nodes) if (!n.fixed) free.add(n);
        java.util.Map<Node, Integer> idx = new java.util.IdentityHashMap<>();
        for (int i = 0; i < free.size(); i++) idx.put(free.get(i), i);
        int C = C();
        double[] y = pack(free), y0 = y.clone();
        double[][] J = jacobian(free, idx, dt).dense();
        int M = y.length;
        double worst = 0;
        String at = "";
        for (int j = 0; j < M; j++) {
            double h = j % C == C - 1 ? 1e-6 * Math.max(1, Math.abs(y[j])) : Math.max(1e-12, 1e-6 * y[j]);
            if (j % C != C - 1 && y[j] - h < 0) continue;
            double[] yp = y.clone(), ym = y.clone();
            yp[j] += h;
            ym[j] -= h;
            set(free, yp);
            double[] rp = residual(free, idx, y0, dt);
            set(free, ym);
            double[] rm = residual(free, idx, y0, dt);
            set(free, y);
            for (int i = 0; i < M; i++) {
                double fd = (rp[i] - rm[i]) / (2 * h);
                double err = Math.abs(fd - J[i][j]) / Math.max(1e-3, Math.max(Math.abs(fd), Math.abs(J[i][j])));
                if (Math.abs(fd - J[i][j]) > 1e-9 && err > worst) {
                    worst = err;
                    at = String.format("row %d col %d: J %.6g FD %.6g", i, j, J[i][j], fd);
                }
            }
        }
        return String.format("%.3g at %s", worst, at);
    }

    /** Debugging: the five largest |J - FD| entries at the current states, scaled by the row and column scales. */
    private String scaledJacobianErrors(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt, double[] sc) {
        int C = C();
        double[] y = pack(free);
        double[][] J = jacobian(free, idx, dt).dense();
        int M = y.length;
        List<double[]> errs = new ArrayList<>();
        for (int j = 0; j < M; j++) {
            double h = j % C == C - 1 ? 1e-6 * Math.max(1, Math.abs(y[j])) : Math.max(1e-12, 1e-6 * y[j]);
            if (j % C != C - 1 && y[j] - h < 0) continue;
            double[] yp = y.clone(), ym = y.clone();
            yp[j] += h;
            ym[j] -= h;
            set(free, yp);
            double[] rp = residual(free, idx, y0, dt);
            set(free, ym);
            double[] rm = residual(free, idx, y0, dt);
            set(free, y);
            for (int i = 0; i < M; i++) {
                double fd = (rp[i] - rm[i]) / (2 * h);
                errs.add(new double[]{Math.abs(fd - J[i][j]) * sc[i] / sc[j], i, j, J[i][j], fd});
            }
        }
        errs.sort((a, b) -> Double.compare(b[0], a[0]));
        StringBuilder o = new StringBuilder();
        for (int k = 0; k < 5 && k < errs.size(); k++) {
            double[] e = errs.get(k);
            o.append(String.format("[%.2e r%d c%d J %.4g FD %.4g] ", e[0], (int) e[1], (int) e[2], e[3], e[4]));
        }
        return o.toString();
    }
}
