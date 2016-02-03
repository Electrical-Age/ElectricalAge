package mods.eln.misc;

import mods.eln.Eln;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.IItemRenderer;

public enum VoltageLevelColor {
    None(null),
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
}
