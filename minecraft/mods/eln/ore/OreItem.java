package mods.eln.ore;

import mods.eln.CommonProxy;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import cpw.mods.fml.common.registry.GameRegistry;

public class OreItem extends GenericItemBlockUsingDamage<OreDescriptor>{

	public OreItem(int id) {
		super(id);
		// TODO Auto-generated constructor stub
	}


	@Override
	public int getMetadata(int par1) {
		// TODO Auto-generated method stub
		return par1;
	}
/*//caca1.5.1
	@Override
	public String getTextureFile() {
		// TODO Auto-generated method stub
		return CommonProxy.BLOCK_PNG;
	}
	*/
	
	@Override
	public void addDescriptor(int damage, OreDescriptor descriptor) {
		// TODO Auto-generated method stub
		super.addDescriptor(damage, descriptor);
		GameRegistry.registerWorldGenerator(descriptor);
		
	}

}
