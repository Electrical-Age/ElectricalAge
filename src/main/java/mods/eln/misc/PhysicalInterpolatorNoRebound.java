package mods.eln.misc;

public class PhysicalInterpolatorNoRebound {

    float factor;
    float factorSpeed = 0;
    float factorPos = 0;
    float factorFiltred = 0;
    float accPerSPerError;
    float slowPerS;

    float ff;

    float maxSpeed = 1000;

    public PhysicalInterpolatorNoRebound(float preTao, float accPerSPerError, float slowPerS) {
        ff = 1 / preTao;
        this.accPerSPerError = accPerSPerError;
        this.slowPerS = slowPerS;
    }

    public void step(float deltaT) {
        factorFiltred += (factor - factorFiltred) * ff * deltaT;
        float error = factorFiltred - factorPos;
        factorSpeed *= 1 - (slowPerS * deltaT);
        factorSpeed += error * accPerSPerError * deltaT;

        if (factorSpeed > maxSpeed) factorSpeed = maxSpeed;
        if (factorSpeed < -maxSpeed) factorSpeed = -maxSpeed;

        factorPos += factorSpeed * deltaT;
    }

	/*public void stepGraphic()
    {
		step(FrameTime.get());
	}*/

    public float get() {
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
        return factor;
    }

    public void setMaxSpeed(float d) {
        maxSpeed = d;
    }
}
