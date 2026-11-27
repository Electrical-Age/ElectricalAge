package mods.eln.api.impl;

import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.Meters;
import mods.eln.compat.jade.ProbeLines;
import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;

import java.util.Locale;
import java.util.Map;

/** A machine's Jade lines (label to value, in order), from its view: an element's, and a hosted machine's. */
public final class MachineLines {
    private MachineLines() {
    }

    /** An element machine's (ElementMachine.wailaLines): its voltage, its power, and its state unless RUNNING (the element words its own). */
    public static ProbeLines element(MachineView v) {
        ProbeLines info = new ProbeLines();
        info.headline(I18N.tr("Voltage"), Utils.plotVolt("", v.volts()));
        boolean producer = v.spec().kind() == MachineSpec.Kind.PRODUCER;
        info.headline(I18N.tr(producer ? "Power out" : "Power"), Utils.plotPower("", v.watts()));
        if (v.state() != MachineState.RUNNING) info.headline(I18N.tr("State"), state(v.state()));
        return info;
    }

    /**
     * A machine at another mod's block (compat/jade): voltage, power, and state always in the headline; buffer and
     * temperature (when it has a heat model) in details.
     */
    public static ProbeLines hosted(MachineView v) {
        ProbeLines info = element(v);
        info.headline(I18N.tr("State"), state(v.state()));
        info.detail(I18N.tr("Buffer"), Meters.joules(v.bufferJoules()) + " / " + Meters.joules(v.bufferCapacityJoules()));
        if (v.spec().thermal().isPresent()) info.detail(I18N.tr("Temperature"), Meters.celsius(v.celsius()));
        return info;
    }

    /**
     * Server: the hosted lines of the machine at pos by the eln:machine capability, or none: for no machine, for one
     * whose spec turned them off, and for EA's own blocks, whose Jade lines (an element machine's among them) are their node's.
     */
    public static Map<String, String> at(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null || be instanceof mods.eln.node.NodeBlockEntity) return Map.of();
        MachineView v = level.getCapability(mods.eln.api.ElnApi.MACHINE, pos, be.getBlockState(), be, null);
        return v == null || !v.spec().jadeLines() ? Map.of() : hosted(v);   // (jadeLines(false): the mod shows its own)
    }

    /** "overvoltage tripped", translated. */
    static String state(MachineState s) {
        return I18N.tr(s.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }
}
