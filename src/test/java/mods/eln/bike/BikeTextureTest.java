package mods.eln.bike;

import org.erisia.cad.Box;
import org.erisia.cad.entity.EntityLayout;
import org.erisia.cad.tex.Sheet;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The committed bike texture is what BikeTexture paints, and the battery's stripe runs round it, meeting at the edges. */
public class BikeTextureTest {
    static final Path PNG = Path.of(System.getProperty("eln.resources", "src/main/resources"), "assets/eln/textures/entity/electric_bike.png");

    @Test
    void theCommittedTextureIsPainted() throws Exception {
        int[] size = new int[2];
        int[] committed = Sheet.readPng(PNG, size);
        assertArrayEquals(BikeTexture.paint().argb, committed, "electric_bike.png is stale: run ./gradlew bikeTexture");
    }

    static boolean green(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return g > 150 && g > r + 60 && g > b + 60;
    }

    @Test
    void theStripeMeetsItselfRoundTheBattery() {
        Sheet s = BikeTexture.paint();
        for (boolean terrain : new boolean[]{false, true}) {
            Box bat = BikeShape.battery(BikeShape.model(terrain));
            float[][] net = EntityLayout.net(bat);
            int stripeRow = -1;
            // the four side faces: the same row green from edge to edge
            for (int f : new int[]{0, 4, 1, 5}) {
                int[] in = Sheet.inside(net[f]);
                int row = -1;
                for (int y = in[1]; y <= in[3]; y++) {
                    boolean all = true;
                    for (int x = in[0]; x <= in[2]; x++) all &= green(s.get(x, y));
                    if (all) row = y;
                }
                assertTrue(row >= 0, "a green row across side face " + EntityLayout.FACE_NAME[f]);
                if (stripeRow < 0) stripeRow = row;
                assertTrue(row == stripeRow, "the same row on every side");
                // and the texels astride its edges too
                int x0 = (int) Math.floor(Math.min(net[f][0], net[f][2])), x1 = (int) Math.ceil(Math.max(net[f][0], net[f][2])) - 1;
                assertTrue(green(s.get(x0, row)) && green(s.get(x1, row)), "green up to the face's edges");
            }
            // none on the top and bottom
            for (int f : new int[]{2, 3}) {
                float[] r = net[f];
                for (int y = (int) Math.floor(Math.min(r[1], r[3])); y < Math.ceil(Math.max(r[1], r[3])); y++)
                    for (int x = (int) Math.floor(Math.min(r[0], r[2])); x < Math.ceil(Math.max(r[0], r[2])); x++)
                        assertTrue(!green(s.get(x, y)), "no stripe on " + EntityLayout.FACE_NAME[f]);
            }
        }
    }
}
