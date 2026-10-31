package mods.eln.node.six;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import static mods.eln.i18n.I18N.tr;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor {
    public Class ElementClass, RenderClass;
    public VoltageLevelColor voltageLevelColor = VoltageLevelColor.None;

    public SixNodeDescriptor(String name, Class ElementClass, Class RenderClass) {
        super(name);
        this.ElementClass = ElementClass;
        this.RenderClass = RenderClass;
    }

    public SixNodeDescriptor(String name, Class ElementClass, Class RenderClass, String iconName) {
        super(name, iconName);
        this.ElementClass = ElementClass;
        this.RenderClass = RenderClass;
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)
// 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    public boolean hasVolume() {
        return false;
    }

    public boolean canBePlacedOnSide(Player player, Coordonate c, Direction side) {
        return canBePlacedOnSide(player, side);
    }

    public boolean canBePlacedOnSide(Player player, Direction side) {
        if (placeDirection != null) {
            for (Direction d : placeDirection) {
                if (d == side)
                    return true;
            }
            Utils.sendMessage(player, tr("You can't place this block at this side"));
            return false;
        }
        return true;
    }


    public void setGhostGroup(GhostGroup ghostGroup) {
        this.ghostGroup = ghostGroup;
    }

    protected GhostGroup ghostGroup = null;

    public boolean hasGhostGroup() {
        return ghostGroup != null;
    }

    public GhostGroup getGhostGroup(Direction side, LRDU front) {
        if (ghostGroup == null) return null;
        return ghostGroup.newRotate(side, front);
    }

    public int getGhostGroupUuid() {
        return -1;
    }

    /** Faces this device may be placed on (null = any). */
    @javax.annotation.Nullable
    public Direction[] getPlaceDirection() {
        return placeDirection;
    }

    public void setPlaceDirection(Direction d) {
        placeDirection = new Direction[]{d};
    }

    public void setPlaceDirection(Direction[] d) {
        placeDirection = d;
    }

    protected Direction[] placeDirection = null;

    public String checkCanPlace(Coordonate coord, Direction direction, LRDU front) {
        if (placeDirection != null) {
            boolean ok = false;
            for (Direction d : placeDirection) {
                if (d == direction) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return tr("You can't place this block at this side");
        }
        GhostGroup ghostGroup = getGhostGroup(direction, front);
        if (ghostGroup != null && !ghostGroup.canBePloted(coord))
            return tr("Not enough space for this block");
        return null;
    }

    public LRDU getFrontFromPlace(Direction side, Player player) {
        switch (side) {
            case YN:
            case YP:
                Direction viewDirection = Utils.entityLivingHorizontalViewDirection(player);
                LRDU front = side.getLRDUGoingTo(viewDirection);
                return side == Direction.YN ? front : front.inverse();

            default:
                return LRDU.Up;
        }
    }

    private int[] footprint;

    /** The scale the device's model is drawn at (its footprint follows). */
    protected float modelScale = 1f;

    /** Half the width of the device's body (its model without pins), voxels: an off-centre device's pins stick out one
     *  voxel past it. */
    public int bodyHalf() {
        return footprint()[0];
    }

    /**
     * Its model across the face, unturned: {yMin, yMax, zMin, zMax} in px from the centre, after modelScale (the element
     * frame: Down/Up along y, Left/Right along z). Null (the default): a square of bodyHalf each way. A device placed
     * anywhere whose model is not a square round its centre gives it, so its body and pins follow the model (Baughn: the
     * probes and relays work as voxel devices at their drawn size).
     */
    protected float[] bodyBounds() {
        return null;
    }

    /** The turn its render gives its model on side at front: the LRDU it passes to glRotateOnX, null if none. */
    public LRDU modelTurn(Direction side, LRDU front) {
        return front;
    }

    private float[] bounds;
    private boolean boundsDone;

    /** How far the model reaches toward each edge (index LRDU.toInt()), px, turned as drawn; null without bodyBounds. */
    public float[] modelReach(Direction side, LRDU front) {
        if (!boundsDone) {
            bounds = bodyBounds();
            boundsDone = true;
        }
        if (bounds == null) return null;
        LRDU turn = modelTurn(side, front);
        float y0 = Float.MAX_VALUE, y1 = -Float.MAX_VALUE, z0 = Float.MAX_VALUE, z1 = -Float.MAX_VALUE;
        for (float y : new float[]{bounds[0], bounds[1]})
            for (float z : new float[]{bounds[2], bounds[3]}) {
                double[] v = {0, y, z};
                if (turn != null) turn.rotateOnXnLeft(v);   // (as glRotateOnX turns it)
                y0 = Math.min(y0, (float) v[1]);
                y1 = Math.max(y1, (float) v[1]);
                z0 = Math.min(z0, (float) v[2]);
                z1 = Math.max(z1, (float) v[2]);
            }
        float[] r = new float[4];
        r[LRDU.Left.toInt()] = -z0;
        r[LRDU.Right.toInt()] = z1;
        r[LRDU.Down.toInt()] = -y0;
        r[LRDU.Up.toInt()] = y1;
        return r;
    }

    /**
     * Its body's reach toward each edge (index LRDU.toInt()), whole voxels: the model's reach rounded up to an odd number,
     * so with its centre on an even voxel each pin's tip is on an even voxel too (the routers' 2-voxel lattice: a cable
     * kit can end there). Without bodyBounds: bodyHalf each way.
     */
    public int[] bodyExtents(Direction side, LRDU front) {
        float[] r = modelReach(side, front);
        int[] e = new int[4];
        for (int i = 0; i < 4; i++) {
            if (r == null) {
                e[i] = bodyHalf();
                continue;
            }
            int n = Math.max(1, (int) Math.ceil(r[i] - 0.01));
            e[i] = n % 2 == 0 ? n + 1 : n;
        }
        return e;
    }

    /** {yMin, yMax, zMin, zMax} of these model parts, px, at modelScale (for bodyBounds); null if none. */
    protected float[] boundsOf(Obj3D.Obj3DPart... parts) {
        BoundingBox b = null;
        for (var p : parts) if (p != null) b = b == null ? p.boundingBox() : b.merge(p.boundingBox());
        if (b == null) return null;
        float s = 16 * modelScale;
        return new float[]{(float) b.min.y * s, (float) b.max.y * s, (float) b.min.z * s, (float) b.max.z * s};
    }

    /** May it go on side, by its own rule (canBePlacedOnSide, which tells the player), without telling anyone? For a
     *  device placed anywhere, whose preview asks every tick. */
    public boolean allowedOnSide(Direction side) {
        if (placeDirection == null) return true;
        for (Direction d : placeDirection) if (d == side) return true;
        return false;
    }

    /** Client: its model in the frame SixNodeRender sets up for an element (on its face, centred), turned to front; for
     *  the placement preview of a device placed anywhere. */
    public void drawPlaced(Direction side, LRDU front) {
    }

    /** Does its model have pins (chips)? Else a device placed off centre gets small pin boxes drawn for it. */
    public boolean drawsOwnPins() {
        return false;
    }

    /** May it be placed anywhere on a face of a voxel block (off centre, several to a face)? Chips. */
    public boolean placeAnywhere() {
        return false;
    }

    /**
     * What the device's model takes on its face, in voxels: {half its width across the face (square, so it needn't follow
     * front), its height off the face}. From the model parts the descriptor holds (its Obj3DPart fields, else its whole
     * Obj3D), once; a device without a model takes the old plate (12 x 12, 3 high). Hosted in a voxel block, this is
     * where it is clicked and outlined (Baughn: the click area of a Signal Trimmer didn't match its model).
     */
    public int[] footprint() {
        if (footprint == null) footprint = measureFootprint();
        return footprint;
    }

    /** Model parts left out of the footprint: hinged ones, whose raw geometry is drawn open (Baughn: a lamp supply's
     *  and a signal processor's open door made their box almost a full block). */
    protected java.util.Set<Obj3D.Obj3DPart> footprintIgnores() {
        return java.util.Set.of();
    }

    private int[] measureFootprint() {
        BoundingBox[] boxes = {BoundingBox.mergeIdentity(), BoundingBox.mergeIdentity()};   // (its parts, its whole models)
        boolean[] any = new boolean[2];
        collectModels(this, 0, boxes, any, footprintIgnores());
        BoundingBox b = any[0] ? boxes[0] : any[1] ? boxes[1] : null;
        if (b == null || !(b.max.x > b.min.x)) return new int[]{6, 3};
        double across = Math.max(Math.max(-b.min.y, b.max.y), Math.max(-b.min.z, b.max.z)) * modelScale;
        int half = (int) Math.ceil(across * 16 - 0.01), height = (int) Math.ceil(b.max.x * modelScale * 16 - 0.01);
        return new int[]{Math.max(2, Math.min(8, half)), Math.max(1, Math.min(16, height))};
    }

    /** The model parts and models held by o's fields, and by the fields of EA objects it holds (as a render obj). */
    private static void collectModels(Object o, int depth, BoundingBox[] boxes, boolean[] any, java.util.Set<Obj3D.Obj3DPart> ignore) {
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass())
            for (var f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive()) continue;
                Object v;
                try {
                    f.setAccessible(true);
                    v = f.get(o);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    continue;
                }
                if (v instanceof Obj3D.Obj3DPart part) {
                    if (ignore.contains(part)) continue;
                    boxes[0] = boxes[0].merge(part.boundingBox());
                    any[0] = true;
                } else if (v instanceof Obj3D.Obj3DPart[] a) {
                    for (var part : a)
                        if (part != null && !ignore.contains(part)) {
                            boxes[0] = boxes[0].merge(part.boundingBox());
                            any[0] = true;
                        }
                } else if (v instanceof Obj3D m) {
                    for (var part : m.parts()) boxes[1] = boxes[1].merge(part.boundingBox());
                    any[1] |= !m.parts().isEmpty();
                } else if (depth == 0 && v != null && !(v instanceof GenericItemBlockUsingDamageDescriptor) && !v.getClass().isEnum()
                    && v.getClass().getName().startsWith("mods.eln.sixnode.")) {
                    collectModels(v, 1, boxes, any, ignore);
                }
            }
    }

    /** The voxels (minX, minY, minZ, maxX, maxY, maxZ) the model takes on face f (EA Direction int). */
    public int[] footprintBox(int f) {
        int[] fp = footprint();
        int face = Direction.fromInt(f).toEnumFacing().get3DDataValue();
        int n = face / 2;   // (DOWN/UP: y, NORTH/SOUTH: z, WEST/EAST: x)
        n = n == 0 ? 1 : n == 1 ? 2 : 0;
        int[] b = {8 - fp[0], 8 - fp[0], 8 - fp[0], 8 + fp[0], 8 + fp[0], 8 + fp[0]};
        boolean low = face % 2 == 0;
        b[n] = low ? 0 : 16 - fp[1];
        b[n + 3] = low ? fp[1] : 16;
        return b;
    }
}
