package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A pipe tank (Baughn 2026-10-29, the shaft redo): one vessel with a vapour port on top and on each side up high, and a
 * liquid port underneath and on each side down low, so it serves as a steam drum (boiler steam in, dry steam off the
 * top, water back down), a separator (wet steam in, steam off the top, water off the bottom) or a hotwell (condensate
 * in, a pump drawing from the bottom). A sight glass on its front shows the water. Over its burst pressure it bursts.
 * Model: mods.eln.cad.models.Tank.
 */
public class TankDescriptor extends TransparentNodeDescriptor {
    final double volume, workingPa, burstPa;
    final Obj3D obj;

    public TankDescriptor(String name, double volume, double workingPa, double burstPa, String model) {
        super(name, TankElement.class, TankRender.class);
        this.volume = volume;
        this.workingPa = workingPa;
        this.burstPa = burstPa;
        this.obj = Eln.obj.getObj(model);
    }

    /**
     * No floor needed (Baughn 2026-11-10): a tank stands on its frame, so a pipe can run to its underside port; it used
     * to break when the block under it went.
     */
    @Override
    public boolean mustHaveFloor() {
        return false;
    }

    /** Volume (m^3) of so many buckets of water (EA units). */
    public static double bucketsVolume(double buckets) {
        return buckets * 1000 * mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / mods.eln.sim.fluid.Water.RHO_L;
    }

    /** The vessel's shell (a box's render scales it to the box). */
    void drawShell() {
        drawPart("shell");
    }

    /**
     * The sight glass: the water filled to `fill` (0..1) and above it steam (a vessel whose gas is mostly steam) or the
     * dark of the empty glass (air).
     */
    void drawGlass(float fill, boolean steam) {
        drawGlass(fill, steam, new float[0], new int[0]);
    }

    /**
     * The sight glass: its liquid to fill, or with layers (oil over water, Baughn 2026-11-03: data-driven fluids) each
     * layer in its species' colour over the window's own (water's), then the steam above if it's mostly steam.
     */
    void drawGlass(float fill, boolean steam, float[] tops, int[] colours) {
        drawPart("glass");
        fill = Math.max(0, Math.min(1, fill));
        if (tops.length <= 1) {
            if (fill > 0.005f) drawScaled("level", GLASS_Y0, fill);
        } else {
            float below = 0;
            float window = (GLASS_Y1 - GLASS_Y0) / 16f;
            for (int l = 0; l < tops.length; l++) {
                float top = Math.max(below, Math.min(1, tops[l]));
                if (top - below > 0.005f) {
                    int c = colours[l];
                    boolean water = c == mods.eln.sim.fluid.SpeciesRegistry.get().byId(mods.eln.sim.fluid.SpeciesRegistry.WATER).colour;
                    // (the window's water is blue; other liquids are the white "liquid" part, tinted by their colour: tinting the
                    // blue made gasoline teal, AdventurAgent)
                    if (!water) mods.eln.gl.GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1);
                    mods.eln.gl.GL11.glPushMatrix();
                    mods.eln.gl.GL11.glTranslatef(0, below * window, 0);
                    drawScaled(water ? "level" : "liquid", GLASS_Y0, top - below);
                    mods.eln.gl.GL11.glPopMatrix();
                    if (!water) mods.eln.gl.GL11.glColor4f(1, 1, 1, 1);
                }
                below = top;
            }
        }
        if (steam && fill < 0.995f) drawScaled("vapour", GLASS_Y1, 1 - fill);
    }

    /**
     * A block of a box's outline or collision: the box's shell and sight glass as TankRender draws them, cut to this
     * block (Baughn 2026-11-09: a box kept its lone tanks' hitboxes, small cylinders with gaps between). The box comes
     * from the element's group on the server and from the render's published size on the client; a lone tank takes
     * its model's hitbox.
     */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape shapeAt(mods.eln.node.transparent.TransparentNodeEntity e, boolean collision) {
        int[] b = boxOf(e);
        if (b == null) return null;
        String key = java.util.Arrays.toString(b);
        mods.eln.node.transparent.ModelHitbox h;
        synchronized (boxHitboxes) {
            h = boxHitboxes.get(key);
            if (h == null && !boxHitboxes.containsKey(key)) {
                h = boxHitbox(b);
                if (boxHitboxes.size() > 512) boxHitboxes.clear();
                boxHitboxes.put(key, h);
            }
        }
        if (h == null) return net.minecraft.world.phys.shapes.Shapes.empty();
        return collision ? h.collision(mods.eln.misc.Direction.XN) : h.shape(mods.eln.misc.Direction.XN);
    }

    private final java.util.Map<String, mods.eln.node.transparent.ModelHitbox> boxHitboxes = new java.util.HashMap<>();

    /** {front ordinal, sx, sy, sz, ox, oy, oz} of the box e's block is in, or null (a lone tank, or not known yet). */
    static int[] boxOf(mods.eln.node.transparent.TransparentNodeEntity e) {
        var level = e.getLevel();
        if (level == null) return null;
        if (!level.isClientSide()) {
            if (!(e.getNode() instanceof mods.eln.node.transparent.TransparentNode n) || !(n.element instanceof TankElement t)) return null;
            TankGroup g = t.group;
            if (g == null || t.front == null) return null;
            var c = t.node.coordonate;
            return new int[]{t.front.ordinal(), g.sx, g.sy, g.sz, c.x - g.x0, c.y - g.y0, c.z - g.z0};
        }
        if (!(e.elementRender instanceof TankRender r) || r.front == null) return null;
        if (r.sx * r.sy * r.sz <= 1) return null;
        return new int[]{r.front.ordinal(), r.sx, r.sy, r.sz, r.ox, r.oy, r.oz};
    }

    /** The hitbox (front XN, i.e. already in world axes) of block b of a box: see shapeAt. */
    mods.eln.node.transparent.ModelHitbox boxHitbox(int[] b) {
        if (obj == null) return null;
        var front = mods.eln.misc.Direction.values()[b[0]];
        int sx = b[1], sy = b[2], sz = b[3];
        boolean alongX = front == mods.eln.misc.Direction.XN || front == mods.eln.misc.Direction.XP;
        double across = alongX ? sx : sz;
        // (from the box's centre to this block's, world axes)
        double dx = b[4] - (sx - 1) / 2.0, dy = b[5] - (sy - 1) / 2.0, dz = b[6] - (sz - 1) / 2.0;
        List<List<double[]>> parts = new java.util.ArrayList<>();
        for (String name : new String[]{"shell", "glass"}) {
            var part = obj.getPart(name);
            if (part == null) continue;
            List<double[]> tris = new java.util.ArrayList<>();
            part.triangles(tris);
            boolean glass = name.equals("glass");
            for (double[] t : tris)
                for (int i = 0; i < 9; i += 3) {
                    double x = t[i], y = t[i + 1], z = t[i + 2];   // (about the block's centre, front XN)
                    if (glass) {   // (on the scaled shell's front, the box's height: TankRender.draw)
                        x -= (across - 1) * TankRender.SHELL_R / 16.0;
                        y *= sy;
                    }
                    double[] w = mods.eln.node.transparent.ModelHitbox.turnedPoint(x, y, z, front);
                    if (!glass) {
                        w[0] *= sx;
                        w[1] *= sy;
                        w[2] *= sz;
                    }
                    t[i] = w[0] - dx + 0.5;
                    t[i + 1] = w[1] - dy + 0.5;
                    t[i + 2] = w[2] - dz + 0.5;
                }
            if (!tris.isEmpty()) parts.add(tris);
        }
        return parts.isEmpty() ? null : mods.eln.node.transparent.ModelHitbox.computedParts(parts);
    }

    void drawPart(String name) {
        var p = obj == null ? null : obj.getPart(name);
        if (p != null) p.draw();
    }

    /** The sight glass's window's ends (px; mods.eln.cad.models.Tank GLASS_Y0, GLASS_Y1). */
    static final float GLASS_Y0 = 2, GLASS_Y1 = 14;

    /** A part of the window's whole height, scaled in y by `share` about `about` (px). */
    void drawScaled(String name, float about, float share) {
        var l = obj == null ? null : obj.getPart(name);
        if (l == null) return;
        float at = about / 16f - 0.5f;
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glTranslatef(0, at, 0);
        mods.eln.gl.GL11.glScalef(1, share, 1);
        mods.eln.gl.GL11.glTranslatef(0, -at, 0);
        l.draw();
        mods.eln.gl.GL11.glPopMatrix();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add(String.format("Holds %.0f buckets of water. Works at up to %.0f bar; bursts over %.0f.",
            volume * mods.eln.sim.fluid.Water.RHO_L / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER / 1000, workingPa / 1e5, burstPa / 1e5));
        list.add("Steam off the top and the high side ports; water off the bottom and the low ones.");
        list.add("A steam drum, a separator for wet steam, or a hotwell for a pump to draw from.");
    }
}
