package mods.eln.node.voxel;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A pipe end as RoutePlacer's end (attach -1, dir as a face) and back: every direction and a free point unchanged. */
public class PipeEndAdapterTest {
    @Test
    void roundTripsEveryDirection() {
        for (int d = -1; d < 6; d++) {
            PipeRouter.End e = new PipeRouter.End(3, -17, 40, d);
            CableRouter.End c = PipePlacer.end(e);
            assertEquals(-1, c.attach());
            assertEquals(e, PipePlacer.end(c), "dir " + d);
        }
    }

    @Test
    void namesTheFace() {
        for (Direction f : Direction.values())
            assertEquals(f.get3DDataValue(), PipePlacer.end(new PipeRouter.End(0, 0, 0, PipePlacer.dirOf(f))).dir(), f.toString());
    }
}
