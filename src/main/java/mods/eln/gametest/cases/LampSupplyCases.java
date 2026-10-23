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
}
