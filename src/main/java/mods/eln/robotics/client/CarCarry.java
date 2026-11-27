package mods.eln.robotics.client;

import mods.eln.robotics.servo.ElevatorCar;
import mods.eln.robotics.servo.Riders;
import mods.eln.robotics.servo.SegmentPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;

/**
 * The client side of the elevator cars: each client places every car by its own clock and carries its own player.
 * <p>
 * After the client's tick (ClientTickEvent.Post: the player has ticked, standing on the car where it was), each car
 * steps its local clock to the next tick of its segment, and a player standing on it goes the same way by
 * player.move (blocks still stop it). The player's previous position stays as its tick began, so the camera glides
 * through the carry; the renderer draws the car between the same two ticks (ElevatorCarRenderer). The player is never
 * moved before its own tick: that would leave nothing to interpolate. A new segment from the server replaces the clock:
 * if the car jumps (a stop the client had run past, a late start), its rider jumps with it.
 */
public final class CarCarry {
    private CarCarry() {
    }

    /** ClientTickEvent.Post. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        LocalPlayer player = mc.player;
        for (ElevatorCar c : new ArrayList<>(ElevatorCar.CLIENT)) {
            if (c.level() != mc.level || c.isRemoved()) {   // (from before a relog, or gone: not ours to step)
                ElevatorCar.CLIENT.remove(c);
                continue;
            }
            if (c.segment == null || c.origin == null) continue;
            AABB b0 = c.boxAt(c.clientPos(c.clientT));
            c.clientT++;
            double s1 = c.clientPos(c.clientT);
            AABB b1 = c.boxAt(s1);
            c.placeAt(s1);
            if (player != null) carry(player, b0, b1);
            if (player != null && c.cabinMode() == ElevatorCar.CABIN_CLAMP) clamp(player, c);
        }
    }

    /** A segment for a car (SegmentPayload's client handler). */
    public static void onSegment(SegmentPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(p.entityId()) instanceof ElevatorCar c)) return;
        AABB b0 = c.segment != null && c.origin != null ? c.boxAt(c.clientPos(c.clientT)) : null;
        c.apply(p);
        if (b0 != null && mc.player != null) carry(mc.player, b0, c.getBoundingBox());
    }

    /** Track B0 spike: keep the player inside a round cabin (the radial limit; the door opens at a stop). */
    static void clamp(LocalPlayer player, ElevatorCar c) {
        if (player.isSpectator() || player.isPassenger()) return;
        var out = c.railSide.getOpposite();
        var k = Riders.clampInside(player.getBoundingBox(), c.getX(), c.getZ(), ElevatorCar.ROUND_IN, c.getY(), c.getY() + ElevatorCar.CABIN,
            out.getStepX(), out.getStepZ(), Math.toRadians(40), c.stopped());
        if (k != null) player.setPos(player.position().add(k));
    }

    static void carry(LocalPlayer player, AABB b0, AABB b1) {
        if (player.isSpectator() || player.isPassenger()) return;
        Riders.Carry k = Riders.carry(player.getBoundingBox(), player.onGround(), b0, b1);
        if (!k.carried()) return;
        player.move(MoverType.SHULKER, k.delta());
        if (k.keepOnGround()) player.setOnGround(true);
        player.resetFallDistance();
    }
}
