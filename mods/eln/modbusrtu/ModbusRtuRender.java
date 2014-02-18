package mods.eln.modbusrtu;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import mods.eln.wirelesssignal.WirelessSignalTxElement;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import com.google.common.base.CaseFormat;


public class ModbusRtuRender extends SixNodeElementRender{

	ModbusRtuDescriptor descriptor;
	public ModbusRtuRender(SixNodeEntity tileEntity, Direction side,SixNodeDescriptor descriptor) {
		super(tileEntity,side,descriptor);
		this.descriptor = (ModbusRtuDescriptor) descriptor;
	}

	
	HashMap<Integer,WirelessTxStatus> wirelessTxStatusList = new HashMap<Integer,WirelessTxStatus>();
	HashMap<Integer,WirelessRxStatus> wirelessRxStatusList = new HashMap<Integer,WirelessRxStatus>();
	

	@Override
	public void draw() {
		// TODO Auto-generated method stub


	}

	int station = -1;
	String name;
	boolean boot = true;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		
		try {
			station = stream.readInt();
			name = stream.readUTF();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(boot)
			clientSend(ModbusRtuElement.serverAllSyncronise);
		boot = false;
		
	}
	

	@Override
	public void serverPacketUnserialize(DataInputStream stream)
			throws IOException {
		// TODO Auto-generated method stub
		super.serverPacketUnserialize(stream);
		
		switch (stream.readByte()) {
		case ModbusRtuElement.clientAllSyncronise:
		{
			wirelessTxStatusList.clear();
			for(int idx = stream.readInt();idx > 0;idx--){
				WirelessTxStatus tx = new WirelessTxStatus();
				tx.readFrom(stream);
				wirelessTxStatusList.put(tx.uuid, tx);
			}
			wirelessRxStatusList.clear();
			for(int idx = stream.readInt();idx > 0;idx--){
				WirelessRxStatus rx = new WirelessRxStatus();
				rx.readFrom(stream);
				wirelessRxStatusList.put(rx.uuid, rx);
			}
			
			
			rxTxChange = true;
		}
		break;
		case ModbusRtuElement.clientTx1Syncronise:
		{
			WirelessTxStatus newTx = new WirelessTxStatus();
			newTx.readFrom(stream);
			wirelessTxStatusList.put(newTx.uuid, newTx);
			rxTxChange = true;
		}
		break;
		case ModbusRtuElement.clientRx1Syncronise:
		{
			WirelessRxStatus newRx = new WirelessRxStatus();
			newRx.readFrom(stream);
			wirelessRxStatusList.put(newRx.uuid, newRx);
			rxTxChange = true;
		}
		break;
		case ModbusRtuElement.clientTxDelete:
		{
			wirelessTxStatusList.remove(stream.readInt());
			rxTxChange = true;
		}
		break;
		case ModbusRtuElement.clientRxDelete:
		{
			wirelessRxStatusList.remove(stream.readInt());
			rxTxChange = true;
		}
		break;
		case ModbusRtuElement.clientRx1Connected:
			WirelessRxStatus rx = wirelessRxStatusList.get(stream.readInt());
			if(rx != null){
				rx.connected = stream.readBoolean();
			}
			break;
		}
	}
	
	boolean rxTxChange = false;
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ModbusRtuGui(player, this);
	}



}
