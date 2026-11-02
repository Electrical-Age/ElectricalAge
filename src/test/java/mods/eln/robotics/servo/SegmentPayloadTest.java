package mods.eln.robotics.servo;

import io.netty.buffer.Unpooled;
import mods.eln.robotics.motion.Segment;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The segment payload survives the wire, and the client's segment is the server's moved to its own clock. */
class SegmentPayloadTest {
    @Test
    void roundTrip() {
        Segment s = new Segment(1000, 2.5, 0.07, 6, 0.1, 0.0025);
        SegmentPayload p = SegmentPayload.of(4711, new Vec3(10.5, 64, -3.5), Direction.UP, Direction.WEST, s, 1012);
        assertEquals(12, p.elapsed());
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        SegmentPayload.CODEC.encode(b, p);
        SegmentPayload q = SegmentPayload.CODEC.decode(b);
        assertEquals(p, q);
        assertEquals(0, b.readableBytes());
        for (double t : new double[]{0, 3.5, 12, 40, 200})
            assertEquals(s.position(t), q.segment().position(t), 1e-12, "same motion at " + t);
    }

    @Test
    void aSegmentNotStartedYetHasNothingElapsed() {
        SegmentPayload p = SegmentPayload.of(1, Vec3.ZERO, Direction.UP, Direction.NORTH, Segment.rest(50, 3), 40);
        assertEquals(0, p.elapsed());
    }
}
