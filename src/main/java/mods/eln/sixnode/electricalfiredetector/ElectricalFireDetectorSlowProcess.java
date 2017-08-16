package mods.eln.sixnode.electricalfiredetector;

import mods.eln.item.electricalitem.BatteryItem;
import mods.eln.misc.Coordonate;
import mods.eln.misc.RcInterpolator;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.electricalwatch.ElectricalWatchContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.item.ItemStack;

import java.util.List;

public class ElectricalFireDetectorSlowProcess implements IProcess {

    ElectricalFireDetectorElement element;

    RcInterpolator rc;

    double t = 0;

    public ElectricalFireDetectorSlowProcess(final ElectricalFireDetectorElement element) {
        this.element = element;
        if (!element.descriptor.batteryPowered) {
            rc = new RcInterpolator(0.6f);
        }
    }

    double getBatteryLevel() {
        ItemStack batteryStack = element.getInventory().getStackInSlot(ElectricalWatchContainer.batteryId);
        BatteryItem battery = (BatteryItem) BatteryItem.getDescriptor(batteryStack);
        if (battery != null) {
            return battery.getEnergy(batteryStack) / battery.getEnergyMax(batteryStack);
        } else {
            return 0;
        }
    }

    @Override
    public void process(double time) {
        if (element.descriptor.batteryPowered) {
            ItemStack batteryStack = element.getInventory().getStackInSlot(ElectricalFireDetectorContainer.Companion.getBatteryId());
            BatteryItem battery = (BatteryItem) BatteryItem.getDescriptor(batteryStack);
            double energy;
            if (battery == null || (energy = battery.getEnergy(batteryStack)) < element.descriptor.PowerComsumption * time * 4) {
                boolean changed = element.powered;
                element.powered = false;
                if (changed) {
                    element.firePresent = false;
                    element.needPublish();
                }
                return;
            } else {
                boolean changed = !element.powered;
                element.powered = true;
                if (changed) element.needPublish();
                battery.setEnergy(batteryStack, energy - element.descriptor.PowerComsumption * time);
            }
        }

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
                            List<Block> blockList = Utils.traceRay(coord.world(), coord.x + 0.5, coord.y + 0.5, coord.z + 0.5,
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
            if (element.firePresent != fireDetected) {
                element.firePresent = fireDetected;
                element.needPublish();
            }
        }

        if (!element.descriptor.batteryPowered) {
            rc.setTarget(element.firePresent ? 1 : 0);
            rc.step((float) time);
            element.outputGateProcess.setOutputNormalized(rc.get());
        }
    }
}
