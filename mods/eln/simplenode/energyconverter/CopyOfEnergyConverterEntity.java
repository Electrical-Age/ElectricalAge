package mods.eln.simplenode.energyconverter;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.Info;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.FMLCommonHandler;
/*
public class CopyOfEnergyConverterEntity extends SimpleNodeEntity implements IEnergyHandler,IEnergySource,IEnergySink{
	
	
	EnergyConverterNode getEnergyConverterNode(){
		return (EnergyConverterNode) getNode();
	}
	
	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return getEnergyConverterNode().canConnectEnergy(from);
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {

		return getEnergyConverterNode().receiveEnergy(from, maxReceive, simulate);
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {

		return getEnergyConverterNode().extractEnergy(from, maxExtract, simulate);
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {

		return getEnergyConverterNode().getEnergyStored(from);
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {

		return getEnergyConverterNode().getEnergyStored(from);
	}

	//**********SRC********
	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public double getOfferedEnergy() {
		// TODO Auto-generated method stub
		return 32;
	}

	@Override
	public void drawEnergy(double amount) {
		// TODO Auto-generated method stub
		
	}


	//********** SINK*************
	@Override
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public double demandedEnergyUnits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double injectEnergyUnits(ForgeDirection directionFrom, double amount) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxSafeInput() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**
	 * Forward for the base TileEntity's updateEntity(), used for creating the energy net link.
	 * Either updateEntity or onLoaded have to be used.
	 *//*
	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!addedToEnet) onLoaded();
	}

	/**
	 * Notification that the base TileEntity finished loading, for advanced uses.
	 * Either updateEntity or onLoaded have to be used.
	 *//*
	public void onLoaded() {
		if (!addedToEnet &&
				!FMLCommonHandler.instance().getEffectiveSide().isClient() &&
				Info.isIc2Available()) {


			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));

			addedToEnet = true;
		}
	}

	/**
	 * Forward for the base TileEntity's invalidate(), used for destroying the energy net link.
	 * Both invalidate and onChunkUnload have to be used.
	 *//*
	@Override
	public void invalidate() {
		super.invalidate();

		onChunkUnload();
	}

	/**
	 * Forward for the base TileEntity's onChunkUnload(), used for destroying the energy net link.
	 * Both invalidate and onChunkUnload have to be used.
	 *//*
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (addedToEnet &&
				Info.isIc2Available()) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));

			addedToEnet = false;
		}
	}
	
	protected int capacity;
	protected int tier;
	protected double energyStored;
	protected boolean addedToEnet;

}
*/