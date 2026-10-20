package mods.eln.node.voxel;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A cable route being laid lapses (Baughn 2026-10-20: waypoints never timed out, and a later cable started from an old
 * attempt): on another item in hand, a minute without a click, the player beyond a full kit's reach of its last point,
 * another dimension; and holds otherwise.
 */
public class CableRouteLapseTest {
    static CablePlacer.Plan plan(long touched) {
        CablePlacer.Plan p = new CablePlacer.Plan(1, new CableRouter.End(0, 16, 0, 0, -1), null, null);
        p.touched = touched;
        return p;
    }

    @Property(tries = 300)
    void lapsesExactlyWhenItShould(@ForAll @LongRange(min = 0, max = 4000) long idle, @ForAll boolean holding, @ForAll boolean sameDim,
                                   @ForAll @IntRange(min = 0, max = 120) int blocksAway, @ForAll @IntRange(min = 0, max = 3) int waypoints) {
        CablePlacer.Plan p = plan(1000);
        for (int i = 0; i < waypoints; i++) p.waypoints.add(new CableRouter.End(16L * 10 * (i + 1), 16, 0, 0, -1));
        CableRouter.End last = p.waypoints.isEmpty() ? p.start : p.waypoints.get(p.waypoints.size() - 1);
        Vec3 at = new Vec3(last.x() / 16.0 + blocksAway, last.y() / 16.0, last.z() / 16.0);
        String why = CablePlacer.lapse(p, 1000 + idle, holding, sameDim, at);
        boolean should = !holding || !sameDim || idle > CablePlacer.IDLE_TICKS || blocksAway * 16 > KitItem.MAX;
        assertEquals(should, why != null, "idle " + idle + " holding " + holding + " same dim " + sameDim + " away " + blocksAway + ": " + why);
    }
}
