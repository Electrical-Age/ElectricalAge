package mods.eln.transparentnode.autominer;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.ElectricalDrillDescriptor;
import mods.eln.item.MiningPipeDescriptor;
import mods.eln.item.OreScanner;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class AutoMinerContainer extends BasicContainer {

    public static final int electricalDrillSlotId = 0;
    public static final int MiningPipeSlotId = 2;
    public static final int StorageStartId = 3;
    public static final int StorageSize = 0;
    public static final int inventorySize = StorageStartId + StorageSize;

    public AutoMinerContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, newSlots(inventory));
    }

    public static Slot[] newSlots(IInventory inventory) {
        Slot[] slots = new Slot[StorageStartId + StorageSize];
        slots[0] = new GenericItemUsingDamageSlot(inventory, electricalDrillSlotId, 134, 8, 1,
            ElectricalDrillDescriptor.class, SlotSkin.medium,
            new String[]{tr("Drill slot")});
        slots[1] = new GenericItemUsingDamageSlot(inventory, 1, 3000, 3000, 1,
            OreScanner.class, SlotSkin.medium, new String[]{tr("Ore scanner slot")});
        slots[2] = new GenericItemUsingDamageSlot(inventory, MiningPipeSlotId, 134 + 18, 8, 64,
            MiningPipeDescriptor.class, SlotSkin.medium, new String[]{tr("Mining pipe slot")});

        return slots;
    }
}
