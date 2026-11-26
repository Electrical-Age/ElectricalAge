package mods.eln.oil;

import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

import java.util.List;

/**
 * The Pipe Heater (Baughn 2026-11-26, todo #623): an electric heater in a pipe line, for oil too viscous to flow cold
 * (viscosity falls steeply with temperature: Species.muLiquid). Laid out as the Inline Pump: the line through its flanks
 * at the pump's height (InlinePumpElement.LINE_Y), placed along the player's view, power on its front. Its node is its own, not lumped into the pipes (a lump hands its wall heat back by
 * wall conductance alone, so the pipes' iron would pay for the coils' heat). An Electric Heater
 * with its coils round the line instead of a thermal face: the same regulator slot and screen, but the regulator holds
 * the oil's temperature; with none it heats at full power until something gives. Its coils (a thermal mass) heat the
 * line's contents through a heat link inside the fluid core's step (FluidNode.wallUA), so a dry or stagnant heater
 * overheats its coils and burns out (820 °C), as an Electric Heater with nothing to heat. Model:
 * mods.eln.cad.models.PipeHeater.
 */
public class PipeHeaterDescriptor extends ElectricHeaterDescriptor {
    /** Coils to contents (W/K): its full power at ~80 K over the oil. */
    public final double wallUA;

    public PipeHeaterDescriptor(String name, double nominalU, double nominalP, ElectricalCableDescriptor cable, String model) {
        super(name, PipeHeaterElement.class, nominalU, nominalP, cable, model);
        wallUA = nominalP / 80;
        cableTerminals = mods.eln.cable.CableTerminals.FRONT;   // (the power plate on its front: mods.eln.cad.models.PipeHeater)
        haloX = 0f;
        haloY = (InlinePumpElement.LINE_Y - 8) / 16f;   // (round the line)
    }

    /** Placed with its line along the player's view, as the Inline Pump. */
    @Override
    public mods.eln.misc.Direction getFrontFromPlace(mods.eln.misc.Direction side, net.minecraft.world.entity.LivingEntity entityLiving) {
        return InlinePumpDescriptor.alongView(entityLiving);
    }

    @Override
    protected void addLines(List<String> list) {
        list.add(mods.eln.i18n.I18N.tr("Placed along your view, in a pipe line."));
        list.add(mods.eln.i18n.I18N.tr("%1$ on its front, %2$ into what flows through it.", mods.eln.api.Meters.volts(nominalU),
            mods.eln.api.Meters.watts(nominalP)));
        list.add(mods.eln.i18n.I18N.tr("Hot oil flows much faster than cold."));
        list.add(mods.eln.i18n.I18N.tr("A regulator holds the oil's temperature; without one it heats at full power."));
        list.add(mods.eln.i18n.I18N.tr("Dry or with nothing flowing, its coils burn out."));
    }
}
