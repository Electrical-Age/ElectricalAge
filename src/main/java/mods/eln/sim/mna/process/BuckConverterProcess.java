package mods.eln.sim.mna.process;

import mods.eln.sim.mna.SubSystem;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;
import mods.eln.sim.mna.state.State;

/**
 * A lossy buck converter between two ports, each a pair of voltage sources to ground (like {@link PvSourceBipole}),
 * coupled each pre-step iteration from both ports' Thevenin equivalents: output voltage = ratio x input voltage,
 * output power = efficiency x input power. With efficiency 1 it is {@link TransformerInterSystemProcess}'s law; the
 * ratio is a plain number, so a tracker may change it every step without a matrix rebuild.
 * <p>
 * Solving both ports at once (not "hold the input at V, then see what flows") keeps a nonlinear source on the input,
 * a solar panel, converging: the panel sees the output reflected through the ratio as an ordinary load line.
 * <ul>
 *   <li>Buck only: the ratio is clamped to (0, 1] (Baughn 2026-11-25: boost is the player's business).</li>
 *   <li>Output voltage limit (a battery's charge voltage): where the ratio would push the output over it, the ratio
 *       is lowered for this iteration until it doesn't ({@link #isLimited}).</li>
 *   <li>Input power limit (the controller's rating): the same, putting the input at the higher-voltage point on its
 *       load line that takes the rating ({@link #isPowerLimited}). Within the solve, as the output limit, because a
 *       tracker stepping the ratio from outside against a steep curve was unstable for large arrays.</li>
 *   <li>Never backwards: when the input would take current from the output (night, input below the reflected output),
 *       both ports float and nothing flows.</li>
 * </ul>
 * Board #79 (MPPT charge controllers).
 */
public class BuckConverterProcess implements IRootSystemPreStepProcess {
    private final State inPos, inNeg, outPos, outNeg;
    private final VoltageSource inPosSrc, inNegSrc, outPosSrc, outNegSrc;
    private double ratio = 0.5, efficiency = 1.0, outMax = Double.POSITIVE_INFINITY;
    private double powerMax = Double.POSITIVE_INFINITY;
    private boolean limited, powerLimited, flowing, outputFloating;
    private double uIn, iIn, uOut, iOut, ratioUsed;

    public BuckConverterProcess(State inPos, State inNeg, VoltageSource inPosSrc, VoltageSource inNegSrc,
                                State outPos, State outNeg, VoltageSource outPosSrc, VoltageSource outNegSrc) {
        this.inPos = inPos;
        this.inNeg = inNeg;
        this.inPosSrc = inPosSrc;
        this.inNegSrc = inNegSrc;
        this.outPos = outPos;
        this.outNeg = outNeg;
        this.outPosSrc = outPosSrc;
        this.outNegSrc = outNegSrc;
    }

    /** Output over input voltage, clamped to (0, 1]. */
    public void setRatio(double ratio) {
        this.ratio = Double.isFinite(ratio) ? Math.min(1.0, Math.max(1e-4, ratio)) : this.ratio;
    }

    public double getRatio() {
        return this.ratio;
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = Math.min(1.0, Math.max(0.01, efficiency));
    }

    /** The output's voltage limit (V); infinite: none. */
    public void setOutputLimit(double volts) {
        this.outMax = volts > 0 ? volts : Double.POSITIVE_INFINITY;
    }

    /** The ratio actually used at the last solve (lower than {@link #getRatio} when the output limit applied). */
    public double getRatioUsed() {
        return this.ratioUsed;
    }

    /** The rated input power (W); infinite: none. Like the output limit, applied within each solve. */
    public void setPowerLimit(double watts) {
        this.powerMax = watts > 0 ? watts : Double.POSITIVE_INFINITY;
    }

    /** Nothing is on the output at the last solve (no path through it: a cable off). */
    public boolean isOutputFloating() {
        return this.outputFloating;
    }

    /** The power limit lowered the ratio at the last solve. */
    public boolean isPowerLimited() {
        return this.powerLimited;
    }

    /** The output limit lowered the ratio at the last solve. */
    public boolean isLimited() {
        return this.limited;
    }

    /** Power flowed forward at the last solve. */
    public boolean isFlowing() {
        return this.flowing;
    }

    public double getInputU() {
        return this.uIn;
    }

    /** Current into the input (A). */
    public double getInputI() {
        return this.iIn;
    }

    public double getOutputU() {
        return this.uOut;
    }

    /** Current out of the output (A). */
    public double getOutputI() {
        return this.iOut;
    }

    public double getInputP() {
        return this.uIn * this.iIn;
    }

    public double getOutputP() {
        return this.uOut * this.iOut;
    }

    @Override
    public void rootSystemPreStepProcess() {
        SubSystem.Th ip = this.inPos.getSubSystem().getTh(this.inPos, this.inPosSrc);
        SubSystem.Th in = this.inNeg.getSubSystem().getTh(this.inNeg, this.inNegSrc);
        SubSystem.Th op = this.outPos.getSubSystem().getTh(this.outPos, this.outPosSrc);
        SubSystem.Th on = this.outNeg.getSubSystem().getTh(this.outNeg, this.outNegSrc);
        double aU = ip.U - in.U, aR = ip.R + in.R;   // input port: what drives it
        double bU = op.U - on.U, bR = op.R + on.R;   // output port: what it drives
        this.outputFloating = !(bR < 1e9);   // (getTh gives 1e19 for a node nothing ties to anything)
        double eta = this.efficiency;
        double n = this.ratio;
        this.limited = false;
        this.powerLimited = false;
        double u = Double.NaN, v = Double.NaN;
        if (aR > 0 && bR > 0 && aU > 0 && Double.isFinite(aU + aR + bU + bR)) {
            for (int k = 0; k < 6; k++) {
                // (n u - bU)/bR = eta/n (aU - u)/aR  =>  u = (n aR bU + eta bR aU) / (n^2 aR + eta bR)
                u = (n * aR * bU + eta * bR * aU) / (n * n * aR + eta * bR);
                v = n * u;
                if (v > this.outMax * (1 + 1e-9)) {
                    // over the output limit: the ratio that puts the output at it, for this iteration (quadratic in n)
                    n = limitRatio(aU, aR, bU, bR, eta, this.outMax, n);
                    this.limited = true;
                    continue;
                }
                if (u * (aU - u) / aR > this.powerMax * (1 + 1e-9)) {
                    // over the rated input power: the ratio that puts the input on its line at the higher-voltage point
                    // taking the rating (past the source's peak, as a real controller backs off)
                    double nP = powerRatio(aU, aR, bU, bR, eta, this.powerMax);
                    if (nP < n) {
                        n = nP;
                        this.powerLimited = true;
                        continue;
                    }
                }
                break;
            }
        }
        double iIn = Double.isFinite(u) ? (aU - u) / aR : 0;
        double iOut = Double.isFinite(v) ? (v - bU) / bR : 0;
        if (!(iIn > 0 && iOut > 0) || !Double.isFinite(u + v) || this.outputFloating) {   // (floating: only rounding flows)
            // backwards, or nothing to convert: both ports float
            this.inPosSrc.setU(ip.U);
            this.inNegSrc.setU(in.U);
            this.outPosSrc.setU(op.U);
            this.outNegSrc.setU(on.U);
            this.flowing = false;
            this.uIn = Math.max(aU, 0);
            this.uOut = Math.max(bU, 0);
            this.iIn = this.iOut = 0;
            this.ratioUsed = this.ratio;
            return;
        }
        // drop over each side's Thevenin share, as PvSourceBipole does
        double fallIn = aU - u, riseOut = v - bU;
        this.inPosSrc.setU(ip.U - fallIn * (ip.R / aR));
        this.inNegSrc.setU(in.U + fallIn * (in.R / aR));
        this.outPosSrc.setU(op.U + riseOut * (op.R / bR));
        this.outNegSrc.setU(on.U - riseOut * (on.R / bR));
        this.flowing = true;
        this.uIn = u;
        this.iIn = iIn;
        this.uOut = v;
        this.iOut = iOut;
        this.ratioUsed = n;
    }

    /**
     * The ratio n at which the input takes pMax at the higher of the two voltages on its line that do: u = (aU +
     * sqrt(aU^2 - 4 aR pMax)) / 2 (or aU / 2, the line's peak, if pMax is beyond it), then n from
     * aR u n^2 - aR bU n + eta bR (u - aU) = 0, the positive root.
     */
    static double powerRatio(double aU, double aR, double bU, double bR, double eta, double pMax) {
        double disc = aU * aU - 4 * aR * pMax;
        double u = disc > 0 ? (aU + Math.sqrt(disc)) / 2 : aU / 2;
        double qa = aR * u, qb = -aR * bU, qc = eta * bR * (u - aU);
        double d = qb * qb - 4 * qa * qc;
        return d < 0 || qa <= 0 ? 1e-4 : (-qb + Math.sqrt(d)) / (2 * qa);
    }

    /**
     * The ratio n at which the output sits at vMax: n u(n) = vMax with u(n) above, i.e.
     * vMax (n^2 aR + eta bR) = n (n aR bU + eta bR aU)  =>  n^2 aR (vMax - bU) - n eta bR aU + vMax eta bR = 0;
     * the smaller root (the larger ratio would be past the peak of v(n)). Falls back to scaling the ratio down.
     */
    static double limitRatio(double aU, double aR, double bU, double bR, double eta, double vMax, double n) {
        double qa = aR * (vMax - bU), qb = -eta * bR * aU, qc = vMax * eta * bR;
        if (Math.abs(qa) < 1e-12 * Math.abs(qb)) return Math.min(n, qc / -qb);
        double disc = qb * qb - 4 * qa * qc;
        if (disc < 0) return n * 0.5;   // vMax unreachable from below: the output can't get there; back off
        double s = Math.sqrt(disc);
        double r1 = (-qb - s) / (2 * qa), r2 = (-qb + s) / (2 * qa);
        double lo = Math.min(r1, r2);
        if (lo > 0 && lo < n) return lo;
        double hi = Math.max(r1, r2);
        return hi > 0 && hi < n ? hi : n * 0.5;
    }
}
