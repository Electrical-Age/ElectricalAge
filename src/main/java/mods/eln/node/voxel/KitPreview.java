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
        LAST_NOTE.remove(player);
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
        boolean debug = RouteDebug.on(p);
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
        // the route: on the preview thread, over a snapshot of the blocks round its ends (Baughn: the router on the server
        // thread made the preview slow); the latest aim wins
        long tSnap = System.nanoTime();
        Job job = job(p, level, kit, plan, c);
        long snapMs = (System.nanoTime() - tSnap) / 1_000_000;
        String from = plan.startJoin != null ? "from a " + k.noun : "";
        slow("preview snapshot (server thread)", tSnap, from);
        long tQueued = System.nanoTime();
        UUID id = p.getUUID();
        WORKER.execute(() -> {
            if (GEN.getOrDefault(id, 0L) != gen) return;   // (aimed elsewhere since)
            long tRoute = System.nanoTime();
            Answer a;
            try {
                a = job.answer();
            } catch (RuntimeException ex) {
                Eln.LOG.warn(k.title() + " route preview failed", ex);
                return;
            }
            int exp = Router.LAST_EXPANSIONS.get();
            long waitMs = (tRoute - tQueued) / 1_000_000, routeMs = (System.nanoTime() - tRoute) / 1_000_000;
            slow("preview route (worker)", tRoute, (from.isEmpty() ? "" : from + ", ") + exp + " expansions");
            // sent from here, not handed back to the server thread: that waited for the next tick, 40-90 ms of every
            // update (the route debug kit found it; Baughn's slow start). The connection's send is thread-safe
            if (GEN.getOrDefault(id, 0L) != gen) return;
            if (p.hasDisconnected()) return;
            send(p, a.ok, a.boxes, c.port() && a.ok, debug);
            if (debug)
                RouteDebug.report(p, (System.nanoTime() - tAim) / 1_000_000, snapMs, waitMs, routeMs, exp, job.startJoin() != null ? k.noun : null,
                    RouteDebug.token(k, job.tier(), job.start(), job.startJoin(), job.wps(), job.to(), job.endJoin()));
            // (always a line: a "can't reach" left over from a moment ago read as if the route drawn now were wrong)
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(a.message));
        });
    }

    /** A route preview to work out on the preview thread: the plan's route to the aim (to), over a snapshot round it. */
    record Job(RoutePlacer.Spec<?> k, RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
               CableRouter.End to, long[] endJoin, int have, RoutePlacer.Click c, boolean again, String refused) {
        /**
         * The answer: the click's refusal if it would be refused (RoutePlacer.startRefusal), else the route; one that
         * works says what the click would do there: connect to a machine, join the cable or pipe aimed at, end at the
         * last waypoint (an open end), or add a waypoint (Baughn: the pipes' suffix, for both kinds; wording Neon's:
         * "Cable: 7.5 blocks \u2014 joins").
         */
        Answer answer() {
            if (refused != null) return new Answer(false, new ArrayList<>(List.of(marker(to))), refused);
            Answer a = KitPreview.answer(k, view, tier, spec, start, startJoin, wps, to, endJoin, have);
            // (a waypoint still on the old run the route starts on: nothing laid yet, but the click takes it)
            if (!a.ok && RoutePlacer.INSIDE.equals(a.message) && !c.finish() && c.join() == null && !again)
                return new Answer(true, new ArrayList<>(List.of(marker(to))), mods.eln.i18n.I18N.tr("%1$: 0.0 blocks", k.title()) + suffix(false, false, false));
            if (!a.ok) return a;
            List<int[]> bs = a.boxes;
            if (k.portCube && c.port()) {
                bs = new ArrayList<>(bs);
                bs.add(port(to));
            }
            return new Answer(true, bs, a.message + k.suffix(again, c.join() != null, c.finish()));
        }
    }

    /** The preview's route for the player's plan to the aim c (its snapshot taken now, on the server thread). */
    static Job job(ServerPlayer p, ServerLevel level, KitSupply kit, RoutePlacer.Plan plan, RoutePlacer.Click c) {
        List<CableRouter.End> wps = new ArrayList<>(plan.waypoints);
        CableRouter.End to = c.at();
        // (aimed at the last waypoint: the route ends there, as the click would)
        boolean again = RoutePlacer.again(plan, c);
        if (again) to = wps.remove(wps.size() - 1);
        String refused = RoutePlacer.startRefusal(plan, c, again, kit);
        RouteView view = null;
        if (refused == null) {
            List<long[]> ends = new ArrayList<>();
            for (CableRouter.End w : plan.waypoints) ends.add(new long[]{w.x(), w.y(), w.z()});
            ends.add(new long[]{plan.start.x(), plan.start.y(), plan.start.z()});
            ends.add(new long[]{to.x(), to.y(), to.z()});
            view = snapshotAround(p, level, ends);
        }
        int have = p.isCreative() ? Integer.MAX_VALUE : kit.length();
        return new Job(plan.kind, view, plan.tier, plan.spec, plan.start, plan.startJoin, List.copyOf(wps), to, c.join(), have, c, again, refused);
    }

    /** What the click would do, after the preview's length: " \u2014 open end", " \u2014 joins", " \u2014 connects" or " \u2014 waypoint". */
    static String suffix(boolean again, boolean join, boolean finish) {
        return again ? " \u2014 " + mods.eln.i18n.I18N.tr("open end")
            : join ? " \u2014 " + mods.eln.i18n.I18N.tr("joins")
            : finish ? " \u2014 " + mods.eln.i18n.I18N.tr("connects")
            : " \u2014 " + mods.eln.i18n.I18N.tr("waypoint");
    }

    /**
     * The preview's action-bar line for the player aiming at hit on face of pos with kit, worked out at once (GameTests):
     * why there is no end there, the route's line, or null (no route started and nothing to say).
     */
    public static String previewForTests(ServerPlayer p, KitSupply kit, BlockPos pos, net.minecraft.core.Direction face, net.minecraft.world.phys.Vec3 hit) {
        ServerLevel level = (ServerLevel) p.level();
        RoutePlacer.Plan plan = RoutePlacer.plan(p.getUUID());
        if (plan != null && plan.kind != RoutePlacer.of(kit)) plan = null;
        RoutePlacer.Click c = RoutePlacer.of(kit).endAt(level, pos, face, hit, kit.tier(), plan == null, false);
        if (c.at() == null) return c.refusal();
        return plan == null ? null : job(p, level, kit, plan, c).answer().message;
    }

    /**
     * Why a route click failed, on its own line above the action bar for a few seconds (Baughn 2026-11-18, #521: the
     * preview's next line, the route's length, replaced it on the action bar at once; client.RouteNoteHud).
     */
    public static void note(ServerPlayer p, String message) {
        LAST_NOTE.put(p.getUUID(), message);
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
    /** The last note sent to each player (GameTests read what a click said). */
    private static final Map<UUID, String> LAST_NOTE = new java.util.concurrent.ConcurrentHashMap<>();

    public static String lastNoteForTests(UUID player) {
        return LAST_NOTE.get(player);
    }

    public static void clearNoteForTests(UUID player) {
        LAST_NOTE.remove(player);
    }

    public record Answer(boolean ok, List<int[]> boxes, String message) {}

    /** A cable route to {@code at}, over a view of the world (GameTests: the snapshot gives the live route). */
    public static Answer answer(RouteView view, int tier, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps, CableRouter.End at,
                                long[] endJoin, int have) {
        return answer(CablePlacer.ROUTE, view, tier, null, start, startJoin, wps, at, endJoin, have);
    }

    /** The kind's route to end over a view of the world (the preview thread): its outline and length, or why not. */
    static <P> Answer answer(RoutePlacer.Spec<P> k, RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin,
                             List<CableRouter.End> wps, CableRouter.End end, long[] endJoin, int have) {
        RoutePlacer.Routed<P> r = k.route(view, tier, spec, start, startJoin, wps, end, endJoin, PREVIEW_BUDGET_MS);
        if (r.error() != null) return new Answer(false, new ArrayList<>(List.of(marker(end))), RoutePlacer.unrouted(r));
        List<int[]> boxes = k.boxes(r, tier, spec);
        int len = r.length();
        if (have < len)
            return new Answer(false, boxes, mods.eln.i18n.I18N.tr("Need %1$ blocks of %2$ (kit has %3$)",
                String.format("%.1f", len / 16.0), k.noun, String.format("%.1f", have / 16.0)));
        return new Answer(true, boxes, mods.eln.i18n.I18N.tr("%1$: %2$ blocks", k.title(), String.format("%.1f", len / 16.0)));
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

    /** A pipe route's preview from start to end with no waypoints, on a snapshot round them (GameTests). */
    public static Answer pipeAnswerForTests(ServerLevel level, int tier, PipeRouter.End start, PipeRouter.End end) {
        int m = PipeRouter.MARGIN / 16 + 2;
        int x0 = (int) Math.floorDiv(Math.min(start.x(), end.x()), 16) - m, y0 = (int) Math.floorDiv(Math.min(start.y(), end.y()), 16) - m;
        int z0 = (int) Math.floorDiv(Math.min(start.z(), end.z()), 16) - m, x1 = (int) Math.floorDiv(Math.max(start.x(), end.x()), 16) + m;
        int y1 = (int) Math.floorDiv(Math.max(start.y(), end.y()), 16) + m, z1 = (int) Math.floorDiv(Math.max(start.z(), end.z()), 16) + m;
        return answer(PipePlacer.ROUTE, RouteView.snapshot(level, x0, y0, z0, x1, y1, z1), tier, null, PipePlacer.end(start), null, List.of(),
            PipePlacer.end(end), null, Integer.MAX_VALUE);
    }
}
