package mods.eln.item.electricalitem;

import mods.eln.Eln;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge Energy (FE = RF) on EA's energy-holding items (1.12 port, new): every item whose descriptor is an
 * {@link IItemEnergyBattery} (portable batteries/condensators, flashlights, mining drill, electrical axe, ...) exposes
 * Capabilities.Energy.ITEM, so other mods' chargers can fill it.
 * <ul>
 * <li>Conversion: config balancing.ElnToThermalExpansionConversionRatio (FE per J, default 4/3), as the energy
 * converter.</li>
 * <li>Rates: per call at most one tick (0.05 s) of the item's charge power in, discharge power out (per item of the
 * stack).</li>
 * <li>Only portable batteries/condensators ({@link BatteryItem}) can be extracted from.</li>
 * <li>Energy stays in the stack's "energy" tag through the descriptor; 26.x changes it by exchanging the item in the
 * ItemAccess inside the transaction (so aborted transfers leave the stack alone).</li>
 * </ul>
 */
public final class ItemEnergyCapability {
    private static final double TICK = 0.05;

    private ItemEnergyCapability() {
    }

    static double ratio() {
        return Eln.ElnToTeConversionRatio;
    }

    /** The stack's IItemEnergyBattery (descriptor), or null. */
    @Nullable
    static IItemEnergyBattery battery(ItemStack stack) {
        Object o = Utils.getItemObject(stack);
        return o instanceof IItemEnergyBattery ? (IItemEnergyBattery) o : null;
    }

    public static void register(RegisterCapabilitiesEvent e) {
        List<Item> items = new ArrayList<>();
        for (var h : mods.eln.registry.ElnRegistry.ALL_ITEMS) {
            Item item = h.get();
            if (item instanceof mods.eln.generic.IDescriptorItem d && d.getEaDescriptor() instanceof IItemEnergyBattery) items.add(item);
        }
        if (!items.isEmpty())
            e.registerItem(Capabilities.Energy.ITEM, (stack, access) -> new Handler(access), items.toArray(new Item[0]));
    }

    private static int toInt(double fe) {
        if (!(fe > 0)) return 0;
        return fe >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(fe);
    }

    /** EnergyHandler over the descriptor's getEnergy/setEnergy (J), in FE. */
    static final class Handler implements EnergyHandler {
        final ItemAccess access;

        Handler(ItemAccess access) {
            this.access = access;
        }

        private ItemStack one() {
            return access.getResource().toStack(1);
        }

        @Override
        public long getAmountAsLong() {
            ItemStack s = one();
            IItemEnergyBattery b = battery(s);
            return b == null ? 0 : (long) access.getAmount() * toInt(b.getEnergy(s) * ratio());
        }

        @Override
        public long getCapacityAsLong() {
            ItemStack s = one();
            IItemEnergyBattery b = battery(s);
            return b == null ? 0 : (long) access.getAmount() * toInt(b.getEnergyMax(s) * ratio());
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            int count = access.getAmount();
            if (count <= 0 || amount <= 0) return 0;
            ItemStack s = one();
            IItemEnergyBattery b = battery(s);
            if (b == null) return 0;
            double e = b.getEnergy(s);
            double room = Math.max(0, b.getEnergyMax(s) - e);
            double joules = Math.min(Math.min((amount / count) / ratio(), room), b.getChargePower(s) * TICK);
            int fe = toInt(joules * ratio());
            if (fe <= 0) return 0;
            b.setEnergy(s, e + fe / ratio());
            return fe * access.exchange(ItemResource.of(s), count, transaction);
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            int count = access.getAmount();
            if (count <= 0 || amount <= 0) return 0;
            ItemStack s = one();
            IItemEnergyBattery b = battery(s);
            if (!(b instanceof BatteryItem)) return 0;
            double e = b.getEnergy(s);
            double joules = Math.min(Math.min((amount / count) / ratio(), e), b.getDischagePower(s) * TICK);
            int fe = toInt(joules * ratio());
            if (fe <= 0) return 0;
            b.setEnergy(s, Math.max(0, e - fe / ratio()));
            return fe * access.exchange(ItemResource.of(s), count, transaction);
        }
    }
}
