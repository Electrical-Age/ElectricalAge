package mods.eln.item;

import mods.eln.Eln;
import mods.eln.debug.DebugType;
import mods.eln.misc.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S2FPacketSetSlot;

public abstract class ItemMovingHelper {
    public abstract boolean acceptsStack(ItemStack stack);
    public abstract ItemStack newStackOfSize(int items);

    public void move(InventoryPlayer src, IInventory dst, int dstSlot, int desired) {
        if(Utils.isCreative((EntityPlayerMP) src.player)) {
            if(desired == 0) {
                dst.setInventorySlotContents(dstSlot, null);
            } else {
                dst.setInventorySlotContents(dstSlot, newStackOfSize(desired));
            }
            return;
        }
        int now = 0;
        ItemStack stack = dst.getStackInSlot(dstSlot);
        if(stack != null) {
            now = stack.stackSize;
        }
        Eln.dp.println(DebugType.OTHER, String.format("IMH.m: now %d, desired %d", now, desired));
        if(now < desired) {
            int diff = desired - now;
            for(int idx = 0; idx < src.getSizeInventory(); idx++) {
                ItemStack invStack = src.getStackInSlot(idx);
                if(invStack == null) continue;
                if(!acceptsStack(invStack)) continue;
                int move = Math.min(invStack.stackSize, diff);
                diff -= move;
                invStack.stackSize -= move;
                if(invStack.stackSize == 0) {
                    invStack = null;
                }
                src.setInventorySlotContents(idx, invStack);
                // Grissess: We need to send this immediately to sync with the client
                syncItemInSlot(src, idx);
                if(diff <= 0) break;
            }
            int moved = (desired - now) - diff;
            Eln.dp.println(DebugType.OTHER, String.format("IMH.m: moved %d into node", moved));
            if(moved > 0) {
                dst.setInventorySlotContents(dstSlot, newStackOfSize(now + moved));
            }
        } else {
            int diff = now - desired;
            Eln.dp.println(DebugType.OTHER, String.format("IMH.m: moving %d items", diff));
            if(diff > 0) {
                if (src.addItemStackToInventory(newStackOfSize(diff))) {
                    if(desired == 0) {
                        dst.setInventorySlotContents(dstSlot, null);
                    } else {
                        dst.setInventorySlotContents(dstSlot, newStackOfSize(desired));
                    }
                    Eln.dp.println(DebugType.OTHER, "IMH.m: move succeeded");
                } else {
                    Eln.dp.println(DebugType.OTHER, "IMH.m: move failed!");
                }
            }
            // Grissess: Since we can't tell how the inventory might have been changed
            // due to addItemStackToInventory, we have to take the conservative
            // approach and assume every slot might have changed.
            syncEntireInventory(src.player);
        }

    }

    public static void syncItemInSlot(InventoryPlayer inv, int slot) {
        EntityPlayerMP playerMP = (EntityPlayerMP) inv.player;
        Container container =  playerMP.openContainer;

        playerMP.playerNetServerHandler.sendPacket(
            new S2FPacketSetSlot(
                container.windowId,
                container.getSlotFromInventory(inv, slot).slotNumber,
                inv.getStackInSlot(slot)
            )
        );

        inv.markDirty();
    }

    public static void syncEntireInventory(EntityPlayer player) {
        IInventory inv = player.inventory;
        EntityPlayerMP playerMP = (EntityPlayerMP) player;

        for(int idx = 0; idx < inv.getSizeInventory(); idx++) {
            Container container = playerMP.openContainer;
            playerMP.playerNetServerHandler.sendPacket(
                new S2FPacketSetSlot(
                    container.windowId,
                    container.getSlotFromInventory(inv, idx).slotNumber,
                    inv.getStackInSlot(idx)
                )
            );
        }

        inv.markDirty();
    }

}
