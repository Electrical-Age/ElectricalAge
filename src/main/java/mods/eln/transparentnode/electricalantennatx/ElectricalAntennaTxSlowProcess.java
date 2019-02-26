package mods.eln.transparentnode.electricalantennatx;

import mods.eln.Eln;
import mods.eln.misc.Coordinate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.IProcess;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxElement;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class ElectricalAntennaTxSlowProcess implements IProcess {

    ElectricalAntennaTxElement element;

    double timeCounter = 0;
    final double periode = 2;

    public ElectricalAntennaTxSlowProcess(ElectricalAntennaTxElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        World world = element.node.coordinate.world();

        if (timeCounter <= 0.0) {
            timeCounter = periode;
            int rangeMax = element.descriptor.rangeMax;
            Coordinate coord = new Coordinate(element.node.coordinate);

            int distance = 0;
            TransparentNode node = null;
            boolean find = false;
            //int a = 0,b = 0;
            do {
                coord.move(element.front);
                distance++;
                Block block;
                block = coord.world().getBlockState(coord.pos).getBlock();
                if ((coord.world().isAirBlock(coord.pos) && block != Blocks.FIRE)) {
                    if (block == Eln.transparentNodeBlock
                        && (node = (TransparentNode) NodeManager.instance.getNodeFromCoordinate(coord)) != null
                        && (node.element instanceof ElectricalAntennaRxElement)) {
                        ElectricalAntennaRxElement rx = (ElectricalAntennaRxElement) node.element;
                        if (rx.front == element.front.getInverse()) {
                            find = true;
                        }
                    }
                    break;
                }
            } while (distance < rangeMax);
            if (!find) {
                element.txDisconnect();
                Coordinate coordCpy = new Coordinate(coord);
                coordCpy.move(element.front.getInverse());
                BlockPos pos = coordCpy.pos;
                if (element.powerResistor.getP() > 50 && coordCpy.world().isAirBlock(pos)) {
                    coordCpy.world().setBlockState(pos, Blocks.FIRE.getDefaultState());
                }
            } else {
                element.powerEfficency = 1 - (element.descriptor.electricalPowerRatioLostOffset + element.descriptor.electricalPowerRatioLostPerBlock * distance);

                if (world.getWorldInfo().isRaining()) element.powerEfficency *= 0.707;
                if (world.getWorldInfo().isThundering()) element.powerEfficency *= 0.707;

                element.rxCoord = node.coordinate;
                element.rxElement = (ElectricalAntennaRxElement) node.element;
            }
            List list = world.getEntitiesWithinAABBExcludingEntity((Entity) null, Coordinate.getAxisAlignedBB(element.node.coordinate, coord));

            for (Object o : list) {
                Entity e = (Entity) o;
                e.setFire((int) (Math.pow(element.powerResistor.getP() / 100.0, 2) + 0.5));
            }
        }

        if (element.powerResistor.getP() > element.descriptor.electricalMaximalPower) {
            element.node.physicalSelfDestruction(2.0f);
        }
        if (element.powerIn.getU() > element.descriptor.electricalMaximalVoltage) {
            element.node.physicalSelfDestruction(2.0f);
        }

        element.placeBoot = false;
        timeCounter -= time;
    }
}
