package mods.eln.node.six;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;

public class SixNodeElementInventory implements IInventory, INBTTReady {
    SixNodeElementRender sixnodeRender = null;
    SixNodeElement sixNodeElement = null;

    int stackLimit;

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElementRender sixnodeRender) {
        inv = new ItemStack[size];
        this.stackLimit = stackLimit;
        this.sixnodeRender = sixnodeRender;
    }

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElement sixNodeElement) {
        inv = new ItemStack[size];
        this.stackLimit = stackLimit;
        this.sixNodeElement = sixNodeElement;
    }


    private ItemStack[] inv;

    private ItemStack[] getInv() {
        return inv;
    }

    @Override
    public int getSizeInventory() {

        return getInv().length;
    }


    @Override
    public ItemStack getStackInSlot(int slot) {

        if (slot >= getInv().length) return null;
        return getInv()[slot];
    }


    @Override
    public ItemStack decrStackSize(int slot, int amt) {
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
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (stack != null) {
            setInventorySlotContents(slot, null);
        }
        return stack;
    }


    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        try {
            getInv()[slot] = stack;
            if (stack != null && stack.stackSize > getInventoryStackLimit()) {
                stack.stackSize = getInventoryStackLimit();
            }
        } catch (Exception e) {
            // TODO: handle exception
        }


    }


    @Override
    public String getName() {
        return "tco.SixNodeInventory";
    }


    @Override
    public int getInventoryStackLimit() {

        return stackLimit;
    }


    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {

		/*if(sixNodeElement != null)
		{
			SixNode sixNode = (SixNode) NodeManager.instance.getNodeFromCoordonate(sixNodeElement.sixNode.coordinate);
			if(sixNode == null) return false;
			if(sixNode.sideElementList[sixNodeElement.side.getInt()] != sixNodeElement) return false;
		//	if( != transparentNodeElement.node) return false;
			return player.getDistanceSq(sixNodeElement.sixNode.coordinate.x + 0.5, sixNodeElement.sixNode.coordinate.y + 0.5, sixNodeElement.sixNode.coordinate.z + 0.5) < 18;
		}*/
        return true;
		
/*		if(sixNodeElement != null)
			return player.getDistanceSq(sixNodeElement.sixNode.coordinate.x + 0.5, sixNodeElement.sixNode.coordinate.y + 0.5, sixNodeElement.sixNode.coordinate.z + 0.5) < 18;
		return player.getDistanceSq(sixnodeRender.tileEntity.xCoord + 0.5, sixnodeRender.tileEntity.yCoord + 0.5, sixnodeRender.tileEntity.zCoord + 0.5) < 18;
*/
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

    }


    @Override
    public void markDirty() {
        if (sixNodeElement != null && !sixNodeElement.sixNode.isDestructing()) {
            sixNodeElement.inventoryChanged();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {

        Utils.readFromNBT(nbt, str, this);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {

        return Utils.writeToNBT(nbt, str, this);
    }


    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {

        return false;
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {

    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean hasCustomName() {

        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return null;
    }


}
