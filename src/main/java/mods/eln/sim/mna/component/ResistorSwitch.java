package mods.eln.sim.mna.component;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.NBTTagCompound;

public class ResistorSwitch extends Resistor implements INBTTReady {

    boolean ultraImpedance = false;
    String name;

    boolean state = false;

    protected double baseR = 1;

    public ResistorSwitch(String name, State aPin, State bPin) {
        super(aPin, bPin);
        this.name = name;
    }

    public void setState(boolean state) {
        this.state = state;
        setR(baseR);
    }

    @Override
    public Resistor setR(double r) {
        baseR = r;
        return super.setR(state ? r : (ultraImpedance ? MnaConst.ultraImpedance : MnaConst.highImpedance));
    }

    public boolean getState() {
        return state;
    }

    boolean diode = false;

    /**
     * Marks this switch as an ideal diode (aPin = anode). The SubSystem then re-checks its state against the
     * solution of the current step (on: current >= 0, off: voltage <= 0) and re-solves when it is inconsistent,
     * so a diode never conducts backwards for a step after a polarity flip (1.7.10 switched only on the
     * previous step's voltage). Set by DiodeProcess.
     */
    public void setDiode(boolean diode) {
        this.diode = diode;
        dirty();
    }

    public boolean isDiode() {
        return diode;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt, String str) {
        str += name;
        setR(nbt.getDouble(str + "R"));
        if (Double.isNaN(baseR) || baseR == 0) {
            if (ultraImpedance) ultraImpedance();
            else highImpedance();
        }
        setState(nbt.getBoolean(str + "State"));
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt, String str) {
        str += name;
        nbt.setDouble(str + "R", baseR);
        nbt.setBoolean(str + "State", getState());
    }

    public void mustUseUltraImpedance() {
        ultraImpedance = true;
    }
}
