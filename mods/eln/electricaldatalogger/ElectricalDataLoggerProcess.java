package mods.eln.electricaldatalogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.INBTTReady;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.misc.Profiler;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalDataLoggerProcess implements IProcess {
	ElectricalDataLoggerElement e;
	
	public ElectricalDataLoggerProcess(ElectricalDataLoggerElement e) {
		this.e = e;
	}

	@Override
	public void process(double time) {
		//Profiler p = new Profiler();
		//p.add("A");
		if(e.pause == false) {
			e.timeToNextSample -= time;
			byte value = ((byte)(e.inputGate.getNormalized() * 255.5 - 128));
			e.sampleStack += value;
			e.sampleStackNbr++;
		}
		//p.add("B");
		if(e.printToDo) {
			ItemStack paperStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.paperSlotId);
			ItemStack printStack = e.inventory.getStackInSlot(ElectricalDataLoggerContainer.printSlotId);
			if(paperStack != null && printStack == null) {
				e.inventory.decrStackSize(ElectricalDataLoggerContainer.paperSlotId, 1);
				ItemStack print = Eln.instance.dataLogsPrintDescriptor.newItemStack(1);
				Eln.instance.dataLogsPrintDescriptor.initializeStack(print, e.logs);
				e.inventory.setInventorySlotContents(ElectricalDataLoggerContainer.printSlotId, print);
			}
			e.printToDo = false;
		}
		//p.add("C");
		if(e.timeToNextSample <= 0.0) {
			e.timeToNextSample += e.logs.samplingPeriod;
			byte value = (byte)(e.sampleStack / e.sampleStackNbr);
			e.sampleStackReset();
			e.logs.write(value);
			
	    	ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
	        DataOutputStream packet = new DataOutputStream(bos);   	
	        
			e.preparePacketForClient(packet);
			
			try {
				packet.writeByte(e.toClientLogsAdd);
				packet.write(value);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//p.add("D");
			e.sendPacketToAllClient(bos);
		}
		//p.stop();
		//Utils.println(p);
	}
}
