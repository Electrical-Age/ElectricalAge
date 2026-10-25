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

    private int[] measureFootprint() {
        BoundingBox[] boxes = {BoundingBox.mergeIdentity(), BoundingBox.mergeIdentity()};   // (its parts, its whole models)
        boolean[] any = new boolean[2];
        collectModels(this, 0, boxes, any);
        BoundingBox b = any[0] ? boxes[0] : any[1] ? boxes[1] : null;
        if (b == null || !(b.max.x > b.min.x)) return new int[]{6, 3};
        double across = Math.max(Math.max(-b.min.y, b.max.y), Math.max(-b.min.z, b.max.z));
        int half = (int) Math.ceil(across * 16 - 0.01), height = (int) Math.ceil(b.max.x * 16 - 0.01);
        return new int[]{Math.max(2, Math.min(8, half)), Math.max(1, Math.min(16, height))};
    }

    /** The model parts and models held by o's fields, and by the fields of EA objects it holds (as a render obj). */
    private static void collectModels(Object o, int depth, BoundingBox[] boxes, boolean[] any) {
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
                    boxes[0] = boxes[0].merge(part.boundingBox());
                    any[0] = true;
                } else if (v instanceof Obj3D.Obj3DPart[] a) {
                    for (var part : a)
                        if (part != null) {
                            boxes[0] = boxes[0].merge(part.boundingBox());
                            any[0] = true;
                        }
                } else if (v instanceof Obj3D m) {
                    for (var part : m.parts()) boxes[1] = boxes[1].merge(part.boundingBox());
                    any[1] |= !m.parts().isEmpty();
                } else if (depth == 0 && v != null && !(v instanceof GenericItemBlockUsingDamageDescriptor) && !v.getClass().isEnum()
                    && v.getClass().getName().startsWith("mods.eln.sixnode.")) {
                    collectModels(v, 1, boxes, any);
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
