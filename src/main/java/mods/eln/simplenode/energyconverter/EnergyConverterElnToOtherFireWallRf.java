package mods.eln.simplenode.energyconverter;

import cofh.api.energy.IEnergyHandler;
import mods.eln.Other;
import net.minecraft.tileentity.TileEntity;

public class EnergyConverterElnToOtherFireWallRf {
	public static void updateEntity(EnergyConverterElnToOtherEntity e) {
		if (e.getWorldObj().isRemote) return;
		if (e.getNode() == null) return;

		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) e.getNode();
		TileEntity tileEntity = node.getFront().getInverse().applyToTileEntity(e);

        if(tileEntity == null || !(tileEntity instanceof IEnergyHandler)) return;
		IEnergyHandler energyHandler = (IEnergyHandler)tileEntity;
		
		double pMax = node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio());
		node.drawEnergy(energyHandler.receiveEnergy(node.getFront().toForge(), (int) pMax, false), Other.getElnToTeConversionRatio());
	}
}
