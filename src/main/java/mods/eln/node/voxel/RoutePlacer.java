package mods.eln.node.voxel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Laying a routed cable or pipe with a kit (or a Toolbag's kits of one kind): one flow for both kinds (Baughn: cables
 * have the better UX, pipes move onto the cable flow). Right-click to start where the kind's spec finds an end (a
 * machine's connector or port, a point on an old cable or pipe for a branch, or a point on the surface clicked);
 * right-click more surfaces for waypoints; right-click a machine, a cable or pipe, or the last waypoint again to finish.
 * Sneak-click takes back the last waypoint (or cancels). The route lapses on another item in hand, a minute without a
 * click, the player too far from it, or another dimension (lapse). The plan and the clicks are here; where a click
 * ends, how a route is found and laid, and what the player reads are the kind's {@link Spec} (CablePlacer.ROUTE,
 * PipePlacer.ROUTE). The preview of the next click is KitPreview.
 */
public final class RoutePlacer {
    private RoutePlacer() {
    }

    /**
     * A click (or the aim) as a route end (Spec.endAt): the end, or null with why not (refusal; null: say nothing); the
     * point on an old cable or pipe it joins (world voxels, or null); whether a click there finishes the route (a
     * machine's connector or port; a join always finishes); whether the preview marks it as a machine's port.
     */
    record Click(CableRouter.End at, long[] join, boolean finish, boolean port, String refusal) {
        static Click none(String refusal) {
            return new Click(null, null, false, false, refusal);
        }
    }

    /**
     * A route found, or why not (error); its length (voxels) and its path in the kind's own form P (cable steps, pipe
     * points); where it joins old runs at its start and end (world voxels, or null): the join clicked, or where the route
     * leaves the old run it ran inside near it (JoinRun).
     */
    record Routed<P>(String error, int length, P path, long[] startJoin, long[] endJoin) {
        Routed(String error, int length, P path) {
            this(error, length, path, null, null);
        }
    }

    /**
     * What differs between cables and pipes (P: the kind's route path). The ends are CableRouter.End for both kinds (a
     * pipe's: attach -1, dir as a face, 3D value), adapted to the kind's router inside its hooks; Router itself is shared.
     */
    abstract static class Spec<P> {
        /** "cable" or "pipe", in the player's messages. */
        final String noun;
        /** Where a route finishes, for the start message: "a device or cable", "a port or pipe". */
        final String finishAt;
        /** The route's particles (show): colour and size. */
        final int particle;
        final float particleSize;
        /** Before a start, the preview marks any end with the port cube (else only a machine's port). */
        final boolean bigStartMarker;
        /** The preview of a route to a machine's port adds the port cube at it. */
        final boolean portCube;

        Spec(String noun, String finishAt, int particle, float particleSize, boolean bigStartMarker, boolean portCube) {
            this.noun = noun;
            this.finishAt = finishAt;
            this.particle = particle;
            this.particleSize = particleSize;
            this.bigStartMarker = bigStartMarker;
            this.portCube = portCube;
        }

        /** "Cable" or "Pipe". */
        String title() {
            return Character.toUpperCase(noun.charAt(0)) + noun.substring(1);
        }

        /** A click (click) or the preview's aim at hit on face of pos, for a kit of tier; start: no route yet. */
        abstract Click endAt(Level level, BlockPos pos, Direction face, Vec3 hit, int tier, boolean start, boolean click);

        /** The route from start over the waypoints to end, free round the joins (or null), over view, within budgetMs. */
        abstract Routed<P> route(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                 CableRouter.End end, long[] endJoin, long budgetMs);

        /** Lay a route found by route (all or nothing, joins included); null if laid, else why not. */
        abstract String lay(Level level, int tier, CableSpec spec, Routed<P> r, long[] startJoin, long[] endJoin, Player player);

        /** The parts a route found by route would lay, as boxes in world voxels (the preview's outline). */
        abstract List<int[]> boxes(Routed<P> r, int tier, CableSpec spec);

        /** The route's polyline for its particles: {x, y, z} in world voxels, then the offset (blocks) of the leg ending there. */
        abstract List<double[]> trace(Routed<P> r);

        /** A click with no route yet that this kind handles itself (cables: mending a burnt run); true if handled. */
        boolean repair(ServerPlayer player, ServerLevel level, BlockPos pos, Vec3 hit, KitSupply kit) {
            return false;
        }
    }

    /** The spec for what a supply lays. */
    static Spec<?> of(KitSupply kit) {
        return kit.pipe() ? PipePlacer.ROUTE : CablePlacer.ROUTE;
    }

    /** A route being laid by a player. */
    static class Plan {
        final Spec<?> kind;
        final int tier;
        /** The kit's spec (power cables): a route is laid of one cable. Null for pipes. */
        CableSpec spec;
        final CableRouter.End start;
        /** Where it starts on an old cable or pipe (a branch: world voxels), or null. */
        final long[] startJoin;
        final List<CableRouter.End> waypoints = new ArrayList<>();
        final net.minecraft.resources.ResourceKey<Level> dim;
        long touched;

        Plan(Spec<?> kind, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, net.minecraft.resources.ResourceKey<Level> dim) {
            this.kind = kind;
            this.tier = tier;
            this.spec = spec;
            this.start = start;
            this.startJoin = startJoin;
            this.dim = dim;
        }

        CableRouter.End last() {
            return waypoints.isEmpty() ? start : waypoints.get(waypoints.size() - 1);
        }

        /** Does supply s lay this route (its kind, tier and spec)? */
        boolean laidBy(KitSupply s) {
            return s != null && of(s) == kind && s.tier() == tier && java.util.Objects.equals(s.spec(), spec);
        }
    }

    /** Per player: the route being laid (one, of either kind). */
    static final Map<UUID, Plan> PLANS = new HashMap<>();

    /** A route left alone this long lapses (Baughn: waypoints must time out; a later cable started from an old attempt). */
    public static final long IDLE_TICKS = 20 * 60;

    static Plan plan(UUID player) {
        return PLANS.get(player);
    }

    /** Forget a player's route (logout, death). */
    public static void forget(UUID player) {
        PLANS.remove(player);
    }

    /** Forget the player's route if it is of this kind. */
    static void forget(UUID player, Spec<?> kind) {
        Plan p = PLANS.get(player);
        if (p != null && p.kind == kind) PLANS.remove(player);
    }

    /** Forget every route (server stop). */
    public static void forgetAll() {
        PLANS.clear();
    }

    /** Is a route being laid by this player? */
    public static boolean active(UUID player) {
        return PLANS.containsKey(player);
    }

    /** ... of this kind? */
    static boolean active(UUID player, Spec<?> kind) {
        Plan p = PLANS.get(player);
        return p != null && p.kind == kind;
    }

    /** The waypoints of the player's route (-1 if none). */
    public static int waypoints(UUID player) {
        Plan p = PLANS.get(player);
        return p == null ? -1 : p.waypoints.size();
    }

    /** Keep the player's route from lapsing as idle (the Toolbag's radial is open). */
    static void touch(UUID player, long now) {
        Plan p = PLANS.get(player);
        if (p != null) p.touched = now;
    }

    /** Drop the player's route; the plan dropped, or null. */
    static Plan drop(UUID player) {
        return PLANS.remove(player);
    }

    /** Add waypoints to the player's route (a Toolbag's new kind keeps them). */
    static void addWaypoints(UUID player, List<CableRouter.End> wps) {
        Plan p = PLANS.get(player);
        if (p != null) p.waypoints.addAll(wps);
    }

    /**
     * Why a route being laid lapses now, or null if it holds: another item in hand (Baughn's rule), a minute without a
     * click, the player further from its last point than a full kit reaches, another dimension.
     */
    static String lapse(Plan plan, long now, boolean holdingItsKit, boolean sameDim, Vec3 at) {
        if (!sameDim) return "another dimension";
        if (!holdingItsKit) return "another item in hand";
        if (now - plan.touched > IDLE_TICKS) return "a minute without a click";
        CableRouter.End last = plan.last();
        double dx = at.x * 16 - last.x(), dy = at.y * 16 - last.y(), dz = at.z * 16 - last.z();
        if (dx * dx + dy * dy + dz * dz > (double) KitItem.MAX * KitItem.MAX) return "too far from it";
        return null;
    }

    /** Each player tick (KitPreview): drop a route that lapsed, and say why. */
    static void checkLapse(ServerPlayer p, long now) {
        Plan plan = PLANS.get(p.getUUID());
        if (plan == null) return;
        String why = lapse(plan, now, plan.laidBy(KitSupply.held(p)), plan.dim == p.level().dimension(), p.position());
        if (why == null) return;
        PLANS.remove(p.getUUID());
        p.sendOverlayMessage(Component.literal(plan.kind.title() + " route dropped (" + why + ")"));
    }

    static boolean sameSpot(CableRouter.End a, CableRouter.End b) {
        return a != null && b != null && Router.sameSpot(a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
    }

    /** The same point and face (a click on the last waypoint again). */
    static boolean same(CableRouter.End a, CableRouter.End b) {
        return a != null && b != null && a.x() == b.x() && a.y() == b.y() && a.z() == b.z() && a.attach() == b.attach();
    }

    /**
     * A kit used on a block face: start, extend or finish a route; sneak: take back a waypoint or cancel. Returns false
     * only for a sneak-click with no route (nothing to do).
     */
    public static boolean use(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, KitSupply kit, Vec3 hit, boolean sneak) {
        return use(of(kit), player, level, pos, face, kit, hit, sneak);
    }

    private static <P> boolean use(Spec<P> k, ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, KitSupply kit, Vec3 hit,
                                   boolean sneak) {
        int tier = kit.tier();
        CableSpec spec = kit.spec();
        UUID id = player.getUUID();
        Plan plan = PLANS.get(id);
        long now = level.getGameTime();
        // (the other kind's route: the next tick's lapse check would drop it for another item in hand; this click starts
        // afresh)
        if (plan != null && plan.kind != k) {
            PLANS.remove(id);
            plan = null;
        }
        boolean other = plan != null && (plan.tier != tier || !java.util.Objects.equals(plan.spec, spec));
        boolean lapsed = plan != null && lapse(plan, now, true, plan.dim == level.dimension(), player.position()) != null;
        if (plan != null && (other || lapsed)) {
            PLANS.remove(id);
            if (other) player.sendOverlayMessage(Component.literal(k.title() + " route cancelled (another kind of " + k.noun + " in hand)"));
            plan = null;
        }
        if (plan != null) plan.touched = now;
        if (sneak) return undo(player, level);
        if (!CablePlacer.reachable(player, pos, hit)) {
            KitPreview.note(player, "Out of reach");
            return true;
        }
        if (plan == null && k.repair(player, level, pos, hit, kit)) return true;
        Click c = k.endAt(level, pos, face, hit, tier, plan == null, true);
        if (c.at() == null) {
            if (c.refusal() != null) player.sendOverlayMessage(Component.literal(c.refusal()));
            return true;
        }
        CableRouter.End at = c.at();
        if (plan == null) {
            Plan p = new Plan(k, tier, spec, at, c.join(), level.dimension());
            p.touched = now;
            PLANS.put(id, p);
            player.sendOverlayMessage(Component.literal(k.title() + " route started. Click waypoints, then " + k.finishAt + "."));
            return true;
        }
        String hint = ". " + kit.undoSentence();   // (wording: Neon, 2026-11-21)
        String from = plan.startJoin != null ? "from a " + k.noun + ", " : "";
        boolean again = again(plan, c);
        String refused = startRefusal(plan, c, again, kit);
        if (refused != null) {
            KitPreview.note(player, refused);
            return true;
        }
        if (!c.finish() && c.join() == null && !again) {
            plan.waypoints.add(at);
            long tRoute = System.nanoTime();
            Routed<P> r = route(k, level, plan, null, null);
            KitPreview.slow("waypoint route (server thread)", tRoute, from + Router.LAST_EXPANSIONS.get() + " expansions");
            if (r.error() != null) {
                plan.waypoints.remove(plan.waypoints.size() - 1);
                KitPreview.note(player, "Can't reach: " + r.error() + hint);
            } else {
                player.sendOverlayMessage(Component.literal(String.format("Waypoint %d: %.1f blocks so far", plan.waypoints.size(), r.length() / 16.0)
                    + ". Click it again to end there."));
                show(player, level, k, r);
            }
            return true;
        }
        // finish: at a machine, a cable or pipe, or the last waypoint
        CableRouter.End end = again ? plan.waypoints.remove(plan.waypoints.size() - 1) : at;
        long tRoute = System.nanoTime();
        Routed<P> r = route(k, level, plan, end, c.join());
        KitPreview.slow("click route (server thread)", tRoute, from + Router.LAST_EXPANSIONS.get() + " expansions");
        if (r.error() != null) {
            if (again) plan.waypoints.add(end);
            KitPreview.note(player, "Can't reach: " + r.error() + hint);
            return true;
        }
        int len = r.length();
        boolean creative = player.isCreative();
        if (!creative && kit.length() < len) {
            if (again) plan.waypoints.add(end);
            player.sendOverlayMessage(Component.literal(String.format("This route takes %.1f blocks of %s; the kit holds %.1f", len / 16.0, k.noun,
                kit.length() / 16.0)));
            show(player, level, k, r);
            return true;
        }
        String err = k.lay(level, plan.tier, plan.spec, r, plan.startJoin, c.join(), player);
        if (err != null) {
            if (again) plan.waypoints.add(end);
            KitPreview.note(player, "Can't lay it: " + err);
            return true;
        }
        PLANS.remove(id);
        if (!creative) kit.take(len);
        player.sendOverlayMessage(Component.literal(String.format("Laid %.1f blocks of %s", len / 16.0, k.noun)));
        return true;
    }

    /** Take back the route's last waypoint, or cancel it if it has none; false if there is no route. */
    public static boolean undo(ServerPlayer player, ServerLevel level) {
        Plan plan = PLANS.get(player.getUUID());
        if (plan == null) return false;
        if (plan.waypoints.isEmpty()) {
            PLANS.remove(player.getUUID());
            KitPreview.note(player, plan.kind.title() + " route cancelled");
        } else {
            plan.waypoints.remove(plan.waypoints.size() - 1);
            // (on the note line: the preview's next line takes the action bar at once, and a waypoint on the way to the
            // aim changes neither the route drawn nor its length; AdventurAgent read an Undo as doing nothing)
            KitPreview.note(player, "Waypoint taken back (" + plan.waypoints.size() + " left)");
            redraw(plan.kind, player, level, plan);   // (and the route left drawn again)
        }
        return true;
    }

    /** Draw the plan's route to its last waypoint as particles, if there is one. */
    private static <P> void redraw(Spec<P> k, ServerPlayer player, ServerLevel level, Plan plan) {
        Routed<P> r = route(k, level, plan, null, null);
        if (r.error() == null) show(player, level, k, r);
    }

    /** Is the aim c at the plan's last waypoint again (a plain surface click there: the route ends there)? */
    static boolean again(Plan plan, Click c) {
        return !c.finish() && c.join() == null && !plan.waypoints.isEmpty() && same(plan.waypoints.get(plan.waypoints.size() - 1), c.at());
    }

    /**
     * Why a click at c is refused for being back at the start spot (no waypoints yet: as the first waypoint, or as the
     * finish; AdventurAgent: a socket clicked twice laid a loop from its pin back to the same pin), or null. The click and
     * the preview both ask.
     */
    static String startRefusal(Plan plan, Click c, boolean again, KitSupply kit) {
        if (again || !plan.waypoints.isEmpty() || !sameSpot(plan.start, c.at())) return null;
        return c.finish() || c.join() != null ? "That's the start. Finish elsewhere, or " + kit.cancelClause() + "."
            : "That's the start. Click elsewhere, or " + kit.cancelClause() + ".";
    }

    /** The plan's route to end (null: to its last waypoint), free round its start join and endJoin (or null), on the live world. */
    static <P> Routed<P> route(Spec<P> k, Level level, Plan plan, CableRouter.End end, long[] endJoin) {
        List<CableRouter.End> wps = new ArrayList<>(plan.waypoints);
        CableRouter.End last = end;
        if (last == null) {
            if (wps.isEmpty()) return new Routed<>(null, 0, null);
            last = wps.remove(wps.size() - 1);
        }
        return k.route(RouteView.live(level), plan.tier, plan.spec, plan.start, plan.startJoin, wps, last, endJoin, PipeRouter.TIME_BUDGET_MS);
    }

    /** The route as particles, for this player only. */
    static <P> void show(ServerPlayer player, ServerLevel level, Spec<P> k, Routed<P> r) {
        if (r.path() == null) return;
        var dust = new net.minecraft.core.particles.DustParticleOptions(k.particle, k.particleSize);
        List<double[]> pts = k.trace(r);
        for (int i = 1; i < pts.size(); i++) {
            double[] a = pts.get(i - 1), b = pts.get(i);
            int n = (int) Math.max(1, (long) (Math.abs(b[0] - a[0]) + Math.abs(b[1] - a[1]) + Math.abs(b[2] - a[2])) / 4);
            for (int s = 0; s <= n; s++) {
                double t = s / (double) n;
                level.sendParticles(player, dust, true, false, (a[0] + (b[0] - a[0]) * t) / 16.0 + b[3], (a[1] + (b[1] - a[1]) * t) / 16.0 + b[4],
                    (a[2] + (b[2] - a[2]) * t) / 16.0 + b[5], 1, 0, 0, 0, 0);
            }
        }
    }
}
