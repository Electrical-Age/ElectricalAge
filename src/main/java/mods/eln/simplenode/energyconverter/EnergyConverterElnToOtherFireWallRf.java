package mods.eln.simplenode.energyconverter;

import cofh.redstoneflux.api.IEnergyReceiver;
import mods.eln.Other;
import net.minecraft.tileentity.TileEntity;

/** RF push (only loaded when RedstoneFlux is present). 1.12: receivers are IEnergyReceiver (1.7.10 IEnergyHandler). */
public class EnergyConverterElnToOtherFireWallRf {
    public static void updateEntity(EnergyConverterElnToOtherEntity e, EnergyConverterElnToOtherNode node, TileEntity tileEntity) {
        if (!(tileEntity instanceof IEnergyReceiver)) return;
        IEnergyReceiver energyHandler = (IEnergyReceiver) tileEntity;

        double pMax = node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio());
        node.drawEnergy(energyHandler.receiveEnergy(node.getFront().toEnumFacing(), (int) pMax, false), Other.getElnToTeConversionRatio());
    }
}
