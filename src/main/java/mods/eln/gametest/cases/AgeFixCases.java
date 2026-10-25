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
        cases.add(new OneHandRightClick());
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

    /**
     * Baughn: when both hands hold an item that handles a right-click, only the main hand's gets it. Two charged
     * flashlights; one right-click as vanilla dispatches it (main hand, then the off hand unless the main consumed).
     */
    static final class OneHandRightClick implements TestCase {
        boolean mainOn, offOff, offAlone;

        public String name() {
            return "right-click: main hand only";
        }

        public void build(TestContext ctx) {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            ItemStack main = Eln.findItemStack("Small Flashlight", 1), off = Eln.findItemStack("Small Flashlight", 1);
            var lamp = (ElectricalLampItem) GenericItemUsingDamageDescriptor.getDescriptor(main);
            lamp.setEnergy(main, 1000);
            lamp.setEnergy(off, 1000);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, main);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, off);
            var r = main.getItem().use(ctx.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            if (!r.consumesAction()) off.getItem().use(ctx.level(), player, net.minecraft.world.InteractionHand.OFF_HAND);
            mainOn = lamp.isLit(player.getMainHandItem());
            offOff = !lamp.isLit(player.getOffhandItem());
            // an off-hand flashlight still works when the main hand is empty
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            off.getItem().use(ctx.level(), player, net.minecraft.world.InteractionHand.OFF_HAND);
            offAlone = lamp.isLit(player.getOffhandItem());
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        public int ticks() {
            return 1;
        }

        public void measure(TestContext ctx) {
            ctx.check("right-click: the main hand's flashlight switches on", mainOn, "");
            ctx.check("right-click: the off hand's stays off", offOff, "");
            ctx.check("right-click: alone, the off hand's switches on", offAlone, "");
        }
    }
}
