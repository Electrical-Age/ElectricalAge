package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.network.ElnNet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the next click with a cable or pipe kit would do (RoutePlacer, through the kit's spec), shown as outlines (Baughn 2026-10-18: no preview of where it goes;
 * like the chisel's): before a route starts, the end it would start at; during one, the whole route to where the
 * player aims (the router runs on the server, so a few times a second, and only when the aim or the plan changes).
 * Sent as an EA packet (Eln.packetKitPreview): ok, then boxes in world voxels; drawn by client.render.KitOutline.
 */
public final class KitPreview {
    private KitPreview() {
    }

    /** Per player: what was last computed (key) and sent. */
    private record Last(String key, boolean sentEmpty) {}

    private static final Map<UUID, Last> LAST = new HashMap<>();

    public static void forget(UUID player) {
        LAST.remove(player);
        GEN.remove(player);
        SNAPS.remove(player);
    }

    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return;
        RoutePlacer.checkLapse(p, level.getGameTime());   // (every tick: Baughn found the outline slow at 4 ticks)
        if (ToolbagShears.tick(p, level)) return;   // (the Toolbag's shears outline the run to cut instead)
        Last last = LAST.get(p.getUUID());
        KitSupply kit = KitSupply.held(p);
        if (kit == null) {
            if (last != null && !last.sentEmpty) {
                send(p, true, List.of());
                LAST.put(p.getUUID(), new Last("", true));
            }
            return;
        }
        RoutePlacer.Spec k = RoutePlacer.of(kit);
        HitResult h = p.pick(p.blockInteractionRange(), 1f, false);
        RoutePlacer.Plan plan = RoutePlacer.plan(p.getUUID());
        // (a route of another kit is no route here; the lapse check above has dropped it already)
        if (plan != null && (plan.kind != k || plan.tier != kit.tier() || plan.dim != level.dimension())) plan = null;
        String key = h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK
            ? bh.getBlockPos().toShortString() + bh.getDirection() + (int) Math.floor(bh.getLocation().x * 8) + "," + (int) Math.floor(bh.getLocation().y * 8)
            + "," + (int) Math.floor(bh.getLocation().z * 8) : "none";
        key = k.noun + "|" + key + "|" + kit.tier() + "|" + kit.spec() + "|" + kit.length() + "|"
            + (plan == null ? "-" : plan.start + "" + plan.waypoints.size() + plan.touched) + "|" + p.isSecondaryUseActive();
        if (last != null && last.key.equals(key)) return;
        LAST.put(p.getUUID(), new Last(key, false));
        long gen = GEN.merge(p.getUUID(), 1L, Long::sum);
        long tAim = System.nanoTime();
        boolean debug = k.telemetry && RouteDebug.on(p);
        List<int[]> boxes = new ArrayList<>();
        if (!(h instanceof BlockHitResult bh) || h.getType() != HitResult.Type.BLOCK || p.isSecondaryUseActive()) {
            send(p, true, boxes);
            return;
        }
        RoutePlacer.Click c = k.endAt(level, bh.getBlockPos(), bh.getDirection(), bh.getLocation(), kit.tier(), plan == null, false);
        if (c.at() == null) {
            send(p, false, boxes);
            if (c.refusal() != null) p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(c.refusal()));
            return;
        }
        if (plan == null) {   // (where it would start: at once)
            boxes.add(k.bigStartMarker || c.port() ? port(c.at()) : marker(c.at()));
            send(p, true, boxes, c.port());
            return;
        }
        List<CableRouter.End> wpsNow = new ArrayList<>(plan.waypoints);
        CableRouter.End end = c.at();
        // (aimed at the last waypoint: the route ends there, as the click would)
        if (k.previewAgain && !c.finish() && c.join() == null && !wpsNow.isEmpty() && RoutePlacer.same(wpsNow.get(wpsNow.size() - 1), end))
            end = wpsNow.remove(wpsNow.size() - 1);
        // the route: on the preview thread, over a snapshot of the blocks round its ends (Baughn: the router on the server
        // thread made the preview slow); the latest aim wins
        long tSnap = System.nanoTime();
        List<long[]> ends = new ArrayList<>();
        for (CableRouter.End w : plan.waypoints) ends.add(new long[]{w.x(), w.y(), w.z()});
        ends.add(new long[]{plan.start.x(), plan.start.y(), plan.start.z()});
        ends.add(new long[]{end.x(), end.y(), end.z()});
        RouteView view = snapshotAround(p, level, ends);
        long snapMs = (System.nanoTime() - tSnap) / 1_000_000;
        String from = plan.startJoin != null ? "from a " + k.noun : "";
        if (k.telemetry) slow("preview snapshot (server thread)", tSnap, from);
        long tQueued = System.nanoTime();
        int tier = plan.tier, have = p.isCreative() ? Integer.MAX_VALUE : kit.length();
        CableSpec spec = plan.spec;
        List<CableRouter.End> wps = List.copyOf(wpsNow);
        CableRouter.End start = plan.start, to = end;
        long[] startJoin = plan.startJoin, endJoin = c.join();
        UUID id = p.getUUID();
        WORKER.execute(() -> {
            if (GEN.getOrDefault(id, 0L) != gen) return;   // (aimed elsewhere since)
            long tRoute = System.nanoTime();
            Answer a;
            try {
                a = k.answer(view, tier, spec, start, startJoin, wps, to, endJoin, have, c);
            } catch (RuntimeException ex) {
                Eln.LOG.warn(k.title() + " route preview failed", ex);
                return;
            }
            int exp = Router.LAST_EXPANSIONS.get();
            long waitMs = (tRoute - tQueued) / 1_000_000, routeMs = (System.nanoTime() - tRoute) / 1_000_000;
            if (k.telemetry) slow("preview route (worker)", tRoute, (from.isEmpty() ? "" : from + ", ") + exp + " expansions");
            // sent from here, not handed back to the server thread: that waited for the next tick, 40-90 ms of every
            // update (the route debug kit found it; Baughn's slow start). The connection's send is thread-safe
            if (GEN.getOrDefault(id, 0L) != gen) return;
            if (p.hasDisconnected()) return;
            List<int[]> bs = a.boxes;
            if (k.portCube && c.port() && a.ok) {
                bs = new ArrayList<>(bs);
                bs.add(port(to));
            }
            send(p, a.ok, bs, c.port() && a.ok, debug);
            if (debug)
                RouteDebug.report(p, (System.nanoTime() - tAim) / 1_000_000, snapMs, waitMs, routeMs, exp, startJoin != null,
                    RouteDebug.token(tier, start, startJoin, wps, to, endJoin));
            // (always a line: a "can't reach" left over from a moment ago read as if the route drawn now were wrong)
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(a.message));
        });
    }

    /**
     * Why a route click failed, on its own line above the action bar for a few seconds (Baughn 2026-11-18, #521: the
     * preview's next line, the route's length, replaced it on the action bar at once; client.RouteNoteHud).
     */
    public static void note(ServerPlayer p, String message) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(Eln.packetRouteNote);
            out.writeUTF(message.length() > 300 ? message.substring(0, 300) : message);
        } catch (IOException ignored) {
        }
        ElnNet.sendToPlayer(p, bos);
    }

    /** Log a step that took long (Baughn 2026-11-12: routes from a cable were slow to start; where does the time go?). */
    static void slow(String what, long t0, String detail) {
        long ms = (System.nanoTime() - t0) / 1_000_000;
        if (ms >= SLOW_MS) mods.eln.Eln.LOG.info("[eln route timing] {}: {} ms {}", what, ms, detail);
    }

    static final long SLOW_MS = 30;

    /** What the preview shows for a route: ok or not, its boxes (or a marker at the end), and the action-bar line. */
    public record Answer(boolean ok, List<int[]> boxes, String message) {}

    /** The route to {@code at}, over a view of the world (the preview thread). */
    public static Answer answer(RouteView view, int tier, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps, CableRouter.End at,
                         long[] endJoin, int have) {
        return answer(view, tier, null, start, startJoin, wps, at, endJoin, have);
    }

    public static Answer answer(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                CableRouter.End at, long[] endJoin, int have) {
        CableRouter.Space sp = CablePlacer.space(view);
        if (startJoin != null) sp = CablePlacer.around(sp, startJoin);
        if (endJoin != null) sp = CablePlacer.around(sp, endJoin);
        CablePart.Tier t = CablePlacer.tierOf(tier, spec);
        CableRouter.Result r = CableRouter.route(sp, t.width(), t.height(), start, wps, at, PREVIEW_BUDGET_MS);
        List<int[]> boxes = new ArrayList<>();
        if (r.error != null) {
            boxes.add(marker(at));
            return new Answer(false, boxes, "Can't reach there: " + r.error + " (sneak-click to cancel the route)");
        }
        for (CableRouter.Piece pc : CableRouter.pieces(r.steps, t.width(), t.height())) {
            CablePart part = pc.face() < 0 ? CablePart.knuckle(tier, spec, pc.points()[0], pc.points()[1], pc.points()[2]) : new CablePart(tier, spec, pc.face(), pc.points());
            for (int[] b : part.boxes())
                boxes.add(new int[]{(int) pc.bx() * 16 + b[0], (int) pc.by() * 16 + b[1], (int) pc.bz() * 16 + b[2],
                    (int) pc.bx() * 16 + b[3], (int) pc.by() * 16 + b[4], (int) pc.bz() * 16 + b[5]});
        }
        int len = r.length();
        if (have < len)
            return new Answer(false, boxes, String.format("Too long: %.1f blocks of cable, the kit holds %.1f", len / 16.0, have / 16.0));
        return new Answer(true, boxes, String.format("Route: %.1f blocks of cable", len / 16.0));
    }

    /** The preview thread's time for one route (more than the click's: it doesn't hold up the server). */
    static final long PREVIEW_BUDGET_MS = 1500;

    /** Per player: the generation of the latest preview asked for (a newer one makes older answers stale). */
    private static final Map<UUID, Long> GEN = new java.util.concurrent.ConcurrentHashMap<>();

    /** One thread: previews queue behind each other, and stale ones are skipped. */
    private static final java.util.concurrent.ExecutorService WORKER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Electrical Age route preview");
        t.setDaemon(true);
        return t;
    });

    /** A snapshot round the route's ends, reused for a second while it covers them (blocks rarely change mid-aim). */
    private record Snap(RouteView view, int x0, int y0, int z0, int x1, int y1, int z1, long taken) {}

    private static final Map<UUID, Snap> SNAPS = new HashMap<>();

    /** A snapshot of the blocks round these route ends (world voxels), reused while the aim stays inside it. */
    static RouteView snapshotAround(ServerPlayer p, ServerLevel level, List<long[]> ends) {
        return snapshotAround(p, level, ends, false);
    }

    /** ... fresh: always a new snapshot (RouteDebug's replay times one). */
    static RouteView snapshotAround(ServerPlayer p, ServerLevel level, List<long[]> ends, boolean fresh) {
        long[] lo = {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}, hi = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        for (long[] v : ends) {
            for (int k = 0; k < 3; k++) {
                lo[k] = Math.min(lo[k], Math.floorDiv(v[k], 16));
                hi[k] = Math.max(hi[k], Math.floorDiv(v[k], 16));
            }
        }
        int m = Math.max(CableRouter.MARGIN, PipeRouter.MARGIN) / 16 + 2;   // (pipes search further round their legs)
        long now = level.getGameTime();
        Snap old = SNAPS.get(p.getUUID());
        if (!fresh && old != null && now - old.taken < 20 && old.x0 <= lo[0] - m && old.y0 <= lo[1] - m && old.z0 <= lo[2] - m
            && old.x1 >= hi[0] + m && old.y1 >= hi[1] + m && old.z1 >= hi[2] + m) return old.view;
        int pad = m + 8;   // (room for the aim to wander without a new snapshot)
        int x0 = (int) lo[0] - pad, y0 = (int) lo[1] - m, z0 = (int) lo[2] - pad, x1 = (int) hi[0] + pad, y1 = (int) hi[1] + m, z1 = (int) hi[2] + pad;
        RouteView v = RouteView.snapshot(level, x0, y0, z0, x1, y1, z1);
        SNAPS.put(p.getUUID(), new Snap(v, x0, y0, z0, x1, y1, z1, now));
        return v;
    }

    /** A small cube round an end point (world voxels). */
    static int[] marker(CableRouter.End at) {
        return new int[]{(int) at.x() - 1, (int) at.y() - 1, (int) at.z() - 1, (int) at.x() + 1, (int) at.y() + 1, (int) at.z() + 1};
    }

    /** A machine's port: a bigger cube centred on where the cable meets it. */
    static int[] port(CableRouter.End at) {
        return new int[]{(int) at.x() - 2, (int) at.y() - 2, (int) at.z() - 2, (int) at.x() + 2, (int) at.y() + 2, (int) at.z() + 2};
    }

    static void send(ServerPlayer p, boolean ok, List<int[]> boxes) {
        send(p, ok, boxes, false);
    }

    /** state: 0 won't work, 1 works, 2 works and connects to a machine (KitOutline's colours). */
    static void send(ServerPlayer p, boolean ok, List<int[]> boxes, boolean device) {
        send(p, ok, boxes, device, false);
    }

    /** ... debug: the client says when it first drew this outline (RouteDebug; a trailing byte, read if there). */
    static void send(ServerPlayer p, boolean ok, List<int[]> boxes, boolean device, boolean debug) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(Eln.packetKitPreview);
            out.writeByte(!ok ? 0 : device ? 2 : 1);
            int n = Math.min(boxes.size(), 2000);
            out.writeShort(n);
            for (int i = 0; i < n; i++) for (int v : boxes.get(i)) out.writeInt(v);
            out.writeByte(debug ? 1 : 0);
        } catch (IOException ignored) {
        }
        ElnNet.sendToPlayer(p, bos);
    }

    /** A pipe route's preview: the pipe as it would be laid from start over the waypoints to end (world voxels). */
    public static Answer pipeAnswer(RouteView view, long[] join, int tier, PipeRouter.End start, List<PipeRouter.End> wps, PipeRouter.End end,
                                    int have, boolean toPort) {
        return pipeAnswer(view, join, null, tier, start, wps, end, have, toPort);
    }

    /** ... free round a start on a pipe too (startJoin, or null). */
    public static Answer pipeAnswer(RouteView view, long[] join, long[] startJoin, int tier, PipeRouter.End start, List<PipeRouter.End> wps,
                                    PipeRouter.End end, int have, boolean toPort) {
        PipeRouter.Space sp = RouteView.obstacles(view);
        if (join != null) sp = RouteView.freeAround(sp, join, PipePart.JOIN_CLEAR);
        if (startJoin != null) sp = RouteView.freeAround(sp, startJoin, PipePart.JOIN_CLEAR);
        PipeRouter.Result r = PipeRouter.route(sp, PipePart.GRID[tier], start, wps, end, PREVIEW_BUDGET_MS);
        List<int[]> bs = new ArrayList<>();
        boolean ok = r.error == null;
        String msg;
        if (!ok) {
            bs.add(new int[]{(int) end.x() - 1, (int) end.y() - 1, (int) end.z() - 1, (int) end.x() + 1, (int) end.y() + 1, (int) end.z() + 1});
            msg = "Can't reach there: " + r.error + " (sneak-click to cancel the route)";
        } else {
            for (var e : PipeRouter.perBlock(r.points).entrySet()) {
                long bx = e.getKey().get(0), by = e.getKey().get(1), bz = e.getKey().get(2);
                for (int[] pts : e.getValue())
                    for (int[] b : new PipePart(tier, pts).boxes())
                        bs.add(new int[]{(int) bx * 16 + b[0], (int) by * 16 + b[1], (int) bz * 16 + b[2], (int) bx * 16 + b[3], (int) by * 16 + b[4], (int) bz * 16 + b[5]});
            }
            int len = r.length();
            if (have < len) {
                ok = false;
                msg = String.format("Too long: %.1f blocks of pipe, the kit holds %.1f", len / 16.0, have / 16.0);
            } else msg = String.format("Route: %.1f blocks of pipe", len / 16.0) + (toPort ? "" : join != null ? " (joins the pipe)" : " (waypoint)");
        }
        return new Answer(ok, bs, msg);
    }

    /** pipeAnswer from start to end with no waypoints, on a snapshot round them (GameTests). */
    public static Answer pipeAnswerForTests(ServerLevel level, int tier, PipeRouter.End start, PipeRouter.End end) {
        int m = PipeRouter.MARGIN / 16 + 2;
        int x0 = (int) Math.floorDiv(Math.min(start.x(), end.x()), 16) - m, y0 = (int) Math.floorDiv(Math.min(start.y(), end.y()), 16) - m;
        int z0 = (int) Math.floorDiv(Math.min(start.z(), end.z()), 16) - m, x1 = (int) Math.floorDiv(Math.max(start.x(), end.x()), 16) + m;
        int y1 = (int) Math.floorDiv(Math.max(start.y(), end.y()), 16) + m, z1 = (int) Math.floorDiv(Math.max(start.z(), end.z()), 16) + m;
        return pipeAnswer(RouteView.snapshot(level, x0, y0, z0, x1, y1, z1), null, tier, start, List.of(), end, Integer.MAX_VALUE, false);
    }
}
