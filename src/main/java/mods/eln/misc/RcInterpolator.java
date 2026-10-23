package mods.eln.misc;

import net.minecraft.nbt.CompoundTag;

public class RcInterpolator implements INBTTReady {

    float ff;
    float factor, factorFiltred;

    public RcInterpolator(float preTao) {
        ff = 1 / preTao;
        factorFiltred = 0;
        factor = 0;
    }

    public void step(float deltaT) {
        factorFiltred += (factor - factorFiltred) * ff * deltaT;
    }

	/*public void stepGraphic()
    {
		step(FrameTime.get());
	}*/

    public float get() {
        return factorFiltred;
    }

    public void setTarget(float value) {
        factor = value;
    }

    public void setValue(float value) {
        factorFiltred = value;
    }

    public void setValueFromTarget() {
        factorFiltred = factor;
    }

    public float getTarget() {
        return factor;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        factor = nbt.getFloatOr(str + "factor", 0f);
        factorFiltred = nbt.getFloatOr(str + "factorFiltred", 0f);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putFloat(str + "factor", factor);
        nbt.putFloat(str + "factorFiltred", factorFiltred);
    }
}
