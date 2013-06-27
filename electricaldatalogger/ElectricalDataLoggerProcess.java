package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalDataLoggerProcess implements IProcess{
	ElectricalDataLoggerElement e;
	
	
	
	public ElectricalDataLoggerProcess(ElectricalDataLoggerElement e) {
		this.e = e;
	}

	@Override
	public void process(double time) {
		if(e.pause == false)
			e.timeToNextSample -= time;
		
		if(e.printToDo)
		{
			ItemStack paperStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.paperSlotId);
			ItemStack printStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.printSlotId);
			if(paperStack != null && printStack == null)
			{
				e.inventory.decrStackSize(ElectricalDataLoggerContainer.paperSlotId, 1);
				ItemStack print = Eln.instance.dataLogsPrintDescriptor.newItemStack(1);
				Eln.instance.dataLogsPrintDescriptor.initializeStack(print,e.logs);
				e.inventory.setInventorySlotContents(ElectricalDataLoggerContainer.printSlotId, print);
			}
			e.printToDo = false;
		}
		
		if(e.timeToNextSample <= 0.0)
		{
			e.timeToNextSample += e.samplingPeriod;
			byte value = ((byte)(e.inputGate.getNormalized() * 255.5 - 128));
			e.logs.write(value);
			
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
	        DataOutputStream packet = new DataOutputStream(bos);   	
	        
			e.preparePacketForClient(packet);
			
			
			try {
				packet.writeByte(e.toClientLogsAdd);
				packet.write(value);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.sendPacketToAllClient(bos);
			
		}
	}
	
	

}
