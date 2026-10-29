package mods.eln.node.voxel;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every voxel part draws for every tier (AdventurAgent's client crash, 2026-10-29: the chunk rebuild of a block holding a
 * large alloy pipe threw ArrayIndexOutOfBounds in VoxelGeometry.of: the pipe sprites were sized for two tiers). Pipes
 * straight, bent and ending on faces, valves of every mode, safety valves, sensors of every kind and adapters on pipes of
 * each tier; cables of each tier; and every per-tier table as long as the tiers.
 */
public class VoxelGeometryTiersTest {
    @Test
    void perTierTablesCoverEveryTier() {
        int n = PipePart.TIER_NAME.length;
        assertEquals(3, n);
        for (Object[] t : new Object[][]{{"GRID", PipePart.GRID.length}, {"DRAWN", PipePart.DRAWN.length}, {"VOLUME_PER_BLOCK", PipePart.VOLUME_PER_BLOCK.length},
            {"OLD_VOLUME_PER_BLOCK", PipePart.OLD_VOLUME_PER_BLOCK.length}, {"K_PER_BLOCK", PipePart.K_PER_BLOCK.length},
            {"WORKING_PA", PipePart.WORKING_PA.length}, {"BURST_PA", PipePart.BURST_PA.length}, {"SOFT_K", PipePart.SOFT_K.length},
            {"VoxelGeometry.PIPE", VoxelGeometry.PIPE.length}, {"PipeKitRecipes.PLATE", PipeKitRecipes.PLATE.length},
            {"PipeKitRecipes.PER_BLOCK", PipeKitRecipes.PER_BLOCK.length}, {"PipeKitRecipes.METAL", PipeKitRecipes.METAL.length}})
            assertEquals(n, (int) t[1], t[0] + " has an entry per pipe tier");
    }

    @Test
    void everyPartOfEveryTierDraws() {
        for (int tier = 0; tier < PipePart.TIER_NAME.length; tier++) {
            int bore = PipePart.GRID[tier];
            List<VoxelPart> parts = new ArrayList<>();
            parts.add(new PipePart(tier, new int[]{0, 8, 8, 16, 8, 8}));                    // straight, face to face
            parts.add(new PipePart(tier, new int[]{8, 0, 8, 8, 8, 8, 8, 8, 16}));           // a bend, from the floor to a side
            for (int mode : new int[]{ValvePart.ON_OFF, ValvePart.CONTROL, ValvePart.THROTTLE})
                parts.add(new ValvePart(8, 8, 8, 0, bore, mode));
            for (int outlet = 0; outlet < 6; outlet++) parts.add(new SafetyValvePart(8, 8, 8, outlet, bore, 10e5));
            for (int sensing : new int[]{PipeSensorPart.PRESSURE, PipeSensorPart.TEMPERATURE, PipeSensorPart.QUALITY})
                parts.add(new PipeSensorPart(8, 8, 8, 3, bore, sensing, 1));
            for (int face = 0; face < 6; face++) parts.add(new AdapterPart(face));
            for (VoxelPart p : parts) {
                var quads = VoxelGeometry.of(List.of(p));
                assertFalse(quads.isEmpty(), PipePart.TIER_NAME[tier] + ": " + p.getClass().getSimpleName() + " draws");
                for (var q : quads) assertTrue(q.sprite() != null, "a sprite for every quad");
            }
            VoxelGeometry.of(parts);   // (all together, as one block's model)
        }
        for (int tier = 0; tier < CablePart.TIERS.length; tier++)
            assertFalse(VoxelGeometry.of(List.of(new CablePart(tier, 0, new int[]{0, 0, 8, 16, 0, 8}))).isEmpty(), "cable tier " + tier + " draws");
    }
}
