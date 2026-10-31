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
            "Ground Cable", "Hub"};
        static final String[] WALL_DEVICES = {"50V Power Socket", "200V Power Socket", "Electrical Anemometer Sensor"};
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
     * Relays off centre, gate north: a 50 V source west, a Power Resistor east (then ground), a Signal Source north on the
     * gate pin. Gate at 50 V: the LV relay closes and the resistor carries the current; gate at 0 V: none. A Signal Relay
     * at 5 V closes: its far contact at the signal source's voltage.
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
            source(c, power ? Wp9aCases.SOURCE : Wp9aCases.SIGNAL_SOURCE, c.at(0, 1, z), power ? 50 : mods.eln.Eln.SVU);
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
            c.check("relay: gate 50 V on its pin: closed", on.getSwitchState(), fmt("gate %.2f V", u(on, Direction.ZN)));
            double i = Wp9aCases.current(rOn);
            Wp9cCases.within(c, "relay: closed, the resistor east of it carries the 50 V source's current [A]", i, 50 / rOn.nominalRs, 0.05 * 50 / rOn.nominalRs);
            c.check("relay: gate 0 V: open, no current", !off.getSwitchState() && Math.abs(Wp9aCases.current(rOff)) < 1e-3,
                fmt("%s, %.4f A", off.getSwitchState(), Wp9aCases.current(rOff)));
            c.check("relay: a Signal Relay closes on signal cables", sig.getSwitchState() && Math.abs(u(sig, Direction.XP) - mods.eln.Eln.SVU) < 0.2,
                fmt("%s, far contact %.2f V", sig.getSwitchState(), u(sig, Direction.XP)));
        }
    }

    /**
     * Probes off centre, signal pin north. An Electrical Probe between a 50 V source (west) and a Power Resistor (east) to
     * ground: its signal pin's cable at 50 V's share of SVU. A Voltage Probe whose cable pin (south) is on a 30 V source.
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
            source(c, Wp9aCases.SOURCE, c.at(0, 1, 1), 50);
            resistor(c, c.at(2, 1, 1), Direction.XN, 48);
            c.placeSix(Wp9aCases.GROUND, c.at(3, 1, 1));
            pVolt = c.at(1, 1, 4);
            vVolt = block(c, pVolt);
            volt = put(c, vVolt, "Voltage Probe", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vVolt, pVolt, volt, Direction.ZP, LV, FLOOR);
            cableOut(c, vVolt, pVolt, volt, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SOURCE, c.at(1, 1, 5), 30);
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
            Wp9cCases.within(c, fmt("probe: the signal on its pin's cable is %.2f V / 50 V of SVU [V]", a), cableU(vProbe, s, Direction.ZN), a / 50 * mods.eln.Eln.SVU, 0.05);
            c.check("probe: it sees the source through its pin (> 45 V)", a > 45, fmt("%.2f V, out %.2f V", a, out));
            int[] f = tip(vVolt, volt, Direction.ZN), b = tip(vVolt, volt, Direction.ZP);
            c.check("voltage probe: its signal pin 6 voxels out on the long side, its cable pin 4 on the short (centre 10,8)",
                f != null && b != null && f[2] == 2 && b[2] == 12 && f[0] == 10, java.util.Arrays.toString(f) + " " + java.util.Arrays.toString(b));
            Wp9cCases.within(c, "voltage probe: 30 V on its cable pin -> 0.6 SVU on its signal pin's cable [V]", cableU(vVolt, f, Direction.ZN), 0.6 * mods.eln.Eln.SVU, 0.05);
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
     * Signal devices off centre: a Wireless Signal Transmitter fed 35 V on its pin and a Receiver whose pin's cable carries
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
            // transmitter: its input pin north, a 35 V signal source beyond
            BlockPos pt = c.at(1, 1, 2);
            var vt = block(c, pt);
            tx = put(c, vt, "Wireless Signal Transmitter", Direction.YN, 2, 0, 0, Direction.ZN);
            cableOut(c, vt, pt, tx, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(1, 1, 1), 35);
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
            // LED vuMeter, its input north on 30 V
            BlockPos pv = c.at(10, 1, 2);
            var vv = block(c, pv);
            vu = put(c, vv, "LED vuMeter", Direction.YN, -2, 0, 0, Direction.ZN);
            cableOut(c, vv, pv, vu, Direction.ZN, SIGNAL, FLOOR);
            source(c, Wp9aCases.SIGNAL_SOURCE, c.at(10, 1, 1), 30);
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
            Wp9cCases.within(c, "wireless: 35 V on the transmitter's pin, out of the receiver's pin [V]", u(rx, Direction.ZN), 35, 0.1);
            Wp9cCases.within(c, "wireless: the receiver's pin's cable [V]", ProbesCase.cableU(vRx, tip(vRx, rx, Direction.ZN), Direction.ZN), 35, 0.1);
            c.check("timer: both pins linked", linked(timer, Direction.ZN) && linked(timer, Direction.ZP), "mask " + timer.slotMask.mask);
            Wp9cCases.within(c, "timer: input pin held high: its output pin's cable high [V]",
                ProbesCase.cableU(vTimer, tip(vTimer, timer, Direction.ZP), Direction.ZP), mods.eln.Eln.SVU, 0.1);
            Wp9cCases.within(c, "LED vuMeter: 30 V on its pin [V]", u(vu, Direction.ZN), 30, 0.1);
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
            floorSocket = String.valueOf(vf.placeAnywhere(desc("50V Power Socket").newItemStack(), Direction.YN, 0.3, 0, 0.5, player(c)));
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
            socket = put(c, vs, "50V Power Socket", Direction.ZN, 2, 0, 0, Direction.YN);
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
            socketErr = String.valueOf(u.placeAnywhere(desc("50V Power Socket").newItemStack(), Direction.ZN, 0.5, 0.45, 0, player(c)));
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
     * Probe aimed at five spots just past the end goes the same place each time, its LV pin on the end: the probe doesn't
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
            double[][] aims = {{9.5, 8}, {10.5, 8}, {11, 7.5}, {10, 9.5}, {12, 8.5}};
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
}
