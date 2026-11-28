package mods.eln.node.voxel;

import mods.eln.robotics.motion.TrackPath;
import mods.eln.robotics.servo.LinearServoDriveElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Laying a servo track with a Track Kit on the shared placer (Baughn 2026-11-27, track B: the kit places just the corners
 * the player means, at any heading). Start at a Linear Servo Drive (its car's start); each click on a block face puts a
 * corner at the floor of the block in front of it (a block centre: points are locked to the grid); a sneak-click there
 * puts a stop, which the track runs straight through (the next leg goes on in line with the last); click the last point
 * again to finish. Corners round into arcs as wide as the legs allow, none tighter than MIN_RADIUS. Laying saves the
 * track on the drive (LinearServoDriveElement.setTrack) and takes its length from the kit; no blocks are placed: the
 * drive draws its track.
 */
public final class TrackPlacer {
    private TrackPlacer() {
    }

    /** No corner tighter than this (m; Baughn: as wide as fits, at least 3). */
    public static final double MIN_RADIUS = 3;
    /** Straight track kept either side of a stop and at both ends (m): half the cabin, so a stopped car is on a straight. */
    public static final double STOP_STRAIGHT = 1.5;
    /** A click this close to the point before it (blocks) is that point again (AdventurAgent: a click on the stop just
     *  marked, landing on the neighbouring block, ran on past it and was refused as a kink). */
    static final double SAME_POINT = 1.5;
    /** The most a stop (or end) put on a block centre may bend the track at the stop before it (degrees). */
    static final double KINK_DEG = 5;
    /** The longest track a drive owns (m). */
    public static final double MAX_LENGTH = 64;
    /** A route end's attach: a corner, or a stop. */
    static final int CORNER = -1, STOP = -2;

    /** A track found: its points (blocks), which are stops, and its path; or why not. */
    public record Track(List<Vec3> points, boolean[] stops, TrackPath path) {
    }

    static Vec3 vec(CableRouter.End e) {
        return new Vec3(e.x() / 16.0, e.y() / 16.0, e.z() / 16.0);
    }

    static CableRouter.End end(Vec3 p, int attach) {
        return new CableRouter.End(Math.round(p.x * 16), Math.round(p.y * 16), Math.round(p.z * 16), attach, 0);
    }

    static String tr(String text, Object... args) {
        return mods.eln.i18n.I18N.tr(text, args);
    }

    /**
     * The track from the points clicked (blocks; points[0] the car's start), stops[i] marking stops: a point repeating the
     * one before it is dropped (clicking a stop again ends the track there: review); a point after a stop is put on the
     * stop's line (kept in line), on the voxel grid if it is a corner, on a block centre if it is a stop or the end (so the
     * car stops on whole blocks; the stop then has a slight kink). The result's points are exactly what build() needs:
     * the drive keeps them, and reloads them unchanged. Throws IllegalArgumentException with the player's reason.
     */
    public static Track lay(List<Vec3> points, boolean[] stops) {
        List<Vec3> p = new ArrayList<>();
        List<Boolean> st = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (!p.isEmpty() && points.get(i).distanceToSqr(p.get(p.size() - 1)) <= SAME_POINT * SAME_POINT + 1e-9) {
                st.set(st.size() - 1, st.get(st.size() - 1) || stops[i]);
                continue;
            }
            p.add(points.get(i));
            st.add(stops[i]);
        }
        int n = p.size();
        boolean[] stp = new boolean[n];
        for (int i = 0; i < n; i++) stp[i] = st.get(i) || i == 0 || i == n - 1;
        for (int i = 1; i + 1 < n; i++)
            if (stp[i]) {   // the next point goes on in line
                Vec3 u = p.get(i).subtract(p.get(i - 1)).normalize();
                double t = p.get(i + 1).subtract(p.get(i)).dot(u);
                // (aimed beside or behind the stop: still straight on, a block past it at least; AdventurAgent read the old
                // refusal as a hint that hid the length)
                Vec3 q = p.get(i).add(u.scale(Math.max(1, t)));
                q = stp[i + 1] ? new Vec3(Math.floor(q.x) + 0.5, Math.rint(q.y), Math.floor(q.z) + 0.5)
                    : new Vec3(Math.rint(q.x * 16) / 16, Math.rint(q.y * 16) / 16, Math.rint(q.z * 16) / 16);
                p.set(i + 1, q);
                // (a stop or end put on a block centre bends the track a little at this stop, where nothing slows the car:
                // more than a few degrees, and the player is asked to click further on; review)
                Vec3 a = p.get(i).subtract(p.get(i - 1)).normalize(), b = q.subtract(p.get(i)).normalize();
                if (a.dot(b) < Math.cos(Math.toRadians(KINK_DEG)))
                    throw new IllegalArgumentException(tr("the track must go on straight through the stop: click further along"));
            }
        return build(p, stp);
    }

    /**
     * The track through exactly these points (a laid track's, as lay() made them), stops[i] marking stops: each corner's
     * radius as wide as its legs allow (each leg shared between the corners at its ends; STOP_STRAIGHT kept at stops and
     * ends); a stop is never rounded. Throws IllegalArgumentException with the player's reason.
     */
    public static Track build(List<Vec3> p, boolean[] stops) {
        int n = p.size();
        if (n < 2) throw new IllegalArgumentException(tr("no track yet"));
        double[] lens = new double[n - 1];
        for (int i = 0; i + 1 < n; i++) {
            lens[i] = p.get(i + 1).distanceTo(p.get(i));
            if (lens[i] < 1e-6) throw new IllegalArgumentException(tr("two points in one place"));
        }
        double[] radii = new double[n - 2];
        for (int c = 0; c + 2 < n; c++) {   // corner c sits at point c+1, between leg c and leg c+1
            int at = c + 1;
            Vec3 u = p.get(at).subtract(p.get(at - 1)).normalize(), v = p.get(at + 1).subtract(p.get(at)).normalize();
            double th = Math.acos(Math.max(-1, Math.min(1, u.dot(v))));
            if (stops[at] || th < 1e-6) continue;   // (a stop, or straight through: no arc)
            if (th > Math.PI - 1e-3) throw new IllegalArgumentException(tr("the track can't turn back on itself"));
            double cut = Math.min(share(p, stops, lens, c, false), share(p, stops, lens, c + 1, true));
            double r = cut / Math.tan(th / 2);
            if (r < MIN_RADIUS - 1e-9)
                throw new IllegalArgumentException(tr("corner %1$ is too tight (radius %2$; at least %3$)", at, String.format("%.1f", r),
                    String.format("%.0f", MIN_RADIUS)));
            radii[c] = r;
        }
        TrackPath path = new TrackPath(p, radii);
        if (path.length() > MAX_LENGTH + 1e-9)
            throw new IllegalArgumentException(tr("too long (%1$ blocks; a drive owns at most %2$)", String.format("%.1f", path.length()),
                String.format("%.0f", MAX_LENGTH)));
        if (path.length() < mods.eln.robotics.servo.LinearServoDriveDescriptor.MIN_TRAVEL - 1e-9)
            throw new IllegalArgumentException(tr("too short (at least %1$ blocks)", mods.eln.robotics.servo.LinearServoDriveDescriptor.MIN_TRAVEL));
        return new Track(List.copyOf(p), stops.clone(), path);
    }

    /** How much of leg `leg` the corner at its start (atStart) or end may take: its free length, halved if both ends turn. */
    private static double share(List<Vec3> p, boolean[] stops, double[] lens, int leg, boolean atStart) {
        int a = leg, b = leg + 1, last = p.size() - 1;
        boolean aTurns = a > 0 && !stops[a], bTurns = b < last && !stops[b];
        double free = lens[leg] - (aTurns ? 0 : STOP_STRAIGHT) - (bTurns ? 0 : STOP_STRAIGHT);
        if (free <= 0) return 0;
        return aTurns && bTurns ? free / 2 : free;
    }

    /** The spec on the shared placer. */
    static final RoutePlacer.Spec<Track> ROUTE = new RoutePlacer.Spec<>("track", "the last point again", 0x8090A0, 1.2f, true, false) {
        @Override
        RoutePlacer.Click endAt(Level level, BlockPos pos, Direction face, Vec3 hit, Vec3 eye, int tier, boolean start, boolean click) {
            if (start) {
                LinearServoDriveElement d = LinearServoDriveElement.at(level, pos);
                // (the preview says nothing off a drive: it overwrote "Laid ... of track"; a click says where to start)
                if (d == null) return RoutePlacer.Click.none(click ? tr("Start a track at a Linear Servo Drive") : null);
                if (d.hasRail()) return RoutePlacer.Click.none(mods.eln.i18n.I18N.tr("This drive runs on its rail: take the rail off its top to lay track"));
                return new RoutePlacer.Click(end(d.trackStart(), CORNER), null, false, false, null);
            }
            BlockPos b = pos.relative(face);   // (the floor of the block in front of the face clicked)
            return new RoutePlacer.Click(end(new Vec3(b.getX() + 0.5, b.getY(), b.getZ() + 0.5), CORNER), null, false, false, null);
        }

        @Override
        boolean sneakMarksStops() {
            return true;
        }

        /** A stop just marked, clicked again, ends the track there (review: its attach differed, so it ran on a block). */
        @Override
        boolean sameEnd(CableRouter.End a, CableRouter.End b) {
            return a != null && b != null && vec(a).distanceToSqr(vec(b)) <= SAME_POINT * SAME_POINT + 1e-9;
        }

        /** "Track: 12.0 blocks \u2014 corner" (sneak-click: a stop; clicked again: the end). */
        @Override
        String suffix(boolean again, boolean join, boolean finish) {
            return " \u2014 " + (again ? mods.eln.i18n.I18N.tr("end here") : mods.eln.i18n.I18N.tr("corner (sneak: stop)"));
        }

        @Override
        RoutePlacer.Routed<Track> route(RouteView view, int tier, CableSpec spec, CableRouter.End start, long[] startJoin, List<CableRouter.End> wps,
                                        CableRouter.End end, long[] endJoin, long budgetMs) {
            List<Vec3> pts = new ArrayList<>();
            List<Boolean> st = new ArrayList<>();
            pts.add(vec(start));
            st.add(true);
            for (CableRouter.End w : wps) {
                pts.add(vec(w));
                st.add(w.attach() == STOP);
            }
            pts.add(vec(end));
            st.add(true);
            boolean[] stops = new boolean[st.size()];
            for (int i = 0; i < stops.length; i++) stops[i] = st.get(i);
            try {
                Track t = TrackPlacer.lay(pts, stops);
                return new RoutePlacer.Routed<>(null, (int) Math.round(t.path().length() * 16), t);
            } catch (IllegalArgumentException ex) {
                return RoutePlacer.Routed.said(mods.eln.i18n.I18N.tr("Track: %1$", ex.getMessage()));
            }
        }

        @Override
        String lay(Level level, int tier, CableSpec spec, RoutePlacer.Routed<Track> r, long[] startJoin, long[] endJoin, Player player) {
            Vec3 s = r.path().points().get(0);
            LinearServoDriveElement d = LinearServoDriveElement.atTrackStart(level, s);
            if (d == null) return tr("its drive is gone");
            return d.setTrack(r.path().points(), r.path().stops());
        }

        @Override
        List<int[]> boxes(RoutePlacer.Routed<Track> r, int tier, CableSpec spec) {
            List<int[]> out = new ArrayList<>();   // (the preview: a dot every half block along the car's path)
            TrackPath path = r.path().path();
            for (double s = 0; s <= path.length() + 1e-9; s += 0.5) {
                Vec3 q = path.at(s);
                int x = (int) Math.round(q.x * 16), y = (int) Math.round(q.y * 16), z = (int) Math.round(q.z * 16);
                out.add(new int[]{x - 1, y, z - 1, x + 1, y + 2, z + 1});
            }
            for (double s : path.stops()) {   // (stops: a bigger mark)
                Vec3 q = path.at(s);
                int x = (int) Math.round(q.x * 16), y = (int) Math.round(q.y * 16), z = (int) Math.round(q.z * 16);
                out.add(new int[]{x - 4, y, z - 4, x + 4, y + 1, z + 4});
            }
            return out;
        }

        @Override
        List<double[]> trace(RoutePlacer.Routed<Track> r) {
            List<double[]> out = new ArrayList<>();
            TrackPath path = r.path().path();
            for (double s = 0; s <= path.length() + 1e-9; s += 0.5) {
                Vec3 q = path.at(s);
                out.add(new double[]{q.x * 16, q.y * 16 + 1, q.z * 16, 0, 0, 0});
            }
            return out;
        }
    };
}
