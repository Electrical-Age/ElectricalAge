package mods.eln.gametest.cases;

import mods.eln.gametest.LabCommands;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.List;

/**
 * A cable kit used on an Auto Miner's power port, which is one of its ghost blocks (Baughn: the click opened its
 * screen, no cable): the ghost passes the click to the kit, while an empty hand still opens the miner. AdventurAgent
 * laid LV cable to both ports in a client (miner-lab1).
 */
public final class MinerCableCases {
    private MinerCableCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Kit());
    }

    static final class Kit implements TestCase {
        public String id() {
            return "autominer_ghost_port_takes_a_kit";
        }

        public int width() {
            return 9;
        }

        public int depth() {
            return 7;
        }

        public int height() {
            return 7;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 9; x++) for (int z = 0; z < 7; z++) c.floor(c.at(x, 0, z));   // (the box's floor is 3 deep)
            c.placeTransparent(LabCommands.transparentId("Auto Miner"), c.at(5, 2, 3));
        }

        /** A block of the test box holding an AutoMinerPowerNode (the miner's power port). */
        static net.minecraft.core.BlockPos findPort(TestContext c) {
            for (int x = 0; x < 9; x++) for (int y = 1; y < 7; y++) for (int z = 0; z < 7; z++) {
                var n = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level()));
                if (n instanceof mods.eln.transparentnode.autominer.AutoMinerPowerNode) return c.at(x, y, z);
            }
            return null;
        }

        public void measure(TestContext c) {
            var p = findPort(c);
            c.check("autominer: a power port on a ghost block", p != null && c.level().getBlockState(p).getBlock() instanceof mods.eln.ghost.GhostBlock,
                p == null ? "none" : c.level().getBlockState(p).toString());
            if (p == null) return;
            var player = Wp9cCases.player(c);
            var kit = new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.CABLE_KITS.get(1).get());
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p), Direction.NORTH, p, false);
            InteractionResult r = c.level().getBlockState(p).useItemOn(kit, c.level(), player, InteractionHand.MAIN_HAND, hit);
            c.check("autominer: the ghost port passes a kit's click on to the kit", r == InteractionResult.PASS, String.valueOf(r));
            mods.eln.node.transparent.TransparentNode miner = null;
            for (int x = 0; x < 9; x++) for (int y = 1; y < 7; y++) for (int z = 0; z < 7; z++)
                if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(x, y, z), c.level()))
                    instanceof mods.eln.node.transparent.TransparentNode t) miner = t;
            if (miner == null) {
                c.check("autominer: found", false, "");
                return;
            }
            var w = miner.element.getWaila();
            c.check("autominer: Jade says where its power goes, first", w.keySet().iterator().next().equals("Power") && w.get("Power").contains("corner"), w.toString());
        }
    }
}
