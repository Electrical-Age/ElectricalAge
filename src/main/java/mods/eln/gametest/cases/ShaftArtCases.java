package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** The shaft redo's art pass (Baughn 2026-10-29): hitboxes that follow each model and turn with the block's front. */
public final class ShaftArtCases {
    private ShaftArtCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new HitboxPerFacing());
    }

    static final int HP = 9 + (4 << 6), GEN4 = 10 + (4 << 6), GAS = 11 + (4 << 6), JOINT = 12 + (4 << 6), HUB = 13 + (4 << 6),
        FLYWHEEL = 14 + (4 << 6), TACHO = 15 + (4 << 6), GEN8 = 16 + (4 << 6), MOTOR = 17 + (4 << 6), LP = 18 + (4 << 6),
        PLANT_CONDENSER = 52 + (82 << 6);

    /**
     * Every shaft part's hitbox, for each of the four fronts: the model's (not the full cell), with its shaft at both
     * shaft faces (left and right of its front); the turbines' inlet on top and exhaust at the back; the plant
     * condenser's exhaust inlets on top, at its back and on its right.
     */
    static final class HitboxPerFacing implements TestCase {
        final List<String> bad = new ArrayList<>();
        int checked;

        public String id() {
            return "shaft_hitbox_per_facing";
        }

        public int ticks() {
            return 2;
        }

        static boolean has(VoxelShape s, double x, double y, double z) {
            for (var b : s.toAabbs()) if (b.contains(new Vec3(x, y, z))) return true;
            return false;
        }

        /** The point 0.47 of a block from the centre toward D, at height Y. */
        static boolean toward(VoxelShape s, Direction d, double y) {
            var f = d.toEnumFacing();
            return has(s, 0.5 + 0.47 * f.getStepX(), y, 0.5 + 0.47 * f.getStepZ());
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 1);
            int[] ids = {HP, LP, GAS, GEN4, GEN8, MOTOR, JOINT, HUB, FLYWHEEL, TACHO, PLANT_CONDENSER};
            String[] names = {"HP turbine", "LP turbine", "gas turbine", "4 kW generator", "8 kW generator", "shaft motor", "joint", "joint hub",
                "flywheel", "tachometer", "plant condenser"};
            for (int i = 0; i < ids.length; i++)
                for (Direction f : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                    var e = ctx.placeTransparent(ids[i], p);
                    if (ids[i] != HUB) {   // (the hub has no front: it faces XP)
                        e.front = f;
                        e.reconnect();
                    }
                    Direction front = e.front;
                    VoxelShape s = ctx.level().getBlockState(p).getShape(ctx.level(), p);
                    String n = names[i] + " facing " + front;
                    var b = s.isEmpty() ? null : s.bounds();
                    if (b == null || (b.minX == 0 && b.minY == 0 && b.minZ == 0 && b.maxX == 1 && b.maxY == 1 && b.maxZ == 1 && s.toAabbs().size() == 1))
                        bad.add(n + ": not the model's hitbox");
                    else if (ids[i] == PLANT_CONDENSER) {
                        if (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5) || !toward(s, front.right(), 0.5))
                            bad.add(n + ": exhaust inlets (top, back, right)");
                    } else {
                        if (!toward(s, front.left(), 0.5) || !toward(s, front.right(), 0.5)) bad.add(n + ": shaft at its left and right");
                        if (ids[i] == HUB && (!toward(s, front, 0.5) || !toward(s, front.back(), 0.5))) bad.add(n + ": shaft on all four sides");
                        if ((ids[i] == HP || ids[i] == LP || ids[i] == GAS) && (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5)))
                            bad.add(n + ": inlet on top, exhaust at its back");
                    }
                    checked++;
                    ctx.level().removeBlock(p, false);
                }
        }

        public void measure(TestContext ctx) {
            ctx.check("shaft hitboxes: each model's, turned with its front (" + checked + " placements)", bad.isEmpty() && checked == 44, String.join("; ", bad));
        }
    }
}
