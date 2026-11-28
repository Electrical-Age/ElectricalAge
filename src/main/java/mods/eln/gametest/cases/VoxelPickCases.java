package mods.eln.gametest.cases;

import mods.eln.compat.jade.VoxelProbe;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.CablePlacer;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.node.voxel.JunctionPart;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelNodeEntity;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.node.voxel.VoxelPick;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
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
        cases.add(new Rays("voxel_pick_behind_imitated_block", VoxelPickCases::underSlab));
        cases.add(new Rays("voxel_pick_device_beside_cable", VoxelPickCases::device));
        cases.add(new Agree());
        cases.add(new ItemClicks());
        cases.add(new JadeEntries());
    }

    /**
     * A line of sight into the block at pos, from eye toward at (block-local voxels), and what it is on: a VoxelPart, a
     * SixNodeElement, or NOTHING. atHit: what the place its ray stops on the outline is on by itself, where that differs
     * or matters (the point form, for clicks worked out by code); null: not checked. eye null: no line, the point at
     * alone (a click placement code works out).
     */
    record Row(BlockPos pos, String what, double[] eye, double[] at, Object want, Object atHit) {
    }

    static final Object NOTHING = "nothing";

    interface Scene {
        List<Row> build(TestContext c, BlockPos at);
    }

    static VoxelNode.Aimed aimedOf(VoxelNode v, Object o) {
        if (o == NOTHING) return VoxelPick.NONE;
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
            java.util.Set<BlockPos> blocks = new java.util.LinkedHashSet<>();
            for (Row r : rows) {
                VoxelNode v = c.voxelByEntity(r.pos());
                blocks.add(r.pos());
                if (r.eye() == null) {
                    var got = v.aimed(r.at()[0] / 16, r.at()[1] / 16, r.at()[2] / 16);
                    c.check(r.what(), got.equals(aimedOf(v, r.want())), "on " + say(v, got) + ", should be " + say(v, aimedOf(v, r.want())));
                    continue;
                }
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
            for (BlockPos pos : blocks) clientView(c, pos);
        }
    }

    /**
     * The client's view of the block at pos is the server's: every part read back from its publish bytes has the same
     * boxes, and every device off centre has the boxes the client works out from what is published of it (its place,
     * front and pin mask). So the client's pick (the outline, Jade's choice) sees what the server's does.
     */
    static void clientView(TestContext c, BlockPos pos) {
        VoxelNode v = c.voxelByEntity(pos);
        String wrong = "";
        for (VoxelPart p : v.parts) {
            try {
                var bytes = new java.io.ByteArrayOutputStream();
                p.serialize(new java.io.DataOutputStream(bytes));
                VoxelPart back = VoxelPart.deserialize(p.kind(), new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
                if (back == null || !Arrays.deepEquals(back.boxes().toArray(), p.boxes().toArray()))
                    wrong += p.kind() + " " + Arrays.deepToString(p.boxes().toArray()) + " came back " + (back == null ? "null" : Arrays.deepToString(back.boxes().toArray())) + "; ";
            } catch (java.io.IOException e) {
                wrong += p.kind() + ": " + e + "; ";
            }
        }
        int devices = 0;
        for (int i = 6; i < mods.eln.node.six.SixNode.SLOTS; i++) {
            SixNodeElement e = v.getElement(i);
            if (e == null) continue;
            devices++;
            int pins = 0;   // (as SixNode publishes them)
            for (mods.eln.misc.LRDU l : mods.eln.misc.LRDU.values()) if (e.getConnectionMask(l) != 0) pins |= 1 << l.toInt();
            var client = VoxelNode.deviceBoxes(e.sixNodeElementDescriptor, e.side, e.front, e.ox, e.oy, e.oz, pins);
            if (!Arrays.deepEquals(client.toArray(), v.elementBoxes(i).toArray()))
                wrong += "slot " + i + " " + Arrays.deepToString(v.elementBoxes(i).toArray()) + " client " + Arrays.deepToString(client.toArray()) + "; ";
        }
        c.check("client view at " + pos.toShortString() + ": parts read back from their publish bytes and devices from their published place have the server's boxes",
            wrong.isEmpty(), v.parts.size() + " part(s), " + devices + " device(s) off centre; " + wrong);
    }

    /**
     * Two floor cables along x, a voxel apart, so each lies in the other's outline: an LV cable laid first (z 5..7), an
     * MV cable laid second (z 8..11).
     */
    static CablePart[] twoCables(TestContext c, BlockPos at) {
        return twoCables(c, at, 9);
    }

    /** ... the second with its centre line at z (10: on the lattice a kit branches from; z 9..12, 2 voxels apart). */
    static CablePart[] twoCables(TestContext c, BlockPos at, int z) {
        CablePart older = new CablePart(LV, 0, new int[]{0, 0, 6, 16, 0, 6}), newer = new CablePart(MV, 0, new int[]{0, 0, z, 16, 0, z});
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
        err = n.addSensorNearForTests(0.5, 0.5, 0.5, PipeSensorPart.PRESSURE);
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
        // a second tee two blocks east whose riser bends 4 voxels over the junction while its level arms run 6: by the
        // arm's next corner alone (as it was), the junction's west half gave the riser
        BlockPos b = at.east(2);
        PipeCases.lay(c, b, 0, 2, 4, 8, 14, 4, 8);
        VoxelNode n = c.voxelByEntity(b);
        String err = n.addJoined(new PipePart(0, new int[]{8, 4, 8, 8, 8, 8, 8, 8, 16}), 8, 4, 8);
        if (err != null) throw new IllegalStateException("bent tee: " + err);
        PipePart bentWest = null, bentRiser = null;
        for (VoxelPart p : n.parts)
            if (p instanceof PipePart pipe) {
                if (pipe.points.length > 6) bentRiser = pipe;
                else if (Math.min(pipe.points[0], pipe.points[3]) < 8) bentWest = pipe;
            }
        if (bentWest == null || bentRiser == null) throw new IllegalStateException("no bent tee: " + n.parts);
        return List.of(
            new Row(b, "junction with a riser that bends just over it: its west half from the side is still the west arm",
                new double[]{j.x - 1.5, j.y, -30}, new double[]{j.x - 1.5, j.y, j.z}, bentWest, bentWest),
            new Row(b, "junction with a riser that bends just over it: its top from the side is the riser",
                new double[]{j.x, j.y + 1.6, -30}, new double[]{j.x, j.y + 1.6, j.z}, bentRiser, null),
            new Row(at, "junction: its west half from the side is the west arm", new double[]{j.x - 1.5, j.y, -30}, new double[]{j.x - 1.5, j.y, j.z}, west, west),
            new Row(at, "junction: its east half from the side is the east arm", new double[]{j.x + 1.5, j.y, -30}, new double[]{j.x + 1.5, j.y, j.z}, east, east),
            // (its top face beside the riser: nearer the riser's first piece than the west arm's)
            new Row(at, "junction: its top from above, beside the riser, is the riser", new double[]{j.x - 1.5, 40, j.z - 1.5}, new double[]{j.x - 1.5, j.y, j.z - 1.5}, riser, null),
            new Row(at, "junction: the riser above it is the riser", new double[]{8, 11, -30}, new double[]{8, 11, 8}, riser, riser));
    }

    /**
     * A cable on the floor under a top slab the block imitates: from above the line meets the slab first and is on
     * nothing of ours (the slab: its own outline, its own breaking); from the side, under the slab, it is on the cable.
     */
    static List<Row> underSlab(TestContext c, BlockPos at) {
        c.level().setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState()
            .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
        CablePart cable = new CablePart(LV, 0, new int[]{0, 0, 8, 16, 0, 8});
        CableCases.voxel(c, at, List.of(cable));
        if (c.voxelByEntity(at).host == null) throw new IllegalStateException("the slab isn't imitated");
        return List.of(
            new Row(at, "under a slab: from above, the slab hides the cable", new double[]{8, 40, 8}, new double[]{8, 0, 8}, NOTHING, NOTHING),
            new Row(at, "under a slab: from the side, the cable", new double[]{8, 0.5, -30}, new double[]{8, 0.5, 8}, cable, cable),
            // (down at the slab's side face, the cable beyond and below it)
            new Row(at, "under a slab: down onto the slab's side, the cable behind it", new double[]{8, 30, -20}, new double[]{8, 12, 0}, NOTHING, null));
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
        // a signal cable 2 voxels from the relay's west pin, along z: a point 0.3 voxels off the pin is in both
        // outlines, and is the device's (a kit clicked there ends at the pin; the outline is fattened round pins for it)
        int[] pin = null;
        for (int[] b : v.elementBoxes(relay.slot).subList(1, v.elementBoxes(relay.slot).size())) if (pin == null || b[0] < pin[0]) pin = b;
        if (pin == null) throw new IllegalStateException("the relay has no pins");
        CablePart near = new CablePart(CablePart.tierOf("Signal Cable"), 0, new int[]{pin[0] - 3, 0, 7, pin[0] - 3, 0, 16});
        CableCases.voxel(c, at, List.of(near));
        double[] byPin = {pin[0] - 0.3, (pin[1] + pin[4]) / 2.0, (pin[2] + pin[5]) / 2.0};
        var end = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), at, net.minecraft.core.Direction.UP, world(at, byPin), true, new long[1][]);
        c.check("device beside a cable: a kit's click 0.3 voxels off its pin, 1.7 from a cable, ends at the device", end != null && end.dir() >= 0
            && Math.abs(end.x() - (at.getX() * 16L + pin[0])) <= 2, end + " pin " + Arrays.toString(pin) + " cable " + Arrays.toString(near.boxes().get(0)));
        return List.of(
            new Row(at, "device beside a cable: a point 0.3 voxels off its pin and 1.7 from a cable (no line)", null, byPin, relay, null),
            new Row(at, "device beside a cable: a point on the cable's top (no line)", null, new double[]{2, 1, 3}, cable, null),
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
                var a = e.aimed(player.getEyePosition(1f), bh.getLocation());
                c.check("agree: the pick is the second cable", a.part() == 1 && a.element() < 0, say(v, a));
                List<int[]> want = new ArrayList<>();
                for (int[] b : k[1].boxes())
                    want.add(new int[]{Math.max(0, b[0] - 2), Math.max(0, b[1] - 2), Math.max(0, b[2] - 2), Math.min(16, b[3] + 2), Math.min(16, b[4] + 2), Math.min(16, b[5] + 2)});
                List<int[]> outline = e.outline(a);
                c.check("agree: the outline drawn is the second cable's", outline != null && Arrays.deepEquals(outline.toArray(), want.toArray()),
                    outline == null ? "none" : Arrays.deepToString(outline.toArray()));
                // Jade: what the server sends this player, and the entry a client picks from it for the same line (a
                // client's parts have no ids: a copy of the cable stands in for them)
                var sent = VoxelProbe.entries(v, player.getEyePosition(1f), bh.getLocation(), 3, "20 C");
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

    /**
     * Item clicks as a real player's, traced from an eye (GameTests' fake player has none: every other case clicks by
     * the point): the part is the one the line of sight is on, and the place on it is where that line met it, not the
     * hit on another part's outline.
     */
    static final class ItemClicks implements TestCase {
        BlockPos two, crossed, piped;
        CablePart[] k;
        CablePart under;

        public String id() {
            return "voxel_pick_item_clicks_follow_the_line";
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            two = c.at(1, 1, 1);
            k = twoCables(c, two, 10);
            // a pipe crossing 3 voxels over a floor cable
            crossed = c.at(3, 1, 1);
            under = new CablePart(LV, 0, new int[]{0, 0, 8, 16, 0, 8});
            CableCases.voxel(c, crossed, List.of(under));
            PipeCases.lay(c, crossed, 0, 4, 4, 0, 4, 4, 16);
            // a pipe behind a thick (thermal) cable
            piped = c.at(5, 1, 1);
            CableCases.voxel(c, piped, List.of(new CablePart(CablePart.tierOf("Copper Thermal Cable"), 0, new int[]{0, 0, 3, 16, 0, 3})));
            PipeCases.lay(c, piped, 0, 0, 4, 8, 16, 4, 8);
        }

        public void measure(TestContext c) {
            var level = c.level();
            // 1. two parallel cables seen low across the first: the ray stops on the first one's outline, the line comes
            // down on the second. A kit branches off the second, under the line; by the point alone, off the first.
            double[][] p = parallax(k[0], k[1]);
            Vec3 eye = world(two, p[0]), hit = hit(c, two, p[0], p[1]);
            long[][] join = new long[1][], joinPoint = new long[1][];
            var end = CablePlacer.endAtForTests(level, two, net.minecraft.core.Direction.UP, hit, eye, true, join, MV);
            CablePlacer.endAtForTests(level, two, net.minecraft.core.Direction.UP, hit, true, joinPoint, MV);
            long x0 = two.getX() * 16L, z0 = two.getZ() * 16L;
            c.check("item clicks: a kit seen low across one cable onto the next branches off the next, under the line", end != null && join[0] != null
                && join[0][2] == z0 + 10 && join[0][0] == x0 + 8, Arrays.toString(join[0]) + " (block at " + x0 + ", " + z0 + ")");
            c.check("item clicks: the same click by its point alone (no eye) is on the first cable", joinPoint[0] != null && joinPoint[0][2] == z0 + 6,
                Arrays.toString(joinPoint[0]));
            VoxelPart cut = mods.eln.node.voxel.ToolbagShears.pieceAtForTests(level, two, hit, eye);
            c.check("item clicks: the Toolbag's shears are on the cable the line is on", cut == k[1], cut == null ? "none" : Arrays.toString(cut.boxes().get(0)));

            // 2. the Toolbag keeps the click a route started at, with its eye, and starts again from it when another kind
            // is chosen: from the same cable, though the player (here: with no eye at all) stands elsewhere by then
            var pl = c.player();
            double px = pl.getX(), py = pl.getY(), pz = pl.getZ();
            var mvKind = mods.eln.node.voxel.CableSpec.preset(MV);
            var lvKind = mods.eln.node.voxel.CableSpec.preset(LV);
            ItemStack bag = ToolbagCases.bag(mods.eln.node.voxel.CableKitItem.kit(MV, 300), mods.eln.node.voxel.CableKitItem.kit(LV, 300));
            pl.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bag);
            pl.snapTo(hit.x, hit.y + 0.5, hit.z, 0, 90);
            try {
                mods.eln.node.voxel.ToolbagItem.click(pl, level, bag, two, net.minecraft.core.Direction.UP, hit, eye, false);
                long[] started = mods.eln.node.voxel.RoutePlacer.startJoinForTests(pl.getUUID());
                ToolbagCases.radial(pl, mods.eln.node.voxel.ToolbagItem.CHOOSE, new int[]{0, LV, lvKind.mm2(), lvKind.colour()});
                long[] again = mods.eln.node.voxel.RoutePlacer.startJoinForTests(pl.getUUID());
                c.check("item clicks: a Toolbag route starts on the cable the line is on", started != null && started[2] == z0 + 10
                    && mods.eln.node.voxel.ToolbagItem.selected(bag) != null, Arrays.toString(started) + " then " + mods.eln.node.voxel.ToolbagItem.selected(bag));
                c.check("item clicks: another kind chosen mid-route starts again from the same cable (the kept click's own eye)", again != null
                    && Arrays.equals(again, started) && mods.eln.node.voxel.ToolbagItem.selected(bag).tier() == LV, Arrays.toString(again));
            } finally {
                ToolbagCases.clean(pl);
                pl.snapTo(px, py, pz, 0, 0);
            }

            // 3. a pipe crossing over a floor cable, seen at 30 degrees along the cable: the ray stops on the pipe's
            // outline, the line passes over the pipe and comes down on the cable 6 voxels on. The kit branches off the
            // cable there, not at the cable's lattice point nearest the hit (under the pipe).
            double[] over = {6.9, 4.6, 8}, into = {13.5, 4.6 - 6.6 * Math.tan(Math.toRadians(30)), 8}, from = new double[3];
            for (int a = 0; a < 3; a++) from[a] = into[a] + 5 * (over[a] - into[a]);
            Vec3 hit2 = hit(c, crossed, from, into), eye2 = world(crossed, from);
            VoxelNode v2 = c.voxelByEntity(crossed);
            var onOutline = v2.aimed(null, hit2);
            long[][] join2 = new long[1][];
            CablePlacer.endAtForTests(level, crossed, net.minecraft.core.Direction.UP, hit2, eye2, true, join2, LV);
            long cx = crossed.getX() * 16L;
            c.check("item clicks: a kit seen low over a crossing pipe onto the cable branches off the cable where the line meets it",
                onOutline.part() >= 0 && v2.parts.get(onOutline.part()) instanceof PipePart && join2[0] != null && join2[0][0] >= cx + 12
                    && join2[0][2] == crossed.getZ() * 16L + 8,
                "outline hit on " + say(v2, onOutline) + " at " + hit2.subtract(Vec3.atLowerCornerOf(crossed)).scale(16) + ", join " + Arrays.toString(join2[0]) + " (block at " + cx + ")");
            long[] pipeJoin = PipePlacer.joinPointForTests(level, crossed, hit2, eye2);
            c.check("item clicks: a pipe kit on that line doesn't join the pipe (the line is on the cable)", pipeJoin == null, Arrays.toString(pipeJoin));

            // 4. a valve onto a pipe seen over a thick cable: the ray stops on the cable's outline, the line goes on into
            // the pipe. By the line the valve fits on the pipe; by the point it is refused (a cable)
            double[] in3 = {8, 4.8, 8}, from3 = {8, 4.8 + 0.1 * 38, -30};
            Vec3 hit3 = hit(c, piped, from3, in3), eye3 = world(piped, from3);
            VoxelNode v3 = c.voxelByEntity(piped);
            var on3 = v3.aimed(null, hit3);
            int before = v3.parts.size();
            String byPoint = v3.addValveAimed(null, hit3, ValvePart.ON_OFF);
            boolean refused = byPoint != null && v3.parts.size() == before;
            String byLine = v3.addValveAimed(eye3, hit3, ValvePart.ON_OFF);
            ValvePart valve = null;
            for (VoxelPart q : v3.parts) if (q instanceof ValvePart x) valve = x;
            c.check("item clicks: a valve seen over a cable onto the pipe behind it fits on the pipe, where the line meets it",
                v3.parts.get(on3.part()) instanceof CablePart && refused && byLine == null && valve != null && valve.x == 8 && valve.y == 4 && valve.z == 8,
                "outline hit on " + say(v3, on3) + "; by the point: " + byPoint + "; by the line: " + byLine + (valve == null ? "" : " at " + valve.x + "," + valve.y + "," + valve.z));
        }
    }

    /** Jade's server data for a block of two cables and a relay: an entry each, and the entry chosen for each. */
    static final class JadeEntries implements TestCase {
        BlockPos at, ribbon;
        SixNodeElement relay;

        public String id() {
            return "voxel_pick_jade_entry_per_part";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 5;
        }

        public void build(TestContext c) {
            at = c.at(1, 1, 1);
            relay = relayBesideCable(c, at);
            CableCases.voxel(c, at, List.of(new CablePart(MV, 0, new int[]{0, 0, 6, 16, 0, 6})));
            ribbon = c.at(3, 1, 1);
            int signal = CablePart.tierOf("Signal Cable");
            for (int z = 2; z <= 14; z += 3) CableCases.voxel(c, ribbon, List.of(new CablePart(signal, 0, new int[]{0, 0, z, 16, 0, z})));
        }

        public void measure(TestContext c) {
            VoxelNode v = c.voxelByEntity(at);
            var sent = VoxelProbe.entries(v, null, null, 3, "20 C");
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
            // the one under the asker's crosshair goes first, and nothing is sent twice
            int[] b1 = v.parts.get(1).boxes().get(0);
            Vec3 over = world(at, new double[]{2, 40, (b1[2] + b1[5]) / 2.0}), on = world(at, new double[]{2, b1[4] + 2, (b1[2] + b1[5]) / 2.0});
            var first = VoxelProbe.entries(v, over, on, 3, "20 C");
            c.check("jade entries: the asker's own goes first, once", first.size() == sent.size() && first.getCompoundOrEmpty(0).getIntOr("id", -9) == v.parts.get(1).id,
                "" + first);
            // a part laid since the server answered has no entry: the block's lines as a whole, not another part's
            var stale = VoxelProbe.choose(sent, new VoxelNode.Aimed(-1, 0), List.<VoxelPart>of(new CablePart(LV, 0, new int[]{0, 0, 14, 16, 0, 14})));
            c.check("jade entries: a part the data doesn't hold gets the block's lines as a whole", stale != null && stale.equals(none), "" + stale);
            wrong = "";
            // five cables side by side in the next block, 3 entries allowed: the one aimed at and the one on each side
            VoxelNode w = c.voxelByEntity(ribbon);
            var capped = VoxelProbe.entries(w, world(ribbon, new double[]{8, 40, 8.5}), world(ribbon, new double[]{8, 3, 8.5}), 3, "20 C", 3);
            java.util.Set<Integer> got = new java.util.TreeSet<>();
            for (int i = 0; i < capped.size(); i++) capped.getCompoundOrEmpty(i).getIntArray("box").ifPresent(b -> got.add(b[2]));
            c.check("jade entries: past the cap, the one aimed at and its neighbours are sent", capped.size() == 4 && got.equals(java.util.Set.of(5, 8, 11))
                && capped.getCompoundOrEmpty(0).getIntArray("box").orElse(new int[6])[2] == 8, got + " of " + capped.size());
            for (int i = 0; i < w.parts.size(); i++) {
                var t = VoxelProbe.choose(capped, new VoxelNode.Aimed(-1, i), w.parts);
                int z = w.parts.get(i).boxes().get(0)[2];
                if (t == null || got.contains(z) != t.contains("id")) wrong += "capped z " + z + ": " + t + "; ";
            }
            c.check("jade entries: past the cap, one that wasn't sent gets the block's lines as a whole", wrong.isEmpty(), wrong);
        }
    }
}
