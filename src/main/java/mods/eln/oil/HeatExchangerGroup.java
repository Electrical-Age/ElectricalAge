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
 * Heat exchanger blocks stacked into one counterflow exchanger (Baughn 2026-10-29): a 2 x 2 cross-section of blocks run
 * N long (1..MAX_LENGTH) along their fronts' axis is one exchanger with N x 4 blocks' UA and side volumes, its ports on
 * its two end faces. As with tank boxes (TankGroup) any other shape stays as one-block exchangers, each a quarter slice;
 * each block keeps its share of both sides by volume, written back before the stack changes and before saving.
 */
public final class HeatExchangerGroup {
    public static final int MAX_LENGTH = 8;
    public final List<HeatExchangerElement> members;
    public final FluidNode a, b;
    /** The box: its lowest corner (block coordinates) and size (blocks); its length runs along X if alongX, else Z. */
    public final int x0, y0, z0, sx, sy, sz;
    public final boolean alongX;

    HeatExchangerGroup(List<HeatExchangerElement> members, int x0, int y0, int z0, int sx, int sy, int sz, boolean alongX) {
        this.members = members;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.alongX = alongX;
        a = merged(true);
        b = merged(false);
    }

    private FluidNode merged(boolean sideA) {
        double volume = 0, w = 0, air = 0, u = 0;
        for (HeatExchangerElement m : members) {
            FluidNode o = sideA ? m.ownA : m.ownB;
            volume += o.volume;
            w += o.mWater;
            air += o.mAir;
            u += o.energy;
        }
        FluidNode n = new FluidNode(volume, y0, sy, false);
        n.mWater = w;
        n.mAir = air;
        n.energy = u;
        n.update();
        return n;
    }

    /** Its length (blocks along its axis). */
    public int length() {
        return alongX ? sx : sz;
    }

    /** The block at its lowest corner: it steps the exchanger and draws it. */
    public HeatExchangerElement master() {
        for (HeatExchangerElement m : members) {
            var c = m.node.coordonate;
            if (c.x == x0 && c.y == y0 && c.z == z0) return m;
        }
        return members.get(0);
    }

    /** Write both sides back into the blocks' shares, by volume. */
    void writeBack() {
        for (HeatExchangerElement m : members) {
            share(a, m.ownA);
            share(b, m.ownB);
        }
    }

    private static void share(FluidNode whole, FluidNode own) {
        double f = own.volume / whole.volume;
        own.mWater = whole.mWater * f;
        own.mAir = whole.mAir * f;
        own.energy = whole.energy * f;
        own.update();
    }

    static final Direction[] DIRS = {Direction.XN, Direction.XP, Direction.YN, Direction.YP, Direction.ZN, Direction.ZP};

    static boolean alongX(HeatExchangerElement h) {
        return h.front == Direction.XN || h.front == Direction.XP;
    }

    /** The exchanger of the same kind in that direction, if any (and ready). */
    static HeatExchangerElement neighbour(HeatExchangerElement h, Direction d, HeatExchangerElement excluded) {
        if (h.node.getNeighbor(d) instanceof TransparentNode n && n.element instanceof HeatExchangerElement o && o != excluded
            && o.desc == h.desc && o.ownA != null && !n.isDestructing()) return o;
        return null;
    }

    /** Re-form the stacks of the exchangers connected to these (`excluded`: the block going away, or null). */
    public static void regroup(Collection<HeatExchangerElement> seeds, HeatExchangerElement excluded) {
        Set<HeatExchangerElement> done = new HashSet<>();
        for (HeatExchangerElement seed : seeds) {
            if (seed == null || seed == excluded || seed.ownA == null || done.contains(seed)) continue;
            Set<HeatExchangerElement> comp = new LinkedHashSet<>();
            ArrayDeque<HeatExchangerElement> queue = new ArrayDeque<>();
            queue.add(seed);
            comp.add(seed);
            while (!queue.isEmpty()) {
                HeatExchangerElement h = queue.poll();
                for (Direction d : DIRS) {
                    HeatExchangerElement o = neighbour(h, d, excluded);
                    if (o != null && comp.add(o)) queue.add(o);
                }
            }
            done.addAll(comp);
            Set<HeatExchangerGroup> old = new HashSet<>();
            for (HeatExchangerElement m : comp) if (m.group != null) old.add(m.group);
            for (HeatExchangerGroup g : old) g.writeBack();
            int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE;
            boolean sameAxis = true, ax = alongX(seed);
            for (HeatExchangerElement m : comp) {
                var c = m.node.coordonate;
                x0 = Math.min(x0, c.x);
                y0 = Math.min(y0, c.y);
                z0 = Math.min(z0, c.z);
                x1 = Math.max(x1, c.x);
                y1 = Math.max(y1, c.y);
                z1 = Math.max(z1, c.z);
                sameAxis &= alongX(m) == ax;
            }
            int sx = x1 - x0 + 1, sy = y1 - y0 + 1, sz = z1 - z0 + 1, across = ax ? sz : sx, len = ax ? sx : sz;
            // a stack: exactly a box, 2 x 2 across its fronts' axis, 1..MAX_LENGTH long
            boolean stack = sameAxis && comp.size() == sx * sy * sz && sy == 2 && across == 2 && len <= MAX_LENGTH;
            HeatExchangerGroup g = stack ? new HeatExchangerGroup(new ArrayList<>(comp), x0, y0, z0, sx, sy, sz, ax) : null;
            for (HeatExchangerElement m : comp) {
                m.group = g;
                m.a = g == null ? m.ownA : g.a;
                m.b = g == null ? m.ownB : g.b;
                m.makePorts();
                FluidWorld.changed(m);
                m.needPublish();
            }
        }
    }
}
