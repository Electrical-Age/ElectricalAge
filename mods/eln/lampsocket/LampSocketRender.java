package mods.eln.lampsocket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.Tickable;

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
	public boolean poweredByLampSupply;
	




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
		super.draw();
		descriptor.draw(front,alphaZ);
	}
	public String channel;
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
			cable = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream),ElectricalCableDescriptor.class);
			
			poweredByLampSupply = stream.readBoolean();
			channel = stream.readUTF();
			
			isConnectedToLampSupply = stream.readBoolean();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	public boolean isConnectedToLampSupply;
	
	ElectricalCableDescriptor cable;
	
	public boolean getGrounded()
	{
		return grounded;
	}
	public void setGrounded(boolean grounded) {
		this.grounded = grounded;
	}
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if(cable == null) return null;
		return cable.render;
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
