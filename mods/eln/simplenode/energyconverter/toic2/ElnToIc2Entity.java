package mods.eln.simplenode.energyconverter.toic2;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.Info;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;

public class ElnToIc2Entity extends EnergyConverterElnToOtherEntity implements IEnergySource{

	@Override
	public String getNodeUuid() {
		// TODO Auto-generated method stub
		return ElnToIc2Node.getNodeUuidStatic();
	}


	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction) {
		/*if(front == null) 
			return false;*/
		if(worldObj.isRemote) return false;
		SimpleNode n = getNode();
		return n.front.back() == Direction.from(direction);
	}

	@Override
	public double getOfferedEnergy() {
		if(worldObj.isRemote)return 0;
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		double pMax = node.getOtherModOutMax();
		return pMax;
	}

	@Override
	public void drawEnergy(double amount) {
		if(worldObj.isRemote);
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
		node.drawEnergy(amount);
	}
	
	
	/**
	 * Forward for the base TileEntity's updateEntity(), used for creating the energy net link.
	 * Either updateEntity or onLoaded have to be used.
	 */
	@Override
	public void updateEntity() {
		super.updateEntity();
		if (!addedToEnet) onLoaded();
	}

	/**
	 * Notification that the base TileEntity finished loading, for advanced uses.
	 * Either updateEntity or onLoaded have to be used.
	 */
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
	 */
	@Override
	public void invalidate() {
		super.invalidate();

		onChunkUnload();
	}

	/**
	 * Forward for the base TileEntity's onChunkUnload(), used for destroying the energy net link.
	 * Both invalidate and onChunkUnload have to be used.
	 */
	@Override
	public void onChunkUnload() {
		super.onChunkUnload();
		if (addedToEnet &&
				Info.isIc2Available()) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));

			addedToEnet = false;
		}
	}
	
	protected boolean addedToEnet;




}
