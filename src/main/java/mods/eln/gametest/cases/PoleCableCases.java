package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gridnode.electricalpole.ElectricalPoleElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A cable kit to a utility pole's foot (nero 1573457386071527568): the foot's socket is the pole transformer's low
 * side, so a kit clicked on it routes there like on any machine; on the pole's ghosts above it still says spans take
 * Overhead Cable. Before, a kit's click on the foot went to the pole, which refused it as if stringing a span, so a
 * pole could not be wired to a grid by hand at all.
 */
public final class PoleCableCases {
    private PoleCableCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new FootKit());
        cases.add(new Pickup());
    }

    static final class FootKit implements TestCase {
        ElectricalPoleElement pole;
        BlockPos at;
        InteractionResult foot, ghost;
        int before;

        public String id() {
            return "pole_foot_takes_a_cable_kit";
        }

        public int width() {
            return 8;
        }

        public int height() {
            return 5;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 8; x++) for (int z = 0; z < 3; z++) c.floor(c.at(x, 0, z));
            at = c.at(1, 1, 1);
            pole = (ElectricalPoleElement) c.placeTransparent(Wp11Cases.UTILITY_POLE, at);
            Wp11Cases.PoleGearCase.fit(c, pole, mods.eln.gridnode.electricalpole.PoleTransformerItem.make(2, 8, 2), at.getX() + 3, at.getZ() + 0.5);
            before = pole.secondaryLoad.getConnectedComponents().size();
            var player = Wp9cCases.player(c);
            ItemStack kit = new ItemStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(1).get(), 64);
            player.setItemInHand(InteractionHand.MAIN_HAND, kit);
            try {
                // the start: the floor 5 blocks east of the pole
                BlockPos start = c.at(6, 0, 1);
                var s = new BlockHitResult(Vec3.atCenterOf(start).add(0, 0.5, 0), Direction.UP, start, false);
                kit.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, s));
                // the end: the pole's foot, its east face, as a player's click (the block's use first, then the item's)
                var f = new BlockHitResult(Vec3.atCenterOf(at).add(0.5, -0.2, 0), Direction.EAST, at, false);
                foot = c.level().getBlockState(at).useItemOn(kit, c.level(), player, InteractionHand.MAIN_HAND, f);
                if (foot == InteractionResult.PASS) kit.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, f));
                // a ghost above: still the pole's (it says spans take Overhead Cable)
                BlockPos up = at.above(2);
                ghost = c.level().getBlockState(up).useItemOn(kit, c.level(), player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(up).add(0.5, 0, 0), Direction.EAST, up, false));
            } finally {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }

        public void measure(TestContext c) {
            c.check("pole foot: a kit's click there goes to the kit, not the pole", foot == InteractionResult.PASS, String.valueOf(foot));
            c.check("pole ghost: a kit's click above is still the pole's", ghost != InteractionResult.PASS, String.valueOf(ghost));
            var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(at.east(), c.level()));
            c.check("pole foot: the kit laid cable up to the pole", n instanceof mods.eln.node.voxel.VoxelNode, String.valueOf(n));
            int after = pole.secondaryLoad.getConnectedComponents().size();
            c.check("pole foot: the cable joins the transformer's low side", after > before, before + " -> " + after);
        }
    }

    /** Picked-up overhead cable tops up the player's piece of the same cross-section (nero 1573459054402084968). */
    static final class Pickup implements TestCase {
        String said;

        public String id() {
            return "overhead_cable_merges_on_pickup";
        }

        public void build(TestContext c) {
            var player = Wp9cCases.player(c);
            var inv = player.getInventory();
            inv.clearContent();
            ItemStack mine = mods.eln.gridnode.OverheadCableItem.make(4, 480), other = mods.eln.gridnode.OverheadCableItem.make(3, 10);
            inv.setItem(0, mine);
            inv.setItem(1, other);
            var drop = new net.minecraft.world.entity.item.ItemEntity(c.level(), player.getX(), player.getY(), player.getZ(),
                mods.eln.gridnode.OverheadCableItem.make(4, 30));
            drop.setNoPickUpDelay();
            c.level().addFreshEntity(drop);
            mods.eln.gridnode.OverheadCableItem.onPickup(new net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre(player, drop));
            said = mods.eln.gridnode.OverheadCableItem.blocks(mine) + "," + mods.eln.gridnode.OverheadCableItem.blocks(other) + ","
                + mods.eln.gridnode.OverheadCableItem.blocks(drop.getItem()) + "," + drop.isRemoved();
            drop.discard();
            inv.clearContent();
        }

        public void measure(TestContext c) {
            c.check("overhead cable: a picked-up piece tops up the same cross-section to 500, the rest stays on the ground",
                said.equals("500,10,10,false"), said);
        }
    }
}
