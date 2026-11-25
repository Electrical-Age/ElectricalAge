package mods.eln.gametest.cases;

import mods.eln.gametest.Reload;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Breaking a piece of a pressurised run deletes what was in it (Baughn 2026-11-25): the rest keep their shares at the
 * same pressure and temperature, and the contents drop by exactly the pieces' shares. Before, a junction left with one
 * arm gave that arm its contents: a run of water went from 10 bar to 4600 and burst.
 */
public final class PipeBreakCases {
    private PipeBreakCases() {
    }

    public static void addTo(List<TestCase> cases) {
        for (String fluid : new String[]{"water", "steam"})
            for (How how : new How[]{How.PLAYER, How.NOBODY, How.SHEARS, How.END_JOIN, How.TEE}) cases.add(new Break(fluid, how));
        cases.add(new Break("water", How.BURST));
    }

    /**
     * How the piece goes: a player mining it, the block destroyed with nobody (an explosion's path), the toolbag's
     * shears, one arm of a two-armed junction (a run extended from a pipe's end inside a block), a tee's riser and then
     * one level arm, or the burst piece of a run that burst and vented.
     */
    enum How {PLAYER, NOBODY, SHEARS, END_JOIN, TEE, BURST}

    /** Pieces in a straight run along x, one block each. */
    static final int N = 5;
    /** When the piece is broken (ticks after build), and the later looks. */
    static final int AT = 30, LATER = 20;

    /** A part's fluid node (a pipe's or a junction's), or null. */
    static FluidNode fluidOf(VoxelPart q) {
        return q instanceof PipePart pp ? pp.fluid : q instanceof JunctionPart j ? j.fluid : null;
    }

    static final class Break implements TestCase {
        final String fluid;
        final How how;
        final BlockPos[] at = new BlockPos[N + 1];   // (the last: the block over the middle, for the tee's riser)
        /** Before the break, per part: its share of its run {water, air, energy, mass} and its run's {P, T}. */
        final Map<VoxelPart, double[]> share = new IdentityHashMap<>(), state = new IdentityHashMap<>();
        String err = "";
        double[] want, got, gotLater, gotReloaded;
        int survivors, gone;
        double worstP, worstT, worstPLater, worstPReloaded;
        boolean burstAfter, burstBefore;

        Break(String fluid, How how) {
            this.fluid = fluid;
            this.how = how;
        }

        public String id() {
            return "pipe_break_deletes_" + fluid + "_" + how.name().toLowerCase(java.util.Locale.ROOT);
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return AT + LATER + 5;
        }

        public void build(TestContext c) {
            for (int i = 0; i < N; i++) {
                at[i] = c.at(1 + i, 1, 1);
                PipeCases.lay(c, at[i], 0, 0, 8, 8, 16, 8, 8);
            }
            at[N] = at[2].above();
            if (how == How.END_JOIN) {
                // the middle piece in two: a pipe to the block's centre, a run extended from its end (a two-armed junction)
                VoxelNode m = c.voxelByEntity(at[2]);
                m.removePart(0, false);
                PipeCases.lay(c, at[2], 0, 0, 8, 8, 8, 8, 8);
                note(c.voxelByEntity(at[2]).addJoined(new PipePart(0, new int[]{8, 8, 8, 16, 8, 8}), 8, 8, 8));
            }
            if (how == How.TEE) {
                note(c.voxelByEntity(at[2]).addJoined(new PipePart(0, new int[]{8, 8, 8, 8, 16, 8}), 8, 8, 8));
                PipeCases.lay(c, at[N], 0, 8, 0, 8, 8, 12, 8);
            }
            for (BlockPos p : at) {
                VoxelNode n = c.voxelByEntity(p);
                if (n == null) continue;
                for (VoxelPart q : n.parts) {
                    FluidNode f = fluidOf(q);
                    if (f == null) continue;
                    if (how == How.BURST) f.fill(0.5 * f.volume * Water.RHO_L, 50e5, 293.15);   // (past iron's 25 bar)
                    else if (fluid.equals("water")) f.fill(0.97 * f.volume * Water.RHO_L, 10e5, 293.15);
                }
                mods.eln.fluid.FluidWorld.changed(n);
            }
            if (fluid.equals("steam")) PipeCases.steam(c, at[0], at[1], at[2], at[3], at[4]);
            if (fluid.equals("steam") && how == How.TEE) PipeCases.steam(c, at[N]);
            c.after(AT, () -> {
                snapshot(c);
                burstBefore = anyBurst(c);
                try {
                    doBreak(c);
                } catch (RuntimeException e) {
                    err += e + "; ";
                }
                compare(c, 0);
            });
            c.after(AT + LATER, () -> {
                compare(c, 1);
                // the save round trip: every block left reloaded, the runs as they were
                try {
                    for (BlockPos p : at) if (c.voxelAt(p) != null) Reload.node(c.level(), p);
                } catch (ReflectiveOperationException e) {
                    err += e + "; ";
                }
                compare(c, 2);
            });
        }

        void note(String e) {
            if (e != null) err += e + "; ";
        }

        List<VoxelPart> parts(TestContext c) {
            List<VoxelPart> out = new ArrayList<>();
            for (BlockPos p : at) {
                VoxelNode n = c.voxelAt(p);
                if (n != null) for (VoxelPart q : n.parts) if (fluidOf(q) != null) out.add(q);
            }
            return out;
        }

        boolean anyBurst(TestContext c) {
            for (VoxelPart q : parts(c)) if (q instanceof PipePart pp && pp.burst) return true;
            return false;
        }

        void snapshot(TestContext c) {
            for (VoxelPart q : parts(c)) {
                FluidNode f = fluidOf(q), r = mods.eln.fluid.FluidWorld.nodeFor(f);
                double s = f.volume / r.volume;   // (a junction is never lumped: its run is itself, s = 1)
                share.put(q, new double[]{r.mWater * s, r.mAir * s, r.energy * s, r.mass() * s});
                state.put(q, new double[]{r.P(), r.T()});
            }
        }

        /** The survivors' runs now (each lump once) against their shares and state before the break; look 0, 1, 2. */
        void compare(TestContext c, int look) {
            double[] w = new double[4], g = new double[4];
            java.util.Set<FluidNode> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            double dp = 0, dt = 0;
            int n = 0;
            for (VoxelPart q : parts(c)) {
                // (after a reload the parts are new objects: matched to the old ones by block and points)
                double[] s = match(q, share), st = match(q, state);
                if (s == null) continue;
                n++;
                for (int k = 0; k < 4; k++) w[k] += s[k];
                FluidNode r = mods.eln.fluid.FluidWorld.nodeFor(fluidOf(q));
                if (seen.add(r)) {
                    g[0] += r.mWater;
                    g[1] += r.mAir;
                    g[2] += r.energy;
                    g[3] += r.mass();
                }
                dp = Math.max(dp, Math.abs(r.P() - st[0]) / st[0]);
                dt = Math.max(dt, Math.abs(r.T() - st[1]));
            }
            if (look == 0) {
                want = w;
                got = g;
                survivors = n;
                gone = share.size() - n;
                worstP = dp;
                worstT = dt;
            } else if (look == 1) {
                gotLater = g;
                worstPLater = dp;
                burstAfter = anyBurst(c);
            } else {
                gotReloaded = g;
                worstPReloaded = dp;
            }
        }

        /** The value for part q, or for the part before a reload with the same kind and points. */
        static double[] match(VoxelPart q, Map<VoxelPart, double[]> m) {
            double[] v = m.get(q);
            if (v != null) return v;
            for (var e : m.entrySet())
                if (e.getKey().getClass() == q.getClass() && same(e.getKey(), q)) return e.getValue();
            return null;
        }

        static boolean same(VoxelPart a, VoxelPart b) {
            if (a instanceof PipePart pa && b instanceof PipePart pb)
                return java.util.Arrays.equals(pa.points, pb.points) && pa.fluid.z == pb.fluid.z;
            if (a instanceof JunctionPart ja && b instanceof JunctionPart jb)
                return ja.x == jb.x && ja.y == jb.y && ja.z == jb.z && ja.fluid.z == jb.fluid.z;
            return false;
        }

        void doBreak(TestContext c) {
            switch (how) {
                case PLAYER -> {
                    var pl = c.player();
                    double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
                    // (eyes 1.4 above the block's floor, looking down at the pipe: a survival player's mining, the whole server path)
                    pl.setPos(at[2].getX() + 0.5, at[2].getY() + 1.4 - pl.getEyeHeight(), at[2].getZ() + 0.5);
                    pl.setXRot(90);
                    pl.setYRot(0);
                    pl.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                    pl.gameMode.destroyBlock(at[2]);
                    pl.snapTo(px, py, pz, 0, 0);
                    if (c.voxelAt(at[2]) != null) err += "the player's mining left the piece; ";
                }
                case NOBODY -> c.level().destroyBlock(at[2], false);
                case SHEARS -> {
                    var pl = c.player();
                    double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
                    pl.snapTo(at[2].getX() + 0.5, at[2].getY(), at[2].getZ() + 0.5, 0, 0);
                    try {
                        var bag = ToolbagCases.ShearsCase.withShears(ToolbagCases.bag());
                        pl.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bag);
                        var h = net.minecraft.world.phys.Vec3.atLowerCornerOf(at[2]).add(0.5, 8.5 / 16, 0.5);
                        mods.eln.node.voxel.ToolbagShears.leftClick(pl, c.level(), bag, at[2], h);
                        mods.eln.node.voxel.ToolbagShears.aimForTests(pl, c.level(), at[2], h);
                        mods.eln.node.voxel.ToolbagShears.leftClick(pl, c.level(), bag, at[2], h);
                    } finally {
                        ToolbagCases.clean(pl);
                        mods.eln.node.voxel.ToolbagShears.forget(pl.getUUID());
                        pl.snapTo(px, py, pz, 0, 0);
                    }
                    if (c.voxelAt(at[2]) != null) err += "the shears left the piece; ";
                }
                case END_JOIN -> breakPipe(c, at[2], pp -> pp.points[0] == 16 || pp.points[pp.points.length - 3] == 16);
                case TEE -> {
                    breakPipe(c, at[2], pp -> pp.points[1] != pp.points[4]);   // (the riser's piece in the tee's block)
                    breakPipe(c, at[2], pp -> pp.points[0] == 16 || pp.points[pp.points.length - 3] == 16);
                }
                case BURST -> {
                    int broken = 0;
                    for (int i = 0; i < N; i++) {
                        VoxelNode m = c.voxelAt(at[i]);
                        if (m != null && m.parts.get(0) instanceof PipePart pp && pp.burst) {
                            if (m.playerAskToBreakPart(c.player(), 0)) m.removeEmptyBlock();
                            broken++;
                        }
                    }
                    if (broken != 1) err += broken + " burst pieces; ";
                }
            }
            if ((how == How.END_JOIN || how == How.TEE) && c.voxelAt(at[2]).parts.stream().anyMatch(q -> q instanceof JunctionPart))
                err += "the junction stayed; ";
        }

        /** A player breaks the first pipe in the block at p that matches (VoxelNode.playerAskToBreakPart, as mining it does). */
        void breakPipe(TestContext c, BlockPos p, java.util.function.Predicate<PipePart> which) {
            VoxelNode m = c.voxelAt(p);
            for (int i = 0; i < m.parts.size(); i++)
                if (m.parts.get(i) instanceof PipePart pp && which.test(pp)) {
                    m.playerAskToBreakPart(c.player(), i);
                    return;
                }
            err += "no such pipe at " + p.toShortString() + "; ";
        }

        static String totals(double[] v) {
            return v == null ? "-" : String.format("water %.6g kg, air %.6g kg, energy %.6g J", v[0], v[1], v[2]);
        }

        static boolean near(double[] a, double[] b, double rel) {
            if (a == null || b == null) return false;
            for (int k = 0; k < a.length; k++) if (Math.abs(a[k] - b[k]) > rel * Math.abs(b[k]) + 1e-15) return false;
            return true;
        }

        public void measure(TestContext c) {
            String what = "pipe break (" + fluid + ", " + how.name().toLowerCase(java.util.Locale.ROOT) + "): ";
            c.line(String.format("%s%d parts gone, %d left; left's shares before %s; their runs after %s, %d steps on %s, reloaded %s; "
                    + "worst change of a run left: %.2g of its pressure, %.2g K (%.2g after %d steps, %.2g reloaded)", what, gone, survivors,
                totals(want), totals(got), LATER, totals(gotLater), totals(gotReloaded), worstP, worstT, worstPLater, LATER, worstPReloaded));
            c.check(what + "built and broken", err.isEmpty(), err);
            c.check(what + "a piece went, others are left", gone > 0 && survivors > 0, gone + " gone, " + survivors + " left");
            c.check(what + "the runs left hold exactly their shares from before (the pieces' shares deleted)", near(got, want, 1e-9),
                totals(want) + " wanted, " + totals(got) + " there");
            c.check(what + "the runs left keep their pressure and temperature", worstP < 1e-6 && worstT < 1e-6,
                String.format("%.2g of the pressure, %.2g K", worstP, worstT));
            c.check(what + "and still do after " + LATER + " steps", worstPLater < 0.01, String.format("%.2g of the pressure", worstPLater));
            // (mass only: the walls trade heat with the air)
            c.check(what + "no water or air made or lost after the break", gotLater != null && got != null
                    && near(new double[]{gotLater[0], gotLater[1]}, new double[]{got[0], got[1]}, how == How.BURST ? 0.01 : 1e-6),
                totals(got) + " -> " + totals(gotLater));
            if (how == How.BURST) c.check(what + "it had burst", burstBefore, "burst before " + burstBefore);
            else c.check(what + "nothing burst", !burstAfter, "burst before " + burstBefore + ", after " + burstAfter);
            c.check(what + "a save round trip keeps the runs left", near(gotReloaded, gotLater, 1e-9) && worstPReloaded - worstPLater < 1e-6,
                totals(gotLater) + " -> " + totals(gotReloaded));
        }
    }
}
