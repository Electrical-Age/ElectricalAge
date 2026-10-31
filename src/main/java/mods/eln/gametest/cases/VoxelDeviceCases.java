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
     * of each, centred, front north); the relays at their half size, their contacts along their length.
     */
    static final class GeometryCase implements TestCase {
        static final String[] FLOOR_DEVICES = {"Low Voltage Relay", "Medium Voltage Relay", "High Voltage Relay", "Very High Voltage Relay",
            "Signal Relay"};
        static final String[] WALL_DEVICES = {};
        String all = "", middle = "", relays = "", stayed = "";
        boolean anywhereOk, middleOk, relaysOk, stayedOk;

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
            // the ones that stay as they are: the redstone converters (Baughn)
            StringBuilder st = new StringBuilder();
            stayedOk = true;
            for (String n : new String[]{"Redstone-to-Voltage Converter"}) {
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
            c.check("voxel devices: each converted device goes anywhere; the redstone converters don't", anywhereOk && stayedOk, stayed);
            c.check("voxel devices: a click in the middle of a free face puts each in the face's own slot (as before)", middleOk, middle);
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
}
