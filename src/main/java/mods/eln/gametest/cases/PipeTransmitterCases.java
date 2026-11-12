package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipePlacer;
import mods.eln.node.voxel.PipeRouter;
import mods.eln.node.voxel.PipeSensorItem;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import mods.eln.sixnode.pipetransmitter.PipeTransmitterElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The pipe sensors as devices on a face (Baughn 2026-11-02): placed by the pipe sensor items on a floor, wall or
 * ceiling, pipe laid from the device's port to the run to measure, 0-SVU out on its signal pin.
 */
public final class PipeTransmitterCases {
    private PipeTransmitterCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FloorPressure());
        cases.add(new WallTemperature());
        cases.add(new BreakAndRefuse());
        cases.add(new JoinsARun());
        cases.add(new LabFixes());
    }

    static VoxelNode node(TestContext c, BlockPos p) {
        return PipeCases.node(c, p);
    }

    static net.minecraft.world.entity.player.Player player(TestContext c) {
        return VoxelDeviceCases.player(c);
    }

    /** The pipe sensor device in the block at p (the first), or null. */
    static PipeTransmitterElement device(TestContext c, BlockPos p) {
        VoxelNode v = node(c, p);
        if (v == null) return null;
        for (var e : v.sideElementList) if (e instanceof PipeTransmitterElement t) return t;
        return null;
    }

    /** Fill every pipe in these blocks with steam at about 6 bar and 150 C (as PipeCases.Sensors). */
    static void steam(TestContext c, BlockPos... blocks) {
        for (BlockPos p : blocks)
            for (VoxelPart q : node(c, p).parts)
                if (q instanceof PipePart || q instanceof mods.eln.node.voxel.JunctionPart) {
                    // (a junction too: a cold fitting on a line of hot steam condenses it all, as cold iron would)
                    var f = q instanceof PipePart pipe ? pipe.fluid : ((mods.eln.node.voxel.JunctionPart) q).fluid;
                    f.fill(0, 1, 423.15);
                    mods.eln.sim.fluid.EaUnits.addWater(f, 6e5 * f.volume / (461.5 * 423.15), 423.15, true);
                    mods.eln.fluid.FluidWorld.changed(node(c, p));
                }
    }

    /** The world direction its signal pin points. */
    static Direction out(PipeTransmitterElement t) {
        return t.side.applyLRDU(t.front);
    }

    /** The voltage on the cable just past the tip of t's pin (laid by VoxelDeviceCases.cableOut), NaN if none. */
    static double cableU(VoxelNode v, PipeTransmitterElement t) {
        Direction d = out(t);
        int[] q = VoxelDeviceCases.tip(v, t, d);
        if (q == null) return Double.NaN;
        int a = d.isY() ? 1 : d == Direction.XN || d == Direction.XP ? 0 : 2;
        if (!(d == Direction.XP || d == Direction.YP || d == Direction.ZP)) q[a] -= 1;
        Object[] k = v.cableAt(q[0], q[1], q[2]);
        return k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
    }

    /** The cable face a device's signal cable lies on: the device's own face (3D value). */
    static int cableFace(PipeTransmitterElement t) {
        return t.side.toEnumFacing().get3DDataValue();
    }

    /**
     * A pressure sensor clicked onto the middle of a floor, a pipe kit's route laid from its port (aimed at the device) to
     * the open end of a two-block run, the run full of steam: it reads the run's absolute pressure, and its signal cable
     * carries reading / 25 bar x SVU. An empty-hand right-click steps the full scale to 50 bar, which a save keeps.
     */
    static final class FloorPressure implements TestCase {
        BlockPos a, b, c2;
        String err, route;
        PipeRouter.End port;

        public String id() {
            return "pipe_transmitter_floor_pressure";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext c) {
            a = c.at(1, 1, 1);
            b = c.at(2, 1, 1);
            c2 = c.at(3, 1, 1);
            PipeCases.lay(c, b, 0, 0, 4, 8, 16, 4, 8);
            PipeCases.lay(c, c2, 0, 0, 4, 8, 16, 4, 8);
            err = PipeSensorItem.place(c.level(), a.below(), net.minecraft.core.Direction.UP, new Vec3(a.getX() + 0.5, a.getY(), a.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            if (err != null) return;
            var t = device(c, a);
            VoxelDeviceCases.cableOut(c, node(c, a), a, t, out(t), VoxelDeviceCases.SIGNAL, cableFace(t));
            port = PipePlacer.devicePortForTests(c.level(), a, new Vec3(a.getX() + (8 + t.ox) / 16.0, a.getY() + 1 / 16.0, a.getZ() + (8 + t.oz) / 16.0));
            var end = PipePlacer.portAtForTests(c.level(), b, net.minecraft.core.Direction.WEST);
            route = port == null || end == null ? "no port: device " + port + ", run " + end : PipePlacer.routeForTests(c.level(), 0, port, end);
            steam(c, a, b, c2);
        }

        public void measure(TestContext c) {
            c.check("pipe transmitter: a pressure sensor goes on the middle of a floor", err == null, "" + err);
            if (err != null) return;
            var t = device(c, a);
            int[] p = t.port();
            c.check("pipe transmitter: its port is 4 voxels over its centre, on the routers' lattice",
                t.side == Direction.YN && p[1] == 4 && p[0] % 4 == 0 && p[2] % 4 == 0 && port != null, java.util.Arrays.toString(p) + " " + port);
            c.check("pipe transmitter: a pipe route ends at its port", route == null, "" + route);
            var run = mods.eln.fluid.FluidWorld.nodeFor(((PipePart) node(c, b).parts.get(0)).fluid);
            double u = cableU(node(c, a), t);
            c.line(String.format("pipe transmitter: run %.2f bar; sensor %.2f bar, connected %b, out %.2f V, cable %.2f V; %s",
                run.P() / 1e5, t.reading, t.connected, t.outputGate.getU(), u, t.getWaila()));
            c.check("pipe transmitter: connected through the routed pipe", t.connected && t.pipe() != null, "");
            c.check("pipe transmitter: reads the run's absolute pressure", Math.abs(t.reading - run.P() / 1e5) < 0.05 && t.reading > 2, String.format("%.2f", t.reading));
            Wp9cCases.within(c, "pipe transmitter: its signal cable carries reading / 25 bar x SVU", u, t.reading / 25 * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
            c.check("pipe transmitter: Jade gives mode, reading, full scale and volts", t.getWaila().containsKey("Pressure sensor")
                && t.getWaila().containsKey("Full scale") && t.getWaila().containsKey("Output"), "" + t.getWaila());
            // an empty-hand right-click steps the full scale; it survives the element's NBT
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            node(c, a).onSlotActivated(fp, t.slot, (8 + t.ox) / 16f, 1 / 16f, (8 + t.oz) / 16f);
            var tag = new net.minecraft.nbt.CompoundTag();
            t.writeToNBT(tag);
            double stepped = t.scale;
            t.scale = 1;
            t.readFromNBT(tag);
            c.check("pipe transmitter: a right-click steps 25 -> 50 bar, and a save keeps it", stepped == 50 && t.scale == 50, stepped + " / " + t.scale);
        }
    }

    /**
     * A temperature sensor on a wall (its signal pin down the wall), pipe laid from its port along the wall into the next
     * block, both full of steam at 150 C: it reads the run's temperature, its cable carries reading / 200 C x SVU. One on a
     * ceiling goes there too, its port 4 voxels under the ceiling, and reads "not connected" without a pipe.
     */
    static final class WallTemperature implements TestCase {
        BlockPos p, q, ceil;
        String err, errCeil;

        public String id() {
            return "pipe_transmitter_wall_temperature";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext c) {
            p = c.at(1, 1, 1);
            q = c.at(2, 1, 1);
            ceil = c.at(3, 1, 1);
            c.floor(p.north());
            c.floor(ceil.above());
            err = PipeSensorItem.place(c.level(), p.north(), net.minecraft.core.Direction.SOUTH, new Vec3(p.getX() + 0.5, p.getY() + 0.5, p.getZ()),
                PipeSensorPart.TEMPERATURE, player(c));
            errCeil = PipeSensorItem.place(c.level(), ceil.above(), net.minecraft.core.Direction.DOWN, new Vec3(ceil.getX() + 0.5, ceil.getY() + 1, ceil.getZ() + 0.5),
                PipeSensorPart.QUALITY, player(c));
            if (err != null) return;
            var t = device(c, p);
            VoxelDeviceCases.cableOut(c, node(c, p), p, t, out(t), VoxelDeviceCases.SIGNAL, cableFace(t));
            int[] o = t.port();
            String e = node(c, p).addPart(new PipePart(0, new int[]{o[0], o[1], o[2], 16, o[1], o[2]}));
            if (e != null) err = "pipe: " + e;
            PipeCases.lay(c, q, 0, 0, o[1], o[2], 16, o[1], o[2]);
            steam(c, p, q);
        }

        public void measure(TestContext c) {
            c.check("pipe transmitter: a temperature sensor goes on a wall, with a pipe to its port", err == null, "" + err);
            c.check("pipe transmitter: one goes on a ceiling", errCeil == null && device(c, ceil) != null && device(c, ceil).side == Direction.YP
                && device(c, ceil).port()[1] == 12, errCeil + " " + (device(c, ceil) == null ? "" : java.util.Arrays.toString(device(c, ceil).port())));
            if (device(c, ceil) != null)
                c.check("pipe transmitter: without a pipe it says so and gives 0 V", !device(c, ceil).connected
                    && "not connected to a pipe".equals(device(c, ceil).getWaila().get("Steam quality sensor")) && Math.abs(device(c, ceil).outputGate.getU()) < 0.1, "" + device(c, ceil).getWaila());
            if (err != null) return;
            var t = device(c, p);
            var run = mods.eln.fluid.FluidWorld.nodeFor(((PipePart) node(c, q).parts.get(0)).fluid);
            double u = cableU(node(c, p), t);
            c.line(String.format("pipe transmitter (wall): side %s front %s; run %.0f C; sensor %.0f C, out %.2f V, cable %.2f V", t.side, t.front,
                run.T() - 273.15, t.reading, t.outputGate.getU(), u));
            c.check("pipe transmitter: its signal pin points down the wall", out(t) == Direction.YN, "" + out(t));
            c.check("pipe transmitter: reads the run's temperature", Math.abs(t.reading - (run.T() - 273.15)) < 1 && t.reading > 50, String.format("%.0f", t.reading));
            Wp9cCases.within(c, "pipe transmitter: its signal cable carries reading / 200 C x SVU", u, Math.min(1, t.reading / 200) * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
        }
    }

    /**
     * Breaking the device drops its pipe sensor item and leaves the pipe laid to it (the player's pipe, as laid). Refused:
     * in mid-air, and on a pipe (Baughn 2026-11-02: the item no longer fits a sensor on a pipe; it goes on a face).
     */
    static final class BreakAndRefuse implements TestCase {
        BlockPos a, run, air;
        String err, midAir, onPipe;
        boolean broke;
        int partsBefore;

        public String id() {
            return "pipe_transmitter_break_and_refuse";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 10;
        }

        public void build(TestContext c) {
            a = c.at(1, 1, 1);
            run = c.at(3, 1, 1);
            air = c.at(1, 3, 1);
            err = PipeSensorItem.place(c.level(), a.below(), net.minecraft.core.Direction.UP, new Vec3(a.getX() + 0.3, a.getY(), a.getZ() + 0.3),
                PipeSensorPart.PRESSURE, player(c));
            if (err == null) {
                int[] o = device(c, a).port();
                String e = node(c, a).addPart(new PipePart(0, new int[]{o[0], o[1], o[2], 16, o[1], o[2]}));
                if (e != null) err = "pipe: " + e;
            }
            midAir = PipeSensorItem.place(c.level(), air, net.minecraft.core.Direction.UP, new Vec3(air.getX() + 0.5, air.getY() + 1, air.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            PipeCases.lay(c, run, 0, 0, 8, 8, 16, 8, 8);
            onPipe = PipeSensorItem.place(c.level(), run, net.minecraft.core.Direction.UP, new Vec3(run.getX() + 0.5, run.getY() + 9 / 16.0, run.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
        }

        public void measure(TestContext c) {
            c.check("pipe transmitter: placed off centre on a lattice point near the click", err == null && device(c, a) != null
                && device(c, a).ox % 4 == 0 && device(c, a).oz % 4 == 0 && device(c, a).ox + device(c, a).oz < 0, err + (device(c, a) == null ? "" : " at " + device(c, a).ox + "," + device(c, a).oz));
            c.check("pipe transmitter: refused in mid-air", midAir != null && node(c, air.above()) == null, "" + midAir);
            c.check("pipe transmitter: refused on a pipe (no sensor fitted on it)", onPipe != null && node(c, run).parts.size() == 1, "" + onPipe);
            if (err != null) return;
            var t = device(c, a);
            c.check("pipe transmitter: it drops its pipe sensor item", t.getDropItemStack().is(mods.eln.registry.ElnRegistry.PIPE_PRESSURE_SENSOR.get()), "" + t.getDropItemStack());
            var v = node(c, a);
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            broke = v.playerAskToBreakSlot(fp, t.slot);
            var drops = c.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(a).inflate(1),
                ie -> ie.getItem().is(mods.eln.registry.ElnRegistry.PIPE_PRESSURE_SENSOR.get()));
            c.check("pipe transmitter: broken, it drops the sensor and the pipe laid to it stays", broke && device(c, a) == null && node(c, a) != null
                && node(c, a).parts.size() == 1 && node(c, a).parts.get(0) instanceof PipePart && !drops.isEmpty(),
                broke + " " + (node(c, a) == null ? "no block" : node(c, a).parts.size() + " parts") + ", drops " + drops.size());
        }
    }

    /**
     * The flow a player tries first: a pipe kit route from the device's port (the device clicked) that finishes by
     * joining a run mid-way. The run is cut there into a junction, and the device reads the run's pressure.
     */
    static final class JoinsARun implements TestCase {
        BlockPos a, run;
        String err;

        public String id() {
            return "pipe_transmitter_joins_a_run";
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext c) {
            a = c.at(1, 1, 1);
            run = c.at(3, 1, 1);
            PipeCases.lay(c, run, 0, 0, 8, 8, 16, 8, 8);
            err = PipeSensorItem.place(c.level(), a.below(), net.minecraft.core.Direction.UP, new Vec3(a.getX() + 0.5, a.getY(), a.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            if (err != null) return;
            var t = device(c, a);
            var port = PipePlacer.devicePortForTests(c.level(), a, new Vec3(a.getX() + (8 + t.ox) / 16.0, a.getY() + 1 / 16.0, a.getZ() + (8 + t.oz) / 16.0));
            long[] join = {run.getX() * 16L + 8, run.getY() * 16L + 8, run.getZ() * 16L + 8};
            err = port == null ? "no port" : PipePlacer.routeAndJoinForTests(c.level(), 0, port, join);
            if (err == null) steam(c, a, a.east(), run);
        }

        public void measure(TestContext c) {
            c.check("pipe transmitter: a route from its port joins a run mid-way", err == null, "" + err);
            if (err != null) return;
            var t = device(c, a);
            long pipes = node(c, run).parts.stream().filter(q -> q instanceof PipePart).count();
            var n = mods.eln.fluid.FluidWorld.nodeFor(((PipePart) node(c, run).parts.get(0)).fluid);
            c.line(String.format("pipe transmitter (joined): run %.2f bar, %d pieces in its block; sensor %.2f bar, connected %b", n.P() / 1e5, pipes, t.reading, t.connected));
            c.check("pipe transmitter: the run is cut into a junction", pipes == 3, "" + pipes);
            c.check("pipe transmitter: reads the joined run's pressure", t.connected && Math.abs(t.reading - n.P() / 1e5) < 0.05 && t.reading > 2, String.format("%.2f", t.reading));
        }
    }

    /**
     * The first client check's findings: clicked on the floor under a pipe it goes beside the pipe or is refused, never
     * with its port in the pipe; a cell full of pipes refuses it; the crosshair on the pipe end over a piped device means
     * the device, whose Jade headline is reading, volts and full scale; a second pipe kit route to a port that has a pipe
     * is refused; its own items are out of the creative tab.
     */
    static final class LabFixes implements TestCase {
        BlockPos under, crowded, a, r;
        String errUnder, errCrowded, errRoute;
        int piecesBefore, piecesAfter;
        net.minecraft.network.chat.Component lastMessage;

        public String id() {
            return "pipe_transmitter_lab_fixes";
        }

        public int width() {
            return 7;
        }

        public int ticks() {
            return 20;
        }

        public void build(TestContext c) {
            under = c.at(1, 1, 1);
            crowded = c.at(5, 1, 0);
            a = c.at(1, 1, 2);
            r = c.at(3, 1, 2);
            PipeCases.lay(c, under, 0, 0, 4, 8, 16, 4, 8);
            errUnder = PipeSensorItem.place(c.level(), under.below(), net.minecraft.core.Direction.UP, new Vec3(under.getX() + 0.5, under.getY(), under.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            for (int z = 4; z <= 12; z += 4) PipeCases.lay(c, crowded, 0, 0, 4, z, 16, 4, z);
            errCrowded = PipeSensorItem.place(c.level(), crowded.below(), net.minecraft.core.Direction.UP, new Vec3(crowded.getX() + 0.5, crowded.getY(), crowded.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            // a pipe kit, as a player: from the run's open end to the device, twice
            PipeCases.lay(c, r, 0, 0, 4, 8, 16, 4, 8);
            errRoute = PipeSensorItem.place(c.level(), a.below(), net.minecraft.core.Direction.UP, new Vec3(a.getX() + 0.5, a.getY(), a.getZ() + 0.5),
                PipeSensorPart.PRESSURE, player(c));
            if (errRoute != null) return;
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
            PipePlacer.forget(fp.getUUID());
            var kit = mods.eln.node.voxel.PipeKitItem.kit(0, 16 * 16);
            var t = device(c, a);
            Vec3 body = new Vec3(a.getX() + (8 + t.ox) / 16.0, a.getY() + 1 / 16.0, a.getZ() + (8 + t.oz) / 16.0);
            var sl = c.level();
            PipePlacer.use(fp, sl, r, net.minecraft.core.Direction.WEST, kit, 0, new Vec3(r.getX(), r.getY() + 0.25, r.getZ() + 0.5));
            PipePlacer.use(fp, sl, a, net.minecraft.core.Direction.UP, kit, 0, body);
            if (t.pipe() == null) errRoute = "the first route didn't reach the port";
            piecesBefore = (int) node(c, a).parts.stream().filter(q -> q instanceof PipePart).count();
            PipePlacer.use(fp, sl, r, net.minecraft.core.Direction.EAST, kit, 0, new Vec3(r.getX() + 1, r.getY() + 0.25, r.getZ() + 0.5));
            boolean handled = PipePlacer.use(fp, sl, a, net.minecraft.core.Direction.UP, kit, 0, body);
            piecesAfter = (int) node(c, a).parts.stream().filter(q -> q instanceof PipePart).count();
            if (!handled) errRoute = "the second click on the device wasn't taken";
            PipePlacer.forget(fp.getUUID());
        }

        public void measure(TestContext c) {
            var tu = device(c, under);
            c.check("pipe transmitter: clicked under a pipe, it goes beside it or is refused, its port never in the pipe",
                errUnder != null || tu != null && tu.pipe() == null, errUnder + (tu == null ? "" : " at " + tu.ox + "," + tu.oz + " port " + java.util.Arrays.toString(tu.port())));
            c.check("pipe transmitter: refused in a cell full of pipes", errCrowded != null && errCrowded.startsWith("Too close to a pipe") && device(c, crowded) == null, "" + errCrowded);
            c.check("pipe transmitter: a route laid to its port by a pipe kit", errRoute == null && piecesBefore >= 1, errRoute + " " + piecesBefore);
            c.check("pipe transmitter: a second route to a port that has a pipe is refused", piecesAfter == piecesBefore, piecesBefore + " -> " + piecesAfter);
            if (errRoute != null) return;
            var t = device(c, a);
            var v = node(c, a);
            int[] p = t.port();
            // on the pipe's end over the device, and just over its body
            var onPipe = v.aimed((p[0] + 0.5) / 16.0, (p[1] + 1.2) / 16.0, (p[2] + 0.5) / 16.0);
            var onBody = v.aimed((p[0] + 1.5) / 16.0, 2.2 / 16.0, (p[2] - 1.5) / 16.0);
            c.check("pipe transmitter: the crosshair on the pipe end over it means the device", onPipe.element() == t.slot && onBody.element() == t.slot,
                onPipe + " " + onBody + " slot " + t.slot);
            var keys = new java.util.ArrayList<>(t.getWaila().keySet());
            c.check("pipe transmitter: Jade's first three lines are reading, volts and full scale", keys.size() == 3 && keys.get(0).equals("Pressure sensor")
                && keys.get(1).equals("Output") && keys.get(2).equals("Full scale"), "" + t.getWaila());
            // its own items stay out of the creative tab
            var tab = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getValue(net.minecraft.resources.Identifier.parse("eln:eln"));
            boolean listed = false;
            if (tab != null) {
                tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(c.level().enabledFeatures(), false, c.level().registryAccess()));
                for (int s = 0; s < 3; s++) {
                    var item = PipeSensorItem.device(s).parentItem;
                    for (var st : tab.getDisplayItems()) listed |= st.is(item);
                }
            }
            c.check("pipe transmitter: its own items are not in the creative tab", tab != null && !listed, tab == null ? "no tab" : "listed " + listed);
        }
    }
}
