package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.voxel.AdapterPart;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.PipeKitItem;
import mods.eln.node.voxel.PipePart;
import mods.eln.node.voxel.PipeSensorPart;
import mods.eln.node.voxel.ValvePart;
import mods.eln.node.voxel.VoxelNode;
import mods.eln.node.voxel.VoxelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * A gallery of voxel parts for a client's look (board #44: AdventurAgent checks the checked-model fixes), on a smooth
 * stone pad, one part per block with a block between: per pipe tier a straight run, a safety valve, each sensor kind,
 * each valve mode, an adapter on the block's east face and a capped end; the large pipe's S (two bends 6 voxels
 * apart); per cable tier a bend, a tee and a cross on the floor. /elnlab voxelgallery places it; the GameTest places it
 * and checks every part went in.
 */
public final class VoxelGallery implements TestCase {
    /** What was placed where (offsets from the origin) and what failed. */
    public record Result(List<String> legend, List<String> errors) {
    }

    public static Result place(TestContext ctx) {
        List<String> legend = new ArrayList<>(), errors = new ArrayList<>();
        for (int x = 0; x <= 20; x++)
            for (int z = 0; z <= 22; z++) ctx.level().setBlockAndUpdate(ctx.at(x, 0, z), Blocks.SMOOTH_STONE.defaultBlockState());
        int[] straight = {0, 8, 8, 16, 8, 8};
        for (int tier = 0; tier < PipePart.TIER_NAME.length; tier++) {
            int z = 1 + 2 * tier, x = 1;
            String t = PipePart.TIER_NAME[tier];
            put(ctx, legend, errors, x, z, t + " pipe, straight", List.of(new PipePart(tier, straight)), null);
            put(ctx, legend, errors, x += 2, z, t + " pipe, safety valve", List.of(new PipePart(tier, straight)), n -> n.addSafetyValveNear(0.5, 0.5, 0.5));
            for (int s : new int[]{PipeSensorPart.PRESSURE, PipeSensorPart.TEMPERATURE, PipeSensorPart.QUALITY}) {
                String kind = s == PipeSensorPart.PRESSURE ? "pressure" : s == PipeSensorPart.TEMPERATURE ? "temperature" : "steam quality";
                put(ctx, legend, errors, x += 2, z, t + " pipe, " + kind + " sensor", List.of(new PipePart(tier, straight)), n -> n.addSensorNear(0.5, 0.5, 0.5, s));
            }
            for (int mode : new int[]{ValvePart.ON_OFF, ValvePart.CONTROL, ValvePart.THROTTLE, ValvePart.PRESSURE}) {
                String kind = mode == ValvePart.ON_OFF ? "on/off" : mode == ValvePart.CONTROL ? "control" : mode == ValvePart.THROTTLE ? "throttle" : "pressure";
                put(ctx, legend, errors, x += 2, z, t + " pipe, " + kind + " valve", List.of(new PipePart(tier, straight)), n -> n.addValve(8, 8, 8, mode));
            }
            put(ctx, legend, errors, x += 2, z, t + " pipe, adapter on the east face", List.of(new PipePart(tier, AdapterPart.stub(5))),
                n -> n.addPart(new AdapterPart(5)));
            put(ctx, legend, errors, x += 2, z, t + " pipe, capped end (west face to the middle)", List.of(new PipePart(tier, new int[]{0, 8, 8, 8, 8, 8})), null);
        }
        put(ctx, legend, errors, 1, 7, "large alloy pipe, two bends 6 voxels apart (an S)", List.of(new PipePart(2, new int[]{0, 8, 8, 6, 8, 8, 6, 12, 8, 16, 12, 8})), null);
        for (int tier = 0; tier < CablePart.TIERS.length; tier++) {
            int z = 9 + 2 * tier;
            String t = CablePart.TIERS[tier].name();
            // on the floor (face 0): north 2, south 3, west 4, east 5
            put(ctx, legend, errors, 1, z, t + ", bend", new ArrayList<>(CablePart.arms(tier, 0, 2, 4)), null);
            put(ctx, legend, errors, 3, z, t + ", tee", new ArrayList<>(CablePart.arms(tier, 0, 2, 3, 4)), null);
            put(ctx, legend, errors, 5, z, t + ", cross", new ArrayList<>(CablePart.arms(tier, 0, 2, 3, 4, 5)), null);
        }
        return new Result(legend, errors);
    }

    interface Then {
        String on(VoxelNode n);
    }

    static void put(TestContext ctx, List<String> legend, List<String> errors, int x, int z, String what, List<? extends VoxelPart> parts, Then then) {
        BlockPos p = ctx.at(x, 1, z);
        String err = null;
        for (VoxelPart part : parts) {
            err = PipeKitItem.place(ctx.level(), p, part, null);
            if (err != null) break;
        }
        if (err == null && then != null) {
            VoxelNode n = ctx.level().getBlockEntity(p) instanceof mods.eln.node.voxel.VoxelNodeEntity e && e.getNode() instanceof VoxelNode v ? v : null;
            err = n == null ? "no voxel node" : then.on(n);
        }
        legend.add(String.format("+%d +1 +%d: %s", x, z, what));
        if (err != null) errors.add(what + " at +" + x + " +1 +" + z + ": " + err);
    }

    // --- the GameTest ---
    Result placed;

    public String id() {
        return "voxel_gallery_places";
    }

    public int width() {
        return 24;
    }

    public int height() {
        return 8;
    }

    public int depth() {
        return 24;
    }

    public int ticks() {
        return 5;
    }

    public void build(TestContext ctx) {
        placed = place(ctx);
    }

    public void measure(TestContext ctx) {
        ctx.check("voxel gallery: every part placed (" + placed.legend().size() + ")", placed.errors().isEmpty(), String.join("; ", placed.errors()));
    }
}
