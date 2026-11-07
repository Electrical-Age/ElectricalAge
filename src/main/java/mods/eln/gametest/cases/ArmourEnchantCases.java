package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

/** E-Coal armour is enchantable (Baughn 2026-11-07): an enchanting table's value, and the armour enchantments apply. */
public final class ArmourEnchantCases {
    private ArmourEnchantCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new TestCase() {
            public String id() {
                return "ecoal_armour_enchantable";
            }

            public int ticks() {
                return 1;
            }

            public void build(TestContext ctx) {
            }

            public void measure(TestContext ctx) {
                var reg = ctx.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var prot = reg.getOrThrow(Enchantments.PROTECTION);
                var unb = reg.getOrThrow(Enchantments.UNBREAKING);
                var feather = reg.getOrThrow(Enchantments.FEATHER_FALLING);
                for (String n : new String[]{"e_coal_helmet", "e_coal_chestplate", "e_coal_leggings", "e_coal_boots"}) {
                    ItemStack s = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("eln", n)));
                    var e = s.get(net.minecraft.core.component.DataComponents.ENCHANTABLE);
                    ctx.check(n + ": enchantable at a table, Protection and Unbreaking apply", e != null && e.value() > 0
                        && prot.value().canEnchant(s) && unb.value().canEnchant(s), "enchantable " + e);
                }
                ItemStack boots = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("eln", "e_coal_boots")));
                ItemStack helmet = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("eln", "e_coal_helmet")));
                ctx.check("Feather Falling on the boots only", feather.value().canEnchant(boots) && !feather.value().canEnchant(helmet), "");
            }
        });
    }
}
