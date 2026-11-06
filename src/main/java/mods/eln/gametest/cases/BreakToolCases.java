package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.voxel.CablePart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The tool that breaks EA's blocks fast (BreakTool; Baughn: a pickaxe for machinery and pipes, shears for cables): the
 * part under the crosshair decides, also in a block that holds both a cable and a device.
 */
public final class BreakToolCases {
    private BreakToolCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new PickaxeAndShears());
    }

    static final class PickaxeAndShears implements TestCase {
        BlockPos six, heater, pipe, cable;

        public String name() {
            return "pickaxe breaks machines and pipes fast, shears cables";
        }

        public int width() {
            return 13;
        }

        public void build(TestContext ctx) {
            six = ctx.at(1, 1, 1);
            ctx.level().setBlockAndUpdate(ctx.at(0, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            ctx.placeSix(Ids.six("Low Voltage Cable"), six, Direction.YN);
            ctx.placeSix(Ids.six("NOT Chip"), six, Direction.XN);
            heater = ctx.placeTransparent(Ids.transparent("48V Electric Heater"), ctx.at(4, 1, 1)).node.coordonate.pos();
            pipe = ctx.at(7, 1, 1);
            PipeCases.lay(ctx, pipe, 1, 0, 8, 8, 16, 8, 8);
            cable = ctx.at(10, 1, 1);
            CableCases.voxel(ctx, cable, List.of(new CablePart(0, CableCases.DOWN, new int[]{0, 0, 8, 16, 0, 8})));
        }

        public int ticks() {
            return 2;
        }

        /** Progress per tick with item in hand, the player's eyes at eye, looking at target. */
        static float progress(TestContext ctx, BlockPos pos, Vec3 eye, Vec3 target, ItemStack item) {
            var level = ctx.level();
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.snapTo(eye.x, eye.y - player.getEyeHeight(), eye.z, 0, 0);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target);
            player.setItemInHand(InteractionHand.MAIN_HAND, item);
            float p = level.getBlockState(pos).getDestroyProgress(player, level, pos);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            return p;
        }

        /** [hand, shears, iron pickaxe] progress, looking at target from eye. */
        static float[] speeds(TestContext ctx, BlockPos pos, Vec3 eye, Vec3 target) {
            return new float[]{progress(ctx, pos, eye, target, ItemStack.EMPTY), progress(ctx, pos, eye, target, new ItemStack(Items.SHEARS)),
                progress(ctx, pos, eye, target, new ItemStack(Items.IRON_PICKAXE))};
        }

        /** Looking down at the middle of pos, at height y in the block. */
        static float[] fromAbove(TestContext ctx, BlockPos pos, double y) {
            Vec3 t = new Vec3(pos.getX() + 0.5, pos.getY() + y, pos.getZ() + 0.5);
            return speeds(ctx, pos, t.add(0.01, 2, 0), t);
        }

        void expect(TestContext ctx, String what, float[] s, boolean shears) {
            float sh = s[1] / s[0], pk = s[2] / s[0];
            String detail = String.format("hand %.4f/tick, shears x%.2f, iron pickaxe x%.2f", s[0], sh, pk);
            if (shears) ctx.check(what + ": shears are fast, a pickaxe is a hand", sh > 4 && Math.abs(pk - 1) < 1e-3, detail);
            else ctx.check(what + ": a pickaxe is fast, shears are a hand", pk > 5 && Math.abs(sh - 1) < 1e-3, detail);
        }

        public void measure(TestContext ctx) {
            expect(ctx, "six-node cable", fromAbove(ctx, six, 0.02), true);
            Vec3 chip = new Vec3(six.getX() + 0.03, six.getY() + 0.5, six.getZ() + 0.5);
            expect(ctx, "six-node chip beside it", speeds(ctx, six, chip.add(2, 0, 0.01), chip), false);
            expect(ctx, "transparent device", fromAbove(ctx, heater, 0.5), false);
            expect(ctx, "pipe", fromAbove(ctx, pipe, 0.5), false);
            expect(ctx, "voxel cable", fromAbove(ctx, cable, 0.02), true);
            // a very fast tool still takes two ticks (vanilla breaks a block at once, every tick, past one a tick)
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            player.setOnGround(true);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE, 100, 20));
            Vec3 t = new Vec3(pipe.getX() + 0.5, pipe.getY() + 0.5, pipe.getZ() + 0.5);
            float fast = progress(ctx, pipe, t.add(0.01, 2, 0), t, new ItemStack(Items.DIAMOND_PICKAXE));
            player.removeEffect(net.minecraft.world.effect.MobEffects.HASTE);
            player.setOnGround(false);
            ctx.check("a diamond pickaxe with Haste XXI still takes two ticks", fast == 0.5f, "progress " + fast + "/tick");
            float[] hand = fromAbove(ctx, pipe, 0.5);
            ctx.check("a bare hand breaks as before (strength 1: 1/30 a tick on the ground, 1/150 in the air)",
                Math.abs(hand[0] - 1 / 30f) < 1e-4 || Math.abs(hand[0] - 1 / 150f) < 1e-4, "hand " + hand[0]);
        }
    }
}
