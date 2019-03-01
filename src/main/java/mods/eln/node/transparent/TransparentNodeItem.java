package mods.eln.node.transparent;

import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TransparentNodeItem extends GenericItemBlockUsingDamage<TransparentNodeDescriptor> {


    public TransparentNodeItem(Block b) {
        super(b);
        setHasSubtypes(true);
        setUnlocalizedName("TransparentNodeItem");
    }


    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState state) {
        if (world.isRemote) return false;
        TransparentNodeDescriptor descriptor = getDescriptor(stack);
        Direction direction = Direction.fromFacing(side).getInverse();
        Direction front = descriptor.getFrontFromPlace(direction, player);
        int[] v = new int[]{descriptor.getSpawnDeltaX(), descriptor.getSpawnDeltaY(), descriptor.getSpawnDeltaZ()};
        front.rotateFromXN(v);
        pos = pos.add(v[0], v[1], v[2]);

        Block bb = world.getBlockState(pos).getBlock();
        if (bb.isReplaceable(world, pos)) ;
        //if(world.getBlock(x, y, z) != Blocks.air) return false;

        Coordinate coord = new Coordinate(pos, world);


        String error;
        if ((error = descriptor.checkCanPlace(coord, front)) != null) {
            Utils.sendMessage(player, error);
            return false;
        }

        GhostGroup ghostgroup = descriptor.getGhostGroup(front);
        if (ghostgroup != null) ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

        TransparentNode node = new TransparentNode();
        node.onBlockPlacedBy(coord, front, player, stack);
        //TODO: Probably use getStateForPlacement instead
        world.setBlockState(pos, Block.getBlockFromItem(this).getStateFromMeta(node.getBlockMetadata() & 0x03));//caca1.5.1
        ((NodeBlock) Block.getBlockFromItem(this)).onBlockPlacedBy(world, pos, direction, player, state);


        node.checkCanStay(true);

        return true;

    }

    // TODO(1.10): Fix item rendering.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        TransparentNodeDescriptor d = getDescriptor(item);
//        if (Utils.nullCheck(d)) return false;
//        return d.handleRenderType(item, type);
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//                                         ItemRendererHelper helper) {
//
//        return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
//    }
//
//    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return getDescriptor(item).shouldUseRenderHelperEln(type, item, helper);
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        Minecraft.getMinecraft().mcProfiler.startSection("TransparentNodeItem");
//
//        if (shouldUseRenderHelperEln(type, item, null)) {
//            switch (type) {
//                case ENTITY:
//                    GL11.glTranslatef(0.00f, 0.3f, 0.0f);
//                    break;
//                case EQUIPPED_FIRST_PERSON:
//                    GL11.glTranslatef(0.50f, 1, 0.5f);
//                    break;
//                case EQUIPPED:
//                    GL11.glTranslatef(0.50f, 1, 0.5f);
//                    break;
//                case FIRST_PERSON_MAP:
//                    break;
//                case INVENTORY:
//                    GL11.glRotatef(90, 0, 1, 0);
//                    break;
//                default:
//                    break;
//            }
//        }
//        getDescriptor(item).renderItem(type, item, data);
//
//        Minecraft.getMinecraft().mcProfiler.endSection();
//    }
}
