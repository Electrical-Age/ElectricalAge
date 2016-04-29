package mods.eln.node.transparent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.Node;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.IFluidHandler;

public class TransparentNode extends Node {

	public TransparentNodeElement element;
	public int elementId;
	public EntityPlayerMP removedByPlayer;

	@Override
	public boolean nodeAutoSave() {

		return false;
	}

	@Override
	public void onNeighborBlockChange() {
		super.onNeighborBlockChange();
		element.onNeighborBlockChange();
	}

	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt.getCompoundTag("node"));

		elementId = nbt.getShort("eid");
		try {
			TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(elementId);
			element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class, TransparentNodeDescriptor.class).newInstance(this, descriptor);
		} catch (InstantiationException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		} catch (NoSuchMethodException e) {

			e.printStackTrace();
		} catch (SecurityException e) {

			e.printStackTrace();
		}
		element.readFromNBT(nbt.getCompoundTag("element"));

	}

	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(Utils.newNbtTagCompund(nbt, "node"));

		nbt.setShort("eid", (short) elementId);

		element.writeToNBT(Utils.newNbtTagCompund(nbt, "element"));

	}

	@Override
	public void onBreakBlock() {

		element.onBreakElement();
		super.onBreakBlock();
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return element.getElectricalLoad(side, lrdu);
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return element.getThermalLoad(side, lrdu);
	}

	@Override
	public int getSideConnectionMask(Direction side, LRDU lrdu) {
		return element.getConnectionMask(side, lrdu);
	}

	@Override
	public String multiMeterString(Direction side) {
		return element.multiMeterString(side);
	}

	@Override
	public String thermoMeterString(Direction side) {
		return element.thermoMeterString(side);
	}

	public IFluidHandler getFluidHandler() {
		return element.getFluidHandler();
	}

	@Override
	public void publishSerialize(DataOutputStream stream) {

		super.publishSerialize(stream);

		try {
			stream.writeShort(this.elementId);
			element.networkSerialize(stream);
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public enum FrontType {
		BlockSide, PlayerView, PlayerViewHorizontal, BlockSideInv
	};

	@Override
	public void initializeFromThat(Direction side, EntityLivingBase entityLiving, ItemStack itemStack) {
		try {
			// Direction front = null;
			TransparentNodeDescriptor descriptor = Eln.transparentNodeItem.getDescriptor(itemStack);
			/*
			 * switch(descriptor.getFrontType()) { case BlockSide: front = side; break; case PlayerView: front = Utils.entityLivingViewDirection(entityLiving).getInverse(); break; case PlayerViewHorizontal: front = Utils.entityLivingHorizontalViewDirection(entityLiving).getInverse(); break;
			 * 
			 * }
			 */

			int metadata = itemStack.getItemDamage();
			elementId = metadata;
			element = (TransparentNodeElement) descriptor.ElementClass.getConstructor(TransparentNode.class, TransparentNodeDescriptor.class).newInstance(this, descriptor);
			element.initializeFromThat(side, entityLiving, itemStack.getTagCompound());
		} catch (InstantiationException e) {

			e.printStackTrace();
		} catch (IllegalAccessException e) {

			e.printStackTrace();
		} catch (IllegalArgumentException e) {

			e.printStackTrace();
		} catch (InvocationTargetException e) {

			e.printStackTrace();
		} catch (NoSuchMethodException e) {

			e.printStackTrace();
		} catch (SecurityException e) {

			e.printStackTrace();
		}

	}

	@Override
	public void initializeFromNBT() {
		element.initialize();
	}

	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz)
	{
		if (element.onBlockActivated(entityPlayer, side, vx, vy, vz)) return true;
		return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
	}

	@Override
	public boolean hasGui(Direction side) {
		if (element == null) return false;
		return element.hasGui();
	}

	public IInventory getInventory(Direction side)
	{
		if (element == null) return null;
		return element.getInventory();
	}

	public Container newContainer(Direction side, EntityPlayer player)
	{
		if (element == null) return null;
		return element.newContainer(side, player);
	}

	@Override
	public int getBlockMetadata() {
		return element.transparentNodeDescriptor.tileEntityMetaTag.meta;
	}

	@Override
	public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
		super.networkUnserialize(stream, player);

		Direction side;
		try {
			if (elementId == stream.readShort())
			{
				element.networkUnserialize(stream, player);
			}
			else
			{
				Utils.println("Transparent node unserialize miss");
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	@Override
	public void connectJob() {
		super.connectJob();
		element.connectJob();
	}

	@Override
	public void disconnectJob() {
		super.disconnectJob();
		element.disconnectJob();
	}

	@Override
	public void checkCanStay(boolean onCreate) {

		super.checkCanStay(onCreate);
		element.checkCanStay(onCreate);
	}

	public void dropElement(EntityPlayerMP entityPlayer) {
		if (element != null)
			if (Utils.mustDropItem(entityPlayer))
				dropItem(element.getDropItemStack());
	}

	@Override
	public String getNodeUuid() {

		return Eln.transparentNodeBlock.getNodeUuid();
	}

	@Override
	public void unload() {
		super.unload();
		if (element != null)
			element.unload();

	}

}
