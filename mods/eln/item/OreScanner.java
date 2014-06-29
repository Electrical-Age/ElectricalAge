package mods.eln.item;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class OreScanner extends GenericItemUsingDamageDescriptorUpgrade{

	public OreScanner(
			String name
			
			) {
		super(name);
		// TODO Auto-generated constructor stub
	}


	
	@Override
	public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer,
			List list, boolean par4) {
		// TODO Auto-generated method stub
		super.addInformation(itemStack, entityPlayer, list, par4);
		/*
		list.add("Nominal :");
		list.add(Utils.plotEnergy("Energy per Operation :",OperationEnergy));
		list.add("Scan Area :" + (radius*2 +1)*(radius*2 +1) + " blocks");
*/
	}




}
