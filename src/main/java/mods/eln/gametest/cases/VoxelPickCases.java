package mods.eln.gametest.cases;

import mods.eln.compat.jade.VoxelProbe;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.node.voxel.VoxelPick;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What the crosshair is on in a voxel block (VoxelPick; Baughn 2026-11-28: "Jade readings on multiple cables in one
 * block sometimes seem to pick the wrong cable. Or pipe."): the pick follows the line of sight to the part drawn under
 * the crosshair, whichever part was laid last and whatever outline the ray stopped on; fittings, pipe sensors and
 * junctions as before; and the outline, Jade's entry, middle-click and breaking agree on it.
 */
public final class VoxelPickCases {
    private VoxelPickCases() {
    }

    static final int LV = CablePart.tierOf("Low Voltage Cable"), MV = CablePart.tierOf("Medium Voltage Cable");

    public static void addTo(List<TestCase> cases) {
        cases.add(new Rays("voxel_pick_parallel_cables", VoxelPickCases::parallel));
        cases.add(new Rays("voxel_pick_fittings", VoxelPickCases::fittings));
        cases.add(new Rays("voxel_pick_junction", VoxelPickCases::junction));
        cases.add(new Rays("voxel_pick_device_beside_cable", VoxelPickCases::device));
        cases.add(new Agree());
        cases.add(new JadeEntries());
    }

    /**
     * A line of sight into the block at pos, from eye toward at (block-local voxels), and what it is on: a VoxelPart or
     * a SixNodeElement. atHit: what the place its ray stops on the outline is on by itself, where that differs or
     * matters (the point form, for clicks worked out by code); null: not checked.
     */
    record Row(BlockPos pos, String what, double[] eye, double[] at, Object want, Object atHit) {
    }

    interface Scene {
        List<Row> build(TestContext c, BlockPos at);
    }

    static VoxelNode.Aimed aimedOf(VoxelNode v, Object o) {
        if (o instanceof SixNodeElement e) return new VoxelNode.Aimed(e.slot, -1);
        return new VoxelNode.Aimed(-1, v.parts.indexOf(o));
    }

    static String say(VoxelNode v, VoxelNode.Aimed a) {
        if (a.element() >= 0) return "device in slot " + a.element();
        if (a.part() < 0) return "nothing";
        VoxelPart p = v.parts.get(a.part());
        return p.kind() + " " + Arrays.toString(p.boxes().get(0)) + " (" + v.nameOf(a) + ")";
    }

    static Vec3 world(BlockPos pos, double[] voxels) {
        return new Vec3(pos.getX() + voxels[0] / 16, pos.getY() + voxels[1] / 16, pos.getZ() + voxels[2] / 16);
    }

    /** Where a ray from eye toward at stops on the block's outline (its shape alone: no other block matters), or null. */
    static Vec3 hit(TestContext c, BlockPos pos, double[] eye, double[] at) {
        Vec3 from = world(pos, eye), to = world(pos, at);
        var r = c.level().getBlockState(pos).getShape(c.level(), pos).clip(from, from.add(to.subtract(from).normalize().scale(8)), pos);
        return r == null ? null : r.getLocation();
    }

    /** Rows of lines of sight into a scene, each on the part or device it should be. */
    static final class Rays implements TestCase {
        final String id;
        final Scene scene;
        List<Row> rows;

        Rays(String id, Scene scene) {
            this.id = id;
            this.scene = scene;
        }

        public String id() {
            return id;
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            rows = scene.build(c, c.at(1, 1, 1));
        }

        public void measure(TestContext c) {
            for (Row r : rows) {
                VoxelNode v = c.voxelByEntity(r.pos());
                Vec3 h = hit(c, r.pos(), r.eye(), r.at());
                if (h == null) {
                    c.check(r.what(), false, "the ray misses the block's outline");
                    continue;
                }
                Vec3 o = Vec3.atLowerCornerOf(r.pos());
                var e = (VoxelNodeEntity) c.level().getBlockEntity(r.pos());
                var got = VoxelPick.pick(e.scene(), world(r.pos(), r.eye()).subtract(o), null, h.subtract(o));
                var want = aimedOf(v, r.want());
                c.check(r.what(), got.equals(want), "on " + say(v, got) + ", should be " + say(v, want) + "; outline hit at " + h.subtract(o).scale(16));
                if (r.atHit() == null) continue;
                var point = v.aimed(h.x - o.x, h.y - o.y, h.z - o.z);
                c.check(r.what() + " (the outline point alone)", point.equals(aimedOf(v, r.atHit())),
                    "on " + say(v, point) + ", should be " + say(v, aimedOf(v, r.atHit())));
            }
        }
    }

    /**
     * Two floor cables along x, a voxel apart, so each lies in the other's outline: an LV cable laid first (z 5..7), an
     * MV cable laid second (z 8..11).
     */
    static CablePart[] twoCables(TestContext c, BlockPos at) {
        CablePart older = new CablePart(LV, 0, new int[]{0, 0, 6, 16, 0, 6}), newer = new CablePart(MV, 0, new int[]{0, 0, 9, 16, 0, 9});
        CableCases.voxel(c, at, List.of(older, newer));
        return new CablePart[]{older, newer};
    }

    /** The line over the older cable's top (0.6 voxels clear of it) that comes down into the newer one: it stops on the
     *  older one's outline. From 12 times as far back as the cables are apart. */
    static double[][] parallax(CablePart older, CablePart newer) {
        int[] a = older.boxes().get(0), b = newer.boxes().get(0);
        double[] over = {8, a[4] + 0.6, (a[2] + a[5]) / 2.0}, into = {8, b[4] - 0.5, (b[2] + b[5]) / 2.0};
        double[] eye = new double[3];
        for (int k = 0; k < 3; k++) eye[k] = into[k] + 12 * (over[k] - into[k]);
        return new double[][]{eye, into};
    }

    static List<Row> parallel(TestContext c, BlockPos at) {
        CablePart[] k = twoCables(c, at);
        int[] a = k[0].boxes().get(0), b = k[1].boxes().get(0);
        double za = (a[2] + a[5]) / 2.0, zb = (b[2] + b[5]) / 2.0, gap = (a[5] + b[2]) / 2.0;
        double[][] p = parallax(k[0], k[1]);
        return List.of(
            // (both outline points are in both cables' fattened boxes: the last laid took them all)
            new Row(at, "parallel cables: from above, over the one laid first", new double[]{8, 40, za}, new double[]{8, 0, za}, k[0], k[0]),
            new Row(at, "parallel cables: from above, over the one laid second", new double[]{8, 40, zb}, new double[]{8, 0, zb}, k[1], k[1]),
            new Row(at, "parallel cables: from above, between them and nearer the first", new double[]{8, 40, gap - 0.2}, new double[]{8, 0, gap - 0.2}, k[0], k[0]),
            new Row(at, "parallel cables: from above, between them and nearer the second", new double[]{8, 40, gap + 0.2}, new double[]{8, 0, gap + 0.2}, k[1], k[1]),
            // (the outline stands 2 voxels proud: seen low across the first cable, the crosshair is drawn on the second
            // while its ray has stopped on the first one's outline)
            new Row(at, "parallax: a line that grazes the first cable's outline and comes down on the second", p[0], p[1], k[1], k[0]));
    }

    /** A level pipe with a valve on it in one block, one with a sensor fitted in the next: the fitting where the
     *  crosshair is on it, the pipe beside it. */
    static List<Row> fittings(TestContext c, BlockPos at) {
        List<Row> rows = new ArrayList<>();
        PipeCases.lay(c, at, 0, 0, 8, 8, 16, 8, 8);
        VoxelNode v = c.voxelByEntity(at);
        String err = v.addValve(8, 8, 8, ValvePart.ON_OFF);
        if (err != null) throw new IllegalStateException("valve: " + err);
        ValvePart valve = null;
        for (VoxelPart p : v.parts) if (p instanceof ValvePart x) valve = x;
        int[] wheel = valve.boxes().get(2);
        double[] w = {(wheel[0] + wheel[3]) / 2.0, (wheel[1] + wheel[4]) / 2.0, (wheel[2] + wheel[5]) / 2.0};
        rows.add(new Row(at, "valve: its handwheel from above", new double[]{w[0], 40, w[2]}, w, valve, valve));
        rows.add(new Row(at, "valve: its body from the side", new double[]{8, 8, -30}, new double[]{8, 8, 8}, valve, valve));
        VoxelPart pipe = v.parts.get(v.partAt(1 / 16.0, 0.5, 0.5));
        rows.add(new Row(at, "valve: the pipe 7 voxels along from it", new double[]{1, 40, 8}, new double[]{1, 8, 8}, pipe, pipe));
        // (the handwheel's outline reaches over the pipe: the ray stops on it, the crosshair is drawn on the pipe below)
        rows.add(new Row(at, "valve: the pipe under the edge of the handwheel's outline", new double[]{wheel[0] - 1.5, 40, 8}, new double[]{wheel[0] - 1.5, 8, 8}, pipe, valve));

        BlockPos s = at.east(2);
        PipeCases.lay(c, s, 0, 0, 8, 8, 16, 8, 8);
        VoxelNode n = c.voxelByEntity(s);
        err = n.addSensorNear(0.5, 0.5, 0.5, PipeSensorPart.PRESSURE);
        if (err != null) throw new IllegalStateException("sensor: " + err);
        PipeSensorPart sensor = null;
        for (VoxelPart p : n.parts) if (p instanceof PipeSensorPart x) sensor = x;
        int[] b = sensor.boxes().get(0);
        double[] mid = {(b[0] + b[3]) / 2.0, (b[1] + b[4]) / 2.0, (b[2] + b[5]) / 2.0}, eye = new double[3];
        for (int a = 0; a < 3; a++) eye[a] = mid[a] + (mid[a] - 8) * 20;   // (out from the pipe's axis, through the sensor)
        eye[0] = mid[0];
        rows.add(new Row(s, "pipe sensor: from its own side", eye, mid, sensor, sensor));
        VoxelPart run = n.parts.get(n.partAt(2 / 16.0, 0.5, 0.5));
        rows.add(new Row(s, "pipe sensor: the pipe 6 voxels along from it", new double[]{2, 8, -30}, new double[]{2, 8, 8}, run, run));
        return rows;
    }

    /** A tee: the junction is never the answer, the arm on the side looked at is; the riser is itself. */
    static List<Row> junction(TestContext c, BlockPos at) {
        VoxelNode v = JunctionCases.tee(c, at);
        JunctionPart j = null;
        PipePart west = null, east = null, riser = null;
        for (VoxelPart p : v.parts) {
            if (p instanceof JunctionPart x) j = x;
            if (!(p instanceof PipePart pipe)) continue;
            int[] q = pipe.points;
            if (q[1] != q[4]) riser = pipe;
            else if (Math.min(q[0], q[3]) < 8) west = pipe;
            else east = pipe;
        }
        if (j == null || west == null || east == null || riser == null) throw new IllegalStateException("no tee: " + v.parts);
        return List.of(
            new Row(at, "junction: its west half from the side is the west arm", new double[]{j.x - 1.5, j.y, -30}, new double[]{j.x - 1.5, j.y, j.z}, west, west),
            new Row(at, "junction: its east half from the side is the east arm", new double[]{j.x + 1.5, j.y, -30}, new double[]{j.x + 1.5, j.y, j.z}, east, east),
            new Row(at, "junction: its west half from above is the west arm", new double[]{j.x - 1.5, 40, j.z - 1.5}, new double[]{j.x - 1.5, j.y, j.z - 1.5}, west, null),
            new Row(at, "junction: the riser above it is the riser", new double[]{8, 11, -30}, new double[]{8, 11, 8}, riser, riser));
    }

    /** An LV cable along the floor (z 2..4) and a relay off centre beside it, in one block. */
    static SixNodeElement relayBesideCable(TestContext c, BlockPos at) {
        CableCases.voxel(c, at, List.of(new CablePart(LV, 0, new int[]{0, 0, 3, 16, 0, 3})));
        VoxelNode v = c.voxelByEntity(at);
        String err = v.placeAnywhere(VoxelDeviceCases.desc("Low Voltage Relay").newItemStack(), Direction.YN, 0.5, 0, 11 / 16.0, c.player());
        if (err != null) throw new IllegalStateException("relay: " + err);
        for (int i = 6; i < mods.eln.node.six.SixNode.SLOTS; i++) if (v.getElement(i) != null) return v.getElement(i);
        throw new IllegalStateException("no relay");
    }

    static List<Row> device(TestContext c, BlockPos at) {
        SixNodeElement relay = relayBesideCable(c, at);
        VoxelNode v = c.voxelByEntity(at);
        int[] body = v.elementBoxes(relay.slot).get(0);
        double bx = (body[0] + body[3]) / 2.0, bz = (body[2] + body[5]) / 2.0;
        VoxelPart cable = v.parts.get(0);
        return List.of(
            new Row(at, "device beside a cable: its body from above", new double[]{bx, 40, bz}, new double[]{bx, 0, bz}, relay, relay),
            new Row(at, "device beside a cable: the cable from above", new double[]{2, 40, 3}, new double[]{2, 0, 3}, cable, cable),
            // (low across the cable onto the device's body: the ray stops on the cable's outline)
            new Row(at, "device beside a cable: seen low across the cable", new double[]{bx, body[4] + 6, bz - 40}, new double[]{bx, body[4] - 0.2, bz}, relay, null));
    }

    /**
     * One player, one line of sight (the parallax one: its ray stops on the first cable's outline, the crosshair is
     * drawn on the second): the outline's boxes, Jade's entry as the client chooses it, middle-click and the cable
     * broken are all the second cable.
     */
    static final class Agree implements TestCase {
        BlockPos at;
        CablePart[] k;

        public String id() {
            return "voxel_pick_outline_jade_and_break_agree";
        }

        public int width() {
            return 2;
        }

        public int depth() {
            return 5;   // (floor_2x4x5: the player looks along z from 1.7 blocks back)
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 4);
            c.floor(at.below());
            k = twoCables(c, at);
        }

        public void measure(TestContext c) {
            VoxelNode v = c.voxelByEntity(at);
            var e = (VoxelNodeEntity) c.level().getBlockEntity(at);
            double[][] p = parallax(k[0], k[1]);
            var player = c.player();
            double x0 = player.getX(), y0 = player.getY(), z0 = player.getZ();
            Vec3 eye = world(at, p[0]);
            player.snapTo(eye.x, eye.y - player.getEyeHeight(), eye.z, 0, 0);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, world(at, p[1]));
            try {
                var ray = player.pick(player.blockInteractionRange() + 1, 1f, false);
                if (!(ray instanceof net.minecraft.world.phys.BlockHitResult bh) || !bh.getBlockPos().equals(at)) {
                    c.check("agree: the player's ray stops on the block", false, ray.getType() + " at " + ray.getLocation());
                    return;
                }
                Vec3 o = Vec3.atLowerCornerOf(at);
                Vec3 h = bh.getLocation().subtract(o);
                var onOutline = v.aimed(h.x, h.y, h.z);
                c.check("agree: the ray stops on the first cable's outline", onOutline.part() == 0, say(v, onOutline) + " at " + h.scale(16));
                var a = e.aimed(player, bh.getLocation());
                c.check("agree: the pick is the second cable", a.part() == 1 && a.element() < 0, say(v, a));
                List<int[]> want = new ArrayList<>();
                for (int[] b : k[1].boxes())
                    want.add(new int[]{Math.max(0, b[0] - 2), Math.max(0, b[1] - 2), Math.max(0, b[2] - 2), Math.min(16, b[3] + 2), Math.min(16, b[4] + 2), Math.min(16, b[5] + 2)});
                List<int[]> outline = e.outline(a);
                c.check("agree: the outline drawn is the second cable's", outline != null && Arrays.deepEquals(outline.toArray(), want.toArray()),
                    outline == null ? "none" : Arrays.deepToString(outline.toArray()));
                // Jade: what the server sends this player, and the entry a client picks from it for the same line (a
                // client's parts have no ids: a copy of the cable stands in for them)
                var sent = VoxelProbe.entries(v, v.aimed(player, bh.getLocation()), 3, "20 C");
                var clientParts = List.<VoxelPart>of(new CablePart(LV, 0, k[0].points.clone()), new CablePart(MV, 0, k[1].points.clone()));
                var entry = VoxelProbe.choose(sent, a, clientParts);
                String title = entry == null ? "none" : entry.getStringOr("elnTitle", "");
                c.check("agree: Jade's entry is the second cable's", entry != null && entry.getIntOr("id", -9) == k[1].id && title.startsWith("Medium Voltage Cable")
                    && sent.getCompoundOrEmpty(0).equals(entry), title + " of " + sent);
                var clone = c.level().getBlockState(at).getCloneItemStack(at, c.level(), true, player);
                c.check("agree: middle-click gives the second cable's kit", clone.getItem() instanceof mods.eln.node.voxel.CableKitItem
                    && clone.getHoverName().getString().contains("Medium Voltage"), clone.getHoverName().getString());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS));
                var state = c.level().getBlockState(at);
                float progress = state.getDestroyProgress(player, c.level(), at), shears = mods.eln.node.BreakTool.progress(state, player, c.level(), at,
                    mods.eln.node.BreakTool.Kind.SHEARS), pickaxe = mods.eln.node.BreakTool.progress(state, player, c.level(), at, mods.eln.node.BreakTool.Kind.PICKAXE);
                c.check("agree: shears break it at a cable's speed", progress == shears && shears > pickaxe, progress + ", shears " + shears + ", pickaxe " + pickaxe);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                c.level().getBlockState(at).onDestroyedByPlayer(c.level(), at, player, net.minecraft.world.item.ItemStack.EMPTY, true, c.level().getFluidState(at));
                c.check("agree: breaking takes the second cable and leaves the first", v.parts.size() == 1 && v.parts.get(0) == k[0], "" + v.parts.size());
            } finally {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
                player.snapTo(x0, y0, z0, 0, 0);
            }
        }
    }

    /** Jade's server data for a block of two cables and a relay: an entry each, and the entry chosen for each. */
    static final class JadeEntries implements TestCase {
        BlockPos at;
        SixNodeElement relay;

        public String id() {
            return "voxel_pick_jade_entry_per_part";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 1);
            relay = relayBesideCable(c, at);
            CableCases.voxel(c, at, List.of(new CablePart(MV, 0, new int[]{0, 0, 6, 16, 0, 6})));
        }

        public void measure(TestContext c) {
            VoxelNode v = c.voxelByEntity(at);
            var sent = VoxelProbe.entries(v, null, 3, "20 C");
            c.line("jade entries: " + sent);
            int parts = 0, devices = 0, whole = 0;
            for (int i = 0; i < sent.size(); i++) {
                var t = sent.getCompoundOrEmpty(i);
                if (t.contains("slot")) devices++;
                else if (t.contains("id")) parts++;
                else whole++;
            }
            c.check("jade entries: one per part, one per device, one for the block as a whole", parts == v.parts.size() && devices == 1 && whole == 1
                && sent.size() <= VoxelProbe.MAX + 1, parts + " parts, " + devices + " devices, " + whole + " whole of " + sent.size());
            String wrong = "";
            for (int i = 0; i < v.parts.size(); i++) {
                VoxelPart p = v.parts.get(i);
                var a = new VoxelNode.Aimed(-1, i);
                var t = VoxelProbe.choose(sent, a, v.parts);
                // (as the client has the parts: without ids)
                List<VoxelPart> copies = new ArrayList<>();
                for (VoxelPart q : v.parts) copies.add(new CablePart(((CablePart) q).tier, 0, ((CablePart) q).points.clone()));
                var byBox = VoxelProbe.choose(sent, a, copies);
                var lines = t == null ? new net.minecraft.nbt.ListTag() : t.getListOrEmpty("elnProbe");
                if (t == null || !t.equals(byBox) || t.getIntOr("id", -9) != p.id || !t.getStringOr("elnTitle", "").equals(v.nameOf(a))
                    || !Arrays.equals(t.getIntArray("box").orElse(null), p.boxes().get(0))
                    || !lines.getStringOr(0, "").startsWith("Voltage\t") || !lines.getStringOr(lines.size() - 1, "").equals("Ambient\t20 C"))
                    wrong += "part " + i + ": " + t + " / " + byBox + "; ";
            }
            c.check("jade entries: each cable's entry has its id, its box, its name and its own lines, and is the one chosen for it", wrong.isEmpty(), wrong);
            var d = VoxelProbe.choose(sent, new VoxelNode.Aimed(relay.slot, -1), v.parts);
            c.check("jade entries: the relay's entry is chosen for its slot", d != null && d.getIntOr("slot", -9) == relay.slot
                && d.getStringOr("elnTitle", "").equals("Low Voltage Relay") && !d.getListOrEmpty("elnProbe").isEmpty(), "" + d);
            var none = VoxelProbe.choose(sent, VoxelPick.NONE, v.parts);
            c.check("jade entries: aimed at none of them, the block's lines as a whole", none != null && !none.contains("id") && !none.contains("slot"), "" + none);
            // the one under the asker's crosshair goes first (it survives the cap), and nothing is sent twice
            var first = VoxelProbe.entries(v, new VoxelNode.Aimed(-1, 1), 3, "20 C");
            c.check("jade entries: the asker's own goes first, once", first.size() == sent.size() && first.getCompoundOrEmpty(0).getIntOr("id", -9) == v.parts.get(1).id,
                "" + first);
        }
    }
}
