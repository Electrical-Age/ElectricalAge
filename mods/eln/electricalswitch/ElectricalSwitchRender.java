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
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.UtilsClient;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

public class ElectricalSwitchRender extends SixNodeElementRender {

	ElectricalSwitchDescriptor descriptor;

	public ElectricalSwitchRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalSwitchDescriptor) descriptor;
		interpol = new RcInterpolator(this.descriptor.speed);
	}

	double voltageAnode = 0, voltageCatode = 0, current = 0, temperature = 0;
	
	RcInterpolator interpol;
	
	@Override
	public void draw() {
		super.draw();

		interpol.setTarget(switchState ? 1f : 0f);	
		interpol.stepGraphic();
		
		front.glRotateOnX();	
		descriptor.draw(interpol.get(), UtilsClient.distanceFromClientPlayer(tileEntity), tileEntity);
	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
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
		super.publishUnserialize(stream);
		try {
			switchState = stream.readBoolean();
			voltageAnode = stream.readShort() / NodeBase.networkSerializeUFactor;
			voltageCatode = stream.readShort() / NodeBase.networkSerializeUFactor;
			current = stream.readShort() / NodeBase.networkSerializeIFactor;
			temperature = stream.readShort() / NodeBase.networkSerializeTFactor;
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		if(boot) {
			interpol.setValue(switchState ? 1f : 0f);
		}
		boot = false;
	}
}
