package mods.eln.gametest.cases;

import mods.eln.node.six.SixNodeElement;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sixnode.wirelesssignal.rx.WirelessSignalRxElement;
import mods.eln.sixnode.wirelesssignal.source.WirelessSignalSourceElement;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import java.util.List;

import static mods.eln.gametest.cases.Wp9cCases.*;

/**
 * Wireless parts without a case in Wp9c: switch, button (auto reset), repeater. Port step T3 (notes/port26/PLAN.md).
 * Each case uses a private channel. 1.12 built them at x = -40 and beyond, off the selftest grid; on 26.x each GameTest
 * has its own box, so the layout is shifted into it (same distances).
 */
public final class WirelessMoreCases {
    private WirelessMoreCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new SourceCase("Wireless Switch", true));
        cases.add(new SourceCase("Wireless Button", false));
        cases.add(new RepeaterCase());
    }

    static SixNodeElement six(TestContext ctx, int damage, BlockPos p) {
        ctx.floor(p.below());
        return ctx.placeSix(damage, p);
    }

    static void channel(TestContext ctx, SixNodeElement e, byte id, String channel) {
        e.networkUnserialize(ctx.stream(o -> {
            o.writeByte(id);
            o.writeUTF(channel);
        }));
    }

    static WirelessSignalRxElement rx(TestContext ctx, BlockPos p, String channel) {
        WirelessSignalRxElement rx = (WirelessSignalRxElement) six(ctx, WIRELESS_RX, p);
        channel(ctx, rx, WirelessSignalRxElement.setChannelId, channel);
        return rx;
    }

    /**
     * A wireless switch or button, clicked once at build, 3 blocks from a receiver on its channel. The switch stays on
     * (receiver = SVU); the button resets itself after 0.21 s (receiver back to 0 V at measure time).
     */
    static final class SourceCase implements TestCase {
        final String device;
        final boolean staysOn;
        WirelessSignalRxElement rx;

        SourceCase(String device, boolean staysOn) {
            this.device = device;
            this.staysOn = staysOn;
        }

        public String name() {
            return "wireless " + device;
        }

        public int width() {
            return 4;
        }

        public void build(TestContext ctx) {
            String ch = "eln selftest " + device + " " + System.nanoTime();
            SixNodeElement src = six(ctx, Ids.six(device), ctx.at(0, 1, 1));
            channel(ctx, src, WirelessSignalSourceElement.setChannelId, ch);
            Wp9aCases.use(ctx, src, ItemStack.EMPTY);
            rx = rx(ctx, ctx.at(3, 1, 1), ch);
        }

        public void measure(TestContext ctx) {
            within(ctx, "wireless " + device + " clicked once: receiver [V]" + (staysOn ? " (= SVU)" : " (auto reset: 0)"),
                out(rx), staysOn ? SVU : 0, 0.01 * SVU);
        }
    }

    /**
     * Signal Source 35 V -> transmitter (range 32) and two receivers 56 blocks away on its channel: one with a
     * repeater (range 32) halfway, one without. Only the repeated one hears it.
     */
    static final class RepeaterCase implements TestCase {
        WirelessSignalRxElement far, repeated;

        public String name() {
            return "wireless repeater";
        }

        public int width() {
            return 58;
        }

        public int depth() {
            return 42;
        }

        public void build(TestContext ctx) {
            String ch = "eln selftest repeater " + System.nanoTime();
            ctx.floor(ctx.at(0, 0, 1));
            signal(ctx, ctx.at(0, 1, 1), 35);
            WirelessSignalTxElement tx = (WirelessSignalTxElement) six(ctx, WIRELESS_TX, ctx.at(1, 1, 1));
            Wp9aCases.orient(tx, l -> l, mods.eln.misc.Direction.XN); // input (front) toward the source at x0, as in Wp9c
            channel(ctx, tx, WirelessSignalTxElement.setChannelId, ch);
            six(ctx, Ids.six("Wireless Signal Repeater"), ctx.at(29, 1, 1));
            repeated = rx(ctx, ctx.at(57, 1, 1), ch);
            far = rx(ctx, ctx.at(57, 1, 1 + 40), ch); // 56 + sideways 40: no repeater within 32 of both
        }

        public void measure(TestContext ctx) {
            within(ctx, "wireless repeater: receiver 56 blocks away via a repeater [V] (35 V)", out(repeated), 35, 0.02 * SVU);
            within(ctx, "wireless repeater: receiver out of range, no repeater [V] (0)", out(far), 0, 0.01 * SVU);
        }
    }
}
