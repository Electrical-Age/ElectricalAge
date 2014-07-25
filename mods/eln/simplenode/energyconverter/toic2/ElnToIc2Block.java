package mods.eln.simplenode.energyconverter.toic2;

import mods.eln.node.simple.SimpleNode;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ElnToIc2Block extends EnergyConverterElnToOtherBlock{

	private Class nodeClass;
	private ElnToIc2Descriptor descriptor;

	public ElnToIc2Block(ElnToIc2Descriptor descriptor) {
		super(Material.rock);
		this.descriptor = descriptor;
		setDescriptor(descriptor);
	}


	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		// TODO Auto-generated method stub
		return new ElnToIc2Entity();
	}

	@Override
	protected SimpleNode newNode() {
		return new ElnToIc2Node();
	}

}
