package mods.eln.sim.mna.component;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.misc.ISubSystemProcessI;
import mods.eln.sim.mna.state.State;

public class Capacitor extends Bipole implements ISubSystemProcessI {

    private double c = 0;
    double cdt;
    /** Voltage across the capacitor at the previous step (the history term of backward Euler). */
    double uPrev = 0;

    public Capacitor() {
    }

    public Capacitor(State aPin, State bPin) {
        connectTo(aPin, bPin);
    }

    /** Current aPin -> bPin during the last step: backward Euler i = C (U_n - U_{n-1}) / dt (1.7.10 returned 0). */
    @Override
    public double getCurrent() {
        return (getU() - uPrev) * cdt;
    }

    public void setC(double c) {
        this.c = c;
        dirty();
    }

    @Override
    public void applyTo(SubSystem s) {
        cdt = c / s.getDt();

        s.addToA(aPin, aPin, cdt);
        s.addToA(aPin, bPin, -cdt);
        s.addToA(bPin, bPin, cdt);
        s.addToA(bPin, aPin, -cdt);
    }

    @Override
    public void simProcessI(SubSystem s) {
        uPrev = s.getXSafe(aPin) - s.getXSafe(bPin);
        double add = uPrev * cdt;
        s.addToI(aPin, add);
        s.addToI(bPin, -add);
    }

    @Override
    public void quitSubSystem() {
        subSystem.removeProcess(this);
        super.quitSubSystem();
    }

    @Override
    public void addedTo(SubSystem s) {
        super.addedTo(s);
        s.addProcess(this);
    }

    public double getE() {
        double u = getU();
        return u * u * c / 2;
    }

    public double getC() {
        return c;
    }
}
