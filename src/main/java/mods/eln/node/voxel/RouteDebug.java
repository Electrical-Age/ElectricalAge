package mods.eln.node.voxel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

/**
 * The route debug kit (Baughn 2026-11-12: "a debug kit, creative only, which outputs timing statistics plus a command to
 * replicate it"): a cable kit tagged by /elnlab debugkit. In a creative player's hand, every route preview it shows says
 * in chat where its time went (the snapshot on the server thread, the wait for the worker, the route itself, the whole
 * aim-to-packet time), the client adds when the outline was first drawn after the packet came, and a clickable
 * /elnlab routereplay command runs the same route again, on the same world, timed both ways (the preview's snapshot and
 * the click's live world).
 */
public final class RouteDebug {
    private RouteDebug() {
    }

    static final String TAG = "elnRouteDebug";

    /** Is this player holding the debug kit (creative only)? */
    public static boolean on(ServerPlayer p) {
        if (!p.isCreative()) return false;
        ItemStack s = p.getMainHandItem();
        return s.getItem() instanceof CableKitItem && s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(TAG, false);
    }

    /** /elnlab debugkit: tag the held cable kit (a copy, named). */
    public static String give(ServerPlayer p) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof CableKitItem)) return "Hold a cable kit";
        ItemStack d = s.copy();
        CustomData.update(DataComponents.CUSTOM_DATA, d, t -> t.putBoolean(TAG, true));
        d.set(DataComponents.CUSTOM_NAME, Component.literal("Route debug kit"));
        if (!p.getInventory().add(d)) net.minecraft.world.Containers.dropItemStack(p.level(), p.getX(), p.getY(), p.getZ(), d);
        return "Given: Route debug kit (works in creative)";
    }

    /** The replay token for a route: tier, the start, its join, the waypoints, the end and its join. */
    static String token(int tier, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps, CableRouter.End at, long[] endJoin) {
        List<Long> v = new ArrayList<>();
        v.add((long) tier);
        end(v, start);
        join(v, startJoin);
        v.add((long) wps.size());
        for (CableRouter.End e : wps) end(v, e);
        end(v, at);
        join(v, endJoin);
        StringBuilder sb = new StringBuilder();
        for (Long x : v) sb.append(sb.isEmpty() ? "" : ".").append(Long.toString(x, 36));
        return sb.toString();
    }

    private static void end(List<Long> v, CableRouter.End e) {
        v.add(e.x());
        v.add(e.y());
        v.add(e.z());
        v.add((long) e.attach());
        v.add((long) e.dir());
    }

    private static void join(List<Long> v, long[] j) {
        v.add(j == null ? 0L : 1L);
        if (j != null) for (long x : j) v.add(x);
    }

    /** Tell the player where a preview's time went, with the replay command (clickable). */
    static void report(ServerPlayer p, long aimToSentMs, long snapMs, long waitMs, long routeMs, int expansions, boolean fromCable, String token) {
        p.sendSystemMessage(Component.literal(String.format("[route debug] aim->packet %d ms: snapshot %d (server thread), wait %d, route %d (%d expansions)%s",
            aimToSentMs, snapMs, waitMs, routeMs, expansions, fromCable ? ", from a cable" : "")));
        String cmd = "/elnlab routereplay " + token;
        p.sendSystemMessage(Component.literal("[route debug] replay: " + cmd).withStyle(Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand(cmd))));
    }

    /** /elnlab routereplay TOKEN: the same route on this world, timed as the preview does it and as the click does. */
    public static String replay(ServerLevel level, net.minecraft.world.entity.player.Player player, String token) {
        String[] f = token.split("\\.");
        long[] v = new long[f.length];
        for (int i = 0; i < f.length; i++) v[i] = Long.parseLong(f[i], 36);
        int[] i = {0};
        int tier = (int) v[i[0]++];
        CableRouter.End start = readEnd(v, i);
        long[] sj = readJoin(v, i);
        int nw = (int) v[i[0]++];
        List<CableRouter.End> wps = new ArrayList<>();
        for (int k = 0; k < nw; k++) wps.add(readEnd(v, i));
        CableRouter.End at = readEnd(v, i);
        long[] ej = readJoin(v, i);
        List<long[]> ends = new ArrayList<>();
        ends.add(new long[]{start.x(), start.y(), start.z()});
        for (CableRouter.End e : wps) ends.add(new long[]{e.x(), e.y(), e.z()});
        ends.add(new long[]{at.x(), at.y(), at.z()});
        long t0 = System.nanoTime();
        RouteView view = player instanceof ServerPlayer sp ? KitPreview.snapshotAround(sp, level, ends, true) : RouteView.live(level);
        long snap = (System.nanoTime() - t0) / 1_000_000;
        t0 = System.nanoTime();
        KitPreview.Answer a = KitPreview.answer(view, tier, null, start, sj, wps, at, ej, Integer.MAX_VALUE);
        long pre = (System.nanoTime() - t0) / 1_000_000;
        int preExp = Router.LAST_EXPANSIONS.get();
        CableRouter.Space s = CablePlacer.space(level);
        if (sj != null) s = CablePlacer.around(s, sj);
        if (ej != null) s = CablePlacer.around(s, ej);
        CablePart.Tier t = CablePlacer.tierOf(tier, null);
        t0 = System.nanoTime();
        CableRouter.Result r = CableRouter.route(s, t.width(), t.height(), start, wps, at);
        long live = (System.nanoTime() - t0) / 1_000_000;
        return String.format("replay: snapshot %d ms, preview route %d ms (%d expansions, %s); click route on the live world %d ms (%d expansions, %s)",
            snap, pre, preExp, a.ok() ? "ok" : a.message(), live, Router.LAST_EXPANSIONS.get(), r.error == null ? "ok" : r.error);
    }

    private static CableRouter.End readEnd(long[] v, int[] i) {
        CableRouter.End e = new CableRouter.End(v[i[0]], v[i[0] + 1], v[i[0] + 2], (int) v[i[0] + 3], (int) v[i[0] + 4]);
        i[0] += 5;
        return e;
    }

    private static long[] readJoin(long[] v, int[] i) {
        if (v[i[0]++] == 0) return null;
        long[] j = {v[i[0]], v[i[0] + 1], v[i[0] + 2]};
        i[0] += 3;
        return j;
    }
}
