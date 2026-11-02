package mods.eln.api.impl;

import mods.eln.api.MachineSpec;
import mods.eln.api.MachineState;
import mods.eln.api.MachineView;
import mods.eln.api.Meters;
import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** A machine's Jade lines (label to value, in order), from its view: an element's, and a hosted machine's. */
public final class MachineLines {
    private MachineLines() {
    }

    /** An element machine's (ElementMachine.wailaLines): its voltage, its power, and its state unless RUNNING (the element words its own). */
    public static Map<String, String> element(MachineView v) {
        Map<String, String> info = new LinkedHashMap<>();
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", v.volts()));
        boolean producer = v.spec().kind() == MachineSpec.Kind.PRODUCER;
        info.put(I18N.tr(producer ? "Power output" : "Power consumption"), Utils.plotPower("", v.watts()));
        if (v.state() != MachineState.RUNNING) info.put(I18N.tr("Machine"), state(v.state()));
        return info;
    }

    /**
     * A machine at another mod's block (compat/jade): the element's lines, then its state, always (the block has no
     * words of its own for it), its buffer, and its temperature with a heat model.
     */
    public static Map<String, String> hosted(MachineView v) {
        Map<String, String> info = element(v);
        info.put(I18N.tr("Machine"), state(v.state()));
        info.put(I18N.tr("Buffer"), Meters.joules(v.bufferJoules()) + " / " + Meters.joules(v.bufferCapacityJoules()));
        if (v.spec().thermal().isPresent()) info.put(I18N.tr("Temperature"), Meters.celsius(v.celsius()));
        return info;
    }

    /**
     * Server: the hosted lines of the machine at pos by the eln:machine capability, or none: for no machine, and for
     * EA's own blocks, whose Jade lines (an element machine's among them) are their node's.
     */
    public static Map<String, String> at(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be == null || be instanceof mods.eln.node.NodeBlockEntity) return Map.of();
        MachineView v = level.getCapability(mods.eln.api.ElnApi.MACHINE, pos, be.getBlockState(), be, null);
        return v == null ? Map.of() : hosted(v);
    }

    /** "overvoltage tripped", translated. */
    static String state(MachineState s) {
        return I18N.tr(s.name().toLowerCase(Locale.ROOT).replace('_', ' '));
    }
}
