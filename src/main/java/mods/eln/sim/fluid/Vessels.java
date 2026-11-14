package mods.eln.sim.fluid;

/**
 * How a machine runs the small vessels behind its flanges, on the fluid core: an inlet's filling valve, an outlet
 * pump's sump, and a draw of gas from a sealed vessel.
 */
public final class Vessels {
    private Vessels() {
    }

    /**
     * A filling valve shuts at FILL_LEVEL (liquid share of its vessel: a gas cap stays above, so the vessel never sees
     * the line's pressure) and passes at most FILL_GAIN of the room under that level in a step.
     */
    public static final double FILL_LEVEL = 0.90, FILL_GAIN = 0.25;

    /**
     * A pump on its vessel's floor runs from SUMP_LOW (liquid share) and at full speed from SUMP_FULL. Its port draws
     * pure liquid only a band over the floor; under that it draws the gas with it, which a pump still primed pulls out
     * until what is left flashes and freezes (a liquefied gas went to 120 K).
     */
    public static final double SUMP_LOW = FluidNetwork.BAND + 0.01, SUMP_FULL = SUMP_LOW + 0.02;

    /**
     * The opening (0..1) of the filling valve into vessel for a step of dt s. The valve is a check valve from the
     * flange's stub into the vessel's top; a flange on the vessel itself would fill it to the brim at line pressure
     * within a step, and a vessel full of liquid with flow through it is the dearest thing the solver knows.
     */
    public static double fillOpening(FluidEdge valve, FluidNode vessel, double dt) {
        return valve.openingFor(FILL_GAIN * (FILL_LEVEL * vessel.volume - vessel.liquidVolume()), dt);
    }

    /** The speed (0..1) of a pump that draws at vessel's floor: it keeps a sump. */
    public static double sumpSpeed(FluidNode vessel) {
        double share = vessel.liquidVolume() / vessel.volume;
        return Math.max(0, Math.min(1, (share - SUMP_LOW) / (SUMP_FULL - SUMP_LOW)));
    }

    /**
     * Draw up to want kg of species id as gas out of a sealed vessel, with everything else in its gas in proportion and
     * all of it with its enthalpy, and never to under 1 atm (a machine has no vacuum pump). The rest of the mix is what
     * a machine would vent or burn: without it the air a vessel starts with never leaves, and no feed gets in. With none
     * of the species in the gas yet it lets out half of what is over 1 atm, to make room. Returns the kg of id taken.
     */
    public static double drawMix(FluidNode v, int id, double want) {
        if (want <= 0) return 0;
        double p = v.P();
        double over = Math.max(0, (p - Water.P_ATM) / p), have = v.vapour(id);
        double f = have > 0 ? Math.min(over, want / have) : 0.5 * over;
        if (f <= 0) return 0;
        double T = v.T();
        SpeciesRegistry reg = SpeciesRegistry.get();
        for (int j = 0; j < v.speciesBound(); j++) {
            double kg = v.vapour(j) * f;
            if (kg <= 0) continue;
            double h = j == SpeciesRegistry.AIR ? Water.hAir(T) : j == SpeciesRegistry.WATER ? v.hVapour(T) : reg.byId(j).hVapour(T);
            v.addMass(j, -kg);
            v.energy -= kg * h;
        }
        if (v.mAir < 0) v.mAir = 0;
        v.update();
        return f * have;
    }
}
