package mods.eln.simplenode.energyconverter;

import java.io.DataInputStream;
import java.io.IOException;

import buildcraft.api.power.IPowerEmitter;

import mods.eln.Other;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Optional.InterfaceList({
		@Optional.Interface(iface = "cofh.api.energy.IEnergyHandler", modid = Other.modIdTe),
		@Optional.Interface(iface = "buildcraft.api.power.IPowerEmitter", modid = Other.modIdBuildcraft)})
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity implements 
	IEnergyHandler,
	IPowerEmitter
	
	/* ,SidedEnvironment *//*,
	ISidedBatteryProvider, IPowerEmitter*//*, IPipeConnection*/{

	public EnergyConverterElnToOtherEntity() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {

		return super.onBlockActivated(entityPlayer, side, vx, vy, vz);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new EnergyConverterElnToOtherGui(player, this);
	}

	float inPowerFactor;
	boolean hasChanges = false;
	public float inPowerMax;

	@Override
	public void serverPublishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.serverPublishUnserialize(stream);
		try {
			inPowerFactor = stream.readFloat();
			inPowerMax = stream.readFloat();

			hasChanges = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getNodeUuid() {
		// TODO Auto-generated method stub
		return EnergyConverterElnToOtherNode.getNodeUuidStatic();
	}

	/*
	 * @Override
	 * 
	 * @Optional.Method(modid = Other.modIdOc) public Node sidedNode(ForgeDirection side) { if(worldObj.isRemote){ if(front.back() == Direction.from(side)) return node(); return null; }else{ if(getNode().getFront().back() == Direction.from(side)) return node(); return null; } }
	 * 
	 * @Override
	 * 
	 * @SideOnly(Side.CLIENT)
	 * 
	 * @Optional.Method(modid = Other.modIdOc) public boolean canConnect(ForgeDirection side) { if(front == null) return false; if(front.back() == Direction.from(side)) return true; return false; }
	 */

	// *************** RF **************
	@Override
	@Optional.Method(modid = Other.modIdTe)
	public boolean canConnectEnergy(ForgeDirection from) {
	//	Utils.println("*****canConnectEnergy*****");
	//	return true;
		if (worldObj.isRemote) return false;
		if(getNode() == null) return false;
		SimpleNode n = getNode();
		return n.getFront().back() == Direction.from(from);
	}

	@Override
	@Optional.Method(modid = Other.modIdTe)
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
	//Utils.println("*****receiveEnergy*****");
		return 0;
	}

	@Override
	@Optional.Method(modid = Other.modIdTe)
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		//Utils.println("*****extractEnergy*****");
		if (worldObj.isRemote) return 0;
		if(getNode() == null) return 0;
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		int extract = (int) Math.min(maxExtract, node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio()));
		if (simulate)
			node.drawEnergy(extract, Other.getElnToTeConversionRatio());

		return extract;
	}

	@Override
	@Optional.Method(modid = Other.modIdTe)
	public int getEnergyStored(ForgeDirection from) {
		//Utils.println("*****getEnergyStored*****");
		return 0;
	}

	@Override
	@Optional.Method(modid = Other.modIdTe)
	public int getMaxEnergyStored(ForgeDirection from) {
		//Utils.println("*****getMaxEnergyStored*****");
		return 0;
	}

	// ***************** Buildcraft ****************
	
	@Override
	@Optional.Method(modid = Other.modIdBuildcraft)
	public boolean canEmitPowerFrom(ForgeDirection side) {
		if (worldObj.isRemote) return false;
		if(getNode() == null) return false;
		SimpleNode n = getNode();
		return n.getFront().back() == Direction.from(side);
	}
	
	// ***************** Bridges ****************

	@Override
	public void updateEntity() {
		super.updateEntity();
		if(Other.teLoaded)EnergyConverterElnToOtherFireWallRf.updateEntity(this);
		if(Other.buildcraftLoaded)EnergyConverterElnToOtherFireWallBuildcraft.updateEntity(this);
	}

	public void onLoaded() {
		
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}

	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);

	}

	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
	}

	protected boolean addedToEnet;





}
