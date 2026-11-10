package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Direction;
import mods.eln.node.NodeMenus;
import mods.eln.registry.ElnRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.util.List;

/**
 * The battery charger's menu shows the player's worn Curios belt and head slots (Baughn: charge a worn battery belt
 * without the Curios screen). The test runtime has no Curios, so a stand-in {@link ElnNodeMenu.Wearables} gives a belt
 * slot that takes only the belt; Curios' own rules (slot types, canEquip/canUnequip and their events) aren't exercised.
 */
public final class ChargerWearCases {
    private ChargerWearCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Worn());
    }

    static final int CHARGER_WEAK = 0 + (66 << 6);   // Weak 50V Battery Charger
    static final List<String> ARMOR = List.of("armor#head", "armor#chest", "armor#legs", "armor#feet");

    static final class Worn implements TestCase {
        public String id() {
            return "charger_worn_slots";
        }

        public int width() {
            return 3;
        }

        public int ticks() {
            return 1;
        }

        mods.eln.node.six.SixNodeElement charger;

        public void build(TestContext ctx) {
            charger = ctx.placeSix(CHARGER_WEAK, ctx.at(1, 1, 1));
        }

        public void measure(TestContext ctx) {
            FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.level());
            fp.getInventory().clearContent();

            // without Curios: the player's four armour slots only (nero 2026-11-10: charge worn armour from the charger)
            ElnNodeMenu plain = NodeMenus.menu(1, fp, charger.sixNode, Direction.YN);
            ctx.check("charger without Curios: the four armour slots", plain.wearKeys.equals(ARMOR)
                && plain.slots.size() == plain.deviceSlots + 36 + 4, plain.slots.size() + " slots, keys " + plain.wearKeys);
            // a worn E-Coal chestplate: shift-click into the charger, and back on
            ItemStack plate = new ItemStack(mods.eln.registry.batch.Wp12dContent.plateECoal.get());
            fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, plate.copy());
            int chest = plain.inventoryEnd + 1;
            ctx.check("the chest slot holds the worn chestplate and takes only chest armour", plain.getSlot(chest).getItem().is(plate.getItem())
                && !plain.getSlot(chest).mayPlace(mods.eln.Eln.findItemStack("Portable Battery")), "" + plain.getSlot(chest).getItem());
            plain.quickMoveStack(fp, chest);
            int pat = -1;
            for (int i = 0; i < plain.deviceSlots; i++) if (plain.getSlot(i).getItem().is(plate.getItem())) pat = i;
            ctx.check("shift-click: the worn chestplate goes into a charger slot", pat >= 0
                && fp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty(), "charger slot " + pat);
            if (pat >= 0) {
                plain.quickMoveStack(fp, pat);
                ctx.check("shift-click: from the charger it goes back on", fp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST)
                    .is(plate.getItem()), "" + fp.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
            }
            fp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);

            SimpleContainer worn = new SimpleContainer(1);
            ElnNodeMenu.Wearables saved = ElnNodeMenu.wearables;
            ElnNodeMenu.wearables = new ElnNodeMenu.Wearables() {
                @Override
                public List<String> keys(Player player, int max) {
                    return List.of("belt#0", "head#0").subList(0, Math.min(2, max));
                }

                @Override
                public Slot slot(Player player, String key, int x, int y) {
                    if (!key.equals("belt#0")) return new ElnNodeMenu.NoSlot(x, y);
                    return new Slot(worn, 0, x, y) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return stack.is(ElnRegistry.BATTERY_BELT.get());
                        }
                    };
                }
            };
            try {
                ElnNodeMenu menu = NodeMenus.menu(1, fp, charger.sixNode, Direction.YN);
                int belt = menu.inventoryEnd + 4, head = belt + 1;
                List<String> keys = new java.util.ArrayList<>(ARMOR);
                keys.addAll(List.of("belt#0", "head#0"));
                ctx.check("charger with worn slots: the armour, then the two Curios slots",
                    menu.slots.size() == menu.deviceSlots + 36 + 6 && menu.wearKeys.equals(keys),
                    menu.slots.size() + " slots, keys " + menu.wearKeys);
                var buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), ctx.level().registryAccess());
                menu.writeOpenData(buf);
                ElnNodeMenu client = ElnNodeMenu.fromNetwork(2, fp.getInventory(), buf);
                ctx.check("the client menu has the same worn slots", client.slots.size() == menu.slots.size()
                    && client.wearKeys.equals(menu.wearKeys), client.slots.size() + " vs " + menu.slots.size());

                ItemStack bat = mods.eln.Eln.findItemStack("Portable Battery");
                ctx.check("the worn belt slot refuses a portable battery; a slot the client lacks takes nothing",
                    !menu.getSlot(belt).mayPlace(bat) && !menu.getSlot(head).mayPlace(new ItemStack(ElnRegistry.HEADLAMP.get())), "");

                // the worn belt: shift-click into the charger, and charged, back on
                worn.setItem(0, new ItemStack(ElnRegistry.BATTERY_BELT.get()));
                menu.quickMoveStack(fp, belt);
                int at = -1;
                for (int i = 0; i < menu.deviceSlots; i++) if (menu.getSlot(i).getItem().is(ElnRegistry.BATTERY_BELT.get())) at = i;
                ctx.check("shift-click: the worn belt goes into a charger slot", at >= 0 && worn.getItem(0).isEmpty(), "charger slot " + at);
                if (at >= 0) {
                    menu.quickMoveStack(fp, at);
                    ctx.check("shift-click: from the charger it goes back on", worn.getItem(0).is(ElnRegistry.BATTERY_BELT.get())
                        && menu.getSlot(at).getItem().isEmpty(), "worn " + worn.getItem(0));
                }

                // a battery out of the charger goes to the inventory, not the worn slot
                menu.getSlot(0).set(bat.copy());
                worn.setItem(0, ItemStack.EMPTY);
                menu.quickMoveStack(fp, 0);
                ctx.check("shift-click: a battery from the charger goes to the inventory", worn.getItem(0).isEmpty()
                    && menu.getSlot(0).getItem().isEmpty() && fp.getInventory().countItem(bat.getItem()) == 1, "worn " + worn.getItem(0));
            } finally {
                ElnNodeMenu.wearables = saved;
                fp.getInventory().clearContent();
            }
        }
    }
}
