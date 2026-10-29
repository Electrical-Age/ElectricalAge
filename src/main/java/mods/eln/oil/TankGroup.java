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
        double volume = 0, w = 0, a = 0, u = 0, loss = 0;
        for (TankElement m : members) {
            volume += m.own.volume;
            w += m.own.mWater;
            a += m.own.mAir;
            u += m.own.energy;
            loss += m.own.lossWPerK;
        }
        node = new FluidNode(volume, y0, sy, false);
        node.mWater = w;
        node.mAir = a;
        node.energy = u;
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

    /** Write the vessel's contents back into its blocks' shares, by volume. */
    void writeBack() {
        for (TankElement m : members) {
            double share = m.own.volume / node.volume;
            m.own.mWater = node.mWater * share;
            m.own.mAir = node.mAir * share;
            m.own.energy = node.energy * share;
            m.own.update();
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
                m.makePorts();
                FluidWorld.changed(m);
                m.needPublish();
            }
        }
    }
}
