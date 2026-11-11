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
                .then(Commands.literal("voxelgallery").executes(c -> voxelGallery(c.getSource(), BlockPos.containing(c.getSource().getPosition()).below()))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> voxelGallery(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos")))))
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
                .then(Commands.literal("logwave").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("minutes", IntegerArgumentType.integer(1, 1440))
                        .executes(c -> logWave(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "minutes"))))))
                .then(Commands.literal("logperiod").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("seconds", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0.05, 86400))
                        .executes(c -> logPeriod(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"),
                            com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "seconds"))))))
                .then(Commands.literal("logmode").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("mode", IntegerArgumentType.integer(0, 2)).executes(c -> {
                        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(BlockPosArgument.getLoadedBlockPos(c, "pos"), c.getSource().getLevel()));
                        int n = 0;
                        if (node instanceof mods.eln.node.six.SixNode six)
                            for (var el : six.sideElementList) {
                                var lg = el instanceof mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement l ? l.logger
                                    : el instanceof mods.eln.sixnode.electricalmath.ElectricalMathElement m ? m.logger : null;
                                if (lg == null) continue;
                                lg.mode = (byte) IntegerArgumentType.getInteger(c, "mode");
                                el.needPublish();
                                n++;
                            }
                        return n;
                    }))))
                .then(Commands.literal("tiltedinputs").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> {
                    // as shift + the wrench key (C) on a right-click: the tilted terminal's inputs to the next edge
                    var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(BlockPosArgument.getLoadedBlockPos(c, "pos"), c.getSource().getLevel()));
                    int n = 0;
                    if (node instanceof mods.eln.node.six.SixNode six)
                        for (var el : six.sideElementList)
                            if (el instanceof mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement l && l.turnInputs()) {
                                n++;
                                String edge = String.valueOf(l.pinEdge());
                                c.getSource().sendSuccess(() -> Component.literal("inputs now on the " + edge + " edge"), false);
                            }
                    if (n == 0) c.getSource().sendFailure(Component.literal("no tilted terminal logger there"));
                    return n;
                })))
                .then(Commands.literal("protection").executes(c -> demo(c.getSource(), () -> protection(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("switches").executes(c -> demo(c.getSource(), () -> furnaceRigs(c.getSource(), BlockPos.containing(c.getSource().getPosition()),
                    "switches", new String[]{"230V Macerator", "230V Compressor", "230V Magnetizer", "230V Plate Machine", "Desulfuriser", "Polymeriser",
                        "Industrial Resin Collector", "Steam Cracker"}, new float[8]))))
                .then(Commands.literal("poles").executes(c -> demo(c.getSource(), () -> poles(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("wiredrawing").executes(c -> demo(c.getSource(), () -> wiredrawing(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("cores").executes(c -> demo(c.getSource(), () -> cores(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("rubber").executes(c -> demo(c.getSource(), () -> rubber(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("itemanim").executes(c -> demo(c.getSource(), () -> itemAnim(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("cracker").executes(c -> demo(c.getSource(), () -> cracker(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("oilrubber").executes(c -> demo(c.getSource(), () -> oilRubber(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("latex").executes(c -> demo(c.getSource(), () -> latex(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("converters").executes(c -> demo(c.getSource(), () -> converters(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("computers").executes(c -> demo(c.getSource(), () -> computers(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("loggerdemo").executes(c -> demo(c.getSource(), () -> loggerDemo(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("spmodules").executes(c -> demo(c.getSource(), () -> spModules(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("tilted").executes(c -> demo(c.getSource(), () -> tilted(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("threelogger").executes(c -> demo(c.getSource(), () -> threeLogger(c.getSource(), BlockPos.containing(c.getSource().getPosition())))))
                .then(Commands.literal("fluiddump").then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> {
                    var pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
                    var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, c.getSource().getLevel()));
                    Object dev = node instanceof mods.eln.node.transparent.TransparentNode tn ? tn.element : node;
                    if (!(dev instanceof mods.eln.sim.fluid.FluidDevice d) || d.fluidPorts().isEmpty()) {
                        c.getSource().sendFailure(Component.literal("no fluid device at " + pos));
                        return 0;
                    }
                    var net = mods.eln.fluid.FluidWorld.networkOf(d.fluidPorts().get(0).node());
                    String text = net == null ? null : net.dump(0.05);
                    if (text == null) {
                        c.getSource().sendFailure(Component.literal("no network stepped there yet"));
                        return 0;
                    }
                    try {
                        var dir = java.nio.file.Path.of("fluidcore-fail");
                        java.nio.file.Files.createDirectories(dir);
                        var f = dir.resolve("dump-" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ() + ".txt");
                        java.nio.file.Files.writeString(f, text);
                        c.getSource().sendSuccess(() -> Component.literal("written to " + f.toAbsolutePath()), false);
                    } catch (java.io.IOException ex) {
                        c.getSource().sendFailure(Component.literal("could not write: " + ex));
                    }
                    return 1;
                })))
                .then(Commands.literal("fluidstats").executes(c -> {
                    var lines = mods.eln.fluid.FluidWorld.stats();
                    for (String l : lines) mods.eln.Eln.LOG.info("fluidstats: " + l);
                    c.getSource().sendSuccess(() -> Component.literal(lines.size() + " lines (server log, 'fluidstats')"), false);
                    return lines.size();
                }))
                .then(Commands.literal("fluidscan").then(Commands.argument("n", IntegerArgumentType.integer(5, 2000))
                    .executes(c -> {
                        mods.eln.fluid.FluidWorld.startScan(IntegerArgumentType.getInteger(c, "n"));
                        c.getSource().sendSuccess(() -> Component.literal("scanning every fluid network (server log, 'fluidscan')"), false);
                        return 1;
                    })))
                .then(Commands.literal("fluidtrace").then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("n", IntegerArgumentType.integer(1, 200))
                        .executes(c -> fluidtrace(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "n"))))))
                .then(Commands.literal("nodes").then(Commands.argument("pos", BlockPosArgument.blockPos()).then(Commands.argument("r", IntegerArgumentType.integer(1, 32))
                    .executes(c -> nodesNear(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), IntegerArgumentType.getInteger(c, "r"))))))
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

    /**
     * Fills the data loggers at pos with a made-up history of the given length (a slow wave, a little ripple, a short
     * spike every 7 minutes) for screenshots and manual tests of the graph.
     */
    static int logWave(CommandSourceStack src, BlockPos pos, int minutes) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        int n = 0;
        if (node instanceof mods.eln.node.six.SixNode six)
            for (var e : six.sideElementList)
                if (e instanceof mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement l) {
                    l.store.clear();
                    long ticks = minutes * 1200L;
                    for (long t = 0; t < ticks; t++) {
                        double v = 0.5 + 0.3 * Math.sin(t / 24000.0 * Math.PI) + 0.05 * Math.sin(t / 7.0) + (t % 8400 < 4 ? 0.3 : 0);
                        // (more channels: a square wave, a ramp)
                        float[] vs = new float[l.store.channels()];
                        for (int c = 0; c < vs.length; c++) vs[c] = (float) (c == 0 ? v : c == 1 ? ((t / 12000) % 2 == 0 ? 0.7 : 0.2) : (t % 18000) / 18000.0);
                        l.store.tick(vs);
                    }
                    l.store.setPeriod(l.store.periodSeconds());
                    l.sendView(null);
                    n++;
                } else if (e instanceof mods.eln.sixnode.electricalmath.ElectricalMathElement m) {
                    // a signal processor's A, B, C and out: a slow wave, a square, a ramp, and A while B is on
                    var st = m.logger.store;
                    st.clear();
                    long ticks = minutes * 1200L;
                    for (long t = 0; t < ticks; t++) {
                        float a = (float) (0.5 + 0.3 * Math.sin(t / 24000.0 * Math.PI) + 0.03 * Math.sin(t / 9.0)), b = (t / 12000) % 2 == 0 ? 0.8f : 0.1f,
                            c = (t % 18000) / 18000f;
                        st.tick(new float[]{a, b, c, b > 0.5 ? a : 0});
                    }
                    st.setPeriod(st.periodSeconds());
                    m.logger.sendView(null);
                    n++;
                }
        int count = n;
        src.sendSuccess(() -> Component.literal(count + " logger(s) filled with " + minutes + " min"), false);
        return n;
    }

    /** Sets the sampling period of the data loggers at pos, as their screen's period field does. */
    static int logPeriod(CommandSourceStack src, BlockPos pos, double seconds) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        int n = 0;
        if (node instanceof mods.eln.node.six.SixNode six)
            for (var e : six.sideElementList)
                if (e instanceof mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement l) {
                    l.store.setPeriod(seconds);
                    l.needPublish();
                    l.sendView(null);
                    n++;
                } else if (e instanceof mods.eln.sixnode.electricalmath.ElectricalMathElement m) {
                    m.logger.store.setPeriod(seconds);
                    m.needPublish();
                    m.logger.sendView(null);
                    n++;
                }
        return n;
    }

    /**
     * The three-channel logger's test layout at pos (as GameTest wp9c three-channel data logger): the logger in the
     * block east of pos, its back west; three floor cables in pos from its pins to three Signal Sources (10, 25, 40 V)
     * north, west and south of pos. For screenshots and trying it.
     */
    static int threeLogger(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var logger = (mods.eln.sixnode.electricaldatalogger.ElectricalDataLoggerElement) host(level, player, 3 + (93 << 6), pos.east());
        orient(logger, mods.eln.misc.LRDU::inverse, mods.eln.misc.Direction.XN);
        int[][] at = {{0, 0, -1}, {-1, 0, 0}, {0, 0, 1}};
        mods.eln.misc.Direction[] toward = {mods.eln.misc.Direction.ZP, mods.eln.misc.Direction.XP, mods.eln.misc.Direction.ZN};
        float[] volts = {10, 25, 40};
        for (int i = 0; i < 3; i++) {
            var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 1 + (3 << 6), pos.offset(at[i][0], at[i][1], at[i][2]));
            orient(s, l -> l, toward[i]);
            float u = volts[i];
            try {
                var bos = new java.io.ByteArrayOutputStream();
                var o = new java.io.DataOutputStream(bos);
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(u);
                s.networkUnserialize(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bos.toByteArray())));
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        for (int[] pts : new int[][]{{16, 0, 4, 8, 0, 4, 8, 0, 0}, {16, 0, 8, 0, 0, 8}, {16, 0, 12, 8, 0, 12, 8, 0, 16}}) {
            String err = mods.eln.node.voxel.PipeKitItem.place(level, pos, new mods.eln.node.voxel.CablePart(0, 0, pts), null);
            if (err != null) {
                src.sendFailure(Component.literal("cable: " + err));
                return 0;
            }
        }
        src.sendSuccess(() -> Component.literal("three-channel logger laid"), false);
        return 1;
    }

    /**
     * Every logger for a look (screenshots): the three processors (spmodules) 2 north, the tilted terminals 8 east, the
     * three-channel logger with its cables 8 west, a data logger 5 south-west, and a processor on a west wall 4 south
     * with a floor cable turning up the wall to it (a knuckle in the corner, under its terminal).
     */
    /** A demo build: what stopped it, said plainly (a block in the way), rather than "an unexpected error". */
    static int demo(CommandSourceStack src, java.util.function.IntSupplier build) {
        try {
            return build.getAsInt();
        } catch (RuntimeException e) {
            src.sendFailure(Component.literal("demo not built: " + e.getMessage() + " (clear the area, or stand elsewhere)"));
            return 0;
        }
    }

    /**
     * Two computers on one field bus, for trying programs that talk (the Applications floppy's BBS and Gorillas): each
     * powered at 50 V from a source behind it, with Janet, 2 RAM and an Applications floppy, at addresses 60 and 61. They
     * face opposite ways 4 blocks apart, so their bus pins face each other, and a bus cable joins the pins.
     */
    static int computers(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int id = mods.eln.robotics.Robotics.COMPUTER_ID;
        var a = placeComputer(level, player, id, pos, 0, 60);
        var toB = a.front.right().toEnumFacing();
        BlockPos pb = pos.relative(toB, 4);
        var b = placeComputer(level, player, id, pb, 180, 61);
        if (b.front.right().toEnumFacing() != toB.getOpposite()) throw new IllegalStateException("the second computer faces the wrong way");
        long[][] join = new long[1][];
        var start = mods.eln.node.voxel.CablePlacer.endAtForTests(level, pos, toB, pinHit(pos, toB), true, join, mods.eln.node.voxel.CablePart.BUS);
        var end = mods.eln.node.voxel.CablePlacer.endAtForTests(level, pb, toB.getOpposite(), pinHit(pb, toB.getOpposite()), false, new long[1][],
            mods.eln.node.voxel.CablePart.BUS);
        String err = start == null || end == null ? "no pin end: " + start + " " + end
            : mods.eln.node.voxel.CablePlacer.routeAndLayJoinedForTests(level, mods.eln.node.voxel.CablePart.BUS, start, join[0], end, null);
        if (err != null) throw new IllegalStateException("bus cable: " + err);
        src.sendSuccess(() -> Component.literal("computers 60 at " + pos.toShortString() + " and 61 at " + pb.toShortString() + ", on one bus"), false);
        return 1;
    }

    /**
     * DC-DC converter rigs for a client check of the realistic converter (Baughn 2026-11-06), in a row along x, 5 apart,
     * each fed 48 V on its left: (a) 2:1 LV on a cheap core (saturates and burns), (b) 14:7 cheap (fine), (c) 13:62 LV:MV
     * cheap (~229 V), (d) 5:24 LV:MV optimal (230.4 V), (e) the Variable converter 14:28 LV:MV cheap with a Signal Trimmer on
     * its front at 50 %, (f) 14:7 cheap with its secondary shorted to ground (overheats).
     */
    /** Places a transparent node of this display name at p, facing as a player looking along +z would place it. */
    static mods.eln.node.transparent.TransparentNodeElement placeTransparent(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player,
                                                                         String name, BlockPos p) {
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(transparentId(name));
        float y0 = player.getYRot(), x0 = player.getXRot();
        player.setYRot(0);
        player.setXRot(0);
        try {
            var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
            if (node == null) throw new IllegalStateException("no room for " + name + " at " + p);
            return node.element;
        } finally {
            player.setYRot(y0);
            player.setXRot(x0);
        }
    }

    /** A source of volts two blocks out on side of p, joined to p by a cable (six-node id). */
    static void feed(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos p, mods.eln.misc.Direction side,
                     int cable, double volts) {
        host(level, player, cable, step(p, side));
        var src = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 3 << 6, step(step(p, side), side));
        orient(src, l -> l, side.getInverse());
        src.networkUnserialize(packet(o -> {
            o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            o.writeFloat((float) volts);
        }));
    }

    /**
     * Wires stage 4: the 48V wire-drawing machine on a 48 V source (back), the 230V one on a 230 V source, and a 48 V-fed
     * DC-DC converter with a cheap core and empty winding slots (an LV cable on its secondary side).
     */
    static int wiredrawing(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int lv = 4 + (32 << 6), mv = 8 + (32 << 6);
        var m48 = placeTransparent(level, player, "48V Wire Drawing Machine", pos);
        feed(level, player, pos, m48.front.back(), lv, mods.eln.Eln.LVU);
        BlockPos p2 = pos.offset(5, 0, 0);
        var m230 = placeTransparent(level, player, "230V Wire Drawing Machine", p2);
        feed(level, player, p2, m230.front.back(), mv, mods.eln.Eln.MVU);
        BlockPos p3 = pos.offset(10, 0, 0);
        var t = (mods.eln.transparentnode.transformer.TransformerElement) placeTransparent(level, player, "DC-DC Converter", p3);
        feed(level, player, p3, t.front.left(), lv, mods.eln.Eln.LVU);
        host(level, player, mv, step(p3, t.front.right()));
        t.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.ferromagneticSlotId,
            mods.eln.Eln.findItemStack("Cheap Ferromagnetic Core", 1));
        t.getInventory().setChanged();
        src.sendSuccess(() -> Component.literal("wiredrawing: 48V machine " + pos.toShortString() + ", 230V machine " + p2.toShortString()
            + ", converter (cheap core, 48 V on its left, a 230 V cable on its right) " + p3.toShortString()), false);
        return 1;
    }

    /**
     * The modular utility pole (Baughn 2026-11-08), three poles 8 apart along +x strung with 11 kV kits: A fed 48 V at its
     * foot through a 48 V transformer (so the line is at 11 kV), B with a 230 V transformer, four streetlights and a stasis
     * anchor, C with one streetlight facing south and nothing else.
     */
    static int poles(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int lv = 4 + (32 << 6);
        var pe = new mods.eln.gridnode.electricalpole.ElectricalPoleElement[3];
        for (int k = 0; k < 3; k++)
            pe[k] = (mods.eln.gridnode.electricalpole.ElectricalPoleElement) placeTransparent(level, player, "Utility Pole", pos.offset(8 * k, 0, 0));
        feed(level, player, pos, mods.eln.misc.Direction.ZN, lv, mods.eln.Eln.LVU);
        var inv = pe[0].getInventory();
        inv.setItem(mods.eln.gridnode.electricalpole.ElectricalPoleElement.TRANSFORMER, mods.eln.gridnode.electricalpole.PoleTransformerItem.make(1, 8, 2));
        inv.setChanged();
        inv = pe[1].getInventory();
        inv.setItem(mods.eln.gridnode.electricalpole.ElectricalPoleElement.TRANSFORMER, mods.eln.gridnode.electricalpole.PoleTransformerItem.make(2, 8, 1));
        inv.setItem(mods.eln.gridnode.electricalpole.ElectricalPoleElement.ANCHOR, new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.POLE_ANCHOR.get()));
        for (int k = 0; k < 4; k++)
            inv.setItem(mods.eln.gridnode.electricalpole.ElectricalPoleElement.LAMP0 + k, new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.STREETLIGHT.get()));
        inv.setChanged();
        inv = pe[2].getInventory();
        inv.setItem(mods.eln.gridnode.electricalpole.ElectricalPoleElement.LAMP0 + 2, new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.STREETLIGHT.get()));
        inv.setChanged();
        // (overhead cable, 6 mm²: Baughn 2026-11-09, poles are strung with it)
        var cable = mods.eln.gridnode.OverheadCableItem.make(6, 8);
        for (int k = 0; k < 2; k++)
            mods.eln.gridnode.GridLink.addLink(pe[k], pe[k + 1], mods.eln.misc.Direction.XP, mods.eln.misc.Direction.XN,
                cable.copy(), 8 * mods.eln.gridnode.OverheadCableItem.rsPerBlock(cable));
        src.sendSuccess(() -> Component.literal("poles: A (48 V fed, 48 V transformer) " + pos.toShortString() + ", B (230 V transformer, 4 lights, anchor) "
            + pos.offset(8, 0, 0).toShortString() + ", C (a light facing south) " + pos.offset(16, 0, 0).toShortString()), false);
        return 1;
    }

    static int converters(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int lv = 4 + (32 << 6), mv = 8 + (32 << 6);
        Object[][] rigs = {
            {"DC-DC Converter", lv, 2, lv, 1, "Cheap Ferromagnetic Core", false},
            {"DC-DC Converter", lv, 14, lv, 7, "Cheap Ferromagnetic Core", false},
            {"DC-DC Converter", lv, 13, mv, 62, "Cheap Ferromagnetic Core", false},
            {"DC-DC Converter", lv, 5, mv, 24, "Optimal Ferromagnetic Core", false},
            {"Variable DC-DC Converter", lv, 14, mv, 28, "Cheap Ferromagnetic Core", false},
            {"DC-DC Converter", lv, 14, lv, 7, "Cheap Ferromagnetic Core", true},
        };
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < rigs.length; i++) {
            Object[] r = rigs[i];
            BlockPos p = pos.offset(5 * i, 0, 0);
            var d = mods.eln.Eln.transparentNodeItem.getDescriptor(transparentId((String) r[0]));
            float y0 = player.getYRot(), x0 = player.getXRot();
            player.setYRot(0);
            player.setXRot(0);
            mods.eln.node.transparent.TransparentNode node;
            try {
                node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
            } finally {
                player.setYRot(y0);
                player.setXRot(x0);
            }
            if (node == null || !(node.element instanceof mods.eln.transparentnode.transformer.TransformerElement t))
                throw new IllegalStateException("no room for a converter at " + p);
            mods.eln.misc.Direction left = t.front.left(), right = t.front.right();
            host(level, player, lv, step(p, left));
            var feed = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 3 << 6, step(step(p, left), left));
            orient(feed, l -> l, right);
            feed.networkUnserialize(packet(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat((float) mods.eln.Eln.LVU);
            }));
            host(level, player, (int) r[3], step(p, right));
            if ((boolean) r[6]) host(level, player, sixId("Ground Cable"), step(step(p, right), right));
            if (t instanceof mods.eln.transparentnode.transformer.VariableTransformerElement) {
                var trim = (mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement) host(level, player, sixId("Signal Trimmer"),
                    step(p, t.front));
                orient(trim, l -> l, t.front.back());
                trim.networkUnserialize(packet(o -> {
                    o.writeByte(mods.eln.sixnode.electricalgatesource.ElectricalGateSourceElement.setVoltagerId);
                    o.writeFloat((float) (mods.eln.Eln.SVU / 2));
                }));
            }
            t.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.primaryCableSlotId,
                mods.eln.Eln.sixNodeItem.getDescriptor((int) r[1]).newItemStack((int) r[2]));
            t.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.secondaryCableSlotId,
                mods.eln.Eln.sixNodeItem.getDescriptor((int) r[3]).newItemStack((int) r[4]));
            t.getInventory().setItem(mods.eln.transparentnode.transformer.TransformerContainer.ferromagneticSlotId,
                mods.eln.Eln.findItemStack((String) r[5], 1));
            t.getInventory().setChanged();
            where.append((char) ('a' + i)).append(' ').append(p.toShortString()).append("; ");
        }
        src.sendSuccess(() -> Component.literal("converters: " + where), false);
        return 1;
    }

    /**
     * Rigs for the ferromagnetic cores' recipes (Baughn 2026-11-06), 5 apart along x, each on 230 V: (a) an Electrical
     * Furnace with a 230V Tungsten Heating Corp, Thermal Insulation and an Analogic Regulator at 650 C, (b) the same at
     * 500 C (too cold to anneal), (c) a 230V Magnetizer.
     */
    static int cores(CommandSourceStack src, BlockPos pos) {
        return furnaceRigs(src, pos, "cores", new String[]{"Electrical Furnace", "Electrical Furnace", "230V Magnetizer"}, new float[]{650, 500, 0});
    }

    /**
     * Rigs for the rubber chain (Baughn 2026-11-07), 5 apart along x, each on 230 V: (a) an Electrical Furnace held at
     * 160 C (vulcanises Rubber Compound), (b) one at 260 C (chars it), (c) and (d) 230V Macerators (soul sand, sulfur).
     */
    static int rubber(CommandSourceStack src, BlockPos pos) {
        return furnaceRigs(src, pos, "rubber", new String[]{"Electrical Furnace", "Electrical Furnace", "230V Macerator", "230V Macerator"},
            new float[]{160, 260, 0, 0});
    }

    /**
     * Rigs for latex (the rubber chain, 2026-11-07), on 230 V: (a) a 230V Compressor (feed it dandelions; latex out of its
     * red flange, latex into its blue one presses to Raw Rubber); (b) an Industrial Resin Collector 8 blocks on, a rubber
     * fig grown against its back.
     */
    static int latex(CommandSourceStack src, BlockPos pos) {
        furnaceRigs(src, pos, "latex", new String[]{"230V Compressor"}, new float[]{0});
        var lvl = src.getLevel();
        // (AdventurAgent: give the IN flange latex to press) an iron tank of latex two blocks behind it, piped to its IN flange
        var cn = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, lvl));
        if (cn instanceof mods.eln.node.transparent.TransparentNode ct && ct.element != null) {
            var back = ct.element.front.back();
            BlockPos tp = pos.relative(back.toEnumFacing(), 2);
            int at = mods.eln.fluid.PortBridge.endSpot(ct.element.front, back, mods.eln.transparentnode.electricalmachine.CompressorPortsDescriptor.IN_Y,
                mods.eln.transparentnode.electricalmachine.CompressorPortsDescriptor.FLANGE_Z);
            var face = back.toEnumFacing();
            // (a point on the IN flange, for the port pick: the face's centre column at the flange's height)
            int yPx = face.get3DDataValue() <= 3 ? at % 17 : at / 17;   // (PortBridge.endSpot: north/south x then y, west/east y then z)
            var hit = net.minecraft.world.phys.Vec3.atCenterOf(pos).relative(face, 0.5).add(0, (yPx - 8) / 16.0, 0);
            String r = tankPiped(src, tp, pos, face, hit, 8000);
            src.sendSuccess(() -> Component.literal("latex tank at " + tp.toShortString() + " to the IN flange: " + r), false);
        }
        BlockPos p = pos.offset(8, 0, 0);
        furnaceRigs(src, p, "collector", new String[]{"Industrial Resin Collector"}, new float[]{0});
        var level = src.getLevel();
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, level));
        if (n instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) {
            BlockPos trunk = p.relative(t.element.front.back().toEnumFacing());
            level.setBlockAndUpdate(trunk.below(), net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState());
            var feature = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE).getOrThrow(mods.eln.rubber.RubberTrees.TREE);
            boolean ok = feature.value().place(level, level.getChunkSource().getGenerator(), level.getRandom(), trunk);
            src.sendSuccess(() -> Component.literal("rubber fig at " + trunk.toShortString() + ": " + (ok ? "grown" : "refused: " + mods.eln.rubber.RubberFigFeature.lastRefusal)), false);
            // an empty iron tank two blocks in front of it, piped from its top flange
            BlockPos tp = p.relative(t.element.front.toEnumFacing(), 2);
            String r = tankPiped(src, tp, p, net.minecraft.core.Direction.UP, null, 0);
            src.sendSuccess(() -> Component.literal("tank at " + tp.toShortString() + " from the collector's flange: " + r), false);
        }
        return 1;
    }

    /**
     * Rigs for the oil route (2026-11-07): (a) a Carbon Black Burner fed by a tank of crude piped to its back flange, a
     * chest under it; (b) a Desulfuriser on 230 V, a tank of diesel piped to its IN flange and an empty tank from its OUT.
     */
    static int oilRubber(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var bd = mods.eln.Eln.transparentNodeItem.getDescriptor(0 + (93 << 6));
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
        var bn = ((mods.eln.node.transparent.TransparentNodeItem) bd.parentItem).placeAt(bd.newItemStack(), player, level, pos, mods.eln.misc.Direction.YN);
        if (bn == null) src.sendFailure(Component.literal("no room for the carbon black burner at " + pos.toShortString()));
        // (a hopper under it, which a transparent node may stand on, and a chest under that)
        level.setBlockAndUpdate(pos.below(2), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), net.minecraft.world.level.block.Blocks.HOPPER.defaultBlockState());
        if (bn != null && bn.element != null) {
            var back = bn.element.front.back().toEnumFacing();
            String r = tankPiped(src, pos.relative(back, 2), pos, back, null, 6000, mods.eln.oil.Oil.CRUDE.get());
            src.sendSuccess(() -> Component.literal("a carbon black burner " + pos.toShortString() + " (a hopper and a chest under it): " + r), false);
        }
        BlockPos dp = pos.offset(8, 0, 0);
        furnaceRigs(src, dp, "desulfuriser", new String[]{"Desulfuriser"}, new float[]{0});
        var dn = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(dp, level));
        if (dn instanceof mods.eln.node.transparent.TransparentNode t && t.element != null) {
            var back = t.element.front.back().toEnumFacing();
            var frontF = t.element.front.toEnumFacing();
            String a = tankPiped(src, dp.relative(back, 2), dp, back, null, 8000, mods.eln.oil.Oil.Cut.DIESEL.source.get());
            String b = tankPiped(src, dp.relative(frontF, 2), dp, frontF, null, 0);
            src.sendSuccess(() -> Component.literal("desulfuriser " + dp.toShortString() + ": diesel in " + a + "; out " + b), false);
        }
        return 1;
    }

    /**
     * The four electrical machines at work, 230V in a row along +x and 48V in a row 5 blocks along +z, each powered and
     * with a stack in its input (iron ore, coal dust, iron ingots, copper ingots): their items in process (todo #278).
     */
    static int itemAnim(CommandSourceStack src, BlockPos pos) {
        String[] kinds = {"Macerator", "Compressor", "Magnetizer", "Plate Machine"};
        net.minecraft.world.item.ItemStack[] inputs = {new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_ORE, 64),
            mods.eln.Eln.findItemStack("Coal Dust", 64), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 64),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT, 64)};
        for (int row = 0; row < 2; row++) {
            String tier = row == 0 ? "230V " : "48V ";
            String[] names = new String[kinds.length];
            for (int i = 0; i < kinds.length; i++) names[i] = tier + kinds[i];
            BlockPos rp = pos.offset(0, 0, 5 * row);
            furnaceRigs(src, rp, "item animations " + tier.trim(), names, new float[kinds.length]);
            for (int i = 0; i < kinds.length; i++) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(rp.offset(5 * i, 0, 0), src.getLevel()));
                if (n instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof mods.eln.transparentnode.electricalmachine.ElectricalMachineElement m) {
                    m.getInventory().setItem(m.inSlotId, inputs[i].copy());
                    m.getInventory().setChanged();
                }
            }
        }
        return 1;
    }

    /** Where a click on a device's port lands (world coordinates), from its spot (FluidPort.at) on its face. */
    static net.minecraft.world.phys.Vec3 portHit(BlockPos dev, mods.eln.sim.fluid.FluidDevice.FluidPort p) {
        var f = net.minecraft.core.Direction.from3DDataValue(p.face());
        double a = p.at() / 17 / 16.0, b = p.at() % 17 / 16.0;
        double x = dev.getX(), y = dev.getY(), z = dev.getZ();
        return switch (f) {
            case NORTH -> new net.minecraft.world.phys.Vec3(x + a, y + b, z);
            case SOUTH -> new net.minecraft.world.phys.Vec3(x + a, y + b, z + 1);
            case WEST -> new net.minecraft.world.phys.Vec3(x, y + a, z + b);
            case EAST -> new net.minecraft.world.phys.Vec3(x + 1, y + a, z + b);
            default -> new net.minecraft.world.phys.Vec3(x + 0.5, y + 0.5, z + 0.5);
        };
    }

    /**
     * The Steam Cracker (the oil route, P5c) on 400 V: a tank of gasoline piped to its C8 flange, a tank of hot steam
     * (3 bar, 150 C; it cools) to its H2O flange, its steam tank filled too (4000 mB: 32 s of cracking), a Polymeriser fed
     * from its C4H6 flange (P5d), and an empty tank from its CH4 flange.
     */
    static int cracker(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        furnaceRigs(src, pos, "steam cracker", new String[]{"Steam Cracker"}, new float[]{0});
        var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level));
        if (!(n instanceof mods.eln.node.transparent.TransparentNode t) || !(t.element instanceof mods.eln.transparentnode.steamcracker.SteamCrackerElement c)) {
            src.sendFailure(Component.literal("no steam cracker at " + pos.toShortString()));
            return 0;
        }
        var ports = c.fluidPorts();
        var back = c.front.back().toEnumFacing();
        var frontF = c.front.toEnumFacing();
        var right = c.front.right().toEnumFacing();
        // (the flanges at model z 12 are on the side of front.right(), those at z 4 on the other)
        String g = tankPiped(src, pos.relative(back, 2).relative(right.getOpposite(), 1), pos, back, portHit(pos, ports.get(0)), 8000, mods.eln.oil.Oil.Cut.GASOLINE.source.get());
        BlockPos stp = pos.relative(back, 2).relative(right, 1);
        String st = tankPiped(src, stp, pos, back, portHit(pos, ports.get(1)), 0);
        // the butadiene into a Polymeriser (on 230 V, 16 Soul Dust in it), three blocks out in front, facing the same way
        BlockPos pp = pos.relative(frontF, 3).relative(right.getOpposite(), 2);
        furnaceRigs(src, pp, "polymeriser", new String[]{"Polymeriser"}, new float[]{0});
        String b;
        var pn = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pp, level));
        if (pn instanceof mods.eln.node.transparent.TransparentNode ptn && ptn.element instanceof mods.eln.transparentnode.polymeriser.PolymeriserElement pe) {
            pe.getInventory().setItem(mods.eln.transparentnode.polymeriser.PolymeriserElement.SOUL_SLOT, new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.SOUL_DUST.get(), 16));
            var pa = mods.eln.node.voxel.PipePlacer.portAtForTests(level, pos, frontF, portHit(pos, ports.get(2)));
            var pb = mods.eln.node.voxel.PipePlacer.portAtForTests(level, pp, pe.front.back().toEnumFacing());
            if (pa == null || pb == null) b = "ports " + pa + " " + pb + " (polymeriser front " + pe.front + ")";
            else {
                var r = mods.eln.node.voxel.PipeRouter.route(mods.eln.node.voxel.PipePlacer.space(level), 4, pa, java.util.List.of(), pb);
                String err = r.error != null ? r.error : mods.eln.node.voxel.PipePlacer.lay(level, 0, r.points, null);
                b = err == null ? "piped to the polymeriser at " + pp.toShortString() : err;
            }
        } else b = "no polymeriser";
        String ng = tankPiped(src, pos.relative(frontF, 2).relative(right, 1), pos, frontF, portHit(pos, ports.get(3)), 0);
        var server = level.getServer();
        server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + 20, () -> {
            var sn = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(stp, level));
            if (sn instanceof mods.eln.node.transparent.TransparentNode tn && tn.element instanceof mods.eln.oil.TankElement tank) {
                var v = tank.vessel;
                v.fill(0, 1, 423.15);
                mods.eln.sim.fluid.EaUnits.addWater(v, 3e5 * v.volume / (461.5 * 423.15), 423.15, true);
                v.update();
                mods.eln.fluid.FluidWorld.changed(tank);
            }
            c.fillIn(new net.neoforged.neoforge.fluids.FluidStack(mods.eln.oil.Oil.STEAM.get(), 4000));
        }));
        src.sendSuccess(() -> Component.literal("steam cracker " + pos.toShortString() + ": gasoline " + g + "; steam " + st + "; butadiene " + b + "; gas " + ng), false);
        return 1;
    }

    /** An iron tank at tp (mB of latex in it), and an EA pipe routed from the device's port on face (nearest hit) to it. */
    static String tankPiped(CommandSourceStack src, BlockPos tp, BlockPos dev, net.minecraft.core.Direction face, net.minecraft.world.phys.Vec3 hit, int latexMb) {
        return tankPiped(src, tp, dev, face, hit, latexMb, mods.eln.oil.Latex.SOURCE.get());
    }

    /** As above, the tank holding mB of fluid f. */
    static String tankPiped(CommandSourceStack src, BlockPos tp, BlockPos dev, net.minecraft.core.Direction face, net.minecraft.world.phys.Vec3 hit, int latexMb,
                            net.minecraft.world.level.material.Fluid fluid) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(40 + (82 << 6));   // (an iron tank)
        var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, tp, mods.eln.misc.Direction.YN);
        if (node == null) return "no room for the tank";
        var a = mods.eln.node.voxel.PipePlacer.portAtForTests(level, dev, face, hit);
        net.minecraft.core.Direction toDev = null;
        for (var f : net.minecraft.core.Direction.Plane.HORIZONTAL) if (tp.relative(f).distManhattan(dev) < tp.distManhattan(dev)) toDev = f;
        if (toDev != null && node.element != null) {   // (a tank's port is on its back: turn it to the device)
            node.element.front = mods.eln.misc.Direction.fromEnumFacing(toDev).back();
            node.element.reconnect();
        }
        if (latexMb > 0) {   // (a second later: poured in the tick it was placed, or before its turn, the latex was lost)
            var server = level.getServer();
            server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + 20, () -> {
                var cap = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, tp, net.minecraft.core.Direction.UP);
                if (cap != null)
                    try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                        int got = cap.insert(net.neoforged.neoforge.transfer.fluid.FluidResource.of(fluid), latexMb, tx);
                        tx.commit();
                        src.sendSuccess(() -> Component.literal(got + " mB poured into the tank at " + tp.toShortString()), false);
                    }
            }));
        }
        final String poured = "filling in a second";
        var b = toDev == null ? null : mods.eln.node.voxel.PipePlacer.portAtForTests(level, tp, toDev);
        if (a == null || b == null) return "ports " + a + " " + b;
        var r = mods.eln.node.voxel.PipeRouter.route(mods.eln.node.voxel.PipePlacer.space(level), 4, a, java.util.List.of(), b);
        String err = r.error != null ? r.error : mods.eln.node.voxel.PipePlacer.lay(level, 0, r.points, null);
        return (err == null ? "piped" : err) + (latexMb > 0 ? ", " + poured : "");
    }

    /** Machines on 230 V, 5 apart along x; a furnace gets a tungsten heating corp, insulation and a regulator at its target (C). */
    static int furnaceRigs(CommandSourceStack src, BlockPos pos, String label, String[] names, float[] targets) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < names.length; i++) {
            BlockPos p = pos.offset(5 * i, 0, 0);
            var d = mods.eln.Eln.transparentNodeItem.getDescriptor(transparentId(names[i]));
            float y0 = player.getYRot(), x0 = player.getXRot();
            player.setYRot(0);
            player.setXRot(0);
            mods.eln.node.transparent.TransparentNode node;
            try {
                node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
            } finally {
                player.setYRot(y0);
                player.setXRot(x0);
            }
            if (node == null || node.element == null) throw new IllegalStateException("no room at " + p);
            var e = node.element;
            mods.eln.misc.Direction side = null;
            for (mods.eln.misc.Direction dir : new mods.eln.misc.Direction[]{e.front.getInverse(), e.front.left(), e.front.right(), e.front})
                if ((e.getConnectionMask(dir, mods.eln.misc.LRDU.Down) & mods.eln.node.NodeBase.maskElectricalPower) != 0) {
                    side = dir;
                    break;
                }
            if (side == null) throw new IllegalStateException("no power side on " + names[i]);
            boolean hv = e instanceof mods.eln.transparentnode.steamcracker.SteamCrackerElement, lv = names[i].startsWith("48V");
            host(level, player, hv ? 12 + (32 << 6) : lv ? 4 + (32 << 6) : 8 + (32 << 6), step(p, side));   // a Low/Medium/High Voltage Cable
            var feed = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 3 << 6, step(step(p, side), side));
            orient(feed, l -> l, side.getInverse());
            feed.networkUnserialize(packet(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat((float) (hv ? mods.eln.Eln.HVU : lv ? mods.eln.Eln.LVU : mods.eln.Eln.MVU));
            }));
            if (e instanceof mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement f) {
                f.getInventory().setItem(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.heatingCorpSlotId,
                    mods.eln.Eln.findItemStack("230V Tungsten Heating Corp", 1));
                f.getInventory().setItem(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.thermalIsolatorSlotId,
                    mods.eln.Eln.findItemStack("Thermal Insulation", 1));
                f.getInventory().setItem(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.thermalRegulatorSlotId,
                    mods.eln.Eln.findItemStack("Analogic Regulator", 1));
                f.getInventory().setChanged();
                float target = (float) (targets[i] - mods.eln.sim.PhysicalConstant.Tamb);   // (the screen shows the target plus the room)
                f.networkUnserialize(packet(o -> {
                    o.writeByte(mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceElement.unserializeTemperatureTarget);
                    o.writeFloat(target);
                }));
            }
            where.append((char) ('a' + i)).append(' ').append(names[i]).append(' ').append(p.toShortString()).append(" (power ").append(side).append("); ");
        }
        src.sendSuccess(() -> Component.literal(label + ": " + where), false);
        return 1;
    }

    /**
     * Rigs for breakers and fuses with set trip currents (Baughn 2026-11-06), along +x from pos, on voxel LV cables: a 48 V
     * source, a cable, the device, a cable, a 10 ohm Power Resistor (4.8 A), a cable, a ground. Row z: a breaker with
     * an LV cable in its slot, closed; row z+3: a fuse holder with a Lead Fuse.
     */
    static int protection(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int lv = mods.eln.node.voxel.CablePart.tierOf("Low Voltage Cable");
        for (int row = 0; row < 2; row++) {
            BlockPos o = pos.offset(0, 0, 3 * row);
            var feed = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 3 << 6, o);
            feed.networkUnserialize(packet(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat((float) mods.eln.Eln.LVU);
            }));
            for (int x : new int[]{1, 3, 5})
                for (var part : mods.eln.node.voxel.CablePart.arms(lv, 0, 4, 5)) {   // (on the floor, west to east)
                    String err = mods.eln.node.voxel.PipeKitItem.place(level, o.offset(x, 0, 0), part, null);
                    if (err != null) throw new IllegalStateException("cable at " + o.offset(x, 0, 0) + ": " + err);
                }
            if (row == 0) {
                var b = (mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement) host(level, player, sixId("Electrical Breaker"), o.offset(2, 0, 0));
                orient(b, l -> l, mods.eln.misc.Direction.XN);
                b.getInventory().setItem(0, mods.eln.Eln.findItemStack("Low Voltage Cable", 1));
                b.getInventory().setChanged();
                b.networkUnserialize(packet(out -> {
                    out.writeByte(mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement.setVoltageMaxId);
                    out.writeFloat(100f);
                }));
                b.networkUnserialize(packet(out -> out.writeByte(mods.eln.sixnode.electricalbreaker.ElectricalBreakerElement.toogleSwitchId)));
            } else {
                var f = (mods.eln.sixnode.ElectricalFuseHolderElement) host(level, player, sixId("Electrical Fuse Holder"), o.offset(2, 0, 0));
                orient(f, l -> l, mods.eln.misc.Direction.XN);
                f.setInstalledFuse((mods.eln.item.ElectricalFuseDescriptor) mods.eln.item.ElectricalFuseDescriptor.getDescriptor(mods.eln.Eln.findItemStack("Lead Fuse", 1)));
            }
            var r = (mods.eln.sixnode.resistor.ResistorElement) host(level, player, sixId("Power Resistor"), o.offset(4, 0, 0));
            orient(r, mods.eln.misc.LRDU::right, mods.eln.misc.Direction.XN);
            r.getInventory().setItem(mods.eln.sixnode.resistor.ResistorElement.coreSlotId, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 36));
            r.getInventory().setChanged();
            host(level, player, sixId("Ground Cable"), o.offset(6, 0, 0));
        }
        src.sendSuccess(() -> Component.literal("protection: breaker row at " + pos.toShortString() + ", fuse row at " + pos.offset(0, 0, 3).toShortString()
            + " (devices at x+2, 10 ohm resistors at x+4: 4.8 A)"), false);
        return 1;
    }

    static BlockPos step(BlockPos p, mods.eln.misc.Direction d) {
        return p.relative(d.toEnumFacing());
    }

    /** Where a click lands on a computer's bus pin: the floor edge of its flank out, 4 voxels right of the middle. */
    static net.minecraft.world.phys.Vec3 pinHit(BlockPos head, net.minecraft.core.Direction out) {
        int lx = -out.getStepZ(), lz = out.getStepX();   // (left, seen facing the face from outside)
        return new net.minecraft.world.phys.Vec3(head.getX() + 0.5 + out.getStepX() * 0.5 - lx * 4 / 16.0, head.getY() + 0.1,
            head.getZ() + 0.5 + out.getStepZ() * 0.5 - lz * 4 / 16.0);
    }

    static mods.eln.robotics.computer.ComputerElement placeComputer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player,
                                                               int id, BlockPos p, float yaw, int address) {
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(id);
        float y0 = player.getYRot(), x0 = player.getXRot();
        player.setYRot(yaw);
        player.setXRot(0);
        mods.eln.node.transparent.TransparentNode node;
        try {
            node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, level, p, mods.eln.misc.Direction.YN);
        } finally {
            player.setYRot(y0);
            player.setXRot(x0);
        }
        if (node == null || !(node.element instanceof mods.eln.robotics.computer.ComputerElement c)) throw new IllegalStateException("no room for a computer at " + p);
        var back = c.front.back().toEnumFacing();
        host(level, player, 4 + (32 << 6), p.relative(back));   // a low voltage cable
        var source = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) host(level, player, 3 << 6, p.relative(back, 2));
        source.networkUnserialize(packet(o -> {
            o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            o.writeFloat(50f);
        }));
        c.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(mods.eln.robotics.Robotics.JANET_ROM.get()));
        c.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(mods.eln.robotics.Robotics.RAM_CHIP.get()));
        c.getInventory().setItem(2, new net.minecraft.world.item.ItemStack(mods.eln.robotics.Robotics.RAM_CHIP.get()));
        c.getInventory().setItem(mods.eln.robotics.computer.ComputerElement.DRIVE_A,
            new net.minecraft.world.item.ItemStack(mods.eln.robotics.Robotics.APPLICATIONS_DISK.get()));
        c.getInventory().setChanged();
        c.networkUnserialize(packet(o -> {
            o.writeByte(mods.eln.robotics.computer.ComputerElement.SET_ADDRESS_ID);
            o.writeFloat(address);
        }), null);
        return c;
    }

    interface PacketWriter {
        void write(java.io.DataOutputStream o) throws java.io.IOException;
    }

    static java.io.DataInputStream packet(PacketWriter w) {
        var bytes = new java.io.ByteArrayOutputStream();
        try {
            w.write(new java.io.DataOutputStream(bytes));
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        return new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()));
    }

    static int loggerDemo(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        spModules(src, pos);
        tilted(src, pos.east(8));
        threeLogger(src, pos.west(8));
        orient(host(level, player, 0 + (93 << 6), pos.offset(-5, 0, 5)), l -> l, mods.eln.misc.Direction.ZP);
        logWave(src, pos.offset(-5, 0, 5), 30);
        BlockPos sp = pos.south(4);
        level.setBlock(sp.west(), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        host(level, player, 4 + (109 << 6), sp, mods.eln.misc.Direction.XN);
        mods.eln.node.voxel.PipeKitItem.place(level, sp, new mods.eln.node.voxel.CablePart(0, 0, new int[]{16, 0, 8, 1, 0, 8}), null);
        mods.eln.node.voxel.PipeKitItem.place(level, sp, mods.eln.node.voxel.CablePart.knuckle(0, 0, 0, 8), null);
        logWave(src, sp, 30);
        return 1;
    }

    /** Three signal processors on the floor 2 north of pos, 2 apart: no chips, 6 cheap chips, 3 advanced chips (18 modules). */
    static int spModules(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        String[] chip = {null, "Cheap Chip", "Advanced Chip"};
        int[] n = {0, 6, 3};
        for (int i = 0; i < 3; i++) {
            var e = (mods.eln.sixnode.electricalmath.ElectricalMathElement) host(level, player, 4 + (109 << 6), pos.offset(-2 + 2 * i, 0, -2));
            if (n[i] > 0) {
                e.getInventory().setItem(0, mods.eln.Eln.findItemStack(chip[i], n[i]));
                e.getInventory().setChanged();
            }
            logWave(src, pos.offset(-2 + 2 * i, 0, -2), 30);
        }
        return 1;
    }

    /**
     * Two tilted terminals for a look (screenshots): one hung from a ceiling block 3 above pos's floor, 2 north, and one
     * on a table (a block) 2 east and 2 north, one high on a wall 2 west and 3 north (its screen leaning down); facing
     * south, toward pos, with 60 minutes of made-up history.
     */
    static int tilted(CommandSourceStack src, BlockPos pos) {
        var level = src.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        BlockPos hang = pos.offset(0, 2, -2), table = pos.offset(2, 0, -2), wall = pos.offset(-2, 2, -2);
        level.setBlock(hang.above(), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(table, net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(), 3);
        var a = host(level, player, 4 + (93 << 6), hang, mods.eln.misc.Direction.YP);
        orient(a, l -> l, mods.eln.misc.Direction.ZP);
        var b = host(level, player, 4 + (93 << 6), table.above(), mods.eln.misc.Direction.YN);
        orient(b, l -> l, mods.eln.misc.Direction.ZP);
        level.setBlock(wall.north(), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        var w = host(level, player, 4 + (93 << 6), wall, mods.eln.misc.Direction.ZN);
        orient(w, l -> l, mods.eln.misc.Direction.YN);
        logWave(src, wall, 60);
        logWave(src, hang, 60);
        logWave(src, table.above(), 60);
        return 1;
    }

    static mods.eln.node.six.SixNodeElement host(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, int damage, BlockPos p) {
        return host(level, player, damage, p, mods.eln.misc.Direction.YN);
    }

    /** A device on side of p, in a voxel block (p's, if it has one): one already on that side (a demo built again) is replaced. */
    public static mods.eln.node.six.SixNodeElement host(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, int damage, BlockPos p,
                                                 mods.eln.misc.Direction side) {
        var d = mods.eln.Eln.sixNodeItem.getDescriptor(damage);
        var at = new mods.eln.misc.Coordonate(p, level);
        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(at) instanceof mods.eln.node.voxel.VoxelNode old && old.getElement(side) != null)
            old.deleteSubBlock(null, side);
        mods.eln.node.voxel.VoxelNode v;
        if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(at) instanceof mods.eln.node.voxel.VoxelNode old && !old.isDestructing()) v = old;
        else {
            v = new mods.eln.node.voxel.VoxelNode();
            v.onBlockPlacedBy(new mods.eln.misc.Coordonate(p, level), side, player, d.newItemStack());
            level.setBlock(p, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().defaultBlockState(), 3);
        }
        if (!v.createSubBlock(d.newItemStack(), side, player)) throw new IllegalStateException("can't host " + d.name + " at " + p);
        return v.getElement(side);
    }

    static void orient(mods.eln.node.six.SixNodeElement e, java.util.function.UnaryOperator<mods.eln.misc.LRDU> terminal, mods.eln.misc.Direction toward) {
        for (var f : mods.eln.misc.LRDU.values())
            if (e.side.applyLRDU(terminal.apply(f)) == toward) {
                e.front = f;
                e.sixNode.reconnect();
                e.sixNode.setNeedPublish(true);
                return;
            }
    }

    /** Log the fluid network of the device at pos (its first port's node) after each of the next n steps (FluidWorld.trace). */
    static int fluidtrace(CommandSourceStack src, BlockPos pos, int n) {
        var node = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, src.getLevel()));
        Object dev = node instanceof mods.eln.node.transparent.TransparentNode tn ? tn.element : node;
        if (!(dev instanceof mods.eln.sim.fluid.FluidDevice d) || d.fluidPorts().isEmpty()) {
            src.sendFailure(Component.literal("no fluid device at " + pos));
            return 0;
        }
        mods.eln.fluid.FluidWorld.traceNode = d.fluidPorts().get(0).node();
        mods.eln.fluid.FluidWorld.traceSteps = n;
        src.sendSuccess(() -> Component.literal("tracing " + n + " steps (server log, 'fluidtrace')"), false);
        return n;
    }

    /** /elnlab nodes POS R: every EA node within R blocks (a cube), by position and kind (its element's class). */
    static int nodesNear(CommandSourceStack src, BlockPos pos, int r) {
        List<String> out = new ArrayList<>();
        for (var n : mods.eln.node.NodeManager.instance.getNodes()) {
            if (n.coordonate.world() != src.getLevel()) continue;
            var p = n.coordonate.pos();
            if (Math.abs(p.getX() - pos.getX()) > r || Math.abs(p.getY() - pos.getY()) > r || Math.abs(p.getZ() - pos.getZ()) > r) continue;
            String kind = n instanceof mods.eln.node.transparent.TransparentNode tn && tn.element != null ? tn.element.getClass().getSimpleName()
                : n.getClass().getSimpleName();
            if (kind.equals("VoxelNode") || kind.equals("SixNode")) continue;   // (cables)
            out.add(p.getX() + " " + p.getY() + " " + p.getZ() + " " + kind);
        }
        java.util.Collections.sort(out);
        String text = out.isEmpty() ? "no EA devices within " + r : String.join("; ", out);
        src.sendSuccess(() -> Component.literal(text), false);
        return out.size();
    }

    public static int inspect(CommandSourceStack src, BlockPos pos) {
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
                try {
                    out.add("- " + v.getWaila(true));
                } catch (RuntimeException e) {
                    out.add("- (waila: " + e + ")");
                }
                if (v.host != null) {
                    out.add("  host " + v.host + (v.carved == null ? "" : ", carved " + v.carved.cardinality() + " voxels"));
                    for (int[] b : v.hostBoxes()) out.add("  solid box " + java.util.Arrays.toString(b));
                }
                for (var p : v.parts) {
                    var t = new net.minecraft.nbt.CompoundTag();
                    p.write(t);
                    out.add("  " + p.kind() + " " + t);
                }
            }
            if (node instanceof mods.eln.node.six.SixNode six)
                for (var el : six.sideElementList) {
                    if (el == null) continue;
                    // (some devices have no Waila: a Hub, a repeater)
                    String place = el.slot >= 6 ? " slot " + el.slot + " at " + el.ox + "," + el.oy + "," + el.oz + ", front " + el.front
                        + ", pins " + (node instanceof mods.eln.node.voxel.VoxelNode v ? pinsOf(v, el) : "-") : "";
                    String waila;
                    try {
                        var w = el.getWaila();
                        waila = w == null ? "{}" : new TreeMap<>(w).toString();
                    } catch (RuntimeException e) {
                        waila = "(waila: " + e + ")";
                    }
                    out.add("- " + el.side + ": " + el.getClass().getSimpleName() + place + " " + waila);
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

    /** The pins of an off-centre device: edge (world direction), tip, kind mask, linked or not. */
    static String pinsOf(mods.eln.node.voxel.VoxelNode v, mods.eln.node.six.SixNodeElement e) {
        List<String> out = new ArrayList<>();
        for (int[] t : v.pinTips(e.slot)) {
            var l = mods.eln.misc.LRDU.fromInt(t[0]);
            out.add(e.side.applyLRDU(l) + "@" + t[1] + "," + t[2] + "," + t[3] + " mask " + Integer.toHexString(e.getConnectionMask(l))
                + (e.slotMask.get(l) ? " linked" : " open"));
        }
        return out.toString();
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
        int id = transparentId("48V Macerator");
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

    /**
     * The voxel part gallery (board #44, for a client's look at the checked-model fixes): on a smooth stone pad at pos
     * (its corner, the pad at pos's level; by default the block under the caller), the legend to chat and to eln-voxelgallery.txt.
     */
    static int voxelGallery(CommandSourceStack src, BlockPos origin) {
        WorldTestContext ctx = new WorldTestContext(src.getLevel(), origin);
        var r = mods.eln.gametest.cases.VoxelGallery.place(ctx);
        StringBuilder b = new StringBuilder("Voxel gallery at " + origin.toShortString() + " (offsets x y z from it; the pad at y +0)\n");
        for (String l : r.legend()) b.append(l).append("\n");
        for (String e : r.errors()) b.append("FAILED ").append(e).append("\n");
        try {
            java.nio.file.Files.writeString(java.nio.file.Path.of("eln-voxelgallery.txt"), b.toString());
        } catch (IOException e) {
            src.sendFailure(Component.literal("legend not written: " + e));
        }
        for (String l : b.toString().split("\n")) src.sendSuccess(() -> Component.literal(l), false);
        src.sendSuccess(() -> Component.literal("voxelgallery: " + r.legend().size() + " blocks, " + r.errors().size() + " failed; legend in eln-voxelgallery.txt"), true);
        return r.legend().size() - r.errors().size();
    }
}
