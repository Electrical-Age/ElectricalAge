package mods.eln.client.entity;

import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A bike rider's foot (no knee: leg pitched at the pedal, hip lifted) stays on its pedal all the way round (Baughn). */
public class RiderPoseTest {
    @Test
    void theFootFollowsThePedalRoundTheCrank() {
        ModelPart leg = new ModelPart(List.of(), Map.of());
        for (int i = 0; i < 36; i++) {
            float a = i * (float) Math.PI / 18;
            RiderPose.leg(leg, a, 1.9f);
            // the foot: the hip (pivot, relative to the default hip at y 12) plus the leg's 12 px turned by xRot
            float footY = (leg.y - 12) + 12 * (float) Math.cos(leg.xRot), footZ = 12 * (float) Math.sin(leg.xRot);
            float pedalY = RiderPose.BB_DOWN + RiderPose.CRANK_LEN * (float) Math.cos(a), pedalZ = -RiderPose.BB_AHEAD + RiderPose.CRANK_LEN * (float) Math.sin(a);
            double miss = Math.hypot(footY - pedalY, footZ - pedalZ);
            assertTrue(miss < 1.8, "crank " + i * 10 + " deg: the foot misses its pedal by " + miss + " px");
        }
    }
}
