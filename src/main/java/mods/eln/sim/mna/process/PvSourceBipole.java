package mods.eln.sim.mna.process;

import mods.eln.misc.INBTTReady;
import mods.eln.sim.PvCurve;
import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;
import net.minecraft.nbt.CompoundTag;

/**
 * A solar panel's two terminals driven by a {@link PvCurve}, like {@link PowerSourceBipole} (two voltage sources to
 * ground, one on each terminal's load): each pre-step iteration it reads the Thevenin equivalent across the panel and
 * puts the pair at the curve's operating point on that load line. Above the open-circuit voltage the panel takes no
 * current (it doesn't sink power: a blocking diode), as the old panel did above Umax.
 * <p>
 * The curve's light and temperature are set by the panel's slow process ({@link PvCurve#at}); here only
 * {@link PvCurve#solve} runs, warm-started from the last operating point.
 */
public class PvSourceBipole implements IRootSystemPreStepProcess, INBTTReady {
    private final State aPin, bPin;
    private final VoltageSource aSrc, bSrc;
    private final PvCurve curve;
    private double u, i;

    public PvSourceBipole(State aPin, State bPin, VoltageSource aSrc, VoltageSource bSrc, PvCurve curve) {
        this.aPin = aPin;
        this.bPin = bPin;
        this.aSrc = aSrc;
        this.bSrc = bSrc;
        this.curve = curve;
    }

    public PvCurve curve() {
        return this.curve;
    }

    /** Terminal voltage of the last solve. */
    public double getU() {
        return this.u;
    }

    /** Current out of the + terminal at the last solve. */
    public double getI() {
        return this.i;
    }

    /** Power delivered at the last solve (W). */
    public double getP() {
        return this.u * this.i;
    }

    @Override
    public void rootSystemPreStepProcess() {
        SubSystem.Th a = this.aPin.getSubSystem().getTh(this.aPin, this.aSrc);
        SubSystem.Th b = this.bPin.getSubSystem().getTh(this.bPin, this.bSrc);
        double uth = a.U - b.U, rth = a.R + b.R;
        double voc = this.curve.voc();
        if (!(uth < voc) || !(rth > 0) || this.curve.iph() <= 0) {
            // open circuit, dark, or the network sits above Voc: no current, the sources follow the network
            this.aSrc.setU(a.U);
            this.bSrc.setU(b.U);
            this.u = Math.min(voc, Math.max(uth, 0));
            this.i = 0;
            return;
        }
        double op = this.curve.solve(uth, rth, this.u);
        if (!Double.isFinite(op)) {
            this.aSrc.setU(a.U);
            this.bSrc.setU(b.U);
            this.i = 0;
            return;
        }
        // current out of +: (op - uth) / rth; written as shares of rth so a high-impedance side stays finite
        double rise = op - uth;
        this.aSrc.setU(a.U + rise * (a.R / rth));
        this.bSrc.setU(b.U - rise * (b.R / rth));
        this.u = op;
        this.i = rise / rth;
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        this.u = nbt.getDoubleOr(str + "U", 0.0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + "U", this.u);
    }
}
