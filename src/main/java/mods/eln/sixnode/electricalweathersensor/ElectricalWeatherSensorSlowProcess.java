package mods.eln.sixnode.electricalweathersensor;

import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcInterpolator;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class ElectricalWeatherSensorSlowProcess implements IProcess, INBTTReady {

    ElectricalWeatherSensorElement element;

    double timeCounter = 0;
    static final double refreshPeriode = 0.2;
    // WP16b: 1.7.10 stepped rc (tau 3 s) with one tick once per refresh (0.2 s = 4 ticks), so the real tau was 12 s;
    // now stepped with the refresh period, tau retuned to 12 s (unchanged behaviour).
    RcInterpolator rc = new RcInterpolator(12f);
    final float premonitionTime = 120;

    public ElectricalWeatherSensorSlowProcess(ElectricalWeatherSensorElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        timeCounter += time;

        if (timeCounter > refreshPeriode) {
            timeCounter -= refreshPeriode;
            Coordonate coord = element.sixNode.coordonate;

            float target = 0f;

            if (coord.getWorldExist()) {
                Level world = coord.world();

                // 0.5 in rain, 1 in a thunderstorm, graded between (at the sensor: Watershed's showers are local)
                target = (float) mods.eln.misc.Utils.getWeatherNoLoad(coord);
                rc.setTarget(target);
            }

	    /*	int rain = world.getWorldInfo().getRainTime();
	    	int thunder = world.getWorldInfo().getThunderTime();
	    	
	    	if (rain < thunder) {
	    		target = target * (1 - f) + f * 0.5f;
	    	} else {
	    		target = target * (1 - f) + f * 1f;
	    	}
	    	*/
            //Utils.println(target);

            rc.step((float) refreshPeriode);
            element.outputGateProcess.setOutputNormalized(rc.get());
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        rc.setValue(nbt.getFloatOr(str + "rc", 0f));

    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putFloat(str + "rc", rc.get());
    }
}
