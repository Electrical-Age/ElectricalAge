package mods.eln.electricalgatesource;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ElectricalGateSourceRender extends SixNodeElementRender {

	ElectricalGateSourceDescriptor descriptor;

	public ElectricalGateSourceRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalGateSourceDescriptor) descriptor;
		interpolator = new RcInterpolator(this.descriptor.speed);
	}

	LRDU front;

	RcInterpolator interpolator;
	
	@Override
	public void draw() {
		super.draw();
		drawSignalPin(front,new float[]{3,3,3,3});

		
		interpolator.setTarget((float)(voltageSyncValue / Eln.SVU));
		interpolator.stepGraphic();
		LRDU.Down.glRotateOnX();
		descriptor.draw(interpolator.get(), UtilsClient.distanceFromClientPlayer(this.tileEntity), tileEntity);
	}
	
	float voltageSyncValue = 0;
	boolean voltageSyncNew = false;
	boolean boot = true;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			front = LRDU.fromInt((b >> 4)&3);
			float readF;
			readF = stream.readFloat();
			if(voltageSyncValue != readF) {
				voltageSyncValue = readF;
				voltageSyncNew = true;
			}
			
			if(boot) {
				boot = false;
				interpolator.setValue((float)(voltageSyncValue / Eln.SVU));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalGateSourceGui(player, this);
	}
}
