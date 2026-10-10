package mods.eln.misc;

import mods.eln.gl.GL11;

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

    public void setGLColor() {
        switch (this) {
            case None:
            case Neutral:
                break;

            case SignalVoltage:
                GL11.glColor3f(.80f, .87f, .82f);
                break;

            case LowVoltage:
                GL11.glColor3f(.55f, .84f, .68f);
                break;

            case MediumVoltage:
                GL11.glColor3f(.55f, .74f, .85f);
                break;

            case HighVoltage:
                GL11.glColor3f(.96f, .80f, .56f);
                break;

            case VeryHighVoltage:
                GL11.glColor3f(.86f, .58f, .55f);
                break;

        }
    }
}

