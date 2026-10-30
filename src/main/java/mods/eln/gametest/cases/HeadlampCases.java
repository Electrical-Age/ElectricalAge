package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.headlamp.HeadlampItem;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/** The headlamp (Baughn 2026-10-30) and the hand drill's model (board #44). */
public final class HeadlampCases {
    private HeadlampCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Worn());
        cases.add(new DrillModel());
    }

    /**
     * Worn in the helmet slot: a leather cap's armour; the headlamp key switches it on (with charge) and off (no charge:
     * stays off); on, it drains 20 W and lights the way its wearer looks.
     */
    static final class Worn implements TestCase {
        Player p;
        ItemStack lamp;
        boolean emptyRefused, armour;
        double e0;
        int lights;

        public String id() {
            return "headlamp_worn";
        }

        public int width() {
            return 10;
        }

        public int ticks() {
            return 30;
        }

        public void build(TestContext ctx) {
            // (a fake player of its own: the shared one's view places other tests' devices)
            p = FakePlayerFactory.get(ctx.level(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("e1a0b2c3-0000-4000-8000-000000000147"), "eln_headlamp"));
            BlockPos at = ctx.at(1, 1, 1);
            p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, -90, 0);   // (yaw -90: looking along +x)
            lamp = new ItemStack(ElnRegistry.HEADLAMP.get());
            var attrs = lamp.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            armour = attrs != null && attrs.modifiers().stream().anyMatch(m -> m.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)
                && m.modifier().amount() == 1);
            p.setItemSlot(EquipmentSlot.HEAD, lamp);
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            emptyRefused = !HeadlampItem.isOn(p.getItemBySlot(EquipmentSlot.HEAD));
            ((HeadlampItem) lamp.getItem()).setEnergy(p.getItemBySlot(EquipmentSlot.HEAD), 1000);
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
            e0 = ((HeadlampItem) worn.getItem()).getEnergy(worn);
            for (int i = 0; i < 20; i++) HeadlampItem.wornTick(worn, p, 0.05);
            // (the light blocks time out in a few ticks: counted now)
            for (BlockPos q : BlockPos.betweenClosed(ctx.at(1, 0, 0), ctx.at(9, 3, 2)))
                if (ctx.level().getBlockState(q).is(ElnRegistry.LIGHT_BLOCK.get())) lights++;
        }

        public void measure(TestContext ctx) {
            ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
            double e = ((HeadlampItem) worn.getItem()).getEnergy(worn);
            ctx.check("headlamp: a leather cap's armour (1) in the helmet slot", armour, "");
            ctx.check("headlamp: the key leaves an empty one off", emptyRefused, "");
            ctx.check("headlamp: the key switches a charged one on", HeadlampItem.isOn(worn), "");
            ctx.check("headlamp: 20 W for a second takes 20 J", Math.abs(e0 - e - 20) < 0.5, (e0 - e) + " J");
            ctx.check("headlamp: light along its wearer's look", lights > 0, lights + " light blocks");
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            ctx.check("headlamp: the key switches it off", !HeadlampItem.isOn(p.getItemBySlot(EquipmentSlot.HEAD)), "");
            p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    /** The hand drill's model (made with the model library, built at compile time): its parts, the bit's axis, the item draw. */
    static final class DrillModel implements TestCase {
        public String id() {
            return "hand_drill_model";
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var obj = mods.eln.Eln.obj.getObj("handdrill");
            ctx.check("drill: the model loads with parts main and bit", obj != null && obj.getPart("main") != null && obj.getPart("bit") != null, "" + obj);
            var d = mods.eln.Eln.sharedItemStackOne.getDescriptor(Wp12Cases.MINING_DRILL);
            ctx.check("drill: drawn as its model", d instanceof mods.eln.item.electricalitem.ElectricalPickaxe pick && pick.hasItemDraw(), "" + d);
        }
    }
}
