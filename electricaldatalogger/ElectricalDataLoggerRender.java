package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;

import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalDataLoggerRender extends SixNodeElementRender{

	SixNodeElementInventory inventory = new SixNodeElementInventory(2,64,this);
	ElectricalDataLoggerDescriptor descriptor;
	long time;
	public ElectricalDataLoggerRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
		time = System.currentTimeMillis();
		clientSend(ElectricalDataLoggerElement.newClientId);
	}


	LRDU front;
	public byte unitType;

	@Override
	public void draw() {
		
		GL11.glLineWidth(2f);
		GL11.glColor4f(1f, 0f, 0f, 1f);
		GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        	
        	GL11.glScalef(1f, -1f, 1f);
        	GL11.glTranslatef(0.1f,-0.5f,0.5f); 	
        	GL11.glRotatef(90,0f,1f,0f);  
        	
	        log.draw();
        GL11.glPopMatrix();
        GL11.glEnable(GL11.GL_LIGHTING);

	}
	
	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.cableRender;
	}
	*/
	
	public float samplingPeriod = 1;
	public float highValue = 60;
	public boolean pause;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			unitType = stream.readByte();
			pause = stream.readBoolean();
			samplingPeriod = stream.readFloat();
			highValue = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
	
	DataLogs log = new DataLogs(ElectricalDataLoggerElement.logsSizeMax);
	boolean waitFistSync = true;
	
	@Override
	public void serverPacketUnserialize(DataInputStream stream) throws IOException {
		byte header = stream.readByte();
		
		switch(header)
		{
		case ElectricalDataLoggerElement.toClientLogsAdd:
		case ElectricalDataLoggerElement.toClientLogsClear:
			if(header == ElectricalDataLoggerElement.toClientLogsClear)
			{
				log.reset();
				waitFistSync = false;
			}
			int size = stream.available();
			while(size != 0)
			{
				size--;
				log.write(stream.readByte());
			}
		//	System.out.println(log);
			break;
		}

		
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalDataLoggerGui(player,inventory,this);
	}
}
