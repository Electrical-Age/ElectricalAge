package mods.eln.simplenode.energyconverter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import mods.eln.misc.Direction;
import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeBlock;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class EnergyConverterElnToOtherBlock extends SimpleNodeBlock {

    private EnergyConverterElnToOtherDescriptor descriptor;

    private IIcon elnIcon, eln2Icon, sideIcon, tbIcon;

    public EnergyConverterElnToOtherBlock(EnergyConverterElnToOtherDescriptor descriptor) {
        super(Material.packedIce);
        this.descriptor = descriptor;
        setDescriptor(descriptor);
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new EnergyConverterElnToOtherEntity();
    }

    @Override
    protected SimpleNode newNode() {
        return new EnergyConverterElnToOtherNode();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess w, int x, int y, int z, int side) {
        SimpleNodeEntity e = (SimpleNodeEntity) w.getTileEntity(x, y, z);
        Direction s = Direction.fromIntMinecraftSide(side);
        if (e == null) return sideIcon;
        if (e.front == null) return sideIcon;
        if (e.front == s) return elnIcon;
        if (e.front.back() == s) return blockIcon;
        return sideIcon;
    }

    public IIcon getIcon(int side, int meta) {
        Direction s = Direction.fromIntMinecraftSide(side);
        if (s == Direction.XP) return elnIcon;
        if (s == Direction.XN) return blockIcon;
		if (s == Direction.YN || s == Direction.YP) return tbIcon;
        return sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        this.blockIcon = register.registerIcon("eln:elntootherlvu_other");
        this.elnIcon = register.registerIcon("eln:elntootherlvu_eln");
        this.sideIcon = register.registerIcon("eln:elntootherlvu_side");
		this.tbIcon = register.registerIcon("eln:elntootherlvu_tb");
    }
}
