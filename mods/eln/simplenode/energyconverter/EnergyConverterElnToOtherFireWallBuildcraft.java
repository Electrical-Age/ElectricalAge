package mods.eln.simplenode.energyconverter;

import buildcraft.api.power.IPowerReceptor;
import buildcraft.api.power.PowerHandler.PowerReceiver;
import buildcraft.api.power.PowerHandler.Type;
import cofh.api.energy.IEnergyHandler;
import mods.eln.Other;
import net.minecraft.tileentity.TileEntity;

public class EnergyConverterElnToOtherFireWallBuildcraft {
	public static void updateEntity(EnergyConverterElnToOtherEntity e){
		if(e.getWorldObj().isRemote)return;
		EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) e.getNode();
		TileEntity tileEntity = node.getFront().getInverse().applyToTileEntity(e);
		if(tileEntity == null || false == tileEntity instanceof IPowerReceptor)return;
		IPowerReceptor powerReceptor = (IPowerReceptor)tileEntity;
		PowerReceiver rx = powerReceptor.getPowerReceiver(node.getFront().toForge());
		if(rx == null) return;
		
		double pMax = node.getOtherModEnergyBuffer(Other.getElnToBuildcraftConversionRatio());
		node.drawEnergy(rx.receiveEnergy(Type.ENGINE, pMax, node.getFront().toForge()),Other.getElnToBuildcraftConversionRatio());
	}
}
