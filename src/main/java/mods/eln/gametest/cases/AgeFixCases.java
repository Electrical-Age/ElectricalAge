package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.electricalitem.ElectricalLampItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Bugs the AGE fork's history pointed at (Baughn, 2026-10-25: "start with the bugs"). */
public final class AgeFixCases {
    private AgeFixCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new EmptyFlashlight());
    }

    /** An empty flashlight doesn't switch on; one that drains goes off (it stayed "on" at 0 J). */
    static final class EmptyFlashlight implements TestCase {
        boolean stayedOff, wentOff, litWhileCharged;

        public String name() {
            return "flashlight: off when empty";
        }

        public void build(TestContext ctx) {
            ItemStack s = Eln.findItemStack("Small Flashlight", 1);
            var lamp = (ElectricalLampItem) GenericItemUsingDamageDescriptor.getDescriptor(s);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            lamp.setEnergy(s, 0);
            lamp.onItemRightClick(s, ctx.level(), player);   // try to switch it on, empty
            lamp.setEnergy(s, 1000);
            stayedOff = !lamp.isLit(s);
            lamp.onItemRightClick(s, ctx.level(), player);   // now on
            litWhileCharged = lamp.isLit(s);
            lamp.electricalItemUpdate(s, 1e6);                 // drained
            lamp.setEnergy(s, 1000);
            wentOff = !lamp.isLit(s);
        }

        public int ticks() {
            return 1;
        }

        public void measure(TestContext ctx) {
            ctx.check("flashlight: an empty one doesn't switch on", stayedOff, "");
            ctx.check("flashlight: a charged one does", litWhileCharged, "");
            ctx.check("flashlight: draining switches it off", wentOff, "");
        }
    }
}
