package mods.eln.node;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;


import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public abstract class TransparentNodeElementRender {
	public TransparentNodeEntity tileEntity;
	public Direction front;
	public boolean grounded;
	
	public TransparentNodeElementRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor)
	{
		this.tileEntity = tileEntity;
	}
	protected   EntityItem unserializeItemStackToEntityItem(DataInputStream stream,EntityItem old) throws IOException
	{
		return Utils.unserializeItemStackToEntityItem(stream,old,tileEntity);
		
	}
	public void drawEntityItem(EntityItem entityItem,double x, double y , double z,float roty,float scale)
	{
		if(entityItem == null) return;
		
		entityItem.hoverStart = 0.0f;
		entityItem.rotationYaw = 0.0f;
		entityItem.motionX = 0.0;
		entityItem.motionY = 0.0;
		entityItem.motionZ =0.0;
		
		Render var10 = null;
		var10 = RenderManager.instance.getEntityRenderObject(entityItem);
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x, (float)y, (float)z);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();		
	}
	
	
	public abstract void draw() ;

	public void networkUnserialize(DataInputStream stream)
	{
		try {
			byte b = stream.readByte();
			front = Direction.fromInt(b & 0x7);		
			grounded = (b & 8) != 0;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public GuiScreen newGuiDraw(Direction side,EntityPlayer player)
	{
		return null;
	}
	public IInventory getInventory()
	{
		return null;
	}
    public void preparePacketForServer(DataOutputStream stream)
    {
    	tileEntity.preparePacketForServer(stream);    	
    }
	
    public void sendPacketToServer(ByteArrayOutputStream bos)
    {
    	tileEntity.sendPacketToServer(bos);
    }
    
    
	public void clientSetGrounded(boolean value)
	{
		clientSendBoolean(TransparentNodeElement.unserializeGroundedId,value);      
	}
    
	public void clientSendBoolean(Byte id,boolean value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(id);
			stream.writeByte(value ? 1 : 0);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
	}	
	public void clientSendId(Byte id)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(id);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
	}		
    public boolean cameraDrawOptimisation()
    {
    	return true;
    }
}
