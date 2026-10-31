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
        cases.add(new LightsKey());
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
        int lights, beam;
        boolean eyeLight;

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
            // one light for hand-held and worn lamps (Baughn 2026-10-31): the beam a client draws counts the headlamp, and
            // with a client drawing beams the server lights the eyes, as for a flashlight
            beam = mods.eln.item.electricalitem.LampItem.beamLight(p);
            var was = mods.eln.item.electricalitem.LampItem.clientBeam;
            mods.eln.item.electricalitem.LampItem.clientBeam = e -> true;
            try {
                BlockPos eyes = BlockPos.containing(p.getX(), p.getY() + 1.62, p.getZ());
                ctx.level().removeBlock(eyes, false);
                HeadlampItem.wornTick(worn, p, 0.05);
                eyeLight = ctx.level().getBlockState(eyes).is(ElnRegistry.LIGHT_BLOCK.get());
            } finally {
                mods.eln.item.electricalitem.LampItem.clientBeam = was;
            }
        }

        public void measure(TestContext ctx) {
            ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
            double e = ((HeadlampItem) worn.getItem()).getEnergy(worn);
            ctx.check("headlamp: a leather cap's armour (1) in the helmet slot", armour, "");
            ctx.check("headlamp: the key leaves an empty one off", emptyRefused, "");
            ctx.check("headlamp: the key switches a charged one on", HeadlampItem.isOn(worn), "");
            ctx.check("headlamp: 20 W for a second takes 20 J", Math.abs(e0 - e - 21) < 0.5, (e0 - e) + " J (and one tick more)");
            ctx.check("headlamp: its beam is the flashlight's (LampItem.beamLight 10)", beam == HeadlampItem.LIGHT, beam + "");
            ctx.check("headlamp: with clients drawing beams, a light at the eyes as for a flashlight", eyeLight, "");
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

    /**
     * The lights key steps every light the player has (Baughn 2026-10-30): a headlamp and a flashlight in each hand step
     * together on one press, each through its own cycle (headlamp off/on, flashlight off/on/boosted); a flashlight alone
     * in the offhand steps too.
     */
    static final class LightsKey implements TestCase {
        static final int SMALL_FLASHLIGHT = 0 + (121 << 6);
        String result = "";
        boolean ok, offhandAlone;

        public String id() {
            return "lights_key_steps_every_light";
        }

        static ItemStack flashlight() {
            var d = (mods.eln.item.electricalitem.ElectricalLampItem) mods.eln.Eln.sharedItemStackOne.getDescriptor(SMALL_FLASHLIGHT);
            ItemStack s = d.newItemStack();
            d.setEnergy(s, 5000);
            return s;
        }

        static int light(ItemStack s) {
            return ((mods.eln.item.electricalitem.LampItem) mods.eln.misc.Utils.getItemObject(s)).beamLight(s);
        }

        public void build(TestContext ctx) {
            Player p = FakePlayerFactory.get(ctx.level(), new com.mojang.authlib.GameProfile(java.util.UUID.fromString("e1a0b2c3-0000-4000-8000-000000000148"), "eln_lights"));
            ItemStack head = new ItemStack(ElnRegistry.HEADLAMP.get());
            ((HeadlampItem) head.getItem()).setEnergy(head, 1000);
            p.setItemSlot(EquipmentSlot.HEAD, head);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, flashlight());
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, flashlight());
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            boolean first = HeadlampItem.isOn(p.getItemBySlot(EquipmentSlot.HEAD)) && light(p.getMainHandItem()) == 10 && light(p.getOffhandItem()) == 10;
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            boolean second = !HeadlampItem.isOn(p.getItemBySlot(EquipmentSlot.HEAD)) && light(p.getMainHandItem()) == 12 && light(p.getOffhandItem()) == 12;
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            boolean third = HeadlampItem.isOn(p.getItemBySlot(EquipmentSlot.HEAD)) && light(p.getMainHandItem()) == 0 && light(p.getOffhandItem()) == 0;
            ok = first && second && third;
            result = first + " " + second + " " + third;
            // a flashlight alone in the offhand
            p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, flashlight());
            mods.eln.server.PlayerKeys.set(p, mods.eln.server.PlayerKeys.HEADLAMP, true);
            offhandAlone = light(p.getOffhandItem()) == 10;
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        public void measure(TestContext ctx) {
            ctx.check("lights key: headlamp and both flashlights step together, each its own cycle", ok, "presses 1, 2, 3: " + result);
            ctx.check("lights key: a flashlight alone in the offhand steps", offhandAlone, "");
        }
    }
}
