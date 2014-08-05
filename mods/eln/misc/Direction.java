package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;


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
	
	
	static Direction[] intToDir = {XN,XP,YN,YP,ZN,ZP};
	
	
	Direction(int dir) {
		
		this.dir = dir;
	}
	
	public int getInt()
	{
		return dir;
	}
	public boolean isNotY(){
		return this != YP && this != YN;
	}
	public boolean isY(){
		return this == YP || this == YN;
	}
	public void applyTo(double[] vector,double distance)
	{
		if(dir == 0)vector[0]-=distance;
		if(dir == 1)vector[0]+=distance;
		if(dir == 2)vector[1]-=distance;
		if(dir == 3)vector[1]+=distance;
		if(dir == 4)vector[2]-=distance;
		if(dir == 5)vector[2]+=distance;
	}
	public void applyTo(int[] vector,int distance)
	{
		if(dir == 0)vector[0]-=distance;
		if(dir == 1)vector[0]+=distance;
		if(dir == 2)vector[1]-=distance;
		if(dir == 3)vector[1]+=distance;
		if(dir == 4)vector[2]-=distance;
		if(dir == 5)vector[2]+=distance;
	}
	
	public int getHorizontalIndex()
	{
		switch(this)
		{
		case XN: return 0;
		case XP: return 1;
		case YN: return 0;
		case YP: return 0;
		case ZN: return 2;
		case ZP: return 3;
		default: return 0;
		
		}
	}
	public static Direction fromHorizontalIndex(int nbr)
	{
		switch(nbr)
		{
		case 0: return XN;
		case 1: return XP;
		case 2: return ZN;
		case 3: return ZP;
		default: return XN;
		
		}
	}
	
	/*public CoordinateTuple ApplyToCoordinates(CoordinateTuple coordinates) {
		CoordinateTuple ret = new CoordinateTuple(coordinates);
		
		ret.coords[dir/2] += GetSign();
		
		return ret;
	}*/
	
	/**
	 * Get the tile entity next to a tile entity following this direction.
	 *
	 * @param tileEntity tile entity to check
	 * @return Adjacent tile entity or null if none exists
	 */
	public TileEntity applyToTileEntity(TileEntity tileEntity) {
		if(tileEntity == null)return null;
		int coords[] = { tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord };
		
		coords[dir/2] += getSign();
		
		if (tileEntity.getWorldObj() != null && tileEntity.getWorldObj().blockExists(coords[0], coords[1], coords[2])) {
			return tileEntity.getWorldObj().getTileEntity(coords[0], coords[1], coords[2]);
		} else {
			return null;
		}
	}
	
	
	public TileEntity applyToTileEntityAndSameClassThan(TileEntity tileEntity,Class c)
	{
		if(tileEntity == null) return null;
		TileEntity findedEntity = applyToTileEntity(tileEntity);
		if(findedEntity == null) return null;
		if( ! Utils.isTheClass(findedEntity, c)) return null;
		return findedEntity;
	}
	
	/**
	 * Get the inverse of this direction (XN -> XP, XP -> XN, etc.)
	 * 
	 * @return Inverse direction
	 */
	public Direction getInverse() {
		int inverseDir = dir - getSign();
		
		for (Direction direction: Direction.values()) {
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
	
	
	
	public void renderBlockFace(int x,int y,float spritDim)
	{
		switch(this)
		{

		case XN:
			GL11.glNormal3f(-1.0f, 0.0f, 0.0f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,0.5F,0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,0.5F,-0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,-0.5F,-0.5f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,-0.5F,0.5f);

			break;
			
		case XP:
			GL11.glNormal3f(1.0f, 0.0f, 0.0f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,0.5F,-0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,0.5F,0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,-0.5F,0.5f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,-0.5F,-0.5f);
			
			break;
		case YN:
			GL11.glNormal3f(0.0f, -1.0f, 0.0f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,-0.5f,-0.5F);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,-0.5f,0.5F);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,-0.5f,0.5F);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,-0.5f,-0.5F);
			break;
		case YP: //YP
			GL11.glNormal3f(0.0f, 1.0f, 0.0f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,0.5f,-0.5F);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,0.5f,0.5F);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,0.5f,0.5F);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,0.5f,-0.5F);
			break;
		case ZN:
			GL11.glNormal3f(0.0f, 0.0f, -1.0f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,0.5F,-0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,0.5F,-0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,-0.5F,-0.5f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,-0.5F,-0.5f);
			break;
		case ZP:
			GL11.glNormal3f(0.0f, 0.0f, 1.0f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+0)*spritDim); GL11.glVertex3f(0.5F,0.5F,0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+0)*spritDim); GL11.glVertex3f(-0.5F,0.5F,0.5f);
			GL11.glTexCoord2f((x+0)*spritDim,(y+1)*spritDim); GL11.glVertex3f(-0.5F,-0.5F,0.5f);
			GL11.glTexCoord2f((x+1)*spritDim,(y+1)*spritDim); GL11.glVertex3f(0.5F,-0.5F,0.5f);
			break;
		default:
			break;
		}
	

	}
	
	int dir;

	static public Direction fromInt(int idx) {	
		for (Direction direction: Direction.values()) {
			if (direction.dir == idx) return direction;
		}
		return null;
	}	
	static public Direction fromIntMinecraftSide(int idx) {	
		idx = (idx +2) % 6;
		for (Direction direction: Direction.values()) {
			if (direction.dir == idx) return direction;
		}
		return null;
	}
	
	public Direction right()
	{
		switch(this)
		{
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
	
	public Direction left()
	{
		return right().getInverse();
	}
	public Direction up()
	{
		switch(this)
		{
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
	public Direction down()
	{
		return up().getInverse();
	}
	public Direction back()
	{
		return getInverse();
	}
	
	
	
	public Direction applyLRDU(LRDU lrdu)
	{
		switch(lrdu)
		{
		case Down:	return this.down();
		case Left: 	return this.left();
		case Right: return this.right();
		case Up: 	return this.up();
		default:
			break;
		}
		return null;
	}
	
	public LRDU getLRDUGoingTo(Direction target)
	{
		for(LRDU lrdu : LRDU.values())
		{
			if(target == applyLRDU(lrdu)) return lrdu;
		}
		return null;
	}
		
	
	
	public void glRotateXnRef()
	{
		
		//toCheck
		switch(this)
		{
		case XN:
			break;
		case XP:
			//GL11.glScalef(-1f, 1, -1f);
			GL11.glRotatef(180, 0f, 1f, 0f);
			break;
		case YN:
			GL11.glRotatef(90f, 0f, 0f, 1f);
			GL11.glScalef(1f, -1f, -1f);
			break;
		case YP:
			GL11.glRotatef(90f, 0f, 0f, -1f);
			break;
		case ZN:
			GL11.glRotatef(270f, 0f, 1f, 0f);
			break;
		case ZP:
			GL11.glRotatef(90f, 0f, 1f, 0f);
			break;
		default:
			break;

		}		
	}
	public void glRotateXnRefInv()
	{
		
		//toCheck
		switch(this)
		{
		case XN:
			break;
		case XP:
			//GL11.glScalef(-1f, 1, -1f);
			GL11.glRotatef(180, 0f, -1f, 0f);
			break;
		case YN:
			GL11.glScalef(1f, -1f, -1f);
			GL11.glRotatef(90f, 0f, 0f, -1f);
			
			break;
		case YP:
			GL11.glRotatef(90f, 0f, 0f, 1f);
			break;
		case ZN:
			GL11.glRotatef(270f, 0f, -1f, 0f);
			break;
		case ZP:
			GL11.glRotatef(90f, 0f, -1f, 0f);
			break;
		default:
			break;

		}		
	}
	
	public void glRotateZnRef()
	{
		
		//toCheck
		switch(this)
		{
		case XN:
			GL11.glRotatef( 90f, 0f, 1f, 0f);
			break;
		case XP:
			GL11.glRotatef( 90f, 0f, -1f, 0f);
			break;
		case YN:
			GL11.glRotatef(90f, 1f, 0f, 0f);
			GL11.glScalef(1f, -1, 1f);
			break;
		case YP:
			GL11.glRotatef(90f, 1f, 0f, 0f);
			GL11.glScalef(1f, 1f, 1f);
			break;
		case ZN:
			//GL11.glRotatef(90f, 0f, -1f, 0f);
			break;
		case ZP:
			GL11.glRotatef(180f, 0f, 1f, 0f);
			break;
		default:
			break;

		}		
	}	

	


	public TileEntity getTileEntity(Coordonate coordonate)
	{
		int x = coordonate.x,y = coordonate.y,z = coordonate.z;
		switch(this)
		{
		case XN:x--;
			break;
		case XP:x++;
			break;
		case YN:y--;
			break;
		case YP:y++;
			break;
		case ZN:z--;
			break;
		case ZP:z++;
			break;
		default:
			break;
		
		}
		
		return coordonate.world().getTileEntity(x, y, z);
	}

	public void writeToNBT(NBTTagCompound nbt,String name) {
		nbt.setByte(name, (byte) getInt());
	}
	static public Direction readFromNBT(NBTTagCompound nbt,String name) {
		return Direction.fromInt(nbt.getByte(name));
	}

	public void rotateFromXN(double[] p) {
		
		double x = p[0],y = p[1],z = p[2];
		switch(this)
		{
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
		
		int x = p[0],y = p[1],z = p[2];
		switch(this)
		{
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
	public void rotateFromXN(Vec3 p) {
		
		double x = p.xCoord,y = p.yCoord,z = p.zCoord;
		switch(this)
		{
		case XN:
			break;
		case XP:
			p.xCoord = -x;
			p.zCoord = -z;
			break;
		case YN:
			p.xCoord = y;
			p.yCoord = x;
			p.zCoord = -z;
			break;
		case YP:
			p.xCoord = y;
			p.yCoord = -x;
			p.zCoord = z;
			break;
		case ZN:
			p.xCoord = -z;
			p.zCoord = x;
			break;
		case ZP:
			p.xCoord = z;
			p.zCoord = -x;
			break;
		default:
			break;
		
		}		
	}
	public void rotateFromXN(Coordonate p) {
		int x = p.x,y = p.y,z = p.z;
		switch(this)
		{
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
	public void glTranslate(float v) {
		
		switch (this) {
		case XN:GL11.glTranslatef(-v,0f,0f);
			break;
		case XP:GL11.glTranslatef(v,0f,0f);
			break;
		case YN:GL11.glTranslatef(0f,-v,0f);
			break;
		case YP:GL11.glTranslatef(0f,v,0f);
			break;
		case ZN:GL11.glTranslatef(0f,0f,-v);
			break;
		case ZP:GL11.glTranslatef(0f,0f,v);
			break;
		default:
			break;


		}
	}

	public static Direction from(ForgeDirection direction) {
		switch(direction){
		case DOWN: return YN;
		case EAST:return XP;
		case NORTH:return ZN;
		case SOUTH:return ZP;
		case UP:return YP;
		case WEST:return XN;
		default:return YN;
		
		}
	}

	public  ForgeDirection toForge() {
		switch(this){
		case YN: return ForgeDirection.DOWN;
		case XP:return ForgeDirection.EAST;
		case ZN:return ForgeDirection.NORTH;
		case ZP:return ForgeDirection.SOUTH;
		case YP:return ForgeDirection.UP;
		case XN:return ForgeDirection.WEST;
		default:return ForgeDirection.UNKNOWN;
		
		}
	}


	
}

