package mods.eln.electricalfurnace;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ElectricalFurnaceRender extends TransparentNodeElementRender{
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(5, 64, this);

	
	public float temperature = 0;
	public boolean powerOn,heatingCorpOn;
//	float temperatureTarget;
	EntityItem entityItemIn = null;
	
	long time;
	
	public ElectricalFurnaceRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		time = System.currentTimeMillis();
		// TODO Auto-generated constructor stub
	}

	float processState,processStatePerSecond;
	
	@Override
	public void draw() {
		front.glRotateXnRef();
		
		processState += processStatePerSecond * FrameTime.getNotCaped();
		if(processState > 1f) processState = 1f;
		
		

		Eln.obj.draw("ElectricFurnace", "furnace");
	//	ClientProxy.obj.draw("ELFURNACE");	
		
		drawEntityItem(entityItemIn, -0.1, -0.20, 0, counter,0.8f);
		counter += (System.currentTimeMillis()-time) * 0.001 *360 / 4;
		if(counter > 360) counter -= 360;
		
		time = System.currentTimeMillis();
	}
	
	float counter = 0;



	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalFurnaceGuiDraw(player, inventory, this);
	}

	
	
	short heatingCorpResistorP = 0;
	
	public boolean temperatureTargetSyncNew = false;
	public float temperatureTargetSyncValue = -1234;

	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
		short read;
		
		try {
			
			Byte b;
			
			b = stream.readByte();
			
			powerOn = (b & 1) != 0;
			heatingCorpOn = (b & 2) != 0;
			
			
			
			float temperatureTargetIncoming = stream.readShort();
			
			if(temperatureTargetIncoming != temperatureTargetSyncValue)
			{
				temperatureTargetSyncValue = temperatureTargetIncoming;
				temperatureTargetSyncNew = true;
			}
			
			temperature = stream.readShort();
			
			
			
			if((read = stream.readShort()) == -1)
			{
				entityItemIn = null;
				stream.readShort();
			}
			else
			{
				entityItemIn = new EntityItem(tileEntity.worldObj,tileEntity.xCoord + 0.5, tileEntity.yCoord + 0.5, tileEntity.zCoord + 1.2, new ItemStack(read, 1, stream.readShort()));
			}
			
			
			heatingCorpResistorP = stream.readShort();
			
			processState = stream.readFloat();
			processStatePerSecond = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public void clientSetPowerOn(boolean value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalFurnaceElement.unserializePowerOnId);
			stream.writeByte(value ? 1 : 0);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
	}
	
	
	public void clientSetTemperatureTarget(float value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(ElectricalFurnaceElement.unserializeTemperatureTarget);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	
	public boolean getPowerOn()
	{
		return powerOn;
	}
}