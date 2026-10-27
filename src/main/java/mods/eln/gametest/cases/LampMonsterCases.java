package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

import static mods.eln.gametest.cases.Wp9aCases.orient;
import static mods.eln.gametest.cases.Wp9aCases.source;

/**
 * A lamp socket removes monsters that appear near it only while it is lit (Baughn 2026-10-27: an unlit socket doing it
 * is a bug). A socket with a small 50 V bulb on a Lamp Supply channel, powered or not; a zombie appears 3 blocks away.
 */
public final class LampMonsterCases {
    private LampMonsterCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new LampMonsterCase(true));
        cases.add(new LampMonsterCase(false));
    }

    static final class LampMonsterCase implements TestCase {
        final boolean powered;
        LampSocketElement lamp;

        LampMonsterCase(boolean powered) {
            this.powered = powered;
        }

        public String id() {
            return "lamp_monsters_" + (powered ? "lit" : "unlit");
        }

        public int width() {
            return 9;
        }

        public int ticks() {
            return 100;
        }

        public void build(TestContext ctx) {
            String ch = "eln lamp monsters " + id() + " " + System.nanoTime();
            if (powered) source(ctx, Wp9aCases.SOURCE, 0, 1, 50);
            ctx.placeSix(Wp9aCases.LV_CABLE, ctx.at(1, 1, 1));
            LampSupplyElement supply = (LampSupplyElement) ctx.placeSix(Ids.six("Lamp Supply"), ctx.at(2, 1, 1));
            orient(supply, f -> f, Direction.XN);
            supply.networkUnserialize(ctx.stream(o -> {
                o.writeByte(LampSupplyElement.setPowerName);
                o.writeByte(0);
                o.writeUTF(ch);
            }));
            lamp = (LampSocketElement) ctx.placeSix(LampLightCases.LAMP_ID << 6, ctx.at(6, 1, 1));
            lamp.getInventory().setItem(0, Eln.sharedItem.getDescriptor(LampLightCases.BULB_ID << 6).newItemStack());
            lamp.getInventory().setChanged();
            lamp.networkUnserialize(ctx.stream(o -> {
                o.writeByte(4); // LampSocketElement.setChannel
                o.writeUTF(ch);
            }));
        }

        public void measure(TestContext ctx) {
            Object process = Wp10aCases.field(lamp, "lampProcess");
            int light = Wp10aCases.<Integer>field(process, "light");
            Monster zombie = EntityTypes.ZOMBIE.create(ctx.level(), EntitySpawnReason.NATURAL);
            var p = ctx.at(3, 1, 1);
            zombie.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            ctx.level().addFreshEntity(zombie);
            lamp.monsterPopFreeProcess.process(1.0);
            String detail = "lamp light " + light + ", zombie removed " + zombie.isRemoved();
            ctx.line(detail);
            if (powered) {
                ctx.check("the lamp is lit", light > 0, detail);
                ctx.check("a lit lamp removes a zombie that appears 3 blocks away", zombie.isRemoved(), detail);
            } else {
                ctx.check("the lamp is dark", light == 0, detail);
                ctx.check("a dark lamp leaves the zombie alone", !zombie.isRemoved(), detail);
            }
            zombie.discard();
        }
    }
}
