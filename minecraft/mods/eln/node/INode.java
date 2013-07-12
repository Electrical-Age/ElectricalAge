package mods.eln.node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;


public interface INode {
    public abstract ElectricalLoad getElectricalLoad(Direction side,LRDU lrdu);
	public abstract ThermalLoad getThermalLoad(Direction side,LRDU lrdu);
	
	
	public static final int maskElectricalPower = 1<<0 ;
	public static final int maskThermal= 1<<1;
	
	public static final int maskElectricalGate = (1<<2);
	public static final int maskElectricalAll = maskElectricalPower | maskElectricalGate;

	public static final int maskElectricalInputGate = maskElectricalPower | maskElectricalGate;
	public static final int maskElectricalOutputGate = maskElectricalGate;
	
	
	public static final int maskWire = 0;
	public static final int maskElectricalWire = (1<<3); 
	public static final int maskThermalWire = maskWire + maskThermal;
	
	public static final int maskSignal = (1<<9);

	public static final int maskColorData = 0xF<<16;
	public static final int maskColorShift = 16;
	public static final int maskColorCareShift = 20;
	public static final int maskColorCareData = 1<<20;
	
	public abstract int getSideConnectionMask(Direction side,LRDU lrdu);
	
	public abstract String multiMeterString(Direction side);
	public abstract String thermoMeterString(Direction side);
	
	
	public abstract void networkSerialize(DataOutputStream stream);

	
    public abstract void initializeFromThat(Direction front,EntityLivingBase entityLiving,ItemStack itemStack);
    public abstract void initializeFromNBT();
    
	public abstract short getBlockId();
	
	public abstract boolean hasGui(Direction side);
	//public abstract IInventory getInventory(Direction side);
	   
	
	
}
