package mods.eln.api.docs;

import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.lowlevel.Load;
import mods.eln.api.lowlevel.PortKind;
import mods.eln.api.lowlevel.PortLink;
import mods.eln.api.lowlevel.PortRef;
import mods.eln.api.lowlevel.PortStatus;
import mods.eln.api.lowlevel.ResistorLoad;
import mods.eln.api.lowlevel.Watchdog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** docs/api.md's low level: a load bank of switchable ohms, guarded, with a probe on a remote cable. */
public final class LoadBank {
    // doc:begin build
    public final Circuit circuit;
    public final Load in, probe;
    public final ResistorLoad bank;
    public final Watchdog guard;

    /** The bank's parts; probeAt is null for none (a JUnit bench has no world to reach into). */
    public LoadBank(BlockPos probeAt) {
        circuit = Circuit.create("load bank");
        in = circuit.load("in");
        bank = circuit.resistorToGround("bank", in, 100);         // 529 W at 230 V
        guard = circuit.watchdogs().volts(in, -10, 250)
            .onTrip(trip -> bank.setOhms(Double.POSITIVE_INFINITY)); // a trip only calls you: open the bank
        probe = circuit.load("probe");
        circuit.resistorToGround("probe bleed", probe, 1e6);        // a voltmeter's resistance
        if (probeAt != null) link = circuit.connect(probe, PortRef.of(probeAt, Direction.UP, PortKind.POWER));
    }
    // doc:end build

    private PortLink link;

    // doc:begin reading
    /** The probe's reading, or why there is none (PortStatus: NO_NODE, NOT_LOADED, WRONG_KIND...). */
    public String probeReading() {
        if (link == null) return "no probe";
        if (link.status() != PortStatus.CONNECTED) return link.status().name();
        return mods.eln.api.Meters.volts(probe.volts());
    }
    // doc:end reading
}
