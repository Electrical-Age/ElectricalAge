package mods.eln.simplenode.energyconverter;

//import cofh.api.energy.IEnergyHandler;
import cofh.api.energy.IEnergyProvider;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import mods.eln.Other;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.DataInputStream;
import java.io.IOException;

@Optional.InterfaceList({
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = Other.modIdIc2),
    @Optional.Interface(iface = "cofh.api.energy.IEnergyProvider", modid = Other.modIdTe),
    @Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = Other.modIdOc)})
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity implements Environment, IEnergyProvider {

    float inPowerFactor;
    boolean hasChanges = false;
    float inPowerMax;

    private EnergyConverterElnToOtherFireWallOc oc;

    EnergyConverterElnToOtherEntity() {
        if (Other.ocLoaded)
            getOc().constructor();
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

    @Override
    public void serverPublishUnserialize(DataInputStream stream) {
        super.serverPublishUnserialize(stream);
        try {
            inPowerFactor = stream.readFloat();
            inPowerMax = stream.readFloat();

            hasChanges = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getNodeUuid() {
        return EnergyConverterElnToOtherNode.getNodeUuidStatic();
    }

//    // ********************IC2********************
//
//    @Optional.Method(modid = Other.modIdIc2)
//    @Override
//    public boolean emitsEnergyTo(TileEntity receiver, EnumFacing direction) {
//        if (worldObj.isRemote)
//            return false;
//        SimpleNode n = getNode();
//        if (n == null)
//            return false;
//        return n.getFront().back() == Direction.fromFacing(direction);
//    }
//
//    @Optional.Method(modid = Other.modIdIc2)
//    @Override
//    public double getOfferedEnergy() {
//        if (worldObj.isRemote)
//            return 0;
//        if (getNode() == null)
//            return 0;
//        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
//        double pMax = node.getOtherModOutMax(node.descriptor.ic2.outMax,
//            Other.getElnToIc2ConversionRatio());
//        return pMax;
//    }
//
//    @Optional.Method(modid = Other.modIdIc2)
//    @Override
//    public void drawEnergy(double amount) {
//        if (worldObj.isRemote)
//            return;
//        if (getNode() == null)
//            return;
//
//        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
//        node.drawEnergy(amount, Other.getElnToIc2ConversionRatio());
//    }
//
//    @Optional.Method(modid = Other.modIdIc2)
//    // @Override
//    public int getSourceTier() {
//        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
//        if (node == null) return 0;
//        return node.descriptor.ic2.tier;
//    }

    // ***************** OC **********************

    @Optional.Method(modid = Other.modIdOc)
    EnergyConverterElnToOtherFireWallOc getOc() {
        if (oc == null)
            oc = new EnergyConverterElnToOtherFireWallOc(this);
        return oc;
    }

    @Override
    @Optional.Method(modid = Other.modIdOc)
    public Node node() {
        return getOc().node;
    }

    @Override
    @Optional.Method(modid = Other.modIdOc)
    public void onConnect(Node node) {
    }

    @Override
    @Optional.Method(modid = Other.modIdOc)
    public void onDisconnect(Node node) {
    }

    @Override
    @Optional.Method(modid = Other.modIdOc)
    public void onMessage(Message message) {
    }

    // *************** RF **************
    @Override
    @Optional.Method(modid = Other.modIdTe)
    public boolean canConnectEnergy(EnumFacing from) {
        // Utils.println("*****canConnectEnergy*****");
        // return true;
        if (worldObj.isRemote)
            return false;
        if (getNode() == null)
            return false;
        SimpleNode n = getNode();
        return n.getFront().back() == Direction.fromFacing(from);
    }

    @Override
    @Optional.Method(modid = Other.modIdTe)
    public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
        // Utils.println("*****extractEnergy*****");
        if (worldObj.isRemote)
            return 0;
        if (getNode() == null)
            return 0;
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        int extract = Math.max(0, Math.min(maxExtract, (int) node.getOtherModEnergyBuffer(Other.getElnToTeConversionRatio())));
        if (!simulate)
            node.drawEnergy(extract, Other.getElnToTeConversionRatio());

        return extract;
    }

    @Override
    @Optional.Method(modid = Other.modIdTe)
    public int getEnergyStored(EnumFacing from) {
        // Utils.println("*****getEnergyStored*****");
        return 0;
    }

    @Override
    @Optional.Method(modid = Other.modIdTe)
    public int getMaxEnergyStored(EnumFacing from) {
        // Utils.println("*****getMaxEnergyStored*****");
        return 0;
    }

    // ***************** Bridges ****************



    @Override
    public void update() {
//        if (Other.ic2Loaded)
//            EnergyConverterElnToOtherFireWallIc2.updateEntity(this);
        if (Other.ocLoaded)
            getOc().updateEntity();
        if (Other.teLoaded)
            EnergyConverterElnToOtherFireWallRf.updateEntity(this);
    }

//    public void onLoaded() {
//        if (Other.ic2Loaded)
//            EnergyConverterElnToOtherFireWallIc2.onLoaded(this);
//    }

    @Override
    public void invalidate() {
        super.invalidate();
//        if (Other.ic2Loaded)
//            EnergyConverterElnToOtherFireWallIc2.invalidate(this);
        if (Other.ocLoaded)
            getOc().invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
//        if (Other.ic2Loaded)
//            EnergyConverterElnToOtherFireWallIc2.onChunkUnload(this);
        if (Other.ocLoaded)
            getOc().onChunkUnload();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        if (Other.ocLoaded)
            getOc().readFromNBT(nbt);
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        if (Other.ocLoaded)
            getOc().writeToNBT(nbt);
        return nbt;
    }
}
