package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * One connected fluid network and its step (notes/pipes/DESIGN.md section 6): heat in, an implicit pressure solve
 * ((C/dt + G) P' = C/dt P + gravity head), then the flows at those pressures move water, air and energy upwind, and
 * every node finds its new equilibrium (Flash). Mass is conserved exactly: each flow leaves one node and enters the
 * other. Small networks (tens of nodes): a dense solve each step.
 * <p>
 * The transport is implicit (as in reactor thermal-hydraulics codes): a node gives at most DRAIN_CAP of each phase it
 * holds per step, and what leaves it beyond that is what flowed in during the step, passed through. The mix and
 * enthalpy passed through come from a linear solve over the network, so a small node (a short pipe) can carry many
 * times its contents per step without its outflow carrying its old contents (that drove a pipe to 1268 K), and pipe
 * volume no longer limits throughput. Each node answers pressure with one compressibility per phase, whichever edge
 * the mass comes through, so through-flow doesn't fake a mass imbalance (Gemini's review, 2026-10-25).
 */
public final class FluidNetwork {
    public static final double G = 9.81;
    /** Outflow per step is capped at this share of what the upstream node holds of that phase. */
    static final double DRAIN_CAP = 0.5;

    public final List<FluidNode> nodes = new ArrayList<>();
    public final List<FluidEdge> edges = new ArrayList<>();

    public FluidNode add(FluidNode n) {
        nodes.add(n);
        return n;
    }

    public FluidEdge connect(FluidNode a, double portA, FluidNode b, double portB, double k) {
        FluidEdge e = new FluidEdge(a, portA, b, portB, k);
        edges.add(e);
        return e;
    }

    public void step(double dt) {
        // 1. heat
        for (FluidNode n : nodes) {
            if (n.fixed) { n.heat = 0; continue; }
            double q = n.heat - n.lossWPerK * (n.T() - Water.T_AMBIENT);
            n.heat = 0;
            if (q != 0) {
                n.energy += q * dt;
                n.update();
            }
        }
        // 2. edges: upstream side, what they carry, conductance; and how each end's pressure answers to that packet
        List<FluidNode> free = new ArrayList<>();
        java.util.Map<FluidNode, Integer> idx = new java.util.IdentityHashMap<>();
        for (FluidNode n : nodes)
            if (!n.fixed) {
                idx.put(n, free.size());
                free.add(n);
            }
        int N = free.size();
        Flash probe = new Flash();
        // each free node's pressure response per kg of liquid and per kg of its own gas (one per phase, whichever edge
        // brings or takes it: per-edge responses made in != out at a small node in a steady through-flow)
        double[] sL = new double[N], sG = new double[N];
        for (int i = 0; i < N; i++) {
            FluidNode n = free.get(i);
            double T = n.T();
            sL[i] = dPdm(n, 1, 0, 0, Water.hLiquid(T), probe);
            double[] z = gasMix(n);
            sG[i] = dPdm(n, 0, z[0], z[1], z[2], probe);
        }
        for (FluidEdge e : edges) {
            chooseUpstream(e);
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            e.sA = ia == null ? 0 : e.xl * sL[ia] + (1 - e.xl) * sG[ia];
            e.sB = ib == null ? 0 : e.xl * sL[ib] + (1 - e.xl) * sG[ib];
        }
        // 3. implicit pressures: P_i' = P_i + dt sum (dP/dm)_i,e x flow into i (flows at the new pressures); solved
        //    again if a check valve would run backwards
        double[] P = new double[N];
        for (int pass = 0; pass < 3; pass++) {
            double[][] A = new double[N][N];
            double[] rhs = new double[N];
            for (int i = 0; i < N; i++) {
                A[i][i] = 1;
                rhs[i] = free.get(i).P();
            }
            for (FluidEdge e : edges) {
                if (e.g == 0) continue;
                // flow a->b = g (Pa' - Pb' + c), c = the port offsets and the head
                double c = e.a.pressureAt(e.portA) - e.a.P() - (e.b.pressureAt(e.portB) - e.b.P()) + e.head;
                Integer ia = idx.get(e.a), ib = idx.get(e.b);
                for (int end = 0; end < 2; end++) {
                    Integer i = end == 0 ? ia : ib;
                    if (i == null) continue;
                    double k = dt * (end == 0 ? -e.sA : e.sB) * e.g;   // (a loses the flow, b gains it)
                    // row i: P_i' - k (Pa' - Pb') = P_i + k c
                    if (ia != null) A[i][ia] -= k;
                    else rhs[i] += k * e.a.P();
                    if (ib != null) A[i][ib] += k;
                    else rhs[i] -= k * e.b.P();
                    rhs[i] += k * c;
                }
            }
            P = solve(A, rhs);
            boolean again = false;
            for (FluidEdge e : edges) {
                double pa = e.a.fixed ? e.a.P() : P[idx.get(e.a)], pb = e.b.fixed ? e.b.P() : P[idx.get(e.b)];
                double offA = e.a.pressureAt(e.portA) - e.a.P(), offB = e.b.pressureAt(e.portB) - e.b.P();
                e.flow = e.g * (pa + offA - pb - offB + e.head);
                if (e.oneWay() && e.flow < 0 && e.g != 0) {
                    e.g = 0;
                    e.flow = 0;
                    again = true;
                }
            }
            if (!again) break;
        }
        // 6. no node's pressure may be driven past the pressures its neighbours push it towards (the linearized solve
        //    can overshoot when a node changes phase or an edge's upstream side flips; a maximum principle); then cap
        //    what leaves each node
        for (int pass = 0; pass < 3; pass++) {
            limitNoReversal(dt);
            limitToNeighbourRange(dt);
        }
        capOutflows(dt);
        // 7. move contents: each edge's liquid and gas shares from its upstream port (the step's starting contents);
        //    the phase caps; what passes through each node (an implicit upwind solve); then the transfer
        int E = edges.size();
        double[] xl = new double[E];
        for (int k = 0; k < E; k++) {
            FluidEdge e = edges.get(k);
            if (e.flow == 0) continue;
            FluidNode u = e.flow > 0 ? e.a : e.b;
            double[] pk = packet(u, port(e, u));
            xl[k] = pk[4] > 0 ? pk[0] : 0;
            if (pk[4] <= 0) e.flow = 0;
        }
        double[][] theta = capPhases(dt, free, idx, xl);
        double[][] pass = passThrough(free, idx, xl, theta);
        for (int k = 0; k < E; k++) transfer(edges.get(k), dt, xl[k], free, idx, theta, pass);
        // 8. new equilibria
        for (FluidNode n : free) n.update();
    }

    /** Port band (fraction of a node's height) over which a port goes from drawing gas to drawing liquid. */
    static final double BAND = 0.1;

    /**
     * What flows out of node u through a port at height fraction port, per kg: liquid, vapour and air shares, specific
     * enthalpy (J/kg), density (kg/m^3) and viscosity (Pa s). A port near the liquid level draws a mix, so the flow
     * doesn't flip between liquid and gas as the level passes it (that chattered).
     */
    static double[] packet(FluidNode u, double port) {
        double T = u.T();
        double mv = u.mVapour(), ma = u.mAir, mg = mv + ma;
        double w;   // liquid share of the volume drawn
        if (u.mLiquid() <= 0) w = 0;
        else if (mg <= 0) w = 1;
        else {
            // (all liquid once the level is a band above the port; a port at the very top counts as one band lower, or a
            // node filling from below could never spill out of it: its level only approaches the top, as an inflow may
            // fill at most half the free space per step. Dysnomiaw's pump pushed 9 bar into a full riser that drew
            // only its last wisp of air into the boiler's feed.)
            double x = Math.max(0, Math.min(1, (u.level() - Math.min(port, 1 - BAND)) / BAND));
            w = x * x * (3 - 2 * x);
        }
        double rhoG = mg > 0 ? mg / u.state.vGas : 0;
        double rho = w * Water.RHO_L + (1 - w) * rhoG;
        double muG = mg > 0 ? (mv * Water.MU_V + ma * Water.MU_AIR) / mg : Water.MU_AIR;
        double mu = w * Water.MU_L + (1 - w) * muG;
        double xl, xv, xa;
        if (rho > 0) {
            xl = w * Water.RHO_L / rho;
            xv = (1 - xl) * (mg > 0 ? mv / mg : 0);
            xa = (1 - xl) * (mg > 0 ? ma / mg : 1);
        } else {
            xl = 0; xv = 0; xa = 1;
            T = Water.T_AMBIENT;
        }
        double h = xl * Water.hLiquid(T) + xv * Water.hVapour(T) + xa * Water.hAir(T);
        // the gravity head weighs what is moved by mass: lifting a stream that is mostly liquid by mass takes a
        // liquid's head (by volume it may be mostly gas; with that, two pipes between two vessels kept water
        // circulating for ever)
        double rhoHead = xl * Water.RHO_L + (1 - xl) * rhoG;
        return new double[]{xl, xv, xa, h, rho, mu, rhoHead};
    }

    /**
     * How n's pressure rises per kg arriving of a packet with these shares (liquid, vapour, air) and specific enthalpy;
     * the same rate applies to it leaving. By a small flash step.
     */
    static double dPdm(FluidNode n, double xl, double xv, double xa, double h, Flash probe) {
        double d = Math.max(1e-7, 1e-4 * n.mass());
        probe.solve(n.mWater + (xl + xv) * d, n.mAir + xa * d, n.energy + h * d, n.volume, n.T());
        double s = (probe.P - n.P()) / d;
        if (!(s > 0)) s = Water.R_AIR * Math.max(n.T(), Flash.T_MIN) / n.volume;   // (an empty node: as air)
        return s;
    }

    /** Which end the flow comes from (by the current pressures and the head), and what it carries. */
    void chooseUpstream(FluidEdge e) {
        double pa = e.a.pressureAt(e.portA), pb = e.b.pressureAt(e.portB);
        boolean fromA = pa >= pb;
        for (int t = 0; t < 2; t++) {
            props(e, fromA);
            double drive = pa - pb + e.head;
            if ((drive >= 0) == fromA) break;
            fromA = !fromA;
        }
        props(e, fromA);
        if (e.oneWay() && !fromA) e.g = 0;
    }

    private static void props(FluidEdge e, boolean fromA) {
        double[] pk = fromA ? packet(e.a, e.portA) : packet(e.b, e.portB);
        e.fromA = fromA;
        e.rho = pk[4];
        e.xl = pk[0];
        e.head = pk[6] * G * (e.zA() - e.zB()) + e.boost;
        e.g = pk[4] * e.k * e.open / pk[5];
    }

    /** Each node's pressure change this step if the flows run as they are (net, by each edge's dP/dm at that end). */
    private java.util.Map<FluidNode, Double> rises(double dt) {
        java.util.Map<FluidNode, Double> r = new java.util.IdentityHashMap<>();
        for (FluidEdge e : edges) {
            if (e.flow == 0) continue;
            if (!e.a.fixed) r.merge(e.a, -e.sA * e.flow * dt, Double::sum);
            if (!e.b.fixed) r.merge(e.b, e.sB * e.flow * dt, Double::sum);
        }
        return r;
    }

    /** An edge whose drive would change sign by the end of the step (counting both ends' net change) is cut back to
     *  where it would just even out. */
    private void limitNoReversal(double dt) {
        var r = rises(dt);
        for (FluidEdge e : edges) {
            if (e.flow == 0) continue;
            double d = e.a.pressureAt(e.portA) - e.b.pressureAt(e.portB) + e.head;
            if (d == 0 || (d > 0) != (e.flow > 0)) {
                e.flow = 0;
                continue;
            }
            double d1 = d + r.getOrDefault(e.a, 0.0) - r.getOrDefault(e.b, 0.0);
            if (d1 != 0 && (d1 > 0) != (d > 0)) e.flow *= 0.9 * d / (d - d1);
        }
    }

    private void limitToNeighbourRange(double dt) {
        for (FluidNode n : nodes) {
            if (n.fixed) continue;
            double P = n.P(), lo = P, hi = P, rise = 0, inRise = 0, outFall = 0;
            for (FluidEdge e : edges) {
                if (e.a != n && e.b != n) continue;
                if (e.oneWay() && e.g == 0) continue;   // (a shut check valve pushes nothing either way)
                double offA = e.a.pressureAt(e.portA) - e.a.P(), offB = e.b.pressureAt(e.portB) - e.b.P();
                // the pressure this edge pushes n towards (where its drive would be zero)
                double towards = e.b == n ? e.a.P() + offA - offB + e.head : e.b.P() + offB - offA - e.head;
                lo = Math.min(lo, towards);
                hi = Math.max(hi, towards);
                if (e.flow == 0) continue;
                double into = e.b == n ? e.flow : -e.flow;   // kg/s into n
                double sN = e.b == n ? e.sB : e.sA;
                rise += sN * into * dt;
                if (into > 0) inRise += sN * into * dt;
                else outFall -= sN * into * dt;
            }
            if (P + rise > hi + 1e-9 && inRise > 0) {
                double f = Math.max(0, Math.min(1, (hi - P + outFall) / inRise));
                for (FluidEdge e : edges) if ((e.b == n && e.flow > 0) || (e.a == n && e.flow < 0)) e.flow *= f;
            } else if (P + rise < lo - 1e-9 && outFall > 0) {
                double f = Math.max(0, Math.min(1, (P - lo + inRise) / outFall));
                for (FluidEdge e : edges) if ((e.a == n && e.flow > 0) || (e.b == n && e.flow < 0)) e.flow *= f;
            }
        }
    }

    /** The liquid level limits (the phase caps and pass-through come after: capPhases). */
    private void capOutflows(double dt) {
        // ... and the liquid level: a node may fill at most half its free space per step (the pressure answers
        // liquid steeply only once it is full; without this a step can overfill it far into its stiff range), and
        // its level may move at most half a port band per step either way (else a port near the level flips between
        // drawing gas and liquid every step, and the water sloshes back and forth for ever)
        for (FluidNode n : nodes) {
            if (n.fixed) continue;
            double inL = 0, outL = 0;
            for (FluidEdge e : edges) {
                double f = out(e, n);
                if (f > 0) outL += f * packet(n, port(e, n))[0];
                else if (f < 0) {
                    FluidNode o = other(e, n);
                    inL -= f * packet(o, port(e, o))[0];
                }
            }
            double band = 0.5 * BAND * n.volume * Water.RHO_L / dt;
            double room = 0.5 * Math.max(0, n.state.vGas - Flash.GAS_MIN * n.volume) + 1e-4 * n.volume;
            double limIn = Math.min(room * Water.RHO_L / dt, band);
            if (inL - outL > limIn) {
                double sc = (limIn + outL) / inL;
                for (FluidEdge e : edges) {
                    if (out(e, n) >= 0) continue;
                    FluidNode o = other(e, n);
                    if (packet(o, port(e, o))[0] > 0) e.flow *= sc;
                }
            } else if (outL - inL > band) {
                double sc = (band + inL) / outL;
                for (FluidEdge e : edges)
                    if (out(e, n) > 0 && packet(n, port(e, n))[0] > 0) e.flow *= sc;
            }
        }
    }

    /** A node's gas: vapour share, air share, specific enthalpy (J/kg); all vapour if it holds no gas. */
    static double[] gasMix(FluidNode n) {
        double T = n.T(), mv = n.mVapour(), ma = n.mAir, mg = mv + ma;
        if (mg <= 0) return new double[]{1, 0, Water.hVapour(T)};
        double yv = mv / mg, ya = ma / mg;
        return new double[]{yv, ya, yv * Water.hVapour(T) + ya * Water.hAir(T)};
    }

    /**
     * Per free node: theta[i] = {liquid, gas}, the share of what leaves it of that phase that comes out of what it holds
     * (the rest is what flowed in, passed through). A node gives at most DRAIN_CAP of each phase it holds per step, net
     * of what of that phase flows in; outflows beyond that are cut (each edge by its tighter phase). Cutting one node's
     * outflows lowers its neighbours' inflows, so a few passes.
     */
    private double[][] capPhases(double dt, List<FluidNode> free, java.util.Map<FluidNode, Integer> idx, double[] xl) {
        int N = free.size(), E = edges.size();
        double[] haveL = new double[N], haveG = new double[N];
        for (int i = 0; i < N; i++) {
            FluidNode n = free.get(i);
            haveL[i] = Math.max(0, n.mLiquid());
            haveG[i] = Math.max(0, n.mVapour() + n.mAir);
        }
        double[] outL = new double[N], outG = new double[N], inL = new double[N], inG = new double[N];
        for (int pass = 0; pass < 200; pass++) {   // (usually 1 or 2; the last pass finds nothing to cut, so the sums are current)
            java.util.Arrays.fill(outL, 0);
            java.util.Arrays.fill(outG, 0);
            java.util.Arrays.fill(inL, 0);
            java.util.Arrays.fill(inG, 0);
            for (int k = 0; k < E; k++) {
                FluidEdge e = edges.get(k);
                if (e.flow == 0) continue;
                FluidNode u = e.flow > 0 ? e.a : e.b, d = e.flow > 0 ? e.b : e.a;
                double m = Math.abs(e.flow);
                Integer iu = idx.get(u), id = idx.get(d);
                if (iu != null) {
                    outL[iu] += m * xl[k];
                    outG[iu] += m * (1 - xl[k]);
                }
                if (id != null) {
                    inL[id] += m * xl[k];
                    inG[id] += m * (1 - xl[k]);
                }
            }
            boolean cut = false;
            for (int i = 0; i < N; i++) {
                double capL = DRAIN_CAP * haveL[i] / dt + inL[i], capG = DRAIN_CAP * haveG[i] / dt + inG[i];
                double fL = outL[i] > capL * (1 + 1e-9) ? capL / outL[i] : 1, fG = outG[i] > capG * (1 + 1e-9) ? capG / outG[i] : 1;
                if (fL >= 1 && fG >= 1) continue;
                cut = true;
                FluidNode n = free.get(i);
                for (int k = 0; k < E; k++) {
                    FluidEdge e = edges.get(k);
                    if (out(e, n) <= 0) continue;
                    double f = 1;
                    if (xl[k] > 0) f = Math.min(f, fL);
                    if (xl[k] < 1) f = Math.min(f, fG);
                    e.flow *= f;
                }
            }
            if (!cut) break;
        }
        double[][] theta = new double[N][2];
        for (int i = 0; i < N; i++) {
            theta[i][0] = outL[i] > 0 && outL[i] * dt > DRAIN_CAP * haveL[i] ? DRAIN_CAP * haveL[i] / (outL[i] * dt) : 1;
            theta[i][1] = outG[i] > 0 && outG[i] * dt > DRAIN_CAP * haveG[i] ? DRAIN_CAP * haveG[i] / (outG[i] * dt) : 1;
        }
        return theta;
    }

    /**
     * What passes through each free node this step, as an implicit upwind solve: pass[i] = {liquid enthalpy (J/kg),
     * gas vapour share, gas air share, gas enthalpy (J/kg)}, the flow-weighted mix of what flows in, where what flows
     * in from a node is its own contents' share (theta) plus its own pass-through for the rest. A node nothing flows
     * into passes its own contents.
     */
    private double[][] passThrough(List<FluidNode> free, java.util.Map<FluidNode, Integer> idx, double[] xl, double[][] theta) {
        int N = free.size(), E = edges.size();
        double[][] own = new double[N][4];
        for (int i = 0; i < N; i++) {
            FluidNode n = free.get(i);
            double[] z = gasMix(n);
            own[i] = new double[]{Water.hLiquid(n.T()), z[0], z[1], z[2]};
        }
        double[][] pass = new double[N][8];   // (4..7: its own contents, read once: transfers change mAir as they go)
        for (int i = 0; i < N; i++) System.arraycopy(own[i], 0, pass[i], 4, 4);
        // liquid (column 0) and gas (columns 1..3): (sum in) X_i - sum_in q (1 - theta_u) X_u = sum_in q theta_u own_u
        for (int phase = 0; phase < 2; phase++) {
            double[][] A = new double[N][N];
            double[][] b = new double[phase == 0 ? 1 : 3][N];
            double[] inSum = new double[N];
            for (int k = 0; k < E; k++) {
                FluidEdge e = edges.get(k);
                if (e.flow == 0) continue;
                double q = Math.abs(e.flow) * (phase == 0 ? xl[k] : 1 - xl[k]);
                if (q <= 0) continue;
                FluidNode u = e.flow > 0 ? e.a : e.b, d = e.flow > 0 ? e.b : e.a;
                Integer iu = idx.get(u), id = idx.get(d);
                if (id == null) continue;
                inSum[id] += q;
                double th = iu == null ? 1 : theta[iu][phase];
                double[] ownU = iu == null ? fixedOwn(u) : own[iu];
                if (iu != null && th < 1) A[id][iu] -= q * (1 - th);
                for (int c = 0; c < b.length; c++) b[c][id] += q * th * ownU[phase == 0 ? 0 : 1 + c];
            }
            for (int i = 0; i < N; i++) {
                if (inSum[i] > 0) A[i][i] += inSum[i];
                else {   // (nothing flows in: it passes its own)
                    java.util.Arrays.fill(A[i], 0);
                    A[i][i] = 1;
                    for (int c = 0; c < b.length; c++) b[c][i] = own[i][phase == 0 ? 0 : 1 + c];
                }
            }
            for (int c = 0; c < b.length; c++) {
                double[][] Ac = new double[N][];
                for (int i = 0; i < N; i++) Ac[i] = A[i].clone();
                double[] x = solve(Ac, b[c]);
                for (int i = 0; i < N; i++) pass[i][phase == 0 ? 0 : 1 + c] = x[i];
            }
        }
        return pass;
    }

    /** A fixed node's contents as passThrough reads them (it always gives its own). */
    private static double[] fixedOwn(FluidNode u) {
        double[] z = gasMix(u);
        return new double[]{Water.hLiquid(u.T()), z[0], z[1], z[2]};
    }

    private static FluidNode other(FluidEdge e, FluidNode n) {
        return e.a == n ? e.b : e.a;
    }

    private static double port(FluidEdge e, FluidNode n) {
        return e.a == n ? e.portA : e.portB;
    }

    /** The flow leaving n through e (kg/s; <= 0 if none). */
    private static double out(FluidEdge e, FluidNode n) {
        if (e.a == n) return e.flow;
        if (e.b == n) return -e.flow;
        return 0;
    }

    /**
     * Move one edge's flow: its liquid share at the liquid enthalpy, its gas share at the gas mix and enthalpy, each from
     * the upstream node's own contents (theta) and from what passes through it (the rest).
     */
    private static void transfer(FluidEdge e, double dt, double xl, List<FluidNode> free, java.util.Map<FluidNode, Integer> idx,
                                 double[][] theta, double[][] pass) {
        e.fLiquid = e.fVapour = e.fAir = e.fEnthalpy = 0;
        e.power = 0;
        if (e.flow == 0) return;
        FluidNode u = e.flow > 0 ? e.a : e.b, d = e.flow > 0 ? e.b : e.a;
        Integer iu = idx.get(u);
        double[] own = fixedOwn(u);
        double thL = 1, thG = 1;
        double[] through = own;
        if (iu != null) {
            thL = theta[iu][0];
            thG = theta[iu][1];
            through = pass[iu];
            own = java.util.Arrays.copyOfRange(pass[iu], 4, 8);
        }
        double m = Math.abs(e.flow);
        double liq = m * xl, gas = m * (1 - xl);
        double hL = thL * own[0] + (1 - thL) * through[0];
        double yv = thG * own[1] + (1 - thG) * through[1], ya = thG * own[2] + (1 - thG) * through[2];
        double hG = thG * own[3] + (1 - thG) * through[3];
        double ys = yv + ya;
        if (ys > 0) {
            yv /= ys;
            ya /= ys;
        }
        double vap = gas * yv, air = gas * ya, h = liq * hL + gas * hG;
        double toD = h;
        if (e.kind == FluidEdge.Kind.TURBINE) {
            e.power = Math.max(0, FluidEdge.TURBINE_EFFICIENCY * vap * (Water.hVapour(u.T()) - Water.hLiquid(Water.TB)));
            toD -= e.power;
        }
        if (!u.fixed) {
            u.mWater -= (liq + vap) * dt;
            u.mAir -= air * dt;
            u.energy -= h * dt;
        }
        if (!d.fixed) {
            d.mWater += (liq + vap) * dt;
            d.mAir += air * dt;
            d.energy += toD * dt;
        }
        double sign = e.flow > 0 ? 1 : -1;
        e.fLiquid = sign * liq;
        e.fVapour = sign * vap;
        e.fAir = sign * air;
        e.fEnthalpy = sign * h;
    }

    /** Gaussian elimination with partial pivoting (A and b are overwritten). */
    static double[] solve(double[][] A, double[] b) {
        int n = b.length;
        for (int c = 0; c < n; c++) {
            int p = c;
            for (int r = c + 1; r < n; r++) if (Math.abs(A[r][c]) > Math.abs(A[p][c])) p = r;
            double[] t = A[c]; A[c] = A[p]; A[p] = t;
            double tb = b[c]; b[c] = b[p]; b[p] = tb;
            double piv = A[c][c];
            if (piv == 0) continue;
            for (int r = c + 1; r < n; r++) {
                double f = A[r][c] / piv;
                if (f == 0) continue;
                for (int k = c; k < n; k++) A[r][k] -= f * A[c][k];
                b[r] -= f * b[c];
            }
        }
        double[] x = new double[n];
        for (int r = n - 1; r >= 0; r--) {
            double s = b[r];
            for (int k = r + 1; k < n; k++) s -= A[r][k] * x[k];
            x[r] = A[r][r] == 0 ? 0 : s / A[r][r];
        }
        return x;
    }
}
