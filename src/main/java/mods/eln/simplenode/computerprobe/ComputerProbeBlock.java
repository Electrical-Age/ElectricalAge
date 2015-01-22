package mods.eln.simplenode.computerprobe;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import mods.eln.misc.Direction;
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

	
	
	public IIcon getIcon(int side, int meta) {
		return icon[side];
	};

	
	

	
	private IIcon[] icon = new IIcon[6];
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register)
    {
        icon[4] = register.registerIcon("eln:computerprobe_xn");
        icon[5] = register.registerIcon("eln:computerprobe_xp");
        icon[2] = register.registerIcon("eln:computerprobe_zn");
        icon[3] = register.registerIcon("eln:computerprobe_zp");
        icon[0] = register.registerIcon("eln:computerprobe_y");
        icon[1] = icon[0];
    }
}
