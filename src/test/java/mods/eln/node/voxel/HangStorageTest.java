package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/** How a cable's hang is kept: the 11-12 layout (`hang`) still drawn, the new one (`hang2`) through saves and the stream. */
public class HangStorageTest {
    @Test
    void anOldHangIsStillDrawn() {
        // a piece along x at y 0 over a gap: the 11-12 jar's chain {axis 0, line y 0, 3 points} from x 0 to 16
        CablePart c = new CablePart(1, 0, new int[]{0, 0, 8, 16, 0, 8});
        c.hang = new int[]{0, 0, 3, 0, 0, 64, 64, -16, 64, 128, 0, 64};
        var d = c.drawn();
        assertEquals(1, d.size());
        assertEquals(0, d.get(0).s0(), 1e-9);
        assertEquals(16, d.get(0).s1(), 1e-9);
        assertEquals(-2, d.get(0).pts()[1][1], 1e-9, "its middle point, 2 voxels down");
        double[] low = {Double.MAX_VALUE};
        VoxelGeometry.model(java.util.List.of(c), null).emit(f -> {
            for (int i = 1; i < f.xyz.length; i += 3) low[0] = Math.min(low[0], f.xyz[i]);
        });
        assertEquals(-2, low[0], 0.05);
    }

    @Test
    void theNewHangRoundTrips() throws Exception {
        CablePart c = new CablePart(1, 0, new int[]{0, 0, 8, 16, 0, 8});
        c.hang2 = new int[]{0, 128, 2, 0, 0, 64, 0, 64, 0, 128, -8, 64, 0, 64, 0};
        var tag = new net.minecraft.nbt.CompoundTag();
        c.write(tag);
        CablePart r = CablePart.read(tag);
        assertArrayEquals(c.hang2, r.hang2);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        c.serialize(new DataOutputStream(bo));
        CablePart s = CablePart.deserialize(new DataInputStream(new ByteArrayInputStream(bo.toByteArray())));
        assertArrayEquals(c.hang2, s.hang2);
        assertEquals(1, s.drawn().size());
        // a client on the older jar reads up to the old slot and stops: the stream keeps the old slot (empty) first
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bo.toByteArray()));
        in.readByte();
        in.readByte();
        in.readByte();
        in.readByte();
        int n = in.readByte();
        for (int i = 0; i < 3 * n; i++) in.readByte();
        assertEquals(0, in.readShort(), "the old hang slot is there, empty");
    }
}
