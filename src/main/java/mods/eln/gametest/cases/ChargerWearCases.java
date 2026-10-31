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

            // without Curios: the menu as before, no worn slots
            ElnNodeMenu plain = NodeMenus.menu(1, fp, charger.sixNode, Direction.YN);
            ctx.check("charger without Curios: no worn slots", plain.wearKeys.isEmpty() && plain.inventoryEnd == plain.slots.size()
                && plain.slots.size() == plain.deviceSlots + 36, plain.slots.size() + " slots, " + plain.deviceSlots + " device");

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
                int belt = menu.inventoryEnd, head = belt + 1;
                ctx.check("charger with worn slots: the two follow the player inventory",
                    menu.slots.size() == menu.deviceSlots + 36 + 2 && menu.wearKeys.equals(List.of("belt#0", "head#0")),
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
