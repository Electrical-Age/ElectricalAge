package mods.eln.simplenode.energyconverter;


import mods.eln.node.simple.SimpleNode;
import mods.eln.node.simple.SimpleNodeBlock;
import mods.eln.node.simple.SimpleNodeEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.12: the 1.7.10 per-side icons (getIcon by the entity's front) are a blockstate property: FRONT = the entity's
 * front as an EnumFacing index, 6 = unknown (all faces "side"). Models (blockstates/energy_converter_eln_to_other_*):
 * front face elntoic2lvu_eln (elntoic2lvu_eln2 when front is north or east, minecraft sides 2/5), back face
 * elntoic2lvu_ic2, other faces elntoic2lvu_side. Item model = front east (1.7.10 getIcon(side, meta): XP eln, XN ic2).
 * Not stored in metadata (always 0); read from the tile entity in getActualState.
 */
public class EnergyConverterElnToOtherBlock extends SimpleNodeBlock {

    public static final PropertyInteger FRONT = PropertyInteger.create("front", 0, 6);

    private EnergyConverterElnToOtherDescriptor descriptor;

    public EnergyConverterElnToOtherBlock(EnergyConverterElnToOtherDescriptor descriptor) {
        super(Material.PACKED_ICE);
        this.descriptor = descriptor;
        setDescriptor(descriptor);
        setDefaultState(blockState.getBaseState().withProperty(FRONT, 6));
    }

    public EnergyConverterElnToOtherDescriptor getDescriptor() {
        return descriptor;
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
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FRONT);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        if (!(te instanceof SimpleNodeEntity)) return state.withProperty(FRONT, 6);
        SimpleNodeEntity e = (SimpleNodeEntity) te;
        if (e.front == null) return state.withProperty(FRONT, 6);
        return state.withProperty(FRONT, e.front.toEnumFacing().getIndex());
    }
}
