package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.sixnode.lampsupply.LampSupplyElement;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.orient;
import static mods.eln.gametest.cases.Wp9aCases.source;

/**
 * Lamp supply powering lamp sockets by channel (no cable between them). Port step T3 (notes/port26/PLAN.md).
 * Electrical Source 50 V -> LV cable -> Lamp Supply (empty cable slot, power channel 0 named by GUI packet, no
 * wireless channel = always on). Two Robust Lamp Sockets (bulb only, no cable: built in; powered by lamp supply is
 * their default) 3 and 5 blocks away: one on the supply's channel lights up (LampSocketProcess.light > 0), one on
 * another channel stays dark.
 */
public final class LampSupplyCases {
    private LampSupplyCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new LampSupplyCase());
        cases.add(new PowerSocketCase());
    }

    static final class LampSupplyCase implements TestCase {
        LampSocketElement on, off;
        LampSupplyElement supply;
        String ch;

        public String name() {
            return "lamp supply";
        }

        public int width() {
            return 9;
        }

        LampSocketElement socket(TestContext ctx, int x, String channel) {
            LampSocketElement l = (LampSocketElement) ctx.placeSix(Wp10aCases.LAMP_SOCKET, ctx.at(x, 1, 1));
            l.getInventory().setItem(0, Eln.sharedItem.getDescriptor(Wp10aCases.LAMP_BULB).newItemStack());
            l.getInventory().setChanged();
            l.networkUnserialize(ctx.stream(o -> {
                o.writeByte(4); // LampSocketElement.setChannel (package-private)
                o.writeUTF(channel);
            }));
            return l;
        }

        public void build(TestContext ctx) {
            ch = "eln selftest lamps " + System.nanoTime();
            source(ctx, Wp9aCases.SOURCE, 0, 1, 50);
            ctx.placeSix(Wp9aCases.LV_CABLE, ctx.at(1, 1, 1));
            supply = (LampSupplyElement) ctx.placeSix(Ids.six("Lamp Supply"), ctx.at(2, 1, 1));
            orient(supply, f -> f, Direction.XN); // its power pin is the front
            // its range slot left empty: the supply works without cables (Baughn 2026-10-17)
            supply.networkUnserialize(ctx.stream(o -> {
                o.writeByte(LampSupplyElement.setPowerName);
                o.writeByte(0);
                o.writeUTF(ch);
            }));
            on = socket(ctx, 5, ch);
            off = socket(ctx, 7, ch + " other");
        }

        /** The lamp's light level (LampSocketProcess.light), wherever it is put (own block or a LightBlock ahead). */
        static int light(LampSocketElement l) {
            Integer v = Wp10aCases.field(Wp10aCases.field(l, "lampProcess"), "light");
            return v;
        }

        public void measure(TestContext ctx) {
            ctx.check("lamp supply: socket on its channel lights up", light(on) > 0, "light " + light(on)
                + ", supply U " + supply.powerLoad.getU() + ", socket U " + on.positiveLoad.getU());
            ctx.check("lamp supply: socket on another channel stays dark", light(off) == 0, "light " + light(off));
        }
    }

    /**
     * A power socket on a lamp supply's channel puts the supply's power on its cable (AGE's wireless outlet; 1.12 left
     * the socket a stub): with a 25 ohm load it carries ~2 A at ~50 V, and the supply draws it from its own cable. A
     * socket on another channel stays dead.
     */
    static final class PowerSocketCase implements TestCase {
        mods.eln.sixnode.powersocket.PowerSocketElement fed, other;
        LampSupplyElement supply;
        mods.eln.sim.mna.component.Resistor load;
        String ch;

        public String name() {
            return "power socket fed by a lamp supply";
        }

        public int width() {
            return 9;
        }

        mods.eln.sixnode.powersocket.PowerSocketElement socket(TestContext ctx, int x, String channel) {
            var p = ctx.at(x, 2, 1);
            ctx.floor(p.north());   // (its wall)
            var s = (mods.eln.sixnode.powersocket.PowerSocketElement) ctx.placeSix(Wp9aCases.SOCKET_50V, p, Direction.ZN);
            s.networkUnserialize(ctx.stream(o -> {
                o.writeByte(mods.eln.sixnode.powersocket.PowerSocketElement.setChannelId);
                o.writeUTF(channel);
            }));
            return s;
        }

        public void build(TestContext ctx) {
            ch = "eln selftest sockets " + System.nanoTime();
            source(ctx, Wp9aCases.SOURCE, 0, 1, 50);
            ctx.placeSix(Wp9aCases.LV_CABLE, ctx.at(1, 1, 1));
            supply = (LampSupplyElement) ctx.placeSix(Ids.six("Lamp Supply"), ctx.at(2, 1, 1));
            orient(supply, f -> f, Direction.XN);
            supply.networkUnserialize(ctx.stream(o -> {
                o.writeByte(LampSupplyElement.setPowerName);
                o.writeByte(0);
                o.writeUTF(ch);
            }));
            fed = socket(ctx, 5, ch);
            other = socket(ctx, 7, ch + " other");
            load = new mods.eln.sim.mna.component.Resistor(fed.powerLoad, null).setR(25);
            Eln.simulator.addElectricalComponent(load);
        }

        public int ticks() {
            return 80;
        }

        public void measure(TestContext ctx) {
            double u = fed.powerLoad.getU(), i = load.getCurrent();
            double supplyIn = Math.abs(supply.powerLoad.getCurrent());
            Eln.simulator.removeElectricalComponent(load);
            String s = String.format("socket %.2f V, %.3f A into 25 ohm; supply cable %.3f A; other socket %.2f V", u, i, supplyIn, other.powerLoad.getU());
            ctx.line("power socket: " + s);
            ctx.check("power socket: carries the supply's power to its load", u > 45 && i > 1.8, s);
            ctx.check("power socket: the supply pays for it", supplyIn > 1.5, s);
            ctx.check("power socket: another channel stays dead", Math.abs(other.powerLoad.getU()) < 1, s);
        }
    }
}
