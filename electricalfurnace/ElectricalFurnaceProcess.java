package mods.eln.electricalfurnace;

import org.omg.CORBA.INITIALIZE;

import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.sim.IProcess;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

public class ElectricalFurnaceProcess implements IProcess{

	ElectricalFurnaceElement furnace;
	TransparentNodeElementInventory inventory;
	
	public ElectricalFurnaceProcess(ElectricalFurnaceElement furnace) {
		this.furnace = furnace;
		this.inventory = (TransparentNodeElementInventory) furnace.getInventory();
	}
	

	
	
	
	ItemStack itemStackInOld = null;

	
	boolean smeltInProcess = false;
	double energyNeeded = 0;
	double energyCounter = 0;
	
	
	
	
	@Override
	public void process(double time) 
	{
		ItemStack itemStack = inventory.getStackInSlot(furnace.thermalIsolatorSlotId);
		
		if(itemStack == null)
		{
			furnace.descriptor.refreshTo(furnace.thermalLoad,1);
		}
		else
		{
			ThermalIsolatorElement element =((GenericItemUsingDamage<ThermalIsolatorElement>)itemStack.getItem()).getDescriptor(itemStack);
			furnace.descriptor.refreshTo(furnace.thermalLoad,element.conductionFactor);
		}
		
		
		ItemStack itemStackIn = inventory.getStackInSlot(ElectricalFurnaceElement.inSlotId);
		ItemStack itemStackOut = inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId);
		if(itemStackInOld != itemStackIn || (! smeltCan()) || smeltInProcess == false)
		{
			smeltInit();
			itemStackInOld = itemStackIn;
		}
		
		
		if(smeltInProcess)
		{
			energyCounter += furnace.smeltResistor.getP()*time;
			if(energyCounter > energyNeeded)
			{
				energyCounter -= energyNeeded;
				smeltItem();
				smeltInit();
			}
		}
		
		
		if(!smeltInProcess)
		{
			furnace.smeltResistor.highImpedance();
		}
		else
		{
			double T = Math.abs(furnace.thermalLoad.Tc) + 1;
			double P = furnace.descriptor.PfT.getValue(T);
			
			furnace.smeltResistor.setR(T/P);
		}
	}

	
	public void smeltInit()
	{
		smeltInProcess = smeltCan();
		if(! smeltInProcess)
		{
			smeltInProcess = false;
			energyNeeded = 1.0;
			energyCounter = 0.0;					
		}
		else
		{
			smeltInProcess = true;
			energyNeeded = 1000;
			energyCounter = 0.0;			
		}
	}
	
    /**
     * Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    private boolean smeltCan()
    {
        if (inventory.getStackInSlot(ElectricalFurnaceElement.inSlotId) == null)
        {
            return false;
        }
        else
        {
            ItemStack var1 = getSmeltResult();
            if (var1 == null) return false;
            if (inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId) == null) return true;
            if (!inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId).isItemEqual(var1)) return false;
            int result = inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId).stackSize + var1.stackSize;
            
            //energyNeeded = 1000.0;
            return (result <= inventory.getInventoryStackLimit() && result <= var1.getMaxStackSize());
        }
    }

    
    public ItemStack getSmeltResult()
    {
    	return FurnaceRecipes.smelting().getSmeltingResult(inventory.getStackInSlot(ElectricalFurnaceElement.inSlotId));
    }
    /**
     * Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack
     */
    public void smeltItem()
    {
    	
   
    	
        if (this.smeltCan())
        {
            ItemStack var1 = getSmeltResult();

            if (inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId) == null)
            {
                inventory.setInventorySlotContents(1,var1.copy());
            }
            else if (inventory.getStackInSlot(ElectricalFurnaceElement.outSlotId).isItemEqual(var1))
            {
            	inventory.decrStackSize(ElectricalFurnaceElement.outSlotId,-var1.stackSize);
            }

            /*--this.furnaceItemStacks[0].stackSize;

            if (this.furnaceItemStacks[0].stackSize <= 0)
            {
                this.furnaceItemStacks[0] = null;
            }*/
            inventory.decrStackSize(ElectricalFurnaceElement.inSlotId, 1);
        }
    }

}
