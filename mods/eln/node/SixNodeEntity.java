package mods.eln.node;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class SixNodeEntity extends NodeBlockEntity {
	//boolean[] syncronizedSideEnable = new boolean[6];
	public SixNodeElementRender[] elementRenderList = new SixNodeElementRender[6];
	short[] elementRenderIdList = new short[6];

	public Block sixNodeCacheBlock = Blocks.air;
	public byte sixNodeCacheBlockMeta = 0;

	public SixNodeEntity()
	{
		for (int idx = 0; idx < 6; idx++)
		{
			elementRenderList[idx] = null;
			elementRenderIdList[idx] = 0;
		}
	}

	/* caca
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction direction) {
		// TODO Auto-generated method stub
		//Utils.println("onBlockActivated " + direction);
		
		return getNode().onBlockActivated(entityPlayer, direction);
	}
	*/

	public static final int singleTargetId = 2;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		Block sixNodeCacheBlockOld = sixNodeCacheBlock;

		super.networkUnserialize(stream);

		try {

			sixNodeCacheBlock = Block.getBlockById(stream.readInt());
			sixNodeCacheBlockMeta = stream.readByte();

			int idx = 0;
			for (idx = 0; idx < 6; idx++){
				short id = stream.readShort();
				if (id == 0){
					elementRenderIdList[idx] = (short) 0;
					elementRenderList[idx] = null;
				}else{
					if (id != elementRenderIdList[idx]){
						elementRenderIdList[idx] = id;
						SixNodeDescriptor descriptor = Eln.sixNodeItem.getDescriptor(id);
						elementRenderList[idx] = (SixNodeElementRender) descriptor.RenderClass.getConstructor(SixNodeEntity.class, Direction.class, SixNodeDescriptor.class).newInstance(this, Direction.fromInt(idx), descriptor);
					}
					elementRenderList[idx].publishUnserialize(stream);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//	worldObj.setLightValue(EnumSkyBlock.Sky, xCoord,yCoord,zCoord,15);
		if (sixNodeCacheBlock != sixNodeCacheBlockOld)	{
			Chunk chunk = worldObj.getChunkFromBlockCoords(xCoord, zCoord);
			chunk.generateHeightMap();
			Utils.updateSkylight(chunk);
			chunk.generateSkylightMap();
			Utils.updateAllLightTypes(worldObj, xCoord, yCoord, zCoord);
		}

	}

	@Override
	public void serverPacketUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.serverPacketUnserialize(stream);

		try {
			int side = stream.readByte();
			int id = stream.readShort();
			if (elementRenderIdList[side] == id)
			{
				elementRenderList[side].serverPacketUnserialize(stream);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean getSyncronizedSideEnable(Direction direction) {
		return elementRenderList[direction.getInt()] != null;
	}

	public Container newContainer(Direction side, EntityPlayer player)
	{
		return ((SixNode) getNode()).newContainer(side, player);
	}

	public GuiScreen newGuiDraw(Direction side, EntityPlayer player)
	{
		return elementRenderList[side.getInt()].newGuiDraw(side, player);
	}

	public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu)
	{
		side = side.applyLRDU(lrdu);
		if (elementRenderList[side.getInt()] == null)
			return null;

		return elementRenderList[side.getInt()].getCableRender(lrdu);
	}

	public int getCableDry(Direction side, LRDU lrdu)
	{
		side = side.applyLRDU(lrdu);
		if (elementRenderList[side.getInt()] == null)
			return 0;

		return elementRenderList[side.getInt()].getCableDry(lrdu);
	}

	@Override
	public boolean cameraDrawOptimisation()
	{
		for (SixNodeElementRender e : elementRenderList)
		{
			if (e != null && !e.cameraDrawOptimisation())
				return false;
		}
		return true;
	}

	@Override
	public void destructor() {
		for (SixNodeElementRender render : elementRenderList)
		{
			if (render != null)
				render.destructor();
		}
		super.destructor();
	}

	/*public float getBlockHardness(World world, int x, int y, int z) {
		// TODO Auto-generated method stub
		return 0;
	}*/
	public int getDamageValue(World world, int x, int y, int z) {
		if (world.isRemote)
		{
			for (int idx = 0; idx < 6; idx++)
			{
				if (elementRenderList[idx] != null)
				{
					return elementRenderIdList[idx];
				}
			}
		}
		return 0;
	}

	public boolean hasVolume(World world, int x, int y, int z) {
		// TODO Auto-generated method stub
		if (worldObj.isRemote)
		{
			for (SixNodeElementRender e : elementRenderList)
			{
				if (e != null && e.sixNodeDescriptor.hasVolume())
					return true;
			}
			return false;
		}
		else
		{
			SixNode node = ((SixNode) getNode());
			if (node == null)
				return false;
			return node.hasVolume();
		}
	}

	@Override
	public void tileEntityNeighborSpawn() {
		// TODO Auto-generated method stub
		for (SixNodeElementRender e : elementRenderList) {
			if (e != null)
				e.notifyNeighborSpawn();
		}
	}

	@Override
	public INodeInfo getInfo() {
		// TODO Auto-generated method stub
		return Eln.sixNodeBlock;
	}
	
	@Override
	public void clientRefresh(float deltaT) {
		for(SixNodeElementRender e : elementRenderList){
			if(e != null){
				e.refresh(deltaT);
			}			
		}

	}
}
// && 