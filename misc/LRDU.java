package mods.eln.misc;

import org.lwjgl.opengl.GL11;

import com.google.common.base.CaseFormat;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;


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
	
	LRDU(int dir) {
		// TODO Auto-generated constructor stub
		this.dir = dir;
	}
	
	public static LRDU fromInt(int value)
	{
		switch (value) {
		case 0: return Left;
		case 1: return Right;
		case 2: return Down;
		case 3: return Up;
		}
		return Left;
	}
	public int toInt()
	{
		return dir;
	}
	
	//Don't change !
	public LRDU inverse()
	{
		switch (this) {
		case Down: return Up;
		case Left:return Right;
		case Right:return Left;
		case Up:return Down;
		}
		return null;
	}
	
	/*
	public LRDU equivalentIfReverseDirection(Direction direction)
	{
		switch(direction)
		{
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
	
	public LRDU inverseIfLR()
	{
		switch (this) {
		case Down: return Down;
		case Left:return Right;
		case Right:return Left;
		case Up:return Up;
		}
		return null;
	}
	
	public void applyTo(double vector[],double value)
	{
		switch (this) {
		case Down: 	vector[1]-=value;
			break;
		case Left: 	vector[0]-=value;
			break;
		case Right: vector[0]+=value;
			break;
		case Up:   	vector[1]+=value;
			break;
		default:
			break;
			
		}
	}
	
	public LRDU getNextClockwise()
	{
		switch (this) {
		case Down: return Left;
		case Left:return Up;
		case Right:return Down;
		case Up:return Right;
		}
		return Left;
	}
	
	public void glRotateOnX()
	{
		switch (this) {
			case Left: break;
			case Up: GL11.glRotatef(90f, 1f, 0f, 0f); break;
			case Right:GL11.glRotatef(180f, 1f, 0f, 0f);  break;
			case Down: GL11.glRotatef(270f, 1f, 0f, 0f); break;
		}
	}
	
	public LRDU left()
	{
		switch (this) {
		case Down: return Right;
		case Left:return Down;
		case Right:return Up;
		case Up:return Left;
		}
		return Left;		
	}
	public LRDU right()
	{
		switch (this) {
		case Down: return Left;
		case Left:return Up;
		case Right:return Down;
		case Up:return Right;
		}
		return Left;
	}
	
	public void writeToNBT(NBTTagCompound nbt,String name) {
		nbt.setByte(name, (byte) toInt());
	}
	static public LRDU readFromNBT(NBTTagCompound nbt,String name) {
		return LRDU.fromInt(nbt.getByte(name));
	}
	
	public int dir;
}

