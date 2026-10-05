package mods.eln.item.electricalitem;

import mods.eln.Other;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.misc.Utils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * Forge Energy (FE = RF) on EA's energy-holding items (1.12 port, new): every stack whose item or descriptor is an
 * {@link IItemEnergyBattery} (portable batteries/condensators, flashlights, mining drill, electrical axe, X-ray
 * scanner, E-Coal armour) exposes {@link CapabilityEnergy#ENERGY}, so other mods' chargers can fill it.
 * <ul>
 * <li>Conversion: config balancing.ElnToThermalExpansionConversionRatio ({@link Other#getElnToTeConversionRatio}, RF
 * per J, default 4/3), the same ratio EA's energy converter uses towards RF.</li>
 * <li>Rates: per call at most one tick (0.05 s) of the item's charge power in, discharge power out.</li>
 * <li>Only portable batteries/condensators ({@link BatteryItem}) can be extracted from; tools, lamps, scanner and
 * armour only receive (they have no discharge power in EA).</li>
 * <li>No own NBT: energy stays in the stack's "energy" tag through the descriptor (read on every call), so stacks
 * stay comparable and EA's ItemEnergyInventoryProcess sees the same value.</li>
 * </ul>
 * Registered on the Forge bus by Wp12Content.preInit.
 */
public class ItemEnergyCapability {
    public static final ResourceLocation KEY = new ResourceLocation("eln", "item_energy");
    private static final double TICK = 0.05;

    @SubscribeEvent
    public void attach(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        Item item = stack.getItem();
        // cheap guard first: this runs for every ItemStack constructed
        if (!(item instanceof GenericItemUsingDamage) && !(item instanceof IItemEnergyBattery)) return;
        if (battery(stack) == null) return;
        event.addCapability(KEY, new Provider(stack));
    }

    /** The stack's IItemEnergyBattery (descriptor or item), or null. */
    @Nullable
    static IItemEnergyBattery battery(ItemStack stack) {
        Object o = Utils.getItemObject(stack);
        return o instanceof IItemEnergyBattery ? (IItemEnergyBattery) o : null;
    }

    static double ratio() {
        return Other.getElnToTeConversionRatio();
    }

    private static final class Provider implements ICapabilityProvider {
        private final Storage storage;

        Provider(ItemStack stack) {
            storage = new Storage(stack);
        }

        @Override
        public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
            return capability == CapabilityEnergy.ENERGY && battery(storage.stack) != null;
        }

        @Override
        @Nullable
        public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
            return hasCapability(capability, facing) ? CapabilityEnergy.ENERGY.cast(storage) : null;
        }
    }

    /** IEnergyStorage over the descriptor's getEnergy/setEnergy (J), in FE. */
    public static final class Storage implements IEnergyStorage {
        final ItemStack stack;

        public Storage(ItemStack stack) {
            this.stack = stack;
        }

        private static int toInt(double fe) {
            if (!(fe > 0)) return 0;
            return fe >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.floor(fe);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            IItemEnergyBattery b = battery(stack);
            if (b == null || maxReceive <= 0) return 0;
            double e = b.getEnergy(stack);
            double room = Math.max(0, b.getEnergyMax(stack) - e);
            double joules = Math.min(Math.min(maxReceive / ratio(), room), b.getChargePower(stack) * TICK);
            int fe = toInt(joules * ratio());
            if (!simulate && fe > 0) b.setEnergy(stack, e + fe / ratio());
            return fe;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            IItemEnergyBattery b = battery(stack);
            if (b == null || maxExtract <= 0 || !canExtract()) return 0;
            double e = b.getEnergy(stack);
            double joules = Math.min(Math.min(maxExtract / ratio(), e), b.getDischagePower(stack) * TICK);
            int fe = toInt(joules * ratio());
            if (!simulate && fe > 0) b.setEnergy(stack, Math.max(0, e - fe / ratio()));
            return fe;
        }

        @Override
        public int getEnergyStored() {
            IItemEnergyBattery b = battery(stack);
            return b == null ? 0 : toInt(b.getEnergy(stack) * ratio());
        }

        @Override
        public int getMaxEnergyStored() {
            IItemEnergyBattery b = battery(stack);
            return b == null ? 0 : toInt(b.getEnergyMax(stack) * ratio());
        }

        @Override
        public boolean canExtract() {
            return battery(stack) instanceof BatteryItem;
        }

        @Override
        public boolean canReceive() {
            return battery(stack) != null;
        }
    }
}
