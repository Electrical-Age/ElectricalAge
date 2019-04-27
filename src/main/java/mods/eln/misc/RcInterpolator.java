package mods.eln.misc;

import net.minecraft.nbt.NBTTagCompound;

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
    public void readFromNBT(NBTTagCompound nbt, String str) {
        factor = nbt.getFloat(str + "factor");
        factorFiltred = nbt.getFloat(str + "factorFiltred");
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt, String str) {
        nbt.setFloat(str + "factor", factor);
        nbt.setFloat(str + "factorFiltred", factorFiltred);
        return nbt;
    }
}
