package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.node.simple.SimpleNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ElnToIc2Block extends EnergyConverterElnToOtherBlock{

	private Class nodeClass;

	public ElnToIc2Block(Class nodeClass) {
		super(Material.rock);
		this.nodeClass = nodeClass;
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		// TODO Auto-generated method stub
		return new ElnToIc2Entity();
	}

	@Override
	protected SimpleNode newNode() {
		// TODO Auto-generated method stub
		try {
			return (SimpleNode) nodeClass.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
