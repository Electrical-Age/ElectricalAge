package mods.eln.misc;

import mods.eln.Eln;

public enum VoltageLevelColor {
    None(null),
    Neutral("neutral"),
    SignalVoltage("signal"),
    LowVoltage("low"),
    MediumVoltage("medium"),
    HighVoltage("high"),
    VeryHighVoltage("veryhigh"),
    Grid("grid"),
    HighGrid("highgrid"),
    Thermal("thermal");

    VoltageLevelColor(final String voltageLevel) {
        this.voltageLevel = voltageLevel;
    }
// 26.x port: drawIconBackground() removed (client rendering; redone in P3)

    private String voltageLevel;

    public static VoltageLevelColor fromVoltage(double voltage) {
        if (voltage < 0) {
            return None;
        } else if (voltage <= 2 * Eln.LVU) {
            return LowVoltage;
        } else if (voltage <= 2 * Eln.MVU) {
            return MediumVoltage;
        } else if (voltage <= 2 * Eln.HVU) {
            return HighVoltage;
        } else if (voltage <= 2 * Eln.VVU) {
            return VeryHighVoltage;
        } else {
            return None;
        }
    }
// 26.x port: setGLColor() removed (client rendering; redone in P3)
}

