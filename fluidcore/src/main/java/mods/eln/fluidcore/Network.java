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
        /**
         * A heat link through its wall (W/K) to a wall held at wallT (K) for the step: heat wallUA (wallT - T) in, inside
         * the implicit step, so a near-empty node can't overshoot past its wall's temperature (a boiler's shell, a
         * condenser's cooling, a tank's loss to the air). wallJ: the heat it passed over the last step (J, + in).
         */
        public double wallUA, wallT, wallJ;
        double pendingWall;
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

        /** Solve its state from m and U again: whoever changes those from outside (the game's adapter) calls it. */
        public void update() {
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

        // ---- reads for the game (its devices and readouts), from the solved state

        /** Species j's liquid and vapour (or gas) mass (kg), and its partial pressure in the gas (Pa). */
        public double liquidMass(int j) {
            return fluid.liquidMass(j);
        }

        public double vapourMass(int j) {
            return fluid.vapourMass(j);
        }

        public double partialP(int j) {
            return fluid.partialP(j);
        }

        /** The pressure at a port at share f of its height, the liquid's head over it included. */
        public double portP(double f) {
            return fluid.portP(f);
        }

        /** The internal energy (J) its masses would have at T, in equilibrium. */
        public double energyAt(double T) {
            return fluid.energyAt(m, T);
        }
    }

    public enum Kind { PIPE, CHECK, PUMP, TURBINE, REGULATOR }

    public static final class Edge {
        public final Node a, b;
        public final double fa, fb;
        public double g, boost;
        /**
         * The return stream's conductance (from b; NaN: g, the same both ways). The game gives each stream the mobility of
         * what its own port draws (rho / mu), so liquid and gas move by their own measure.
         */
        public double gBack = Double.NaN;
        /**
         * The height of a's port over b's (m): the two draw heights. Each stream pays the head of its own fluid over it
         * (Network.streams). The game's ports draw at one height and attach at another (a boiler's dome flange underneath
         * draws at its top), and lumped runs and reservoirs meet ports at other heights.
         */
        public final double dz;
        /** With FREEZE_HEAD: the densities each stream's head is weighed by, taken at the step's start (NaN: live). */
        double rhoA = Double.NaN, rhoB = Double.NaN;
        public Kind kind = Kind.PIPE;
        /**
         * PUMP: a centrifugal pump from a to b. qMax (kg/s at full speed against no head), h0 its shut-off head (Pa), speed
         * (0..1: scales the flow, the shut-off head stays, as the game's pumps), eta its efficiency. Its flow against the
         * back-pressure dp (b's port over a's, its own head included) is speed qMax sqrt(1 - dp / h0), smoothed over
         * PUMP_W of h0 at the shut-off so its slope stays finite (Network.pumpStreams); qMax and h0 are for a liquid,
         * and scale with the density drawn under PUMP_PRIME_RHO (a gas: it loses its prime); the work it does on the
         * fluid, q dp / (rho eta), goes into b as power. work: that power over the last step (W).
         */
        public double qMax, h0, speed = 1, eta = 0.7, work;
        /**
         * A gas pump (an ejector, a compressor): qMax is a volume (m^3/s at full speed against no head), the flow its
         * port's gas density times that (none as far as the port is under the liquid), and h0 the pressure it can make
         * whatever the density (no loss of prime).
         */
        public boolean gas;
        /**
         * TURBINE: a steam turbine from a (its inlet, drawn at its port) to b (its exhaust). Its nozzle passes q = nozzle
         * open sqrt(rho / p_a (p_a + p_b) S softplus((p_a - p_b) / S)) (the ellipse law, rho the inlet gas's density held for
         * the step), and takes eta x drop (J/kg, the isentropic drop to b's pressure, set by the game each step) for each kg
         * of water it passes out of the fluid, as shaft work.
         */
        public double nozzle, open = 1, drop;
        /**
         * REGULATOR: a self-acting pressure valve from a to b, forward only, conductance g. Reducing (relief false): it holds
         * b at setPa, its drive min(p_a, set) - p_b; relief: it holds a at setPa, its drive p_a - max(p_b, set). The min
         * and max are softplus-smoothed over REG_W of the set point, the drive over SPLIT_DP as a check valve's. Inside
         * Newton, so a regulator answers within the step (as an explicit offset set from last step's pressures it
         * overshot small vessels).
         */
        public double setPa;
        public boolean relief;
        /**
         * Gas only (a purge valve, Baughn 2026-11-10: "lets out only gas, never liquid ... it goes by phase"): the
         * stream from a carries a's gas alone (gasDraw), and only as far as its port is above the liquid (a float valve):
         * gasShare = 1 - a's liquid window at the port, taken at the step's start.
         */
        public boolean gasOnly;
        double gasShare = 1;
        /** The work (J) the last step put in (a pump) or took out (a turbine); and the last solve's share of it. */
        public double workJ;
        double pendingWork;
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
        /**
         * What the last step moved through it, a to b: each species (kg), then energy (J, conduction included), before
         * the limiter's cuts (which are rare and tiny). Null until it has carried something.
         */
        public double[] moved;
        /** The last solve's share of moved (taken back out if that solve is thrown away). */
        double[] pending;

        Edge(Node a, double fa, Node b, double fb, double g) {
            this.a = a;
            this.fa = fa;
            this.b = b;
            this.fb = fb;
            this.g = g;
            double d = (a.z + fa * a.H) - (b.z + fb * b.H);
            this.dz = Math.abs(d) > 1e-9 ? d : 0;
        }

        /** Level, the same conductance both ways and the network's band: the plain pipe's streams (every test before heads). */
        boolean plain() {
            return dz == 0 && (Double.isNaN(gBack) || gBack == g) && Double.isNaN(band) && !gasOnly;
        }

        /**
         * Its own band (Pa; NaN: SPLIT_DP) over which its two streams swap through dp = 0. At rest each carries g band ln 2,
         * mixing its two ends: a heat exchanger's slices, joined by wide pipes with a slow liquid stream through them, mixed
         * back to one node at 10 Pa, so its joints take a narrower band.
         */
        public double band = Double.NaN;

        double gB() {
            return Double.isNaN(gBack) ? g : gBack;
        }
    }

    public final List<Node> nodes = new ArrayList<>();
    public final List<Edge> edges = new ArrayList<>();

    public Node add(Node n) {
        wake();
        if (!nodes.isEmpty() && !java.util.Arrays.equals(nodes.get(0).species, n.species))
            throw new IllegalArgumentException("one network, one species list (and one implementation)");
        nodes.add(n);
        return n;
    }

    /** Join node a's port at share fa of its height to b's at fb (at any two heights: Edge.dz). */
    public Edge connect(Node a, double fa, Node b, double fb, double g) {
        Edge e = new Edge(a, fa, b, fb, g);
        edges.add(e);
        wake();
        return e;
    }

    // ---- smooth pieces

    /**
     * The contents (kg/m^3) at which conduction is half on: a near vacuum. At 1 kg/m^3 (about air at 1 bar) it cut a heat
     * exchanger's hot exhaust, ~0.5 kg/m^3, to a third of its conductance, and its steam raiser fell under the
     * well-mixed bound (53 % at NTU 2.5, against 71 %; 72 % at 0.01).
     */
    public static double M_CONTACT = 0.01;

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
        if (e.kind == Kind.TURBINE) return turbineFlow(e)[0];
        if (e.kind == Kind.REGULATOR) return regulatorFlow(e)[0];
        if (e.kind != Kind.PIPE || !e.plain()) {
            double[] q = streams(e);
            return q[0] - q[1];
        }
        return e.g * (portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost);
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
        double[] q = streams0(e);
        if (e.gasOnly && e.kind != Kind.PUMP) {
            q[0] *= e.gasShare;
            q[2] *= e.gasShare;
        }
        return q;
    }

    static double[] streams0(Edge e) {
        double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost;
        if (e.kind == Kind.PUMP) return pumpStreams(e, dp);
        if (e.kind == Kind.TURBINE || e.kind == Kind.REGULATOR) return new double[4];
        if (!e.plain()) return headStreams(e, dp);
        // (a check valve: the forward stream alone; a hard max(0, dp) put a kink where a valve sits balanced, and Newton
        // stalled on it in the game's networks: openings and pumps at rest)
        if (e.kind == Kind.CHECK && SPLIT_DP <= 0) return dp > 0 ? new double[]{e.g * dp, 0, e.g, 0} : new double[4];
        if (SPLIT_DP > 0) {
            double x = dp / SPLIT_DP;
            // (far above the band the return stream is under 1e-13 of the flow: none, exactly; taken as q+ - q there,
            // rounding left it a few 1e-16 kg/s either side of zero, and a negative stream sends b's species from a)
            if (x > 30) return new double[]{e.g * dp, 0, e.g, 0};
            double sp = x > 30 ? x : x < -30 ? Math.exp(x) : Math.log1p(Math.exp(x));
            double sig = x > 30 ? 1 : x < -30 ? Math.exp(x) : 1 / (1 + Math.exp(-x));
            double qp = e.g * SPLIT_DP * sp;
            if (e.kind == Kind.CHECK) return new double[]{qp, 0, e.g * sig, 0};
            return new double[]{qp, qp - e.g * dp, e.g * sig, e.g * sig - e.g};
        }
        return dp >= 0 ? new double[]{e.g * dp, 0, e.g, 0} : new double[]{0, -e.g * dp, 0, -e.g};
    }

    /**
     * The streams of a non-level edge, or one whose two streams have their own conductances: q+ = g S softplus((dp +
     * rho_a G dz) / S) from a and q- = gBack S softplus(-(dp + rho_b G dz) / S) from b, each paying the head of its own
     * fluid (rhoDraw) over the two ports' height difference, so a stream runs only where a column of what it carries
     * would. Equal fluids and conductances: the net is g (dp + rho G dz), and level, the plain pipe's streams. A check
     * valve has q+ alone. Returns {q+, q-, dq+ / d(dp + ha), dq- / d(dp + hb)}.
     */
    static double[] headStreams(Edge e, double dp) {
        double ha = e.dz == 0 ? 0 : G * e.dz * (Double.isNaN(e.rhoA) ? rhoDraw(e.a, e.fa) : e.rhoA);
        double hb = e.dz == 0 ? 0 : G * e.dz * (Double.isNaN(e.rhoB) ? rhoDraw(e.b, e.fb) : e.rhoB);
        double ga = e.g, gb = e.gB();
        double S = Double.isNaN(e.band) ? SPLIT_DP : e.band;
        if (S <= 0) {
            double up = dp + ha, down = -(dp + hb);
            double qp = up > 0 ? ga * up : 0, qm = e.kind == Kind.CHECK || down <= 0 ? 0 : gb * down;
            return new double[]{qp, qm, up > 0 ? ga : 0, e.kind == Kind.CHECK || down <= 0 ? 0 : -gb};
        }
        double xp = (dp + ha) / S;
        double qp = ga * S * spc(xp), dqp = ga * sgc(xp);
        if (e.kind == Kind.CHECK) return new double[]{qp, 0, dqp, 0};
        double xm = -(dp + hb) / S;
        return new double[]{qp, gb * S * spc(xm), dqp, -gb * sgc(xm)};
    }

    /** The pump's smoothing at the shut-off (share of h0). */
    public static double PUMP_W = 0.002;

    /** A pump's forward drive: dp plus the head of what it draws over the height it lifts (negative: it pumps uphill). */
    static double pumpDrive(Edge e, double dp) {
        return dp + (e.dz == 0 ? 0 : G * e.dz * (Double.isNaN(e.rhoA) ? rhoDraw(e.a, e.fa) : e.rhoA));
    }

    /**
     * A pump's stream {q, 0, dq / d(drive), 0}: speed qMax sqrt(sp(1 + drive / h0)), sp a softplus w = PUMP_W wide (a
     * finite slope at the shut-off), gated off past the shut-off head over SPLIT_DP as a check valve is: the curve's
     * own tail, h0 w wide, kept a trickle going that pumped a closed liquid-full vessel to 1.2 times the shut-off head
     * and crept a stalled pump's full pipe up for ever.
     */
    static double[] pumpStreams(Edge e, double dp) {
        if (e.qMax <= 0 || e.h0 <= 0 || e.speed <= 0) return new double[4];
        // (a centrifugal pump moves a volume and makes a head in metres: drawing gas, its flow and shut-off pressure fall
        // with the density, so it loses its prime instead of pumping a vessel's vapour out to a hard vacuum)
        double f = prime(e), h0 = e.gas ? e.h0 : e.h0 * f, qMax = e.gas ? e.qMax * drawRho(e) : e.qMax * f;
        if (!(h0 > 0)) return new double[4];
        double arg = pumpDrive(e, dp), w = PUMP_W, u = (1 + arg / h0) / w;
        double sp = w * spc(u);
        // (far past its shut-off the softplus underflows to nothing, and so does the flow; its slope 0 / 0 was NaN)
        if (!(sp > 0)) return new double[4];
        double phi = Math.sqrt(sp), dphi = sgc(u) / (2 * phi);
        // (and shut past the shut-off head as a check valve shuts, over SPLIT_DP: the curve's own tail, h0 PUMP_W wide,
        // crept a stalled pump's full pipe up for ever)
        double x = (h0 + arg) / SPLIT_DP, gate = sgc(x), dgate = gate * sgc(-x) / SPLIT_DP;
        double q = e.speed * qMax * phi * gate, dq = e.speed * qMax * (dphi / h0 * gate + phi * dgate);
        return new double[]{q, 0, dq, 0};
    }

    /** A pump's rated flow and head are for a liquid at least this dense (kg/m^3); below it both scale with density. */
    public static double PUMP_PRIME_RHO = 500;

    /** The share of a pump's rated flow and shut-off head it has on what it draws (held for the step as heads are). */
    static double prime(Edge e) {
        return Math.max(0, Math.min(1, drawRho(e) / PUMP_PRIME_RHO));
    }

    /** The density a pump draws (held for the step as heads are). */
    static double drawRho(Edge e) {
        return Math.max(0, Double.isNaN(e.rhoA) ? e.gas ? gasDraw(e) : rhoDraw(e.a, e.fa) : e.rhoA);
    }

    /** An edge that carries only its a side's gas (a gas pump): never liquid, even with its port inside the window. */
    static boolean gasOnly(Edge e) {
        return e.kind == Kind.PUMP && e.gas || e.gasOnly;
    }

    /** What a kg from a's side of the edge carries: a gas pump its gas alone (gasDraw), else the port's draw. */
    static double[] drawOf(Edge e) {
        return gasOnly(e) ? e.a.fluid.gasDraw() : e.a.fluid.draw(e.fa);
    }

    /** What a gas pump draws: the gas, as far as its port is above the liquid (under water, an ejector moves nothing). */
    static double gasDraw(Edge e) {
        return e.a.fluid.rhoGas() * (1 - e.a.fluid.liquidWindow(e.fa));
    }

    /**
     * The power a pump puts into the fluid (W) at its current states, and its slope in the drive: q P / (rho eta), P the
     * pressure it lifts by (a softplus of -drive, SPLIT_DP wide: nothing where the flow runs downhill on its own).
     */
    static double[] pumpWork(Edge e) {
        if (e.kind != Kind.PUMP) return null;
        double dp = portP(e.a, e.fa) - portP(e.b, e.fb) + e.boost, arg = pumpDrive(e, dp);
        double[] qs = pumpStreams(e, dp);
        double rho = Double.isNaN(e.rhoA) ? rhoDraw(e.a, e.fa) : e.rhoA;
        if (qs[0] == 0 || rho <= 0) return new double[2];
        double S = SPLIT_DP, x = -arg / S, P = S * spc(x), dP = -sgc(x), k = 1 / (rho * e.eta);
        return new double[]{qs[0] * P * k, (qs[2] * P + qs[0] * dP) * k};
    }

    /**
     * A turbine's nozzle flow {q, dq/dp_a, dq/dp_b}: nozzle open sqrt(rho T), T = (p_a + p_b) S softplus((p_a - p_b) / S) / p_a,
     * the ellipse law p_a^2 - p_b^2 with its root's infinite slope at p_b = p_a smoothed over S (SPLIT_DP) as a check valve
     * is, and nothing back.
     */
    static double[] turbineFlow(Edge e) {
        if (e.nozzle <= 0 || e.open <= 0) return new double[3];
        double pa = portP(e.a, e.fa), pb = portP(e.b, e.fb), S = SPLIT_DP, x = (pa - pb) / S;
        double rho = Double.isNaN(e.rhoA) ? e.a.fluid.rhoGas() : e.rhoA;
        // (the ellipse law q = c sqrt(rho / p_a (p_a^2 - p_b^2)): choked, c p_a / sqrt(R T), as a nozzle passes)
        double sp = S * spc(x), T = (pa + pb) * sp / pa;
        if (!(T > 0) || !(rho > 0) || !(pa > 0)) return new double[3];
        double c = e.nozzle * e.open, q = c * Math.sqrt(rho * T), dqdT = q / (2 * T), sg = (pa + pb) * sgc(x);
        return new double[]{q, dqdT * ((sp + sg) / pa - T / pa), dqdT * (sp - sg) / pa};
    }

    /** A regulator's smoothing of its min / max, as a share of its set point. */
    public static double REG_W = 0.005;

    /** A regulator's flow {q, dq/dp_a, dq/dp_b} (Edge.setPa). */
    static double[] regulatorFlow(Edge e) {
        if (e.g <= 0 || e.setPa <= 0) return new double[3];
        double pa = portP(e.a, e.fa), pb = portP(e.b, e.fb), set = e.setPa, W = Math.max(SPLIT_DP, REG_W * set);
        double drive, da, db;
        if (e.relief) {
            double y = (pb - set) / W;
            drive = pa - (set + W * spc(y));
            da = 1;
            db = -sgc(y);
        } else {
            double y = (set - pa) / W;
            drive = set - W * spc(y) - pb;
            da = sgc(y);
            db = -1;
        }
        double S = SPLIT_DP, x = drive / S, q = e.g * S * spc(x), dq = e.g * sgc(x);
        return new double[]{q, dq * da, dq * db};
    }

    /** A turbine's shaft power (W) and its slopes: eta drop x the water it passes ({W, dW/dp_a, dW/dp_b, water share}). */
    static double[] turbineWork(Edge e) {
        double[] q = turbineFlow(e);
        if (q[0] == 0 || e.drop <= 0) return new double[4];
        double xw = e.a.fluid.draw(e.fa)[0], k = e.eta * e.drop;
        return new double[]{k * q[0] * xw, k * q[1] * xw, k * q[2] * xw, xw};
    }

    /** softplus and its slope, cut 30 either side as the streams are. */
    static double spc(double x) {
        return x > 30 ? x : x < -30 ? Math.exp(x) : Math.log1p(Math.exp(x));
    }

    static double sgc(double x) {
        return x > 30 ? 1 : x < -30 ? Math.exp(x) : 1 / (1 + Math.exp(-x));
    }

    /**
     * The density a head is weighed by: what the port at f draws, its liquid share by the port's window and the rest
     * gas, blended by that share (as the game's legacy solve weighed its heads; the consistent 1 / (w / rhoL + (1 - w) /
     * rhoG) jumps from gas to liquid over the last percent of the window).
     */
    static double rhoDraw(Node n, double f) {
        double w = n.fluid.liquidWindow(f);
        if (HARMONIC_HEAD) {
            double rl = n.fluid.rhoLiquid(f), rg = n.fluid.rhoGas();
            if (w >= 1) return rl;
            if (w <= 0 || rl <= 0) return rg;
            return 1 / (w / rl + (1 - w) / Math.max(rg, 1e-12));
        }
        return w * n.fluid.rhoLiquid(f) + (1 - w) * n.fluid.rhoGas();
    }

    /**
     * Weigh each stream's head by its port's draw density at the step's start, held through the step (as the game holds
     * each stream's conductance): no slopes of the density in the Jacobian. Live, their finite differences cost 2(S + 1)
     * node solves a port an iteration, which in MixFluid made the multi-species property tests crawl.
     */
    public static boolean FREEZE_HEAD = true;

    /** Experiment: weigh a head by the drawn mix's own density, 1 / (w / rhoL + (1 - w) / rhoG). */
    public static boolean HARMONIC_HEAD = "1".equals(System.getenv("FLUIDCORE_HARMONIC"));

    /** d rhoDraw / d(masses, U) of a free node, by central differences of its state (as the exchange's slopes are). */
    static double[] rhoDrawSlope(Node n, double f) {
        int S = n.m.length, C = S + 1;
        double[] d = new double[C];
        Object state = n.fluid.saveState();
        for (int j = 0; j < C; j++) {
            double keep = j < S ? n.m[j] : n.U;
            double h = j == S ? 1e-6 * Math.max(1, Math.abs(keep)) : Math.max(1e-12, 1e-6 * keep);
            double lo = j < S ? Math.max(0, keep - h) : keep - h, hi = keep + h;
            double[] r = new double[2];
            for (int side = 0; side < 2; side++) {
                if (j < S) n.m[j] = side == 0 ? hi : lo;
                else n.U = side == 0 ? hi : lo;
                n.update();
                r[side] = rhoDraw(n, f);
            }
            if (j < S) n.m[j] = keep;
            else n.U = keep;
            n.fluid.restoreState(state);
            d[j] = (r[0] - r[1]) / (hi - lo);
        }
        return d;
    }

    // ---- the step

    /** Counters for tests and measurements. */
    public static final class Stats {
        public long steps, iterations, substeps, failures, solves, stalls, slept;
        /** Steps ended early (CUT_SHORT): their remaining time dropped; and of those, steps that moved nothing at all. */
        public long cut, stuck;
        /** Solves accepted out of iterations while crawling at the precision floor (under STALL_OK; Network.solve). */
        public long crawls;
        /** The last failed solve's residual norms by iteration (tests). */
        public final List<Double> lastFail = new ArrayList<>();
        /** The last failed solve's scaled residual by row (tests). */
        public double[] lastRows = new double[0];
        final List<Double> norms = new ArrayList<>();
    }

    public final Stats stats = new Stats();
    /**
     * Newton iterations a solve, and how many times a step may halve (8: down to dt / 256). At 5, one random network's
     * violent first steps (hot liquid running into a cold, nearly empty node) still failed at dt / 32; at 8 none fails
     * in 100 random networks x 3000 steps, with or without conduction, and the deeper halvings cost nothing elsewhere.
     */
    public static int MAX_ITERS = 25, MAX_DEPTH = 8;

    /** One step of dt seconds. */
    public void step(double dt) {
        stats.steps++;
        if (asleep) {
            if (signature() == sleepSignature) {
                stats.slept++;
                done = dt;
                return;
            }
            wake();
        }
        List<Node> free = new ArrayList<>();
        for (Node n : nodes) if (!n.fixed) free.add(n);
        for (Edge e : edges) {
            if (e.moved != null) java.util.Arrays.fill(e.moved, 0);
            if (e.pending != null) java.util.Arrays.fill(e.pending, 0);
            e.workJ = e.pendingWork = 0;
        }
        double[] y0 = pack(free);
        done = 0;
        for (Node n : free) n.wallJ = n.pendingWall = 0;
        for (Edge e : edges) {
            boolean f = FREEZE_HEAD && (e.dz != 0 || e.kind == Kind.PUMP) || e.kind == Kind.TURBINE;
            e.rhoA = !f ? Double.NaN : e.kind == Kind.TURBINE ? e.a.fluid.rhoGas() : e.gas ? gasDraw(e) : rhoDraw(e.a, e.fa);
            e.rhoB = f ? rhoDraw(e.b, e.fb) : Double.NaN;
            if (e.gasOnly) e.gasShare = Math.max(0, 1 - e.a.fluid.liquidWindow(e.fa));
        }
        progressed = false;
        stepSubsteps = 0;
        if (solve(free, y0, dt)) done = dt;
        else {
            // (no convergence: two half steps, each from where the last ended)
            restore(free, y0);
            if (CUT_SHORT && ADAPTIVE) adaptive(free, dt);
            else substep(free, dt, 1);
        }
        for (Edge e : edges) {
            e.flow = flow(e);
            e.work = e.workJ / dt;
        }
        quiet = isQuiet(free, y0, pack(free)) ? quiet + 1 : 0;
        if (SLEEP_STEPS > 0 && quiet >= SLEEP_STEPS) {
            asleep = true;
            sleepSignature = signature();
        }
    }

    // ---- sleeping

    /**
     * A network whose every node changed by less than SLEEP_REL of its contents (masses, and energy against a typical
     * 1e5 J/kg) in each of SLEEP_STEPS steps in a row goes to sleep: its steps then cost one pass over its inputs, until
     * any of them changes (a node's contents or heat, an edge's conductance, boost, kind, conductance or exchange; nodes
     * or edges added) or wake() is called. 0 steps: never sleep.
     */
    public static int SLEEP_STEPS = 100;
    public static double SLEEP_REL = 1e-9;
    private boolean asleep;
    private int quiet;
    private long sleepSignature;

    public boolean asleep() {
        return asleep;
    }

    /** Wake it (the adapter calls this whenever it changes anything; the signature catches the rest). */
    public void wake() {
        asleep = false;
        quiet = 0;
    }

    private boolean isQuiet(List<Node> free, double[] y0, double[] y1) {
        int C = C(), S = C - 1;
        double worst = 0;
        int at = -1;
        for (int i = 0; i < free.size(); i++) {
            double m = Math.max(1e-9, massOf(y0, C * i, S));
            for (int k = 0; k < S; k++) {
                double r = Math.abs(y1[C * i + k] - y0[C * i + k]) / m;
                if (r > worst) { worst = r; at = C * i + k; }
            }
            double r = Math.abs(y1[C * i + S] - y0[C * i + S]) / (m * 1e5);
            if (r > worst) { worst = r; at = C * i + S; }
        }
        restless = worst;
        restlessAt = at;
        return worst <= SLEEP_REL;
    }

    /** The last step's largest change (relative, as isQuiet measures it) and its row: why a network stays awake. */
    public double restless;
    public int restlessAt = -1;

    /** A hash of everything outside the solve that a step depends on. */
    private long signature() {
        long h = 0x9E3779B97F4A7C15L ^ nodes.size() * 31L ^ edges.size();
        for (Node n : nodes) {
            for (double x : n.m) h = mixIn(h, x);
            h = mixIn(mixIn(mixIn(mixIn(h, n.U), n.heat), n.wallUA), n.wallT);
        }
        for (Edge e : edges) {
            h = mixIn(mixIn(mixIn(mixIn(h, e.g), e.gBack), e.boost), e.conductance);
            h = mixIn(mixIn(mixIn(h, e.exchange), e.kind.ordinal()), e.setPa);
        }
        return h;
    }

    private static long mixIn(long h, double x) {
        long z = h ^ Double.doubleToLongBits(x);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * With CUT_SHORT (the game): sub-steps sized as they go, halved on a failure and doubled on a success, down to dt /
     * 2^(MAX_DEPTH + ADAPTIVE_EXTRA), at most MAX_SUBSTEPS. A fixed halving tree never grew back, so a transient needing
     * steps of 1e-7 s for 1e-4 s (boiling water flooding a warm, dry pump line: its air saturates and cools, and the
     * pressure drop pulls the water in) cut every tick after its first converged sub-step, and the network stalled.
     * Backward Euler has a root past the transient for steps up to ~1e-2 s, so once through, the steps grow back.
     */
    private void adaptive(List<Node> free, double dt) {
        double t = 0, h = dt / 2, hMin = dt / Math.pow(2, MAX_DEPTH + ADAPTIVE_EXTRA);
        while (t < dt * (1 - 1e-12)) {
            stats.substeps++;
            if (++stepSubsteps > MAX_SUBSTEPS) {
                stats.cut++;
                return;
            }
            h = Math.min(h, dt - t);
            double[] y = pack(free);
            if (solve(free, y, h)) {
                progressed = true;
                done += h;
                t += h;
                h *= 2;
            } else {
                restore(free, y);
                h /= 2;
                if (h < hMin) {
                    stats.failures++;
                    stats.cut++;
                    if (!progressed) stats.stuck++;
                    return;
                }
            }
        }
    }

    /** Adaptive sub-steps in the game (CUT_SHORT), and how many halvings below MAX_DEPTH they may go. */
    public static boolean ADAPTIVE = true;
    public static int ADAPTIVE_EXTRA = 12;

    private boolean substep(List<Node> free, double dt, int depth) {
        stats.substeps++;
        if (CUT_SHORT && ++stepSubsteps > MAX_SUBSTEPS) {
            // (a step that has taken MAX_SUBSTEPS sub-steps ends where it is: a network needing thousands of sub-steps
            // every tick stalled the server)
            stats.cut++;
            return false;
        }
        for (int half = 0; half < 2; half++) {
            double[] y = pack(free);
            if (solve(free, y, dt / 2)) {
                progressed = true;
                done += dt / 2;
            }
            else {
                restore(free, y);
                // (a step that has converged nothing yet goes 6 levels deeper before it is cut: cut at once, a network
                // whose first 1/256 of a tick fails would never move)
                if (depth < MAX_DEPTH || CUT_SHORT && !progressed && depth < MAX_DEPTH + 6) {
                    if (!substep(free, dt / 2, depth + 1)) return false;
                } else if (CUT_SHORT) {
                    // (the step ends at the last sub-step that converged: the rest of its time is dropped, so the network
                    // runs slow for a moment rather than taking a best iterate that may be no physical state at all)
                    stats.failures++;
                    stats.cut++;
                    if (!progressed) stats.stuck++;
                    return false;
                } else {
                    stats.failures++;
                    solve(free, y, dt / 2);   // (its best iterate: still conservative)
                    done += dt / 2;
                }
            }
        }
        return true;
    }

    /**
     * When a sub-step fails at the floor: end the step at the last sub-step that converged (true; the game) or take the
     * failed one's best iterate and go on (false; conservative, but possibly far from a physical state).
     */
    public static boolean CUT_SHORT = false;
    /** This step has converged at least one sub-step. */
    private boolean progressed;
    /**
     * The time (s) the last step actually covered: its dt, or with CUT_SHORT what its converged sub-steps covered (the game
     * carries the rest into the next step, so heat put in over the dropped time isn't lost).
     */
    public double done;
    /** With CUT_SHORT: at most this many sub-steps a step (then it ends where it is); and this step's count. */
    public static int MAX_SUBSTEPS = 256;
    private int stepSubsteps;

    /** Components per node: the species masses, then the energy (one count for the whole network). */
    private int C() {
        return nodes.isEmpty() ? 3 : nodes.get(0).m.length + 1;
    }

    double[] pack(List<Node> free) {
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
        for (Node n : free) {
            n.wallJ -= n.pendingWall;
            n.pendingWall = 0;
        }
        for (Edge e : edges) {
            e.workJ -= e.pendingWork;
            e.pendingWork = 0;
            if (e.pending == null) continue;
            for (int k = 0; k < e.pending.length; k++) e.moved[k] -= e.pending[k];
            java.util.Arrays.fill(e.pending, 0);
        }
    }

    void set(List<Node> free, double[] y) {
        int C = C(), S = C - 1;
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            for (int k = 0; k < S; k++) n.m[k] = y[C * i + k];
            n.U = y[C * i + S];
            n.update();
        }
    }

    /**
     * The edge's net fluxes from a to b at the current states, or null for none: kg/s of each mass, then the energy its
     * flows carry (W; the two streams and the exchange), then the heat it conducts (C + 1 entries). The one definition of
     * the fluxes: the residual and the end of the step both use it, so the step ends on exactly the fluxes Newton solved.
     */
    private static double[] flux(Edge e, int C) {
        double[] f = null;
        if (e.conductance > 0) {
            f = new double[C + 1];
            f[C] = conduction(e);
        }
        if (e.exchange > 0) {
            double[] x = exchangeFlux(e);
            if (f == null) f = new double[C + 1];
            for (int k = 0; k < C; k++) f[k] += x[k];
        }
        if (e.kind == Kind.TURBINE || e.kind == Kind.REGULATOR) {
            double q = e.kind == Kind.TURBINE ? turbineFlow(e)[0] : regulatorFlow(e)[0];
            if (q == 0) return f;
            double[] ca = e.a.fluid.draw(e.fa);
            if (f == null) f = new double[C + 1];
            for (int k = 0; k < C; k++) f[k] += q * ca[k];
            return f;
        }
        double[] qs = streams(e);
        if (qs[0] == 0 && qs[1] == 0) return f;
        double[] ca = qs[0] != 0 ? drawOf(e) : null, cb = qs[1] != 0 ? e.b.fluid.draw(e.fb) : null;
        if (f == null) f = new double[C + 1];
        for (int k = 0; k < C; k++) f[k] += (ca != null ? qs[0] * ca[k] : 0) - (cb != null ? qs[1] * cb[k] : 0);
        return f;
    }

    /** The residual R = Y' - Y0 - dt (fluxes in + heat) at the nodes' current states (each node's C rows). */
    double[] residual(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        int C = C(), S = C - 1;
        double[] r = new double[C * free.size()];
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            for (int k = 0; k < S; k++) r[C * i + k] = n.m[k] - y0[C * i + k];
            r[C * i + S] = n.U - y0[C * i + S] - dt * n.heat - (n.wallUA > 0 ? dt * n.wallUA * (n.wallT - n.T()) : 0);
        }
        for (Edge e : edges) {
            double[] f = flux(e, C);
            if (f == null) continue;
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            f[S] += f[C];
            for (int k = 0; k < C; k++) {
                if (ia != null) r[C * ia + k] += dt * f[k];   // (a loses what flows to b)
                if (ib != null) r[C * ib + k] -= dt * f[k];
            }
        }
        for (Edge e : edges) {
            double[] w = pumpWork(e);
            Integer ib = w == null ? null : idx.get(e.b);
            if (ib != null) r[C * ib + S] -= dt * w[0];
            if (e.kind == Kind.TURBINE && (ib = idx.get(e.b)) != null) r[C * ib + S] += dt * turbineWork(e)[0];
        }
        return r;
    }

    /** dR/dY' at the current states. */
    /** A node's port (rhoDrawSlope's cache within one Jacobian). */
    private record PortKey(Node n, double f) {
        @Override
        public boolean equals(Object o) {
            return o instanceof PortKey k && k.n == n && k.f == f;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(n) * 31 + Double.hashCode(f);
        }
    }

    /** A turbine's entries: F_k = q ca_k out of a into b, and its work W = eta drop q ca_0 out of b's energy. */
    private static void turbineJacobian(Sparse J, Edge e, java.util.Map<Node, Integer> idx, int C, double dt) {
        int S = C - 1;
        double[] q = e.kind == Kind.REGULATOR ? regulatorFlow(e) : turbineFlow(e);
        double[] ca = e.a.fluid.draw(e.fa);
        double[][] dca = e.a.fluid.drawSlope(e.fa);
        double[] dpa = e.a.fluid.portPSlope(e.fa), dpb = e.b.fluid.portPSlope(e.fb);
        double k = e.kind == Kind.TURBINE && e.drop > 0 ? e.eta * e.drop : 0;
        Integer ia = idx.get(e.a), ib = idx.get(e.b);
        for (int side = 0; side < 2; side++) {
            Integer col = side == 0 ? ia : ib;
            if (col == null) continue;
            for (int j = 0; j < C; j++) {
                double dq = side == 0 ? q[1] * dpa[j] : q[2] * dpb[j];
                for (int r = 0; r < C; r++) {
                    double dF = dq * ca[r] + (side == 0 ? q[0] * dca[r][j] : 0);
                    if (ia != null) J.add(C * ia + r, C * col + j, dt * dF);
                    if (ib != null) J.add(C * ib + r, C * col + j, -dt * dF);
                }
                double dW = k * (dq * ca[0] + (side == 0 ? q[0] * dca[0][j] : 0));
                if (ib != null) J.add(C * ib + S, C * col + j, dt * dW);
            }
        }
    }

    private Sparse jacobian(List<Node> free, java.util.Map<Node, Integer> idx, double dt) {
        int C = C(), S = C - 1;
        int M = C * free.size();
        Sparse J = new Sparse(M);
        for (int i = 0; i < M; i++) J.add(i, i, 1);
        // a wall's heat: R_U -= dt UA (Tw - T), so dR_U = dt UA dT
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            if (n.wallUA <= 0) continue;
            double[] dT = n.fluid.tSlope();
            for (int j = 0; j < C; j++) J.add(C * i + S, C * i + j, dt * n.wallUA * dT[j]);
        }
        java.util.Map<PortKey, double[]> rhoSlopes = new java.util.HashMap<>();
        for (Edge e : edges) {
            // (every entry an edge can touch is added, zeros too: skipping zeros let entries come and go, and each new
            // one rebuilt the sparse LU's symbolic factorisation, most steps at 2,000 nodes)
            if (e.kind == Kind.TURBINE || e.kind == Kind.REGULATOR) {
                // (a regulator: the same entries without the work, its flow a function of p_a and p_b apart)
                turbineJacobian(J, e, idx, C, dt);
                continue;
            }
            // F_k = q+ ca_k - q- cb_k; dF_k = dq+ d(arg+) ca_k - dq- d(arg-) cb_k + q+ dca_k (a) - q- dcb_k (b), arg+ = dp + ha,
            // arg- = dp + hb (level: both dp)
            double[] qs = streams(e);
            double[] ca = drawOf(e), cb = e.b.fluid.draw(e.fb);
            // (a gas pump's gas-only draw: its composition's slopes left out, as small as the gas share's changes)
            double[][] dca = qs[0] != 0 && !gasOnly(e) ? e.a.fluid.drawSlope(e.fa) : null, dcb = qs[1] != 0 ? e.b.fluid.drawSlope(e.fb) : null;
            double[] dpa = e.a.fluid.portPSlope(e.fa), dpb = e.b.fluid.portPSlope(e.fb);
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            double[] dha = e.dz != 0 && ia != null && Double.isNaN(e.rhoA) ? rhoSlopes.computeIfAbsent(new PortKey(e.a, e.fa), p -> rhoDrawSlope(e.a, e.fa)) : null;
            double[] dhb = e.dz != 0 && ib != null && Double.isNaN(e.rhoB) ? rhoSlopes.computeIfAbsent(new PortKey(e.b, e.fb), p -> rhoDrawSlope(e.b, e.fb)) : null;
            for (int k = 0; k < C; k++) {
                double dFdp = qs[2] * ca[k] - qs[3] * cb[k];
                for (int side = 0; side < 2; side++) {
                    Integer col = side == 0 ? ia : ib;
                    if (col == null) continue;
                    double[] dpx = side == 0 ? dpa : dpb;
                    double sign = side == 0 ? 1 : -1;
                    double[][] dc = side == 0 ? dca : dcb;
                    double qn = side == 0 ? qs[0] : -qs[1];
                    double[] dh = side == 0 ? dha : dhb;
                    for (int j2 = 0; j2 < C; j2++) {
                        double dF;
                        if (dha == null && dhb == null) dF = dFdp * sign * dpx[j2] + (dc != null ? qn * dc[k][j2] : 0);
                        else {
                            double dArg = sign * dpx[j2], dHead = dh != null ? G * e.dz * dh[j2] : 0;
                            double dArgP = side == 0 ? dArg + dHead : dArg, dArgM = side == 1 ? dArg + dHead : dArg;
                            dF = qs[2] * dArgP * ca[k] - qs[3] * dArgM * cb[k] + (dc != null ? qn * dc[k][j2] : 0);
                        }
                        if (ia != null) J.add(C * ia + k, C * col + j2, dt * dF);
                        if (ib != null) J.add(C * ib + k, C * col + j2, -dt * dF);
                    }
                }
            }
        }
        // a pump's work into its outlet: dW = dW/d(drive) (dPa - dPb)
        for (Edge e : edges) {
            double[] w = pumpWork(e);
            Integer ib = w == null ? null : idx.get(e.b);
            if (ib == null) continue;
            Integer ia = idx.get(e.a);
            for (int side = 0; side < 2; side++) {
                Integer col = side == 0 ? ia : ib;
                if (col == null) continue;
                double[] dpx = side == 0 ? e.a.fluid.portPSlope(e.fa) : e.b.fluid.portPSlope(e.fb);
                double sign = side == 0 ? 1 : -1;
                for (int j = 0; j < C; j++) J.add(C * ib + S, C * col + j, -dt * w[1] * sign * dpx[j]);
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
    /**
     * Out of iterations, a residual under this is accepted as the precision floor (Network.solve). In the GameTests such
     * solves end at 1e-10 to 1e-7 (a few thousand in 28 steam tests); the few over 1e-7 sub-step as other failures do.
     */
    public static double CRAWL_OK = 1e-7;
    /** The pressure chop: an iteration moves no node's pressure by more than CHOP times itself or a bar (0: off). */
    public static double CHOP = 1;
    /** Fraction-to-the-boundary floor (share of the node's contents): components under it are damped on their own. */
    public static double FLOOR = 1e-4;
    /** Tests: a trace of each Newton iteration (null: off). */
    public static StringBuilder DEBUG = null;

    /**
     * End the step conservatively whatever Newton reached: the contents become the start's plus dt times the fluxes
     * at the final iterate (Y' = Y0 + dt F(Y'_iter)), so mass and energy balance exactly even when the residual isn't
     * zero; converged, this moves Y' by no more than the residual.
     */
    void finish(List<Node> free, java.util.Map<Node, Integer> idx, double[] y0, double dt) {
        int C = C(), S = C - 1;
        // each transfer: {from, to (indices into free, -1 for a fixed node or outside), component, amount moved over
        // dt, edge (-1: not an edge's flow)}; the heat supplied to each node first
        List<double[]> moves = new ArrayList<>();
        for (int i = 0; i < free.size(); i++) {
            Node n = free.get(i);
            move(moves, -1, i, S, dt * n.heat, -1);
            n.pendingWall = n.wallUA > 0 ? dt * n.wallUA * (n.wallT - n.T()) : 0;
            n.wallJ += n.pendingWall;
            move(moves, -1, i, S, n.pendingWall, -1);
        }
        // (a pump's work comes from outside, into its outlet; a turbine's goes out of its exhaust, to its shaft)
        for (Edge e : edges) {
            e.pendingWork = 0;
            double[] w = pumpWork(e);
            Integer ib = idx.get(e.b);
            if (w != null) {
                e.pendingWork = dt * w[0];
                if (ib != null) move(moves, -1, ib, S, dt * w[0], -1);
            } else if (e.kind == Kind.TURBINE) {
                double tw = turbineWork(e)[0];
                e.pendingWork = dt * tw;
                if (ib != null) move(moves, ib, -1, S, dt * tw, -1);
            }
            e.workJ += e.pendingWork;
        }
        // (an edge's net flux of each component: its two streams' swap of one species cancels here rather than in the
        // limiter, where two nearly empty nodes swapping a trace each way kept cutting each other's sends without end)
        for (int g = 0; g < edges.size(); g++) {
            Edge e = edges.get(g);
            double[] f = flux(e, C);
            if (e.pending != null) java.util.Arrays.fill(e.pending, 0);
            if (f == null) continue;
            if (e.moved == null || e.moved.length != C) {
                e.moved = new double[C];
                e.pending = new double[C];
            }
            for (int k = 0; k < C; k++) e.pending[k] = dt * (k == S ? f[k] + f[C] : f[k]);
            for (int k = 0; k < C; k++) e.moved[k] += e.pending[k];
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            int a = ia == null ? -1 : ia, b = ib == null ? -1 : ib;
            for (int k = 0; k < C; k++) move(moves, a, b, k, dt * f[k], g);
            move(moves, a, b, S, dt * f[C], -1);
        }
        // the limiter: a node can't send more of a mass than it started with plus what it receives (Newton's tolerance
        // is relative to the node's whole contents, so a dry node's water could end 1e-10 of its air below zero);
        // its sends of that mass are scaled down, which keeps every transfer in flux form and so conserves exactly
        set(free, Limiter.apply(C, y0, moves));
    }

    /** Adds the transfer of x of component k from a to b (the other way for x < 0, nothing for 0). */
    private static void move(List<double[]> moves, int a, int b, int k, double x, int edge) {
        if (x > 0) moves.add(new double[]{a, b, k, x, edge});
        else if (x < 0) moves.add(new double[]{b, a, k, -x, edge});
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
        // (kept only if it is nearer the step's end than the start is: through an edge stiff for the step, a steam pipe at
        // 10 bar opening into a pipe of air at 1 bar, the explicit flow is 100 times what the small node can hold, and the
        // predicted state was thousands of bar, where Newton's linear solve was singular)
        double[] s = scales(free, y0);
        if (norm(residual(free, idx, y0, dt), s) > norm(r, s)) set(free, y0);
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
            if (DEBUG != null) {
                double[][] Jd = jacobian(free, idx, dt).dense();
                double worst = 0;
                for (int i = 0; i < r.length; i++) {
                    double row = r[i], mag = Math.abs(r[i]);
                    for (int j = 0; j < r.length; j++) {
                        row += Jd[i][j] * d[j];
                        mag += Math.abs(Jd[i][j] * d[j]);
                    }
                    worst = Math.max(worst, Math.abs(row) / Math.max(mag, 1e-300));
                }
                DEBUG.append(String.format("  linear solve: worst |J d + r| / scale %.3e%n", worst));
            }
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
                                // (its own balance wants it up: not held but stepped by its own row's Newton step, -r / J_kk,
                                // and the rest solved again with that step fixed; setting it alone left the others' step
                                // computed for the far larger fall Newton wanted, and a pipe of air taking its first steam
                                // stalled; -r alone is the explicit inflow, blind to the node's own pressure pushing back, and
                                // poured 90 g into a pipe of 34 g)
                                int row = C * i + k;
                                double jkk = J.get(row, row), target = -r[row] / Math.max(1, jkk);
                                if (d[row] == target) continue;
                                J.unitRow(row);
                                minus[row] = target;
                                held = true;
                                if (DEBUG != null) {
                                    DEBUG.append(String.format("  row %d stepped: Newton's d %.4g, r %.4g, J_kk %.4g, target %.4g; row:", row, d[row], r[row], jkk, target));
                                    double[][] Jd = jacobian(free, idx, dt).dense();
                                    for (int c2 = 0; c2 < Jd.length; c2++) DEBUG.append(String.format(" %.3g", Jd[row][c2]));
                                    DEBUG.append("\n  energy row:");
                                    for (int c2 = 0; c2 < Jd.length; c2++) DEBUG.append(String.format(" %.3g", Jd[C * i + C - 1][c2]));
                                    DEBUG.append("\n");
                                }
                                continue;
                            }
                            J.unitRow(C * i + k);
                            minus[C * i + k] = 0;
                            held = true;
                            if (DEBUG != null) DEBUG.append("  held row ").append(C * i + k).append('\n');
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
            // pressure chop (Appleyard's, from reservoir simulation): no node's pressure may move by more than itself (or a
            // bar) in one iteration by the linear model. Filling a nearly full tank from a 50 bar vessel, the first step
            // poured in a thousand times the tank, Newton swung between empty and over-full, and its best iterate emptied
            // the tank; chopped, the pressure climbs the knee a doubling at a time
            if (CHOP > 0)
                for (int i = 0; i < free.size(); i++) {
                    Node n = free.get(i);
                    double[] dP = n.fluid.portPSlope(1);
                    double change = 0;
                    for (int j = 0; j < C; j++) change += dP[j] * d[C * i + j];
                    double room = CHOP * Math.max(n.P(), 1e5);
                    if (Math.abs(change) * alpha > room) alpha = room / Math.abs(change);
                }
            if (DEBUG != null) {
                DEBUG.append(String.format("it %d nr %.3e alpha0 %.3e%n  y %s%n  r*s %s%n  d %s%n", it, nr, alpha, java.util.Arrays.toString(y),
                    java.util.Arrays.toString(scaled(r, s)), java.util.Arrays.toString(d)));
            }
            boolean accepted = false, crawling = false;
            for (int ls = 0; ls < 12; ls++, alpha *= 0.5) {
                double[] yt = y.clone();
                for (int i = 0; i < y.length; i++) yt[i] += alpha * d[i];
                set(free, yt);
                double[] rt = residual(free, idx, y0, dt);
                double nt = norm(rt, s);
                if (DEBUG != null && ls < 3) DEBUG.append(String.format("    ls %d alpha %.3e nt %.3e r*s %s%n", ls, alpha, nt, java.util.Arrays.toString(scaled(rt, s))));
                if (nt < (1 - 1e-4 * alpha) * nr || nt < TOL) {
                    crawling = nt >= TOL && nt < CRAWL_OK && nt > 0.5 * nr;
                    y = yt;
                    r = rt;
                    nr = nt;
                    accepted = true;
                    break;
                }
            }
            stats.norms.add(nr);
            // (crawling at the precision floor: under CRAWL_OK and an iteration took less than half off; more iterations
            // only shave slivers. Accepted at once: on the clone, networks crawled to MAX_ITERS every tick, each
            // iteration a line search re-flashing every node, and three 4-9 node networks cost 2-4 ms a step)
            if (accepted && crawling) {
                stats.crawls++;
                return true;
            }
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
        // (out of iterations crawling at the precision floor: each iteration took a sliver off the residual, so the line
        // search never stalled, but rounding keeps it there; under CRAWL_OK, accepted: the step ends conservatively. A
        // steam jet into an emptied condenser failed every sub-step this way at 3e-10 to 3e-8, and its network made no
        // progress for seconds)
        if (nr >= TOL && nr < CRAWL_OK) {
            stats.crawls++;
            return true;
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
