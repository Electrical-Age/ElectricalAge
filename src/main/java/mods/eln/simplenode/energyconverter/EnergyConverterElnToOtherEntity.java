package mods.eln.simplenode.energyconverter;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.simple.SimpleNodeEntity;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * EA -> other mods energy exporter. Energy leaves through the back face (front.back()), as in 1.7.10.
 * 26.x: Forge Energy only (1.12: FE, RF and IC2 EU; RF is FE now and IC2 has no 26.x port): the back face offers an
 * extract-only {@link EnergyHandler} (Capabilities.Energy.BLOCK, registered in Eln), and every tick the buffer is pushed
 * into the neighbour behind if it accepts energy there. Ratio: config balancing.ElnToThermalExpansionConversionRatio
 * (FE per J, default 4/3), as 1.12.
 */
public class EnergyConverterElnToOtherEntity extends SimpleNodeEntity {

    float inPowerFactor;
    boolean hasChanges = false;
    public float inPowerMax;

    private final ForgeEnergy forgeEnergy = new ForgeEnergy();

    public EnergyConverterElnToOtherEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.ENERGY_CONVERTER_ENTITY.get(), pos, state);
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

    // ******************** Forge Energy ********************

    /** The node, without side effects (capability probes are frequent). */
    @Nullable
    EnergyConverterElnToOtherNode peekNode() {
        if (level == null || level.isClientSide()) return null;
        NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(worldPosition, level));
        return n instanceof EnergyConverterElnToOtherNode ? (EnergyConverterElnToOtherNode) n : null;
    }

    /** The export face: front.back() (server: the node's front; client: the published front). */
    private boolean isExportFace(net.minecraft.core.@Nullable Direction facing) {
        if (facing == null || level == null) return false;
        Direction f;
        if (level.isClientSide()) {
            f = front;
        } else {
            EnergyConverterElnToOtherNode node = peekNode();
            f = node == null ? null : node.getFront();
        }
        return f != null && f.back() == Direction.fromEnumFacing(facing);
    }

    /** Capabilities.Energy.BLOCK provider: the back face only. */
    public @Nullable EnergyHandler getEnergyHandler(net.minecraft.core.@Nullable Direction facing) {
        return isExportFace(facing) ? forgeEnergy : null;
    }

    static double feRatio() {
        return Eln.ElnToTeConversionRatio;
    }

    /** Extract-only FE view of the node's energy buffer (J * ratio); transactional (the buffer is journaled). */
    final class ForgeEnergy extends SnapshotJournal<Double> implements EnergyHandler {
        @Override
        protected Double createSnapshot() {
            EnergyConverterElnToOtherNode node = peekNode();
            return node == null ? 0.0 : node.energyBuffer;
        }

        @Override
        protected void revertToSnapshot(Double snapshot) {
            EnergyConverterElnToOtherNode node = peekNode();
            if (node != null) node.energyBuffer = snapshot;
        }

        @Override
        public long getAmountAsLong() {
            EnergyConverterElnToOtherNode node = peekNode();
            return node == null ? 0 : (long) Math.max(0, node.getOtherModEnergyBuffer(feRatio()));
        }

        @Override
        public long getCapacityAsLong() {
            EnergyConverterElnToOtherNode node = peekNode();
            return node == null ? 0 : (long) (node.energyBufferMax * feRatio());
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            EnergyConverterElnToOtherNode node = peekNode();
            if (node == null || amount <= 0) return 0;
            int extract = Math.max(0, Math.min(amount, (int) node.getOtherModEnergyBuffer(feRatio())));
            if (extract > 0) {
                updateSnapshots(transaction);
                node.drawEnergy(extract, feRatio());
            }
            return extract;
        }
    }

    /**
     * Push the buffer into the neighbour behind (1.7.10: EnergyConverterElnToOtherFireWallRf.updateEntity).
     */
    void pushEnergy() {
        if (level == null || level.isClientSide()) return;
        EnergyConverterElnToOtherNode node = peekNode();
        if (node == null || node.getFront() == null) return;
        Direction back = node.getFront().back();
        BlockPos target = worldPosition.relative(back.toEnumFacing());
        EnergyHandler storage = level.getCapability(Capabilities.Energy.BLOCK, target, node.getFront().toEnumFacing());
        if (storage == null) return;
        int pMax = (int) node.getOtherModEnergyBuffer(feRatio());
        if (pMax <= 0) return;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = storage.insert(pMax, tx);
            if (accepted > 0) {
                node.drawEnergy(accepted, feRatio());
                tx.commit();
            }
        }
    }

    @Override
    public void update() {
        super.update();
        syncFrontState();
        pushEnergy();
    }

    /** Block state FRONT = the node's front (1.12 getActualState read it from the entity). */
    void syncFrontState() {
        EnergyConverterElnToOtherNode node = peekNode();
        if (node == null || node.getFront() == null) return;
        int f = node.getFront().toEnumFacing().get3DDataValue();
        BlockState s = getBlockState();
        if (s.hasProperty(EnergyConverterElnToOtherBlock.FRONT) && s.getValue(EnergyConverterElnToOtherBlock.FRONT) != f)
            level.setBlock(worldPosition, s.setValue(EnergyConverterElnToOtherBlock.FRONT, f), 3);
    }
}
