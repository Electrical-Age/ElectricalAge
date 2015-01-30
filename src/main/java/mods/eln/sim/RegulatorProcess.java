package mods.eln.sim;

import mods.eln.misc.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;

public abstract class RegulatorProcess implements IProcess, INBTTReady {

	RegulatorType type = RegulatorType.none;
	
	double target;
	
	double OnOffHysteresisDiv2;
	
	double Pfactor, Ifactor, Dfactor;
	double P, I, D;
	double hitLast = 0, errorIntegrated = 0;
	
	boolean boot = true;
	
	String name;
	
	public void setManuel() {
		type = RegulatorType.manuel;
	}
	
	public void setNone() {
		type = RegulatorType.none;
	}
	
	public void setOnOff(double OnOffHysteresisFactor, double workingPoint) {
		type = RegulatorType.onOff;
		this.OnOffHysteresisDiv2 = OnOffHysteresisFactor * workingPoint / 2;
		boot = false;
		setCmd(0.0);
	}
	
	public void setAnalog(double P, double I, double D, double workingPoint) {
		P /= workingPoint;
		I /= workingPoint;
		D /= workingPoint;
		
		if (!boot && (this.P != P || this.I != I || this.D != D || type != RegulatorType.analog)) {
			errorIntegrated = 0;
			hitLast = getHit();
		}
		
		this.P = P;
		this.I = I;
		this.D = D;
		
		type = RegulatorType.analog;
		boot = false;
	}
	/*
	public void setMechanical(double P, double I, double D, double workingPoint) {
		P /= workingPoint;
		I /= workingPoint;
		D /= workingPoint;
		
		if (!boot && (this.P != P || this.I != I || this.D != D || type != RegulatorType.mechanical)) {
			errorIntegrated = 0;
			hitLast = getHit();
		}
		
		this.P = P;
		this.I = I;
		this.D = D;
		
		type = RegulatorType.mechanical;
		boot = false;
	}*/
	
	public void setTarget(double target) {
		this.target = target;
		/*this.OnOffHysteresisDiv2 = target * OnOffHysteresisFactor / 2;
		P = Pfactor / target;
		I = Ifactor / target;
		D = Dfactor / target;*/
	}
	
	public double getTarget() {
		return target;
	}
	
	protected abstract double getHit();

	protected abstract void setCmd(double cmd);

	public RegulatorProcess(String name) {
		this.name = name;
	}

	@Override
	public void process(double time) {
		double hit = getHit();

		switch (type) {
			case manuel:
				break;
			case none:
				setCmd(1.0);
				break;
			case analog:
	//		case mechanical:
				double error = target - hit;
				double fP = error * P;
				double cmd = fP - (hit - hitLast) * D * time;

				errorIntegrated += error * time * I;

				if (errorIntegrated > 1.0 - fP) {
					errorIntegrated = 1.0 - fP;
					if (errorIntegrated < 0.0) errorIntegrated = 0.0;
				} else if (errorIntegrated < (-1.0 + fP)) {
					errorIntegrated = (-1.0 + fP);
					if (errorIntegrated > 0.0) errorIntegrated = 0.0;
				}

				cmd += errorIntegrated;

				if (cmd > 1.0) setCmd(1.0);
				else if (cmd < -1.0) setCmd(-1.0);
				else setCmd(cmd);

				hitLast = hit;
				break;

			case onOff:
				if (hit > target + OnOffHysteresisDiv2) setCmd(0.0);
				if (hit < target - OnOffHysteresisDiv2) setCmd(1.0);
				break;
		/*case digital:
			break;*/
			default:
				break;
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		errorIntegrated = nbt.getDouble(str + name + "errorIntegrated");
		if (Double.isNaN(errorIntegrated)) errorIntegrated = 0;
		setTarget(nbt.getDouble(str + name + "target"));
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setDouble(str + name + "errorIntegrated", errorIntegrated);
		nbt.setDouble(str + name + "target", target);
	}
}
