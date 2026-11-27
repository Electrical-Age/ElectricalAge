package mods.eln.robotics.servo;

import mods.eln.Eln;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client: the track a car runs on (track B: laid with the Track Kit), sent once with the car's spawn (pairing
 * data) and again when its drive's track changes; the car's motion still comes by SegmentPayload. Points in voxels (x, y,
 * z, stop flag per point); none: the car runs on its drive's rail (a straight path from the segment's origin and axis).
 */
public record TrackPayload(int entityId, int[] track) implements CustomPacketPayload {
    public static final Type<TrackPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "servo_track"));
    public static final StreamCodec<FriendlyByteBuf, TrackPayload> CODEC = StreamCodec.ofMember(TrackPayload::write, TrackPayload::read);
    /** At most this many points (a 64 m track from clicks a block apart). */
    static final int MAX_POINTS = 128;

    /** The payload for car entityId on points (blocks) with stops; null points: none (the rail). */
    public static TrackPayload of(int entityId, List<Vec3> points, boolean[] stops) {
        if (points == null) return new TrackPayload(entityId, new int[0]);
        int[] t = new int[points.size() * 4];
        for (int i = 0; i < points.size(); i++) {
            Vec3 q = points.get(i);
            t[4 * i] = (int) Math.round(q.x * 16);
            t[4 * i + 1] = (int) Math.round(q.y * 16);
            t[4 * i + 2] = (int) Math.round(q.z * 16);
            t[4 * i + 3] = stops[i] ? 1 : 0;
        }
        return new TrackPayload(entityId, t);
    }

    /** Its points (blocks), or null: none. */
    public List<Vec3> points() {
        if (track.length < 8) return null;
        List<Vec3> p = new ArrayList<>();
        for (int i = 0; i + 3 < track.length; i += 4) p.add(new Vec3(track[i] / 16.0, track[i + 1] / 16.0, track[i + 2] / 16.0));
        return p;
    }

    public boolean[] stops() {
        boolean[] s = new boolean[track.length / 4];
        for (int i = 0; i < s.length; i++) s[i] = track[4 * i + 3] != 0;
        return s;
    }

    void write(FriendlyByteBuf b) {
        b.writeVarInt(entityId);
        b.writeVarInt(track.length / 4);
        for (int v : track) b.writeInt(v);
    }

    static TrackPayload read(FriendlyByteBuf b) {
        int id = b.readVarInt(), n = b.readVarInt();
        if (n < 0 || n > MAX_POINTS) throw new IllegalArgumentException("servo track of " + n + " points");
        int[] t = new int[4 * n];
        for (int i = 0; i < t.length; i++) t[i] = b.readInt();
        return new TrackPayload(id, t);
    }

    @Override
    public Type<TrackPayload> type() {
        return TYPE;
    }

    /** Client side: what to do with one (set by the client setup). */
    public static java.util.function.Consumer<TrackPayload> clientHandler = p -> {
    };
}
