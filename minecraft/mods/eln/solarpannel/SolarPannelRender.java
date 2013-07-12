package mods.eln.solarpannel;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.client.ClientProxy;
import mods.eln.electricalfurnace.ElectricalFurnaceElement;
import mods.eln.misc.Direction;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.node.TransparentNodeRender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class SolarPannelRender extends TransparentNodeElementRender{

	public SolarPannelDescriptor descriptor;
	public SolarPannelRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (SolarPannelDescriptor) descriptor;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void draw() {
		float alpha;
		if(hasTracker == false)
		{
			alpha = (float) descriptor.alphaTrunk(pannelAlphaSyncValue);
		}
		else
		{
			alpha = (float) descriptor.alphaTrunk(SolarPannelSlowProcess.getSolarAlpha(tileEntity.worldObj));
		}
		front.getInverse().glRotateXnRefInv();
		
		GL11.glTranslatef(0, 2, 0);
		
		GL11.glRotatef((float) (alpha*180/Math.PI - 90), 0, 0, -1);
		Eln.obj.draw("SOLARPANEL_2X2","Cube");	
	}

	
	

	
	public boolean pannelAlphaSyncNew = false;
	public float pannelAlphaSyncValue = -1234;
	
	public boolean hasTracker;
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		
		short read;
		
		try {
			
			Byte b;
				
			
			hasTracker = stream.readBoolean();
			
			float pannelAlphaIncoming = stream.readFloat();
			
			if(pannelAlphaIncoming != pannelAlphaSyncValue)
			{
				pannelAlphaSyncValue = pannelAlphaIncoming;
				pannelAlphaSyncNew = true;
			}
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void clientSetPannelAlpha(float value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(SolarPannelElement.unserializePannelAlpha);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1 , 64, this);

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new SolarPannelGuiDraw(player, inventory, this);
	}
	
	
	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
	}

}
