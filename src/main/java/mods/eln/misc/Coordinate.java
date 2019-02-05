package mods.eln.misc;

import mods.eln.node.NodeBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.Nonnull;

public class Coordinate implements INBTTReady {

    @Nonnull
    public BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0, 0, 0);
    private int dimension = 0;
    private World w = null;

    public Coordinate() {
    }

    public Coordinate(Coordinate coord) {
        BlockPos o = coord.pos;
        pos.setPos(o.getX(), o.getY(), o.getZ());
        dimension = coord.dimension;
    }

    public Coordinate(NBTTagCompound nbt, String str) {
        readFromNBT(nbt, str);
    }

    public Coordinate(NodeBlockEntity entity) {
        BlockPos o = entity.getPos();
        pos.setPos(o.getX(), o.getY(), o.getZ());
        dimension = entity.getWorld().provider.getDimension();
    }

    public Coordinate(@Nonnull BlockPos o, int dimension) {
        pos.setPos(o.getX(), o.getY(), o.getZ());
        this.dimension = dimension;
    }

    public Coordinate(@Nonnull BlockPos o, @Nonnull World w) {
        pos.setPos(o.getX(), o.getY(), o.getZ());
        this.dimension = w.provider.getDimension();
    }

    public Coordinate(int x, int y, int z, int dimension) {
        pos.setPos(x, y, z);
        this.dimension = dimension;
    }

    public Coordinate(int x, int y, int z, World world) {
        pos.setPos(x, y, z);
        dimension = world.provider.getDimension();
        if (world.isRemote)
            this.w = world;
    }

    public Coordinate(TileEntity entity) {
        BlockPos o = entity.getPos();
        pos.setPos(o.getX(), o.getY(), o.getZ());
        dimension = entity.getWorld().provider.getDimension();
        if (entity.getWorld().isRemote)
            this.w = entity.getWorld();
    }

    @Override
    public int hashCode() {
        return pos.hashCode() * 31 + dimension;
    }


    public int getDimension() {
        return dimension;
    }

    public World world() {
        if (w == null) {
            // TODO(1.12): This is not cached. Oversight or deliberate?
            return FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(getDimension());
        }
        return w;
    }

    public Coordinate newWithOffset(int x, int y, int z) {
        return new Coordinate(pos.add(x, y, z), dimension);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Coordinate)) return false;
        Coordinate id = (Coordinate) obj;
        return id.pos == pos && id.dimension == dimension;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        int x = nbt.getInteger(str + "x");
        int y = nbt.getInteger(str + "y");
        int z = nbt.getInteger(str + "z");
        pos.setPos(x, y, z);
        dimension = nbt.getInteger(str + "d");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setInteger(str + "x", pos.getX());
        nbt.setInteger(str + "y", pos.getY());
        nbt.setInteger(str + "z", pos.getZ());
        nbt.setInteger(str + "d", dimension);
        return nbt;
    }

    @Override
    public String toString() {
        return "X : " + pos.getX() + " Y : " + pos.getY() + " Z : " + pos.getZ() + " D : " + dimension;
    }

    public void move(Direction facing) {
        pos.move(facing.toForge());
    }

    public Coordinate moved(final Direction direction) {
        Coordinate moved = new Coordinate(this);
        moved.move(direction);
        return moved;
    }

    public static AxisAlignedBB getAxisAlignedBB(Coordinate a, Coordinate b) {
        return new AxisAlignedBB(a.pos, b.pos);
    }

    public AxisAlignedBB getAxisAlignedBB(int ray) {
        return new AxisAlignedBB(
            new BlockPos(pos.getX() - ray, pos.getY() - ray, pos.getZ() - ray),
            new BlockPos(pos.getX() + ray + 1, pos.getY() + ray + 1, pos.getZ() + ray + 1));
    }

    public double distanceTo(Entity e) {
        return Math.abs(e.posX - (pos.getX() + 0.5)) + Math.abs(e.posY - (pos.getY() + 0.5)) + Math.abs(e.posZ - (pos.getZ() + 0.5));
    }

    public boolean doesBlockExist() {
        return !world().isAirBlock(pos);
    }

    public boolean doesWorldExist() {
        return DimensionManager.getWorld(dimension) != null;
    }

    public void setPosition(double[] vp) {
        pos.setPos(vp[0], vp[1], vp[2]);
    }

    public void setPosition(Vec3d vp) {
        pos.setPos(vp.xCoord, vp.yCoord, vp.zCoord);
    }

    public TileEntity getTileEntity() {
        return world().getTileEntity(pos);
    }

    public double trueDistanceTo(Coordinate c) {
        return c.pos.getDistance(pos.getX(), pos.getY(), pos.getZ());
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
        w = null;
    }

    public void copyFrom(Coordinate c) {
        pos.setPos(c.pos.getX(), c.pos.getY(), c.pos.getZ());
        this.dimension = c.dimension;
    }

    public void applyTransformation(Direction front, Coordinate coordinate) {
        front.rotateFromXN(this);
        BlockPos o = coordinate.pos;
        pos.setPos(pos.getX() + o.getX(),
        pos.getY() + o.getY(),
        pos.getZ() + o.getZ());
    }

    public void setWorld(World worldObj) {
        if (worldObj.isRemote)
            w = worldObj;
        dimension = worldObj.provider.getDimension();
    }

    public void setBlock(Block b) {
        world().setBlockState(pos, b.getDefaultState());
    }

    public boolean isAir() {
        return world().isAirBlock(pos);
    }

    public IBlockState getBlockState() {
        return world().getBlockState(pos);
    }

    public Block getBlock() { return getBlockState().getBlock(); }

    public int getMeta(){
        return Utils.getMetaFromPos(this);
    }

    public Coordinate subtract(Coordinate b) {
        return newWithOffset(-b.pos.getX(), -b.pos.getY(), -b.pos.getZ());
    }
    public Coordinate negate() {
        return new Coordinate(-pos.getX(), -pos.getY(), -pos.getZ(), dimension);
    }

}
