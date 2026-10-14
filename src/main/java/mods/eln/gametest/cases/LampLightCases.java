package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.orient;
import static mods.eln.gametest.cases.Wp9aCases.source;

/**
 * Lamps light the world (Baughn 2026-10-14: "EA lights don't work"). The other lamp cases check LampSocketProcess's
 * light value only; these check the block light the world ends up with where the lamp puts it (its own block or a
 * LightBlock ahead). Every socket with a small 50 V bulb, and every bulb in a Robust Lamp Socket; powered by a Lamp
 * Supply on a channel (50 V, or 200 V with medium voltage cable for the 200 V bulbs).
 */
public final class LampLightCases {
    private LampLightCases() {
    }

    static final int LAMP_ID = 64, BULB_ID = 4;
    static final int MV_CABLE = 8 + (32 << 6);
    /** Socket sub ids and the face they go on (sconce: a wall; suspended: the ceiling). */
    static final int[] SOCKETS = {0, 1, 4, 5, 6, 7, 8, 9, 12, 13};
    static final int[] BULBS = {0, 1, 2, 4, 5, 16, 17, 18, 32, 36, 37, 38};
    static final java.util.Set<Integer> MV_BULBS = java.util.Set.of(2, 18, 36, 38);

    public static void addTo(List<TestCase> cases) {
        for (int s : SOCKETS) cases.add(new LampLightCase(s, 0));
        for (int b : BULBS) if (b != 0) cases.add(new LampLightCase(4, b));
    }

    static final class LampLightCase implements TestCase {
        final int socketSub, bulbSub;
        LampSocketElement lamp;
        LampSupplyElement supply;

        LampLightCase(int socketSub, int bulbSub) {
            this.socketSub = socketSub;
            this.bulbSub = bulbSub;
        }

        public String id() {
            return "lamp_light_s" + socketSub + "_b" + bulbSub;
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 100;
        }

        String label() {
            return Eln.sixNodeItem.getDescriptor(socketSub + (LAMP_ID << 6)).name + " + "
                + Eln.sharedItem.getDescriptor(bulbSub + (BULB_ID << 6)).name;
        }

        public void build(TestContext ctx) {
            boolean mv = MV_BULBS.contains(bulbSub);
            int cable = mv ? MV_CABLE : Wp9aCases.LV_CABLE;
            String ch = "eln lamp light " + id() + " " + System.nanoTime();
            source(ctx, Wp9aCases.SOURCE, 0, 1, mv ? 200 : 50);
            ctx.placeSix(cable, ctx.at(1, 1, 1));
            supply = (LampSupplyElement) ctx.placeSix(Ids.six("Lamp Supply"), ctx.at(2, 1, 1));
            orient(supply, f -> f, Direction.XN);
            supply.getInventory().setItem(0, Eln.sixNodeItem.getDescriptor(cable).newItemStack(1));
            supply.getInventory().setChanged();
            supply.networkUnserialize(ctx.stream(o -> {
                o.writeByte(LampSupplyElement.setPowerName);
                o.writeByte(0);
                o.writeUTF(ch);
            }));
            BlockPos p = ctx.at(6, 1, 1);
            Direction side = Direction.YN;
            if (socketSub == 9) {            // sconce: on a wall
                ctx.floor(p.north());
                side = Direction.ZN;
            } else if (socketSub == 12 || socketSub == 13) {   // suspended: from the ceiling
                ctx.floor(p.above());
                side = Direction.YP;
            }
            lamp = (LampSocketElement) ctx.placeSix(socketSub + (LAMP_ID << 6), p, side);
            lamp.getInventory().setItem(0, Eln.sharedItem.getDescriptor(bulbSub + (BULB_ID << 6)).newItemStack());
            lamp.getInventory().setItem(1, Eln.sixNodeItem.getDescriptor(cable).newItemStack(1));
            lamp.getInventory().setChanged();
            lamp.networkUnserialize(ctx.stream(o -> {
                o.writeByte(4); // LampSocketElement.setChannel
                o.writeUTF(ch);
            }));
        }

        public void measure(TestContext ctx) {
            Object process = Wp10aCases.field(lamp, "lampProcess");
            int light = Wp10aCases.<Integer>field(process, "light");
            Coordonate lb = Wp10aCases.field(process, "lbCoord");
            settle(ctx);
            BlockPos at = lb.pos();
            int world = ctx.level().getBrightness(LightLayer.BLOCK, at);
            int max = 0;
            for (BlockPos q : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1)))
                max = Math.max(max, ctx.level().getBrightness(LightLayer.BLOCK, q));
            String detail = label() + ": lamp light " + light + ", light put at " + at + " ("
                + ctx.level().getBlockState(at).getBlock().getName().getString() + "), block light there " + world
                + ", max around " + max + ", socket U " + String.format("%.1f", lamp.positiveLoad.getU())
;
            ctx.line(detail);
            ctx.check(label() + " is lit", light > 0, detail);
            ctx.check(label() + " lights the world", light > 0 && world >= light, detail);
        }
    }

    /** Wait for the light engine thread (the test server runs ticks without pause). */
    static void settle(TestContext ctx) {
        var engine = (net.minecraft.server.level.ThreadedLevelLightEngine) ctx.level().getChunkSource().getLightEngine();
        BlockPos o = ctx.at(0, 0, 0);
        for (int round = 0; round < 2; round++) {
            var f = engine.waitForPendingTasks(o.getX() >> 4, o.getZ() >> 4);
            long deadline = System.nanoTime() + 2_000_000_000L;
            while (!f.isDone() && System.nanoTime() < deadline) {
                engine.tryScheduleUpdate();
                Thread.onSpinWait();
            }
        }
    }
}
