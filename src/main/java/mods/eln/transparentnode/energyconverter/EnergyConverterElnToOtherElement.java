package mods.eln.transparentnode.energyconverter;

import mods.eln.Eln;
import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.network.ClientValues;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * EA -> other mods energy exporter. EA power comes in on the front: a machine on the Machine API (a consumer rated at
 * the exporter's maximum power) whose taken joules fill the buffer; energy leaves through the back face
 * (front.back()), as in 1.7.10. 26.x: Forge Energy only (1.12:
 * FE, RF and IC2 EU; RF is FE now and IC2 has no 26.x port): the back face offers an extract-only {@link EnergyHandler}
 * (Capabilities.Energy.BLOCK on the transparent-node block entity, registered in Eln), and every simulator tick the
 * buffer is pushed into the neighbour behind if that block is loaded and accepts energy there. Ratio: config
 * balancing.ElnToThermalExpansionConversionRatio (FE per J, default 4/3), as 1.12.
 */
public class EnergyConverterElnToOtherElement extends TransparentNodeElement {

    final EnergyConverterElnToOtherDescriptor descriptor;

    /** The EA side; its parts are in this element's lists. */
    public final ElementMachine machine;
    final InputProcess inputProcess = new InputProcess();
    final PushProcess pushProcess = new PushProcess();

    public double energyBuffer = 0;
    public double energyBufferMax;
    public double inStdVoltage;
    public double inPowerMax;

    public double inPowerFactor = 0.5;

    public static final byte setInPowerFactor = 1;

    private final ForgeEnergy forgeEnergy = new ForgeEnergy();

    public EnergyConverterElnToOtherElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        this.descriptor = (EnergyConverterElnToOtherDescriptor) descriptor;
        this.descriptor.applyTo(this);
        // in the constructor, as the other elements: readFromNBT (before initialize) restores what is listed here.
        machine = Machines.forElement(this, this.descriptor.machineSpec());
        slowProcessList.add(inputProcess);
        slowProcessList.add(pushProcess);
    }

    @Override
    public void initialize() {
        connect();
    }

    public Direction getFront() {
        return front;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        // as the simple-node exporter: the front face, any cable position on it
        return machine.connectionMask(side);
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.powerLoad();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    /**
     * Each tick: what the step delivered goes into the buffer, and the next ask is the GUI's share of the maximum
     * power, eased off as the buffer passes half full (none when full). The ask scales with (U / Unom)^2, as the
     * resistor sized at nominal volts it replaces drew.
     */
    class InputProcess implements IProcess {
        /** The power asked for this tick, W (before the line's share). */
        double inP;

        @Override
        public void process(double time) {
            double energyMiss = energyBufferMax - energyBuffer;
            double factor = energyMiss <= 0 ? 0 : Math.min(1, energyMiss / energyBufferMax * 2);
            if (factor < 0.005) factor = 0;
            inP = factor * inPowerMax * inPowerFactor;
            double r = machine.voltageRatio();   // (above nominal volts: no more than rated, the machine's ceiling)
            energyBuffer += machine.takeJoules(Math.min(inP * r * r * time, machine.ratedJoulesPerTick()));
        }
    }

    @Override
    public java.util.Map<String, String> getWaila() {
        java.util.Map<String, String> info = super.getWaila();
        info.putAll(machine.wailaLines());
        return info;
    }

    public double getOtherModEnergyBuffer(double conversionRatio) {
        return energyBuffer * conversionRatio;
    }

    public void drawEnergy(double otherModEnergy, double conversionRatio) {
        energyBuffer -= otherModEnergy / conversionRatio;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("energyBuffer", energyBuffer);
        nbt.putDouble("inPowerFactor", inPowerFactor);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        energyBuffer = nbt.getDoubleOr("energyBuffer", 0.0);
        // (an older handler stored what the client sent: a NaN or negative factor threw in every tick)
        inPowerFactor = ClientValues.load(nbt.getDoubleOr("inPowerFactor", 0.5), 0, 1, 0.5);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) inPowerFactor);
            stream.writeFloat((float) inPowerMax);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte id = super.networkUnserialize(stream);
        try {
            if (id == setInPowerFactor) {
                inPowerFactor = ClientValues.clamp(stream.readFloat(), 0, 1, inPowerFactor);   // (the screen's slider: 0..1)
                needPublish();
                return unserializeNulldId;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return id;
    }

    // ******************** Forge Energy ********************

    static double feRatio() {
        return Eln.ElnToTeConversionRatio;
    }

    /** Capabilities.Energy.BLOCK on a face: the back face only (TransparentNode adds: not while burnt). */
    @Override
    public @Nullable EnergyHandler getEnergyHandler(net.minecraft.core.@Nullable Direction side) {
        if (side == null || front == null) return null;
        return Direction.fromEnumFacing(side) == front.back() ? forgeEnergy : null;
    }

    /** Extract-only FE view of the energy buffer (J * ratio); transactional (the buffer is journaled). */
    final class ForgeEnergy extends SnapshotJournal<Double> implements EnergyHandler {
        @Override
        protected Double createSnapshot() {
            return energyBuffer;
        }

        @Override
        protected void revertToSnapshot(Double snapshot) {
            energyBuffer = snapshot;
        }

        @Override
        public long getAmountAsLong() {
            return (long) Math.max(0, getOtherModEnergyBuffer(feRatio()));
        }

        @Override
        public long getCapacityAsLong() {
            return (long) (energyBufferMax * feRatio());
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount <= 0 || isBurnt()) return 0;
            int extract = Math.max(0, Math.min(amount, (int) getOtherModEnergyBuffer(feRatio())));
            if (extract > 0) {
                updateSnapshots(transaction);
                drawEnergy(extract, feRatio());
            }
            return extract;
        }
    }

    /**
     * Push the buffer into the neighbour behind (1.7.10: EnergyConverterElnToOtherFireWallRf.updateEntity), once per
     * simulator tick (the 20 Hz server tick, on the server thread; the simple-node exporter did it in its block-entity
     * tick). EA simulates unloaded chunks too: an unloaded neighbour is left alone (a capability lookup would load it).
     */
    class PushProcess implements IProcess {
        @Override
        public void process(double time) {
            push();
        }
    }

    /** One push into the block behind; returns the FE it took. */
    public int push() {
        if (front == null || !(world() instanceof ServerLevel level)) return 0;
        int pMax = (int) getOtherModEnergyBuffer(feRatio());
        if (pMax <= 0) return 0;
        BlockPos target = node.coordonate.pos().relative(front.back().toEnumFacing());
        if (level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) == null) return 0;
        EnergyHandler storage = level.getCapability(Capabilities.Energy.BLOCK, target, front.toEnumFacing());
        if (storage == null) return 0;
        try (Transaction tx = Transaction.openRoot()) {
            int accepted = storage.insert(pMax, tx);
            if (accepted > 0) {
                drawEnergy(accepted, feRatio());
                tx.commit();
            }
            return accepted;
        }
    }
}
