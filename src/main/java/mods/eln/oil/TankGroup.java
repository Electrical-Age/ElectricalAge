package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.fluid.FluidNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tank blocks of one tier that fill a box (A x B x C, any size) are one vessel (Baughn 2026-10-29): one FluidNode with
 * their volumes summed, its ports on the box's outer faces. The rule: a connected set of tank blocks merges only if it
 * is exactly a box; any other shape stays as separate one-block tanks (simple and sound: a box has one height, one top
 * and one bottom). Each block keeps its share of the contents by volume (TankElement.own): the vessel is formed from
 * the shares, and written back into them before it changes (a block broken, placed or unloaded) and before saving, so a
 * split shares the contents by volume and saved state survives a reload.
 */
public final class TankGroup {
    public final List<TankElement> members;
    public final FluidNode node;
    /** The box: its lowest corner (block coordinates) and size (blocks). */
    public final int x0, y0, z0, sx, sy, sz;

    TankGroup(List<TankElement> members, int x0, int y0, int z0, int sx, int sy, int sz) {
        this.members = members;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        double volume = 0, loss = 0;
        for (TankElement m : members) {
            volume += m.own.volume;
            loss += m.own.lossWPerK;
        }
        node = new FluidNode(volume, y0, sy, false);
        for (TankElement m : members) node.addShareOf(m.own, 1);   // (every species: the blocks' contents summed)
        written = stamp();
        node.lossWPerK = loss;
        node.update();
    }

    /** The block at its lowest corner: it draws the whole tank. */
    public TankElement master() {
        for (TankElement m : members) {
            var c = m.node.coordonate;
            if (c.x == x0 && c.y == y0 && c.z == z0) return m;
        }
        return members.get(0);
    }

    /**
     * Write the vessel's contents back into its blocks' shares: the liquid by layer, from the bottom up to the vessel's
     * level, the gas over it (Baughn 2026-11-09: a box split by a broken corner shared its oil by volume, the upper blocks
     * as full as the lower, and one-block tanks can't pass liquid down and gas up through one coupling, so it stayed up
     * there). Each block at the vessel's temperature, scaled to its energy exactly, so a save and reload (which sums the
     * shares) changes nothing.
     */
    static final double FULL = 0.98;

    /** The vessel's contents when its blocks' shares last matched it (stamp()), so an unchanged vessel isn't written again:
     *  the shares come from float-rounded saves, and a fresh layered split of their sum differs in the last bits. */
    private double[] written;

    private double[] stamp() {
        double[] st = new double[3 + (node.extra == null ? 0 : node.extra.length)];
        st[0] = node.mWater;
        st[1] = node.mAir;
        st[2] = node.energy;
        for (int i = 3; i < st.length; i++) st[i] = node.extra[i - 3];
        return st;
    }

    void writeBack() {
        if (java.util.Arrays.equals(written, stamp())) return;
        List<FluidNode> owns = new ArrayList<>(members.size());
        int[] rows = new int[members.size()];
        for (int i = 0; i < members.size(); i++) {
            owns.add(members.get(i).own);
            rows[i] = members.get(i).node.coordonate.y - y0;
        }
        share(node, sy, owns, rows);
        written = stamp();
    }

    /**
     * writeBack's split: node's contents into the blocks' shares owns (rows[i]: own i's layer, 0 the bottom, of sy).
     * <p>
     * Over-full (more liquid at its nominal density than FULL of every layer holds, as in a box pumped full, its liquid
     * compressed against the walls) every block takes its volume's share of everything, energy too: the layered split
     * took the liquid share as vL / V, over 1 once vL passed V, and the gas share (1 - that) / (V - vL, floored at 1e-12)
     * came out hugely negative: negative air, the shares' energy NaN, and the box reloaded from them fell 1 K per update
     * to 120 K (Incognito 2026-11-25).
     * <p>
     * Else the liquid fills the rows from the bottom, FULL of each at most, its layers in order (the densest lowest: oil
     * over water stays over it when the box splits), and the gas fills what is left. Each block is at the vessel's
     * temperature, and what that misses of the vessel's energy is shared by heat capacity, added, never scaled: liquid
     * under 0 C has negative energy and vapour positive, and a scale by node.energy / (their sum near 0) sent shares to
     * 4000 K and 120 K.
     */
    public static void share(FluidNode node, int sy, List<FluidNode> owns, int[] rows) {
        node.update();
        double V = node.volume, vL = node.liquidVolume(), level = vL / (FULL * V) * sy;
        int n = node.speciesBound();
        if (level > sy) {
            for (FluidNode own : owns) {
                own.setShareOf(node, own.volume / V);
                own.update();
            }
            return;
        }
        double vG = Math.max(1e-12, V - vL), rowV = V / sy;
        // the rows' spans of the liquid column (liquid volume from the bottom), and its layers'
        double[] rowLo = new double[sy], rowHi = new double[sy];
        double cum = 0;
        for (int j = 0; j < sy; j++) {
            rowLo[j] = cum;
            cum += FULL * Math.max(0, Math.min(1, level - j)) * rowV;
            rowHi[j] = cum;
        }
        int layers = node.layers();
        double top = layers > 0 ? node.layerTop(layers - 1) : 0;
        double[] layLo = new double[layers], layHi = new double[layers];
        for (int l = 0; l < layers; l++) {
            layLo[l] = l == 0 ? 0 : layHi[l - 1];
            layHi[l] = top > 0 ? node.layerTop(l) / top * vL : 0;
        }
        double T = node.T(), sum = 0, capSum = 0;
        double[] cap = new double[owns.size()];
        for (int i = 0; i < owns.size(); i++) {
            FluidNode own = owns.get(i);
            int j = Math.max(0, Math.min(sy - 1, rows[i]));
            double lf = FULL * Math.max(0, Math.min(1, level - j)), inRow = own.volume / rowV;
            double sG = Math.max(0, (1 - lf) * own.volume / vG);
            double[] ex = n > 2 ? new double[n - 2] : null;
            for (int id = 0; id < n; id++) {
                if (id == mods.eln.sim.fluid.SpeciesRegistry.AIR) continue;
                int l = node.layerOf(id);
                double sL = 0;
                if (l >= 0 && layHi[l] > layLo[l])
                    sL = Math.max(0, Math.min(rowHi[j], layHi[l]) - Math.max(rowLo[j], layLo[l])) / (layHi[l] - layLo[l]) * inRow;
                double m = Math.max(0, node.liquid(id) * sL + node.vapour(id) * sG);
                if (id == mods.eln.sim.fluid.SpeciesRegistry.WATER) own.mWater = m;
                else ex[id - 2] = m;
            }
            own.mAir = Math.max(0, node.mAir * sG);
            own.extra = ex;
            own.energy = own.energyAt(T);
            cap[i] = Math.max(0, own.energyAt(T + 1) - own.energy);
            sum += own.energy;
            capSum += cap[i];
        }
        // (what the shares at T miss of the vessel's energy, by heat capacity; by volume if they have none)
        double rest = node.energy - sum;
        for (int i = 0; i < owns.size(); i++) {
            FluidNode own = owns.get(i);
            own.energy += rest * (capSum > 0 ? cap[i] / capSum : own.volume / V);
            own.update();
        }
    }

    static final Direction[] DIRS = {Direction.XN, Direction.XP, Direction.YN, Direction.YP, Direction.ZN, Direction.ZP};

    /** The tank of the same tier in that direction, if any (and ready). */
    static TankElement neighbour(TankElement t, Direction d, TankElement excluded) {
        if (t.node.getNeighbor(d) instanceof TransparentNode n && n.element instanceof TankElement o && o != excluded
            && o.desc == t.desc && o.own != null && !n.isDestructing()) return o;
        return null;
    }

    /** Whether the tanks connected to SEED fill a box of more than one (what regroup would make one vessel of). */
    static boolean wouldBox(TankElement seed) {
        Set<TankElement> comp = new HashSet<>();
        ArrayDeque<TankElement> queue = new ArrayDeque<>();
        queue.add(seed);
        comp.add(seed);
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
        while (!queue.isEmpty()) {
            TankElement t = queue.poll();
            var c = t.node.coordonate;
            x0 = Math.min(x0, c.x);
            y0 = Math.min(y0, c.y);
            z0 = Math.min(z0, c.z);
            x1 = Math.max(x1, c.x);
            y1 = Math.max(y1, c.y);
            z1 = Math.max(z1, c.z);
            for (Direction d : DIRS) {
                TankElement o = neighbour(t, d, null);
                if (o != null && comp.add(o)) queue.add(o);
            }
        }
        return comp.size() > 1 && comp.size() == (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
    }

    /**
     * Re-form the vessels of the tanks connected to these (a block placed, broken or unloaded: `excluded` is the one
     * going away, or null).
     */
    public static void regroup(Collection<TankElement> seeds, TankElement excluded) {
        Set<TankElement> done = new HashSet<>();
        for (TankElement seed : seeds) {
            if (seed == null || seed == excluded || seed.own == null || done.contains(seed)) continue;
            // the connected set
            Set<TankElement> comp = new LinkedHashSet<>();
            ArrayDeque<TankElement> queue = new ArrayDeque<>();
            queue.add(seed);
            comp.add(seed);
            while (!queue.isEmpty()) {
                TankElement t = queue.poll();
                for (Direction d : DIRS) {
                    TankElement o = neighbour(t, d, excluded);
                    if (o != null && comp.add(o)) queue.add(o);
                }
            }
            done.addAll(comp);
            // their contents back into their shares (and the going block's share out of the vessel)
            Set<TankGroup> old = new HashSet<>();
            for (TankElement m : comp) if (m.group != null) old.add(m.group);
            for (TankGroup g : old) g.writeBack();
            // a box?
            int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
            for (TankElement m : comp) {
                var c = m.node.coordonate;
                x0 = Math.min(x0, c.x);
                y0 = Math.min(y0, c.y);
                z0 = Math.min(z0, c.z);
                x1 = Math.max(x1, c.x);
                y1 = Math.max(y1, c.y);
                z1 = Math.max(z1, c.z);
            }
            int sx = x1 - x0 + 1, sy = y1 - y0 + 1, sz = z1 - z0 + 1;
            TankGroup g = comp.size() > 1 && comp.size() == sx * sy * sz ? new TankGroup(new ArrayList<>(comp), x0, y0, z0, sx, sy, sz) : null;
            for (TankElement m : comp) {
                m.group = g;
                m.vessel = g == null ? m.own : g.node;
                m.remakePorts();
                m.showWater();
                FluidWorld.changed(m);
                m.needPublish();
            }
        }
    }
}
