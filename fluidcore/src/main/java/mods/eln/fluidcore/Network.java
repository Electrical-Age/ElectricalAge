package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;

/**
 * A network of fluid volumes and the flows between them, stepped fully implicitly (board #61, 08-core-plan.md M2).
 * <p>
 * Each free node holds water mw, air ma (kg) and internal energy U (J); a fixed node is a boundary (atmosphere,
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
        public double mw, ma, U;
        /** Heat into it (W), applied each step (free nodes). */
        public double heat;
        /** The last solved state (outputs and slopes), and its temperature (the next solve's hint). */
        WaterAir.Split split;
        double[] out;
        double[][] slopes;
        double tHint = 300;

        public Node(double V, double z, double H, boolean fixed) {
            this.V = V;
            this.z = z;
            this.H = H;
            this.fixed = fixed;
        }

        /** Fill to water mass mw (all of it at temperature T) and air ma. */
        public Node fill(double mw, double ma, double T) {
            this.mw = mw;
            this.ma = ma;
            this.U = WaterAir.energyAt(mw, ma, V, T);
            this.tHint = T;
            update();
            return this;
        }

        void update() {
            split = split == null ? WaterAir.solve(mw, ma, U, V, tHint) : WaterAir.solve(mw, ma, U, V, split);
            tHint = split.T();
            out = WaterAir.outputs(split, V);
            slopes = null;   // (taken when the Jacobian needs them: slopes())
        }

        double[][] slopes() {
            if (slopes == null) slopes = fixed ? new double[WaterAir.OUTPUTS][3] : WaterAir.outputSlopes(split, mw, ma, U, V);
            return slopes;
        }

        public double P() {
            return out[WaterAir.P];
        }

        public double T() {
            return split.T();
        }

        public double level() {
            return out[WaterAir.LEVEL];
        }

        public double liquid() {
            return out[WaterAir.ML];
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

    /** The liquid's head over a port at share f, for a level L (shares of H): rho g H softplus(L - f), smooth at the surface. */
    static double head(Node n, double L, double f) {
        double w = 0.25 * BAND;
        return G * n.H / WaterAir.V0 * WaterAir.softplus(L - f, w);
    }

    static double headSlope(Node n, double L, double f) {
        double w = 0.25 * BAND, x = (L - f) / w;
        double sig = x > 30 ? 1 : x < -30 ? Math.exp(x) : 1 / (1 + Math.exp(-x));
        return G * n.H / WaterAir.V0 * sig;
    }

    // ---- fluxes

    /** Port pressure of node n at share f, and its slope against n's (mw, ma, U). */
    static double portP(Node n, double f) {
        return n.P() + head(n, n.level(), f);
    }

    static double[] portPSlope(Node n, double f) {
        double hs = headSlope(n, n.level(), f);
        double[] d = new double[3];
        for (int j = 0; j < 3; j++) d[j] = n.slopes()[WaterAir.P][j] + hs * n.slopes()[WaterAir.LEVEL][j];
        return d;
    }

    /** What a kg drawn from node n's port at f carries: {water, air, energy} per kg. */
    /**
     * The window's argument: -1 where the liquid's surface is at the port (or lower), 1 a band above it. A port draws
     * liquid only once the surface is over it, so a dry node's floor port draws no water it hasn't got (centred on the
     * port, it drew half "liquid" from a dry node and drove its water negative); a port at the top counts as a band
     * lower, so a full node can push liquid out of it.
     */
    static double windowArg(Node n, double f) {
        return 2 * (n.level() - Math.min(f, 1 - BAND)) / BAND - 1;
    }

    static double[] draw(Node n, double f) {
        double xl = smoothStep(windowArg(n, f));
        double yv = n.out[WaterAir.YV], ya = n.out[WaterAir.YA];
        return new double[]{xl + (1 - xl) * yv, (1 - xl) * ya, xl * n.out[WaterAir.HL] + (1 - xl) * n.out[WaterAir.HG]};
    }

    /** Slopes of draw() against n's (mw, ma, U): [component][j]. */
    static double[][] drawSlope(Node n, double f) {
        double x = windowArg(n, f);
        double xl = smoothStep(x), dxl = 2 * stepSlope(x) / BAND;
        double yv = n.out[WaterAir.YV], ya = n.out[WaterAir.YA], hl = n.out[WaterAir.HL], hg = n.out[WaterAir.HG];
        double[][] d = new double[3][3];
        for (int j = 0; j < 3; j++) {
            double dL = dxl * n.slopes()[WaterAir.LEVEL][j], dy = n.slopes()[WaterAir.YV][j], da = n.slopes()[WaterAir.YA][j];
            d[0][j] = dL * (1 - yv) + (1 - xl) * dy;
            d[1][j] = -dL * ya + (1 - xl) * da;
            d[2][j] = dL * (hl - hg) + xl * n.slopes()[WaterAir.HL][j] + (1 - xl) * n.slopes()[WaterAir.HG][j];
        }
        return d;
    }


    /** The exchange's fluxes {water, air, energy} from a to b (negative: b to a) at the current states (Edge.exchange). */
    static double[] exchangeFlux(Edge e) {
        double[] f = new double[3];
        if (e.exchange <= 0) return f;
        Node a = e.a, b = e.b;
        double wl = smoothStep(windowArg(a, e.fa)), wg = 1 - smoothStep(windowArg(b, e.fb));
        double vgB = b.split.vg();
        double rhoG = (b.out[WaterAir.MV] + b.out[WaterAir.MA]) / vgB, rhoL = 1 / a.split.v();
        double Q = e.exchange * Math.max(0, rhoL - rhoG) * G * wl * wg;
        if (Q <= 0) return f;
        // liquid down: Q rhoL of a's liquid; gas up: Q of b's gas (its real contents' share: the cushion stays)
        double gasM = Q * rhoG;
        f[0] = Q * rhoL - gasM * b.out[WaterAir.YV];
        f[1] = -gasM * b.out[WaterAir.YA];
        f[2] = Q * rhoL * a.out[WaterAir.HL] - gasM * b.out[WaterAir.HG];
        return f;
    }

    /** The edge's mass flow at the current states. */
    static double flow(Edge e) {
        double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost;
        return e.kind == Kind.CHECK ? e.g * Math.max(0, dp) : e.g * dp;
    }

    // ---- the step

    /** Counters for tests and measurements. */
    public static final class Stats {
        public long steps, iterations, substeps, failures, solves;
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

    private static double[] pack(List<Node> free) {
        double[] y = new double[3 * free.size()];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            y[3 * i] = n.mw;
            y[3 * i + 1] = n.ma;
            y[3 * i + 2] = n.U;
        }
        return y;
    }

    private static void restore(List<Node> free, double[] y) {
        set(free, y);
    }

    private static void set(List<Node> free, double[] y) {
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            n.mw = y[3 * i];
            n.ma = y[3 * i + 1];
            n.U = y[3 * i + 2];
            n.update();
        }
    }

    /** The residual R = Y' - Y0 - dt (fluxes in + heat) at the nodes' current states (each node's 3 rows). */
    private double[] residual(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        double[] r = new double[3 * free.size()];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            r[3 * i] = n.mw - y0[3 * i];
            r[3 * i + 1] = n.ma - y0[3 * i + 1];
            r[3 * i + 2] = n.U - y0[3 * i + 2] - dt * n.heat;
        }
        for (Edge e : edges) {
            if (e.exchange > 0) {
                double[] x = exchangeFlux(e);
                Integer ia = idx.get(e.a), ib = idx.get(e.b);
                for (int k = 0; k < 3; k++) {
                    if (ia != null) r[3 * ia + k] += dt * x[k];
                    if (ib != null) r[3 * ib + k] -= dt * x[k];
                }
            }
            double q = flow(e);
            if (q == 0) continue;
            Node up = q > 0 ? e.a : e.b;
            double[] c = draw(up, q > 0 ? e.fa : e.fb);
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            for (int k = 0; k < 3; k++) {
                double f = q * c[k];
                if (ia != null) r[3 * ia + k] += dt * f;   // (a loses q c)
                if (ib != null) r[3 * ib + k] -= dt * f;
            }
        }
        return r;
    }

    /** dR/dY' at the current states. */
    private Sparse jacobian(List<Node> free, java.util.Map<Node, Integer> idx, double dt) {
        int M = 3 * free.size();
        Sparse J = new Sparse(M);
        for (int i = 0; i < M; i++) J.add(i, i, 1);
        for (Edge e : edges) {
            double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost;
            boolean open = e.kind != Kind.CHECK || dp > 0;
            double q = open ? e.g * dp : 0;
            boolean fromA = q > 0 || (q == 0 && dp >= 0);
            Node up = fromA ? e.a : e.b;
            double fUp = fromA ? e.fa : e.fb;
            double[] c = draw(up, fUp);
            double[][] dc = drawSlope(up, fUp);
            double[] dpa = portPSlope(e.a, e.fa), dpb = portPSlope(e.b, e.fb);
            Integer ia = idx.get(e.a), ib = idx.get(e.b), iu = idx.get(up);
            for (int k = 0; k < 3; k++) {
                // F_k = q c_k; dF_k = g c_k (dPa - dPb) + q dc_k (upstream)
                for (int side = 0; side < 2; side++) {
                    Integer col = side == 0 ? ia : ib;
                    if (col == null || !open) continue;
                    double[] dpx = side == 0 ? dpa : dpb;
                    double sign = side == 0 ? 1 : -1;
                    for (int j = 0; j < 3; j++) {
                        double dF = e.g * c[k] * sign * dpx[j];
                        if (ia != null) J.add(3 * ia + k, 3 * col + j, dt * dF);
                        if (ib != null) J.add(3 * ib + k, 3 * col + j, -dt * dF);
                    }
                }
                if (iu != null && q != 0)
                    for (int j = 0; j < 3; j++) {
                        double dF = q * dc[k][j];
                        if (ia != null) J.add(3 * ia + k, 3 * iu + j, dt * dF);
                        if (ib != null) J.add(3 * ib + k, 3 * iu + j, -dt * dF);
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
                double[] y = {n.mw, n.ma, n.U};
                for (int j = 0; j < 3; j++) {
                    double h = j == 2 ? 1e-6 * Math.max(1, Math.abs(y[2])) : Math.max(1e-12, 1e-6 * y[j]);
                    if (j < 2 && y[j] - h < 0) h = Math.max(y[j], 1e-15);
                    double[] fp = perturbed(n, j, h, e), fm = perturbed(n, j, -h, e);
                    for (int k = 0; k < 3; k++) {
                        double dF = (fp[k] - fm[k]) / (2 * h);
                        if (ia != null) J.add(3 * ia + k, 3 * col + j, dt * dF);
                        if (ib != null) J.add(3 * ib + k, 3 * col + j, -dt * dF);
                    }
                }
            }
        }
        return J;
    }

    /** exchangeFlux(e) with node n's component j moved by d (n restored after). */
    private static double[] perturbed(Node n, int j, double d, Edge e) {
        double mw = n.mw, ma = n.ma, U = n.U;
        var split = n.split;
        var out = n.out;
        var slopes = n.slopes;
        if (j == 0) n.mw += d;
        else if (j == 1) n.ma += d;
        else n.U += d;
        n.update();
        double[] f = exchangeFlux(e);
        n.mw = mw;
        n.ma = ma;
        n.U = U;
        n.split = split;
        n.out = out;
        n.slopes = slopes;
        n.tHint = split.T();
        return f;
    }

    private final java.util.Map<Long, SparseLU> luCache = new java.util.HashMap<>();

    /** Row scales: a node's masses by its contents, its energy by its contents times a typical specific energy. */
    private static double[] scales(List<Node> free, double[] y0) {
        double[] s = new double[y0.length];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            double m = Math.max(1e-6 * n.V, y0[3 * i] + y0[3 * i + 1]);
            s[3 * i] = s[3 * i + 1] = 1 / m;
            s[3 * i + 2] = 1 / (m * 1e5);
        }
        return s;
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
        double[] r = residual(free, idx, y0, dt);   // (R = Y' - Y0 - dt F, so Y0 + dt F = Y' - R)
        double[] y = pack(free);
        for (int i = 0; i < y.length; i++) y[i] -= r[i];
        set(free, y);
    }

    /**
     * Newton's starting point: an explicit predictor, Y* = Y0 + dt F(Y0), each node's change scaled down as far as keeps
     * its masses non-negative (to 90 % of the way). Started from Y0, an empty riser a pump fed was linearised as a node of
     * air (its pressure answering heat as air's does), so Newton refused the water while admitting its energy and
     * deadlocked whatever the step; from the predicted state the riser holds water and Newton converges.
     */
    private void predict(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        double[] r = residual(free, idx, y0, dt);   // (at Y = Y0: R = -dt F(Y0))
        double[] y = y0.clone();
        for (int i = 0; i < free.size(); i++) {
            double f = 1;
            for (int k = 0; k < 2; k++) {
                double m = y0[3 * i + k], dm = -r[3 * i + k];
                if (dm < 0 && m + dm < 0) f = Math.min(f, m > 0 ? 0.9 * m / -dm : 0);
            }
            for (int k = 0; k < 3; k++) y[3 * i + k] = y0[3 * i + k] - f * r[3 * i + k];
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
                    for (int k = 0; k < 2; k++) {
                        double tiny = 1e-12 * Math.max(1e-3, y[3 * i] + y[3 * i + 1]);
                        if (d[3 * i + k] < 0 && y[3 * i + k] <= tiny) {
                            if (r[3 * i + k] < 0) {
                                // (its own balance wants it up: not held but stepped along its diagonal, -r)
                                d[3 * i + k] = -r[3 * i + k];
                                continue;
                            }
                            J.unitRow(3 * i + k);
                            minus[3 * i + k] = 0;
                            held = true;
                        }
                    }
                if (!held) break;
                stats.solves++;
                d = J.solve(minus);
            }
            // fraction to the boundary with a floor: a component holding more than 1e-4 of its node's contents
            // limits the whole step to 99 % of the way to zero (keeping Newton's direction); one with less is damped on
            // its own (shrinking the whole step for 3 mg of water in a tank stalled a pump's first step into an empty
            // riser; damping every component on its own broke the direction elsewhere and the scenarios crawled)
            double alpha = 1;
            for (int i = 0; i < free.size(); i++)
                for (int k = 0; k < 2; k++) {
                    double m = y[3 * i + k], dm = d[3 * i + k];
                    if (!(dm < 0 && m + dm < 0)) continue;
                    double floor = FLOOR * Math.max(1e-9, y[3 * i] + y[3 * i + 1]);
                    if (m > floor) alpha = Math.min(alpha, 0.99 * m / -dm);
                    else d[3 * i + k] = -0.99 * Math.max(0, m);
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
                // (stalled at the precision floor: a liquid-full node turns a rounding error in its mass into a pressure,
                // so the residual can't go lower; the step still ends conservatively)
                if (nr < STALL_OK) return true;
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
        double[] y = pack(free), y0 = y.clone();
        double[][] J = jacobian(free, idx, dt).dense();
        int M = y.length;
        double worst = 0;
        String at = "";
        for (int j = 0; j < M; j++) {
            double h = j % 3 == 2 ? 1e-6 * Math.max(1, Math.abs(y[j])) : Math.max(1e-12, 1e-6 * y[j]);
            if (j % 3 != 2 && y[j] - h < 0) continue;
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
}
