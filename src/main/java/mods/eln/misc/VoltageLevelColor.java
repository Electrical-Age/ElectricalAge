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
        // (each tier up to the geometric middle between it and the next; 1.12 used 2 x each tier, which fitted x4 steps)
        } else if (voltage <= Math.sqrt(Eln.LVU * Eln.MVU)) {
            return LowVoltage;
        } else if (voltage <= Math.sqrt(Eln.MVU * Eln.HVU)) {
            return MediumVoltage;
        } else if (voltage <= Math.sqrt(Eln.HVU * Eln.VVU)) {
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

