package mods.eln.misc;

import mods.eln.sim.IProcess;
import net.minecraft.nbt.CompoundTag;

public class WindProcess implements IProcess, INBTTReady {

    double windHit = 5;
    double windTarget = 5;
    double windVariation = 0;
    double windTargetNoose = 0;
    RcInterpolator windTargetFiltred = new RcInterpolator(60);

    @Override
    public void process(double time) {
        double varF = 0.01;
        windHit += windVariation * time;
        windVariation += (getTarget() - windHit) * varF * time + (Math.random() * 2 - 1) * 0.1 * time;
        windVariation *= (1 - 0.01 * time);

        if (Math.random() < time / 1200) {
            newWindTarget();
        }

        if (Math.random() < time / 120) {
            windTargetNoose = (Math.random() * 2 - 1) * 1.2;
        }

        //windLast = windHit;

        //weather.setTarget(Utils.getWeather(world))
        windTargetFiltred.setTarget((float) windTarget);
        windTargetFiltred.step((float) time);

        //Utils.println("WIND : " + windHit + "  TARGET : " + getTarget());
    }

    public void newWindTarget() {
        float next = (float) (Math.pow(Math.random(), 3.0) * 20);
        windTarget += (next - windTarget) * 0.7;
    }

    public double getTarget() {
        return windTargetNoose + windTargetFiltred.get();
    }

    public double getTargetNotFiltred() {
        return windTargetNoose + windTargetFiltred.getTarget();
    }

    public double getWind(int y) {
        y = Math.min(y, 100);
        return Math.max(0, windHit * y / 100);
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        windHit = nbt.getDoubleOr(str + "windHit", 0.0);
        windTarget = nbt.getDoubleOr(str + "windTarget", 0.0);
        windVariation = nbt.getDoubleOr(str + "windVariation", 0.0);
        windTargetFiltred.setValue(nbt.getFloatOr(str + "windTargetFiltred", 0f));
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + "windHit", windHit);
        nbt.putDouble(str + "windTarget", windTarget);
        nbt.putDouble(str + "windVariation", windVariation);
        nbt.putFloat(str + "windTargetFiltred", windTargetFiltred.get());
    }
}
