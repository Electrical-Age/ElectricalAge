package mods.eln.ore;

import net.minecraft.block.Block;
import mods.eln.CommonProxy;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import cpw.mods.fml.common.registry.GameRegistry;

public class OreItem extends GenericItemBlockUsingDamage<OreDescriptor>{

	public OreItem(Block b ) {
		super(b);
		
	}


	@Override
	public int getMetadata(int par1) {
		
		return par1;
	}
/*//caca1.5.1
	@Override
	public String getTextureFile() {
		
		return CommonProxy.BLOCK_PNG;
	}
	*/
	
	@Override
	public void addDescriptor(int damage, OreDescriptor descriptor) {
		
		super.addDescriptor(damage, descriptor);
		GameRegistry.registerWorldGenerator(descriptor,0);
		
	}

}
