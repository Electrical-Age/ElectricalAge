package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.voxel.CablePart;
import mods.eln.node.voxel.VoxelNode;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Small devices as voxel devices at their drawn size (Baughn: probes and relays took their whole face, in the way of
 * cables round them): each goes off centre on a face, its pins (one voxel past its body, which follows its model) touch a
 * voxel cable of their kind, and it works through them; the harness round-trips every node. A cable laid from a pin to
 * the block's edge meets a six-node device in the next block by EA's edge rule.
 */
public final class VoxelDeviceCases {
    private VoxelDeviceCases() {
    }

    static final int SIGNAL = CablePart.tierOf("Signal Cable"), LV = CablePart.tierOf("Low Voltage Cable"),
        THERMAL = CablePart.tierOf("Copper Thermal Cable");
    static final int FLOOR = 0, NORTH_WALL = 2;   // (cable faces, 3D values)

    public static void addTo(List<TestCase> cases) {
        cases.add(new GeometryCase());
        cases.add(new RelaysCase());
        cases.add(new ProbesCase());
        cases.add(new ThermalProbesCase());
        cases.add(new SignalDevicesCase());
        cases.add(new PowerDevicesCase());
        cases.add(new InlineCase());
        cases.add(new TapCase());
        cases.add(new SlotPinsCase());
        cases.add(new FreeEndCase());
        cases.add(new MetersChargersAlarmsCase());
        cases.add(new InLineChainCase());
        cases.add(new WrenchInLineCase());
        cases.add(new PassivesInLineCase());
        cases.add(new DiodeRigCase());
        cases.add(new RheostatThermistorCase());
        cases.add(new ProbesLeaveNoTraceCase());
    }

    static int id(String name) {
        return Ids.six(name);
    }

    static mods.eln.node.six.SixNodeDescriptor desc(String name) {
        return mods.eln.Eln.sixNodeItem.getDescriptor(id(name));
    }

    static net.minecraft.world.entity.player.Player player(TestContext c) {
        return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(c.level());
    }

    /** The edge of face that points toward dir. */
    static LRDU toward(Direction face, Direction dir) {
        for (LRDU l : LRDU.values()) if (face.applyLRDU(l) == dir) return l;
        throw new IllegalStateException(dir + " is not along " + face);
    }

    /** A voxel block at p (empty). */
    static VoxelNode block(TestContext c, BlockPos p) {
        return CableCases.ChipsPackedCase.block(c, p);
    }

    /** Places the named device off centre at (ox, oy, oz) on face of v, its front toward dir, by the placement rules:
     *  the element, or an exception saying why not. */
    static SixNodeElement put(TestContext c, VoxelNode v, String name, Direction face, int ox, int oy, int oz, Direction dir) {
        var d = desc(name);
        LRDU f = toward(face, dir);
        int slot = v.freeSlot();
        String err = v.placeErrorAt(d.newItemStack(), slot, face, ox, oy, oz, f);
        if (err != null) throw new IllegalStateException(name + " off centre at " + ox + "," + oy + "," + oz + ": " + err);
        if (!v.createSubBlockAt(d.newItemStack(), slot, face, ox, oy, oz, player(c))) throw new IllegalStateException(name + " not created");
        var e = v.getElement(slot);
        e.front = f;
        v.reconnect();
        v.setNeedPublish(true);
        return e;
    }

    /** The tip of e's pin toward dir (block voxels), or null. */
    static int[] tip(VoxelNode v, SixNodeElement e, Direction dir) {
        for (int[] t : v.pinTips(e.slot))
            if (e.side.applyLRDU(LRDU.fromInt(t[0])) == dir) return new int[]{t[1], t[2], t[3]};
        return null;
    }

    /** A cable of tier on cable face `face` (3D value) from e's pin toward dir straight to the block's edge. */
    static void cableOut(TestContext c, VoxelNode v, BlockPos p, SixNodeElement e, Direction dir, int tier, int face) {
        int[] t = tip(v, e, dir);
        if (t == null) throw new IllegalStateException(e.sixNodeElementDescriptor.name + " has no pin toward " + dir);
        int[] end = t.clone();
        int a = dir.isY() ? 1 : dir == Direction.XN || dir == Direction.XP ? 0 : 2;
        boolean pos = dir == Direction.XP || dir == Direction.YP || dir == Direction.ZP;
        end[a] = pos ? 16 : 0;
        CableCases.voxel(c, p, List.of(new CablePart(tier, face, new int[]{t[0], t[1], t[2], end[0], end[1], end[2]})));
    }

    /** Is e's pin toward dir linked to something (a cable of its kind)? */
    static boolean linked(SixNodeElement e, Direction dir) {
        return e.slotMask.get(e.side.getLRDUGoingTo(dir));
    }

    static double u(SixNodeElement e, Direction dir) {
        var l = e.getElectricalLoad(e.side.getLRDUGoingTo(dir));
        return l == null ? Double.NaN : l.getU();
    }

    /** A six-node Signal Source or Electrical Source at p on its floor, at u volts. */
    static mods.eln.sixnode.electricalsource.ElectricalSourceElement source(TestContext c, int damage, BlockPos p, double u) {
        var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(damage, p);
        s.networkUnserialize(c.stream(o -> {
            o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            o.writeFloat((float) u);
        }));
        return s;
    }

    /** A six-node Power Resistor at p, terminals along dir's axis, n core items (Wp9aCases.resistor). */
    static mods.eln.sixnode.resistor.ResistorElement resistor(TestContext c, BlockPos p, Direction dir, int n) {
        var r = (mods.eln.sixnode.resistor.ResistorElement) c.placeSix(Wp9aCases.POWER_RESISTOR, p);
        Wp9aCases.orient(r, LRDU::right, dir);
        r.getInventory().setItem(0, Wp9aCases.coal(n));
        r.getInventory().setChanged();
        return r;
    }

    static String fmt(String f, Object... a) {
        return String.format(java.util.Locale.ROOT, f, a);
    }

    /**
     * Every device converted here: placed anywhere, a click in the middle of a free face still puts it in the face's own
     * slot (the old placement, ports at the face's edges); its body follows its drawn model (logged: the body box and reach
     * of each, centred, front north); lopsided models keep their pins at the model's edges (a Voltage Probe's signal
     * pin 6 voxels from its centre on the long side, its cable pin 4 on the short side); the relays at their half size.
     */
    static final class GeometryCase implements TestCase {
        static final String[] FLOOR_DEVICES = {"Low Voltage Relay", "Medium Voltage Relay", "High Voltage Relay", "Very High Voltage Relay",
            "Signal Relay", "Electrical Probe", "Voltage Probe", "Thermal Probe", "Temperature Probe", "Wireless Signal Receiver",
            "Wireless Signal Transmitter", "Wireless Signal Repeater", "Electrical Timer", "LED vuMeter", "Signal Switch with LED",
            "Ground Cable", "Hub", "Energy Meter", "Advanced Energy Meter", "Weak 48V Battery Charger", "48V Battery Charger",
            "230V Battery Charger", "Nuclear Alarm", "Standard Alarm", "10A Diode", "25A Diode", "Signal Diode", "Power Capacitor",
            "Power Inductor", "Power Resistor", "Rheostat", "Thermistor", "Electrical Fuse Holder"};
        static final String[] WALL_DEVICES = {"48V Power Socket", "230V Power Socket", "Electrical Anemometer Sensor"};
        String all = "", middle = "", lopsided = "", relays = "", stayed = "";
        boolean anywhereOk, middleOk, lopsidedOk, relaysOk, stayedOk;

        public String name() {
            return "small devices as voxel devices: size and pins";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        public void build(TestContext c) {
            var yn = Direction.YN;
            var zn = Direction.ZN;
            StringBuilder log = new StringBuilder("voxel devices (body box x, y, z; reach toward each edge, voxels), centred, front north:");
            StringBuilder mid = new StringBuilder();
            anywhereOk = middleOk = true;
            for (String n : FLOOR_DEVICES) anywhereOk &= desc(n).placeAnywhere();
            for (String n : WALL_DEVICES) anywhereOk &= desc(n).placeAnywhere();
            // the ones that stay as they are: the analog vuMeter, the power switches, the redstone converters (Baughn)
            StringBuilder st = new StringBuilder();
            stayedOk = true;
            for (String n : new String[]{"Analog vuMeter", "Low Voltage Switch", "Redstone-to-Voltage Converter"}) {
                var d = mods.eln.Eln.sixNodeItem.all().values().stream().filter(x -> x.name.equals(n)).findFirst().orElse(null);
                st.append(n).append('=').append(d == null ? "missing" : d.placeAnywhere()).append("; ");
                stayedOk &= d != null && !d.placeAnywhere();
            }
            stayed = st.toString();
            int i = 0;
            for (String n : FLOOR_DEVICES) {
                BlockPos p = c.at(i % 8 * 2, 1, i / 8 * 2);
                c.floor(p.below());
                var v = block(c, p);
                // a click in the middle of the free floor: the face's own slot
                String err = v.placeAnywhere(desc(n).newItemStack(), yn, 0.5, 0, 0.5, player(c));
                boolean ok = err == null && v.getElement(yn) != null && v.getElement(yn).sixNodeElementDescriptor.name.equals(n);
                middleOk &= ok;
                if (!ok) mid.append(n).append(": ").append(err).append("; ");
                i++;
            }
            for (String n : WALL_DEVICES) {
                BlockPos p = c.at(i % 8 * 2, 1, i / 8 * 2 + 1);
                c.floor(p.below());
                c.floor(p.north());
                var v = block(c, p);
                String err = v.placeAnywhere(desc(n).newItemStack(), zn, 0.5, 0.5, 0, player(c));
                boolean ok = err == null && v.getElement(zn) != null;
                middleOk &= ok;
                if (!ok) mid.append(n).append(": ").append(err).append("; ");
                i++;
            }
            middle = mid.toString();
            // every one's body and pins, centred in a slot past the faces (computed, not placed)
            for (String n : FLOOR_DEVICES) log.append(' ').append(n).append(describe(desc(n), yn, toward(yn, zn))).append(';');
            for (String n : WALL_DEVICES) log.append(' ').append(n).append(describe(desc(n), zn, desc(n).getFrontFromPlace(zn, player(c)))).append(';');
            all = log.toString();
            // a Voltage Probe, front north: its signal pin on the model's long side (5 px: tip 6 voxels out), its cable
            // pin on the short side (3 px: tip 4 voxels out); its body 4 voxels across the other way (2 px each side)
            var vp = desc("Voltage Probe");
            int[] e = vp.bodyExtents(yn, toward(yn, zn));
            lopsided = fmt("Voltage Probe reach north %d, south %d, west %d, east %d", e[toward(yn, zn).toInt()], e[toward(yn, Direction.ZP).toInt()],
                e[toward(yn, Direction.XN).toInt()], e[toward(yn, Direction.XP).toInt()]);
            lopsidedOk = e[toward(yn, zn).toInt()] == 5 && e[toward(yn, Direction.ZP).toInt()] == 3 && e[toward(yn, Direction.XN).toInt()] == 3
                && e[toward(yn, Direction.XP).toInt()] == 3;
            // turned east, the long side goes east
            int[] t = vp.bodyExtents(yn, toward(yn, Direction.XP));
            lopsided += fmt("; turned east: east %d, west %d", t[toward(yn, Direction.XP).toInt()], t[toward(yn, Direction.XN).toInt()]);
            lopsidedOk &= t[toward(yn, Direction.XP).toInt()] == 5 && t[toward(yn, Direction.XN).toInt()] == 3;
            // the relays: 8 voxels long (their contacts along it), 5 wide (their gate across), at half scale
            var r = desc("Low Voltage Relay");
            int[] re = r.bodyExtents(yn, toward(yn, zn));
            relays = fmt("LV relay, gate north: north %d, south %d, west %d, east %d; footprint %d,%d", re[toward(yn, zn).toInt()], re[toward(yn, Direction.ZP).toInt()],
                re[toward(yn, Direction.XN).toInt()], re[toward(yn, Direction.XP).toInt()], r.footprint()[0], r.footprint()[1]);
            relaysOk = re[toward(yn, zn).toInt()] == 3 && re[toward(yn, Direction.ZP).toInt()] == 3 && re[toward(yn, Direction.XN).toInt()] == 5
                && re[toward(yn, Direction.XP).toInt()] == 5;
        }

        static String describe(mods.eln.node.six.SixNodeDescriptor d, Direction side, LRDU front) {
            int[] b = VoxelNode.bodyBox(d, side, front, 0, 0, 0);
            int[] e = d.bodyExtents(side, front);
            return fmt("=[%d..%d, %d..%d, %d..%d] reach L%d R%d D%d U%d", b[0], b[3], b[1], b[4], b[2], b[5], e[0], e[1], e[2], e[3]);
        }

        public void measure(TestContext c) {
            c.line(all);
            c.check("voxel devices: each converted device goes anywhere; the analog vuMeter, power switches and redstone converters don't", anywhereOk && stayedOk, stayed);
            c.check("voxel devices: a click in the middle of a free face puts each in the face's own slot (as before)", middleOk, middle);
            c.check("voxel devices: a lopsided model's body follows it, turned with its front", lopsidedOk, lopsided);
            c.check("voxel devices: a relay's body is its half-size model (contacts along, gate across)", relaysOk, relays);
        }
    }

    /**
     * Relays off centre, gate north: an LVU source west, a Power Resistor east (then ground), a Signal Source north on the
     * gate pin. Gate at SVU: the LV relay closes and the resistor carries the current; gate at 0 V: none. A Signal Relay
     * at SVU closes: its far contact at the signal source's voltage.
     */
    static final class RelaysCase implements TestCase {
        mods.eln.sixnode.electricalrelay.ElectricalRelayElement on, off, sig;
        mods.eln.sixnode.resistor.ResistorElement rOn, rOff;
        VoxelNode vOn;

        public String name() {
            return "relays as voxel devices";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        mods.eln.sixnode.electricalrelay.ElectricalRelayElement row(TestContext c, String relay, int z, double gate, boolean power) {
            for (int x = 0; x < 5; x++) c.floor(c.at(x, 0, z));
            c.floor(c.at(1, 0, z - 1));
            BlockPos p = c.at(1, 1, z);
            var v = block(c, p);
            var e = (mods.eln.sixnode.electricalrelay.ElectricalRelayElement) put(c, v, relay, Direction.YN, 0, 0, 2, Direction.ZN);
            int tier = power ? LV : SIGNAL;
            cableOut(c, v, p, e, Direction.XN, tier, FLOOR);
            cableOut(c, v, p, e, Direction.XP, tier, FLOOR);
            cableOut(c, v, p, e, Direction.ZN, SIGNAL, FLOOR);
            source(c, power ? Wp9aCases.SOURCE : Wp9aCases.SIGNAL_SOURCE, c.at(0, 1, z), power ? mods.eln.Eln.LVU : mods.eln.Eln.SVU);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(1, 1, z - 1), gate);
            if (z == 1) vOn = v;
            return e;
        }

        public void build(TestContext c) {
            on = row(c, "Low Voltage Relay", 1, mods.eln.Eln.SVU, true);
            rOn = resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            off = row(c, "Low Voltage Relay", 4, 0, true);
            rOff = resistor(c, c.at(2, 1, 4), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 4));
            sig = row(c, "Signal Relay", 7, mods.eln.Eln.SVU, false);
        }

        public void measure(TestContext c) {
            int[] w = tip(vOn, on, Direction.XN), e = tip(vOn, on, Direction.XP), n = tip(vOn, on, Direction.ZN);
            c.check("relay: pins one voxel past its half-size body (contacts at x 2 and 14, gate at z 6; centre 8,10)",
                w != null && e != null && n != null && w[0] == 2 && w[2] == 10 && e[0] == 14 && n[2] == 6 && n[0] == 8,
                java.util.Arrays.toString(w) + " " + java.util.Arrays.toString(e) + " " + java.util.Arrays.toString(n));
            c.check("relay: its three pins are linked to the cables", linked(on, Direction.XN) && linked(on, Direction.XP) && linked(on, Direction.ZN),
                "mask " + on.slotMask.mask);
            c.check("relay: gate SVU on its pin: closed", on.getSwitchState(), fmt("gate %.2f V", u(on, Direction.ZN)));
            double i = Wp9aCases.current(rOn);
            Wp9cCases.within(c, "relay: closed, the resistor east of it carries the LVU source's current [A]", i, mods.eln.Eln.LVU / rOn.nominalRs, 0.05 * mods.eln.Eln.LVU / rOn.nominalRs);
            c.check("relay: gate 0 V: open, no current", !off.getSwitchState() && Math.abs(Wp9aCases.current(rOff)) < 1e-3,
                fmt("%s, %.4f A", off.getSwitchState(), Wp9aCases.current(rOff)));
            c.check("relay: a Signal Relay closes on signal cables", sig.getSwitchState() && Math.abs(u(sig, Direction.XP) - mods.eln.Eln.SVU) < 0.2,
                fmt("%s, far contact %.2f V", sig.getSwitchState(), u(sig, Direction.XP)));
        }
    }

    /**
     * Probes off centre, signal pin north. An Electrical Probe between a half-LVU source (west) and a Power Resistor (east) to
     * ground: its signal pin's cable at its voltage's share of SVU over its default 0..LVU range. A Voltage Probe whose
     * cable pin (south) is on a 0.6 LVU source.
     */
    static final class ProbesCase implements TestCase {
        SixNodeElement probe, volt;
        VoxelNode vProbe, vVolt;
        BlockPos pProbe, pVolt;

        public String name() {
            return "probes as voxel devices";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 5; x++) for (int z = 0; z < 7; z++) c.floor(c.at(x, 0, z));
            pProbe = c.at(1, 1, 1);
            vProbe = block(c, pProbe);
            probe = put(c, vProbe, "Electrical Probe", Direction.YN, 0, 0, 2, Direction.ZN);
            cableOut(c, vProbe, pProbe, probe, Direction.XN, LV, FLOOR);
            cableOut(c, vProbe, pProbe, probe, Direction.XP, LV, FLOOR);
            cableOut(c, vProbe, pProbe, probe, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 0.5 * mods.eln.Eln.LVU);
            resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            pVolt = c.at(1, 1, 4);
            vVolt = block(c, pVolt);
            volt = put(c, vVolt, "Voltage Probe", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vVolt, pVolt, volt, Direction.ZP, LV, FLOOR);
            cableOut(c, vVolt, pVolt, volt, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SOURCE, c.at(1, 1, 5), 0.6 * mods.eln.Eln.LVU);
        }

        static double cableU(VoxelNode v, int[] t, Direction dir) {
            int[] q = t.clone();
            int a = dir == Direction.XN || dir == Direction.XP ? 0 : 2;
            q[a] += dir == Direction.XP || dir == Direction.ZP ? 0 : -1;
            Object[] k = v.cableAt(q[0], q[1], q[2]);
            return k == null || k[0] == null ? Double.NaN : ((mods.eln.sim.ElectricalLoad) k[0]).getU();
        }

        public void measure(TestContext c) {
            int[] s = tip(vProbe, probe, Direction.ZN), w = tip(vProbe, probe, Direction.XN);
            // its model: 8 px across its contacts, 4 px toward its signal side and 2 px behind (centre z 10)
            c.check("probe: pins at its model's edges (signal at z 4, contacts at x 2 and 14)", s != null && w != null && s[2] == 4 && w[0] == 2,
                java.util.Arrays.toString(s) + " " + java.util.Arrays.toString(w));
            c.check("probe: its three pins linked", linked(probe, Direction.XN) && linked(probe, Direction.XP) && linked(probe, Direction.ZN), "mask " + probe.slotMask.mask);
            double a = u(probe, Direction.XN), out = u(probe, Direction.ZN);
            Wp9cCases.within(c, fmt("probe: the signal on its pin's cable is %.2f V / LVU of SVU [V]", a), cableU(vProbe, s, Direction.ZN), a / mods.eln.Eln.LVU * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
            c.check("probe: it sees the source through its pin (> 0.45 LVU)", a > 0.45 * mods.eln.Eln.LVU, fmt("%.2f V, out %.2f V", a, out));
            int[] f = tip(vVolt, volt, Direction.ZN), b = tip(vVolt, volt, Direction.ZP);
            c.check("voltage probe: its signal pin 6 voxels out on the long side, its cable pin 4 on the short (centre 10,8)",
                f != null && b != null && f[2] == 2 && b[2] == 12 && f[0] == 10, java.util.Arrays.toString(f) + " " + java.util.Arrays.toString(b));
            Wp9cCases.within(c, "voltage probe: 0.6 LVU on its cable pin -> 0.6 SVU on its signal pin's cable [V]", cableU(vVolt, f, Direction.ZN), 0.6 * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
        }
    }

    /**
     * The thermal pins (none had used them): a Thermal Probe off centre, its two thermal pins on Copper Thermal Cables,
     * the west one held at 30 C above ambient: the probe reads it, and its signal pin carries it. A Temperature Probe on
     * a hot LV cable (its back pin electrical, the cable's thermal load) reads it; one with a thermal cable in its slot
     * (its back pin now thermal) on the same kind of hot LV cable doesn't join it, and doesn't break; one with a thermal
     * cable in its slot on a hot thermal cable reads that.
     */
    static final class ThermalProbesCase implements TestCase {
        static final double HOT = 30;
        SixNodeElement thermal, onLv, slotOnLv, slotOnThermal;
        VoxelNode vThermal;
        final List<mods.eln.sim.ThermalLoad> bodies = new ArrayList<>();
        String err = "";

        public String name() {
            return "thermal and temperature probes as voxel devices";
        }

        public int width() {
            return 8;
        }

        public int depth() {
            return 3;
        }

        public int ticks() {
            return 200;
        }

        /** A body held at HOT, joined to the cable taking voxel q of v (slow: an electrical cable's thermal load). */
        void heat(VoxelNode v, int[] q, boolean slow) {
            Object[] k = v.cableAt(q[0], q[1], q[2]);
            if (k == null || k[1] == null) {
                err += "no thermal load at " + java.util.Arrays.toString(q) + "; ";
                return;
            }
            var body = new mods.eln.sim.ThermalLoad(HOT, 1e9, 1e-3, 1e9);
            if (slow) body.setAsSlow();
            else body.setAsFast();
            mods.eln.Eln.simulator.addThermalLoad(body);
            mods.eln.Eln.simulator.addThermalConnection(new mods.eln.sim.ThermalConnection(body, (mods.eln.sim.ThermalLoad) k[1]));
            bodies.add(body);
        }

        static int[] outside(int[] t, Direction dir) {
            int[] q = t.clone();
            int a = dir == Direction.XN || dir == Direction.XP ? 0 : 2;
            q[a] += dir == Direction.XP || dir == Direction.ZP ? 0 : -1;
            return q;
        }

        SixNodeElement temperature(TestContext c, int x, boolean thermalSlot, int tier) {
            BlockPos p = c.at(x, 1, 1);
            var v = block(c, p);
            var e = put(c, v, "Temperature Probe", Direction.YN, 2, 0, 0, Direction.ZN);
            if (thermalSlot) {
                ((mods.eln.sixnode.thermalsensor.ThermalSensorElement) e).getInventory()
                    .setItem(mods.eln.sixnode.thermalsensor.ThermalSensorContainer.cableSlotId, mods.eln.Eln.sixNodeItem.getDescriptor(Wp9bCases.THERMAL_CABLE).newItemStack());
                ((mods.eln.sixnode.thermalsensor.ThermalSensorElement) e).getInventory().setChanged();
            }
            cableOut(c, v, p, e, Direction.ZP, tier, FLOOR);
            heat(v, outside(tip(v, e, Direction.ZP), Direction.ZP), tier != THERMAL);
            return e;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 8; x++) c.floor(c.at(x, 0, 1));
            BlockPos p = c.at(1, 1, 1);
            vThermal = block(c, p);
            thermal = put(c, vThermal, "Thermal Probe", Direction.YN, 0, 0, 2, Direction.ZN);
            cableOut(c, vThermal, p, thermal, Direction.XN, THERMAL, FLOOR);
            cableOut(c, vThermal, p, thermal, Direction.XP, THERMAL, FLOOR);
            cableOut(c, vThermal, p, thermal, Direction.ZN, SIGNAL, FLOOR);
            heat(vThermal, outside(tip(vThermal, thermal, Direction.XN), Direction.XN), false);
            onLv = temperature(c, 3, false, LV);
            slotOnLv = temperature(c, 5, true, LV);
            slotOnThermal = temperature(c, 7, true, THERMAL);
        }

        static double t(SixNodeElement e, Direction dir) {
            var l = e.getThermalLoad(e.side.getLRDUGoingTo(dir));
            return l == null ? Double.NaN : l.Tc;
        }

        public void measure(TestContext c) {
            c.check("thermal probe: heated", err.isEmpty(), err);
            c.check("thermal probe: both thermal pins and its signal pin linked", linked(thermal, Direction.XN) && linked(thermal, Direction.XP)
                && linked(thermal, Direction.ZN), "mask " + thermal.slotMask.mask);
            double tc = t(thermal, Direction.XN);
            c.check(fmt("thermal probe: reads the hot thermal cable (%.0f C above ambient) through its pin", HOT), tc > 0.8 * HOT && tc <= HOT + 1e-6, fmt("%.2f C", tc));
            Wp9cCases.within(c, "thermal probe: its signal pin at T / 50 C of SVU (its default 0..50 C range) [V]", u(thermal, Direction.ZN),
                tc / 50 * mods.eln.Eln.SVU, 0.05);
            double a = t(onLv, Direction.ZP), b = t(slotOnLv, Direction.ZP), d = t(slotOnThermal, Direction.ZP);
            c.check("temperature probe: on a hot LV cable, reads it through its back pin", linked(onLv, Direction.ZP) && a > 0.8 * HOT && a <= HOT + 1e-6,
                fmt("linked %s, %.2f C, mask %d", linked(onLv, Direction.ZP), a, onLv.slotMask.mask));
            c.check("temperature probe: a thermal cable in its slot, on an LV cable: its thermal pin doesn't join the LV cable",
                !linked(slotOnLv, Direction.ZP) && Math.abs(b) < 1, fmt("linked %s, %.2f C", linked(slotOnLv, Direction.ZP), b));
            c.check("temperature probe: a thermal cable in its slot, on a hot thermal cable: reads it", linked(slotOnThermal, Direction.ZP) && d > 0.8 * HOT, fmt("%.2f C", d));
            for (var h : bodies) mods.eln.Eln.simulator.removeThermalLoad(h);
        }
    }

    /**
     * Signal devices off centre: a Wireless Signal Transmitter fed 0.7 SVU on its pin and a Receiver whose pin's cable carries
     * it; a Repeater; an Electrical Timer whose input pin is held high (its output pin's cable high); a LED vuMeter and a
     * Signal Switch with LED (closed) on a Signal Source.
     */
    static final class SignalDevicesCase implements TestCase {
        SixNodeElement tx, rx, rep, timer, vu, sw;
        VoxelNode vRx, vTimer;

        public String name() {
            return "signal devices as voxel devices";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 14; x++) for (int z = 0; z < 4; z++) c.floor(c.at(x, 0, z));
            String channel = "eln voxel " + System.nanoTime();
            // transmitter: its input pin north, a 0.7 SVU signal source beyond
            BlockPos pt = c.at(1, 1, 2);
            var vt = block(c, pt);
            tx = put(c, vt, "Wireless Signal Transmitter", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vt, pt, tx, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(1, 1, 1), 0.7 * mods.eln.Eln.SVU);
            tx.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement.setChannelId);
                o.writeUTF(channel);
            }));
            BlockPos pr = c.at(4, 1, 2);
            vRx = block(c, pr);
            rx = put(c, vRx, "Wireless Signal Receiver", Direction.YN, -2, 0, 0, Direction.ZN);
            cableOut(c, vRx, pr, rx, Direction.ZN, SIGNAL, FLOOR);
            rx.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxElement.setChannelId);
                o.writeUTF(channel);
            }));
            BlockPos pp = c.at(6, 1, 2);
            var vp = block(c, pp);
            rep = put(c, vp, "Wireless Signal Repeater", Direction.YN, 0, 0, -2, Direction.XP);
            cableOut(c, vp, pp, rep, Direction.XP, SIGNAL, FLOOR);
            // timer: input north on SVU, output south
            BlockPos pm = c.at(8, 1, 2);
            vTimer = block(c, pm);
            timer = put(c, vTimer, "Electrical Timer", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vTimer, pm, timer, Direction.ZN, SIGNAL, FLOOR);
            cableOut(c, vTimer, pm, timer, Direction.ZP, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(8, 1, 1), mods.eln.Eln.SVU);
            // LED vuMeter, its input north on 0.6 SVU
            BlockPos pv = c.at(10, 1, 2);
            var vv = block(c, pv);
            vu = put(c, vv, "LED vuMeter", Direction.YN, -2, 0, 0, Direction.ZN);
            cableOut(c, vv, pv, vu, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(10, 1, 1), 0.6 * mods.eln.Eln.SVU);
            // LED switch, closed: north on SVU, its south contact follows
            BlockPos ps = c.at(12, 1, 2);
            var vs = block(c, ps);
            sw = put(c, vs, "Signal Switch with LED", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vs, ps, sw, Direction.ZN, SIGNAL, FLOOR);
            cableOut(c, vs, ps, sw, Direction.ZP, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(12, 1, 1), mods.eln.Eln.SVU);
            ((mods.eln.sixnode.electricalswitch.ElectricalSwitchElement) sw).setSwitchState(true);
        }

        public void measure(TestContext c) {
            c.check("wireless: transmitter's and receiver's pins linked, repeater placed with its pin linked", linked(tx, Direction.ZN) && linked(rx, Direction.ZN)
                && linked(rep, Direction.XP), tx.slotMask.mask + " " + rx.slotMask.mask + " " + rep.slotMask.mask);
            Wp9cCases.within(c, "wireless: 0.7 SVU on the transmitter's pin, out of the receiver's pin [V]", u(rx, Direction.ZN), 0.7 * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
            Wp9cCases.within(c, "wireless: the receiver's pin's cable [V]", ProbesCase.cableU(vRx, tip(vRx, rx, Direction.ZN), Direction.ZN), 0.7 * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
            c.check("timer: both pins linked", linked(timer, Direction.ZN) && linked(timer, Direction.ZP), "mask " + timer.slotMask.mask);
            Wp9cCases.within(c, "timer: input pin held high: its output pin's cable high [V]",
                ProbesCase.cableU(vTimer, tip(vTimer, timer, Direction.ZP), Direction.ZP), mods.eln.Eln.SVU, 0.1);
            Wp9cCases.within(c, "LED vuMeter: 0.6 SVU on its pin [V]", u(vu, Direction.ZN), 0.6 * mods.eln.Eln.SVU, 0.01 * mods.eln.Eln.SVU);
            c.check("LED switch: both pins linked; closed, its south contact at the source's SVU", linked(sw, Direction.ZN) && linked(sw, Direction.ZP)
                && Math.abs(u(sw, Direction.ZP) - mods.eln.Eln.SVU) < 0.2, fmt("mask %d, %.2f V", sw.slotMask.mask, u(sw, Direction.ZP)));
        }
    }

    /**
     * Power devices off centre: a Ground Cable whose west pin's cable runs to a Power Resistor from a 50 V source (the
     * current flows); a Hub with no cables in its slots has no pins, and with LV cables in its west and east slots joins a
     * 50 V source to a resistor; power sockets go on walls only, off centre there their pin (down) meets a cable on the
     * wall to a source below; an Anemometer likewise on a wall.
     */
    static final class PowerDevicesCase implements TestCase {
        SixNodeElement ground, hub, socket, wind;
        mods.eln.sixnode.resistor.ResistorElement rGround, rHub;
        String floorSocket = "", floorWind = "", hubBare = "", openFloor = "";
        boolean openFloorOk;

        public String name() {
            return "power devices as voxel devices";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 12; x++) for (int z = 0; z < 5; z++) c.floor(c.at(x, 0, z));
            // ground: source 50 V, resistor, then the ground's west pin
            BlockPos pg = c.at(3, 1, 1);
            var vg = block(c, pg);
            ground = put(c, vg, "Ground Cable", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vg, pg, ground, Direction.XN, LV, FLOOR);
            rGround = resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            source(c, Wp9aCases.SOURCE, c.at(1, 1, 1), 50);
            // hub: placed bare (no pins), then LV cables in its west and east slots
            BlockPos ph = c.at(6, 1, 1);
            var vh = block(c, ph);
            hub = put(c, vh, "Hub", Direction.YN, 0, 0, 2, Direction.ZN);
            hubBare = "pins bare: " + vh.pinTips(hub.slot).size();
            for (Direction d : new Direction[]{Direction.XN, Direction.XP})
                hub.getInventory().setItem(mods.eln.sixnode.hub.HubContainer.cableSlotId + hub.side.getLRDUGoingTo(d).toInt(), Wp9aCases.lvCable());
            hub.getInventory().setChanged();
            cableOut(c, vh, ph, hub, Direction.XN, LV, FLOOR);
            cableOut(c, vh, ph, hub, Direction.XP, LV, FLOOR);
            source(c, Wp9aCases.SOURCE, c.at(5, 1, 1), 50);
            rHub = resistor(c, c.at(7, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(8, 1, 1));
            // sockets and the anemometer: refused on the floor
            var vf = block(c, c.at(10, 1, 1));
            floorSocket = String.valueOf(vf.placeAnywhere(desc("48V Power Socket").newItemStack(), Direction.YN, 0.3, 0, 0.5, player(c)));
            floorWind = String.valueOf(vf.placeAnywhere(desc("Electrical Anemometer Sensor").newItemStack(), Direction.YN, 0.3, 0, 0.5, player(c)));
            // a click on the floor in the open, as a player with voxel cables on: refused, no empty block left, item kept
            boolean conv = mods.eln.node.voxel.CableConverter.convertOnLoad;
            mods.eln.node.voxel.CableConverter.convertOnLoad = true;
            try {
                var wd = desc("Electrical Anemometer Sensor");
                BlockPos air = c.at(12, 1, 1);
                boolean placed = ((mods.eln.node.six.SixNodeItem) wd.parentItem).placeAt(wd.newItemStack(), player(c), c.level(), air, Direction.YN,
                    new double[]{0.3, 0, 0.5});
                openFloor = "placed " + placed + ", block " + c.level().getBlockState(air).getBlock().getName().getString();
                openFloorOk = !placed && c.level().getBlockState(air).isAir();
            } finally {
                mods.eln.node.voxel.CableConverter.convertOnLoad = conv;
            }
            // a socket on a north wall (y 2), its pin down; a source on the wall of the block below
            for (int y = 1; y <= 2; y++) c.floor(c.at(1, y, 3));
            var below = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) c.placeSix(Wp9aCases.SOURCE, c.at(1, 1, 4), Direction.ZN);
            below.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat(50f);
            }));
            BlockPos ps = c.at(1, 2, 4);
            var vs = block(c, ps);
            socket = put(c, vs, "48V Power Socket", Direction.ZN, 2, 0, 0, Direction.YN);
            cableOut(c, vs, ps, socket, Direction.YN, LV, NORTH_WALL);
            // an anemometer on a north wall, its pin wherever its front puts it
            c.floor(c.at(4, 2, 3));
            BlockPos pw = c.at(4, 2, 4);
            var vw = block(c, pw);
            var d = desc("Electrical Anemometer Sensor");
            LRDU f = d.getFrontFromPlace(Direction.ZN, player(c));
            int slot = vw.freeSlot();
            String err = vw.placeErrorAt(d.newItemStack(), slot, Direction.ZN, 2, -2, 0, f);
            if (err == null && vw.createSubBlockAt(d.newItemStack(), slot, Direction.ZN, 2, -2, 0, player(c))) {
                wind = vw.getElement(slot);
                for (int[] t : vw.pinTips(slot)) cableOut(c, vw, pw, wind, Direction.ZN.applyLRDU(LRDU.fromInt(t[0])), SIGNAL, NORTH_WALL);
            } else floorWind += "; wall: " + err;
        }

        public void measure(TestContext c) {
            c.check("ground: its pin linked; the resistor carries 50 V's current to it", linked(ground, Direction.XN)
                && Math.abs(Wp9aCases.current(rGround) - 50 / rGround.nominalRs) < 0.05 * 50 / rGround.nominalRs,
                fmt("mask %d, %.3f A", ground.slotMask.mask, Wp9aCases.current(rGround)));
            c.check("hub: no pins with empty slots; with LV cables in two slots, two pins that join source and resistor",
                hubBare.equals("pins bare: 0") && linked(hub, Direction.XN) && linked(hub, Direction.XP)
                    && Math.abs(Wp9aCases.current(rHub) - 50 / rHub.nominalRs) < 0.05 * 50 / rHub.nominalRs,
                fmt("%s, mask %d, %.3f A", hubBare, hub.slotMask.mask, Wp9aCases.current(rHub)));
            c.check("sockets and anemometer: not on a floor", floorSocket.startsWith("It can't go on that side") && floorWind.startsWith("It can't go on that side"),
                floorSocket + " / " + floorWind);
            c.check("anemometer: clicked on an open floor, refused without leaving an empty block", openFloorOk, openFloor);
            c.check("socket: off centre on a wall, its pin down on a cable", socket != null && linked(socket, Direction.YN), socket == null ? "-" : "mask " + socket.slotMask.mask);
            c.check("anemometer: off centre on a wall, its pin on a cable", wind != null && wind.slotMask.mask != 0, wind == null ? floorWind : "mask " + wind.slotMask.mask);
        }
    }

    static int cableLength(VoxelNode v) {
        int n = 0;
        for (var part : v.parts) if (part instanceof CablePart cp) n += cp.dropLength();
        return n;
    }

    /**
     * Devices placed onto a cable already laid (AdventurAgent: with a cable across the floor every place and facing was
     * refused). A straight LV run from a 50 V source to a resistor and ground; a click on the run puts the device in
     * line, turned so its contacts lie along the run, the cable cut where it goes and given back. An LV relay with its
     * gate on 50 V passes the current, one with its gate at 0 V doesn't, an Electrical Probe in line reads 50 V. The cut
     * keeps the cable: what is left plus what came back is the run's 16 voxels.
     */
    static final class InlineCase implements TestCase {
        SixNodeElement on, off, probe;
        mods.eln.sixnode.resistor.ResistorElement rOn, rOff, rProbe;
        VoxelNode vProbe;
        String accounts = "";
        boolean accountsOk = true;

        public String name() {
            return "devices placed in line on a cable";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        SixNodeElement row(TestContext c, String name, int z, Double gate) {
            for (int x = 0; x < 5; x++) for (int dz = -1; dz <= 1; dz++) c.floor(c.at(x, 0, z + dz));
            source(c, Wp9aCases.SOURCE, c.at(0, 1, z), 50);
            BlockPos p = c.at(1, 1, z);
            var v = block(c, p);
            CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            var stack = desc(name).newItemStack();
            double[] h = {0.55, 0, 0.5};
            var plan = mods.eln.node.voxel.DevicePlacer.plan(v, stack, v.freeSlot(), Direction.YN, h, player(c));
            String err = v.placeAnywhere(stack, Direction.YN, h[0], h[1], h[2], player(c));
            SixNodeElement e = null;
            for (int i = 6; i < mods.eln.node.six.SixNode.SLOTS; i++) if (v.getElement(i) != null) e = v.getElement(i);
            int left = cableLength(v), back = plan.cut() == null ? -1 : plan.cut().returned();
            accounts += fmt("%s: %s, cut %s, left %d + back %d; ", name, err, plan.cut() != null, left, back);
            accountsOk &= err == null && e != null && plan.cut() != null && left + back == 16 && back > 0;
            if (e == null) return null;
            Direction front = e.side.applyLRDU(e.front);
            cableOut(c, v, p, e, front, SIGNAL, FLOOR);
            if (gate != null) {
                BlockPos g = front == Direction.ZN ? c.at(1, 1, z - 1) : c.at(1, 1, z + 1);
                source(c, Wp9aCases.SIGNAL_SOURCE, g, gate);
            }
            if (name.equals("Electrical Probe")) vProbe = v;
            return e;
        }

        public void build(TestContext c) {
            on = row(c, "Low Voltage Relay", 1, mods.eln.Eln.SVU);
            rOn = resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            off = row(c, "Low Voltage Relay", 4, 0.0);
            rOff = resistor(c, c.at(2, 1, 4), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 4));
            probe = row(c, "Electrical Probe", 7, null);
            rProbe = resistor(c, c.at(2, 1, 7), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 7));
        }

        public void measure(TestContext c) {
            c.check("in line: each placed on the run, the cable cut and its cut-out length given back (kept: 16 voxels)", accountsOk, accounts);
            if (on == null || off == null || probe == null) return;
            c.check("in line: a relay's contacts lie along the run, both linked", linked(on, Direction.XN) && linked(on, Direction.XP)
                && linked(off, Direction.XN) && linked(off, Direction.XP), on.slotMask.mask + " " + off.slotMask.mask);
            double i = Wp9aCases.current(rOn);
            Wp9cCases.within(c, "in line: gate on 50 V, the relay passes the source's current to the resistor [A]", i, 50 / rOn.nominalRs, 0.05 * 50 / rOn.nominalRs);
            Wp9cCases.within(c, "in line: gate at 0 V, none [A]", Wp9aCases.current(rOff), 0, 1e-3);
            Wp9cCases.within(c, "in line: the run's current flows through an Electrical Probe too [A]", Wp9aCases.current(rProbe), 50 / rProbe.nominalRs,
                0.05 * 50 / rProbe.nominalRs);
            Direction f = probe.side.applyLRDU(probe.front);
            Wp9cCases.within(c, "in line: the probe reads 50 V on its signal pin's cable [V]",
                ProbesCase.cableU(vProbe, tip(vProbe, probe, f), f), mods.eln.Eln.SVU, 0.1);
        }
    }

    /**
     * A device with one pin of the cable's kind, aimed beside a cable, taps it: a Voltage Probe clicked just beside a
     * 50 V LV run reads it; a power socket clicked above a cable on a wall gets its pin onto it (AdventurAgent: always
     * "Too close"). A device with no pin of the cable's kind, clicked on it, is refused with what to do instead.
     */
    static final class TapCase implements TestCase {
        SixNodeElement volt, socket;
        String voltErr = "", rxErr = "", socketErr = "";

        public String name() {
            return "devices tapping a cable beside them";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        static SixNodeElement placed(VoxelNode v) {
            for (int i = 6; i < mods.eln.node.six.SixNode.SLOTS; i++) if (v.getElement(i) != null) return v.getElement(i);
            return null;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 4; x++) for (int z = 0; z < 9; z++) c.floor(c.at(x, 0, z));
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 50);
            BlockPos p = c.at(1, 1, 1);
            var v = block(c, p);
            // (the run a quarter of the way across: a Voltage Probe is 10 voxels tip to tip, more than half a block)
            CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 4, 16, 0, 4})));
            voltErr = String.valueOf(v.placeAnywhere(desc("Voltage Probe").newItemStack(), Direction.YN, 0.5, 0, 0.35, player(c)));
            volt = placed(v);
            BlockPos q = c.at(1, 1, 4);
            var w = block(c, q);
            CableCases.voxel(c, q, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            rxErr = String.valueOf(w.placeAnywhere(desc("Wireless Signal Receiver").newItemStack(), Direction.YN, 0.6, 0, 0.5, player(c)));
            // a wall to the north at y 2, an LV cable across it at y 4
            c.floor(c.at(1, 2, 6));
            BlockPos s = c.at(1, 2, 7);
            var u = block(c, s);
            CableCases.voxel(c, s, List.of(new CablePart(LV, NORTH_WALL, new int[]{0, 4, 0, 16, 4, 0})));
            socketErr = String.valueOf(u.placeAnywhere(desc("48V Power Socket").newItemStack(), Direction.ZN, 0.5, 0.45, 0, player(c)));
            socket = placed(u);
        }

        public void measure(TestContext c) {
            c.check("tap: a Voltage Probe clicked beside the run is placed with its cable pin on it", volt != null && voltErr.equals("null"), voltErr);
            if (volt != null) {
                Direction f = volt.side.applyLRDU(volt.front);
                Wp9cCases.within(c, "tap: it reads the run's 50 V [V]", u(volt, f), mods.eln.Eln.SVU, 0.1);
            }
            c.check("tap: a receiver (no pin of the cable's kind) clicked on the run is refused, told what to do",
                rxErr.contains("place the device first"), rxErr);
            c.check("tap: a power socket above a wall cable gets its pin onto it", socket != null && linked(socket, Direction.YN), socketErr
                + (socket == null ? "" : ", mask " + socket.slotMask.mask));
        }
    }

    /**
     * Pins that follow a device's slots (AdventurAgent: a Hub given two LV cables in its GUI showed no pins, and
     * `/elnlab inspect` on its block failed): a Hub off centre gets a pin for each slot filled and loses it when emptied,
     * and its block is published each time so clients draw them; a Temperature Probe given a thermal cable likewise.
     * `/elnlab inspect` works on a block with each converted device off centre. A Copper Thermal Cable kit aimed at a
     * Thermal Probe's signal pin ends at its nearest thermal pin, a signal kit at the signal pin.
     */
    static final class SlotPinsCase implements TestCase {
        String hub = "", temp = "", inspect = "", kit = "";
        boolean hubOk, tempOk, inspectOk = true, kitOk;

        public String name() {
            return "pins that follow a device's slots";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        static boolean published(VoxelNode v, Runnable change) {
            v.setNeedPublish(false);
            change.run();
            return v.getNeedPublish();
        }

        public void build(TestContext c) {
            for (int x = 0; x < 24; x++) for (int z = 0; z < 8; z++) c.floor(c.at(x, 0, z));
            BlockPos ph = c.at(1, 1, 1);
            var vh = block(c, ph);
            var h = put(c, vh, "Hub", Direction.YN, 0, 0, 2, Direction.ZN);
            var inv = h.getInventory();
            int bare = vh.pinTips(h.slot).size();
            boolean p1 = published(vh, () -> {
                inv.setItem(mods.eln.sixnode.hub.HubContainer.cableSlotId + 0, Wp9aCases.lvCable());
                inv.setItem(mods.eln.sixnode.hub.HubContainer.cableSlotId + 3, Wp9aCases.lvCable());
                inv.setChanged();
            });
            int two = vh.pinTips(h.slot).size();
            boolean p2 = published(vh, () -> {
                inv.setItem(mods.eln.sixnode.hub.HubContainer.cableSlotId + 3, net.minecraft.world.item.ItemStack.EMPTY);
                inv.setChanged();
            });
            int one = vh.pinTips(h.slot).size();
            hub = fmt("pins bare %d, slots 0 and 3 %d (published %s), slot 3 emptied %d (published %s)", bare, two, p1, one, p2);
            hubOk = bare == 0 && two == 2 && p1 && one == 1 && p2;
            // a temperature probe: its back pin's kind changes; the block is published
            BlockPos pt = c.at(3, 1, 1);
            var vt = block(c, pt);
            var t = put(c, vt, "Temperature Probe", Direction.YN, 2, 0, 0, Direction.ZN);
            boolean p3 = published(vt, () -> {
                t.getInventory().setItem(mods.eln.sixnode.thermalsensor.ThermalSensorContainer.cableSlotId,
                    mods.eln.Eln.sixNodeItem.getDescriptor(Wp9bCases.THERMAL_CABLE).newItemStack());
                t.getInventory().setChanged();
            });
            var back = t.side.getLRDUGoingTo(Direction.ZP);
            temp = fmt("published %s, back pin mask %x", p3, t.getConnectionMask(back));
            tempOk = p3 && t.getConnectionMask(back) == NodeBaseMasks.THERMAL;
            // /elnlab inspect on a block with each converted floor device off centre
            var src = c.level().getServer().createCommandSourceStack().withSuppressedOutput();
            int i = 0;
            for (String n : GeometryCase.FLOOR_DEVICES) {
                BlockPos p = c.at(i % 10 * 2, 1, 3 + i / 10 * 2);
                i++;
                var v = block(c, p);
                try {
                    put(c, v, n, Direction.YN, 2, 0, 0, Direction.ZN);
                    int lines = mods.eln.gametest.LabCommands.inspect(src, p);
                    if (lines < 3) {
                        inspectOk = false;
                        inspect += n + ": " + lines + " lines; ";
                    }
                } catch (RuntimeException e) {
                    inspectOk = false;
                    inspect += n + ": " + e + "; ";
                }
            }
            try {
                mods.eln.gametest.LabCommands.inspect(src, ph);
            } catch (RuntimeException e) {
                inspectOk = false;
                inspect += "hub: " + e;
            }
            // a thermal probe, front south (its signal pin there): kits aimed at that pin
            BlockPos pp = c.at(5, 1, 1);
            var vp = block(c, pp);
            var probe = put(c, vp, "Thermal Probe", Direction.YN, 0, 0, -2, Direction.ZP);
            int[] sig = tip(vp, probe, Direction.ZP), west = tip(vp, probe, Direction.XN), east = tip(vp, probe, Direction.XP);
            var hit = net.minecraft.world.phys.Vec3.atLowerCornerOf(pp).add((sig[0] + 0.5) / 16.0, 0.5 / 16, (sig[2] - 1.5) / 16.0);
            var th = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), pp, net.minecraft.core.Direction.UP, hit, false, new long[1][], THERMAL);
            var sg = mods.eln.node.voxel.CablePlacer.endAtForTests(c.level(), pp, net.minecraft.core.Direction.UP, hit, false, new long[1][], SIGNAL);
            long bx = pp.getX() * 16L, bz = pp.getZ() * 16L;
            kit = fmt("signal pin %s, thermal pins %s %s; thermal kit ends at %s, signal kit at %s", java.util.Arrays.toString(sig),
                java.util.Arrays.toString(west), java.util.Arrays.toString(east), th == null ? "-" : (th.x() - bx) + "," + (th.z() - bz),
                sg == null ? "-" : (sg.x() - bx) + "," + (sg.z() - bz));
            kitOk = th != null && sg != null && th.z() - bz == west[2] && (th.x() - bx == west[0] || th.x() - bx == east[0])
                && sg.x() - bx == sig[0] && sg.z() - bz == sig[2];
        }

        public void measure(TestContext c) {
            c.check("slot pins: a Hub's pins follow its slots, and clients are told", hubOk, hub);
            c.check("slot pins: a Temperature Probe's back pin turns thermal with a thermal cable in its slot, and clients are told", tempOk, temp);
            c.check("slot pins: /elnlab inspect works on every converted device off centre and a Hub", inspectOk, inspect);
            c.check("slot pins: a thermal kit ends at a thermal probe's thermal pin, a signal kit at its signal pin", kitOk, kit);
        }
    }

    /** Masks the checks compare with. */
    static final class NodeBaseMasks {
        static final int THERMAL = mods.eln.node.NodeBase.maskThermal;
    }

    /**
     * AdventurAgent's free end: an LV run from a block's west edge to voxel 8 along x, at z 8 on the floor. A Voltage
     * Probe aimed at seven spots past the end, out to the cell's edges, goes the same place each time, its LV pin on the end: the probe doesn't
     * fit beyond voxel 8 (10 voxels tip to tip), so the end is drawn back and the cable taken off comes back. A click on
     * the end of the cable puts a device on the floor the cable lies on, not on the open side clicked.
     */
    static final class FreeEndCase implements TestCase {
        final List<String> where = new ArrayList<>();
        String detail = "", face = "";
        boolean ok = true;

        public String name() {
            return "a device aimed past a cable's free end";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public void build(TestContext c) {
            // (the last two at the cell's edges, AdventurAgent: there the probe went beside the end)
            double[][] aims = {{9.5, 8}, {10.5, 8}, {11, 7.5}, {10, 9.5}, {12, 8.5}, {13, 1.2}, {15.5, 14.8}};
            for (int i = 0; i < aims.length; i++) {
                BlockPos p = c.at(1 + i % 4 * 2, 1, 1 + i / 4 * 3);
                c.floor(p.below());
                var v = block(c, p);
                CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 8, 0, 8})));
                var stack = desc("Voltage Probe").newItemStack();
                double[] h = {aims[i][0] / 16, 0.5 / 16, aims[i][1] / 16};
                var plan = mods.eln.node.voxel.DevicePlacer.plan(v, stack, v.freeSlot(), Direction.YN, h, player(c));
                String err = v.placeAnywhere(stack, Direction.YN, h[0], h[1], h[2], player(c));
                var e = TapCase.placed(v);
                int back = plan.cut() == null ? 0 : plan.cut().returned();
                String at = e == null ? "-" : e.ox + "," + e.oy + "," + e.oz + " " + e.front;
                where.add(at);
                Direction west = Direction.XN;
                int[] t = e == null ? null : tip(v, e, west);
                Object[] k = t == null ? null : v.cableAt(t[0] - 1, t[1], t[2]);
                boolean one = err == null && e != null && t != null && k != null && cableLength(v) + back == 8
                    && e.getConnectionMask(e.side.getLRDUGoingTo(west)) != 0;
                detail += fmt("aim %.1f,%.1f: %s, at %s, pin west %s, cable %d + back %d; ", aims[i][0], aims[i][1], err, at,
                    t == null ? "-" : java.util.Arrays.toString(t), cableLength(v), back);
                ok &= one;
            }
            // a click on the cable's east end (its open side) is a place on the floor
            BlockPos q = c.at(7, 1, 4);
            c.floor(q.below());
            var w = block(c, q);
            CableCases.voxel(c, q, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 8, 0, 8})));
            face = String.valueOf(w.placeFace(net.minecraft.core.Direction.EAST, 8.0 / 16, 0.5 / 16, 8.2 / 16));
        }

        public void measure(TestContext c) {
            c.check("free end: each aim puts the Voltage Probe's LV pin on the end, the cable kept", ok, detail);
            c.check("free end: the same place for every aim", where.stream().distinct().count() == 1, where.toString());
            c.check("free end: a click on the cable's end goes on the floor it lies on", face.equals("YN"), face);
        }
    }

    /**
     * Batch 2 (Baughn, #178): the energy meters, battery chargers and alarms as voxel devices. An Energy Meter put in line
     * on a 50 V run counts the current to a resistor; an Advanced Energy Meter off centre between cables does too; a 50V
     * Battery Charger put on a run (in line) and a Weak 50V and a 200V one off centre are fed through their pins; the two
     * alarms off centre see a Signal Source on their pin.
     */
    static final class MetersChargersAlarmsCase implements TestCase {
        SixNodeElement meter, adv, c50, cWeak, c200, nuclear, standard;
        mods.eln.sixnode.resistor.ResistorElement rMeter, rAdv;
        String placed = "";

        public String name() {
            return "energy meters, battery chargers and alarms as voxel devices";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        static void close(TestContext c, SixNodeElement m) {
            m.networkUnserialize(c.stream(o -> o.writeByte(mods.eln.sixnode.energymeter.EnergyMeterElement.clientToggleStateId)));
        }

        public void build(TestContext c) {
            for (int x = 0; x < 10; x++) for (int z = 0; z < 10; z++) c.floor(c.at(x, 0, z));
            // an Energy Meter clicked onto a run: in line
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 50);
            BlockPos pm = c.at(1, 1, 1);
            var vm = block(c, pm);
            CableCases.voxel(c, pm, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            placed += "meter: " + vm.placeAnywhere(desc("Energy Meter").newItemStack(), Direction.YN, 0.55, 0, 0.5, player(c)) + "; ";
            meter = TapCase.placed(vm);   // (in line: it starts closed)
            rMeter = resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            // an Advanced Energy Meter off centre, its pins west and east
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 4), 50);
            BlockPos pa = c.at(1, 1, 4);
            var va = block(c, pa);
            adv = put(c, va, "Advanced Energy Meter", Direction.YN, 0, 0, 2, Direction.XN);
            cableOut(c, va, pa, adv, Direction.XN, LV, FLOOR);
            cableOut(c, va, pa, adv, Direction.XP, LV, FLOOR);
            close(c, adv);
            rAdv = resistor(c, c.at(2, 1, 4), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 4));
            // a 200V charger off centre, fed from the west
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 7), 200);
            BlockPos p2 = c.at(1, 1, 7);
            var v2 = block(c, p2);
            c200 = put(c, v2, "230V Battery Charger", Direction.YN, 0, 0, 2, Direction.ZN);
            cableOut(c, v2, p2, c200, Direction.XN, mods.eln.node.voxel.CablePart.tierOf("Medium Voltage Cable"), FLOOR);
            // a 50V charger clicked onto a 50 V run
            source(c, Wp9aCases.SOURCE, c.at(4, 1, 1), 50);
            BlockPos p5 = c.at(5, 1, 1);
            var v5 = block(c, p5);
            CableCases.voxel(c, p5, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            placed += "charger: " + v5.placeAnywhere(desc("48V Battery Charger").newItemStack(), Direction.YN, 0.55, 0, 0.5, player(c));
            c50 = TapCase.placed(v5);
            // a weak one off centre, fed from the east
            BlockPos pw = c.at(5, 1, 4);
            var vw = block(c, pw);
            cWeak = put(c, vw, "Weak 48V Battery Charger", Direction.YN, 0, 0, 2, Direction.ZN);
            cableOut(c, vw, pw, cWeak, Direction.XP, LV, FLOOR);
            source(c, Wp9aCases.SOURCE, c.at(6, 1, 4), 50);
            // the alarms, their pin north on SVU
            BlockPos pn = c.at(5, 1, 8);
            var vn = block(c, pn);
            nuclear = put(c, vn, "Nuclear Alarm", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vn, pn, nuclear, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(5, 1, 7), mods.eln.Eln.SVU);
            BlockPos ps = c.at(8, 1, 8);
            var vs = block(c, ps);
            standard = put(c, vs, "Standard Alarm", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vs, ps, standard, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(8, 1, 7), mods.eln.Eln.SVU);
        }

        public void measure(TestContext c) {
            c.check("batch 2: an Energy Meter and a 50V charger clicked onto runs go in line", meter != null && c50 != null
                && linked(meter, Direction.XN) && linked(meter, Direction.XP) && linked(c50, Direction.XN) && linked(c50, Direction.XP), placed);
            Wp9cCases.within(c, "batch 2: the Energy Meter in line passes 50 V's current to the resistor [A]", Wp9aCases.current(rMeter),
                50 / rMeter.nominalRs, 0.05 * 50 / rMeter.nominalRs);
            double counted = meter == null ? 0 : (double) mods.eln.gametest.LabCommands.field(meter, "energyStack");
            // (its sign follows which way it faces the current: placed by the player's facing, a wrench turns it round)
            c.check("batch 2: and counts its energy", Math.abs(counted) > 10, fmt("%.2f J", counted));
            Wp9cCases.within(c, "batch 2: an Advanced Energy Meter off centre passes it too [A]", Wp9aCases.current(rAdv),
                50 / rAdv.nominalRs, 0.05 * 50 / rAdv.nominalRs);
            Wp9cCases.within(c, "batch 2: a 200V charger off centre fed through its west pin [V]", u(c200, Direction.XN), 200, 2);
            Wp9cCases.within(c, "batch 2: a 50V charger in line on the run [V]", c50 == null ? 0 : u(c50, Direction.XN), 50, 1);
            Wp9cCases.within(c, "batch 2: a weak 50V charger fed through its east pin [V]", u(cWeak, Direction.XP), 50, 1);
            Wp9cCases.within(c, "batch 2: the Nuclear Alarm sees SVU on its pin [V]", u(nuclear, Direction.ZN), mods.eln.Eln.SVU, 0.1);
            Wp9cCases.within(c, "batch 2: the Standard Alarm sees SVU on its pin [V]", u(standard, Direction.ZN), mods.eln.Eln.SVU, 0.1);
        }
    }

    /**
     * AdventurAgent's run, placed as a player does (the item's use on the cable, voxel cables on): a 50 V source, then a
     * floor LV run through five voxel blocks with an Energy Meter, an Advanced Energy Meter and a 50V Battery Charger put
     * in line in the first, third and fifth, then a resistor and ground. The meters start closed when put in line (they
     * opened the run: 25 mV past the first), so the resistor carries the current, both meters count it and the charger
     * has power.
     */
    static final class InLineChainCase implements TestCase {
        final SixNodeElement[] dev = new SixNodeElement[3];
        mods.eln.sixnode.resistor.ResistorElement r;
        String placed = "";

        public String name() {
            return "a run with meters and a charger put in line by a player";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        public void build(TestContext c) {
            for (int x = 0; x < 10; x++) c.floor(c.at(x, 0, 1));
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 50);
            for (int x = 1; x <= 5; x++) {
                block(c, c.at(x, 1, 1));
                CableCases.voxel(c, c.at(x, 1, 1), List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            }
            boolean conv = mods.eln.node.voxel.CableConverter.convertOnLoad;
            mods.eln.node.voxel.CableConverter.convertOnLoad = true;
            var player = player(c);
            try {
                String[] names = {"Energy Meter", "Advanced Energy Meter", "48V Battery Charger"};
                for (int i = 0; i < 3; i++) {
                    BlockPos p = c.at(1 + 2 * i, 1, 1);
                    var d = desc(names[i]);
                    var stack = d.newItemStack();
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                    var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(p).add(0.55, 1 / 16.0, 0.5),
                        net.minecraft.core.Direction.UP, p, false);
                    var res = d.parentItem.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                    var v = (VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level()));
                    dev[i] = TapCase.placed(v);
                    placed += names[i] + ": " + res + "; ";
                }
            } finally {
                mods.eln.node.voxel.CableConverter.convertOnLoad = conv;
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            r = resistor(c, c.at(6, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(7, 1, 1));
        }

        public void measure(TestContext c) {
            c.check("chain: the three were put in line by the item's use", dev[0] != null && dev[1] != null && dev[2] != null, placed);
            if (dev[0] == null || dev[1] == null || dev[2] == null) return;
            Wp9cCases.within(c, "chain: the resistor past them carries the source's current [A]", Wp9aCases.current(r), 50 / r.nominalRs, 0.05 * 50 / r.nominalRs);
            double e1 = (double) mods.eln.gametest.LabCommands.field(dev[0], "energyStack"), e2 = (double) mods.eln.gametest.LabCommands.field(dev[1], "energyStack");
            c.check("chain: both meters closed and counting", ((mods.eln.sixnode.energymeter.EnergyMeterElement) dev[0]).shunt.getState()
                && ((mods.eln.sixnode.energymeter.EnergyMeterElement) dev[1]).shunt.getState() && Math.abs(e1) > 10 && Math.abs(e2) > 10, fmt("%.1f J, %.1f J", e1, e2));
            Wp9cCases.within(c, "chain: the charger has power on its pins [V]", u(dev[2], Direction.XN), 50, 1);
            String w = String.valueOf(dev[2].getWaila());
            c.check("chain: the charger doesn't say it has no power in", !w.contains("none: run a cable"), w);
        }
    }

    /**
     * The wrench on a device in line (AdventurAgent: a quarter turn took an Energy Meter's pins off the run and cut it):
     * two Energy Meters put in line on one 50 V run, the second wrenched once. It turns straight round, its pins stay on
     * the run's ends, the run stays live and it counts with the opposite sign to the first. An off-centre meter not on a
     * cable turns a quarter as before.
     */
    static final class WrenchInLineCase implements TestCase {
        SixNodeElement first, turned, free;
        mods.eln.sixnode.resistor.ResistorElement r;
        String turn = "", freeTurn = "";

        public String name() {
            return "a wrench reverses a device in line";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        static void wrench(TestContext c, SixNodeElement e) {
            var pl = player(c);
            pl.setShiftKeyDown(true);
            mods.eln.server.PlayerKeys.set(pl, mods.eln.server.PlayerKeys.WRENCH, true);
            try {
                e.onBlockActivated(pl, e.side, 0.5f, 0.5f, 0.5f);
            } finally {
                mods.eln.server.PlayerKeys.set(pl, mods.eln.server.PlayerKeys.WRENCH, false);
                pl.setShiftKeyDown(false);
            }
        }

        public void build(TestContext c) {
            for (int x = 0; x < 10; x++) for (int z = 0; z < 5; z++) c.floor(c.at(x, 0, z));
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 50);
            for (int x = 1; x <= 2; x++) {
                block(c, c.at(x, 1, 1));
                CableCases.voxel(c, c.at(x, 1, 1), List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            }
            SixNodeElement[] m = new SixNodeElement[2];
            for (int i = 0; i < 2; i++) {
                var v = (VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(c.at(1 + i, 1, 1), c.level()));
                v.placeAnywhere(desc("Energy Meter").newItemStack(), Direction.YN, 0.55, 0, 0.5, player(c));
                m[i] = TapCase.placed(v);
            }
            first = m[0];
            turned = m[1];
            r = resistor(c, c.at(3, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(4, 1, 1));
            if (turned != null) {
                var before = turned.front;
                wrench(c, turned);
                turn = before + " -> " + turned.front;
            }
            // an off-centre meter on its own: a quarter turn
            var vf = block(c, c.at(1, 1, 3));
            free = put(c, vf, "Energy Meter", Direction.YN, 0, 0, 0, Direction.ZN);
            var b = free.front;
            wrench(c, free);
            freeTurn = b + " -> " + free.front;
        }

        public void measure(TestContext c) {
            c.check("wrench in line: the meter turns straight round", turned != null && turn.endsWith("-> " + turned.front)
                && turn.startsWith(turned.front.inverse() + " "), turn);
            if (first == null || turned == null) return;
            c.check("wrench in line: its pins stay on the run", linked(turned, Direction.XN) && linked(turned, Direction.XP), "mask " + turned.slotMask.mask);
            Wp9cCases.within(c, "wrench in line: the run stays live [A]", Wp9aCases.current(r), 50 / r.nominalRs, 0.05 * 50 / r.nominalRs);
            double e1 = (double) mods.eln.gametest.LabCommands.field(first, "energyStack"), e2 = (double) mods.eln.gametest.LabCommands.field(turned, "energyStack");
            c.check("wrench in line: it counts with the other sign", e1 * e2 < 0 && Math.abs(e2) > 10, fmt("%.1f J, %.1f J", e1, e2));
            String[] f = freeTurn.split(" -> ");
            c.check("wrench: a quarter turn, clockwise", f.length == 2 && LRDU.valueOf(f[0]).getNextClockwise() == LRDU.valueOf(f[1]), freeTurn);
        }
    }

    /**
     * Batch 3 (Baughn: "Inline parts: Yes, let's"): the in-line passives clicked onto a 50 V LV run, each in its own
     * block between the source and a 100 ohm resistor to ground. A Power Resistor and a Thermistor add their resistance;
     * of two 10A Diodes, the second wrenched round, one conducts and the other blocks; a Power Capacitor blocks the
     * steady current; a Power Inductor with copper and a core passes it; a fuse holder with an LV fuse passes it, one
     * without blocks; a Rheostat goes in line with its signal pin free.
     */
    static final class PassivesInLineCase implements TestCase {
        final java.util.Map<String, SixNodeElement> dev = new java.util.LinkedHashMap<>();
        final java.util.Map<String, mods.eln.sixnode.resistor.ResistorElement> load = new java.util.HashMap<>();
        String placed = "";

        public String name() {
            return "in-line passives put on a cable";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        SixNodeElement row(TestContext c, String key, String name, int i) {
            int x0 = i / 5 * 6, z = 1 + i % 5 * 3;
            for (int x = x0; x < x0 + 5; x++) c.floor(c.at(x, 0, z));
            source(c, Wp9aCases.SOURCE, c.at(x0, 1, z), 50);
            BlockPos p = c.at(x0 + 1, 1, z);
            var v = block(c, p);
            CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            String err = v.placeAnywhere(desc(name).newItemStack(), Direction.YN, 0.55, 0, 0.5, player(c));
            var e = TapCase.placed(v);
            placed += key + ": " + err + (e == null ? "" : " pins " + v.pinTips(e.slot).size()) + "; ";
            load.put(key, resistor(c, c.at(x0 + 2, 1, z), Direction.XN, 48));
            c.placeSix(Wp9aCases.GROUND, c.at(x0 + 3, 1, z));
            if (e != null) dev.put(key, e);
            return e;
        }

        static void core(SixNodeElement e, int n) {
            e.getInventory().setItem(0, Wp9aCases.coal(n));
            e.getInventory().setChanged();
        }

        public void build(TestContext c) {
            var r = row(c, "resistor", "Power Resistor", 0);
            if (r != null) core(r, 36);
            var t = row(c, "thermistor", "Thermistor", 1);
            if (t != null) core(t, 36);
            row(c, "diode a", "10A Diode", 2);
            var b = row(c, "diode b", "10A Diode", 3);
            if (b != null) WrenchInLineCase.wrench(c, b);
            row(c, "capacitor", "Power Capacitor", 4);
            var l = row(c, "inductor", "Power Inductor", 5);
            if (l != null) {
                l.getInventory().setItem(0, Wp10aCases.shared(Wp10aCases.COPPER_CABLE, mods.eln.item.CopperCableDescriptor.class, 1));
                l.getInventory().setItem(1, Wp10aCases.shared(Wp10aCases.CORE_OPTIMAL, mods.eln.item.FerromagneticCoreDescriptor.class, 1));
                l.getInventory().setChanged();
            }
            var f = row(c, "fuse", "Electrical Fuse Holder", 6);
            if (f != null) Wp9aCases.use(c, f, mods.eln.Eln.sharedItem.getDescriptor(Wp9aCases.FUSE_LV).newItemStack());
            row(c, "no fuse", "Electrical Fuse Holder", 7);
            row(c, "rheostat", "Rheostat", 8);
        }

        double i(String k) {
            return Math.abs(Wp9aCases.current(load.get(k)));
        }

        public void measure(TestContext c) {
            c.check("passives: each put in line, both contacts on the run", dev.size() == 9 && dev.values().stream().allMatch(e ->
                e.slotMask.get(e.side.getLRDUGoingTo(Direction.XN)) && e.slotMask.get(e.side.getLRDUGoingTo(Direction.XP))), placed);
            if (dev.size() != 9) return;
            for (String k : new String[]{"resistor", "thermistor"}) {
                double rd = ((mods.eln.sixnode.resistor.ResistorElement) dev.get(k)).nominalRs, rl = load.get(k).nominalRs;
                Wp9cCases.within(c, fmt("passives: a %s of %.1f ohm in line: 50 V over it and 100 ohm [A]", k, rd), i(k), 50 / (rl + rd), 0.03 * 50 / (rl + rd));
            }
            double a = i("diode a"), b = i("diode b");
            c.check("passives: of two diodes, one turned round, one conducts and one blocks",
                Math.max(a, b) > 0.45 && Math.min(a, b) < 1e-3, fmt("%.4f A, %.4f A", a, b));
            c.check("passives: a capacitor in line blocks the steady current", i("capacitor") < 0.05, fmt("%.4f A", i("capacitor")));
            Wp9cCases.within(c, "passives: an inductor with copper and a core passes it [A]", i("inductor"), 0.5, 0.03);
            Wp9cCases.within(c, "passives: a fuse holder with an LV fuse passes it [A]", i("fuse"), 0.5, 0.03);
            c.check("passives: one without a fuse blocks it", i("no fuse") < 1e-3, fmt("%.4f A", i("no fuse")));
            var rh = dev.get("rheostat");
            c.check("passives: a rheostat in line has its signal pin too", rh.getConnectionMask(rh.front) != 0 && placed.contains("rheostat: null pins 3"), placed);
        }
    }

    /**
     * AdventurAgent's diode rig, every part put in line on one floor LV run through the item's use, as a player does: a
     * 50 V Electrical Source, a diode, a Power Resistor with 46 coal dust (68 ohm), a Ground Cable, in successive blocks.
     * A 10A and a 25A Diode, each as placed and wrenched round: of each pair one passes 50 V / 68 ohm and one blocks. A
     * Signal Diode likewise between a Signal Source at SVU and one at 0 V: one conducts, one blocks. (Into a signal
     * input alone a blocking diode still lets it float up to the source: an input draws next to nothing.) (With the resistor's coal dust
     * slot empty it is 0.01 ohm: the diode then shorts the source, the symptoms AdventurAgent saw.)
     */
    static final class DiodeRigCase implements TestCase {
        final List<SixNodeElement[]> rows = new ArrayList<>();

        public String name() {
            return "a diode rig put in line by a player";
        }

        public int width() {
            return 24;
        }

        public int height() {
            return 8;
        }

        public int depth() {
            return 24;
        }

        static SixNodeElement use(TestContext c, String name, BlockPos p) {
            var player = player(c);
            var d = desc(name);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, d.newItemStack());
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atLowerCornerOf(p).add(0.55, 1 / 16.0, 0.5),
                net.minecraft.core.Direction.UP, p, false);
            d.parentItem.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            var v = (VoxelNode) mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, c.level()));
            return v == null ? null : TapCase.placed(v);
        }

        /** A run through 7 blocks at z and these devices put on it in blocks 1, 2, 3, ... (null: none). */
        static SixNodeElement[] run(TestContext c, int z, int tier, String... devices) {
            for (int x = 0; x < 7; x++) {
                c.floor(c.at(x, 0, z));
                block(c, c.at(x, 1, z));
                CableCases.voxel(c, c.at(x, 1, z), List.of(new CablePart(tier, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            }
            boolean conv = mods.eln.node.voxel.CableConverter.convertOnLoad;
            mods.eln.node.voxel.CableConverter.convertOnLoad = true;
            SixNodeElement[] e = new SixNodeElement[devices.length];
            try {
                for (int i = 0; i < devices.length; i++) e[i] = use(c, devices[i], c.at(1 + i, 1, z));
            } finally {
                mods.eln.node.voxel.CableConverter.convertOnLoad = conv;
            }
            return e;
        }

        static void volts(TestContext c, SixNodeElement src, double u) {
            if (src != null) src.networkUnserialize(c.stream(o -> {
                o.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                o.writeFloat((float) u);
            }));
        }

        void row(TestContext c, int z, String diode, boolean flip) {
            var e = run(c, z, LV, "Electrical Source", diode, "Power Resistor", "Ground Cable");
            volts(c, e[0], 50);
            if (e[2] != null) {
                e[2].getInventory().setItem(0, Wp9aCases.coal(46));   // (68 ohm)
                e[2].getInventory().setChanged();
            }
            if (flip && e[1] != null) WrenchInLineCase.wrench(c, e[1]);
            rows.add(e);
        }

        void signalRow(TestContext c, int z, boolean flip) {
            var e = run(c, z, SIGNAL, "Signal Source", "Signal Diode", "Signal Source");
            volts(c, e[0], mods.eln.Eln.SVU);
            volts(c, e[2], 0);
            if (flip && e[1] != null) WrenchInLineCase.wrench(c, e[1]);
            rows.add(e);
        }

        public void build(TestContext c) {
            row(c, 1, "10A Diode", false);
            row(c, 4, "10A Diode", true);
            row(c, 7, "25A Diode", false);
            row(c, 10, "25A Diode", true);
            signalRow(c, 13, false);
            signalRow(c, 16, true);
        }

        /** The current through a row's resistor. */
        static double through(SixNodeElement r) {
            return Math.abs(u(r, Direction.XN) - u(r, Direction.XP)) / ((mods.eln.sixnode.resistor.ResistorElement) r).nominalRs;
        }

        public void measure(TestContext c) {
            for (int pair = 0; pair < 2; pair++) {
                var a = rows.get(2 * pair);
                var b = rows.get(2 * pair + 1);
                String name = pair == 0 ? "10A" : "25A";
                boolean all = java.util.Arrays.stream(a).allMatch(java.util.Objects::nonNull) && java.util.Arrays.stream(b).allMatch(java.util.Objects::nonNull);
                c.check("diode rig: the " + name + " rows were put in line", all, java.util.Arrays.toString(a) + java.util.Arrays.toString(b));
                if (!all) continue;
                double ia = through(a[2]), ib = through(b[2]);
                c.check(fmt("diode rig: %s Diode as placed and wrenched: one passes 50 V / 68 ohm, one blocks", name),
                    Math.abs(Math.max(ia, ib) - 50 / 68.0) < 0.03 && Math.min(ia, ib) < 1e-3, fmt("%.4f A, %.4f A", ia, ib));
            }
            var s1 = rows.get(4);
            var s2 = rows.get(5);
            boolean all = java.util.Arrays.stream(s1).allMatch(java.util.Objects::nonNull) && java.util.Arrays.stream(s2).allMatch(java.util.Objects::nonNull);
            c.check("diode rig: the Signal Diode rows were put in line", all, java.util.Arrays.toString(s1) + java.util.Arrays.toString(s2));
            if (!all) return;
            var d1 = (mods.eln.sixnode.diode.DiodeElement) s1[1];
            var d2 = (mods.eln.sixnode.diode.DiodeElement) s2[1];
            double i1 = Math.abs(d1.anodeLoad.getCurrent()), i2 = Math.abs(d2.anodeLoad.getCurrent());
            c.check("diode rig: Signal Diode as placed and wrenched between SVU and 0 V: one conducts and one blocks",
                d1.resistorSwitch.getState() != d2.resistorSwitch.getState() && Math.max(i1, i2) > 1e-3 && Math.min(i1, i2) < 1e-6,
                fmt("conducting %s / %s, %.4f A / %.6f A", d1.resistorSwitch.getState(), d2.resistorSwitch.getState(), i1, i2));
        }
    }

    /**
     * A Rheostat in line on a 50 V run before 100 ohm to ground, its core 68 ohm, its signal pin's cable to a Signal
     * Source: at 0 V it is nearly shorted, at SVU it is 68 ohm. A Thermistor in line, its body held 30 C warm, is 30 %
     * lower (its -1 %/C).
     */
    static final class RheostatThermistorCase implements TestCase {
        SixNodeElement low, high, therm;
        mods.eln.sixnode.resistor.ResistorElement rLow, rHigh, rTherm;
        mods.eln.sim.ThermalLoad body;

        public String name() {
            return "a rheostat's signal and a thermistor's heat in line";
        }

        public int width() {
            return 10;
        }

        public int height() {
            return 5;
        }

        public int depth() {
            return 10;
        }

        SixNodeElement row(TestContext c, String name, int z) {
            for (int x = 0; x < 5; x++) for (int dz = -1; dz <= 1; dz++) c.floor(c.at(x, 0, z + dz));
            source(c, Wp9aCases.SOURCE, c.at(0, 1, z), 50);
            BlockPos p = c.at(1, 1, z);
            var v = block(c, p);
            CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            v.placeAnywhere(desc(name).newItemStack(), Direction.YN, 0.55, 0, 0.5, player(c));
            var e = TapCase.placed(v);
            if (e != null) {
                e.getInventory().setItem(0, Wp9aCases.coal(46));
                e.getInventory().setChanged();
            }
            return e;
        }

        void gate(TestContext c, SixNodeElement e, int z, double u) {
            if (e == null) return;
            Direction f = e.side.applyLRDU(e.front);
            var v = (VoxelNode) e.sixNode;
            cableOut(c, v, c.at(1, 1, z), e, f, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, f == Direction.ZN ? c.at(1, 1, z - 1) : c.at(1, 1, z + 1), u);
        }

        public void build(TestContext c) {
            low = row(c, "Rheostat", 1);
            gate(c, low, 1, 0);
            rLow = resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            high = row(c, "Rheostat", 4);
            gate(c, high, 4, mods.eln.Eln.SVU);
            rHigh = resistor(c, c.at(2, 1, 4), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 4));
            therm = row(c, "Thermistor", 7);
            rTherm = resistor(c, c.at(2, 1, 7), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 7));
            if (therm != null) {
                body = new mods.eln.sim.ThermalLoad(30, 1e9, 1e-3, 1e9);
                body.setAsSlow();
                mods.eln.Eln.simulator.addThermalLoad(body);
                mods.eln.Eln.simulator.addThermalConnection(new mods.eln.sim.ThermalConnection(body,
                    (mods.eln.sim.ThermalLoad) mods.eln.gametest.LabCommands.field(therm, "thermalLoad")));
            }
        }

        static double r(SixNodeElement e) {
            return ((mods.eln.sim.mna.component.Resistor) mods.eln.gametest.LabCommands.field(e, "r")).getR();
        }

        public void measure(TestContext c) {
            c.check("rheostat, thermistor: placed in line", low != null && high != null && therm != null, "");
            if (low == null || high == null || therm == null) return;
            double rl = r(low), rh = r(high), rt = r(therm);
            c.check("rheostat: its signal pin's cable linked", linked(low, low.side.applyLRDU(low.front)) && linked(high, high.side.applyLRDU(high.front)),
                low.slotMask.mask + " " + high.slotMask.mask);
            c.check(fmt("rheostat: 0 V on its signal: nearly shorted (%.2f ohm); SVU: its 68 ohm (%.1f)", rl, rh), rl < 1 && Math.abs(rh - 68) < 1,
                fmt("%.2f / %.2f ohm", rl, rh));
            Wp9cCases.within(c, "rheostat at SVU: 50 V over it and 100 ohm [A]", Math.abs(Wp9aCases.current(rHigh)), 50 / (100 + rh), 0.02 * 50 / (100 + rh));
            Wp9cCases.within(c, "rheostat at 0 V: 50 V over 100 ohm [A]", Math.abs(Wp9aCases.current(rLow)), 50 / (100 + rl), 0.02 * 50 / (100 + rl));
            double tc = ((mods.eln.sim.ThermalLoad) mods.eln.gametest.LabCommands.field(therm, "thermalLoad")).Tc;
            c.check(fmt("thermistor: warmed to %.1f C, its 68 ohm falls 1 %%/C", tc), tc > 20 && Math.abs(rt - 68 * (1 - 0.01 * tc)) < 1,
                fmt("%.2f ohm", rt));
            Wp9cCases.within(c, "thermistor: 50 V over it and 100 ohm [A]", Math.abs(Wp9aCases.current(rTherm)), 50 / (100 + rt), 0.02 * 50 / (100 + rt));
            if (body != null) mods.eln.Eln.simulator.removeThermalLoad(body);
        }
    }
    /**
     * The placement checks build throwaway elements of the held device (DevicePlacer.plan, VoxelNode.placeErrorAt: the
     * preview does it every tick while a player aims a device at a voxel block) and leave the static registries as they
     * were, for every device that goes anywhere: a Wireless Button's probes stayed in the wireless channels until the
     * server stopped. A bare probe, built and not yet unloaded, has joined nothing either.
     */
    static final class ProbesLeaveNoTraceCase implements TestCase {
        String planned = "", bare = "";
        int devices;

        public String name() {
            return "placement probes leave the static registries as they were";
        }

        public int width() {
            return 4;
        }

        static int entries(java.util.Map<String, ? extends java.util.List<?>> m) {
            int n = 0;
            for (var l : m.values()) n += l.size();
            return n;
        }

        /** Entries in every static registry a device joins: wireless transmitters, repeaters, lamp supplies, sockets. */
        static String registries() {
            return "tx " + entries(mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement.channelMap)
                + ", source " + entries(mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceElement.channelMap)
                + ", spots " + mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot.spots.size()
                + ", supplies " + entries(mods.eln.sixnode.lampsupply.LampSupplyElement.channelMap)
                + ", sockets " + entries(mods.eln.sixnode.powersocket.PowerSocketElement.channelMap)
                + ", light observers " + mods.eln.sixnode.lampsocket.LightBlockEntity.observers.size();
        }

        public void build(TestContext c) {
            for (int x = 0; x < 4; x++) c.floor(c.at(x, 0, 1));
            c.level().setBlockAndUpdate(c.at(2, 1, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            BlockPos p = c.at(1, 1, 1), q = c.at(2, 1, 1);
            var v = block(c, p);
            CableCases.voxel(c, p, List.of(new CablePart(LV, FLOOR, new int[]{0, 0, 8, 16, 0, 8})));
            var w = block(c, q);
            CableCases.voxel(c, q, List.of(new CablePart(LV, NORTH_WALL, new int[]{0, 4, 0, 16, 4, 0})));
            var player = player(c);
            StringBuilder pl = new StringBuilder(), ba = new StringBuilder();
            for (var d : new java.util.TreeMap<>(mods.eln.Eln.sixNodeItem.all()).values()) {
                if (!d.placeAnywhere()) continue;
                devices++;
                var stack = d.newItemStack();
                String before = registries();
                // on the cable (masks, in line, at an end), beside it, far from it (placeErrorAt); and on a wall
                mods.eln.node.voxel.DevicePlacer.plan(v, stack, v.freeSlot(), Direction.YN, new double[]{0.55, 0, 0.5}, player);
                mods.eln.node.voxel.DevicePlacer.plan(v, stack, v.freeSlot(), Direction.YN, new double[]{0.5, 0, 0.3}, player);
                mods.eln.node.voxel.DevicePlacer.plan(v, stack, v.freeSlot(), Direction.YN, new double[]{0.5, 0, 0.05}, player);
                mods.eln.node.voxel.DevicePlacer.plan(w, stack, w.freeSlot(), Direction.ZN, new double[]{0.55, 0.25, 0}, player);
                String after = registries();
                if (!after.equals(before)) pl.append(d.name).append(": ").append(before).append(" -> ").append(after).append("; ");
                // a bare probe as VoxelNode.standIn builds one
                SixNodeElement probe;
                try {
                    probe = (SixNodeElement) d.ElementClass.getConstructor(mods.eln.node.six.SixNode.class, Direction.class,
                        mods.eln.node.six.SixNodeDescriptor.class).newInstance(v, Direction.YN, d);
                } catch (ReflectiveOperationException | RuntimeException ex) {
                    c.line("probes: " + d.name + " not built (" + ex + "), as standIn gives null");
                    continue;
                }
                String built = registries();
                probe.unload();
                String unloaded = registries();
                if (!built.equals(before) || !unloaded.equals(before))
                    ba.append(d.name).append(": ").append(before).append(" -> built ").append(built).append(" -> unloaded ").append(unloaded).append("; ");
            }
            planned = pl.toString();
            bare = ba.toString();
        }

        public int ticks() {
            return 1;
        }

        public void measure(TestContext c) {
            c.check("probes: there are devices that go anywhere", devices > 20, devices + " devices");
            c.check("probes: the placement checks leave every registry as it was", planned.isEmpty(), planned);
            c.check("probes: a bare probe joins no registry, and its unload leaves none", bare.isEmpty(), bare);
        }
    }
}
