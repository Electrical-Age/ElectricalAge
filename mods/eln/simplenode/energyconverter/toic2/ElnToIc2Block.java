package mods.eln.simplenode.energyconverter.toic2;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeEntity;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
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

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(IBlockAccess w, int x, int y, int z, int side) {
		SimpleNodeEntity e = (SimpleNodeEntity) w.getTileEntity(x, y, z);
		Direction s = Direction.fromIntMinecraftSide(side);
		if(e.front == null) return sideIcon;
		if(e.front == s) return elnIcon;
		if(e.front.back() == s) return blockIcon;
		return sideIcon;
		
	}


	private IIcon elnIcon;
	private IIcon sideIcon;
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register)
    {
        this.blockIcon = register.registerIcon("eln:elntoic2lvu_ic2");
        this.elnIcon = register.registerIcon("eln:elntoic2lvu_eln");
        this.sideIcon = register.registerIcon("eln:elntoic2lvu_side");
    }

}
