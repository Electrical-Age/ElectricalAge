package mods.eln.transparentnode.electricalantennatx;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.sim.IProcess;
import mods.eln.transparentnode.electricalantennarx.ElectricalAntennaRxElement;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
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
        //if(element.rxCoord == null)
        World world = element.node.coordonate.world();

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
                if (element.placeBoot || element.rxCoord == null || coord.world().blockExists(coord.x, coord.y, coord.z)) {
                    //	a++;
                    if ((block = coord.getBlock()) != Blocks.air && block != Blocks.fire) {
                        if (block == Eln.transparentNodeBlock
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
                    if (coordCpy.world().blockExists(coordCpy.x, coordCpy.y, coordCpy.z)) {
                        if (coordCpy.getBlock() == Blocks.air) {
                            coordCpy.world().setBlock(coordCpy.x, coordCpy.y, coordCpy.z, Blocks.fire);
                        }
                    }
                }
            } else {
                element.powerEfficency = 1 - (element.descriptor.electricalPowerRatioLostOffset + element.descriptor.electricalPowerRatioLostPerBlock * distance);

                if (world.getWorldInfo().isRaining()) element.powerEfficency *= 0.707;
                if (world.getWorldInfo().isThundering()) element.powerEfficency *= 0.707;

                element.rxCoord = node.coordonate;
                element.rxElement = (ElectricalAntennaRxElement) node.element;
            }
            List list = world.getEntitiesWithinAABBExcludingEntity((Entity) null, Coordonate.getAxisAlignedBB(element.node.coordonate, coord));

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
