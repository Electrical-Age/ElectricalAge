package mods.eln.lampsocket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.translate.EntityArrays;
import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.item.LampDescriptor;
import mods.eln.item.LampDescriptor.Type;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlockEntity;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import mods.eln.sound.SoundCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.EnumSkyBlock;


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
	
	float pertuVy = 0,pertuPy = 0;
	float pertuVz = 0,pertuPz = 0;
	float weatherAlphaZ = 0,weatherAlphaY = 0;
	
	List entityList = new ArrayList();
	float entityTimout = 0;
	@Override
	public void draw() {
		super.draw();
		
		if(descriptor.render instanceof LampSocketSuspendedObjRender){
			float dt = FrameTime.get();
			/*if(Math.random() < 0.2*dt || (pertuVy == 0 && pertuPy == 0)){
				pertuVy += Math.random();
			}
			if(Math.random() < 0.2*dt || (pertuVz == 0 && pertuPz == 0)){
				pertuVz += Math.random();
			}*/
			entityTimout -= dt;
			if(entityTimout < 0){
				entityList = tileEntity.getWorldObj().getEntitiesWithinAABB(Entity.class, new Coordonate(tileEntity.xCoord,tileEntity.yCoord-2,tileEntity.zCoord,tileEntity.getWorldObj()).getAxisAlignedBB(2));
				entityTimout = 0.1f;
			}
			
			for (Object o : entityList ) {
				Entity e = (Entity) o;
				float eFactor = 0;
				if(e instanceof EntityArrow) eFactor = 1f;
				if(e instanceof EntityLivingBase) eFactor = 4f;

				if(eFactor == 0) continue;
				pertuVz += e.motionX * eFactor * dt;
				pertuVy += e.motionZ * eFactor * dt;
			}

			
			if(tileEntity.getWorldObj().getSavedLightValue(EnumSkyBlock.Sky, tileEntity.xCoord,tileEntity.yCoord,tileEntity.zCoord) > 3){
				float weather = (float) Utils.getWeather(tileEntity.getWorldObj())*0.9f+0.1f;
		
				weatherAlphaY += (0.4-Math.random())*dt*Math.PI/0.2*weather;
				weatherAlphaZ += (0.4-Math.random())*dt*Math.PI/0.2*weather;
				if(weatherAlphaY > 2*Math.PI) weatherAlphaY -= 2*Math.PI;
				if(weatherAlphaZ > 2*Math.PI) weatherAlphaZ -= 2*Math.PI;
				pertuVy += Math.random()*Math.sin(weatherAlphaY)*weather*weather*dt*3; 
				pertuVz += Math.random()*Math.cos(weatherAlphaY)*weather*weather*dt*3; 
				
				pertuVy += 0.4 * dt * weather*Math.signum(pertuVy) * Math.random();
				pertuVz += 0.4 * dt * weather*Math.signum(pertuVz) * Math.random();
			}
			
			
			pertuVy -= pertuPy/10*dt;
			pertuVy *= (1-0.2*dt); 
			pertuPy += pertuVy;
			
			pertuVz -= pertuPz/10*dt;
			pertuVz *= (1-0.2*dt); 
			pertuPz += pertuVz;
		}
		
		descriptor.render.draw(this);
		
		

		
		
	}
	public String channel;
	LampDescriptor lampDescriptor = null;
	float alphaZ;
	byte light,oldLight =-1;
	
	void setLight(byte newLight){
		light = newLight;
		if(lampDescriptor != null && lampDescriptor.type == Type.eco &&  oldLight != -1 && oldLight < 9 && light >= 9){
			play(new SoundCommand("eln:neon_lamp").mulVolume(1, 1f).smallRange());
		}
		oldLight = light;
	}
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			grounded = (b & (1<<6)) != 0;
			
			ItemStack lampStack = Utils.unserialiseItemStack(stream);
			lampDescriptor = (LampDescriptor) Utils.getItemObject(lampStack);
			alphaZ = stream.readFloat();
			cable = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(Utils.unserialiseItemStack(stream),ElectricalCableDescriptor.class);
			
			poweredByLampSupply = stream.readBoolean();
			channel = stream.readUTF();
			
			isConnectedToLampSupply = stream.readBoolean();
			
			setLight(stream.readByte());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	@Override
	public void serverPacketUnserialize(DataInputStream stream)
			throws IOException {
		// TODO Auto-generated method stub
		super.serverPacketUnserialize(stream);
		setLight(stream.readByte());
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
	
	
	@Override
	public boolean cameraDrawOptimisation() {
		return descriptor.cameraOpt;
	}
}
