package mods.eln.sixnode.electricalweathersensor;

import mods.eln.misc.Coordinate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.RcInterpolator;
import mods.eln.sim.IProcess;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class ElectricalWeatherSensorSlowProcess implements IProcess, INBTTReady {

    ElectricalWeatherSensorElement element;

    double timeCounter = 0;
    static final double refreshPeriode = 0.2;
    RcInterpolator rc = new RcInterpolator(3f);
    final float premonitionTime = 120;

    public ElectricalWeatherSensorSlowProcess(ElectricalWeatherSensorElement element) {
        this.element = element;
    }

    @Override
    public void process(double time) {
        timeCounter += time;

        if (timeCounter > refreshPeriode) {
            timeCounter -= refreshPeriode;
            Coordinate coord = element.sixNode.coordinate;

            float target = 0f;

            if (coord.doesWorldExist()) {
                World world = coord.world();

                if (world.isRaining()) {
                    //float f = Math.max(0f, (float)((premonitionTime - rain * time) / premonitionTime));
                    target = 0.5f;
                }
                if (world.isThundering()) {
                    target = 1.0f;
                }
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

            rc.step((float) time);
            element.outputGateProcess.setOutputNormalized(rc.get());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        rc.setValue(nbt.getFloat(str + "rc"));

    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setFloat(str + "rc", rc.get());
        return nbt;
    }
}
