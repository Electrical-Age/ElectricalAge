package mods.eln.electricalswitch;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.Node;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;


public class ElectricalSwitchRender extends SixNodeElementRender{

	ElectricalSwitchDescriptor descriptor;

	public ElectricalSwitchRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalSwitchDescriptor) descriptor;

	}

	double voltageAnode = 0,voltageCatode = 0,current = 0,temperature = 0;
	LRDU front;

	@Override
	public void draw() {
		super.draw();
		Minecraft.getMinecraft().mcProfiler.startSection("Switch");
		
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];


		
		front.glRotateOnX();
		
		
		
		Obj3D obj = descriptor.getObj();
		if(obj != null)
		{
			String type = obj.getString("type");
			if(type.equals("lever"))
			{
				obj.draw("case");

				Obj3DPart part = obj.getPart("lever");
				
				
				if(part != null) 
				{
					float switchDelta;
					if(switchState == false)
					{
						switchDelta = part.getFloat("alphaOff") - switchAlpha;
					}
					else
					{
						switchDelta = part.getFloat("alphaOn") - switchAlpha;
					}
					float speed = part.getFloat("speed") * (FrameTime.get()); 
					if(speed > 1.0) speed = 1f;
					switchAlpha += switchDelta * speed;
					
					
					part.draw(switchAlpha, 0, 0, 1);
				}
			}
			//obj.draw("lever_lever");
		}
		//ClientProxy.obj.draw("HighVoltageSwitchAll", "lever_lever");
		/*

		ClientProxy.obj.draw("HighVoltageSwitch");
		
		GL11.glRotatef(leverAlpha, 0, 0, 1);
		ClientProxy.obj.draw("HighVoltageSwitchLever");
		*/	
		
		
	
		Minecraft.getMinecraft().mcProfiler.endSection();
	}
	
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.cableRender;
	}
	
	@Override
	public void glListDraw() {

	}
	@Override
	public boolean glListEnable() {
		return false;	
	}


	boolean boot = true;
	float switchAlpha = 0;
	boolean switchState;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b>>4)&3);
			switchState = stream.readBoolean();
			voltageAnode = stream.readShort() /Node.networkSerializeUFactor;
			voltageCatode = stream.readShort() /Node.networkSerializeUFactor;
			current = stream.readShort() /Node.networkSerializeIFactor;
			temperature = stream.readShort() /Node.networkSerializeTFactor;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		if(boot)
		{
			Obj3D obj = descriptor.getObj();
			if(obj != null)
			{
				String type = obj.getString("type");
				if(type.equals("lever"))
				{
					Obj3DPart part = obj.getPart("lever");			
					if(part != null) 
					{
						float switchDelta;
						if(switchState == false)
						{
							switchAlpha = part.getFloat("alphaOff");
						}
						else
						{
							switchAlpha = part.getFloat("alphaOn");
						}
					}
				}
			}
		}
		boot = false;
	}
}
