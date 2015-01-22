package mods.eln.simplenode.test;

import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeBlock;

public class TestBlock extends SimpleNodeBlock{

	public TestBlock() {
		super(Material.rock);
	}


	@Override
	public TileEntity createNewTileEntity(World var1, int meta) {
		return new TestEntity();
	}

	@Override
	protected SimpleNode newNode() {
		return new TestNode();
	}

}
