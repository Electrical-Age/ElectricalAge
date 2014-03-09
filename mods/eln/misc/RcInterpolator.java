package mods.eln.misc;

import mods.eln.client.FrameTime;

public class RcInterpolator {
	public RcInterpolator(float preTao) {
		ff = 1/preTao;
		factorFiltred = 0;
		factor = 0;
	}
	
	
	float ff;
	
	public void step(float deltaT)
	{
		factorFiltred += (factor - factorFiltred) * ff * deltaT;

	}
	float factor,factorFiltred;
	public void stepGraphic()
	{
		step(FrameTime.get());
	}
	public float get()
	{
		return factorFiltred;
	}
	public void setTarget(float value) {
		factor = value;
	}
	public void setValue(float value)
	{
		factorFiltred = value;
	}
	public void setValueFromTarget() {
		// TODO Auto-generated method stub
		factorFiltred = factor;
	}
	public float getTarget() {
		// TODO Auto-generated method stub
		return factor;
	}
}
