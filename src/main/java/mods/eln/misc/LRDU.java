package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Represents the 6 possible directions along the axis of a block.
 */
public enum LRDU {
    /**

     */
    Left(0),
    /**

     */
    Right(1),

    /**

     */
    Down(2),
    /**

     */
    Up(3);

    public int dir;

    LRDU(int dir) {
        this.dir = dir;
    }

    public static LRDU fromInt(int value) {
        switch (value) {
            case 0:
                return Left;
            case 1:
                return Right;
            case 2:
                return Down;
            case 3:
                return Up;
        }
        return Left;
    }

    public int toInt() {
        return dir;
    }

    //Don't change !
    public LRDU inverse() {
        switch (this) {
            case Down:
                return Up;
            case Left:
                return Right;
            case Right:
                return Left;
            case Up:
                return Down;
        }
        return null;
    }

	/*
	public LRDU equivalentIfReverseDirection(Direction direction) {
		switch(direction) {
			case XN:
			case XP:
			case ZN:
			case ZP:
				if(this == Down || this == Up) return this;
				return this.inverse();
			case YN:
			case YP:
				if(this == Down || this == Up) return this;
				return this.inverse();
		}
	
		return null;	
	}*/

    public LRDU inverseIfLR() {
        switch (this) {
            case Down:
                return Down;
            case Left:
                return Right;
            case Right:
                return Left;
            case Up:
                return Up;
        }
        return null;
    }

    public void applyTo(double vector[], double value) {
        switch (this) {
            case Down:
                vector[1] -= value;
                break;
            case Left:
                vector[0] -= value;
                break;
            case Right:
                vector[0] += value;
                break;
            case Up:
                vector[1] += value;
                break;
            default:
                break;
        }
    }

    public float[] rotate4PinDistances(float[] distances) {
        if (distances.length != 4) return distances;
        switch (this) {
            case Left:
                return new float[]{distances[3], distances[2], distances[0], distances[1]};

            case Down:
                return new float[]{distances[1], distances[0], distances[3], distances[2]};

            case Right:
                return new float[]{distances[2], distances[3], distances[1], distances[0]};

            case Up:
            default:
                return distances;
        }
    }

    public LRDU getNextClockwise() {
        switch (this) {
            case Down:
                return Left;
            case Left:
                return Up;
            case Right:
                return Down;
            case Up:
                return Right;
        }
        return Left;
    }

    public void glRotateOnX() {
        switch (this) {
            case Left:
                break;
            case Up:
                GL11.glRotatef(90f, 1f, 0f, 0f);
                break;
            case Right:
                GL11.glRotatef(180f, 1f, 0f, 0f);
                break;
            case Down:
                GL11.glRotatef(270f, 1f, 0f, 0f);
                break;
        }
    }

    public void rotateOnXnLeft(double[] v) {
        double y = v[1];
        double z = v[2];
        switch (this) {
            case Left:
                break;
            case Up:
                v[1] = -z;
                v[2] = y;
                break;
            case Right:
                v[1] = -y;
                v[2] = -z;
                break;
            case Down:
                v[1] = z;
                v[2] = -y;
                break;
        }
    }

    public Vec3d rotateOnXnLeft(Vec3d v) {
        double x = v.xCoord;
        double y = v.yCoord;
        double z = v.zCoord;
        switch (this) {
            case Left:
                return v;
            case Up:
                return new Vec3d(x, -z, y);
            case Right:
                return new Vec3d(x, -y, -z);
            case Down:
                return new Vec3d(x, z, -y);
            default:
                return v;
        }
    }

    public LRDU left() {
        switch (this) {
            case Down:
                return Right;
            case Left:
                return Down;
            case Right:
                return Up;
            case Up:
                return Left;
        }
        return Left;
    }

    public LRDU right() {
        switch (this) {
            case Down:
                return Left;
            case Left:
                return Up;
            case Right:
                return Down;
            case Up:
                return Right;
        }
        return Left;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String name) {
        nbt.setByte(name, (byte) toInt());
        return nbt;
    }

    static public LRDU readFromNBT(NBTTagCompound nbt, String name) {
        return LRDU.fromInt(nbt.getByte(name));
    }

    public void serialize(DataOutputStream stream) {
        try {
            stream.writeByte(this.toInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public LRDU deserialize(DataInputStream stream) {
        try {
            return fromInt(stream.readByte());
        } catch (IOException e) {
            e.printStackTrace();
            return Up;
        }
    }
}
