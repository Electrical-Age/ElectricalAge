package mods.eln.node.transparent;

import mods.eln.gl.GL11;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNode.FrontType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class TransparentNodeDescriptor extends GenericItemBlockUsingDamageDescriptor {
    public Class ElementClass, RenderClass;
    protected VoltageLevelColor voltageLevelColor = VoltageLevelColor.None;

    /** Faces with a floor terminal plate, where floor cables draw a lug (null = none; see CableTerminals). */
    public mods.eln.cable.CableTerminals cableTerminals;

    public TransparentNodeDescriptor(String name, Class ElementClass, Class RenderClass) {
        super(name);
        this.ElementClass = ElementClass;
        this.RenderClass = RenderClass;
    }


    protected GhostGroup ghostGroup = null;

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)
// 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    public void objItemScale(Obj3D obj) {
        if (obj == null) return;
        float factor = obj.yDim * 0.6f;
        //factor = obj.dimMaxInv*1.0f;
        factor = Math.max(factor, (Math.max(obj.zMax, -obj.xMin) + Math.max(obj.xMax, -obj.zMin)) * 0.7f);
        factor = 1f / factor;

        GL11.glScalef(factor, factor, factor);
        //GL11.glTranslatef((Math.max(obj.zMax,-obj.xMin) - Math.max(obj.xMax,-obj.zMin))*0.5f,-((obj.yMax + Math.max(-obj.xMin,obj.zMax)*0.3f) + (obj.yMin + Math.min(obj.zMin,-obj.xMax)*0.3f))*0.4f,0.0f);
        GL11.glTranslatef((Math.min(obj.zMin, obj.xMin) + Math.max(obj.xMax, obj.zMax)) / 2 - (obj.xMax + obj.xMin) / 2, 1.0f - (obj.xMax + obj.xMin) / 2 - (obj.zMax + obj.zMin) / 2 - (obj.yMax + obj.yMin) / 2, 0.0f);
    }

    /** The device's block properties (registry); most devices are metal machines. */
    public net.minecraft.world.level.block.state.BlockBehaviour.Properties blockProperties(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.mapColor(net.minecraft.world.level.material.MapColor.METAL).strength(1.0f, 1.0f)
            .sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion();
    }

    /** The device's block (registry): a plain TransparentNodeBlock, or a subclass with its own model and state. */
    public TransparentNodeBlock newBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return new TransparentNodeBlock(p, this);
    }

    public FrontType getFrontType() {
        return FrontType.PlayerViewHorizontal;
    }

    public boolean mustHaveFloor() {
        return true;
    }

    public boolean mustHaveCeiling() {
        return false;
    }

    public boolean mustHaveWall() {
        return false;
    }

    public boolean mustHaveWallFrontInverse() {
        return false;
    }

    /**
     * Does the block next to coord, toward to, hold a device on its face toward it? Any full face does (Baughn): a full
     * block, the top of a slab or of an upside-down stair, the back of a stair, glass. 1.12 asked for a whole opaque
     * cube.
     */
    public static boolean holds(Coordonate coord, Direction to) {
        Level w = coord.world();
        var pos = coord.pos().relative(to.toEnumFacing());
        return w.getBlockState(pos).isFaceSturdy(w, pos, to.getInverse().toEnumFacing(), SupportType.FULL);
    }

    /** What else this device can stand on (mustHaveFloor), beside a full face: a hopper. */
    public boolean isFloor(Block below) {
        return below instanceof HopperBlock;
    }

    public String checkCanPlace(Coordonate coord, Direction front) {
        if (mustHaveFloor() && !holds(coord, Direction.YN)) {
            Coordonate temp = new Coordonate(coord);
            temp.move(Direction.YN);
            Block block = temp.getBlock();
            if (block == null || !isFloor(block))
                return tr("You can't place this block at this side");
        }
        if (mustHaveCeiling() && !holds(coord, Direction.YP)) return tr("You can't place this block at this side");
        if (mustHaveWallFrontInverse() && !holds(coord, front.getInverse())) return tr("You can't place this block at this side");
        if (mustHaveWall() && !holds(coord, Direction.XN) && !holds(coord, Direction.XP) && !holds(coord, Direction.ZN)
            && !holds(coord, Direction.ZP))
            return tr("You can't place this block at this side");

        GhostGroup ghostGroup = getGhostGroup(front);
        if (ghostGroup != null && ghostGroup.canBePloted(coord) == false) return tr("Not enough space for this block");
        return null;
    }


    public Direction getFrontFromPlace(Direction side, LivingEntity entityLiving) {
        Direction front = Direction.XN;
        switch (getFrontType()) {
            case BlockSide:
                front = side;
                break;
            case BlockSideInv:
                front = side.getInverse();
                break;
            case PlayerView:
                front = Utils.entityLivingViewDirection(entityLiving).getInverse();
                break;
            case PlayerViewHorizontal:
                front = Utils.entityLivingHorizontalViewDirection(entityLiving).getInverse();
                break;

        }
        return front;
    }

    public boolean hasGhostGroup() {
        return ghostGroup != null;
    }

    public GhostGroup getGhostGroup(Direction front) {
        if (ghostGroup == null) return null;
        return ghostGroup.newRotate(front);
    }

    public int getGhostGroupUuid() {

        return -1;
    }

    public int getSpawnDeltaX() {

        return 0;
    }

    public int getSpawnDeltaY() {

        return 0;
    }

    public int getSpawnDeltaZ() {

        return 0;
    }

    public void addCollisionBoxesToList(AABB par5AxisAlignedBB, List list, Level world, int x, int y, int z) {
        AABB bb = new AABB(x, y, z, x + 1, y + 1, z + 1); // 1.7.10: stone.getCollisionBoundingBoxFromPool (absolute)
        if (par5AxisAlignedBB.intersects(bb)) list.add(bb);
    }

    public void setGhostGroup(GhostGroup ghostGroup) {
        this.ghostGroup = ghostGroup;
    }
}
