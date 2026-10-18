package mods.eln.node.six;

import mods.eln.generic.IDescriptorItem;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.ghost.GhostGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The item of one six-node device (1.12: a sub-item of SixNodeItem). Using it on a block face places the device on
 * that face of the air (or six-node) block in front, as in 1.12 placeBlockAt.
 */
public class SixNodeItem extends Item implements IDescriptorItem {
    public final SixNodeDescriptor descriptor;

    public SixNodeItem(Properties properties, SixNodeDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
    }

    @Override
    public Object getEaDescriptor() {
        return descriptor;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        // the block in front of the clicked face, and the element faces back toward the clicked block
        // once cables are voxel cables (ElnConfig [cables] convertOnLoad), the old cable items are coils: ingredients and
        // device parts, not placed (Baughn 2026-10-18); a cable kit lays cable
        if (mods.eln.node.voxel.CableConverter.convertOnLoad && descriptor instanceof mods.eln.sixnode.electricalcable.ElectricalCableDescriptor c
            && mods.eln.node.voxel.CablePart.tierOf(c.name) >= 0) {
            if (ctx.getPlayer() != null) ctx.getPlayer().sendOverlayMessage(net.minecraft.network.chat.Component.literal("Cables are laid with a cable kit (craft one from these)"));
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Direction direction = Direction.fromIntMinecraftSide(ctx.getClickedFace().get3DDataValue()).getInverse();
        boolean ok = placeAt(ctx.getItemInHand(), ctx.getPlayer(), world, pos, direction);
        if (ok && ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** 1.12 placeBlockAt: place this device at pos on its face `direction` (pointing at the supporting block). */
    public boolean placeAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction direction) {
        SixNodeBlock block = SixNodeBlock.get();
        BlockState old = world.getBlockState(pos);
        if (old.isAir() || old.canBeReplaced()) {
            Coordonate coord = new Coordonate(pos, world);
            String error;
            if ((error = descriptor.checkCanPlace(coord, direction, LRDU.Up)) != null) {
                if (player != null) Utils.sendMessage(player, error);
                return false;
            }
            if (!block.getIfOtherBlockIsSolid(world, pos, direction)) return false;
            GhostGroup ghostgroup = descriptor.getGhostGroup(direction, LRDU.Up);
            if (ghostgroup != null) ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

            SixNode sixNode = new SixNode();
            sixNode.onBlockPlacedBy(new Coordonate(pos, world), direction, player, stack);
            sixNode.createSubBlock(stack, direction, player);
            world.setBlock(pos, block.stateFor(world, pos), 3);
            return true;
        } else if (old.getBlock() == block) {
            SixNodeEntity e = (SixNodeEntity) world.getBlockEntity(pos);
            SixNode sixNode = e == null ? null : (SixNode) e.getNode();
            if (sixNode == null) {
                world.removeBlock(pos, false);
                return false;
            }
            if (!sixNode.getSideEnable(direction) && block.getIfOtherBlockIsSolid(world, pos, direction)) {
                sixNode.createSubBlock(stack, direction, player);
                return true;
            }
        }
        return false;
    }
}
