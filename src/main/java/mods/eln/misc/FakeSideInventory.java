package mods.eln.misc;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class FakeSideInventory implements ISidedInventory {

    static private final FakeSideInventory instance = new FakeSideInventory();

    public static FakeSideInventory getInstance() {
        return instance;
    }

    @Override
    public int getSizeInventory() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int var1) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int var1, int var2) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int var1) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int var1, ItemStack var2) {

    }

    @Override
    public String getName() {
        return "FakeSideInventory";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(getName());
    }

    @Override
    public int getInventoryStackLimit() {
        return 0;
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer var1) {
        return false;
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2) {
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
    public int[] getSlotsForFace(EnumFacing side) {
        return new int[]{};
    }

    @Override
    public boolean canInsertItem(int var1, ItemStack var2, EnumFacing var3) {
        return false;
    }

    @Override
    public boolean canExtractItem(int var1, ItemStack var2, EnumFacing var3) {
        return false;
    }
}
