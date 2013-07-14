package mods.eln.heatfurnace;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import mods.eln.client.FrameTime;
import mods.eln.electricalfurnace.ElectricalFurnaceElement;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.node.Node;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;


public class HeatFurnaceRender extends TransparentNodeElementRender {

	double temperature;
	float gainSyncValue = -1234, temperatureTargetSyncValue = -1234;
	boolean gainSyncNew = false,temperatureTargetSyncNew = false;
	short power;
	
	public boolean controleExternal,takeFuel;
	
	HeatFurnaceDescriptor descriptor;
	public HeatFurnaceRender(TransparentNodeEntity tileEntity,TransparentNodeDescriptor descriptor) {
		super(tileEntity,descriptor);
		this.descriptor = (HeatFurnaceDescriptor) descriptor;
		interpolator = new PhysicalInterpolator(0.4f,8.0f,0.9f,0.2f);
		coord = new Coordonate(tileEntity);
	}
	Coordonate coord;
	PhysicalInterpolator interpolator;
	@Override
	public void draw() {	
		
		List list = tileEntity.worldObj.getEntitiesWithinAABB(EntityPlayer.class, coord.getAxisAlignedBB(1));
		if(list.size() == 0)
			interpolator.setTarget(0f);
		else
			interpolator.setTarget(1f);
		interpolator.stepGraphic();
		
		front.glRotateXnRef();
		descriptor.draw(interpolator.get());
		
		if(entityItemIn != null)
			drawEntityItem(entityItemIn, -0.1, -0.30, 0, counter,0.8f);
		
		counter += FrameTime.get() * 60;
		if(counter >= 360f) counter -= 360;
		
		/*if(Math.random() < 1 * FrameTime.get() * descriptor.flamePopRate * power/descriptor.nominalPower)
		{
			double [] p = new double[3];
			
			p[0] = Math.random() * descriptor.flameDeltaX + descriptor.flameStartX;
			p[1] = Math.random() * descriptor.flameDeltaY + descriptor.flameStartY;
			p[2] = Math.random() * descriptor.flameDeltaZ + descriptor.flameStartZ;
			front.rotateFromXN(p);
			p[0] += tileEntity.xCoord + 0.5;p[1] += tileEntity.yCoord+0.5;p[2] += tileEntity.zCoord+0.5;
			tileEntity.worldObj.spawnParticle("flame",p[0],p[1],p[2], 0.0D, 0.0D, 0.0D);
		}*/
	}
	float counter = 0;
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);

	boolean boot = true;
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new HeatFurnaceGuiDraw(player, inventory, this);
	}
	

	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			controleExternal = stream.readBoolean();
			takeFuel = stream.readBoolean();
			
			
			temperature = stream.readShort() /Node.networkSerializeTFactor;
			float readF;
			readF = stream.readFloat();
			if(gainSyncValue != readF || controleExternal)
			{
				gainSyncValue = readF;
				gainSyncNew = true;
			}
			readF = stream.readFloat();
			if(temperatureTargetSyncValue!= readF || controleExternal)
			{
				temperatureTargetSyncValue = readF;
				temperatureTargetSyncNew = true;
			}
			
		
			power = stream.readShort();
			
			
			entityItemIn = unserializeItemStackToEntityItem(stream,entityItemIn);
	
			
			if(boot)
			{
				coord.move(front);
				//coord.move(front);
				boot = false;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	
	public void clientToogleControl()
	{
		clientSendId(HeatFurnaceElement.unserializeToogleControlExternalId);
	}	
	public void clientToogleTakeFuel()
	{
		clientSendId(HeatFurnaceElement.unserializeToogleTakeFuelId);
	}
	EntityItem entityItemIn;
	public void clientSetGain(float value)
	{
        try {
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        DataOutputStream stream = new DataOutputStream(bos);   	
	
	        preparePacketForServer(stream);
			
			stream.writeByte(HeatFurnaceElement.unserializeGain);
			stream.writeFloat(value);
			
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
			
			stream.writeByte(HeatFurnaceElement.unserializeTemperatureTarget);
			stream.writeFloat(value);
			
			sendPacketToServer(bos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}        
        		
	}
	
	
	@Override
	public boolean cameraDrawOptimisation() {
		// TODO Auto-generated method stub
		return false;
	}
}
