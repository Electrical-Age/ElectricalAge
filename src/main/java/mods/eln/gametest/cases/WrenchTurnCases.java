package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The wrench key and a click turn each device the same way, by the shared helpers (todo #556, Baughn): the click is
 * used (true) and the device's turned state moves one step. Rows, one per device that had turned by hand: the wind
 * turbine (its click had gone on to the held item), the antennas, a lamp socket that turns by half turns and the Modbus
 * RTU. (A six-node cable's wrench colour-care toggle is gone: Baughn, obsolete with voxel cables.)
 */
public final class WrenchTurnCases {
    private WrenchTurnCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Row("wrench_turn_wind_turbine", ctx -> ctx.placeTransparent(Wp10bCases.WIND_TURBINE, ctx.at(2, 1, 1)),
            "cableFront", o -> ((Direction) o).right()));
        cases.add(new Row("wrench_turn_antenna_tx", ctx -> ctx.placeTransparent(Wp10bCases.ANTENNA_TX_LOW, ctx.at(2, 1, 1)),
            "rot", o -> ((mods.eln.misc.LRDU) o).getNextClockwise()));
        cases.add(new Row("wrench_turn_antenna_rx", ctx -> ctx.placeTransparent(Wp10bCases.ANTENNA_RX_LOW, ctx.at(2, 1, 1)),
            "rot", o -> ((mods.eln.misc.LRDU) o).getNextClockwise()));
        cases.add(new Row("wrench_turn_lamp_socket", ctx -> ctx.placeSix(Wp10aCases.LAMP_SOCKET, ctx.at(2, 1, 1)),
            "front", o -> ((mods.eln.misc.LRDU) o).getNextClockwise()));
        cases.add(new Row("wrench_turn_lamp_socket_half_turns", ctx -> {
            ctx.floor(ctx.at(1, 1, 1));   // (a sconce hangs on a wall)
            return ctx.placeSix(Ids.six("Sconce Lamp Socket"), ctx.at(2, 1, 1), Direction.XN);
        }, "front", o -> ((mods.eln.misc.LRDU) o).inverse()));
        cases.add(new Row("wrench_turn_modbus_rtu", ctx -> ctx.placeSix(Wp9cCases.MODBUS_RTU, ctx.at(2, 1, 1)),
            "front", o -> ((mods.eln.misc.LRDU) o).getNextClockwise()));
    }

    /** Place a device, click it holding the wrench key, and check the click was used and field moved to next(was). */
    static final class Row implements TestCase {
        final String id, field;
        final Function<TestContext, Object> place;
        final UnaryOperator<Object> next;
        Object element;

        Row(String id, Function<TestContext, Object> place, String field, UnaryOperator<Object> next) {
            this.id = id;
            this.place = place;
            this.field = field;
            this.next = next;
        }

        public String id() {
            return id;
        }

        public int ticks() {
            return 10;
        }

        /** The wind turbine's mast and blades reach y + 3 (as in Wp10bCases). */
        public int height() {
            return 5;
        }

        public void build(TestContext ctx) {
            element = place.apply(ctx);
        }

        public void measure(TestContext ctx) {
            Object was = GuiPacketFuzzCases.get(element, field), expected = next.apply(was);
            var player = ctx.player();
            mods.eln.server.PlayerKeys.set(player, mods.eln.server.PlayerKeys.WRENCH, true);
            boolean used;
            try {
                used = element instanceof SixNodeElement s
                    ? s.sixNode.onBlockActivated(player, s.side, 0.5f, 0.5f, 0.5f)
                    : ((TransparentNodeElement) element).node.onBlockActivated(player, Direction.YP, 0.5f, 1f, 0.5f);
            } finally {
                mods.eln.server.PlayerKeys.set(player, mods.eln.server.PlayerKeys.WRENCH, false);
            }
            Object now = GuiPacketFuzzCases.get(element, field);
            ctx.check(id + ": the wrench click is used", used, element.getClass().getSimpleName());
            ctx.check(id + ": " + field + " " + was + " to " + expected, expected.equals(now), "now " + now);
        }
    }
}
