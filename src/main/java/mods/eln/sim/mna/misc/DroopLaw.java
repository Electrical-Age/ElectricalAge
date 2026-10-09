package mods.eln.sim.mna.misc;

import mods.eln.sim.mna.SubSystem;

/**
 * Terminal voltage of a machine (shaft generator, heat turbine) modelled as a source to ground whose delivered power
 * droops linearly with voltage: P = k * (E - U), E = no-load (EMF) voltage, k = W per V of droop. The source faces
 * the Thevenin equivalent th of the rest of its SubSystem, so P = U * (U - th.U) / th.R; equating gives
 * U^2 / R + (k - th.U / R) U - k E = 0, of which the positive root is taken.
 *
 * The same law holds for E < th.U (the machine absorbs power, i.e. a generator motoring): the product of the
 * roots is -k E R < 0, so there is exactly one positive root, and it lies between E and th.U.
 */
public final class DroopLaw {
    private DroopLaw() {
    }

    /** Voltage to set on the source. */
    public static double terminalU(SubSystem.Th th, double E, double k) {
        if (th.isHighImpedance()) return E;
        double a = 1 / th.R;
        double b = k - th.U / th.R;
        double c = -k * E;
        return (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
    }

    /**
     * Limits the power the source absorbs from the network to pMax (> 0): if U would make it absorb more, returns
     * the higher voltage at which it absorbs exactly pMax (or th.U when the network can't supply pMax at all).
     */
    public static double limitAbsorbed(SubSystem.Th th, double U, double pMax) {
        if (th.isHighImpedance()) return U;
        double absorbed = U * (th.U - U) / th.R;
        if (absorbed <= pMax) return U;
        double disc = th.U * th.U - 4 * pMax * th.R;
        if (disc < 0) return U; // the network's maximum transfer (at th.U / 2) is below pMax; U is already above it
        return Math.max(U, (th.U + Math.sqrt(disc)) / 2);
    }
}
