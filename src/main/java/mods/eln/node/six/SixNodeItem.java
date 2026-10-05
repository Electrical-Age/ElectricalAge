package mods.eln.node.six;



import net.minecraft.util.math.BlockPos;
import mods.eln.compat.WorldCompat;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import mods.eln.compat.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class SixNodeItem extends GenericItemBlockUsingDamage<SixNodeDescriptor> implements IItemRenderer {

    public SixNodeItem(Block b) {
        super(b);
        setHasSubtypes(true);
        setTranslationKey("SixNodeItem");
    }

    @Override
    public int getMetadata(int damageValue) {
        return damageValue;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int side = facing.getIndex();
        Block block = WorldCompat.getBlock(world, x, y, z);

        if ((block == Blocks.SNOW_LAYER) && ((WorldCompat.getMeta(world, x, y, z) & 0x7) < 1)) {
            side = 1;
        } else if ((block != Blocks.VINE) && (block != Blocks.TALLGRASS) && (block != Blocks.DEADBUSH) && (!block.isReplaceable(world, new BlockPos(x, y, z)))) {
            if (side == 0)
                y--;

            if (side == 1)
                y++;

            if (side == 2)
                z--;

            if (side == 3)
                z++;

            if (side == 4)
                x--;

            if (side == 5)
                x++;
        }

        if (stack.getCount() == 0)
            return EnumActionResult.FAIL;
        BlockPos placePos = new BlockPos(x, y, z);
        if (!player.canPlayerEdit(placePos, EnumFacing.byIndex(side), stack))
            return EnumActionResult.FAIL;
        if ((y == 255) && (this.block.getDefaultState().getMaterial().isSolid()))
            return EnumActionResult.FAIL;

        int i1 = getMetadata(stack.getMetadata());
        int metadata = i1; // 1.7.10: block.onBlockPlaced(...) returned the item meta (not overridden by EA)

        if (placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            IBlockState placed = world.getBlockState(placePos);
            SoundType soundType = this.block.getSoundType(placed, world, placePos, player);
            world.playSound(null, x + 0.5F, y + 0.5F, z + 0.5F, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            stack.shrink(1);
        }

        return EnumActionResult.SUCCESS;
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */

    // func_150936_a <= canPlaceItemBlockOnSide
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World par1World, BlockPos pos, EnumFacing side, EntityPlayer par6EntityPlayer, ItemStack par7ItemStack) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int par5 = side.getIndex();
        if (!isStackValidToPlace(par7ItemStack))
            return false;
        int[] vect = new int[]{x, y, z};
        Direction.fromIntMinecraftSide(par5).applyTo(vect, 1);
        SixNodeDescriptor descriptor = getDescriptor(par7ItemStack);
        if (descriptor.canBePlacedOnSide(par6EntityPlayer, new Coordonate(x, y, z, par1World), Direction.fromIntMinecraftSide(par5).getInverse()) == false) {
            return false;
        }
        if (WorldCompat.getBlock(par1World, vect[0], vect[1], vect[2]) == Eln.sixNodeBlock)
            return true;
        if (super.canPlaceBlockOnSide(par1World, pos, side, par6EntityPlayer, par7ItemStack))
            return true;

        return false;
    }

    public boolean isStackValidToPlace(ItemStack stack) {
        SixNodeDescriptor descriptor = getDescriptor(stack);
        return descriptor != null;
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        if (world.isRemote)
            return false;
        if (!isStackValidToPlace(stack))
            return false;

        Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
        Block blockOld = WorldCompat.getBlock(world, x, y, z);
        SixNodeBlock block = (SixNodeBlock) Block.getBlockFromItem(this);
        if (blockOld == Blocks.AIR || blockOld.isReplaceable(world, new BlockPos(x, y, z))) {
            // blockID = this.getBlockID();

            Coordonate coord = new Coordonate(x, y, z, world);
            SixNodeDescriptor descriptor = getDescriptor(stack);

            String error;
            if ((error = descriptor.checkCanPlace(coord, direction, LRDU.Up)) != null) {
                Utils.sendMessage(player, error);
                return false;
            }

            if (block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                GhostGroup ghostgroup = descriptor.getGhostGroup(direction, LRDU.Up);
                if (ghostgroup != null)
                    ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

                SixNode sixNode = new SixNode();
                sixNode.onBlockPlacedBy(new Coordonate(x, y, z, world), direction, player, stack);
                sixNode.createSubBlock(stack, direction, player);

                WorldCompat.setBlock(world, x, y, z, block, metadata, 0x03);
                block.getIfOtherBlockIsSolid(world, x, y, z, direction);
                block.onBlockPlacedBy(world, x, y, z, Direction.fromIntMinecraftSide(side).getInverse(), player, metadata);
                return true;

            }
        } else if (blockOld == block) {

            SixNode sixNode = (SixNode) ((SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z)).getNode();
            if (sixNode == null) {
                WorldCompat.setBlockToAir(world, x, y, z);
                return false;
            }
            if (sixNode.getSideEnable(direction) == false && block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                sixNode.createSubBlock(stack, direction, player);
                block.onBlockPlacedBy(world, x, y, z, Direction.fromIntMinecraftSide(side).getInverse(), player, metadata);
                return true;
            }

        } else {
            SixNode sixNode = (SixNode) ((SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z)).getNode();
            if (sixNode == null) {
                WorldCompat.setBlockToAir(world, x, y, z);
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        if (getDescriptor(item) == null)
            return false;
        return getDescriptor(item).handleRenderType(item, type);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (!isStackValidToPlace(item))
            return false;
        return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
    }

    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (!isStackValidToPlace(item))
            return false;
        return getDescriptor(item).shouldUseRenderHelperEln(type, item, helper);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (!isStackValidToPlace(item))
            return;

        Minecraft.getMinecraft().profiler.startSection("SixNodeItem");
        if (shouldUseRenderHelperEln(type, item, null)) {
            switch (type) {

                case ENTITY:
                    GL11.glRotatef(90, 0, 0, 1);
                    // GL11.glTranslatef(0, 1, 0);
                    break;

                case EQUIPPED_FIRST_PERSON:
                    GL11.glRotatef(160, 0, 1, 0);
                    GL11.glTranslatef(-0.70f, 1, -0.7f);
                    GL11.glScalef(1.8f, 1.8f, 1.8f);
                    GL11.glRotatef(-90, 1, 0, 0);
                    break;
                case EQUIPPED:
                    GL11.glRotatef(180, 0, 1, 0);
                    GL11.glTranslatef(-0.70f, 1, -0.7f);
                    GL11.glScalef(1.5f, 1.5f, 1.5f);
                    break;
                case FIRST_PERSON_MAP:
                    // GL11.glTranslatef(0, 1, 0);
                    break;
                case INVENTORY:
                    GL11.glRotatef(-90, 0, 1, 0);
                    GL11.glRotatef(-90, 1, 0, 0);
                    break;
                default:
                    break;
            }
        }
        // GL11.glTranslatef(0, 1, 0);
        getDescriptor(item).renderItem(type, item, data);
        Minecraft.getMinecraft().profiler.endSection();
    }
}
