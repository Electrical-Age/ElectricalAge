package mods.eln.battery;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDUMask;
import mods.eln.node.NodeBase;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.lwjgl.opengl.GL11;

public class BatteryRender extends TransparentNodeElementRender {

	//public double voltagePositive = 0,voltageNegative,current = 0,temperature = 0;
	public float energy, life;
	
	public BatteryDescriptor descriptor;
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(2, 64, this);
	
	public BatteryRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (BatteryDescriptor) descriptor;
	}

	@Override
	public void draw() {
		front.glRotateXnRef();
		descriptor.draw(plus, minus);
	}

	boolean plus,minus;
	/*
	@Override
	public GuiContainer newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stupb
		return new TransformatorGuiDraw(player, inventory, this);
	}*/
	
	LRDUMask lrdu = new LRDUMask();
	
	float power;
	
	@Override
	public void networkUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.networkUnserialize(stream);
		try {
			/*voltagePositive = stream.readShort() / Node.networkSerializeUFactor;
			voltageNegative = stream.readShort() / Node.networkSerializeUFactor;
			current = stream.readShort() / Node.networkSerializeIFactor;
			temperature = stream.readShort() / Node.networkSerializeTFactor;
			*/
			power = stream.readFloat();
			energy = stream.readFloat();
			life = stream.readShort() / 1000.0f;

			lrdu.deserialize(stream);
			
			plus = true;
			minus = true;
			//plus = lrdu.get(lrdu)
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new BatteryGuiDraw(player, inventory, this);
	}
}
