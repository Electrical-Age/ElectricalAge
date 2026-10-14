package mods.eln.gametest;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lab-only scene builders for the render port (P3 R2), op level 2:
 * - `/elnlab gallery x y z`: every device, unpowered, laid out like the 1.12 gallery (selftest GalleryCase, same
 *   spacing and order), cameras written to eln-gallery-26.json in the server directory;
 * - `/elnlab burn r x y z`: burn out every device within r blocks (overvoltage), to look at burnt renders;
 * - `/elnlab cables n x y z`: n low-voltage cables in connected rows of 50, for frame-time measurement;
 * - `/elnlab machines n x y z`: n 50V macerators, 2 apart.
 */
public final class LabCommands {
    private LabCommands() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> e.getDispatcher().register(
            Commands.literal("elnlab").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("gallery").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> gallery(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("burn").then(Commands.argument("r", IntegerArgumentType.integer(1, 64))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> burn(c.getSource(), IntegerArgumentType.getInteger(c, "r"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))
                .then(Commands.literal("cables").then(Commands.argument("n", IntegerArgumentType.integer(1, 5000))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> cables(c.getSource(), IntegerArgumentType.getInteger(c, "n"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))
                .then(Commands.literal("oil").executes(c -> {
                    var data = mods.eln.oil.ReservoirData.get(c.getSource().getLevel());
                    c.getSource().sendSuccess(() -> Component.literal(data.all().size() + " oil lake(s); " + mods.eln.oil.Reservoirs.debug()), false);
                    for (var r : data.all()) {
                        String line = String.format("  #%d: %s, %d / %d mB (%.0f%%), surface y %.3f", r.id(), r.describeBox(), r.amount(), r.capacity(),
                            100.0 * r.amount() / Math.max(1, r.capacity()), r.surface() / 8.0);
                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return data.all().size();
                }))
                .then(Commands.literal("netdump").executes(c -> netdump(c.getSource(), null, 0))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> netdump(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), 8))
                        .then(Commands.argument("r", IntegerArgumentType.integer(0, 512))
                            .executes(c -> netdump(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "r"))))))
                .then(Commands.literal("inspect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> inspect(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("machines").then(Commands.argument("n", IntegerArgumentType.integer(1, 500))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> machines(c.getSource(), IntegerArgumentType.getInteger(c, "n"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))));
    }

    /** The solved network to eln-netdump/&lt;stamp&gt;.json and .falstad.txt in the server directory (NetDump). */
    static int netdump(CommandSourceStack src, BlockPos pos, int r) {
        try {
            long t0 = System.nanoTime();
            var file = NetDump.dump(java.nio.file.Path.of("eln-netdump"), pos == null ? null : new mods.eln.misc.Coordonate(pos, src.getLevel()), r);
            String msg = "netdump: " + file + " (" + (System.nanoTime() - t0) / 1_000_000 + " ms)";
            mods.eln.Eln.LOG.info(msg);
            src.sendSuccess(() -> Component.literal(msg), false);
            return 1;
        } catch (java.io.IOException | RuntimeException e) {
            src.sendFailure(Component.literal("netdump failed: " + e));
            return 0;
        }
    }

    /** What a node holds, for debugging by hand: each element's WAILA lines, plus lamp and lamp supply state. */
    static int inspect(CommandSourceStack src, BlockPos pos) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        List<String> out = new ArrayList<>();
        if (node == null) out.add("no EA node at " + pos);
        else {
            out.add(node.getClass().getSimpleName() + " at " + pos + ", in simulation " + node.isAdded() + ", light " + (node instanceof mods.eln.node.Node n ? n.getLightValue() : -1));
            if (node instanceof mods.eln.node.transparent.TransparentNode tn && tn.element != null) {
                out.add("- " + tn.element.getClass().getSimpleName() + " " + new TreeMap<>(tn.element.getWaila()));
                if (tn.getFluidHandler() instanceof mods.eln.fluid.ElementFluidHandler h)
                    out.add("  tank: " + h.getFluidAmount() + " / " + h.getCapacity() + " mB of " + h.getFluid().getFluid());
            }
            if (node instanceof mods.eln.node.six.SixNode six)
                for (var el : six.sideElementList) {
                    if (el == null) continue;
                    out.add("- " + el.side + ": " + el.getClass().getSimpleName() + " " + new TreeMap<>(el.getWaila()));
                    try {
                        if (el instanceof mods.eln.sixnode.lampsocket.LampSocketElement l) {
                            Object p = field(l, "lampProcess");
                            out.add("  lamp: light " + field(p, "light") + " at " + field(p, "lbCoord") + ", poweredByLampSupply "
                                + field(l, "poweredByLampSupply") + ", channel '" + l.channel + "', connected " + field(l, "isConnectedToLampSupply")
                                + ", U " + l.positiveLoad.getU() + ", bulb " + l.getInventory().getItem(0) + ", cable " + l.getInventory().getItem(1));
                        }
                        if (el instanceof mods.eln.sixnode.lampsupply.LampSupplyElement s) {
                            List<String> es = new ArrayList<>();
                            for (Object e : (List<?>) field(s, "entries"))
                                es.add("power '" + field(e, "powerChannel") + "' wireless '" + field(e, "wirelessChannel") + "'");
                            out.add("  supply: " + es + ", states " + java.util.Arrays.toString((boolean[]) field(s, "channelStates"))
                                + ", range " + s.getRange() + ", U " + s.powerLoad.getU() + ", I " + s.powerLoad.getI());
                        }
                    } catch (RuntimeException e) {
                        out.add("  (" + e + ")");
                    }
                }
        }
        for (String line : out) {
            mods.eln.Eln.LOG.info("elnlab inspect: {}", line);
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return out.size();
    }

    static Object field(Object o, String name) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                var f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                // superclass
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("no field " + name + " in " + o.getClass());
    }

    public static int sixId(String name) {
        for (var e : Eln.sixNodeItem.all().entrySet()) if (e.getValue().name.equals(name)) return e.getKey();
        throw new IllegalStateException("no six-node device " + name);
    }

    public static int transparentId(String name) {
        for (var e : Eln.transparentNodeItem.all().entrySet()) if (e.getValue().name.equals(name)) return e.getKey();
        throw new IllegalStateException("no transparent device " + name);
    }

    static int burn(CommandSourceStack src, int r, BlockPos at) {
        int n = 0;
        for (mods.eln.node.NodeBase node : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (node.coordonate.world() != src.getLevel() || !node.coordonate.pos().closerThan(at, r + 0.5)) continue;
            if (node instanceof mods.eln.node.six.SixNode six) {
                for (Direction d : Direction.values()) {
                    var e = six.getElement(d);
                    if (e != null) {
                        e.burn(mods.eln.sim.process.destruct.FailureCause.OVERVOLTAGE, mods.eln.sim.process.destruct.Failure.Kind.ELECTRONICS);
                        n++;
                    }
                }
            } else if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) {
                t.element.burn(mods.eln.sim.process.destruct.FailureCause.OVERVOLTAGE, mods.eln.sim.process.destruct.Failure.Kind.MACHINE);
                n++;
            }
        }
        int fn = n;
        src.sendSuccess(() -> Component.literal("burnt " + fn + " device(s)"), true);
        return n;
    }

    static int cables(CommandSourceStack src, int n, BlockPos origin) {
        WorldTestContext ctx = new WorldTestContext(src.getLevel(), origin);
        int id = sixId("Low Voltage Cable");
        for (int i = 0; i < n; i++) {
            BlockPos p = ctx.at(i % 50, 1, (i / 50) * 2);
            ctx.floor(p.below());
            try {
                ctx.placeSix(id, p, Direction.YN);
            } catch (RuntimeException ex) {
                ctx.check("cable " + i, false, ex.getMessage());
            }
        }
        src.sendSuccess(() -> Component.literal("placed " + n + " cables, " + ctx.failures + " failed"), true);
        return n;
    }

    static int machines(CommandSourceStack src, int n, BlockPos origin) {
        WorldTestContext ctx = new WorldTestContext(src.getLevel(), origin);
        int id = transparentId("50V Macerator");
        for (int i = 0; i < n; i++) {
            BlockPos p = ctx.at((i % 10) * 2, 1, (i / 10) * 2);
            ctx.floor(p.below());
            try {
                ctx.placeTransparent(id, p);
            } catch (RuntimeException ex) {
                ctx.check("macerator " + i, false, ex.getMessage());
            }
        }
        src.sendSuccess(() -> Component.literal("placed " + n + " macerators, " + ctx.failures + " failed"), true);
        return n;
    }

    record Device(boolean six, int damage, String name) {
    }

    /** 1.12 SmokeCases.allDevices: one per descriptor (aliases share one), six-node then transparent, by id. */
    static List<Device> allDevices() {
        List<Device> out = new ArrayList<>();
        Map<Object, Boolean> seen = new IdentityHashMap<>();
        for (Map.Entry<Integer, SixNodeDescriptor> e : new TreeMap<>(Eln.sixNodeItem.all()).entrySet())
            if (seen.put(e.getValue(), true) == null) out.add(new Device(true, e.getKey(), e.getValue().name));
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.all()).entrySet())
            if (seen.put(e.getValue(), true) == null) out.add(new Device(false, e.getKey(), e.getValue().name));
        return out;
    }

    static final int PER_ROW = 7;

    static JsonObject cam(String label, double x, double eyeY, double z, float yaw, float pitch) {
        JsonObject o = new JsonObject();
        o.addProperty("label", label);
        o.addProperty("x", x);
        o.addProperty("y", eyeY - 1.62);
        o.addProperty("z", z);
        o.addProperty("yaw", yaw);
        o.addProperty("pitch", pitch);
        return o;
    }

    /** 1.12 GalleryCase.build + measure, same layout and camera rules. */
    static int gallery(CommandSourceStack src, BlockPos origin) {
        ServerLevel level = src.getLevel();
        WorldTestContext ctx = new WorldTestContext(level, origin);
        List<Device> devices = allDevices();
        int sixCount = (int) devices.stream().filter(Device::six).count();
        int sixRows = sixCount / PER_ROW + 1;
        int i = 0, t = 0, placed = 0;
        JsonArray closeups = new JsonArray(), rows = new JsonArray();
        Map<Integer, double[]> rowSpan = new TreeMap<>();
        for (Device d : devices) {
            BlockPos p;
            if (d.six) p = ctx.at(2 + (i % PER_ROW) * 3, 1, 2 + (i / PER_ROW) * 6);
            else p = ctx.at(4 + (t % PER_ROW) * 8, 1, 2 + sixRows * 6 + 6 + (t / PER_ROW) * 12);
            if (d.six) i++;
            else t++;
            BlockPos at;
            try {
                if (d.six) {
                    for (BlockPos f : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, -1, 1))) ctx.floor(f.immutable());
                    Direction[] faces = Eln.sixNodeItem.getDescriptor(d.damage).getPlaceDirection();
                    Direction side = faces == null || faces.length == 0 ? Direction.YN : faces[0];
                    ctx.floor(p.relative(side.toEnumFacing()));
                    ctx.placeSix(d.damage, p, side);
                    at = p;
                } else {
                    TransparentNodeDescriptor td = Eln.transparentNodeItem.getDescriptor(d.damage);
                    ctx.floor(p.below());
                    ctx.floor(p.offset(td.getSpawnDeltaX(), td.getSpawnDeltaY() - 1, td.getSpawnDeltaZ()));
                    TransparentNodeElement e;
                    try {
                        e = mods.eln.gametest.cases.Wp10bCases.placeFacing(ctx, d.damage, p, Direction.XN);
                    } catch (IllegalStateException noSuchFront) {
                        e = ctx.placeTransparent(d.damage, p);
                    }
                    at = new BlockPos(e.node.coordonate.x, e.node.coordonate.y, e.node.coordonate.z);
                }
            } catch (RuntimeException ex) {
                ctx.check("gallery placed " + d.name, false, String.valueOf(ex.getMessage()));
                continue;
            }
            placed++;
            double x = at.getX() + 0.5, y = at.getY(), z = at.getZ() + 0.5;
            String label = (d.six ? "six-" : "transparent-") + d.damage + " " + d.name;
            if (d.six) closeups.add(cam(label, x, y + 2.2, z - 1.8, 0f, 45f));
            else closeups.add(cam(label, x - 4.5, y + 2.5, z, -90f, 20f));
            int key = (d.six ? 0 : 100000) + at.getZ();
            double[] s = rowSpan.computeIfAbsent(key, k -> new double[]{1e9, -1e9, z, y, 0});
            s[0] = Math.min(s[0], x);
            s[1] = Math.max(s[1], x);
            s[4] = d.six ? 0 : 1;
        }
        int r = 0;
        for (double[] s : rowSpan.values()) {
            boolean big = s[4] > 0;
            double back = big ? 12 : 6, up = big ? 6 : 3.5;
            rows.add(cam("row " + (++r), (s[0] + s[1]) / 2, s[3] + up, s[2] - back, 0f, 30f));
        }
        JsonObject out = new JsonObject();
        out.add("rows", rows);
        out.add("closeups", closeups);
        try (Writer w = new FileWriter("eln-gallery-26.json")) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        } catch (IOException e) {
            ctx.check("gallery json written", false, e.toString());
        }
        int fp = placed;
        src.sendSuccess(() -> Component.literal("gallery: " + fp + "/" + devices.size() + " placed, " + rows.size()
            + " rows, cameras in eln-gallery-26.json"), true);
        return placed;
    }
}
