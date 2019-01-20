package mods.eln.block;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.Eln;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

public class ArcMetalBlock extends Block {

    private static final String name = "arc_metal_block";
    private IIcon icon;

    public ArcMetalBlock() {
        super(Material.rock);
        setBlockName(name);
        setBlockTextureName("eln:" + name);
        setCreativeTab(Eln.creativeTab);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.icon = iconRegister.registerIcon("eln:" + name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int damage) {
        return this.icon;
    }
}
