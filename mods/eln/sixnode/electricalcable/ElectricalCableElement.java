package mods.eln.sixnode.electricalcable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import javax.swing.text.MaskFormatter;


import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.IThermalDestructorDescriptor;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.Simulator;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class ElectricalCableElement extends SixNodeElement {

	public ElectricalCableDescriptor descriptor;
	
	public ElectricalCableElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ElectricalCableDescriptor) descriptor;
		color = 0;
		colorCare = 1;
		electricalLoad.setCanBeSimplifiedByLine(true);
		electricalLoadList.add(electricalLoad);
		thermalLoadList.add(thermalLoad);
		Utils.d[0] = electricalLoad;

	}

	public NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	
	int color;
	int colorCare;

	@Override
	public void readFromNBT(NBTTagCompound nbt ) {
		super.readFromNBT(nbt);
		byte b = nbt.getByte("color");
		color = b & 0xF;
		colorCare = (b >> 4) & 1;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setByte("color", (byte) (color + (colorCare << 4)));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return electricalLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return thermalLoad;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		return descriptor.getNodeMask() /*+ NodeBase.maskElectricalWire*/ + (color << NodeBase.maskColorShift) + (colorCare << NodeBase.maskColorCareShift);
	}

	@Override
	public String multiMeterString() {
		return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
	}

	@Override
	public String thermoMeterString() {
		return Utils.plotCelsius("T", thermalLoad.Tc);
	}

	@Override
	public void networkSerialize(DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte((color << 4));
	    /*	stream.writeShort((short) (electricalLoad.Uc * NodeBase.networkSerializeUFactor));
	    	stream.writeShort((short) (electricalLoad.getCurrent() * NodeBase.networkSerializeIFactor));
	    	stream.writeShort((short) (thermalLoad.Tc * NodeBase.networkSerializeTFactor));*/
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void initialize() {
		descriptor.applyTo(electricalLoad);
		descriptor.applyTo(thermalLoad);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
	/*	World w = sixNode.coordonate.world();
		boolean exist = w.blockExists(10000, 0, 0);
		int id = w.getBlockId(10000, 0, 0);*/
		ItemStack currentItemStack = entityPlayer.getCurrentEquippedItem();
		//int i;
		if(Utils.isPlayerUsingWrench(entityPlayer)) {
			colorCare = colorCare ^ 1;
			Utils.addChatMessage(entityPlayer,"Wire color care " + colorCare);
			sixNode.reconnect();
		}
		else if(currentItemStack != null) {
			Item item = currentItemStack.getItem();

			GenericItemUsingDamageDescriptor gen = BrushDescriptor.getDescriptor(currentItemStack);
			if(gen != null && gen instanceof BrushDescriptor) {
				BrushDescriptor brush = (BrushDescriptor) gen;
				int brushColor = brush.getColor(currentItemStack);
				if(brushColor != color) {
					if(brush.use(currentItemStack)) {
						color = brushColor;
						sixNode.reconnect();
					}
					else {
						Utils.addChatMessage(entityPlayer,"Brush is empty");
					}
				}
			}
		}
		return false;
	}


}
