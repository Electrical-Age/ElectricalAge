package mods.eln.node;

import javax.management.NotificationBroadcaster;


import mods.eln.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class TransparentNodeElementInventory implements IInventory, INBTTReady{
	TransparentNodeElementRender transparentNodeRender = null;
	TransparentNodeElement transparentNodeElement = null;
	
	int stackLimit;
	
	public TransparentNodeElementInventory(int size,int stackLimit,TransparentNodeElementRender TransparentnodeRender)
	{
		inv = new ItemStack[size];
		this.stackLimit = stackLimit;
		this.transparentNodeRender = TransparentnodeRender;
	}
	
	public TransparentNodeElementInventory(int size,int stackLimit,TransparentNodeElement TransparentNodeElement)
	{
		inv = new ItemStack[size];
		this.stackLimit = stackLimit;
		this.transparentNodeElement = TransparentNodeElement;
	}
	
	
	
	
	private ItemStack[] inv;
	
	private ItemStack[] getInv()
	{
		return inv;
	}

	@Override
	public int getSizeInventory() {
		// TODO Auto-generated method stub
		return getInv().length;
	}



	@Override
	public ItemStack getStackInSlot(int slot) {
		// TODO Auto-generated method stub
		return getInv()[slot];
	}



	@Override
	public ItemStack decrStackSize(int slot, int amt){
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) {
                if (stack.stackSize <= amt) {
                        setInventorySlotContents(slot, null);
                } else {
                        stack = stack.splitStack(amt);
                        if (stack.stackSize == 0) {
                                setInventorySlotContents(slot, null);
                        }
                }
        }
        return stack;
	}



	@Override
	public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) {
                setInventorySlotContents(slot, null);
        }
        return stack;
	}



	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		getInv()[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
        }   	
	}



/**
     * Decrease the size of the stack in slot (first int arg) by the amount of the second int arg. Returns the new
     * stack.
     *//*
public ItemStack decrStackSize(int par1, int par2)
{
     if (inv[par1] != null)
     {
             if (inv[par1].stackSize <= par2)
             {
                     ItemStack itemstack = inv[par1];
                     inv[par1] = null;
                     return itemstack;
             }

             ItemStack itemstack1 = inv[par1].splitStack(par2);

             if (inv[par1].stackSize == 0)
             {
                     inv[par1] = null;
             }

             return itemstack1;
     }
     else
     {
             return null;
     }
}*/

/**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     *//*
public ItemStack getStackInSlotOnClosing(int par1)
{
     if (inv[par1] != null)
     {
             ItemStack itemstack = inv[par1];
             inv[par1] = null;
             return itemstack;
     }
     else
     {
             return null;
     }
}*/

/**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     *//*
public void setInventorySlotContents(int par1, ItemStack par2ItemStack)
{
     inv[par1] = par2ItemStack;

     if (par2ItemStack != null && par2ItemStack.stackSize > getInventoryStackLimit())
     {
             par2ItemStack.stackSize = getInventoryStackLimit();
     }
}*/
	@Override
	public String getInventoryName() { 
		return "tco.TransparentNodeInventory";
	}



	@Override
	public int getInventoryStackLimit() {
		// TODO Auto-generated method stub
		return stackLimit;
	}



	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		
		if(transparentNodeElement != null)
		{
			if(NodeManager.instance.getNodeFromCoordonate(transparentNodeElement.node.coordonate) != transparentNodeElement.node) return false;
			return player.getDistance(transparentNodeElement.node.coordonate.x + 0.5, transparentNodeElement.node.coordonate.y + 0.5, transparentNodeElement.node.coordonate.z + 0.5) < 10;
		}
		return true;
		//return player.getDistanceSq(transparentNodeRender.tileEntity.xCoord + 0.5, transparentNodeRender.tileEntity.yCoord + 0.5, transparentNodeRender.tileEntity.zCoord + 0.5) < 18;
	}



	@Override
	public void openInventory() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void closeInventory() {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public void markDirty() {
		// TODO Auto-generated method stub
		if(transparentNodeElement != null)
		{
			transparentNodeElement.inventoryChange(this);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		Utils.readFromNBT(nbt, str, this);
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		// TODO Auto-generated method stub
		Utils.writeToNBT(nbt, str, this);
	}



	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean hasCustomInventoryName() {
		// TODO Auto-generated method stub
		return false;
	}


}
