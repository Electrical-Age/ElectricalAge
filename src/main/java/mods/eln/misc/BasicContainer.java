package mods.eln.misc;

import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.gui.SlotWithSkin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

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
    public boolean canInteractWith(@NotNull EntityPlayer player) {
        return inventory.isUsableByPlayer(player);
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
        // slot.xPos = helper.
        return super.addSlotToContainer(slot);
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotId) {
        Slot slot = this.inventorySlots.get(slotId);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            int invSize = inventory.getSizeInventory();
            if (slotId < invSize) {
                this.mergeItemStack(stack, invSize, inventorySlots.size(), true);
            } else {
                if (!this.mergeItemStack(stack, 0, invSize, true)) {
                    if (slotId < invSize + 27) {
                        this.mergeItemStack(stack, invSize + 27, inventorySlots.size(), false);
                    } else {
                        this.mergeItemStack(stack, invSize, invSize + 27, false);
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

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
            while (!par1ItemStack.isEmpty() && (!par4 && k < par3 || par4 && k >= par2)) {
                slot = this.inventorySlots.get(k);

                itemstack1 = slot.getStack();

                if (slot.isItemValid(par1ItemStack) && !itemstack1.isEmpty() && itemstack1.getItem() == par1ItemStack.getItem() && (!par1ItemStack.getHasSubtypes() || par1ItemStack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(par1ItemStack, itemstack1)) {
                    int l = itemstack1.getCount() + par1ItemStack.getCount();
                    int maxSize = Math.min(slot.getSlotStackLimit(), par1ItemStack.getMaxStackSize());
                    if (l <= maxSize) {
                        par1ItemStack.setCount(0);
                        itemstack1.setCount(1);
                        slot.onSlotChanged();
                        flag1 = true;
                    } else if (itemstack1.getCount() < maxSize) {
                        par1ItemStack.splitStack(maxSize - itemstack1.getCount());
                        itemstack1.setCount(maxSize);
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

        if (!par1ItemStack.isEmpty()) {
            if (par4) {
                k = par3 - 1;
            } else {
                k = par2;
            }

            while (!par4 && k < par3 || par4 && k >= par2) {
                slot = this.inventorySlots.get(k);
                itemstack1 = slot.getStack();

                if (!itemstack1.isEmpty() && slot.isItemValid(par1ItemStack)) {
                    int l = par1ItemStack.getCount();
                    int maxSize = Math.min(slot.getSlotStackLimit(), par1ItemStack.getMaxStackSize());
                    if (l <= maxSize) {
                        slot.putStack(par1ItemStack.copy());
                        slot.onSlotChanged();
                        par1ItemStack.setCount(0);
                        flag1 = true;
                        break;
                    } else {
                        par1ItemStack.splitStack(maxSize);
                        ItemStack newItemStack = par1ItemStack.copy();
                        newItemStack.setCount(maxSize);
                        slot.putStack(newItemStack);
                        slot.onSlotChanged();
                        flag1 = true;
                        break;
                    }
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
    public ItemStack slotClick(int arg0, int arg1, ClickType type, EntityPlayer arg3) {
        if (arg0 >= this.inventorySlots.size()) {
            System.out.println("Damned !!! What happen ?");
            Utils.sendMessage(arg3, "Damn! Sorry, this is a debug");
            Utils.sendMessage(arg3, "message from Electrical age.");
            Utils.sendMessage(arg3, "Could you send me a message about that?");
            Utils.sendMessage(arg3, "Thanks :D");
            return null;
        }
        return super.slotClick(arg0, arg1, type, arg3);
    }
}
