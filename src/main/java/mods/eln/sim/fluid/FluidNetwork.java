package mods.eln.sim.fluid;

import java.util.ArrayList;
import java.util.List;

/**
 * One connected fluid network: its nodes and edges as the world joined them. The fluid core (fluidcore/, board #61)
 * steps it (CoreStep); this class keeps the network's shape and the port packets the adapter takes each stream's
 * conductance from (what a port draws: liquid share, density, viscosity) until mobility moves into the core.
 */
public final class FluidNetwork {
    public static final double G = 9.81;

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

    /** The fluid core's mirror of this network. */
    CoreStep core;

    public void step(double dt) {
        CoreStep.of(this).step(this, dt);
    }

    /**
     * The longest stretch (s) this network went without progress since it was built: ticks in a row whose step covered
     * under half the tick (Baughn 2026-11-10: over a quarter of a second is not workable, whatever the devices do).
     */
    public double longestStall() {
        return core == null ? 0 : core.maxStall;
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
}
