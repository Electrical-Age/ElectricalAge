package mods.eln.api.impl;

import mods.eln.api.Machine;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.TripCause;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

/**
 * The eln:machine data attachment of a hosted block entity. Saved: the machine's state under the api.machine.* keys
 * (ElementHost.saveState), taken from the live machine at save time, so the owner never marks anything. Synced: a
 * Snapshot of what a view shows, for the client's read-only machine. Also holds the block entity's Machine handle.
 */
public final class MachineData {
    /** The key of the saved state inside the attachment. */
    static final String STATE = "state";

    /** Server: the state loaded with the block entity until a node takes it; the state at retirement after. */
    CompoundTag saved;
    /** Server: the node hosting the machine now, or null. */
    MachineNode node;
    /** The owner's handle (server: a HostedMachine; client: a ClientMachine), made at the first of(). */
    Machine handle;
    /** Client: the last synced view, or null before the first. */
    volatile Snapshot snapshot;

    /** The state to save: the live machine's, else what was loaded (null: nothing). */
    CompoundTag stateTag() {
        MachineNode n = node;
        if (n != null) {
            CompoundTag t = new CompoundTag();
            n.host.saveState(t);
            return t;
        }
        return saved;
    }

    static final IAttachmentSerializer<MachineData> SERIALIZER = new IAttachmentSerializer<>() {
        @Override
        public MachineData read(IAttachmentHolder holder, ValueInput input) {
            MachineData d = new MachineData();
            d.saved = input.read(STATE, CompoundTag.CODEC).orElse(null);
            return d;
        }

        @Override
        public boolean write(MachineData d, ValueOutput output) {
            CompoundTag t = d.stateTag();
            if (t == null) return false;
            output.store(STATE, CompoundTag.CODEC, t);
            return true;
        }
    };

    static final AttachmentSyncHandler<MachineData> SYNC = new AttachmentSyncHandler<>() {
        @Override
        public void write(RegistryFriendlyByteBuf buf, MachineData d, boolean initialSync) {
            MachineNode n = d.node;
            Snapshot s = n == null ? null : Snapshot.of(n.host);
            buf.writeBoolean(s != null);
            if (s != null) s.write(buf);
        }

        @Override
        public MachineData read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, MachineData previous) {
            MachineData d = previous != null ? previous : new MachineData();
            d.snapshot = buf.readBoolean() ? Snapshot.read(buf) : null;
            return d;
        }
    };

    /** What a client's view of the machine shows. */
    public record Snapshot(MachineState state, double volts, double voltageRatio, double amps, double watts,
                           double bufferJoules, double bufferCapacityJoules, double celsius, TripCause lastTrip) {
        static Snapshot of(MachineView v) {
            return new Snapshot(v.state(), v.volts(), v.voltageRatio(), v.amps(), v.watts(), v.bufferJoules(),
                v.bufferCapacityJoules(), v.celsius(), v.lastTrip().orElse(null));
        }

        /** Same state and trip, and every number within 1% (or a hundredth of a unit). */
        boolean near(Snapshot o) {
            return o != null && state == o.state && java.util.Objects.equals(lastTrip, o.lastTrip)
                && near(volts, o.volts) && near(voltageRatio, o.voltageRatio) && near(amps, o.amps) && near(watts, o.watts)
                && near(bufferJoules, o.bufferJoules) && near(bufferCapacityJoules, o.bufferCapacityJoules) && near(celsius, o.celsius);
        }

        private static boolean near(double a, double b) {
            return Math.abs(a - b) <= Math.max(0.01, 0.01 * Math.max(Math.abs(a), Math.abs(b)));
        }

        void write(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeByte(state.ordinal());
            for (double d : new double[]{volts, voltageRatio, amps, watts, bufferJoules, bufferCapacityJoules, celsius})
                buf.writeDouble(d);
            buf.writeBoolean(lastTrip != null);
            if (lastTrip != null) {
                buf.writeByte(lastTrip.state().ordinal());
                buf.writeDouble(lastTrip.measured());
                buf.writeDouble(lastTrip.limit());
                buf.writeBoolean(lastTrip.burns());
            }
        }

        static Snapshot read(net.minecraft.network.FriendlyByteBuf buf) {
            MachineState[] states = MachineState.values();
            MachineState st = states[Math.floorMod(buf.readByte(), states.length)];
            double[] d = new double[7];
            for (int i = 0; i < d.length; i++) d[i] = buf.readDouble();
            TripCause trip = null;
            if (buf.readBoolean())
                trip = new TripCause(states[Math.floorMod(buf.readByte(), states.length)], buf.readDouble(), buf.readDouble(), buf.readBoolean());
            return new Snapshot(st, d[0], d[1], d[2], d[3], d[4], d[5], d[6], trip);
        }
    }
}
