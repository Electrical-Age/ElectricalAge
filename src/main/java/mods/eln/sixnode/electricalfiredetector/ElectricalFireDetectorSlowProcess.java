package mods.eln.sixnode.electricalfiredetector;

import mods.eln.misc.Coordonate;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;

import java.util.ArrayList;

public class ElectricalFireDetectorSlowProcess implements IProcess {

    ElectricalFireDetectorElement element;

    boolean firePresent;
    RcInterpolator rc = new RcInterpolator(0.6f);

    double t = 0;

    public ElectricalFireDetectorSlowProcess(ElectricalFireDetectorElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        t += time;

        if (t >= element.descriptor.updateInterval) {
            t = 0;
            boolean fireDetected = false;

            int maxRangeHalf = ((int) element.descriptor.maxRange - 1) / 2;
            Coordonate detectionBBCenter = new Coordonate();
            detectionBBCenter.copyFrom(element.getCoordonate());
            switch (element.side) {
                case XP:
                    detectionBBCenter.x -= maxRangeHalf;
                    break;

                case XN:
                    detectionBBCenter.x += maxRangeHalf;
                    break;

                case YP:
                    detectionBBCenter.y -= maxRangeHalf;
                    break;

                case YN:
                    detectionBBCenter.y += maxRangeHalf;
                    break;

                case ZP:
                    detectionBBCenter.z -= maxRangeHalf;
                    break;

                case ZN:
                    detectionBBCenter.z += maxRangeHalf;
                    break;
            }

            for (int dx = -maxRangeHalf; dx <= maxRangeHalf; ++dx)
                for (int dy = -maxRangeHalf; dy <= maxRangeHalf; ++dy)
                    for (int dz = -maxRangeHalf; dz <= maxRangeHalf; ++dz) {
                        Block block = detectionBBCenter.world().getBlock(detectionBBCenter.x + dx, detectionBBCenter.y + dy,
                                detectionBBCenter.z + dz);
                        if (block.getClass() == BlockFire.class) {
                            fireDetected = true;

                            Coordonate coord = element.getCoordonate();
                            ArrayList<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5,
                                    detectionBBCenter.x + dx + 0.5, detectionBBCenter.y + dy + 0.5, detectionBBCenter.z + dz + 0.5);

                            for (Block b : blockList)
                                if (b.isOpaqueCube()) {
                                    fireDetected = false;
                                    break;
                                }

                            if (fireDetected) {
                                break;
                            }
                        }
                    }
            if (firePresent != fireDetected) {
                firePresent = fireDetected;
                element.needPublish();
            }
        }

        rc.setTarget(firePresent ? 1 : 0);
        rc.step((float) time);
        element.outputGateProcess.setOutputNormalized(rc.get());
    }
}
