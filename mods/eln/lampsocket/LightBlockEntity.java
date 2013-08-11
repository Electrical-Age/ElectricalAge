package mods.eln.lampsocket;

import java.util.ArrayList;

import mods.eln.Eln;
import mods.eln.INBTTReady;

import mods.eln.misc.Coordonate;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;

public class LightBlockEntity extends TileEntity{
	/*
	class LightBlockTag implements INBTTReady{
		int uuid;
		int light;
		@Override
		public void readFromNBT(NBTTagCompound nbt, String str) {
			uuid = nbt.getInteger(str + "uuid");
			light = nbt.getByte(str + "light");
		}
		@Override
		public void writeToNBT(NBTTagCompound nbt, String str) {
			// TODO Auto-generated method stub
			nbt.setInteger(str + "uuid", uuid);
			nbt.setByte(str + "light", (byte) light);
		}		
		
		
	}
	*
	*
	*/
	static void addObserver(LightBlockObserver observer)
	{
		observers.add(observer);
	}
	static void removeObserver(LightBlockObserver observer)
	{
		observers.remove(observer);
	}
	ArrayList<Integer> lightList = new ArrayList<Integer>();
	
	public interface LightBlockObserver{
		void lightBlockDestructor(Coordonate coord);
	}
	
	public static ArrayList<LightBlockObserver> observers = new ArrayList<LightBlockObserver>();
	
	
	
	
	void addLight(int light)
	{
		lightList.add(light);
		lightManager();
	}
	void removeLight(int light)
	{
		//int meta = worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
		for(int idx = 0;idx<lightList.size();idx++)
		{
			if(lightList.get(idx) == light)
			{
				lightList.remove(idx);
				lightManager();
				return;
			}
		}
		System.out.println("Assert void removeLight(int light)");
	}
	
	void remplaceLight(int oldLight,int newLight)
	{
		for(int idx = 0;idx<lightList.size();idx++)
		{
			if(lightList.get(idx) == oldLight)
			{
				lightList.set(idx, newLight);
				lightManager();
				return;
			}
		}	
		System.out.println("Assert void remplaceLight(int oldLight,int newLight)");
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		byte[]  array = new byte[lightList.size()];
		for(int idx = 0;idx<lightList.size();idx++)
		{
			int value = lightList.get(idx);
			array[idx] = (byte) value;
		}
		nbt.setByteArray("values", array);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		byte [] array = nbt.getByteArray("values");
		
		for(byte value : array)
		{
			lightList.add((int) value);
		}
	}
	
	
	
	int getLight()
	{
		int light = 0;
		for(Integer value : lightList)
		{
			if(light < value) light = value;
		}
		return light;
	}
	
	void lightManager()
	{
		if(lightList.size() == 0)
		{
			worldObj.setBlock(xCoord, yCoord, zCoord, 0);
		}
		else
		{
			int light = getLight();
			if(light != worldObj.getBlockMetadata(xCoord, yCoord, zCoord)){
				
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, light,2);
				worldObj.updateLightByType(EnumSkyBlock.Block,xCoord, yCoord, zCoord);
			}
		}
	}	
	
	public static void addLight(Coordonate coord,int light)
	{
		int blockId = coord.getBlockId();
		if(blockId != Eln.lightBlockId)
			coord.setBlock(Eln.lightBlockId, light);
		((LightBlockEntity)coord.getTileEntity()).addLight(light);
	}
	public static void removeLight(Coordonate coord,int light)
	{
		int blockId = coord.getBlockId();
		if(blockId != Eln.lightBlockId) return;
		((LightBlockEntity)coord.getTileEntity()).removeLight(light);
		
	}
	
	public static void remplaceLight(Coordonate coord,int oldLight,int newLight)
	{
		int blockId = coord.getBlockId();
		if(blockId != Eln.lightBlockId)
		{
			//coord.setBlock(Eln.lightBlockId, newLight);
			System.out.println("ASSERT public static void remplaceLight(Coordonate coord,int oldLight,int newLight) " + coord);
			return;
		}
		((LightBlockEntity)coord.getTileEntity()).remplaceLight(oldLight,newLight);		
	}
	public int getClientLight() {
		// TODO Auto-generated method stub
		return clientLight;
	}
	
	int clientLight = 0;
}
