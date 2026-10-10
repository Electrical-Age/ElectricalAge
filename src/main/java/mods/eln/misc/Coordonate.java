package mods.eln.misc;


import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

public class Coordonate implements INBTTReady {

    public int x, y, z, dimention;

    public Coordonate() {
        x = 0;
        y = 0;
        z = 0;
        dimention = 0;
    }

    public Coordonate(Coordonate coord) {
        x = coord.x;
        y = coord.y;
        z = coord.z;
        dimention = coord.dimention;
    }

    public Coordonate(CompoundTag nbt, String str) {
        readFromNBT(nbt, str);
    }

    @Override
    public int hashCode() {
        return (x + y) * 0x10101010 + z;
    }


    public int worldDimension() {
        return dimention;
    }

    private Level w = null;

    public Level world() {
        //	Side sideCS = FMLCommonHandler.instance().getEffectiveSide();
        //	if (sideCS == Side.CLIENT) return null;

        //Minecraft m = Minecraft.getMinecraft();
        //if(FMLCommonHandler.instance().getSidedDelegate().)
        //WorldManager
        //Minecraft.getMinecraft().
        //World
        //Minecraft m = Minecraft.getMinecraft();

		
		/*if(w == null) *///w = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(worldDimension());

        if (w == null) {
            return Dimensions.level(worldDimension());
        }
        return w;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public Coordonate(BlockPos p, Level world) {
        this(p.getX(), p.getY(), p.getZ(), world);
    }

    public Coordonate(int x, int y, int z, int dimention) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimention = dimention;
    }

    public Coordonate(int x, int y, int z, Level world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimention = Dimensions.id(world);
        if (world.isClientSide())
            this.w = world;
    }

    public Coordonate(BlockEntity entity) {
        this(entity.getBlockPos(), entity.getLevel());
    }

    public Coordonate newWithOffset(int x, int y, int z) {
        return new Coordonate(this.x + x, this.y + y, this.z + z, dimention);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Coordonate)) return false;
        Coordonate id = (Coordonate) obj;
        return id.x == x && id.y == y && id.z == z && id.dimention == dimention;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        x = nbt.getIntOr(str + "x", 0);
        y = nbt.getIntOr(str + "y", 0);
        z = nbt.getIntOr(str + "z", 0);
        dimention = nbt.getIntOr(str + "d", 0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putInt(str + "x", x);
        nbt.putInt(str + "y", y);
        nbt.putInt(str + "z", z);
        nbt.putInt(str + "d", dimention);
    }

    @Override
    public String toString() {
        return "X : " + x + " Y : " + y + " Z : " + z + " D : " + dimention;
    }

    public void move(Direction dir) {
        switch (dir) {
            case XN:
                x--;
                break;
            case XP:
                x++;
                break;
            case YN:
                y--;
                break;
            case YP:
                y++;
                break;
            case ZN:
                z--;
                break;
            case ZP:
                z++;
                break;
            default:
                break;
        }
    }

    public Coordonate moved(final Direction direction) {
        Coordonate moved = new Coordonate(this);
        moved.move(direction);
        return moved;
    }

    public Block getBlock() {
        return world().getBlockState(pos()).getBlock();
    }

    public static AABB getAxisAlignedBB(Coordonate a, Coordonate b) {
        AABB bb = new AABB(
            Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z),
            Math.max(a.x, b.x) + 1.0, Math.max(a.y, b.y) + 1.0, Math.max(a.z, b.z) + 1.0);
        return bb;
    }

    public AABB getAxisAlignedBB(int ray) {
        AABB bb = new AABB(
            x - ray, y - ray, z - ray,
            x + ray + 1, y + ray + 1, z + ray + 1);
        return bb;
    }

    public double distanceTo(Entity e) {
        return Math.abs(e.getX() - (x + 0.5)) + Math.abs(e.getY() - (y + 0.5)) + Math.abs(e.getZ() - (z + 0.5));
    }
	
	/*public void setBlock(int id, int meta) {
		WorldCompat.setBlock(world(), x, y, z, id, meta, 2);
	}*/

    /** The block's chunk is loaded (1.12: blockExists): nodes simulate everywhere, but touch the world only here. */
    public boolean getBlockExist() {
        Level w = world();
        if (w == null) return false;
        return w.isLoaded(pos());
    }

    public boolean getWorldExist() {
        return world() != null;
    }

    public void copyTo(double[] v) {
        v[0] = x + 0.5;
        v[1] = y + 0.5;
        v[2] = z + 0.5;

    }

    public void setPosition(double[] vp) {
        this.x = (int) vp[0];
        this.y = (int) vp[1];
        this.z = (int) vp[2];
    }

    public void setPosition(Vec3 vp) {
        this.x = (int) vp.x;
        this.y = (int) vp.y;
        this.z = (int) vp.z;
    }

    public BlockEntity getTileEntity() {
        return world().getBlockEntity(pos());
    }

    public void invalidate() {
        x = -1;
        y = -1;
        z = -1;
        dimention = Dimensions.INVALID;
    }

    public boolean isValid() {
        return dimention != Dimensions.INVALID;
    }

    public double trueDistanceTo(Coordonate c) {
        long dx = x - c.x;
        long dy = y - c.y;
        long dz = z - c.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public void setDimention(int dimention) {
        this.dimention = dimention;
        w = null;
    }

    public void copyFrom(Coordonate c) {
        this.x = c.x;
        this.y = c.y;
        this.z = c.z;
        this.dimention = c.dimention;
    }

    public void applyTransformation(Direction front, Coordonate coordonate) {
        front.rotateFromXN(this);
        x += coordonate.x;
        y += coordonate.y;
        z += coordonate.z;
    }

    public void setWorld(Level world) {
        if (world.isClientSide())
            w = world;
        dimention = Dimensions.id(world);
    }

    public void setBlock(Block b) {
        world().setBlockAndUpdate(pos(), b.defaultBlockState());
    }

    public int compareTo(Coordonate o) {
        if (dimention != o.dimention) {
            return dimention - o.dimention;
        } else if (x != o.x) {
            return x - o.x;
        } else if (y != o.y) {
            return y - o.y;
        } else if (z != o.z) {
            return z - o.z;
        }
        return 0;
    }

    public Coordonate subtract(Coordonate b) {
        return newWithOffset(-b.x, -b.y, -b.z);
    }

    public Coordonate negate() {
        return new Coordonate(-x, -y, -z, dimention);
    }
}
