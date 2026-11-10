package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.ghost.GhostElement;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.transparentnode.autominer.AutoMinerContainer;
import mods.eln.transparentnode.autominer.AutoMinerElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Auto Miner at work: a drill, pipes, a chest at its drop position, and 800 V on its power load (a source on the
 * load itself: its power ports are ghost blocks). Each case puts back the world it dug into under its box.
 */
public final class AutoMinerCases {
    private AutoMinerCases() {
    }

    static final int AUTO_MINER = Wp10aCases.AUTO_MINER;

    public static void addTo(List<TestCase> cases) {
        cases.add(new BelowZeroCase());
    }

    static ItemStack drill() {
        for (int i = 0; i < 64 * 128; i++)
            if (Eln.sharedItem.getDescriptor(i) instanceof ElectricalDrillDescriptor d) return d.newItemStack();
        throw new IllegalStateException("no electrical drill");
    }

    /** A miner ready to work: drill, 64 pipes, a chest at its drop position (1 forward, 1 down), powered. */
    abstract static class Rig implements TestCase {
        AutoMinerElement miner;
        BlockPos node, chest;
        VoltageSource source;
        final Map<BlockPos, BlockState> saved = new LinkedHashMap<>();

        public int width() {
            return 9;
        }

        public int height() {
            return 7;
        }

        public int depth() {
            return 7;
        }

        void rig(TestContext ctx) {
            miner = (AutoMinerElement) ctx.placeTransparent(AUTO_MINER, ctx.at(4, 3, 3));
            node = miner.node.coordonate.pos();
            Coordonate c = new Coordonate(1, -1, 0, ctx.level());
            c.applyTransformation(miner.front, miner.node.coordonate);
            chest = c.pos();
            ctx.level().setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
            miner.getInventory().setItem(AutoMinerContainer.electricalDrillSlotId, drill());
            miner.getInventory().setItem(AutoMinerContainer.MiningPipeSlotId, ElnDeviceRegistry.miningPipeDescriptor.newItemStack(64));
            miner.getInventory().setChanged();
            ElectricalLoad load = Batch1Cases.field(miner, "inPowerLoad");
            double u = Batch1Cases.<Double>field(miner.getDescriptor(), "nominalVoltage");
            source = new VoltageSource("autominer test", load, null).setU(u);
            Eln.simulator.addElectricalComponent(source);
        }

        /** Sets a block outside the box, keeping the one there to put back. */
        void set(TestContext ctx, BlockPos p, BlockState s) {
            p = p.immutable();
            saved.putIfAbsent(p, ctx.level().getBlockState(p));
            ctx.level().setBlockAndUpdate(p, s);
        }

        Object job() {
            return Batch1Cases.field(Batch1Cases.field(miner, "slowProcess"), "job");
        }

        int pipeLength() {
            return Batch1Cases.<Integer>field(Batch1Cases.field(miner, "slowProcess"), "pipeLength");
        }

        void unpower() {
            if (source != null) Eln.simulator.removeElectricalComponent(source);
            source = null;
        }

        /** The miner removed (with its pipe), the dug blocks put back, the dropped items gone. */
        void cleanUp(TestContext ctx) {
            unpower();
            NodeBase n = node == null ? null : NodeManager.instance.getNodeFromCoordonate(new Coordonate(node, ctx.level()));
            if (n instanceof TransparentNode) ctx.level().removeBlock(node, false);
            List<Map.Entry<BlockPos, BlockState>> back = new ArrayList<>(saved.entrySet());
            java.util.Collections.reverse(back);
            for (Map.Entry<BlockPos, BlockState> e : back) ctx.level().setBlockAndUpdate(e.getKey(), e.getValue());
            saved.clear();
            if (node != null) {
                var box = new net.minecraft.world.phys.AABB(node).inflate(3, 0, 3).expandTowards(0, ctx.level().getMinY() - node.getY(), 0);
                for (ItemEntity i : ctx.level().getEntitiesOfClass(ItemEntity.class, box)) i.discard();
            }
        }
    }

    /**
     * Mining below y = 0 (26.x worlds go down to y = -64): the miner drills its pipe from its box down past y = -1 to
     * bedrock the case puts at y = -3 (the shaft under the box is cleared, 3 x 3 where its scan would mine). Every pipe
     * segment has the pipe ghost id; 1.12 gave each segment its height, and the one at y = -1 had the machine's own id
     * (-1), so breaking it broke the miner. Breaking that segment now retracts the pipe and leaves the miner. Also: the
     * mined stacks waiting for room in the chest are saved (1.12 lost them on a restart).
     */
    static final class BelowZeroCase extends Rig {
        String premise = "";

        public String id() {
            return "autominer_below_zero";
        }

        public int ticks() {
            return 700;   // (~20 pipe segments at 1 s each, 50 J at 50 W)
        }

        public void build(TestContext ctx) {
            try {
                rig(ctx);
                if (node.getY() < 3) {
                    premise = "the box is at y " + node.getY() + ": the case needs to drill down to y = -1 from above";
                    return;
                }
                // the shaft: the column under the miner, and 3 x 3 from 3 down (where its scan mines: depth / 10 + 0.1 < 2)
                for (int y = node.getY() - 1; y >= -2; y--)
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dz = -1; dz <= 1; dz++) {
                            if ((dx != 0 || dz != 0) && y > node.getY() - 3) continue;
                            BlockPos p = new BlockPos(node.getX() + dx, y, node.getZ() + dz);
                            if (p.equals(chest)) continue;
                            set(ctx, p, Blocks.AIR.defaultBlockState());
                        }
                set(ctx, new BlockPos(node.getX(), -3, node.getZ()), Blocks.BEDROCK.defaultBlockState());
            } catch (RuntimeException e) {
                cleanUp(ctx);
                throw e;
            }
        }

        public void measure(TestContext ctx) {
            try {
                if (!premise.isEmpty()) {
                    ctx.check("autominer below zero: premise", false, premise);
                    return;
                }
                int x = node.getX(), z = node.getZ();
                GhostElement atMinus1 = Eln.ghostManager.getGhost(new Coordonate(x, -1, z, ctx.level()));
                ctx.check("autominer below zero: drilled past y = -1 to the bedrock at y = -3", atMinus1 != null && "done".equals(String.valueOf(job())),
                    "pipe length " + pipeLength() + " from y " + node.getY() + ", job " + job() + ", segment at y -1 " + atMinus1);
                if (atMinus1 == null) return;
                int wrong = 0, segments = 0;
                for (int y = node.getY() - 1; y >= -2; y--) {
                    GhostElement g = Eln.ghostManager.getGhost(new Coordonate(x, y, z, ctx.level()));
                    if (g == null) continue;
                    segments++;
                    if (g.getUUID() == miner.getDescriptor().getGhostGroupUuid()) wrong++;
                }
                ctx.check("autominer below zero: no pipe segment has the machine's ghost id", wrong == 0, wrong + " of " + segments);

                // a queued stack survives a save and load
                unpower();
                Batch1Cases.<List<ItemStack>>field(Batch1Cases.field(miner, "slowProcess"), "itemsToDrop").add(new ItemStack(Items.DIAMOND, 3));
                mods.eln.gametest.Reload r = mods.eln.gametest.Reload.node(ctx.level(), node);
                miner = (AutoMinerElement) ((TransparentNode) r.fresh).element;
                List<ItemStack> queued = Batch1Cases.field(Batch1Cases.field(miner, "slowProcess"), "itemsToDrop");
                ctx.check("autominer: the stacks waiting for room in the chest are saved", queued.size() == 1 && queued.get(0).is(Items.DIAMOND)
                    && queued.get(0).getCount() == 3, String.valueOf(queued));

                // breaking the segment at y = -1: the pipe comes up, the miner stays
                ctx.level().removeBlock(new BlockPos(x, -1, z), false);
                NodeBase n = NodeManager.instance.getNodeFromCoordonate(new Coordonate(node, ctx.level()));
                int left = 0;
                for (int y = node.getY() - 1; y >= -2; y--)
                    if (Eln.ghostManager.getGhost(new Coordonate(x, y, z, ctx.level())) != null) left++;
                ctx.check("autominer below zero: breaking the pipe at y = -1 leaves the miner and takes the pipe up",
                    n instanceof TransparentNode t && t.element instanceof AutoMinerElement && left == 0,
                    "node " + n + ", " + left + " pipe ghost(s) left");
            } catch (ReflectiveOperationException e) {
                ctx.check("autominer below zero: reload", false, e.toString());
            } finally {
                cleanUp(ctx);
            }
        }
    }
}
