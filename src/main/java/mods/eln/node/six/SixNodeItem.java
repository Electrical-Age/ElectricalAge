package mods.eln.node.six;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.ghost.GhostGroup;
import mods.eln.init.ModBlock;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class SixNodeItem extends GenericItemBlockUsingDamage<SixNodeDescriptor> {
    public SixNodeItem(Block b) {
        super(b);
        setHasSubtypes(true);
        setTranslationKey("SixNodeItem");
    }

    @Override
    public int getMetadata(int damageValue) {
        return damageValue;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack stack = player.getHeldItem(hand);
        Block block = world.getBlockState(pos).getBlock();
        int side = facing.getIndex();
        if ((block == Blocks.SNOW_LAYER) && ((Utils.getMetaFromPos(world, pos) & 0x7) < 1)) {
            side = 1;
        } else if ((block != Blocks.VINE) && (block != Blocks.TALLGRASS) && (block != Blocks.DEADBUSH) && (!block.isReplaceable(world, pos))) {
            if (side == 0) pos = pos.add(0,-1,0);
            if (side == 1) pos = pos.add(0,1,0);
            if (side == 2) pos = pos.add(0,0,-1);
            if (side == 3) pos = pos.add(0,0,1);
            if (side == 4) pos = pos.add(-1,0,0);
            if (side == 5) pos = pos.add(1,0,0);
        }
        if (stack.isEmpty())
            return EnumActionResult.FAIL;
        if (!player.canPlayerEdit(pos, EnumFacing.getFacingFromVector(hitX,hitY,hitZ), stack))
            return EnumActionResult.FAIL;
        if ((pos.getY() == 255) && (this.block.getMaterial(world.getBlockState(pos)).isSolid()))
            return EnumActionResult.FAIL;

        // TODO(1.12): Whatever, we're discarding all this code. Thank god.
/*
        int i1 = getMetadata(stack.getItemDamage());
        int metadata = this.block.getMetaFromState(this.block.onBlockPlaced(world, pos, facing, hitX, hitY, hitZ, i1, player));
        if (placeBlockAt(stack, player, world, pos, EnumFacing.getFacingFromVector(hitX, hitY, hitZ), hitX, hitY, hitZ, block.getStateFromMeta(metadata))) {
            world.playSound(player, new BlockPos(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F), this.block.getSoundType().getStepSound(), SoundCategory.BLOCKS, this.block.getSoundType().volume + 1.0F / 2.0F, this.block.getSoundType().getPitch() * 0.8F);
            stack.stackSize -= 1;
        }
*/
        return EnumActionResult.SUCCESS;
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */
    // func_150936_a <= canPlaceItemBlockOnSide
    @Override
    public boolean canPlaceBlockOnSide(World par1World, BlockPos pos, EnumFacing side, EntityPlayer par6EntityPlayer, ItemStack par7ItemStack) {
        if (!isStackValidToPlace(par7ItemStack))
            return false;
        int[] vect = new int[]{pos.getX(), pos.getY(), pos.getZ()};
        Direction.fromFacing(side).applyTo(vect, 1);
        SixNodeDescriptor descriptor = getDescriptor(par7ItemStack);
        if (descriptor.canBePlacedOnSide(par6EntityPlayer, new Coordinate(pos, par1World), Direction.fromFacing(side).getInverse()))
            return false;
        if (par1World.getBlockState(new BlockPos(vect[0], vect[1], vect[2])).getBlock() == ModBlock.sixNodeBlock)
            return true;
        return super.canPlaceBlockOnSide(par1World, pos, side, par6EntityPlayer, par7ItemStack);
    }

    public boolean isStackValidToPlace(ItemStack stack) {
        SixNodeDescriptor descriptor = getDescriptor(stack);
        return descriptor != null;
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world,BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state) {
        if (world.isRemote)  return false;
        if (!isStackValidToPlace(stack)) return false;
        Direction direction = Direction.fromFacing(side).getInverse();
        Block blockOld = world.getBlockState(pos).getBlock();
        SixNodeBlock block = (SixNodeBlock) Block.getBlockFromItem(this);
        if (world.isAirBlock(pos) || blockOld.isReplaceable(world, pos)) {
            // blockID = this.getBlockID();
            Coordinate coord = new Coordinate(pos, world);
            SixNodeDescriptor descriptor = getDescriptor(stack);
            String error;
            if ((error = descriptor.checkCanPlace(coord, direction, LRDU.Up)) != null) {
                Utils.sendMessage(player, error);
                return false;
            }
            if (block.getIfOtherBlockIsSolid(world, pos, direction)) {
                GhostGroup ghostgroup = descriptor.getGhostGroup(direction, LRDU.Up);
                if (ghostgroup != null)
                    ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());
                SixNode sixNode = new SixNode();
                sixNode.onBlockPlacedBy(new Coordinate(pos, world), direction, player, stack);
                sixNode.createSubBlock(stack, direction, player);
                world.setBlockState(pos, block.getStateFromMeta( block.getMetaFromState(state) & 0x03));
                block.getIfOtherBlockIsSolid(world, pos, direction);
                block.onBlockPlacedBy(world, pos, Direction.fromFacing(side).getInverse(), player, state);
                return true;
            }
        } else if (blockOld == block) {
            SixNode sixNode = (SixNode) ((SixNodeEntity) world.getTileEntity(pos)).getNode();
            if (sixNode == null) {
                world.setBlockToAir(pos);
                return false;
            }
            if (!sixNode.getSideEnable(direction) && block.getIfOtherBlockIsSolid(world, pos, direction)) {
                sixNode.createSubBlock(stack, direction, player);
                block.onBlockPlacedBy(world, pos, Direction.fromFacing(side).getInverse(), player, state);
                return true;
            }
        } else {
            SixNode sixNode = (SixNode) ((SixNodeEntity) world.getTileEntity(pos)).getNode();
            if (sixNode == null) {
                world.setBlockToAir(pos);
                return false;
            }
        }
        return false;
    }

    // TODO(1.10): Fix item rendering.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        if (getDescriptor(item) == null)
//            return false;
//        return getDescriptor(item).handleRenderType(item, type);
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        if (!isStackValidToPlace(item))
//            return false;
//        return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
//    }
//
//    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        if (!isStackValidToPlace(item))
//            return false;
//        return getDescriptor(item).shouldUseRenderHelperEln(type, item, helper);
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (!isStackValidToPlace(item))
//            return;
//
//        Minecraft.getMinecraft().profiler.startSection("SixNodeItem");
//        if (shouldUseRenderHelperEln(type, item, null)) {
//            switch (type) {
//
//                case ENTITY:
//                    GL11.glRotatef(90, 0, 0, 1);
//                    // GL11.glTranslatef(0, 1, 0);
//                    break;
//
//                case EQUIPPED_FIRST_PERSON:
//                    GL11.glRotatef(160, 0, 1, 0);
//                    GL11.glTranslatef(-0.70f, 1, -0.7f);
//                    GL11.glScalef(1.8f, 1.8f, 1.8f);
//                    GL11.glRotatef(-90, 1, 0, 0);
//                    break;
//                case EQUIPPED:
//                    GL11.glRotatef(180, 0, 1, 0);
//                    GL11.glTranslatef(-0.70f, 1, -0.7f);
//                    GL11.glScalef(1.5f, 1.5f, 1.5f);
//                    break;
//                case FIRST_PERSON_MAP:
//                    // GL11.glTranslatef(0, 1, 0);
//                    break;
//                case INVENTORY:
//                    GL11.glRotatef(-90, 0, 1, 0);
//                    GL11.glRotatef(-90, 1, 0, 0);
//                    break;
//                default:
//                    break;
//            }
//        }
//        // GL11.glTranslatef(0, 1, 0);
//        getDescriptor(item).renderItem(type, item, data);
//        Minecraft.getMinecraft().profiler.endSection();
//    }
}
