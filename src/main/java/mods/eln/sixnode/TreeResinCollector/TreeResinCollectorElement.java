package mods.eln.sixnode.TreeResinCollector;



import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.io.DataOutputStream;
import java.io.IOException;

public class TreeResinCollectorElement extends SixNodeElement {

    TreeResinCollectorSlowProcess slowProcess = new TreeResinCollectorSlowProcess(this);

    final float occupancyMax = 2f;
    final float occupancyProductPerSecondPerTreeBlock = 3f / 5f / (60f * 24f); // 3 par jour, pour 5 tronc de haut

    double timeFromLastActivated = 0;

    public TreeResinCollectorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        slowProcessList.add(slowProcess);
        slowProcessList.add(new NodePeriodicPublishProcess(sixNode, 10f, 10f));
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString() {
        return null;
    }

    /** For the probe (Jade): what's in the pot, and how fast it fills (or that it isn't on a tree). */
    @Override
    public java.util.Map<String, String> getWaila() {
        var out = new java.util.LinkedHashMap<String, String>();
        double rate = getProductPerSecond();
        out.put("Resin in pot", String.format("%.2f / %d", Math.min(occupancyMax, getProduct(rate)), (int) occupancyMax));
        out.put("Rate", rate < 1e-6 ? "none: not on a tree (needs leaves round its logs)" : String.format("%.1f per hour", rate * 3600));
        return out;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
    }

    double getProductPerSecond() {
        Coordonate coord = sixNode.coordonate;
        Level world = coord.world();
        int[] posWood = new int[3];
        int[] posCollector = new int[3];
        Direction woodDirection = side;
        posWood[0] = coord.x;
        posWood[1] = coord.y;
        posWood[2] = coord.z;
        posCollector[0] = coord.x;
        posCollector[1] = coord.y;
        posCollector[2] = coord.z;
        woodDirection.applyTo(posWood, 1);
        int yStart, yEnd;

        while (TreeResinCollectorDescriptor.isWood(WorldCompat.getBlock(world, posWood[0], posWood[1] - 1, posWood[2]))) {
            posWood[1]--;
        }
        yStart = posWood[1];

        posWood[1] = coord.y;
        while (TreeResinCollectorDescriptor.isWood(WorldCompat.getBlock(world, posWood[0], posWood[1] + 1, posWood[2]))) {
            posWood[1]++;
        }
        yEnd = posWood[1];
        // a genuine tree (Baughn: "anything that appears to be Genuine Tree"): leaves anywhere round the logs joined to
        // this one. 1.7 wanted a leaf in the collector's own column beside the trunk, which today's trees often lack on
        // some sides: of several collectors round one tree, only some worked
        int leafCount = treeLeaves(world, new net.minecraft.core.BlockPos(posWood[0], coord.y, posWood[2]));

        int collectiorCount = 0;
        Coordonate coordTemp = new Coordonate(posCollector[0], 0, posCollector[2], world);
        posCollector[1] = yStart;
        for (posCollector[1] = yStart; posCollector[1] <= yEnd; posCollector[1]++) {
            coordTemp.y = posCollector[1];
            // if(world.getBlockId(posCollector[0],posCollector[1]+1,posCollector[2]) == Eln.treeResinCollectorBlock.blockID)
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordTemp);
            if (node instanceof SixNode) {
                SixNode six = (SixNode) node;
                if (six.getElement(side) != null && six.getElement(side) instanceof TreeResinCollectorElement) {
                    collectiorCount++;
                }
            }
        }
        if (collectiorCount == 0) {
            collectiorCount++;
            Utils.println("ASSERT collectiorCount == 0");
        }
        double leaf = leafCount >= TREE_LEAVES ? 1 : 0.000000001;
        double productPerSeconde = Math.min(0.05, occupancyProductPerSecondPerTreeBlock * (yEnd - yStart + 1) / collectiorCount) * leaf;
        return productPerSeconde;
    }

    /** A tree has at least this many leaves within 2 blocks (straight out) of its logs. */
    static final int TREE_LEAVES = 4;
    /** At most this many joined logs are looked at (a big jungle tree has ~100-200). */
    static final int MAX_LOGS = 256;

    /** Leaves within 2 blocks, along the axes, of the logs joined (also diagonally: branches) to {@code start}. */
    static int treeLeaves(Level world, net.minecraft.core.BlockPos start) {
        java.util.Set<net.minecraft.core.BlockPos> logs = new java.util.HashSet<>(), leaves = new java.util.HashSet<>();
        java.util.ArrayDeque<net.minecraft.core.BlockPos> todo = new java.util.ArrayDeque<>();
        if (!world.getBlockState(start).is(net.minecraft.tags.BlockTags.LOGS)) return 0;
        logs.add(start);
        todo.add(start);
        while (!todo.isEmpty() && leaves.size() < TREE_LEAVES) {
            var p = todo.poll();
            for (var d : net.minecraft.core.Direction.values())
                for (int k = 1; k <= 2; k++) {
                    var q = p.relative(d, k);
                    var st = world.getBlockState(q);
                    if (st.is(net.minecraft.tags.BlockTags.LEAVES)) leaves.add(q);
                    else if (!st.isAir()) break;
                }
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        var q = p.offset(dx, dy, dz);
                        if (logs.size() >= MAX_LOGS || logs.contains(q) || !world.getBlockState(q).is(net.minecraft.tags.BlockTags.LOGS)) continue;
                        logs.add(q);
                        todo.add(q);
                    }
        }
        return leaves.size();
    }

    double getProduct(double productPerSecond) {
        double product = productPerSecond * timeFromLastActivated;
        return product;
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side,
                                    float vx, float vy, float vz) {
        double productPerSeconde = getProductPerSecond();
        double product = getProduct(productPerSeconde);

        int productI;
        if (product > occupancyMax) {
            productI = (int) occupancyMax;
            timeFromLastActivated = 0;
        } else {
            productI = (int) product;
            timeFromLastActivated -= (productI) / productPerSeconde;
        }

        for (int idx = 0; idx < productI; idx++) {
            if (ElnDeviceRegistry.treeResin != null) sixNode.dropItem(ElnDeviceRegistry.treeResin.newItemStack(1));
        }

        Utils.sendMessage(entityPlayer, "Tree Resin in pot : " + String.format("%1.2f", productPerSeconde * timeFromLastActivated));
        needPublish();
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        timeFromLastActivated = nbt.getDoubleOr("timeFromLastActivated", 0.0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putDouble("timeFromLastActivated", timeFromLastActivated);
    }

    class TreeResinCollectorSlowProcess implements IProcess {
        TreeResinCollectorElement element;

        public TreeResinCollectorSlowProcess(TreeResinCollectorElement element) {
            this.element = element;
        }

        @Override
        public void process(double time) {
            element.timeFromLastActivated += time;
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            if (getCoordonate().getBlockExist())
                stream.writeFloat((float) getProduct(getProductPerSecond()));
            else
                stream.writeFloat(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
