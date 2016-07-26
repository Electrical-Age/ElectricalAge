package mods.eln.misc;

import mods.eln.Eln;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public enum VoltageLevelColor {
    None(null),
    Neutral("neutral"),
    SignalVoltage("signal"),
    LowVoltage("low"),
    MediumVoltage("medium"),
    HighVoltage("high"),
    VeryHighVoltage("veryhigh"),
    Thermal("thermal");

    VoltageLevelColor(final String voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    public void drawIconBackground(IItemRenderer.ItemRenderType type) {
        if (voltageLevel != null &&
            type == IItemRenderer.ItemRenderType.INVENTORY || type == IItemRenderer.ItemRenderType.FIRST_PERSON_MAP) {
            UtilsClient.drawIcon(type, new ResourceLocation("eln", "textures/voltages/" + voltageLevel + ".png"));
        }
    }

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

    public static VoltageLevelColor fromMaxCurrent(double maxCurrent) {
        if (maxCurrent <= 0) {
            return None;
        } else if (maxCurrent <= Eln.VVP / Eln.VVU) {
            return VeryHighVoltage;
        } else if (maxCurrent <= Eln.HVP / Eln.HVU) {
            return HighVoltage;
        } else if (maxCurrent <= Eln.MVP / Eln.MVU) {
            return MediumVoltage;
        } else if (maxCurrent <= Eln.LVP / Eln.LVU) {
            return LowVoltage;
        } else {
            return None;
        }
    }

    public static VoltageLevelColor fromCable(ElectricalCableDescriptor descriptor) {
        if (descriptor != null) {
            if (descriptor.signalWire) {
                return SignalVoltage;
            } else {
                return fromVoltage(descriptor.electricalNominalVoltage);
            }
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

