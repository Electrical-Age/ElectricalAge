package mods.eln.sixnode.electricallightsensor;


import mods.eln.compat.WorldCompat;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;

public class ElectricalLightSensorSlowProcess implements IProcess {

    ElectricalLightSensorElement element;

    int light = 0;
    double timeCounter = 0;
    static final double refreshPeriode = 0.2;

    public ElectricalLightSensorSlowProcess(ElectricalLightSensorElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        timeCounter += time;

        if (timeCounter > refreshPeriode) {
            timeCounter -= refreshPeriode;

            if (!element.sixNode.coordonate.getBlockExist()) return;
            Coordonate coord = element.sixNode.coordonate;
            //int light = WorldCompat.getSavedLightValue(coord.world(), net.minecraft.world.level.LightLayer.SKY, coord.x, coord.y, coord.z) - coord.world().skylightSubtracted;
            //	Utils.println("Light : " + light);
            Level world = coord.world();
            //if(element.descriptor.dayLightOnly) {
            if (world.dimensionType().hasSkyLight()) { // 1.7.10 !hasNoSky
                int i1 = WorldCompat.getSavedLightValue(world, net.minecraft.world.level.LightLayer.SKY, coord.x, coord.y, coord.z) - world.getSkyDarken();
                i1 = Math.max(0, i1);
                float f = (float) Utils.sunAngleRadians(world);

                if (f < (float) Math.PI) {
                    f += (0.0F - f) * 0.2F;
                } else {
                    f += (((float) Math.PI * 2F) - f) * 0.2F;
                }

                i1 = Math.round((float) i1 * Mth.cos(f));

                if (i1 < 0) {
                    i1 = 0;
                }

                if (i1 > 15) {
                    i1 = 15;
                }

                light = i1;
            }
            //}
            if (!element.descriptor.dayLightOnly) {
                // light = Math.max(light, (int)(WorldCompat.getBlockLightValue(world, coord.x, coord.y, coord.z)));
                //light = 0;
                light = Math.max(light, Utils.getLight(world, net.minecraft.world.level.LightLayer.BLOCK, coord.x, coord.y, coord.z));
            }
            element.outputGateProcess.setOutputNormalized(light / 15.0);
        }
    }
}
