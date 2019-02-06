package mods.eln.simplenode.energyconverter;

import cofh.api.energy.IEnergyReceiver;
import mods.eln.Other;
import net.minecraft.tileentity.TileEntity;

class EnergyConverterElnToOtherFireWallRf {
    // TODO(1.10): Fix RF conversion

    static void updateEntity(EnergyConverterElnToOtherEntity e) {
        if (e.getWorld().isRemote) return;
        if (e.getNode() == null) return;

        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) e.getNode();
        TileEntity tileEntity = node.getFront().getInverse().applyToTileEntity(e);

        if (!(tileEntity instanceof IEnergyReceiver)) return;
        IEnergyReceiver receiver = (IEnergyReceiver) tileEntity;

        double pMax = node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio());
        node.drawEnergy(receiver.receiveEnergy(node.getFront().toForge(), (int) pMax, false), Other.getElnToTeConversionRatio());
    }
}
