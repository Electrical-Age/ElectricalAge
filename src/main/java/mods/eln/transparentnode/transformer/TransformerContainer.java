package mods.eln.transparentnode.transformer;

import mods.eln.generic.GenericItemUsingDamageSlot;
import mods.eln.gui.ISlotSkin.SlotSkin;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.BasicContainer;
import mods.eln.node.six.SixNodeItemSlot;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;

import static mods.eln.i18n.I18N.tr;

public class TransformerContainer extends BasicContainer {
	public static final int primaryCableSlotId = 0;
	public static final int secondaryCableSlotId = 1;
	public static final int ferromagneticSlotId = 2;
	
	public TransformerContainer(EntityPlayer player, IInventory inventory) {
		
		super(player, inventory,new Slot[]{
				new SixNodeItemSlot(inventory,primaryCableSlotId,58,30,4,new Class[]{ElectricalCableDescriptor.class},
					SlotSkin.medium,
					new String[]{tr("Electrical cable slot")}),
				new SixNodeItemSlot(inventory,secondaryCableSlotId,100,30,4,
					new Class[]{ElectricalCableDescriptor.class}, SlotSkin.medium,
					new String[]{tr("Electrical cable slot")}),
				new GenericItemUsingDamageSlot(inventory,ferromagneticSlotId,58 + (100-58)/2 + 0,30,1,
					new Class[]{FerromagneticCoreDescriptor.class},SlotSkin.medium,
					new String[]{tr("Ferromagnetic core slot")})
			
				//	new SlotFilter(inventory,1,62 + 18,17,1,new ItemStackFilter[]{new ItemStackFilter(Eln.sixNodeBlock,0xFF,Eln.electricalCableId)})
			});
	}


}
