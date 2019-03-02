package mods.eln.sixnode.TreeResinCollector;

import mods.eln.Eln;
import mods.eln.init.Items;
import mods.eln.misc.Coordinate;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
    }

    double getProductPerSecond() {
        Coordinate coord = sixNode.coordinate;
        World world = coord.world();
        int[] posWood = new int[3];
        int[] posCollector = new int[3];
        Direction woodDirection = side;
        posWood = Utils.posToArray(coord.pos);
        //Tried not using the function again (since the values are the same)
        posCollector = posWood;
        woodDirection.applyTo(posWood, 1);
        int leafCount = 0;
        int yStart, yEnd;

        while (TreeResinCollectorDescriptor.isWood(world.getBlockState(new BlockPos(posWood[0], posWood[1] - 1, posWood[2])).getBlock())) {
            posWood[1]--;
        }
        yStart = posWood[1];

        posWood[1] = coord.pos.getY();
        // timeCounter-= timeTarget;
        while (TreeResinCollectorDescriptor.isWood(world.getBlockState(new BlockPos(posWood[0], posWood[1] + 1, posWood[2])).getBlock())) {
            if (TreeResinCollectorDescriptor.isLeaf(world.getBlockState(new BlockPos(posCollector[0], posWood[1] + 1, posCollector[2])).getBlock()))
                leafCount++;
            posWood[1]++;
        }
        yEnd = posWood[1];

        int collectorCount = 0;
        Coordinate coordTemp = new Coordinate(posCollector[0], 0, posCollector[2], world);
        posCollector[1] = yStart;
        for (posCollector[1] = yStart; posCollector[1] <= yEnd; posCollector[1]++) {
            coordTemp.pos.setY(posCollector[1]);
            // if(world.getBlockId(posCollector[0],posCollector[1]+1,posCollector[2]) == Eln.treeResinCollectorBlock.blockID)
            NodeBase node = NodeManager.instance.getNodeFromCoordinate(coordTemp);
            if (node instanceof SixNode) {
                SixNode six = (SixNode) node;
                if (six.getElement(side) != null && six.getElement(side) instanceof TreeResinCollectorElement) {
                    collectorCount++;
                }
            }
        }
        if (collectorCount == 0) {
            collectorCount++;
            Utils.println("ASSERT collectorCount == 0");
        }
        double leaf = leafCount >= 1 ? 1 : 0.000000001;
        double productPerSeconde = Math.min(0.05, occupancyProductPerSecondPerTreeBlock * (yEnd - yStart + 1) / collectorCount) * leaf;
        return productPerSeconde;
    }

    double getProduct(double productPerSecond) {
        double product = productPerSecond * timeFromLastActivated;
        return product;
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
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
            sixNode.dropItem(Items.treeResin.newItemStack());
        }

        Utils.sendMessage(entityPlayer, "Tree Resin in pot : " + String.format("%1.2f", productPerSeconde * timeFromLastActivated));
        needPublish();
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        timeFromLastActivated = nbt.getDouble("timeFromLastActivated");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("timeFromLastActivated", timeFromLastActivated);
        return nbt;
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
            if (getCoordinate().doesBlockExist())
                stream.writeFloat((float) getProduct(getProductPerSecond()));
            else
                stream.writeFloat(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
