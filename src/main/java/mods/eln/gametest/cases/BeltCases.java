package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.item.belt.BatteryBeltItem;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * The battery belt (Baughn 2026-10-27): what its pockets take (EA portable batteries and condensators, not other items
 * or another belt), and that as an EA item battery it is charged whole and gives only from what can discharge.
 */
public final class BeltCases {
    private BeltCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Pockets());
    }

    static final class Pockets implements TestCase {
        public String id() {
            return "battery_belt_pockets_and_charge";
        }

        public int ticks() {
            return 1;
        }

        public void build(TestContext ctx) {
        }

        public void measure(TestContext ctx) {
            var beltItem = mods.eln.registry.ElnRegistry.BATTERY_BELT.get();
            ItemStack belt = new ItemStack(beltItem);
            ItemStack bat = new ItemStack(mods.eln.Eln.itemsByName.get("Portable Battery"));
            ItemStack cond = new ItemStack(mods.eln.Eln.itemsByName.get("Portable Condensator"));
            IItemEnergyBattery bb = BatteryBeltItem.battery(bat), cb = BatteryBeltItem.battery(cond);
            ctx.check("the belt takes EA portable batteries and condensators, not dirt or another belt", BatteryBeltItem.accepts(bat)
                && BatteryBeltItem.accepts(cond) && !BatteryBeltItem.accepts(new ItemStack(Items.DIRT)) && !BatteryBeltItem.accepts(belt.copy()), "");
            bb.setEnergy(bat, 0);
            cb.setEnergy(cond, 0);
            BatteryBeltItem.setContents(belt, List.of(bat, cond, ItemStack.EMPTY, ItemStack.EMPTY));
            double max = beltItem.getEnergyMax(belt);
            ctx.check("it stands for its contents", Math.abs(max - bb.getEnergyMax(bat) - cb.getEnergyMax(cond)) < 1, "" + max);
            beltItem.setEnergy(belt, max);   // (an EA charger)
            var full = BatteryBeltItem.contents(belt);
            ctx.check("charged whole, both pockets fill", Math.abs(bb.getEnergy(full.get(0)) - bb.getEnergyMax(full.get(0))) < 1
                && Math.abs(cb.getEnergy(full.get(1)) - cb.getEnergyMax(full.get(1))) < 1, "");
            beltItem.setEnergy(belt, max - 1000);   // (a tool drawing on it)
            ctx.check("drawn on, it gives what was asked", Math.abs(beltItem.getEnergy(belt) - (max - 1000)) < 1, "" + beltItem.getEnergy(belt));
            ctx.check("its discharge power is its pockets'", Math.abs(beltItem.getDischagePower(belt) - bb.getDischagePower(bat) - cb.getDischagePower(cond)) < 1e-6, "");
            ctx.check("a source for tools, like a portable battery (priority 2)", beltItem.getPriority(belt) == 2, "");
        }
    }
}
