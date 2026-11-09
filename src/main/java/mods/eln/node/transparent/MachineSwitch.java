package mods.eln.node.transparent;

import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * The on/off switch on a machine's control box (Nero 2026-11-08; Baughn OK): right-click the little lever, or
 * sneak-right-click anywhere on the machine, to switch it. Off, the machine does no work and draws no power; its lever
 * points down and its run LED is dark. Machines built before stay on (no key saved = on).
 */
public final class MachineSwitch {
    public static final String NBT = "machineOn";
    /** The lever's swing either side of level, degrees. */
    static final float SWING = 30;

    public boolean on = true;

    public void write(CompoundTag nbt) {
        if (!on) nbt.putBoolean(NBT, false);
    }

    public void read(CompoundTag nbt) {
        on = nbt.getBooleanOr(NBT, true);
    }

    /**
     * A right-click on the machine: sneaking anywhere, or aimed at the lever (the model part "toggle"), switches it.
     * True if it was a switch click (the caller opens no screen then).
     */
    public boolean activated(TransparentNodeElement e, Player player, Obj3D.Obj3DPart toggle, float vx, float vy, float vz) {
        if (!player.isShiftKeyDown() && !aims(toggle, e.front, vx, vy, vz)) return false;
        on = !on;
        e.needPublish();
        var c = e.coordonate();
        if (c.world() != null) c.world().playSound(null, c.pos(), net.minecraft.sounds.SoundEvents.LEVER_CLICK,
            net.minecraft.sounds.SoundSource.BLOCKS, 0.3f, on ? 0.6f : 0.5f);
        return true;
    }

    /** Is the block-local hit (0..1) on the lever, give or take a voxel? The part's box is in model coordinates (front XN). */
    public static boolean aims(Obj3D.Obj3DPart toggle, Direction front, float vx, float vy, float vz) {
        if (toggle == null || front == null) return false;
        var bb = toggle.boundingBox();
        double[] lo = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE}, hi = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (int k = 0; k < 8; k++) {
            double[] p = {(k & 1) == 0 ? bb.min.x : bb.max.x, (k & 2) == 0 ? bb.min.y : bb.max.y, (k & 4) == 0 ? bb.min.z : bb.max.z};
            front.rotateFromXN(p);
            for (int a = 0; a < 3; a++) {
                lo[a] = Math.min(lo[a], p[a] + 0.5);
                hi[a] = Math.max(hi[a], p[a] + 0.5);
            }
        }
        double m = 1.5 / 16;
        return vx >= lo[0] - m && vx <= hi[0] + m && vy >= lo[1] - m && vy <= hi[1] + m && vz >= lo[2] - m && vz <= hi[2] + m;
    }

    /** Draw the lever tilted by tilt (1 on: up, 0 off: down; eased by the render), about its root on the box's face. */
    public static void drawToggle(Obj3D.Obj3DPart toggle, float tilt) {
        if (toggle == null) return;
        var bb = toggle.boundingBox();
        float px = (float) bb.max.x, py = (float) bb.centre().y, pz = (float) bb.centre().z;
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glTranslatef(px, py, pz);
        // (the lever points to -x: turning it about z by a negative angle lifts its end)
        mods.eln.gl.GL11.glRotatef(-SWING * (2 * tilt - 1), 0, 0, 1);
        mods.eln.gl.GL11.glTranslatef(-px, -py, -pz);
        toggle.draw();
        mods.eln.gl.GL11.glPopMatrix();
    }
}
