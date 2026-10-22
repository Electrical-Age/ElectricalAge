package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.network.ElnNet;
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
 * What the next click with a cable kit would do, shown as outlines (Baughn 2026-10-18: no preview of where it goes;
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
        CablePlacer.checkLapse(p, level.getGameTime());   // (every tick: Baughn found the outline slow at 4 ticks)
        Last last = LAST.get(p.getUUID());
        if (!(p.getMainHandItem().getItem() instanceof CableKitItem kit)) {
            if (last != null && !last.sentEmpty) {
                send(p, true, List.of());
                LAST.put(p.getUUID(), new Last("", true));
            }
            return;
        }
        HitResult h = p.pick(p.blockInteractionRange(), 1f, false);
        CablePlacer.Plan plan = CablePlacer.PLANS.get(p.getUUID());
        String key = h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK
            ? bh.getBlockPos().toShortString() + bh.getDirection() + (int) Math.floor(bh.getLocation().x * 8) + "," + (int) Math.floor(bh.getLocation().y * 8)
            + "," + (int) Math.floor(bh.getLocation().z * 8) : "none";
        key += "|" + kit.tier + "|" + (plan == null ? "-" : plan.start + "" + plan.waypoints.size() + plan.touched) + "|" + p.isSecondaryUseActive();
        if (last != null && last.key.equals(key)) return;
        LAST.put(p.getUUID(), new Last(key, false));
        long gen = GEN.merge(p.getUUID(), 1L, Long::sum);
        List<int[]> boxes = new ArrayList<>();
        if (!(h instanceof BlockHitResult bh) || h.getType() != HitResult.Type.BLOCK || p.isSecondaryUseActive()) {
            send(p, true, boxes);
            return;
        }
        long[][] join = new long[1][];
        CableRouter.End at = CablePlacer.endAt(level, bh.getBlockPos(), bh.getDirection(), bh.getLocation(), plan == null, join);
        // a machine's port: snapped to it and in its own colour (Baughn: it wasn't obvious that cables connect to machines)
        boolean device = at != null && CablePlacer.isDevice(level, bh.getBlockPos(), bh.getLocation());
        if (at == null) {
            send(p, false, boxes);
            p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Not a place for a cable"));
            return;
        }
        if (plan == null || plan.tier != kit.tier) {   // (where it would start: at once)
            boxes.add(device ? port(at) : marker(at));
            send(p, true, boxes, device);
            return;
        }
        // the route: on the preview thread, over a snapshot of the blocks round its ends (Baughn: the router on the server
        // thread made the preview slow); the latest aim wins
        RouteView view = snapshotFor(p, level, plan, at);
        int tier = kit.tier, have = p.isCreative() ? Integer.MAX_VALUE : KitItem.length(p.getMainHandItem());
        List<CableRouter.End> wps = List.copyOf(plan.waypoints);
        CableRouter.End start = plan.start;
        long[] startJoin = plan.startJoin, endJoin = join[0];
        UUID id = p.getUUID();
        var server = level.getServer();
        WORKER.execute(() -> {
            if (GEN.getOrDefault(id, 0L) != gen) return;   // (aimed elsewhere since)
            Answer a = answer(view, tier, start, startJoin, wps, at, endJoin, have);
            server.execute(() -> {
                if (GEN.getOrDefault(id, 0L) != gen) return;
                ServerPlayer now = server.getPlayerList().getPlayer(id);
                if (now == null) return;
                List<int[]> bs = a.boxes;
                if (device && a.ok) {
                    bs = new ArrayList<>(bs);
                    bs.add(port(at));
                }
                send(now, a.ok, bs, device && a.ok);
                // (always a line: a "can't reach" left over from a moment ago read as if the route drawn now were wrong)
                now.sendOverlayMessage(net.minecraft.network.chat.Component.literal(a.message));
            });
        });
    }

    /** What the preview shows for a route: ok or not, its boxes (or a marker at the end), and the action-bar line. */
    public record Answer(boolean ok, List<int[]> boxes, String message) {}

    /** The route to {@code at}, over a view of the world (the preview thread). */
    public static Answer answer(RouteView view, int tier, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps, CableRouter.End at,
                         long[] endJoin, int have) {
        CableRouter.Space sp = CablePlacer.space(view);
        if (startJoin != null) sp = CablePlacer.around(sp, startJoin);
        if (endJoin != null) sp = CablePlacer.around(sp, endJoin);
        CablePart.Tier t = CablePart.TIERS[tier];
        CableRouter.Result r = CableRouter.route(sp, t.width(), t.height(), start, wps, at, PREVIEW_BUDGET_MS);
        List<int[]> boxes = new ArrayList<>();
        if (r.error != null) {
            boxes.add(marker(at));
            return new Answer(false, boxes, "Can't reach there: " + r.error);
        }
        for (CableRouter.Piece pc : CableRouter.pieces(r.steps, t.width(), t.height())) {
            CablePart part = pc.face() < 0 ? CablePart.knuckle(tier, pc.points()[0], pc.points()[1], pc.points()[2]) : new CablePart(tier, pc.face(), pc.points());
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

    static RouteView snapshotFor(ServerPlayer p, ServerLevel level, CablePlacer.Plan plan, CableRouter.End at) {
        long[] lo = {Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE}, hi = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        List<CableRouter.End> ends = new ArrayList<>(plan.waypoints);
        ends.add(plan.start);
        ends.add(at);
        for (CableRouter.End e : ends) {
            long[] v = {e.x(), e.y(), e.z()};
            for (int k = 0; k < 3; k++) {
                lo[k] = Math.min(lo[k], Math.floorDiv(v[k], 16));
                hi[k] = Math.max(hi[k], Math.floorDiv(v[k], 16));
            }
        }
        int m = CableRouter.MARGIN / 16 + 2;
        long now = level.getGameTime();
        Snap old = SNAPS.get(p.getUUID());
        if (old != null && now - old.taken < 20 && old.x0 <= lo[0] - m && old.y0 <= lo[1] - m && old.z0 <= lo[2] - m
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
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(Eln.packetKitPreview);
            out.writeByte(!ok ? 0 : device ? 2 : 1);
            int n = Math.min(boxes.size(), 2000);
            out.writeShort(n);
            for (int i = 0; i < n; i++) for (int v : boxes.get(i)) out.writeInt(v);
        } catch (IOException ignored) {
        }
        ElnNet.sendToPlayer(p, bos);
    }
}
