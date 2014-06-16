package mods.eln.misc;

import mods.eln.client.FrameTime;

public class PhysicalInterpolator {
	
	public PhysicalInterpolator(float preTao,float accPerSPerError,float slowPerS,float rebond) {
		ff = 1/preTao;
		this.accPerSPerError = accPerSPerError;
		this.slowPerS = slowPerS;
		this.rebond = rebond;
	}
	
	float factor;
	float factorSpeed = 0;
	float factorPos = 0;
	float factorFiltred = 0;
	float accPerSPerError;
	float slowPerS;
	
	float ff;
	float rebond;
	
	public void step(float deltaT)
	{
		factorFiltred += (factor - factorFiltred) * ff * deltaT;
		float error = factorFiltred - factorPos;
		factorSpeed *= 1 - (slowPerS * deltaT);
		factorSpeed += error * accPerSPerError * deltaT;
		
		if(factorSpeed > maxSpeed) factorSpeed = maxSpeed;
		if(factorSpeed < -maxSpeed) factorSpeed = -maxSpeed;
		
		
		factorPos += factorSpeed * deltaT;
		if(factorPos > 1.0)
		{
			factorFiltred = factor;
			factorPos = 1.0f;
			factorSpeed = -factorSpeed * rebond;
		}
		if(factorPos < 0.0)
		{
			factorFiltred = factor;
			factorPos = 0.0f;
			factorSpeed = -factorSpeed * rebond;
		}
	}
	/*public void stepGraphic()
	{
		step(FrameTime.get());
	}*/
	public float get()
	{
		return factorPos;
	}
	public void setPos(float value) {
		factorPos = value;
		factorFiltred = value;
		setTarget(value);
	}
	public void setTarget(float value) {
		factor = value;
	}
	public float getTarget() {
		// TODO Auto-generated method stub
		return factor;
	}
	float maxSpeed = 1000;
	public void setMaxSpeed(float d) {
		// TODO Auto-generated method stub
		maxSpeed = d;
	}
}
