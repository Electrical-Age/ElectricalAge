package mods.eln.robotics.servo;

import mods.eln.Eln;
import mods.eln.robotics.motion.Segment;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Server -> client: a carriage's motion. Sent to everyone tracking it when a move starts or changes (a new call, a
 * sagging supply, a stop), on a fault (at rest where it stopped), and to a player who starts tracking it. Carries where
 * the axis is (origin of position 0, the axis, the side the rail is on), the segment without its start tick, and the
 * ticks of it already gone on the server: the client counts on from there by its own clock (no vanilla position sync).
 */
public record SegmentPayload(int entityId, Vec3 origin, Direction axis, Direction railSide, double from, double v0, double to,
                             double vmax, double accel, long elapsed) implements CustomPacketPayload {
    public static final Type<SegmentPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "servo_segment"));
    public static final StreamCodec<FriendlyByteBuf, SegmentPayload> CODEC = StreamCodec.ofMember(SegmentPayload::write, SegmentPayload::read);

    /** The payload for carriage entityId moving on segment s, now at tick `now` of the drive's clock. */
    public static SegmentPayload of(int entityId, Vec3 origin, Direction axis, Direction railSide, Segment s, long now) {
        return new SegmentPayload(entityId, origin, axis, railSide, s.from(), s.v0(), s.to(), s.vmax(), s.accel(), Math.max(0, now - s.startTick()));
    }

    /** The segment on the client's clock: it started at tick 0. */
    public Segment segment() {
        return new Segment(0, from, v0, to, vmax, accel);
    }

    void write(FriendlyByteBuf b) {
        b.writeVarInt(entityId);
        b.writeDouble(origin.x);
        b.writeDouble(origin.y);
        b.writeDouble(origin.z);
        b.writeByte(axis.get3DDataValue());
        b.writeByte(railSide.get3DDataValue());
        b.writeDouble(from);
        b.writeDouble(v0);
        b.writeDouble(to);
        b.writeDouble(vmax);
        b.writeDouble(accel);
        b.writeVarLong(elapsed);
    }

    static SegmentPayload read(FriendlyByteBuf b) {
        int id = b.readVarInt();
        Vec3 o = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
        Direction axis = Direction.from3DDataValue(b.readByte()), side = Direction.from3DDataValue(b.readByte());
        return new SegmentPayload(id, o, axis, side, b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readVarLong());
    }

    @Override
    public Type<SegmentPayload> type() {
        return TYPE;
    }

    /** Client side: what to do with one (set by the client setup; nothing on a dedicated server). */
    public static java.util.function.Consumer<SegmentPayload> clientHandler = p -> {
    };
}
