package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gridnode.electricalpole.ElectricalPoleElement;
import mods.eln.misc.Direction;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxElement;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Range checks stop at the dimension (Coordonate.trueDistanceTo is infinite between two): a wireless switch in the
 * Nether at the same x, y, z as a receiver on its channel here does not drive it, and a utility pole in the Nether
 * 5 blocks from one here can't be linked to it. 1.12 compared x, y and z only. The Nether half is built by the case
 * at this box's coordinates (its chunk forced, its blocks restored and its nodes removed at the end), since the
 * harness only looks after the test's own level.
 */
public final class DimensionCases {
    private DimensionCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new ApartCase());
    }

    /** The same test context in another level, at the same absolute positions. */
    static TestContext in(TestContext ctx, ServerLevel level) {
        return new TestContext() {
            public ServerLevel level() {
                return level;
            }

            public BlockPos at(int dx, int dy, int dz) {
                return ctx.at(dx, dy, dz);
            }

            public void floor(BlockPos p) {
                level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
            }

            public void line(String text) {
                ctx.line(text);
            }

            public void check(String what, boolean ok, String detail) {
                ctx.check(what, ok, detail);
            }

            public void checkValue(String what, double measured, double expected) {
                ctx.checkValue(what, measured, expected);
            }
        };
    }

    static final class ApartCase implements TestCase {
        ServerLevel nether;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();
        final List<BlockPos> netherNodes = new ArrayList<>();
        final List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        WirelessSignalRxElement rx;
        ElectricalPoleElement here, there;
        String poleLinks = "";

        public String id() {
            return "dimensions_keep_wireless_and_poles_apart";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public int floorDepth() {
            return 10;
        }

        /** Air at p (the Nether's own block kept to restore), stone below. */
        void clear(BlockPos p, int up) {
            for (int dy = -1; dy <= up; dy++) {
                BlockPos q = p.above(dy).immutable();
                saved.putIfAbsent(q, nether.getBlockState(q));
                nether.setBlockAndUpdate(q, dy < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }

        void force(BlockPos p) {
            var c = new net.minecraft.world.level.ChunkPos(p.getX() >> 4, p.getZ() >> 4);
            if (forced.contains(c)) return;
            forced.add(c);
            nether.setChunkForced(c.x(), c.z(), true);
        }

        public void build(TestContext ctx) {
            nether = ctx.level().getServer().getLevel(Level.NETHER);
            if (nether == null) throw new IllegalStateException("no Nether on this server");
            TestContext there = in(ctx, nether);
            String ch = "eln selftest dimensions " + System.nanoTime();
            BlockPos src = ctx.at(1, 1, 1), poleHere = ctx.at(4, 1, 2), poleThere = ctx.at(4, 1, 7);
            try {
                force(src);
                force(poleThere);
                // wireless: a switch, on, in the Nether; a receiver at the same x, y, z here
                clear(src, 1);
                SixNodeElement sw = there.placeSix(Ids.six("Wireless Switch"), src);
                netherNodes.add(src);
                WirelessMoreCases.channel(there, sw, WirelessSignalSourceElement.setChannelId, ch);
                Wp9aCases.use(there, sw, ItemStack.EMPTY);
                rx = WirelessMoreCases.rx(ctx, src, ch);
                // poles: one here, one in the Nether 5 blocks away, overhead cable used on both (here first)
                clear(poleThere, 5);
                this.there = (ElectricalPoleElement) there.placeTransparent(Wp11Cases.UTILITY_POLE, poleThere);
                netherNodes.add(this.there.node.coordonate.pos());
                here = (ElectricalPoleElement) ctx.placeTransparent(Wp11Cases.UTILITY_POLE, poleHere);
                Player p = Wp9aCases.player(ctx);
                ItemStack hand = mods.eln.gridnode.OverheadCableItem.make(KitDeviceCases.thick().mm2(), 10);
                p.setItemInHand(InteractionHand.MAIN_HAND, hand);
                try {
                    here.onBlockActivated(p, Direction.XP, 0.5F, 0.5F, 0.5F);
                    this.there.onBlockActivated(p, Direction.XP, 0.5F, 0.5F, 0.5F);
                } finally {
                    p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                poleLinks = here.gridLinkList.size() + " here, " + this.there.gridLinkList.size() + " there, cable left "
                    + mods.eln.gridnode.OverheadCableItem.blocks(hand);
                // forget the Nether pole as the starting point (a later case's first click would see it)
                mods.eln.gridnode.GridElement.forget(p.getUUID());
            } catch (RuntimeException e) {
                cleanUp();
                throw e;
            }
        }

        public void measure(TestContext ctx) {
            try {
                Wp9cCases.within(ctx, "dimensions: a wireless switch in the Nether doesn't drive a receiver at the same x, y, z here [V]",
                    Wp9cCases.out(rx), 0, 0.01 * Wp9cCases.SVU);
                ctx.check("dimensions: a pole here and one in the Nether are not linked", here.gridLinkList.isEmpty() && there.gridLinkList.isEmpty(),
                    poleLinks);
            } finally {
                cleanUp();
            }
        }

        /** The Nether nodes removed, its blocks restored, its chunks released. */
        void cleanUp() {
            for (BlockPos p : netherNodes) {
                nether.removeBlock(p, false);
                NodeBase n = NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, nether));
                if (n != null) mods.eln.node.NodeOrphans.remove(n);
            }
            netherNodes.clear();
            List<Map.Entry<BlockPos, BlockState>> back = new ArrayList<>(saved.entrySet());
            java.util.Collections.reverse(back);   // (top down: nothing above loses its support while restoring)
            for (Map.Entry<BlockPos, BlockState> e : back) nether.setBlockAndUpdate(e.getKey(), e.getValue());
            saved.clear();
            for (var c : forced) nether.setChunkForced(c.x(), c.z(), false);
            forced.clear();
        }
    }
}
