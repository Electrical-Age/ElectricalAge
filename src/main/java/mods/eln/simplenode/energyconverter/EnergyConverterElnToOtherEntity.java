package mods.eln.simplenode.energyconverter;

import cofh.redstoneflux.api.IEnergyProvider;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import mods.eln.Other;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * EA -> other mods energy exporter. Energy leaves through the back face (front.back()), as in 1.7.10.
 * <ul>
 * <li>Forge Energy (1.12 port, new; FE = RF): {@link CapabilityEnergy#ENERGY} on the back face (extract only), and
 * every tick the buffer is pushed into the neighbour behind if it accepts FE there (replaces the 1.7.10 RF push).
 * Same ratio as RF: config balancing.ElnToThermalExpansionConversionRatio (FE per J, default 4/3).</li>
 * <li>RF (CoFH RedstoneFlux API, mod id "redstoneflux"): IEnergyProvider (1.7.10 IEnergyHandler; extract only), and
 * the push falls back to an RF IEnergyReceiver neighbour when it has no FE capability.</li>
 * <li>IC2 EU (mod id "ic2"): IEnergySource, tier/max packet per descriptor, ratio ElnToIndustrialCraftConversionRatio.</li>
 * <li>OpenComputers: dropped (1.12 port, not in the pack).</li>
 * </ul>
 */
@Optional.InterfaceList({
    @Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = EnergyConverterElnToOtherEntity.MODID_IC2),
    @Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyProvider", modid = EnergyConverterElnToOtherEntity.MODID_RF)})
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity implements IEnergySource, IEnergyProvider {

    // 1.12 mod ids (Other.modIdIc2 = "IC2" and modIdTe = "Eln" are the 1.7.10 ones; Loader ids are case-sensitive)
    public static final String MODID_IC2 = "ic2";
    public static final String MODID_RF = "redstoneflux";
    private static Boolean ic2Loaded, rfLoaded;

    static boolean ic2Loaded() {
        if (ic2Loaded == null) ic2Loaded = Loader.isModLoaded(MODID_IC2);
        return ic2Loaded;
    }

    static boolean rfLoaded() {
        if (rfLoaded == null) rfLoaded = Loader.isModLoaded(MODID_RF);
        return rfLoaded;
    }

    float inPowerFactor;
    boolean hasChanges = false;
    public float inPowerMax;

    protected boolean addedToEnet;

    private final ForgeEnergy forgeEnergy = new ForgeEnergy();

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

    // ******************** Forge Energy (1.12 port) ********************

    /** The node without SimpleNodeEntity.getNode()'s side effects (no DelayedBlockRemove; capability probes are frequent). */
    @Nullable
    EnergyConverterElnToOtherNode peekNode() {
        if (world == null || world.isRemote) return null;
        NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(pos.getX(), pos.getY(), pos.getZ(), world));
        return n instanceof EnergyConverterElnToOtherNode ? (EnergyConverterElnToOtherNode) n : null;
    }

    /** The export face: front.back() (server: the node's front; client: the published front). */
    private boolean isExportFace(@Nullable EnumFacing facing) {
        if (facing == null || world == null) return false;
        Direction f;
        if (world.isRemote) {
            f = front;
        } else {
            EnergyConverterElnToOtherNode node = peekNode();
            f = node == null ? null : node.getFront();
        }
        return f != null && f.back() == Direction.fromEnumFacing(facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return isExportFace(facing);
        return super.hasCapability(capability, facing);
    }

    @Override
    @Nullable
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityEnergy.ENERGY) return isExportFace(facing) ? CapabilityEnergy.ENERGY.cast(forgeEnergy) : null;
        return super.getCapability(capability, facing);
    }

    static double feRatio() {
        return Other.getElnToTeConversionRatio();
    }

    /** Extract-only FE view of the node's energy buffer (J * ratio); nothing on the client. */
    final class ForgeEnergy implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            EnergyConverterElnToOtherNode node = peekNode();
            if (node == null) return 0;
            int extract = Math.max(0, Math.min(maxExtract, (int) node.getOtherModEnergyBuffer(feRatio())));
            if (!simulate) node.drawEnergy(extract, feRatio());
            return extract;
        }

        @Override
        public int getEnergyStored() {
            EnergyConverterElnToOtherNode node = peekNode();
            return node == null ? 0 : (int) Math.max(0, node.getOtherModEnergyBuffer(feRatio()));
        }

        @Override
        public int getMaxEnergyStored() {
            EnergyConverterElnToOtherNode node = peekNode();
            return node == null ? 0 : (int) (node.energyBufferMax * feRatio());
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }

    /**
     * Push the buffer into the neighbour behind (1.7.10: EnergyConverterElnToOtherFireWallRf.updateEntity, RF only).
     * FE first; an RF-only receiver when it has no FE capability on that face.
     */
    void pushEnergy() {
        if (world.isRemote) return;
        if (getNode() == null) return;

        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        TileEntity tileEntity = node.getFront().getInverse().applyToTileEntity(this);
        if (tileEntity == null) return;
        EnumFacing face = node.getFront().toEnumFacing(); // the neighbour's face towards us
        if (tileEntity.hasCapability(CapabilityEnergy.ENERGY, face)) {
            IEnergyStorage storage = tileEntity.getCapability(CapabilityEnergy.ENERGY, face);
            if (storage == null || !storage.canReceive()) return;
            double pMax = node.getOtherModEnergyBuffer(feRatio());
            node.drawEnergy(storage.receiveEnergy((int) pMax, false), feRatio());
            return;
        }
        if (rfLoaded())
            EnergyConverterElnToOtherFireWallRf.updateEntity(this, node, tileEntity);
    }

    // ********************IC2********************

    @Optional.Method(modid = MODID_IC2)
    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, EnumFacing direction) {
        if (world.isRemote)
            return false;
        SimpleNode n = getNode();
        if (n == null)
            return false;
        return n.getFront().back() == Direction.fromEnumFacing(direction);
    }

    @Optional.Method(modid = MODID_IC2)
    @Override
    public double getOfferedEnergy() {
        if (world.isRemote)
            return 0;
        if (getNode() == null)
            return 0;
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        double pMax = node.getOtherModOutMax(node.descriptor.ic2.outMax,
            Other.getElnToIc2ConversionRatio());
        return pMax;
    }

    @Optional.Method(modid = MODID_IC2)
    @Override
    public void drawEnergy(double amount) {
        if (world.isRemote)
            return;
        if (getNode() == null)
            return;

        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        node.drawEnergy(amount, Other.getElnToIc2ConversionRatio());
    }

    @Optional.Method(modid = MODID_IC2)
    @Override
    public int getSourceTier() {
        EnergyConverterElnToOtherNode node = (EnergyConverterElnToOtherNode) getNode();
        if (node == null) return 0;
        return node.descriptor.ic2.tier;
    }

    // *************** RF **************
    @Override
    @Optional.Method(modid = MODID_RF)
    public boolean canConnectEnergy(EnumFacing from) {
        if (world.isRemote)
            return false;
        if (getNode() == null)
            return false;
        SimpleNode n = getNode();
        return n.getFront().back() == Direction.fromEnumFacing(from);
    }

    @Override
    @Optional.Method(modid = MODID_RF)
    public int extractEnergy(EnumFacing from, int maxExtract, boolean simulate) {
        if (world.isRemote)
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
    @Optional.Method(modid = MODID_RF)
    public int getEnergyStored(EnumFacing from) {
        return 0;
    }

    @Override
    @Optional.Method(modid = MODID_RF)
    public int getMaxEnergyStored(EnumFacing from) {
        return 0;
    }

    // ***************** Bridges ****************

    @Override
    public void update() {
        super.update();
        if (ic2Loaded())
            EnergyConverterElnToOtherFireWallIc2.updateEntity(this);
        pushEnergy();
    }

    public void onLoaded() {
        if (ic2Loaded())
            EnergyConverterElnToOtherFireWallIc2.onLoaded(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (ic2Loaded())
            EnergyConverterElnToOtherFireWallIc2.invalidate(this);
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        if (ic2Loaded())
            EnergyConverterElnToOtherFireWallIc2.onChunkUnload(this);
    }
}
