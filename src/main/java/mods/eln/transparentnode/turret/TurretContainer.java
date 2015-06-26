package mods.eln.transparentnode.turret;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.EntitySensorFilterDescriptor;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

public class TurretContainer extends BasicContainer {

    public static final int filterId = 0;

    public TurretContainer(EntityPlayer player, IInventory inventory) {
        super(player, inventory, new Slot[]{
                new GenericItemUsingDamageSlot(inventory, filterId, 184 / 2 - 12, 8, 1,
                        EntitySensorFilterDescriptor.class,
                        SlotSkin.medium,
                        new String[]{"Entity filter slot"}),
        });
    }
}
