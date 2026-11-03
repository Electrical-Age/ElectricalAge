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
 * <p>
 * Any species (Species): a port draws gas above the liquid, else the liquid layer at its height, blended across a
 * band at each interface; a node gives at most DRAIN_CAP of each layer and of its gas per step; the pass-through solve
 * carries each species' share. Water and air alone give the numbers they gave before species were general.
 */
public final class FluidNetwork {
    public static final double G = 9.81;
    /** Outflow per step is capped at this share of what the upstream node holds of that phase. */
    static final double DRAIN_CAP = 0.5;

    public final List<FluidNode> nodes = new ArrayList<>();
    public final List<FluidEdge> edges = new ArrayList<>();
    /** This step's solved pressures (the bounds the limiters hold nodes to). */
    private final java.util.Map<FluidNode, Double> solved = new java.util.IdentityHashMap<>();

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
        for (FluidEdge e : edges) {
            e.pkA = null;
            e.pkB = null;
        }
        Flash probe = new Flash();
        // each free node's pressure response per kg of liquid and per kg of its own gas (one per phase, whichever edge
        // brings or takes it: per-edge responses made in != out at a small node in a steady through-flow)
        // (the liquid probe is the node's own liquid mix, per kg; rhoL its density, to answer a packet of another liquid by
        // its volume: Argon's review)
        double[] sL = new double[N], sG = new double[N], rhoL = new double[N];
        for (int i = 0; i < N; i++) {
            FluidNode n = free.get(i);
            Mix lm = liquidMix(n), gm = gasMix(n);
            sL[i] = dPdm(n, lm.share, null, lm.h, probe);
            rhoL[i] = lm.rho;
            sG[i] = dPdm(n, null, gm.share, gm.h, probe);
        }
        for (FluidEdge e : edges) {
            chooseUpstream(e);
            Integer ia = idx.get(e.a), ib = idx.get(e.b);
            e.sA = ia == null ? 0 : response(e, sL[ia], rhoL[ia], sG[ia]);
            e.sB = ib == null ? 0 : response(e, sL[ib], rhoL[ib], sG[ib]);
        }
        // 3. implicit pressures: P_i' = P_i + dt sum (dP/dm)_i,e x flow into i (flows at the new pressures); solved
        //    again if a check valve would run backwards, or a flow runs against the side it was taken from (the edge
        //    then takes its packet from the other side: a short pipe drained by its valve draws from its boiler even
        //    when it started the step at a higher pressure)
        double[] P = new double[N];
        for (int pass = 0; pass < 8; pass++) {
            double[][] A = new double[N][N];
            double[] rhs = new double[N];
            for (int i = 0; i < N; i++) {
                A[i][i] = 1;
                rhs[i] = free.get(i).P();
            }
            for (FluidEdge e : edges) {
                if (e.g == 0) continue;
                // flow a->b = g (Pa' - Pb' + c), c = the port offsets and the head
                double c = e.a.pressureOver(e.portA, e.spanA) - e.a.P() - (pressureB(e) - e.b.P()) + e.head;
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
                double offA = e.a.pressureOver(e.portA, e.spanA) - e.a.P(), offB = pressureB(e) - e.b.P();
                e.flow = e.g * (pa + offA - pb - offB + e.head);
                if (e.oneWay() && e.flow < 0 && e.g != 0) {
                    e.g = 0;
                    e.flow = 0;
                    again = true;
                } else if (e.area > 0 && e.g != 0 && pass < 7) {
                    // a valve's orifice law, at the drop just solved (between its guess and this, for a quiet fixed point)
                    double gt = orificeG(e, pa + offA - pb - offB + e.head);
                    if (Math.abs(gt - e.g) > 0.05 * e.g) {
                        e.g = Math.sqrt(e.g * gt);
                        again = true;
                    }
                }
                if (!e.oneWay() && e.flow != 0 && (e.flow > 0) != e.fromA && pass < 3) {
                    props(e, !e.fromA);
                    Integer ja = idx.get(e.a), jb = idx.get(e.b);
                    e.sA = ja == null ? 0 : response(e, sL[ja], rhoL[ja], sG[ja]);
                    e.sB = jb == null ? 0 : response(e, sL[jb], rhoL[jb], sG[jb]);
                    again = true;
                }
            }
            if (!again) break;
        }
        // 6. no node's pressure may be driven past the pressures its neighbours push it towards (the linearized solve
        //    can overshoot when a node changes phase or an edge's upstream side flips; a maximum principle); then cap
        //    what leaves each node
        solved.clear();
        for (int i = 0; i < N; i++) solved.put(free.get(i), P[i]);
        for (int pass = 0; pass < 3; pass++) {
            limitNoReversal(dt);
            limitToNeighbourRange(dt);
        }
        capOutflows(dt);
        // 7. move contents: each edge's liquid and gas shares from its upstream port (the step's starting contents);
        //    the phase caps; what passes through each node (an implicit upwind solve); then the transfer
        int E = edges.size();
        double[] xl = new double[E];
        Packet[] pks = new Packet[E];
        for (int k = 0; k < E; k++) {
            FluidEdge e = edges.get(k);
            if (e.flow == 0) continue;
            FluidNode u = e.flow > 0 ? e.a : e.b;
            Packet pk = packet(e, u);
            pks[k] = pk;
            xl[k] = pk.rho > 0 ? pk.xl : 0;
            if (pk.rho <= 0) e.flow = 0;
        }
        Own[] own = new Own[N];
        for (int i = 0; i < N; i++) own[i] = new Own(free.get(i));
        double[][] theta = capPhases(dt, free, idx, xl, pks);
        Pass[] pass = passThrough(free, idx, xl, pks, theta, own);
        for (int k = 0; k < E; k++) transfer(edges.get(k), dt, xl[k], pks[k], idx, theta, pass, own);
        // 8. new equilibria
        for (FluidNode n : free) n.update();
    }

    /** Port band (fraction of a node's height) over which a port goes from drawing gas to drawing liquid. */
    static final double BAND = 0.1;

    /**
     * What flows out of a node through a port: its liquid (mass) share; each node layer's share of that liquid (from the
     * bottom; sums to 1); each species' share of its gas (by id; sums to 1); specific enthalpy (J/kg); density (kg/m^3);
     * viscosity (Pa s); the density its gravity head weighs (by mass); and its liquid's density.
     */
    static final class Packet {
        double xl, h, rho, mu, rhoHead, rhoLiq, T;
        double[] layerShare, gas;
        FluidNode node;

        /** Water vapour's share of the packet (by mass). */
        double xv() {
            return (1 - xl) * gas[SpeciesRegistry.WATER];
        }
    }

    /** The packet the upstream end u of e gives this step (cached on the edge). */
    static Packet packet(FluidEdge e, FluidNode u) {
        if (u == e.a) {
            if (e.pkA == null) e.pkA = packet(e.a, e.portA, e.spanA);
            return e.pkA;
        }
        if (e.pkB == null) e.pkB = packet(e.b, e.portB, e.spanB);
        return e.pkB;
    }

    /**
     * The port window's liquid share for a liquid top at fraction top: a smooth step a band wide over a point port, or as
     * tall as an opening (span) over it, wet as far as the liquid reaches up it.
     */
    private static double window(double top, double port, double span) {
        double x;
        if (span > BAND) {
            double lo = Math.max(0, port - span / 2), hi = Math.min(1, port + span / 2);
            x = hi > lo ? Math.max(0, Math.min(1, (top - lo) / (hi - lo))) : (top >= port ? 1 : 0);
        } else x = Math.max(0, Math.min(1, (top - Math.min(port, 1 - BAND)) / BAND));
        return x * x * (3 - 2 * x);
    }

    /**
     * What flows out of node u through a port at height fraction port. A port near an interface (gas over liquid, or one
     * liquid layer over another) draws a mix, so the flow doesn't flip between them as the interface passes it (that
     * chattered): each layer's share is the window at its top less the window at its bottom.
     */
    static Packet packet(FluidNode u, double port) {
        return packet(u, port, 0);
    }

    /** The same through an opening span tall (a share of u's height, centred on port): FluidPort.span. */
    static Packet packet(FluidNode u, double port, double span) {
        Packet pk = new Packet();
        pk.node = u;
        Flash st = u.state;
        double T = u.T();
        SpeciesRegistry reg = SpeciesRegistry.get();
        int nb = st.n;
        // the gas: its species' shares, density and viscosity
        double mg = 0;
        for (int i = 0; i < nb; i++) mg += st.vap[i];
        pk.gas = new double[nb];
        double muNum = 0;
        if (mg > 0) {
            for (int i = 0; i < nb; i++) {
                if (st.vap[i] == 0) continue;
                pk.gas[i] = st.vap[i] / mg;
                muNum += st.vap[i] * reg.byId(i).muV;
            }
        } else pk.gas[SpeciesRegistry.AIR] = 1;
        // the liquid: each layer's window share (by volume)
        int L = st.layers;
        pk.layerShare = new double[Math.max(L, 1)];
        double w = 0;
        double[] wk = new double[Math.max(L, 1)];
        if (L > 0 && st.vLiquid > 0) {
            if (mg <= 0) {
                // (no gas: all liquid, the layers by the window, or the top one if the port is above them all)
                double below = 0, sum = 0;
                for (int l = 0; l < L; l++) {
                    double top = L == 1 ? u.level() : st.layerTop[l];
                    double wt = window(top, port, span);
                    wk[l] = wt - below;
                    below = wt;
                    sum += wk[l];
                }
                if (sum > 0) for (int l = 0; l < L; l++) wk[l] /= sum;
                else wk[L - 1] = 1;
                w = 1;
            } else {
                // (all liquid once the level is a band above the port; a port at the very top counts as one band lower, or a
                // node filling from below could never spill out of it: its level only approaches the top, as an inflow may
                // fill at most half the free space per step. Dysnomiaw's pump pushed 9 bar into a full riser that drew
                // only its last wisp of air into the boiler's feed.)
                double below = 0;
                for (int l = 0; l < L; l++) {
                    double top = L == 1 ? u.level() : st.layerTop[l];
                    double wt = window(top, port, span);
                    wk[l] = wt - below;
                    below = wt;
                }
                w = below;
            }
        }
        double rhoG = mg > 0 ? mg / st.vGas : 0;
        double liqRho = 0, liqMu = 0;   // (sums over the layers of window share x density, x viscosity)
        for (int l = 0; l < L; l++) {
            if (wk[l] == 0) continue;
            liqRho += wk[l] * st.layerRho[l];
            liqMu += wk[l] * layerMu(st, l, T);
        }
        if (L == 1 && mg <= 0) {
            liqRho = st.layerRho[0];
            liqMu = layerMu(st, 0, T);
        }
        double rho = liqRho + (1 - w) * rhoG;
        double muG = mg > 0 ? muNum / mg : Water.MU_AIR;
        double mu = liqMu + (1 - w) * muG;
        double xl;
        if (rho > 0) {
            xl = liqRho / rho;
        } else {
            xl = 0;
            java.util.Arrays.fill(pk.gas, 0);
            pk.gas[SpeciesRegistry.AIR] = 1;
            T = Water.T_AMBIENT;
        }
        // each layer's share of the liquid by mass
        double liqRhoMix = w > 0 ? liqRho / w : (L > 0 ? st.layerRho[L - 1] : Water.RHO_L);
        if (L == 1) {
            pk.layerShare[0] = 1;
            liqRhoMix = st.layerRho[0];
        } else if (liqRho > 0) for (int l = 0; l < L; l++) pk.layerShare[l] = wk[l] * st.layerRho[l] / liqRho;
        // enthalpy: the liquid (each layer's species at their liquid enthalpy), then each gas species
        double hLiq = 0;
        if (xl > 0) for (int l = 0; l < L; l++) if (pk.layerShare[l] != 0) hLiq += pk.layerShare[l] * layerH(st, l, T);
        double h = xl * hLiq;
        for (int i = 0; i < nb; i++) if (pk.gas[i] != 0) h += (1 - xl) * pk.gas[i] * reg.byId(i).hVapour(T);
        // the gravity head weighs what is moved by mass: lifting a stream that is mostly liquid by mass takes a
        // liquid's head (by volume it may be mostly gas; with that, two pipes between two vessels kept water
        // circulating for ever)
        pk.rhoHead = xl * liqRhoMix + (1 - xl) * rhoG;
        pk.xl = xl;
        pk.h = h;
        pk.rho = rho;
        pk.mu = mu;
        pk.rhoLiq = liqRhoMix;
        pk.T = T;
        return pk;
    }

    /** A layer's viscosity at T: its species' (one), or their mass-weighted log mix. */
    static double layerMu(Flash st, int l, double T) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double lnSum = 0, mSum = 0;
        int only = -1, count = 0;
        for (int i = 0; i < st.n; i++) {
            if (st.liq[i] <= 0 || !st.inLayer(i, l)) continue;
            only = i;
            count++;
            lnSum += st.liq[i] * Math.log(reg.byId(i).muLiquid(T));
            mSum += st.liq[i];
        }
        if (count == 1) return reg.byId(only).muLiquid(T);
        return mSum > 0 ? Math.exp(lnSum / mSum) : Water.MU_L;
    }

    /** A layer's liquid enthalpy at T (J/kg): its species' by their mass shares. */
    static double layerH(Flash st, int l, double T) {
        SpeciesRegistry reg = SpeciesRegistry.get();
        double h = 0;
        for (int i = 0; i < st.n; i++) {
            if (st.liq[i] <= 0 || !st.inLayer(i, l)) continue;
            h += st.liq[i] / st.layerMass[l] * reg.byId(i).hLiquid(T);
        }
        return h;
    }

    /** The per-kg composition (species shares by id), its enthalpy (J/kg) and, for a liquid, its density. */
    static final class Mix {
        double[] share;
        double h, rho;
    }

    /** A node's liquid as a whole (all layers; water if it holds none: the probe then asks how liquid water would answer). */
    static Mix liquidMix(FluidNode n) {
        Mix m = new Mix();
        Flash st = n.state;
        double T = n.T();
        m.share = new double[st.n];
        double total = 0;
        for (int i = 0; i < st.n; i++) if (st.liq[i] > 0) total += st.liq[i];
        int count = 0, only = -1;
        for (int i = 0; i < st.n; i++) if (st.liq[i] > 0) { count++; only = i; }
        SpeciesRegistry reg = SpeciesRegistry.get();
        if (count <= 1) {
            int id = count == 1 ? only : SpeciesRegistry.WATER;
            m.share[id] = 1;
            m.h = reg.byId(id).hLiquid(T);
            m.rho = reg.byId(id).rhoL;
            return m;
        }
        for (int i = 0; i < st.n; i++) {
            if (st.liq[i] <= 0) continue;
            m.share[i] = st.liq[i] / total;
            m.h += m.share[i] * reg.byId(i).hLiquid(T);
        }
        m.rho = total / st.vLiquid;
        return m;
    }

    /**
     * How n's pressure rises per kg arriving of a packet with these species shares (liquid and gas, by id; either may be
     * null) and specific enthalpy; the same rate applies to it leaving. By a small flash step.
     */
    static double dPdm(FluidNode n, double[] liq, double[] gas, double h, Flash probe) {
        // the stiffer of a small and a larger step: a node just on the saturation curve answers a small step on the flat
        // two-phase branch, while the step's real change may carry it onto the steep single-phase one (a short pipe of
        // saturated steam then pulled in twice its contents, overshot, and the flow reversed every other step)
        double s = 0;
        int nb = Math.max(n.speciesBound(), Math.max(liq == null ? 0 : liq.length, gas == null ? 0 : gas.length));
        double[] per = new double[nb];   // (each species' share of a kg arriving)
        for (int i = 0; i < nb; i++) {
            double l = liq != null && i < liq.length ? liq[i] : 0, g = gas != null && i < gas.length ? gas[i] : 0;
            per[i] = l + g;
        }
        double[] extra = nb > 2 ? new double[nb - 2] : null;
        // (both ways: a node just short of liquid-full answers a gain stiffly and a loss softly, one just past it the
        // other way round; with water's real bulk modulus the step overshot the knee between them every other step)
        for (double frac : new double[]{1e-4, 2e-2, -1e-4, -2e-2}) {
            double d = Math.max(1e-7, Math.abs(frac) * n.mass()) * Math.signum(frac);
            boolean negative = false;
            for (int i = 0; i < nb && !negative; i++) if (n.mass(i) + per[i] * d < 0) negative = true;
            if (negative) continue;
            if (extra != null) for (int i = 2; i < nb; i++) extra[i - 2] = n.mass(i) + per[i] * d;
            probe.solve(n.mWater + per[0] * d, n.mAir + per[1] * d, extra, n.energy + h * d, n.volume, n.T());
            s = Math.max(s, (probe.P - n.P()) / d);
        }
        if (!(s > 0)) s = Water.R_AIR * Math.max(n.T(), Flash.T_MIN) / n.volume;   // (an empty node: as air)
        return s;
    }

    /** How a node's pressure answers a kg of e's packet: its liquid by volume against the node's own liquid, its gas. */
    private static double response(FluidEdge e, double sL, double rhoNodeLiq, double sG) {
        double rp = e.fromA ? (e.pkA != null ? e.pkA.rhoLiq : rhoNodeLiq) : (e.pkB != null ? e.pkB.rhoLiq : rhoNodeLiq);
        if (rp > 0 && rp != rhoNodeLiq) sL *= rhoNodeLiq / rp;
        return e.xl * sL + (1 - e.xl) * sG;
    }

    /** The pressure at e's b end: its opening's, or for an entry edge carrying liquid, the opening's wet mean. */
    static double pressureB(FluidEdge e) {
        if (e.entrySpan > 0 && packet(e, e.a).xl > 0.5) return e.b.pressureOver(e.portB, e.entrySpan);
        return e.b.pressureOver(e.portB, e.spanB);
    }

    /** Which end the flow comes from (by the current pressures and the head), and what it carries. */
    void chooseUpstream(FluidEdge e) {
        double pa = e.a.pressureOver(e.portA, e.spanA), pb = pressureB(e);
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
        Packet pk = packet(e, fromA ? e.a : e.b);
        e.fromA = fromA;
        e.rho = pk.rho;
        e.xl = pk.xl;
        e.head = pk.rhoHead * G * (e.zA() - e.zB()) + e.boost;
        e.g = e.area > 0 ? orificeG(e, e.a.pressureOver(e.portA, e.spanA) - pressureB(e) + e.head) : pk.rho * e.k * e.open / pk.mu;
    }

    /**
     * A metering edge's (a valve's) conductance g = m / dP for a drop dP, by the orifice law m = CD A open sqrt(2 rho
     * dP): both phases by their density (liquid ~14 x steam at 10 bar), linear under DP_LINEAR. The network's other
     * edges stay on their linear lossless law; a valve on it with that law was 2 % of a lossless pipe, still 10 kg/s of
     * water at 5 bar: a feed valve couldn't meter (found 2026-10-29 building the combined cycle).
     */
    static double orificeG(FluidEdge e, double dP) {
        return FluidEdge.CD * e.area * e.open * Math.sqrt(2 * Math.max(e.rho, 1e-6) / Math.max(Math.abs(dP), FluidEdge.DP_LINEAR));
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
            double d = e.a.pressureOver(e.portA, e.spanA) - pressureB(e) + e.head;
            // (a flow against the starting drive is the implicit solve's answer to what the step does at the other
            // end: left alone; only an overshoot past the even point is cut back)
            if (d == 0 || (d > 0) != (e.flow > 0)) continue;
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
                double offA = e.a.pressureOver(e.portA, e.spanA) - e.a.P(), offB = pressureB(e) - e.b.P();
                // the pressure this edge pushes n towards (where its drive would be zero), by the other end's pressure at
                // the end of the step as solved (by its start, a boiler couldn't feed a pipe its valve was emptying)
                double pa = solved.getOrDefault(e.a, e.a.P()), pb = solved.getOrDefault(e.b, e.b.P());
                double towards = e.b == n ? pa + offA - offB + e.head : pb + offB - offA - e.head;
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

    /**
     * What of a packet counts against a node's liquid room, per kg of the flow: its liquid by volume, in kg of water (so
     * water alone counts as before).
     */
    private static double liquidRoom(Packet pk) {
        return pk.rhoLiq == Water.RHO_L || pk.rhoLiq <= 0 ? pk.xl : pk.xl * (Water.RHO_L / pk.rhoLiq);
    }

    /** The liquid level limits (the phase caps and pass-through come after: capPhases). */
    private void capOutflows(double dt) {
        // ... and the liquid level: a node may fill at most half its free space per step (the pressure answers
        // liquid steeply only once it is full; without this a step can overfill it far into its stiff range), and
        // its level may move at most half a port band per step either way (else a port near the level flips between
        // drawing gas and liquid every step, and the water sloshes back and forth for ever). Liquid counts by volume
        // (Argon's review: oil displacing water in a full pipe adds volume), and each layer's own volume moves at most
        // half a band too, so an interface can't jump a side port while the total level holds still.
        // (until nothing more is cut: cutting what a node takes in raises what is left in the node feeding it, which may
        // already have been checked. One pass in node order let a short pipe between a big hot vessel and a full tank
        // take the vessel's water in while the tank cut what the pipe passed on: 0.45 kg into 0.07 L, 1446 bar, a burst)
        for (int pass = 0; pass < 50; pass++) {
        boolean cut = false;
        for (FluidNode n : nodes) {
            if (n.fixed) continue;
            double inL = 0, outL = 0;
            for (FluidEdge e : edges) {
                double f = out(e, n);
                if (f > 0) outL += f * liquidRoom(packet(e, n));
                else if (f < 0) inL -= f * liquidRoom(packet(e, other(e, n)));
            }
            double band = 0.5 * BAND * n.volume * Water.RHO_L / dt;
            double room = 0.5 * Math.max(0, n.state.vGas - Flash.GAS_MIN * n.volume) + 1e-4 * n.volume;
            double limIn = Math.min(room * Water.RHO_L / dt, band);
            if (inL - outL > limIn * (1 + 1e-9) + 1e-12) {
                double sc = (limIn + outL) / inL;
                for (FluidEdge e : edges) {
                    if (out(e, n) >= 0) continue;
                    if (packet(e, other(e, n)).xl > 0) e.flow *= sc;
                }
                cut = true;
            } else if (outL - inL > band * (1 + 1e-9) + 1e-12) {
                double sc = (band + inL) / outL;
                for (FluidEdge e : edges)
                    if (out(e, n) > 0 && packet(e, n).xl > 0) e.flow *= sc;
                cut = true;
            }
            if (capLayers(n, band)) cut = true;
        }
        if (!cut) break;
        }
    }

    /**
     * Each liquid group's own volume in a node moves at most band (kg of water a step, per second) either way. Only
     * where more than one group is about (one group: the total level's limit is the same).
     */
    private boolean capLayers(FluidNode n, double band) {
        int[] reps = SpeciesRegistry.get().groupReps();
        java.util.Map<Integer, double[]> by = null;   // group -> {in, out}
        for (FluidEdge e : edges) {
            double f = out(e, n);
            if (f == 0) continue;
            Packet pk = packet(e, f > 0 ? n : other(e, n));
            if (pk.xl <= 0) continue;
            Flash st = pk.node.state;
            for (int l = 0; l < st.layers; l++) {
                double ls = pk.layerShare[l];
                if (ls == 0) continue;
                if (by == null) by = new java.util.HashMap<>();
                double vol = Math.abs(f) * pk.xl * ls / st.layerRho[l] * Water.RHO_L;
                by.computeIfAbsent(reps[st.layerGroup[l]], g -> new double[2])[f > 0 ? 1 : 0] += vol;
            }
        }
        if (by == null) return false;
        for (int l = 0; l < n.state.layers; l++) by.computeIfAbsent(reps[n.state.layerGroup[l]], g -> new double[2]);
        if (by.size() < 2) return false;
        boolean cut = false;
        for (var en : by.entrySet()) {
            int g = en.getKey();
            double in = en.getValue()[0], out = en.getValue()[1];
            if (in - out > band * (1 + 1e-9) + 1e-12) {
                double sc = (band + out) / in;
                for (FluidEdge e : edges) if (out(e, n) < 0 && carries(packet(e, other(e, n)), g)) e.flow *= sc;
                cut = true;
            } else if (out - in > band * (1 + 1e-9) + 1e-12) {
                double sc = (band + in) / out;
                for (FluidEdge e : edges) if (out(e, n) > 0 && carries(packet(e, n), g)) e.flow *= sc;
                cut = true;
            }
        }
        return cut;
    }

    /** Does the packet carry liquid of group g (by its representative id). */
    private static boolean carries(Packet pk, int g) {
        if (pk.xl <= 0) return false;
        Flash st = pk.node.state;
        int[] reps = SpeciesRegistry.get().groupReps();
        for (int l = 0; l < st.layers; l++) if (pk.layerShare[l] > 0 && reps[st.layerGroup[l]] == g) return true;
        return false;
    }

    /** A node's gas: each species' share (by id) and specific enthalpy (J/kg); all water vapour if it holds no gas. */
    static Mix gasMix(FluidNode n) {
        Mix m = new Mix();
        Flash st = n.state;
        double T = n.T();
        m.share = new double[st.n];
        double mg = 0;
        for (int i = 0; i < st.n; i++) mg += st.vap[i];
        SpeciesRegistry reg = SpeciesRegistry.get();
        if (mg <= 0) {
            m.share[SpeciesRegistry.WATER] = 1;
            m.h = Water.hVapour(T);
            return m;
        }
        for (int i = 0; i < st.n; i++) {
            if (st.vap[i] == 0) continue;
            m.share[i] = st.vap[i] / mg;
            m.h += m.share[i] * reg.byId(i).hVapour(T);
        }
        return m;
    }

    /**
     * A node's own contents as what leaves it reads them (once, before any transfer: transfers change the masses as
     * they go): each liquid layer's enthalpy and species shares, the liquid as a whole, and its gas.
     */
    static final class Own {
        final double[] layerH;
        final double[][] layerComp;
        final double liquidH, gasH;
        final double[] liquidComp, gasComp;

        Own(FluidNode n) {
            Flash st = n.state;
            double T = n.T();
            SpeciesRegistry reg = SpeciesRegistry.get();
            int L = st.layers;
            layerH = new double[Math.max(L, 1)];
            layerComp = new double[Math.max(L, 1)][st.n];
            for (int l = 0; l < L; l++) {
                for (int i = 0; i < st.n; i++) {
                    if (st.liq[i] <= 0 || !st.inLayer(i, l)) continue;
                    layerComp[l][i] = st.liq[i] / st.layerMass[l];
                    layerH[l] += layerComp[l][i] * reg.byId(i).hLiquid(T);
                }
            }
            if (L <= 1) {
                // (one liquid, or none: water's, as the liquid probe)
                liquidComp = L == 1 ? layerComp[0] : new double[st.n];
                if (L == 0) liquidComp[SpeciesRegistry.WATER] = 1;
                liquidH = L == 1 ? layerH[0] : Water.hLiquid(T);
            } else {
                Mix lm = liquidMix(n);
                liquidComp = lm.share;
                liquidH = lm.h;
            }
            Mix gm = gasMix(n);
            gasComp = gm.share;
            gasH = gm.h;
        }
    }

    /** What passes through a node this step: its liquid's enthalpy and species shares, its gas's. */
    static final class Pass {
        double liquidH, gasH;
        double[] liquidComp, gasComp;
    }

    /**
     * Per free node: theta[i] = {liquid layers from the bottom..., gas}, the share of what leaves it from that layer (or
     * its gas) that comes out of what it holds (the rest is what flowed in, passed through). A node gives at most
     * DRAIN_CAP of each layer and of its gas per step; what leaves its layers beyond that is passed through, from what
     * liquid flows in (shared among the layers by their excess: else two layers could each pass on all of it, Argon's
     * review). Outflows beyond that are cut (each edge by its tightest layer or phase). Cutting one node's outflows
     * lowers its neighbours' inflows, so a few passes.
     */
    private double[][] capPhases(double dt, List<FluidNode> free, java.util.Map<FluidNode, Integer> idx, double[] xl, Packet[] pks) {
        int N = free.size(), E = edges.size();
        double[][] haveL = new double[N][];
        double[] haveG = new double[N];
        for (int i = 0; i < N; i++) {
            FluidNode n = free.get(i);
            Flash st = n.state;
            haveL[i] = new double[Math.max(st.layers, 1)];
            for (int l = 0; l < st.layers; l++) haveL[i][l] = Math.max(0, st.layerMass[l]);
            double g = 0;
            for (int s = 0; s < st.n; s++) g += st.vap[s];
            haveG[i] = Math.max(0, g);
        }
        double[][] outL = new double[N][];
        for (int i = 0; i < N; i++) outL[i] = new double[haveL[i].length];
        double[] outG = new double[N], inL = new double[N], inG = new double[N];
        double[][] fL = new double[N][];
        for (int i = 0; i < N; i++) fL[i] = new double[haveL[i].length];
        for (int pass = 0; pass < 200; pass++) {   // (usually 1 or 2; the last pass finds nothing to cut, so the sums are current)
            for (int i = 0; i < N; i++) java.util.Arrays.fill(outL[i], 0);
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
                    double[] ls = pks[k].layerShare;
                    for (int l = 0; l < outL[iu].length && l < ls.length; l++) if (ls[l] != 0) outL[iu][l] += m * xl[k] * ls[l];
                    outG[iu] += m * (1 - xl[k]);
                }
                if (id != null) {
                    inL[id] += m * xl[k];
                    inG[id] += m * (1 - xl[k]);
                }
            }
            boolean cut = false;
            for (int i = 0; i < N; i++) {
                double capG = DRAIN_CAP * haveG[i] / dt + inG[i];
                double fG = outG[i] > capG * (1 + 1e-9) ? capG / outG[i] : 1;
                int L = haveL[i].length;
                boolean all = fG >= 1;
                if (L == 1) {
                    double capL = DRAIN_CAP * haveL[i][0] / dt + inL[i];
                    fL[i][0] = outL[i][0] > capL * (1 + 1e-9) ? capL / outL[i][0] : 1;
                    all &= fL[i][0] >= 1;
                } else {
                    double excess = 0;
                    for (int l = 0; l < L; l++) excess += Math.max(0, outL[i][l] - DRAIN_CAP * haveL[i][l] / dt);
                    for (int l = 0; l < L; l++) {
                        double own = DRAIN_CAP * haveL[i][l] / dt, ex = Math.max(0, outL[i][l] - own);
                        double allowed = own + (excess > 0 ? Math.min(1, inL[i] / excess) * ex : 0);
                        fL[i][l] = outL[i][l] > allowed * (1 + 1e-9) ? allowed / outL[i][l] : 1;
                        all &= fL[i][l] >= 1;
                    }
                }
                if (all) continue;
                cut = true;
                FluidNode n = free.get(i);
                for (int k = 0; k < E; k++) {
                    FluidEdge e = edges.get(k);
                    if (out(e, n) <= 0) continue;
                    double f = 1;
                    if (xl[k] > 0) {
                        double[] ls = pks[k].layerShare;
                        for (int l = 0; l < L && l < ls.length; l++) if (ls[l] != 0) f = Math.min(f, fL[i][l]);
                    }
                    if (xl[k] < 1) f = Math.min(f, fG);
                    e.flow *= f;
                }
            }
            if (!cut) break;
        }
        double[][] theta = new double[N][];
        for (int i = 0; i < N; i++) {
            int L = haveL[i].length;
            theta[i] = new double[L + 1];
            for (int l = 0; l < L; l++)
                theta[i][l] = outL[i][l] > 0 && outL[i][l] * dt > DRAIN_CAP * haveL[i][l] ? DRAIN_CAP * haveL[i][l] / (outL[i][l] * dt) : 1;
            theta[i][L] = outG[i] > 0 && outG[i] * dt > DRAIN_CAP * haveG[i] ? DRAIN_CAP * haveG[i] / (outG[i] * dt) : 1;
        }
        return theta;
    }

    /**
     * What passes through each free node this step, as an implicit upwind solve: per phase, the flow-weighted mix of
     * what flows in (enthalpy and each species' share), where what flows in from a node is its own contents' share
     * (theta, per layer) plus its own pass-through for the rest. A node nothing flows into passes its own contents.
     */
    private Pass[] passThrough(List<FluidNode> free, java.util.Map<FluidNode, Integer> idx, double[] xl, Packet[] pks,
                               double[][] theta, Own[] own) {
        int N = free.size(), E = edges.size();
        int nb = 2;
        for (FluidNode n : nodes) nb = Math.max(nb, n.state.n);
        Pass[] pass = new Pass[N];
        for (int i = 0; i < N; i++) pass[i] = new Pass();
        // columns: 0 the enthalpy, 1 + s the share of species s; (sum in) X_i - sum_in q (1 - theta_u) X_u = sum_in q theta_u own_u
        for (int phase = 0; phase < 2; phase++) {
            double[][] A = new double[N][N];
            double[][] b = new double[1 + nb][N];
            boolean[] used = new boolean[1 + nb];
            used[0] = true;
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
                Own ou = iu == null ? new Own(u) : own[iu];
                if (phase == 1) {
                    double th = iu == null ? 1 : theta[iu][theta[iu].length - 1];
                    if (iu != null && th < 1) A[id][iu] -= q * (1 - th);
                    b[0][id] += q * th * ou.gasH;
                    for (int s = 0; s < ou.gasComp.length; s++) if (ou.gasComp[s] != 0) {
                        b[1 + s][id] += q * th * ou.gasComp[s];
                        used[1 + s] = true;
                    }
                } else {
                    double[] ls = pks[k].layerShare;
                    int L = u.state.layers;
                    if (L <= 1) {
                        // (one layer, or a fixed node's: as one liquid)
                        double th = iu == null ? 1 : theta[iu][0];
                        if (iu != null && th < 1) A[id][iu] -= q * (1 - th);
                        b[0][id] += q * th * ou.liquidH;
                        for (int s = 0; s < ou.liquidComp.length; s++) if (ou.liquidComp[s] != 0) {
                            b[1 + s][id] += q * th * ou.liquidComp[s];
                            used[1 + s] = true;
                        }
                    } else for (int l = 0; l < L; l++) {
                        if (ls[l] == 0) continue;
                        double ql = q * ls[l];
                        double th = iu == null ? 1 : theta[iu][l];
                        if (iu != null && th < 1) A[id][iu] -= ql * (1 - th);
                        b[0][id] += ql * th * ou.layerH[l];
                        for (int s = 0; s < ou.layerComp[l].length; s++) if (ou.layerComp[l][s] != 0) {
                            b[1 + s][id] += ql * th * ou.layerComp[l][s];
                            used[1 + s] = true;
                        }
                    }
                }
            }
            for (int i = 0; i < N; i++) {
                double[] oc = phase == 0 ? own[i].liquidComp : own[i].gasComp;
                for (int s = 0; s < oc.length; s++) if (oc[s] != 0) used[1 + s] = true;
            }
            for (int i = 0; i < N; i++) {
                if (inSum[i] > 0) A[i][i] += inSum[i];
                else {   // (nothing flows in: it passes its own)
                    java.util.Arrays.fill(A[i], 0);
                    A[i][i] = 1;
                    double[] oc = phase == 0 ? own[i].liquidComp : own[i].gasComp;
                    b[0][i] = phase == 0 ? own[i].liquidH : own[i].gasH;
                    for (int s = 0; s < nb; s++) b[1 + s][i] = s < oc.length ? oc[s] : 0;
                }
            }
            for (int i = 0; i < N; i++) {
                if (phase == 0) {
                    pass[i].liquidComp = new double[nb];
                } else pass[i].gasComp = new double[nb];
            }
            for (int c = 0; c < b.length; c++) {
                if (!used[c]) continue;
                double[][] Ac = new double[N][];
                for (int i = 0; i < N; i++) Ac[i] = A[i].clone();
                double[] x = solve(Ac, b[c]);
                for (int i = 0; i < N; i++) {
                    if (c == 0) {
                        if (phase == 0) pass[i].liquidH = x[i];
                        else pass[i].gasH = x[i];
                    } else (phase == 0 ? pass[i].liquidComp : pass[i].gasComp)[c - 1] = x[i];
                }
            }
        }
        return pass;
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
     * Move one edge's flow: its liquid share (from each layer the port draws: the layer's own contents for theta, what
     * passes through for the rest), its gas share at the gas mix and enthalpy (the same way), species by species.
     */
    private static void transfer(FluidEdge e, double dt, double xl, Packet pk, java.util.Map<FluidNode, Integer> idx,
                                 double[][] theta, Pass[] pass, Own[] own) {
        e.fLiquid = e.fVapour = e.fAir = e.fEnthalpy = 0;
        e.power = 0;
        if (e.fSpecies != null) java.util.Arrays.fill(e.fSpecies, 0);
        if (e.flow == 0) return;
        FluidNode u = e.flow > 0 ? e.a : e.b, d = e.flow > 0 ? e.b : e.a;
        Integer iu = idx.get(u);
        Own ou = iu == null ? new Own(u) : own[iu];
        Pass through = iu == null ? null : pass[iu];
        double m = Math.abs(e.flow);
        double liq = m * xl, gas = m * (1 - xl);
        int nb = Math.max(u.state.n, Math.max(through == null ? 0 : through.liquidComp.length, 2));
        // the liquid: by layer, own share theta, the rest passed through
        double[] cL = new double[nb];
        double hL;
        int L = u.state.layers;
        if (L <= 1) {
            double thL = iu == null ? 1 : theta[iu][0];
            hL = thL * ou.liquidH + (1 - thL) * (through == null ? ou.liquidH : through.liquidH);
            for (int s = 0; s < nb; s++) {
                double o = s < ou.liquidComp.length ? ou.liquidComp[s] : 0, t = through == null ? o : through.liquidComp[s];
                cL[s] = thL * o + (1 - thL) * t;
            }
        } else {
            hL = 0;
            for (int l = 0; l < L; l++) {
                double ls = pk.layerShare[l];
                if (ls == 0) continue;
                double th = iu == null ? 1 : theta[iu][l];
                hL += ls * (th * ou.layerH[l] + (1 - th) * (through == null ? ou.layerH[l] : through.liquidH));
                for (int s = 0; s < nb; s++) {
                    double o = s < ou.layerComp[l].length ? ou.layerComp[l][s] : 0, t = through == null ? o : through.liquidComp[s];
                    cL[s] += ls * (th * o + (1 - th) * t);
                }
            }
        }
        double cs = 0;
        for (int s = 0; s < nb; s++) cs += cL[s];
        if (cs > 0) for (int s = 0; s < nb; s++) cL[s] /= cs;
        // the gas
        double thG = iu == null ? 1 : theta[iu][theta[iu].length - 1];
        double[] y = new double[nb];
        for (int s = 0; s < nb; s++) {
            double o = s < ou.gasComp.length ? ou.gasComp[s] : 0, t = through == null ? o : through.gasComp[s];
            y[s] = thG * o + (1 - thG) * t;
        }
        double hG = thG * ou.gasH + (1 - thG) * (through == null ? ou.gasH : through.gasH);
        double ys = 0;
        for (int s = 0; s < nb; s++) ys += y[s];
        if (ys > 0) for (int s = 0; s < nb; s++) y[s] /= ys;
        double h = liq * hL + gas * hG;
        double toD = h;
        double vap = gas * y[SpeciesRegistry.WATER];
        if (e.kind == FluidEdge.Kind.TURBINE) {
            e.power = Math.max(0, FluidEdge.TURBINE_EFFICIENCY * vap * (Water.hVapour(u.T()) - Water.hLiquid(Water.TB)));
            toD -= e.power;
        }
        double sign = e.flow > 0 ? 1 : -1;
        if (nb > 2 && (e.fSpecies == null || e.fSpecies.length < nb)) e.fSpecies = new double[nb];
        for (int s = 0; s < nb; s++) {
            double ms = (cL[s] != 0 ? liq * cL[s] : 0) + (y[s] != 0 ? gas * y[s] : 0);
            if (ms == 0) continue;
            if (s == SpeciesRegistry.WATER) ms = liq * cL[s] + vap;   // (as water was always moved: liquid + vapour)
            if (!u.fixed) u.addMass(s, -ms * dt);
            if (!d.fixed) d.addMass(s, ms * dt);
            if (e.fSpecies != null) e.fSpecies[s] = sign * ms;
        }
        if (!u.fixed) u.energy -= h * dt;
        if (!d.fixed) d.energy += toD * dt;
        e.fLiquid = sign * liq * cL[SpeciesRegistry.WATER];
        e.fVapour = sign * vap;
        e.fAir = sign * gas * y[SpeciesRegistry.AIR];
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
