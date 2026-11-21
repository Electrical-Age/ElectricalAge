package mods.eln.sim;

import mods.eln.item.regulator.IRegulatorDescriptor.RegulatorType;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import net.minecraft.nbt.CompoundTag;

public abstract class RegulatorProcess implements IProcess, INBTTReady {

    RegulatorType type = RegulatorType.None;
    double target;
    double OnOffHysteresisDiv2;
    double P, I, D;
    double hitLast = 0, errorIntegrated = 0;
    boolean boot = true;
    String name;

    public void setManual() {
        integralSaved = false;   // (a saved integral is for a PID taking over straight after the load, not hours later)
        type = RegulatorType.Manual;
    }

    /** Is no regulator in charge (the command is the owner's)? */
    public boolean isManual() {
        return type == RegulatorType.Manual;
    }

    public void setNone() {
        integralSaved = false;
        type = RegulatorType.None;
    }

    public void setOnOff(double OnOffHysteresisFactor, double workingPoint) {
        pid = integralSaved = false;
        type = RegulatorType.OnOff;
        this.OnOffHysteresisDiv2 = OnOffHysteresisFactor * workingPoint / 2;
        boot = false;
        setCmd(0.0);
    }

    public void setAnalog(double P, double I, double D, double workingPoint) {
        pid = integralSaved = false;
        P /= workingPoint;
        I /= workingPoint;
        D /= workingPoint;

        if (!boot && (this.P != P || this.I != I || this.D != D || type != RegulatorType.Analog)) {
            errorIntegrated = 0;
            hitLast = getHit();
        }

        this.P = P;
        this.I = I;
        this.D = D;

        type = RegulatorType.Analog;
        boot = false;
    }

    /**
     * A proper PID (Baughn 1573753560456499223: the heat furnaces' Analogic Regulator): kp per kelvin of error, ki per
     * kelvin-second, kd per kelvin/second of the measurement's rise (on the measurement, not the error: no kick when the
     * set point moves; through a tf-second filter). The command is 0..1; the integral only moves while that doesn't pin
     * it, or when the error would bring it back, nor while the plant can't give more (no windup). It starts at 0 (the
     * heat furnaces' air answers in seconds: a bumpless start from a full fire overshot 54 K) or, after a load, where it
     * was saved. Call it instead of IRegulatorDescriptor.applyTo, which would restart it.
     */
    public void setPid(double kp, double ki, double kd, double tf) {
        boolean fresh = !pid || type != RegulatorType.Analog;
        type = RegulatorType.Analog;
        pid = true;
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.tf = tf;
        if (fresh) pidStart = true;
        boot = false;
    }

    /** Tests: the PID's state (integral, filtered rate of the measurement, kind). */
    public String pidState() {
        return String.format("I %.4f rate %.4f pid %s type %s target %.1f", errorIntegrated, rate, pid, type, target);
    }

    boolean pid, pidStart = true, integralSaved;
    double kp, ki, kd, tf, rate;

    /** Can't the plant give more than it does now, whatever the command (a coal bed still spreading)? No windup then. */
    protected boolean limitedUp() {
        return false;
    }

    public void setTarget(double target) {
        this.target = target;
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
            case Manual:
                break;
            case None:
                setCmd(1.0);
                break;
            case Analog:
                if (pid) {
                    stepPid(hit, time);
                    break;
                }
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

            case OnOff:
                if (hit > target + OnOffHysteresisDiv2) setCmd(0.0);
                if (hit < target - OnOffHysteresisDiv2) setCmd(1.0);
                break;

            default:
                break;
        }
    }

    private void stepPid(double hit, double time) {
        if (pidStart) {
            hitLast = hit;
            rate = 0;
            if (!integralSaved) errorIntegrated = 0;
            pidStart = integralSaved = false;
        }
        double e = target - hit;
        rate += ((hit - hitLast) / Math.max(time, 1e-9) - rate) * (tf <= 0 ? 1 : Math.min(1, time / tf));
        hitLast = hit;
        double u = kp * e + errorIntegrated - kd * rate;
        // (conditional integration: not while pinned at an end the error pushes further into)
        if (!((u >= 1 || limitedUp()) && e > 0) && !(u <= 0 && e < 0)) errorIntegrated = Utils.limit(errorIntegrated + ki * e * time, -1, 1);
        setCmd(Utils.limit(kp * e + errorIntegrated - kd * rate, 0, 1));
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        errorIntegrated = nbt.getDoubleOr(str + name + "errorIntegrated", 0.0);
        integralSaved = true;
        if (Double.isNaN(errorIntegrated)) errorIntegrated = 0;
        double t = nbt.getDoubleOr(str + name + "target", 0.0);
        setTarget(Double.isFinite(t) ? t : 0.0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + name + "errorIntegrated", errorIntegrated);
        nbt.putDouble(str + name + "target", target);
    }
}
