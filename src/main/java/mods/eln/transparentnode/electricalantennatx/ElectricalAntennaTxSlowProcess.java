package mods.eln.transparentnode.electricalantennatx;


import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.IProcess;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.FailureCause;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxElement;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;

import java.util.List;

public class ElectricalAntennaTxSlowProcess implements IProcess {

    private final Failure failure;

    ElectricalAntennaTxElement element;

    double timeCounter = 0;
    final double periode = 2;

    public ElectricalAntennaTxSlowProcess(ElectricalAntennaTxElement element) {
        this.element = element;
        this.failure = new Failure(element).electronics();
    }

    @Override
    public void process(double time) {
        //if(element.rxCoord == null)
        Level world = element.node.coordonate.world();

        if (timeCounter <= 0.0) {
            timeCounter = periode;
            int rangeMax = element.descriptor.rangeMax;
            Coordonate coord = new Coordonate(element.node.coordonate);

            int distance = 0;
            TransparentNode node = null;
            boolean find = false;
            //int a = 0,b = 0;
            do {
                coord.move(element.front);
                distance++;
                Block block;
                if (element.placeBoot || element.rxCoord == null || WorldCompat.blockExists(coord.world(), coord.x, coord.y, coord.z)) {
                    //	a++;
                    if (!mods.eln.misc.Utils.isAirBlock(block = coord.getBlock()) && block != Blocks.FIRE) {
                        if (block instanceof mods.eln.node.transparent.TransparentNodeBlock // 1.12: the one transparent-node block
                            && (node = (TransparentNode) NodeManager.instance.getNodeFromCoordonate(coord)) != null
                            && (node.element instanceof ElectricalAntennaRxElement)) {
                            ElectricalAntennaRxElement rx = (ElectricalAntennaRxElement) node.element;
                            if (rx.front == element.front.getInverse()) {
                                find = true;
                            }
                        }
                        break;
                    }
                } else {
                    //	b++;
                    NodeBase unknowNode = NodeManager.instance.getNodeFromCoordonate(coord);
                    if (node != null) {
                        if (unknowNode instanceof TransparentNode
                            && (((TransparentNode) unknowNode).element instanceof ElectricalAntennaRxElement)) {
                            node = (TransparentNode) unknowNode;
                            ElectricalAntennaRxElement rx = (ElectricalAntennaRxElement) node.element;
                            if (rx.front == element.front.getInverse()) {
                                find = true;
                            }
                        }
                        break;
                    }
                }
            } while (distance < rangeMax);
            if (!find) {
                element.txDisconnect();
                Coordonate coordCpy = new Coordonate(coord);
                coordCpy.move(element.front.getInverse());
                if (element.powerResistor.getP() > 50) {
                    if (WorldCompat.blockExists(coordCpy.world(), coordCpy.x, coordCpy.y, coordCpy.z)) {
                        if (mods.eln.misc.Utils.isAirBlock(coordCpy.getBlock())) {
                            WorldCompat.setBlock(coordCpy.world(), coordCpy.x, coordCpy.y, coordCpy.z, Blocks.FIRE);
                        }
                    }
                }
            } else {
                element.powerEfficency = 1 - (element.descriptor.electricalPowerRatioLostOffset + element.descriptor.electricalPowerRatioLostPerBlock * distance);

                // (rain and thunder each cost 30 %, graded, at the transmitter)
                double[] weather = mods.eln.misc.Utils.weatherAt(world, element.node.coordonate.pos());
                element.powerEfficency *= Math.pow(0.707, weather[0]) * Math.pow(0.707, weather[1]);

                element.rxCoord = node.coordonate;
                element.rxElement = (ElectricalAntennaRxElement) node.element;
            }
            List list = world.getEntities((Entity) null, Coordonate.getAxisAlignedBB(element.node.coordonate, coord));

            for (Object o : list) {
                Entity e = (Entity) o;
                e.igniteForSeconds((int) (Math.pow(element.powerResistor.getP() / 100.0, 2) + 0.5));
            }
        }

        if (element.powerResistor.getP() > element.descriptor.electricalMaximalPower) {
            failure.fail(FailureCause.OVERHEAT, element.powerResistor.getP());
        }
        if (element.powerIn.getU() > element.descriptor.electricalMaximalVoltage) {
            failure.fail(FailureCause.OVERVOLTAGE, element.powerIn.getU());
        }

        element.placeBoot = false;
        timeCounter -= time;
    }
}
