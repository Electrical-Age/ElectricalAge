package mods.eln.misc;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Represents the 6 possible directions along the axis of a block.
 */
public enum Direction {
    /**
     * -X
     */
    XN(0),
    /**
     * +X
     */
    XP(1),

    /**
     * -Y
     */
    YN(2), //MC-Code starts with 0 here
    /**
     * +Y
     */
    YP(3), // 1...

    /**
     * -Z
     */
    ZN(4),
    /**
     * +Z
     */
    ZP(5);

    static final Direction[] intToDir = {XN, XP, YN, YP, ZN, ZP};

    int dir;

    Direction(int dir) {
        this.dir = dir;
    }

    public int getInt() {
        return dir;
    }

    public boolean isNotY() {
        return this != YP && this != YN;
    }

    public boolean isY() {
        return this == YP || this == YN;
    }

    public void applyTo(double[] vector, double distance) {
        if (dir == 0) vector[0] -= distance;
        if (dir == 1) vector[0] += distance;
        if (dir == 2) vector[1] -= distance;
        if (dir == 3) vector[1] += distance;
        if (dir == 4) vector[2] -= distance;
        if (dir == 5) vector[2] += distance;
    }

    public void applyTo(int[] vector, int distance) {
        if (dir == 0) vector[0] -= distance;
        if (dir == 1) vector[0] += distance;
        if (dir == 2) vector[1] -= distance;
        if (dir == 3) vector[1] += distance;
        if (dir == 4) vector[2] -= distance;
        if (dir == 5) vector[2] += distance;
    }

    public int getHorizontalIndex() {
        switch (this) {
            case XN:
                return 0;
            case XP:
                return 1;
            case YN:
                return 0;
            case YP:
                return 0;
            case ZN:
                return 2;
            case ZP:
                return 3;
            default:
                return 0;
        }
    }

    public static Direction fromHorizontalIndex(int nbr) {
        switch (nbr) {
            case 0:
                return XN;
            case 1:
                return XP;
            case 2:
                return ZN;
            case 3:
                return ZP;
            default:
                return XN;
        }
    }

	/*public CoordinateTuple ApplyToCoordinates(CoordinateTuple coordinates) {
		CoordinateTuple ret = new CoordinateTuple(coordinates);
		
		ret.coords[dir / 2] += GetSign();
		
		return ret;
	}*/

    /**
     * Get the tile entity next to a tile entity following this direction.
     *
     * @param tileEntity tile entity to check
     * @return Adjacent tile entity or null if none exists
     */
    public BlockEntity applyToTileEntity(BlockEntity tileEntity) {
        if (tileEntity == null) return null;
        int coords[] = {tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY(), tileEntity.getBlockPos().getZ()};

        coords[dir / 2] += getSign();

        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(coords[0], coords[1], coords[2]);
        if (tileEntity.getLevel() != null && tileEntity.getLevel().isLoaded(p)) {
            return tileEntity.getLevel().getBlockEntity(p);
        } else {
            return null;
        }
    }

    public BlockEntity applyToTileEntityAndSameClassThan(BlockEntity tileEntity, Class c) {
        if (tileEntity == null) return null;
        BlockEntity findedEntity = applyToTileEntity(tileEntity);
        if (findedEntity == null) return null;
        if (!Utils.isTheClass(findedEntity, c)) return null;
        return findedEntity;
    }

    /**
     * Get the inverse of this direction (XN -> XP, XP -> XN, etc.)
     *
     * @return Inverse direction
     */
    public Direction getInverse() {
        int inverseDir = dir - getSign();

        for (Direction direction : Direction.values()) {
            if (direction.dir == inverseDir) return direction;
        }

        return this;
    }

    /**
     * Convert this direction to a Minecraft side value.
     *
     * @return Minecraft side value
     */
    public int toSideValue() {
        return (dir + 4) % 6;
    }

    /**
     * Determine direction sign (N for negative or P for positive).
     *
     * @return -1 if the direction is negative, +1 if the direction is positive
     */
    private int getSign() {
        return (dir % 2) * 2 - 1;
    }
// 26.x port: renderBlockFace() removed (client rendering; redone in P3)

    static public Direction fromInt(int idx) {
        for (Direction direction : Direction.values()) {
            if (direction.dir == idx) return direction;
        }
        return null;
    }

    static public Direction fromIntMinecraftSide(int idx) {
        idx = (idx + 2) % 6;
        for (Direction direction : Direction.values()) {
            if (direction.dir == idx) return direction;
        }
        return null;
    }

    public Direction right() {
        switch (this) {
            case XN:
                return ZP;
            case XP:
                return ZN;
            case YN:
                return ZN;
            case YP:
                return ZP;
            case ZN:
                return XN;
            case ZP:
                return XP;
        }
        return null;
    }

    public Direction left() {
        return right().getInverse();
    }

    public Direction up() {
        switch (this) {
            case XN:
                return YP;
            case XP:
                return YP;
            case YN:
                return XP;
            case YP:
                return XP;
            case ZN:
                return YP;
            case ZP:
                return YP;
        }
        return null;
    }

    public Direction down() {
        return up().getInverse();
    }

    public Direction back() {
        return getInverse();
    }

    public Direction applyLRDU(LRDU lrdu) {
        switch (lrdu) {
            case Down:
                return this.down();
            case Left:
                return this.left();
            case Right:
                return this.right();
            case Up:
                return this.up();
            default:
                break;
        }
        return null;
    }

    public LRDU getLRDUGoingTo(Direction target) {
        for (LRDU lrdu : LRDU.values()) {
            if (target == applyLRDU(lrdu)) return lrdu;
        }
        return null;
    }
// 26.x port: glRotateXnRef() removed (client rendering; redone in P3)
// 26.x port: glRotateXnRefInv() removed (client rendering; redone in P3)
// 26.x port: glRotateZnRef() removed (client rendering; redone in P3)

    public BlockEntity getTileEntity(Coordonate coordonate) {
        int x = coordonate.x, y = coordonate.y, z = coordonate.z;
        switch (this) {
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

        return coordonate.world().getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
    }

    public void writeToNBT(CompoundTag nbt, String name) {
        nbt.putByte(name, (byte) getInt());
    }

    static public Direction readFromNBT(CompoundTag nbt, String name) {
        return Direction.fromInt(nbt.getByteOr(name, (byte) 0));
    }

    public void rotateFromXN(double[] p) {
        double x = p[0], y = p[1], z = p[2];
        switch (this) {
            case XN:
                break;
            case XP:
                p[0] = -x;
                p[2] = -z;
                break;
            case YN:
                p[0] = y;
                p[1] = x;
                p[2] = -z;
                break;
            case YP:
                p[0] = y;
                p[1] = -x;
                p[2] = z;
                break;
            case ZN:
                p[0] = -z;
                p[2] = x;
                break;
            case ZP:
                p[0] = z;
                p[2] = -x;
                break;
            default:
                break;
        }
    }

    public void rotateFromXN(int[] p) {
        int x = p[0], y = p[1], z = p[2];
        switch (this) {
            case XN:
                break;
            case XP:
                p[0] = -x;
                p[2] = -z;
                break;
            case YN:
                p[0] = y;
                p[1] = x;
                p[2] = -z;
                break;
            case YP:
                p[0] = y;
                p[1] = -x;
                p[2] = z;
                break;
            case ZN:
                p[0] = -z;
                p[2] = x;
                break;
            case ZP:
                p[0] = z;
                p[2] = -x;
                break;
            default:
                break;
        }
    }

    /** 1.12: Vec3 is immutable, so this returns the rotated vector (callers assign it). */
    public Vec3 rotateFromXN(Vec3 p) {
        double x = p.x, y = p.y, z = p.z;
        switch (this) {
            case XN:
                return p;
            case XP:
                return new Vec3(-x, y, -z);
            case YN:
                return new Vec3(y, x, -z);
            case YP:
                return new Vec3(y, -x, z);
            case ZN:
                return new Vec3(-z, y, x);
            case ZP:
                return new Vec3(z, y, -x);
            default:
                return p;
        }
    }

    public void rotateFromXN(Coordonate p) {
        int x = p.x, y = p.y, z = p.z;
        switch (this) {
            case XN:
                break;
            case XP:
                p.x = -x;
                p.z = -z;
                break;
            case YN:
                p.x = y;
                p.y = x;
                p.z = -z;
                break;
            case YP:
                p.x = y;
                p.y = -x;
                p.z = z;
                break;
            case ZN:
                p.x = -z;
                p.z = x;
                break;
            case ZP:
                p.x = z;
                p.z = -x;
                break;
            default:
                break;
        }
    }
// 26.x port: glTranslate() removed (client rendering; redone in P3)

    public static Direction fromEnumFacing(net.minecraft.core.Direction direction) {
        if (direction == null) return YN;
        switch (direction) {
            case DOWN:
                return YN;
            case EAST:
                return XP;
            case NORTH:
                return ZN;
            case SOUTH:
                return ZP;
            case UP:
                return YP;
            case WEST:
                return XN;
            default:
                return YN;
        }
    }

    /** Was toForge(); ForgeDirection.UNKNOWN -> null. */
    public net.minecraft.core.Direction toEnumFacing() {
        switch (this) {
            case YN:
                return net.minecraft.core.Direction.DOWN;
            case XP:
                return net.minecraft.core.Direction.EAST;
            case ZN:
                return net.minecraft.core.Direction.NORTH;
            case ZP:
                return net.minecraft.core.Direction.SOUTH;
            case YP:
                return net.minecraft.core.Direction.UP;
            case XN:
                return net.minecraft.core.Direction.WEST;
            default:
                return null;
        }
    }
// 26.x port: glRotateZnRefInv() removed (client rendering; redone in P3)
}
