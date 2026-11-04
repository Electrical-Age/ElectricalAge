package mods.eln.api;

import java.util.Locale;

/**
 * Why a machine tripped: the state it went to, the value that tripped it and the limit, in the state's unit (V for
 * overvoltage, degrees C for overheat). burns: the spec has burnsOnOvervoltage and the machine burns rather than
 * resets (an EA element burns out; another mod's block gets its spec's burn handler).
 */
public record TripCause(MachineState state, double measured, double limit, boolean burns) {
    public TripCause {
        if (!state.tripped()) throw new IllegalArgumentException(state + " is not a trip");
    }

    /** "overvoltage: 270.0 V over 260.0 V", plus "(burns)" when it does. */
    @Override
    public String toString() {
        String what = switch (state) {
            case OVERVOLTAGE_TRIPPED -> "overvoltage";
            default -> "overheat";
        };
        String unit = switch (state) {
            case OVERVOLTAGE_TRIPPED -> "V";
            default -> "C";
        };
        return String.format(Locale.ROOT, "%s: %.1f %s over %.1f %s%s", what, measured, unit, limit, unit,
            burns ? " (burns)" : "");
    }
}
