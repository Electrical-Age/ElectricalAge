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
                .then(Commands.literal("cables").then(Commands.argument("n", IntegerArgumentType.integer(1, 5000))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> cables(c.getSource(), IntegerArgumentType.getInteger(c, "n"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))
                .then(Commands.literal("machines").then(Commands.argument("n", IntegerArgumentType.integer(1, 500))
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> machines(c.getSource(), IntegerArgumentType.getInteger(c, "n"), BlockPosArgument.getLoadedBlockPos(c, "pos"))))))));
    }

    public static int sixId(String name) {
        for (var e : Eln.sixNodeItem.all().entrySet()) if (e.getValue().name.equals(name)) return e.getKey();
        throw new IllegalStateException("no six-node device " + name);
    }

    public static int transparentId(String name) {
        for (var e : Eln.transparentNodeItem.all().entrySet()) if (e.getValue().name.equals(name)) return e.getKey();
        throw new IllegalStateException("no transparent device " + name);
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
