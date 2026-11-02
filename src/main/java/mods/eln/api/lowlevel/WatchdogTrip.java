package mods.eln.api.lowlevel;

import java.util.Locale;

/** Why a watchdog tripped: the part it watches, the unit (V, A or W), the value it saw and the limit it crossed. */
public record WatchdogTrip(String part, String unit, double measured, double limit) {
    /** "coil: 52.0 V over 40.0 V" (or "under" for a minimum). */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s: %.1f %s %s %.1f %s", part, measured, unit, measured < limit ? "under" : "over", limit, unit);
    }
}
