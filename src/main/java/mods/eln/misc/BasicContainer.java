package mods.eln.misc;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class BasicContainer extends Container {

    protected IInventory inventory;

    public BasicContainer(EntityPlayer player, IInventory inventory, Slot[] slot) {
        this.inventory = inventory;

        for (int i = 0; i < slot.length; i++) {
            addSlotToContainer(slot[i]);
        }

        bindPlayerInventory(player.inventory);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return inventory.isUseableByPlayer(player);
    }

    protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new SlotWithSkin(inventoryPlayer, j + i * 9 + 9, j * 18, i * 18, SlotSkin.medium));
                // 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new SlotWithSkin(inventoryPlayer, i, i * 18, 58, SlotSkin.medium));
            // addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18,
            // 142));
        }
    }

    @Override
    protected Slot addSlotToContainer(Slot slot) {
        // slot.xDisplayPosition = helper.
        return super.addSlotToContainer(slot);
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
        ItemStack itemstack = null;
        Slot slot = (Slot) this.inventorySlots.get(slotId);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            int invSize = inventory.getSizeInventory();
            if (slotId < invSize) {
                if (!this.mergeItemStack(itemstack1, invSize, inventorySlots.size(), true)) {
                }
                // return null;
                // this.mergeItemStack(itemstack1, invSize, inventorySlots.size(), true);
            } else {
                if (!this.mergeItemStack(itemstack1, 0, invSize, true)) {
                    if (slotId < invSize + 27) {
                        if (!this.mergeItemStack(itemstack1, invSize + 27, inventorySlots.size(), false)) {
                        }
                    } else {
                        if (!this.mergeItemStack(itemstack1, invSize, invSize + 27, false)) {
                        }
                    }
                }

                // return null;
                // this.mergeItemStack(itemstack1, 0, invSize, false);
            }
            // if (!this.mergeItemStack(itemstack1, 0, inventorySlots.size(), true))
            // return null;
            // this.mergeItemStack(itemstack1, slotId, inventorySlots.size(), true);
            // this.mergeItemStack(itemstack1, 0, slotId - 1, true);

            if (itemstack1.stackSize == 0) {
                slot.putStack((ItemStack) null);
            } else {
                slot.onSlotChanged();
            }
        }

        // return itemstack;
        return null;
    }

    protected boolean mergeItemStack(ItemStack par1ItemStack, int par2, int par3, boolean par4) {
        boolean flag1 = false;
        int k = par2;

        if (par4) {
            k = par3 - 1;
        }

        Slot slot;
        ItemStack itemstack1;

        if (par1ItemStack.isStackable()) {
            while (par1ItemStack.stackSize > 0 && (!par4 && k < par3 || par4 && k >= par2)) {
                slot = (Slot) this.inventorySlots.get(k);

                itemstack1 = slot.getStack();

                if (slot.isItemValid(par1ItemStack) && itemstack1 != null && itemstack1.getItem() == par1ItemStack.getItem() && (!par1ItemStack.getHasSubtypes() || par1ItemStack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(par1ItemStack, itemstack1)) {
                    int l = itemstack1.stackSize + par1ItemStack.stackSize;
                    int maxSize = Math.min(slot.getSlotStackLimit(), par1ItemStack.getMaxStackSize());
                    if (l <= maxSize) {
                        par1ItemStack.stackSize = 0;
                        itemstack1.stackSize = l;
                        slot.onSlotChanged();
                        flag1 = true;
                    } else if (itemstack1.stackSize < maxSize) {
                        par1ItemStack.stackSize -= maxSize - itemstack1.stackSize;
                        itemstack1.stackSize = maxSize;
                        slot.onSlotChanged();
                        flag1 = true;
                    }
                }

                if (par4) {
                    --k;
                } else {
                    ++k;
                }
            }
        }

        if (par1ItemStack.stackSize > 0) {
            if (par4) {
                k = par3 - 1;
            } else {
                k = par2;
            }

            while (!par4 && k < par3 || par4 && k >= par2) {
                slot = (Slot) this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (itemstack1 == null && slot.isItemValid(par1ItemStack)) {
                    int l = par1ItemStack.stackSize;
                    int maxSize = Math.min(slot.getSlotStackLimit(), par1ItemStack.getMaxStackSize());
                    if (l <= maxSize) {
                        slot.putStack(par1ItemStack.copy());
                        slot.onSlotChanged();
                        par1ItemStack.stackSize = 0;
                        flag1 = true;
                        break;
                    } else {
                        par1ItemStack.stackSize -= maxSize;
                        ItemStack newItemStack = par1ItemStack.copy();
                        newItemStack.stackSize = maxSize;
                        slot.putStack(newItemStack);
                        slot.onSlotChanged();
                        flag1 = true;
                        break;
                    }
                    /*
					 * slot.putStack(par1ItemStack.copy()); slot.onSlotChanged(); par1ItemStack.stackSize = 0; flag1 = true;
					 */
                    // break;
                }

                if (par4) {
                    --k;
                } else {
                    ++k;
                }
            }
        }

        return flag1;
    }

    @Override
    public ItemStack slotClick(int arg0, int arg1, int arg2, EntityPlayer arg3) {
        if (arg0 >= this.inventorySlots.size()) {
            Eln.dp.println(DebugType.OTHER, "Damned !!! What happen ?");
            Utils.addChatMessage(arg3, "Damn! Sorry, this is a debug");
            Utils.addChatMessage(arg3, "message from Electrical age.");
            Utils.addChatMessage(arg3, "Could you send me a message about that?");
            Utils.addChatMessage(arg3, "Thanks :D");
            return null;
        }
        return super.slotClick(arg0, arg1, arg2, arg3);
    }
}
