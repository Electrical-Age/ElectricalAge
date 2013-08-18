package mods.eln.batterycharger;

import ibxm.Channel;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import mods.eln.Eln;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.lampsupply.LampSupplyDescriptor;
import mods.eln.lampsupply.LampSupplyGui;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.PhysicalInterpolator;
import mods.eln.misc.Utils;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;

public class BatteryChargerRender extends SixNodeElementRender{


	
	BatteryChargerDescriptor descriptor;
	public BatteryChargerRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (BatteryChargerDescriptor) descriptor;

		coord = new Coordonate(tileEntity);
	}
	Coordonate coord;
	boolean [] charged = new boolean[]{false,false,false,false};
	boolean [] batteryPresence = new boolean[]{false,false,false,false};
	@Override
	public void draw() {	
		super.draw();
		
		alpha += 90*FrameTime.get();
		if(alpha > 360) alpha -=360;


		drawEntityItem(b[0], 0.1875,0.15625, 0.15625,alpha, 0.2f);
		drawEntityItem(b[1], 0.1875,0.15625, -0.15625,alpha, 0.2f);
		drawEntityItem(b[2], 0.1875,-0.15625, 0.15625,alpha, 0.2f);
		drawEntityItem(b[3], 0.1875,-0.15625, -0.15625,alpha, 0.2f);
		
		//LRDU.Down.glRotateOnX();
		descriptor.draw(batteryPresence,charged);
		
		
	}
	
	float alpha = 0;
	
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
			GL11.glRotatef(90, 0f, 1f, 0f);
			GL11.glRotatef(roty, 0, 1, 0);
			GL11.glScalef(scale, scale, scale);
			GL11.glTranslatef(0.0f,-0.25f,0.0f);
			var10.doRender(entityItem,0, 0, 0, 0, 0);	
		GL11.glPopMatrix();	
		

	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.cable.render;
	}
	
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new BatteryChargerGui(this,player,inventory);
	}


	EntityItem[] b = new EntityItem[4];
	boolean powerOn;
	private float voltage;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			powerOn = stream.readBoolean();
			voltage = stream.readFloat();
			
			for(int idx = 0;idx < 4;idx++){
				b[idx] = Utils.unserializeItemStackToEntityItem(stream, b[idx], tileEntity);
			}
			
			byte temp = stream.readByte();
			for(int idx = 0;idx < 4;idx++){
				charged[idx] = (temp & 1) != 0;
				temp = (byte) (temp >> 1);
			}
			temp = stream.readByte();
			for(int idx = 0;idx < 4;idx++){
				batteryPresence[idx] = (temp & 1) != 0;
				temp = (byte) (temp >> 1);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}





	SixNodeElementInventory inventory = new SixNodeElementInventory(5, 64, this);
}
