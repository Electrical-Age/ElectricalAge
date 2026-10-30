package mods.eln.node.voxel;

import net.minecraft.resources.Identifier;
import org.erisia.cad.Box;
import org.erisia.cad.Face;
import org.erisia.cad.Frustum;
import org.erisia.cad.Model;
import org.erisia.cad.Ring;
import org.erisia.cad.Solid;
import org.erisia.cad.Sweep;
import org.erisia.cad.UvMap;

import java.util.ArrayList;
import java.util.List;

/**
 * What a voxel node's parts look like, as textured quads (notes/cables/PLAN.md step 8: baked into the chunk mesh, not
 * drawn every frame): round pipes with fittings and flanges, valves, adapters, and cables as boxes in their six-node
 * texture. Pure geometry, no client classes; VoxelModel (client) bakes it.
 * <p>
 * Made with the checked model library (org.erisia.cad, board #44): {@link #model} builds the block's solids in voxels
 * (px, y up, the block 0..16), with what they mean (a fitting stands on its pipe, inside its hitbox), and tests run the
 * checker over every tier, fitting, mode, orientation and route shape (VoxelGeometryCadTest).
 */
public final class VoxelGeometry {
    private VoxelGeometry() {
    }

    /**
     * One quad: its texture (block-atlas sprite id), colour (ARGB, multiplied in), corners (block-local 0..1, x y z each,
     * wound counter-clockwise seen from outside), UVs (0..1 of the sprite, u v each), and the block face it lies on
     * (3D value; -1 if inside the block), for culling.
     */
    public record Quad(Identifier sprite, int argb, float[] xyz, float[] uv, int face) {
    }

    /** Each pipe tier's sprite (PipePart.TIER_NAME order; the large alloy pipe is the alloy's metal, drawn wider). */
    public static final Identifier[] PIPE = {Identifier.fromNamespaceAndPath("eln", "block/pipe_iron"),
        Identifier.fromNamespaceAndPath("eln", "block/pipe_alloy"), Identifier.fromNamespaceAndPath("eln", "block/pipe_alloy")};
    static final int SIDES = 12;

    /** The cable texture of a tier (the six-node cable's, copied onto the block atlas: textures/block/cable/). */
    public static Identifier cableSprite(int tier) {
        if (CablePart.TIERS[tier].thermal()) return Identifier.fromNamespaceAndPath("eln", "block/cable/thermal");
        return Identifier.fromNamespaceAndPath("eln", CablePart.TIERS[tier].height() > 1 && CablePart.TIERS[tier].name().startsWith("Very")
            ? "block/cable/cablevhv" : "block/cable/cable");
    }

    public static List<Quad> of(List<VoxelPart> parts) {
        return of(parts, null);
    }

    /** ... with lugs where floor cables end at a terminal plate (lugs: conductor height px per horizontal face, or null). */
    public static List<Quad> of(List<VoxelPart> parts, float[] lugs) {
        List<Quad> out = new ArrayList<>();
        model(parts, lugs).emit(f -> out.add(quad(f)));
        return out;
    }

    /** The block's solids (and their relations, for the tests). */
    public static Model model(List<VoxelPart> parts, float[] lugs) {
        Model m = new Model();
        // the faces an adapter's flange covers: a pipe ending there draws no flange or cap of its own (they lay in the
        // flange's face and z-fought with it)
        boolean[] adapters = new boolean[6];
        for (VoxelPart part : parts) if (part instanceof AdapterPart a) adapters[a.face] = true;
        List<int[]> cableBoxes = new ArrayList<>();
        for (VoxelPart part : parts) {
            if (part instanceof ValvePart v) valve(m, v);
            else if (part instanceof SafetyValvePart sv) safetyValve(m, sv, parts);
            else if (part instanceof PipeSensorPart ps) sensor(m, ps, parts);
            else if (part instanceof AdapterPart a)
                box(m, a.boxes().get(0), PIPE[0]).named("adapter flange").rgb(0.75f, 0.55f, 0.3f);   // (brass)
            else if (part instanceof PipePart pipe) {
                // (burst: scorched)
                int argb = pipe.burst ? rgb(0.55f, 0.35f, 0.25f) : 0xFFFFFFFF;
                pipe(m, pipe, PIPE[pipe.tier], argb, adapters);
            } else if (part instanceof CablePart c) {
                // (black insulation, Baughn; burnt: scorched brown)
                int argb = c.burnt != null ? rgb(0.35f, 0.22f, 0.14f) : rgb(0.22f, 0.22f, 0.24f);
                cable(m, c, argb, cableBoxes);
                if (lugs != null && c.face == 0 && !c.isKnuckle())
                    for (CablePart.Port port : c.cablePorts())
                        if (port.face() >= 2 && lugs[port.face() - 2] > 0) lug(m, c, port, lugs[port.face() - 2]);
            }
        }
        return m;
    }

    // --- the parts ---

    /**
     * A gate valve: a body round the pipe, a stem, and a round handwheel wider than the pipe (Baughn: they looked like
     * buttons); the wheel red when shut, green when open. The control valve has a square actuator, the pressure valve a
     * dome and a nub showing its flow.
     */
    static void valve(Model m, ValvePart v) {
        int h = v.bore / 2 + 1, r = v.wheel(), s = v.reach();
        double[] c = {v.x, v.y, v.z};
        int[] o = others(v.axis);
        tube(m, v.axis, c[o[0]], c[o[1]], h, c[v.axis] - h, c[v.axis] + h, PIPE[0]).named("valve body").rgb(0.45f, 0.45f, 0.5f);
        int stem = v.axis == 1 ? 0 : 1;   // (the stem's axis: up, or along x for a vertical pipe)
        int[] so = others(stem);
        tube(m, stem, c[so[0]], c[so[1]], 0.75, c[stem] + h - 0.5, c[stem] + s, PIPE[0]).named("valve stem").rgb(0.45f, 0.45f, 0.5f);
        // (the control and throttle valves: red shut, through yellow, to green open)
        float op = (float) (v.mode == ValvePart.ON_OFF ? (v.open ? 1 : 0) : v.opening());
        int shown = rgb(0.8f - 0.5f * op, 0.2f + 0.55f * op, 0.15f + 0.15f * op);
        double plane = c[stem] + s;
        if (v.mode == ValvePart.PRESSURE) {
            // the pressure valve: a dome actuator (brass relieving, steel reducing) and a nub on the body's downstream
            // side showing its flow (Baughn 2026-10-29)
            double[] lo = new double[3], hi = new double[3];
            lo[stem] = plane;
            hi[stem] = plane + 1.5;
            lo[so[0]] = c[so[0]] - 2;
            hi[so[0]] = c[so[0]] + 2;
            lo[so[1]] = c[so[1]] - 2;
            hi[so[1]] = c[so[1]] + 2;
            boxD(m, lo, hi, PIPE[0]).named("pressure dome").argb(v.pmode == ValvePart.RELIEF ? rgb(0.75f, 0.6f, 0.25f) : rgb(0.6f, 0.62f, 0.68f));
            double[] nlo = new double[3], nhi = new double[3];
            double tip = c[v.axis] + v.flow * (h + 1);
            int side = so[0] == v.axis ? so[1] : so[0];
            nlo[v.axis] = Math.min(c[v.axis] + v.flow * h, tip);
            nhi[v.axis] = Math.max(c[v.axis] + v.flow * h, tip);
            nlo[side] = c[side] - 0.75;
            nhi[side] = c[side] + 0.75;
            nlo[stem] = c[stem] + h - 1.5;
            nhi[stem] = c[stem] + h;
            boxD(m, nlo, nhi, PIPE[0]).named("flow nub").rgb(0.85f, 0.85f, 0.2f);
            return;
        }
        if (v.mode == ValvePart.CONTROL) {
            // an actuator: a square box on the stem, coloured by the opening (its signal: a cable laid into the block)
            double[] lo = new double[3], hi = new double[3];
            lo[stem] = plane;
            hi[stem] = plane + 1;
            lo[so[0]] = c[so[0]] - 2;
            hi[so[0]] = c[so[0]] + 2;
            lo[so[1]] = c[so[1]] - 2;
            hi[so[1]] = c[so[1]] + 2;
            boxD(m, lo, hi, PIPE[0]).named("actuator").argb(shown);
            return;
        }
        // the rim: a ring (AdventurAgent: 16 cubes read as beads); two spokes across
        Ring rim = m.add(new Ring(stem, c[so[0]], c[so[1]], r - 1.1, r, plane, plane + 1, 16));
        rim.named("handwheel").material(PIPE[0]).argb(shown).uv(RING_UV);
        for (int a = 0; a < 2; a++) {
            double[] lo = new double[3], hi = new double[3];
            lo[stem] = plane + 0.2;
            hi[stem] = plane + 0.8;
            lo[so[a]] = c[so[a]] - r + 0.5;
            hi[so[a]] = c[so[a]] + r - 0.5;
            lo[so[1 - a]] = c[so[1 - a]] - 0.4;
            hi[so[1 - a]] = c[so[1 - a]] + 0.4;
            boxD(m, lo, hi, PIPE[0]).named("spoke " + a).argb(shown);
        }
    }

    /** The drawn radius of the pipe a fitting sits on (the one through its point), or the narrowest of its grid width. */
    static double pipeRadius(List<VoxelPart> parts, int x, int y, int z, int bore) {
        for (VoxelPart p : parts) if (p instanceof PipePart pipe && pipe.passes(x, y, z)) return pipe.drawn() / 2;
        for (int t = 0; t < PipePart.GRID.length; t++) if (PipePart.GRID[t] == bore) return PipePart.DRAWN[t] / 2;
        return bore / 2.0;
    }

    /**
     * A fitting's size step (px) on a pipe of drawn radius rp: the old fittings (made for the 4-voxel pipe, radius 2)
     * at their size on the large alloy pipe, and at half of it on iron (Baughn 2026-10-30: the safety valve looked
     * huge on the thin pipes), a little more on alloy so its body stays clear of the pipe's surface.
     */
    static double fittingScale(double rp) {
        return Math.min(1, Math.max(0.5, 0.8 * rp));
    }

    /** The pipe solid a fitting sits on (for its relations): the drawn pipe through its point along axis. */
    static Solid pipeUnder(Model m, int axis, double[] c, double rp) {
        int[] o = others(axis);
        return m.add(Frustum.cylinder(axis, c[o[0]], c[o[1]], rp, 0, 16)).named("pipe under").ghost();
    }

    /** The axis a fitting's outlet points along, and the pipe's axis (the first axis across it the pipe runs on). */
    static int outletAxis(int outlet) {
        return outlet / 2 == 0 ? 1 : outlet / 2 == 1 ? 2 : 0;
    }

    static int pipeAxis(List<VoxelPart> parts, int x, int y, int z, int outletAxis) {
        for (VoxelPart p : parts)
            if (p instanceof PipePart pipe && pipe.passes(x, y, z))
                for (int a = 0; a < 3; a++) {
                    if (a == outletAxis) continue;
                    int[] lo = {x, y, z}, hi = {x, y, z};
                    lo[a] -= 1;
                    hi[a] += 1;
                    if (pipe.passes(lo[0], lo[1], lo[2]) && pipe.passes(hi[0], hi[1], hi[2])) return a;
                }
        return outletAxis == 0 ? 1 : 0;
    }

    /**
     * The safety valve: a brass body standing on the pipe (from its axis, so it meets the round surface), a red cap
     * stacked on the body (Argon/Baughn 2026-10-30: the old cap ended in the body's top plane and z-fought), sized to the
     * pipe as drawn.
     */
    static void safetyValve(Model m, SafetyValvePart sv, List<VoxelPart> parts) {
        double rp = pipeRadius(parts, sv.x, sv.y, sv.z, sv.bore), s = fittingScale(rp);
        int ax = outletAxis(sv.outlet), dir = sv.outlet % 2 == 1 ? 1 : -1;
        double[] c = {sv.x, sv.y, sv.z};
        int[] o = others(ax);
        double base = c[ax], top = base + dir * (rp + 3 * s), end = top + dir * s;
        Solid body = tube(m, ax, c[o[0]], c[o[1]], 1.4 * s, Math.min(base, top), Math.max(base, top), PIPE[0]).named("safety valve body").rgb(0.8f, 0.6f, 0.3f);
        Solid cap = tube(m, ax, c[o[0]], c[o[1]], 2 * s, Math.min(top, end), Math.max(top, end), PIPE[0]).named("safety valve cap").rgb(0.75f, 0.15f, 0.12f);
        Solid pipe = pipeUnder(m, pipeAxis(parts, sv.x, sv.y, sv.z, ax), c, rp);
        Solid hit = box(m, sv.boxes().get(0), null).named("safety valve hitbox").ghost();
        m.attach(body, pipe, rp).abut(cap, body).within(cap, hit).disjoint(cap, pipe)
            .ratio("safety valve cap radius / pipe radius", 2 * s / rp, 0.5, 2).ratio("safety valve body radius / pipe radius", 1.4 * s / rp, 0.4, 1.5);
    }

    /**
     * The pipe sensor (Baughn 2026-10-27): a stem (steel for pressure, copper for temperature, blue for steam quality)
     * standing on the pipe, and a dial stacked on it whose face goes green to red with the reading's share of full
     * scale; sized to the pipe as drawn, as the safety valve.
     */
    static void sensor(Model m, PipeSensorPart ps, List<VoxelPart> parts) {
        double rp = pipeRadius(parts, ps.x, ps.y, ps.z, ps.bore), s = fittingScale(rp);
        int ax = outletAxis(ps.outlet), dir = ps.outlet % 2 == 1 ? 1 : -1;
        double[] c = {ps.x, ps.y, ps.z};
        int[] o = others(ax);
        double base = c[ax], top = base + dir * (rp + 2 * s), end = top + dir * s;
        int stemRgb = ps.sensing == PipeSensorPart.PRESSURE ? rgb(0.7f, 0.7f, 0.72f) : ps.sensing == PipeSensorPart.TEMPERATURE ? rgb(0.8f, 0.5f, 0.3f)
            : rgb(0.35f, 0.55f, 0.85f);
        Solid stem = tube(m, ax, c[o[0]], c[o[1]], 0.6 * s, Math.min(base, top), Math.max(base, top), PIPE[0]).named("sensor stem").argb(stemRgb);
        float f = (float) Math.max(0, Math.min(1, ps.shown));
        Solid dial = tube(m, ax, c[o[0]], c[o[1]], 2 * s, Math.min(top, end), Math.max(top, end), PIPE[0]).named("sensor dial")
            .rgb(0.2f + 0.7f * f, 0.75f - 0.55f * f, 0.15f);
        Solid pipe = pipeUnder(m, pipeAxis(parts, ps.x, ps.y, ps.z, ax), c, rp);
        Solid hit = box(m, ps.boxes().get(0), null).named("sensor hitbox").ghost();
        m.attach(stem, pipe, rp).abut(dial, stem).within(dial, hit).disjoint(dial, pipe)
            .ratio("sensor dial radius / pipe radius", 2 * s / rp, 0.5, 2);
    }

    /** A pipe: a round tube along each straight piece, a fitting ring at each bend, a flange where it meets a face. */
    static void pipe(Model m, PipePart pipe, Identifier sprite, int argb, boolean[] adapters) {
        double r = pipe.drawn() / 2.0;
        int[] p = pipe.points;
        int n = p.length / 3;
        double[] pts = new double[p.length];
        for (int i = 0; i < p.length; i++) pts[i] = p[i];
        // run into the bend a little so the tubes meet without a gap
        for (Frustum piece : Sweep.along(pts, r, 0.7)) {
            m.add(piece).named("pipe").material(sprite).argb(argb).uv(TUBE_UV).culled();
            int f0 = piece.axis == 1 ? 0 : piece.axis == 2 ? 2 : 4;
            piece.open(piece.a0 == 0 && adapters[f0], piece.a1 == 16 && adapters[f0 + 1]);
        }
        List<double[]> fittings = new ArrayList<>();
        for (int i = 1; i < n - 1; i++) {
            int[] c = {p[3 * i], p[3 * i + 1], p[3 * i + 2]};
            int axis = p[3 * i - 3] != c[0] ? 0 : p[3 * i - 2] != c[1] ? 1 : 2;
            int[] o = others(axis);
            // two bends close together (a large pipe's, 6 voxels apart): their rings' sides lay in one plane and
            // z-fought, so the later ring is turned half a side (no side of it faces the earlier one's)
            double[] bb = {c[0] - r - 0.35, c[1] - r - 0.35, c[2] - r - 0.35, c[0] + r + 0.35, c[1] + r + 0.35, c[2] + r + 0.35};
            boolean near = false;
            for (double[] f : fittings) near |= bb[0] < f[3] && f[0] < bb[3] && bb[1] < f[4] && f[1] < bb[4] && bb[2] < f[5] && f[2] < bb[5];
            m.add(new Frustum(axis, c[o[0]], c[o[1]], r + 0.35, r + 0.35, c[axis] - r - 0.3, c[axis] + r + 0.3, SIDES, near ? 0 : 0.5))
                .named("pipe fitting").material(sprite).argb(argb).uv(TUBE_UV).culled();
            fittings.add(bb);
        }
        for (int end = 0; end < 2; end++) {
            int i = end == 0 ? 0 : n - 1;
            int x = p[3 * i], y = p[3 * i + 1], z = p[3 * i + 2];
            int face = PipePart.faceOf(x, y, z);
            if (face < 0 || adapters[face]) continue;
            int axis = face <= 1 ? 1 : face <= 3 ? 2 : 0;
            int[] c = {x, y, z};
            int[] o = others(axis);
            double at = c[axis];
            tube(m, axis, c[o[0]], c[o[1]], r + 0.75, at == 0 ? 0 : 15, at == 0 ? 1 : 16, sprite).named("pipe flange").argb(argb);
        }
    }

    /**
     * A cable: its boxes, textured as six-node cables are (CableRender): the strip of the texture's left half centred
     * on u = 1/4, across the cable on top and running down its sides, v along it.
     */
    static void cable(Model m, CablePart c, int argb, List<int[]> drawn) {
        List<int[]> boxes = c.boxes();
        int[] p = c.points;
        for (int i = 0; i < boxes.size(); i++) {
            int along, n;
            if (c.isKnuckle()) {
                along = p[0] != 0 && p[0] != 16 ? 0 : p[1] != 0 && p[1] != 16 ? 1 : 2;
                n = along == 1 ? 0 : 1;
            } else {
                int k = 3 * (i + 1);
                along = p[k] != p[k - 3] ? 0 : p[k + 1] != p[k - 2] ? 1 : 2;
                n = CablePart.normalAxis(c.face);
            }
            int[] bx = trim(boxes.get(i).clone(), along, drawn);
            if (bx == null) continue;
            drawn.add(bx);
            int w = 3 - along - n;
            double cw = (bx[w] + bx[w + 3]) / 2.0, half = (bx[w + 3] - bx[w]) / 2.0;
            boolean knuckle = c.isKnuckle();
            double top = c.isKnuckle() || CablePart.plane(c.face) == 0 ? bx[n + 3] : bx[n];
            UvMap uv = f -> {
                int fa = f.index / 2;
                float[] out = new float[8];
                for (int j = 0; j < 4; j++) {
                    double[] v = {f.xyz[3 * j], f.xyz[3 * j + 1], f.xyz[3 * j + 2]};
                    double u, vv = v[along] / 16;
                    double depth = Math.abs(top - v[n]);
                    if (knuckle) u = 0.25 + (v[w] - cw) / 32;   // (a knuckle: just the strip)
                    else if (fa == w) u = 0.25 + Math.signum(v[w] - cw) * (half / 32 + depth / 16);
                    else u = 0.25 + (v[w] - cw) / 32;
                    if (fa == along && !knuckle) vv = 0.5 - depth / 16;
                    out[2 * j] = (float) clamp01(u);
                    out[2 * j + 1] = (float) clamp01(vv);
                }
                return out;
            };
            box(m, bx, cableSprite(c.tier)).named("cable box " + i).argb(argb).uv(uv);
        }
    }

    /**
     * A cable box cut back along its length where an earlier cable box in the block covers its whole cross-section
     * there (a bend's corner, a branch's root): their tops overlapped, each with its own strip of texture, and z-fought.
     * Null if nothing is left.
     */
    static int[] trim(int[] b, int along, List<int[]> drawn) {
        for (int[] d : drawn) {
            boolean overlap = true, covers = true;
            for (int a = 0; a < 3; a++) {
                overlap &= b[a] < d[a + 3] && d[a] < b[a + 3];
                if (a != along) covers &= d[a] <= b[a] && d[a + 3] >= b[a + 3];
            }
            if (!overlap || !covers) continue;
            if (d[along] <= b[along]) b[along] = Math.max(b[along], d[along + 3]);
            else if (d[along + 3] >= b[along + 3]) b[along + 3] = Math.min(b[along + 3], d[along]);
            if (b[along + 3] <= b[along]) return null;
        }
        return b;
    }

    /**
     * A cable lug where a floor cable ends at a device's terminal plate (as six-node cables draw them, CableRender.drawKnuckles):
     * the cable turns up against the face, a copper crimp barrel tops it and a copper palm lies on the plate's middle
     * conductor, H px above the floor.
     */
    static void lug(Model m, CablePart c, CablePart.Port port, float H) {
        int along = port.face() >= 4 ? 0 : 2, across = 2 - along;
        double edge = along == 0 ? port.x() : port.z(), in = edge == 0 ? 1 : -1, mid = across == 0 ? port.x() : port.z();
        double w2 = c.tierInfo().width() / 2.0, h = c.tierInfo().height();
        lugBox(m, along, across, 0, H - 0.1, mid, w2 + 0.05, edge + in * 0.25, edge + in * (0.25 + h), cableSprite(c.tier)).named("lug riser")
            .rgb(0.22f, 0.22f, 0.24f);
        int copper = rgb(0.77f, 0.43f, 0.27f);   // (dull copper: a lug you can touch isn't bare metal, Baughn)
        lugBox(m, along, across, H - 0.1, H + 0.6, mid, w2 + 0.15, edge + in * 0.15, edge + in * (0.35 + h), PIPE[0]).named("lug barrel").argb(copper);
        lugBox(m, along, across, H - 0.85, H + 0.85, mid, Math.max(0.75, w2 - 0.25), edge, edge + in * 0.25, PIPE[0]).named("lug palm").argb(copper);
    }

    static Solid lugBox(Model m, int along, int across, double y0, double y1, double mid, double half, double a0, double a1, Identifier sprite) {
        double[] lo = new double[3], hi = new double[3];
        lo[1] = y0;
        hi[1] = y1;
        lo[across] = mid - half;
        hi[across] = mid + half;
        lo[along] = Math.min(a0, a1);
        hi[along] = Math.max(a0, a1);
        return boxD(m, lo, hi, sprite);
    }

    // --- the voxel target: solids, texture coordinates, quads ---

    static double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    static int[] others(int axis) {
        return axis == 0 ? new int[]{1, 2} : axis == 1 ? new int[]{0, 2} : new int[]{0, 1};
    }

    static int rgb(float r, float g, float b) {
        return 0xFF000000 | Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
    }

    /** A box in fractional voxels, its faces never culled against the block's. */
    static Solid boxD(Model m, double[] lo, double[] hi, Identifier sprite) {
        return m.add(new Box(lo, hi)).material(sprite).uv(BOX_UV);
    }

    /** A box in voxels (empty: none), its faces on the block's faces culled against the neighbour, UVs planar. */
    static Solid box(Model m, int[] bx, Identifier sprite) {
        Box b = new Box(bx[0], bx[1], bx[2], bx[3], bx[4], bx[5]);
        if (sprite != null) b.material(sprite).uv(BOX_UV).culled();
        if (bx[3] <= bx[0] || bx[4] <= bx[1] || bx[5] <= bx[2]) b.ghost();   // (nothing to draw)
        return m.add(b);
    }

    /** A capped tube along axis, centred at (c1, c2) on the other two, from a0 to a1 (voxels); caps on the block's faces culled. */
    static Solid tube(Model m, int axis, double c1, double c2, double r, double a0, double a1, Identifier sprite) {
        return m.add(Frustum.cylinder(axis, c1, c2, r, a0, a1)).material(sprite).uv(TUBE_UV).culled();
    }

    /** Planar UVs on a box face's two other axes, at vanilla's density (16 voxels a sprite), v down. */
    static final UvMap BOX_UV = f -> {
        int fa = f.index / 2;
        float[] uv = new float[8];
        for (int i = 0; i < 4; i++) {
            double u = fa != 0 ? f.xyz[3 * i] : f.xyz[3 * i + 2], v = fa == 1 ? f.xyz[3 * i + 2] : f.xyz[3 * i + 1];
            uv[2 * i] = (float) (u / 16);
            uv[2 * i + 1] = (float) (1 - v / 16);
        }
        return uv;
    };

    /** A tube's sides wrap the sprite once round (u), v along the axis; its caps planar. */
    static final UvMap TUBE_UV = f -> {
        Frustum t = (Frustum) f.solid;
        int S = t.sides;
        int[] o = others(t.axis);
        float[] uv = new float[8];
        for (int i = 0; i < 4; i++) {
            if (f.kind == Frustum.SIDE) {
                int k1 = (f.index + 1) % S;
                uv[2 * i] = (float) ((f.index + (f.vid[i] % S == k1 && f.vid[i] < 2 * S ? 1 : 0)) / (double) S);
                uv[2 * i + 1] = (float) clamp01(f.xyz[3 * i + t.axis] / 16);
            } else {
                uv[2 * i] = (float) clamp01(f.xyz[3 * i + o[0]] / 16);
                uv[2 * i + 1] = (float) clamp01(f.xyz[3 * i + o[1]] / 16);
            }
        }
        return uv;
    };

    /** A ring's faces: the sprite across each segment. */
    static final UvMap RING_UV = f -> {
        int n = ((Ring) f.solid).n, k = f.index;
        return new float[]{k / (float) n, 0, (k + 1) / (float) n, 0, (k + 1) / (float) n, 1, k / (float) n, 1};
    };

    /** The block face (3D value) a face lies on, facing out, if its solid's faces may be culled; else -1. */
    static int cullFace(Face f) {
        if (!f.solid.cull || !(f.kind == Box.FACE || f.kind == Frustum.CAP0 || f.kind == Frustum.CAP1)) return -1;
        int axis = Math.abs(f.nx) > 0.5 ? 0 : Math.abs(f.ny) > 0.5 ? 1 : 2;
        double n = axis == 0 ? f.nx : axis == 1 ? f.ny : f.nz;
        for (int i = 0; i < f.n; i++) {
            double v = f.xyz[3 * i + axis];
            if (n < 0 ? v > 0 : v < 16) return -1;
        }
        return axis == 1 ? (n < 0 ? 0 : 1) : axis == 2 ? (n < 0 ? 2 : 3) : (n < 0 ? 4 : 5);
    }

    static Quad quad(Face f) {
        float[] xyz = new float[12];
        for (int i = 0; i < 12; i++) xyz[i] = (float) (f.xyz[i] / 16);
        return new Quad((Identifier) f.solid.material, f.solid.argb, xyz, f.uv, cullFace(f));
    }
}
