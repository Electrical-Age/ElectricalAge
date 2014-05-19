package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.PlayerManager;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;

import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class ElectricalDataLoggerRender extends SixNodeElementRender {

	SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);
	ElectricalDataLoggerDescriptor descriptor;
	long time;
	public ElectricalDataLoggerRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;
		time = System.currentTimeMillis();
		clientSend(ElectricalDataLoggerElement.newClientId);
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}

	@Override
	public void draw() {
		super.draw();
		descriptor.draw(log, front);
	}

	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return descriptor.cableRender;
	}
	*/
	
	public boolean pause;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			log.unitType = stream.readByte();
			pause = stream.readBoolean();
			log.samplingPeriod = stream.readFloat();
			log.maxValue = stream.readFloat();
			log.minValue = stream.readFloat();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	DataLogs log = new DataLogs(ElectricalDataLoggerElement.logsSizeMax);
	boolean waitFistSync = true;
	
	@Override
	public void serverPacketUnserialize(DataInputStream stream) throws IOException {
		byte header = stream.readByte();
		
		switch(header) {
		case ElectricalDataLoggerElement.toClientLogsAdd:
		case ElectricalDataLoggerElement.toClientLogsClear:
			if(header == ElectricalDataLoggerElement.toClientLogsClear) {
				log.reset();
				waitFistSync = false;
			}
			int size = stream.available();
			while(size != 0) {
				size--;
				log.write(stream.readByte());
			}
		//	Utils.println(log);
			break;
		}
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalDataLoggerGui(player, inventory, this);
	}
}
