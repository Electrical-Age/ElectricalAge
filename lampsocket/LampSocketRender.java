package mods.eln.lampsocket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.client.ClientProxy;
import mods.eln.item.LampDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.Node;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet250CustomPayload;


public class LampSocketRender extends SixNodeElementRender{
	
	
	LampSocketDescriptor lampSocketDescriptor = null;
	LampSocketDescriptor descriptor;
	public LampSocketRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (LampSocketDescriptor) descriptor;
		lampSocketDescriptor = (LampSocketDescriptor) descriptor;
		// TODO Auto-generated constructor stub
	}

	SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);;
	boolean grounded = true;
	
	




	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new LampSocketGuiDraw(player, inventory,this);
	}

	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}
	
	
	@Override
	public void draw() {
		descriptor.draw(front,alphaZ);
	}

	LampDescriptor lampDescriptor = null;
	float alphaZ;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			grounded = (b & (1<<6)) != 0;
			
			lampDescriptor = (LampDescriptor) Eln.sharedItem.getDescriptor(stream.readShort());
			alphaZ = stream.readFloat();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	public boolean getGrounded()
	{
		return grounded;
	}
	public void setGrounded(boolean grounded) {
		this.grounded = grounded;
	}
	
	public void clientSetGrounded(boolean value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(LampSocketElement.setGroundedId);
			stream.writeByte(value ? 1 : 0);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        
	}
}
