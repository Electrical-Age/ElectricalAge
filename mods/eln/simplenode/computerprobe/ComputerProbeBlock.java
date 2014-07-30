package mods.eln.simplenode.computerprobe;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeBlock;

public class ComputerProbeBlock extends SimpleNodeBlock{

	public ComputerProbeBlock() {
		super(Material.rock);
	}

	@Override
	public TileEntity createNewTileEntity(World var1, int var2) {
		return new ComputerProbeEntity();
	}

	@Override
	protected SimpleNode newNode() {
		return new ComputerProbeNode();
	}

}
