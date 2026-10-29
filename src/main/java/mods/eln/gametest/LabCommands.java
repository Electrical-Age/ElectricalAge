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
                .then(Commands.literal("devicevolts").then(Commands.argument("name", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .executes(c -> deviceVolts(c.getSource(), com.mojang.brigadier.arguments.StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("voxelcables")
                    .then(Commands.literal("convert").then(Commands.argument("r", IntegerArgumentType.integer(0, 100000))
                        .executes(c -> voxelCables(c.getSource(), "convert", IntegerArgumentType.getInteger(c, "r")))))
                    .then(Commands.literal("dry").then(Commands.argument("r", IntegerArgumentType.integer(0, 100000))
                        .executes(c -> voxelCables(c.getSource(), "dry", IntegerArgumentType.getInteger(c, "r")))))
                    .then(Commands.literal("revert").then(Commands.argument("r", IntegerArgumentType.integer(0, 100000))
                        .executes(c -> voxelCables(c.getSource(), "revert", IntegerArgumentType.getInteger(c, "r"))))))
                .then(Commands.literal("oil").executes(c -> {
                    var data = mods.eln.reservoir.ReservoirData.get(c.getSource().getLevel());
                    c.getSource().sendSuccess(() -> Component.literal(data.all().size() + " reservoir(s); " + mods.eln.reservoir.Reservoirs.debug()), false);
                    for (var r : data.all()) {
                        String line = "  " + r;
                        c.getSource().sendSuccess(() -> Component.literal(line), false);
                    }
                    return data.all().size();
                }))
                .then(Commands.literal("netdump").executes(c -> netdump(c.getSource(), null, 0))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> netdump(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), 8))
                        .then(Commands.argument("r", IntegerArgumentType.integer(0, 512))
                            .executes(c -> netdump(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "r"))))))
                .then(Commands.literal("remove").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> remove(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("reconnect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> {
                        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(BlockPosArgument.getLoadedBlockPos(c, "pos"), c.getSource().getLevel()));
                        if (n == null) return 0;
                        n.reconnect();
                        c.getSource().sendSuccess(() -> Component.literal("reconnected " + n.getClass().getSimpleName()), false);
                        return 1;
                    })))
                .then(Commands.literal("heat").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("celsius", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(-200, 3000))
                        .executes(c -> heat(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"),
                            com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "celsius"))))))
                .then(Commands.literal("inspect").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> inspect(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
                .then(Commands.literal("machines").then(Commands.argument("n", IntegerArgumentType.integer(1, 500))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> machines(c.getSource(), IntegerArgumentType.getInteger(c, "n"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))));
    }

    /**
     * /elnlab voxelcables convert|dry|revert r: six-node cables within r blocks (horizontally) to voxel cables, a report
     * of what would convert and why not, or back. The report goes to the chat and, in full, to eln-voxelcables.txt.
     */
    static int voxelCables(CommandSourceStack src, String mode, int r) {
        BlockPos at = BlockPos.containing(src.getPosition());
        var level = src.getLevel();
        Map<String, Integer> why = new TreeMap<>();
        int done = 0, knuckles = 0;
        StringBuilder log = new StringBuilder();
        for (mods.eln.node.NodeBase n : new ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.coordonate.world() != level) continue;
            if (r > 0 && (Math.abs(n.coordonate.x - at.getX()) > r || Math.abs(n.coordonate.z - at.getZ()) > r)) continue;
            if (mode.equals("revert")) {
                if (n instanceof mods.eln.node.voxel.VoxelNode v) {
                    if (v.legacy != null && mods.eln.node.voxel.CableConverter.revert(v) == null) done++;
                }
                continue;
            }
            if (!(n instanceof mods.eln.node.six.SixNode six) || n instanceof mods.eln.node.voxel.VoxelNode) continue;
            // (the lab: load what the plan looks at; on a live server conversion waits for chunks to load)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) level.getChunk((n.coordonate.x >> 4) + dx, (n.coordonate.z >> 4) + dz);
            String w = mode.equals("dry") ? mods.eln.node.voxel.CableConverter.plan(six).skip() : mods.eln.node.voxel.CableConverter.convert(six);
            if (w == null) done++;
            else {
                why.merge(w, 1, Integer::sum);
                log.append(n.coordonate.pos().toShortString()).append(": ").append(w).append('\n');
            }
        }
        if (mode.equals("revert"))
            for (mods.eln.node.NodeBase n : new ArrayList<>(mods.eln.node.NodeManager.instance.getNodes()))
                if (n instanceof mods.eln.node.voxel.VoxelNode v && n.coordonate.world() == level
                    && (r == 0 || Math.abs(n.coordonate.x - at.getX()) <= r && Math.abs(n.coordonate.z - at.getZ()) <= r))
                    knuckles += mods.eln.node.voxel.CableConverter.revertKnuckles(v);
        int fd = done, fk = knuckles;
        String verb = mode.equals("dry") ? "would convert" : mode.equals("revert") ? "reverted" : "converted";
        src.sendSuccess(() -> Component.literal(verb + " " + fd + " block(s)" + (mode.equals("revert") ? ", removed " + fk + " knuckle(s)" : "")), true);
        for (var e : why.entrySet()) src.sendSuccess(() -> Component.literal("  kept " + e.getValue() + ": " + e.getKey()), false);
        try (Writer w = new FileWriter("eln-voxelcables.txt")) {
            w.write(verb + " " + done + "\n" + log);
        } catch (IOException e) {
            src.sendFailure(Component.literal("report not written: " + e));
        }
        return done;
    }

    /**
     * /elnlab devicevolts NAME: every device's (not cable's) load voltages to eln-devicevolts-NAME.json, keyed by
     * position, element side and load index: compare before and after a cable conversion.
     */
    static int deviceVolts(CommandSourceStack src, String name) {
        JsonObject out = new JsonObject();
        int n = 0, errors = 0;
        String firstError = null;
        for (mods.eln.node.NodeBase node : new ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
          try {
            String at = node.coordonate.dimention + " " + node.coordonate.pos().toShortString();
            List<Object> elements = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            if (node instanceof mods.eln.node.six.SixNode six) {
                for (Direction d : Direction.values()) {
                    var e = six.getElement(d);
                    if (e != null && !(e instanceof mods.eln.sixnode.electricalcable.ElectricalCableElement)) {
                        elements.add(e);
                        keys.add(at + " " + d + " " + e.getClass().getSimpleName());
                    }
                }
            } else if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) {
                elements.add(t.element);
                keys.add(at + " " + t.element.getClass().getSimpleName());
            }
            for (int i = 0; i < elements.size(); i++) {
                Object e = elements.get(i);
                Object list;
                try {
                    list = field(e, "electricalLoadList");
                } catch (RuntimeException ex) {
                    continue;
                }
                JsonArray a = new JsonArray();
                for (Object l : (List<?>) list) a.add(((mods.eln.sim.mna.state.State) l).state);
                out.add(keys.get(i), a);
                n++;
            }
          } catch (RuntimeException ex) {
            errors++;
            if (firstError == null) firstError = node.getClass().getSimpleName() + ": " + ex;
          }
        }
        if (errors > 0) {
            String fe = firstError;
            int fn0 = errors;
            src.sendSuccess(() -> Component.literal("devicevolts: " + fn0 + " node(s) failed, first: " + fe), false);
        }
        try (Writer w = new FileWriter("eln-devicevolts-" + name + ".json")) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, w);
        } catch (IOException e) {
            src.sendFailure(Component.literal("not written: " + e));
            return 0;
        }
        int fn = n;
        src.sendSuccess(() -> Component.literal("devicevolts: " + fn + " device(s) to eln-devicevolts-" + name + ".json"), false);
        return n;
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
    /** Remove the node at pos from the simulation and the node list (no drops; its block, if any, stays). */
    static int remove(CommandSourceStack src, BlockPos pos) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        if (node == null) {
            src.sendFailure(Component.literal("no EA node at " + pos));
            return 0;
        }
        String what = node.getClass().getSimpleName();
        mods.eln.Eln.LOG.info("elnlab remove: {} at {} by {}", what, pos.toShortString(), src.getTextName());
        mods.eln.node.NodeOrphans.remove(node);
        src.sendSuccess(() -> Component.literal("removed " + what + " at " + pos.toShortString()), true);
        return 1;
    }

    /**
     * /elnlab heat pos celsius: hold every thermal load of the node at pos at that temperature (a heat capacity nothing
     * can move, until the node reloads). For testing hot things (Incandescence) without building a furnace for them.
     */
    static int heat(CommandSourceStack src, BlockPos pos, double celsius) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        java.util.List<mods.eln.sim.ThermalLoad> loads = new java.util.ArrayList<>();
        if (node instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) loads.addAll(t.element.thermalLoadList);
        if (node instanceof mods.eln.node.six.SixNode s6)
            for (mods.eln.misc.Direction d : mods.eln.misc.Direction.values()) {
                var e = s6.getElement(d);
                if (e != null) loads.addAll(e.thermalLoadList);
            }
        if (node instanceof mods.eln.node.voxel.VoxelNode v)
            for (int f = 0; f < 6; f++) {
                var t = v.cableThermal(f);
                if (t != null) loads.add(t);
            }
        if (loads.isEmpty()) {
            src.sendFailure(Component.literal("no thermal load at " + pos.toShortString()));
            return 0;
        }
        for (var t : loads) {
            t.Tc = celsius - mods.eln.sim.PhysicalConstant.Tamb;
            t.C = 1e15;
        }
        int n = loads.size();
        src.sendSuccess(() -> Component.literal("holding " + n + " thermal load(s) at " + celsius + " C at " + pos.toShortString()), true);
        return n;
    }

    static int inspect(CommandSourceStack src, BlockPos pos) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        List<String> out = new ArrayList<>();
        if (node == null) out.add("no EA node at " + pos);
        else {
            out.add(node.getClass().getSimpleName() + " at " + pos + ", in simulation " + node.isAdded() + ", light " + (node instanceof mods.eln.node.Node n ? n.getLightValue() : -1));
            if (node instanceof mods.eln.node.transparent.TransparentNode tn && tn.element != null) {
                out.add("- " + tn.element.getClass().getSimpleName() + " " + new TreeMap<>(tn.element.getWaila()));
                out.add(String.format("  ambient: stamped %.1f C, here now %.1f C", tn.element.ambient + mods.eln.sim.PhysicalConstant.Tamb,
                    mods.eln.sim.Ambient.celsius(src.getLevel(), pos)));
                if (tn.getFluidHandler() instanceof mods.eln.fluid.ElementFluidHandler h)
                    out.add("  tank: " + h.getFluidAmount() + " / " + h.getCapacity() + " mB of " + h.getFluid().getFluid());
            }
            // links to neighbours (debugging live conversions: Incognito's spawn lamp)
            List<String> links = new ArrayList<>();
            for (var nc : node.nodeConnectionList) {
                var o = field(nc, "N1") == node ? (mods.eln.node.NodeBase) field(nc, "N2") : (mods.eln.node.NodeBase) field(nc, "N1");
                links.add(o.coordonate.x + "," + o.coordonate.y + "," + o.coordonate.z + (field(nc, "EC") != null ? " E" : "") + (o.isAdded() ? "" : " (gone)"));
            }
            out.add("  links: " + links);
            if (node instanceof mods.eln.node.voxel.VoxelNode v) {
                out.add("- " + v.getWaila(true));
                for (var p : v.parts) {
                    var t = new net.minecraft.nbt.CompoundTag();
                    p.write(t);
                    out.add("  " + p.kind() + " " + t);
                }
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
                            out.add("  lamp load: grounded " + field(l, "grounded") + ", front " + field(l, "front") + ", simulated " + !l.positiveLoad.isNotSimulated()
                                + ", in list x" + l.electricalLoadList.stream().filter(x -> x == l.positiveLoad).count() + ", R " + l.lampResistor.getR()
                                + ", resistor in list x" + l.electricalComponentList.stream().filter(x -> x == l.lampResistor).count()
                                + ", resistor subsystem " + (l.lampResistor.getSubSystem() != null) + ", abstracted " + l.positiveLoad.isAbstracted()
                                + ", burnt " + l.isBurnt() + ", own node " + (l.sixNode == node) + ", node destructing " + (l.sixNode != null && l.sixNode.isDestructing())
                                + ", mna root " + (mods.eln.Eln.simulator.mna.addStates.contains(l.positiveLoad) ? "pending" : "-"));
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

    public static Object field(Object o, String name) {
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
