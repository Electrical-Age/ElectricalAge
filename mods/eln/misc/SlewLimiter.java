package mods.eln.misc;

import javax.crypto.spec.PSource;

public class SlewLimiter {
	private float positiveSlewRate;
	private float negativeSlewRate;
	private float target = 0; 
	private float position = 0;
	
	public SlewLimiter(float slewRate) {
		setSlewRate(slewRate);
	}
	
	public SlewLimiter(float positive, float negative) {
		setSlewRate(positive, negative);
	}
	
	public float getTarget() {
		return target;
	}

	public void setTarget(float target) {
		this.target = target;
	}
	
	public float getPosition() {
		return position;
	}

	public void setPosition(float position) {
		this.position = position;
	}
	
	public boolean targetReached() {
		return position == target;
	}
	
	public boolean targetReached(float tolerance) {
		return Math.abs(position - target ) <= tolerance;
	}
	
	public float getPositiveSlewRate() {
		return positiveSlewRate;
	}
	
	public float getNegativeSlewRate() {
		return negativeSlewRate;
	}
	
	public void setSlewRate(float slewRate) {
		this.positiveSlewRate = Math.abs(slewRate);
		this.negativeSlewRate = Math.abs(slewRate);
	}
	
	public void setSlewRate(float positive, float negative) {
		this.positiveSlewRate = Math.abs(positive);
		this.negativeSlewRate = Math.abs(negative);
	}
	
	public void step(float deltaTime) {
		float delta = target - position;
		if (delta > 0f)
			delta = Math.min(delta, positiveSlewRate * deltaTime);
		else if (delta < 0f)
			delta = Math.max(delta, -negativeSlewRate * deltaTime);
		position += delta;
	}
}
