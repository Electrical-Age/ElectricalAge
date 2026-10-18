package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * One connected fluid network and its step (notes/pipes/DESIGN.md section 6): heat in, an implicit pressure solve
 * ((C/dt + G) P' = C/dt P + gravity head), then the flows at those pressures move water, air and energy upwind, and
 * every node finds its new equilibrium (Flash). Mass is conserved exactly: each flow leaves one node and enters the
 * other. Small networks (tens of nodes): a dense solve each step.
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
        for (FluidEdge e : edges) {
            chooseUpstream(e);
            e.sA = e.a.fixed ? 0 : dPdm(e.a, e, probe);
            e.sB = e.b.fixed ? 0 : dPdm(e.b, e, probe);
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
                if (e.kind == FluidEdge.Kind.CHECK && e.flow < 0 && e.g != 0) {
                    e.g = 0;
                    e.flow = 0;
                    again = true;
                }
            }
            if (!again) break;
        }
        // 6. cap what leaves each node per phase
        capOutflows(dt);
        // 7. move contents
        for (FluidEdge e : edges) transfer(e, dt);
        // 8. new equilibria
        for (FluidNode n : free) n.update();
    }

    /**
     * How n's pressure rises per kg of e's packet arriving (liquid, or gas of the upstream node's mix, with its
     * enthalpy); the same rate applies to it leaving. By a small flash step.
     */
    static double dPdm(FluidNode n, FluidEdge e, Flash probe) {
        FluidNode u = e.fromA ? e.a : e.b;
        double port = e.fromA ? e.portA : e.portB;
        double fw, fa, h;
        double T = u.T();
        if (u.mLiquid() > 0 && port < u.level()) {
            fw = 1; fa = 0; h = Water.hLiquid(T);
        } else {
            double mv = u.mVapour(), ma = u.mAir, m = mv + ma;
            if (m <= 0) { fw = 0; fa = 1; T = Water.T_AMBIENT; }
            else { fw = mv / m; fa = ma / m; }
            h = fw * Water.hVapour(T) + fa * Water.hAir(T);
        }
        double d = Math.max(1e-7, 1e-4 * n.mass());
        probe.solve(n.mWater + fw * d, n.mAir + fa * d, n.energy + h * d, n.volume, n.T());
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
        if (e.kind == FluidEdge.Kind.CHECK && !fromA) e.g = 0;
    }

    private static void props(FluidEdge e, boolean fromA) {
        FluidNode u = fromA ? e.a : e.b;
        double port = fromA ? e.portA : e.portB;
        boolean liquid = u.mLiquid() > 0 && port < u.level();
        double rho, mu;
        if (liquid) {
            rho = Water.RHO_L;
            mu = Water.MU_L;
        } else {
            double mv = u.mVapour(), ma = u.mAir, m = mv + ma;
            rho = m > 0 ? m / u.state.vGas : 0;
            mu = m > 0 ? (mv * Water.MU_V + ma * Water.MU_AIR) / m : Water.MU_AIR;
        }
        e.fromA = fromA;
        e.rho = rho;
        e.head = rho * G * (e.zA() - e.zB());
        e.g = rho * e.k * e.open / mu;
    }

    private void capOutflows(double dt) {
        for (FluidNode n : nodes) {
            if (n.fixed) continue;
            double outL = 0, outG = 0;
            for (FluidEdge e : edges) {
                double f = out(e, n);
                if (f <= 0) continue;
                if (liquidFrom(e, n)) outL += f;
                else outG += f;
            }
            double haveL = n.mLiquid(), haveG = n.mVapour() + n.mAir;
            double sL = outL * dt > DRAIN_CAP * haveL ? DRAIN_CAP * haveL / (outL * dt) : 1;
            double sG = outG * dt > DRAIN_CAP * haveG ? DRAIN_CAP * haveG / (outG * dt) : 1;
            if (sL < 1 || sG < 1)
                for (FluidEdge e : edges) {
                    if (out(e, n) <= 0) continue;
                    e.flow *= liquidFrom(e, n) ? sL : sG;
                }
        }
    }

    /** The flow leaving n through e (kg/s; <= 0 if none). */
    private static double out(FluidEdge e, FluidNode n) {
        if (e.a == n) return e.flow;
        if (e.b == n) return -e.flow;
        return 0;
    }

    private static boolean liquidFrom(FluidEdge e, FluidNode n) {
        double port = e.a == n ? e.portA : e.portB;
        return n.mLiquid() > 0 && port < n.level();
    }

    private static void transfer(FluidEdge e, double dt) {
        e.fLiquid = e.fVapour = e.fAir = e.fEnthalpy = 0;
        e.power = 0;
        if (e.flow == 0) return;
        FluidNode u = e.flow > 0 ? e.a : e.b, d = e.flow > 0 ? e.b : e.a;
        double m = Math.abs(e.flow);
        double T = u.T();
        double liq = 0, vap = 0, air = 0, h;
        if (liquidFrom(e, u)) {
            liq = m;
            h = m * Water.hLiquid(T);
        } else {
            double mv = u.mVapour(), ma = u.mAir, tot = mv + ma;
            if (tot <= 0) return;
            vap = m * mv / tot;
            air = m * ma / tot;
            h = vap * Water.hVapour(T) + air * Water.hAir(T);
        }
        double toD = h;
        if (e.kind == FluidEdge.Kind.TURBINE) {
            e.power = Math.max(0, FluidEdge.TURBINE_EFFICIENCY * vap * (Water.hVapour(T) - Water.hLiquid(Water.TB)));
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
