package mods.eln.node.six;

import mods.eln.compat.IInventoryCompat;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class SixNodeElementInventory implements IInventoryCompat, INBTTReady {
    SixNodeElementRender sixnodeRender = null;
    SixNodeElement sixNodeElement = null;

    int stackLimit;

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElementRender sixnodeRender) {
        inv = new ItemStack[size];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
        this.stackLimit = stackLimit;
        this.sixnodeRender = sixnodeRender;
    }

    public SixNodeElementInventory(int size, int stackLimit, SixNodeElement sixNodeElement) {
        inv = new ItemStack[size];
        java.util.Arrays.fill(inv, ItemStack.EMPTY);
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

        if (slot >= getInv().length) return ItemStack.EMPTY;
        return getInv()[slot];
    }


    @Override
    public ItemStack decrStackSize(int slot, int amt) {
        ItemStack stack = getStackInSlot(slot);
        if (!Utils.isEmpty(stack)) {
            if (stack.getCount() <= amt) {
                setInventorySlotContents(slot, ItemStack.EMPTY);
            } else {
                stack = stack.splitStack(amt);
                if (stack.getCount() == 0) {
                    setInventorySlotContents(slot, ItemStack.EMPTY);
                }
            }
        }
        return stack;
    }


    @Override
    public ItemStack removeStackFromSlot(int slot) {
        ItemStack stack = getStackInSlot(slot);
        if (!Utils.isEmpty(stack)) {
            setInventorySlotContents(slot, ItemStack.EMPTY);
        }
        return stack;
    }


    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        try {
            getInv()[slot] = stack == null ? ItemStack.EMPTY : stack; // never store null (1.12)
            if (!Utils.isEmpty(stack) && stack.getCount() > getInventoryStackLimit()) {
                stack.setCount(getInventoryStackLimit());
            }
        } catch (Exception e) {
            // TODO: handle exception
        }


    }


    @Override
    public String getInventoryName() {
        return "tco.SixNodeInventory";
    }


    @Override
    public int getInventoryStackLimit() {

        return stackLimit;
    }


    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {

		/*if(sixNodeElement != null)
		{
			SixNode sixNode = (SixNode) NodeManager.instance.getNodeFromCoordonate(sixNodeElement.sixNode.coordonate);
			if(sixNode == null) return false;
			if(sixNode.sideElementList[sixNodeElement.side.getInt()] != sixNodeElement) return false;
		//	if( != transparentNodeElement.node) return false;
			return player.getDistanceSq(sixNodeElement.sixNode.coordonate.x + 0.5, sixNodeElement.sixNode.coordonate.y + 0.5, sixNodeElement.sixNode.coordonate.z + 0.5) < 18;
		}*/
        return true;
		
/*		if(sixNodeElement != null)
			return player.getDistanceSq(sixNodeElement.sixNode.coordonate.x + 0.5, sixNodeElement.sixNode.coordonate.y + 0.5, sixNodeElement.sixNode.coordonate.z + 0.5) < 18;
		return player.getDistanceSq(sixnodeRender.tileEntity.getPos().getX() + 0.5, sixnodeRender.tileEntity.getPos().getY() + 0.5, sixnodeRender.tileEntity.getPos().getZ() + 0.5) < 18;
*/
    }


    @Override
    public void openInventory() {


    }


    @Override
    public void closeInventory() {


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
    public void writeToNBT(NBTTagCompound nbt, String str) {

        Utils.writeToNBT(nbt, str, this);
    }


    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemstack) {

        return false;
    }

    @Override
    public boolean hasCustomInventoryName() {

        return false;
    }


}
