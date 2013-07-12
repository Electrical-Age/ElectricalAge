package mods.eln.client;


import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalConnection;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalConnection;
import mods.eln.sim.ThermalLoad;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class FrameTime implements ITickHandler{
	static FrameTime instance;
	public FrameTime()
	{
		instance = this;
		TickRegistry.registerTickHandler(this, Side.CLIENT);
		
	}
	public void init()
	{
	//	NodeBlockEntity.nodeAddedList.clear();
	}
	public void stop()
	{
	//	NodeBlockEntity.nodeAddedList.clear();
	}
	
	public static float get()
	{
		if(Utils.isGameInPause())
			return 0f;
		return instance.deltaT;
	}
	public static float getNotCaped()
	{
		float value = get();
		if(value > 0.1f) return 0.1f;
		return value;
	}
	float deltaT = 0.02f;
	long oldNanoTime = 0;
	boolean boot = true;
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		long nanoTime = System.nanoTime();
		if(boot){
			boot = false;
		}
		else
		{
			deltaT = (nanoTime-oldNanoTime)*0.000000001f;
		//	System.out.println(deltaT);
		}
		oldNanoTime = nanoTime;
	//	System.out.println("delta T : " + deltaT);
   


	}
	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
		

	}
	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.RENDER);
		
	}
	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "Miaou2";
	}

	
}
