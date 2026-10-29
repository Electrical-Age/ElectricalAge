package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.SafetyValvePart;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Pipe sizes (Baughn 2026-10-29): iron and alloy pipes drawn at a quarter of their old 4 and 6 voxels and taking 2 on
 * the grid; the large alloy pipe drawn and taking 6. Fittings saved before keep loading (their old widths mapped).
 */
public final class PipeSizeCases {
    private PipeSizeCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Widths());
        cases.add(new SideBySide());
        cases.add(new OldFittingsLoad());
        cases.add(new LargeFittings());
    }

    static int across(PipePart p) {
        int[] b = p.boxes().get(0);
        return b[3] - b[0];   // (a pipe along z: its x extent)
    }

    /** Each tier's grid width and drawn width. */
    static final class Widths implements TestCase {
        public String id() {
            return "pipe_widths_per_tier";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            int[] want = {2, 2, 6};
            double[] drawn = {1.0, 1.5, 6.0};
            for (int t = 0; t < 3; t++) {
                PipePart p = new PipePart(t, new int[]{8, 8, 0, 8, 8, 16});
                ctx.check("pipe " + PipePart.TIER_NAME[t] + ": " + want[t] + " voxels on the grid, drawn " + drawn[t],
                    across(p) == want[t] && p.drawn() == drawn[t], across(p) + " / " + p.drawn());
            }
            ctx.check("large alloy pipe: 256 x the alloy's volume, 1024 x its conductance",
                Math.abs(PipePart.VOLUME_PER_BLOCK[2] / PipePart.VOLUME_PER_BLOCK[1] - 256) < 1e-6
                    && Math.abs(PipePart.K_PER_BLOCK[2] / PipePart.K_PER_BLOCK[1] - 1024) < 1e-6, "");
        }
    }

    /** In one block: iron pipes at 4 and 8 fit side by side (2 wide); large ones at 4 and 12 do, at 4 and 8 they don't. */
    static final class SideBySide implements TestCase {
        String iron, large, clash;

        public String id() {
            return "pipe_side_by_side_by_width";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
            BlockPos a = ctx.at(2, 1, 1), b = ctx.at(5, 1, 1);
            iron = PipeKitItem.place(ctx.level(), a, new PipePart(0, new int[]{4, 8, 0, 4, 8, 16}), null);
            if (iron == null) iron = PipeKitItem.place(ctx.level(), a, new PipePart(0, new int[]{8, 8, 0, 8, 8, 16}), null);
            large = PipeKitItem.place(ctx.level(), b, new PipePart(2, new int[]{4, 8, 0, 4, 8, 16}), null);
            if (large == null) large = PipeKitItem.place(ctx.level(), b, new PipePart(2, new int[]{12, 8, 0, 12, 8, 16}), null);
            clash = PipeKitItem.place(ctx.level(), b, new PipePart(2, new int[]{8, 4, 0, 8, 4, 16}), null);
        }

        public void measure(TestContext ctx) {
            ctx.check("iron pipes 4 voxels apart fit", iron == null, "" + iron);
            ctx.check("large pipes at 4 and 12 fit", large == null, "" + large);
            ctx.check("a third large pipe between them doesn't", clash != null, "" + clash);
        }
    }

    /** A valve, safety valve and sensor saved with their old widths (4: iron, 6: alloy; no "g" key) load at 2. */
    static final class OldFittingsLoad implements TestCase {
        public String id() {
            return "pipe_old_fittings_load_with_new_widths";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var v = new ValvePart(8, 8, 8, 0, 6, ValvePart.ON_OFF);
            var t = new net.minecraft.nbt.CompoundTag();
            v.write(t);
            t.remove("g");
            t.putIntArray("at", new int[]{8, 8, 8, 0, 6});
            var old = (ValvePart) mods.eln.node.voxel.VoxelPart.load(withKind(t, ValvePart.KIND));
            var s = new SafetyValvePart(8, 8, 8, 1, 4, 10e5);
            var ts = new net.minecraft.nbt.CompoundTag();
            s.write(ts);
            ts.remove("g");
            var oldS = (SafetyValvePart) mods.eln.node.voxel.VoxelPart.load(withKind(ts, SafetyValvePart.KIND));
            var now = new ValvePart(8, 8, 8, 0, 6, ValvePart.ON_OFF);
            var tn = new net.minecraft.nbt.CompoundTag();
            now.write(tn);
            var kept = (ValvePart) mods.eln.node.voxel.VoxelPart.load(withKind(tn, ValvePart.KIND));
            ctx.check("an old alloy valve (6) loads at the alloy pipe's 2", old.bore == 2, "" + old.bore);
            ctx.check("an old iron safety valve (4) loads at 2", oldS.bore == 2, "" + oldS.bore);
            ctx.check("a large pipe's valve saved now (6, with the g key) stays 6", kept.bore == 6, "" + kept.bore);
        }

        static net.minecraft.nbt.CompoundTag withKind(net.minecraft.nbt.CompoundTag t, String kind) {
            if (!t.contains("kind")) t.putString("kind", kind);
            return t;
        }
    }

    /** A valve and a safety valve fit on a large pipe, and its fittings are sized to it. */
    static final class LargeFittings implements TestCase {
        String valve, safety;
        VoxelNode n;

        public String id() {
            return "pipe_large_fittings";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
            BlockPos a = ctx.at(2, 1, 1), b = ctx.at(4, 1, 1);
            PipeCases.lay(ctx, a, 2, 8, 8, 0, 8, 8, 16);
            PipeCases.lay(ctx, b, 2, 8, 8, 0, 8, 8, 16);
            n = PipeCases.node(ctx, a);
            valve = n.addValve(8, 8, 8, ValvePart.ON_OFF);
            safety = PipeCases.node(ctx, b).addSafetyValveNear(0.5, 0.68, 0.5);   // (on its top: the 6-voxel pipe reaches to 11)
        }

        public void measure(TestContext ctx) {
            ValvePart v = null;
            for (var p : n.parts) if (p instanceof ValvePart vp) v = vp;
            ctx.check("a valve fits on a large pipe, sized to it", valve == null && v != null && v.bore == 6, valve + " " + (v == null ? "none" : v.bore));
            ctx.check("a safety valve fits on a large pipe", safety == null, "" + safety);
        }
    }
}
