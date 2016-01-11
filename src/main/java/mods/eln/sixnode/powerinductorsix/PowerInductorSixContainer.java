package mods.eln.sixnode.powerinductorsix;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.CopperCableDescriptor;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.BasicContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class PowerInductorSixContainer extends BasicContainer {
    
	static final int cableId = 0;
	static final int coreId = 1;
	
	public PowerInductorSixContainer(EntityPlayer player, IInventory inventory) {
		super(player, inventory, new Slot[]{
			new GenericItemUsingDamageSlot(inventory, cableId, 132, 8, 19, CopperCableDescriptor.class,
				SlotSkin.medium,
				new String[]{tr("Copper cable slot"), tr("(Increases inductance value)")}),
			new GenericItemUsingDamageSlot(inventory, coreId, 132 + 20, 8, 1, FerromagneticCoreDescriptor.class,
				SlotSkin.medium, new String[]{tr("Ferromagnetic core slot")})
		});
	}
}
/*				new SlotFilter(inventory, 0, 62 + 0, 17, new ItemStackFilter[]{new ItemStackFilter(Block.wood, 0, 0)}),
                new SlotFilter(inventory, 1, 62 + 18, 17, new ItemStackFilter[]{new ItemStackFilter(Item.coal, 0, 0)})
*/
