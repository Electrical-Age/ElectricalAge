package mods.eln.node.six;

import mods.eln.node.NodeManager;

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
        if (mods.eln.node.voxel.CableConverter.convertOnLoad && (descriptor instanceof mods.eln.sixnode.electricalcable.ElectricalCableDescriptor
            || descriptor instanceof mods.eln.sixnode.thermalcable.ThermalCableDescriptor) && mods.eln.node.voxel.CablePart.tierOf(descriptor.name) >= 0) {
            if (ctx.getPlayer() != null) ctx.getPlayer().sendOverlayMessage(net.minecraft.network.chat.Component.literal("Cables are laid with a cable kit (craft one from these)"));
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        Direction direction = Direction.fromIntMinecraftSide(ctx.getClickedFace().get3DDataValue()).getInverse();
        // clicking a voxel block (its cables or pipes): the device goes into that block, on the face behind the click
        if (world.getBlockState(ctx.getClickedPos()).getBlock() instanceof mods.eln.node.voxel.VoxelNodeBlock)
            pos = ctx.getClickedPos();
        net.minecraft.world.phys.Vec3 h = ctx.getClickLocation();
        boolean ok = placeAt(ctx.getItemInHand(), ctx.getPlayer(), world, pos, direction,
            new double[]{h.x - pos.getX(), h.y - pos.getY(), h.z - pos.getZ()});
        if (ok && ctx.getPlayer() != null && !ctx.getPlayer().isCreative()) ctx.getItemInHand().shrink(1);
        return ok ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** 1.12 placeBlockAt: place this device at pos on its face `direction` (pointing at the supporting block). */
    public boolean placeAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction direction) {
        return placeAt(stack, player, world, pos, direction, null);
    }

    /** The same; at, if given: where on the face (block-local), for a device that may go anywhere on it (chips). */
    public boolean placeAt(ItemStack stack, Player player, Level world, BlockPos pos, Direction direction, double[] at) {
        SixNodeBlock block = SixNodeBlock.get();
        boolean anywhere = at != null && descriptor.placeAnywhere() && mods.eln.node.voxel.CableConverter.convertOnLoad;
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

            // once six-node blocks are converted (ElnConfig [cables] convertOnLoad), new devices go into voxel blocks
            if (mods.eln.node.voxel.CableConverter.convertOnLoad) {
                var v = new mods.eln.node.voxel.VoxelNode();
                v.onBlockPlacedBy(new Coordonate(pos, world), direction, player, stack);
                if (anywhere) {
                    String err = v.placeAnywhere(stack, direction, at[0], at[1], at[2], player);   // (an empty block: it fits)
                    if (err != null) mods.eln.Eln.LOG.warn("Electrical Age: placing {} at {} in a new block: {}", descriptor.name, pos, err);
                } else v.createSubBlock(stack, direction, player);
                world.setBlock(pos, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().stateFor(world, pos), 3);
                return true;
            }
            SixNode sixNode = new SixNode();
            sixNode.onBlockPlacedBy(new Coordonate(pos, world), direction, player, stack);
            sixNode.createSubBlock(stack, direction, player);
            world.setBlock(pos, block.stateFor(world, pos), 3);
            return true;
        } else if (old.getBlock() instanceof mods.eln.node.voxel.VoxelNodeBlock) {
            // a voxel block: on a free face whose plate is clear of its cables and pipes
            if (!(world.getBlockEntity(pos) instanceof mods.eln.node.voxel.VoxelNodeEntity ve) || !(ve.getNode() instanceof mods.eln.node.voxel.VoxelNode v))
                return false;
            if (anywhere) {
                if (!block.getIfOtherBlockIsSolid(world, pos, direction)) return false;
                String err = v.placeAnywhere(stack, direction, at[0], at[1], at[2], player);
                if (err != null && player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(err));
                return err == null;
            }
            if (v.isSideKept(direction.getInt())) return v.createSubBlock(stack, direction, player);   // (refuses, and says why)
            if (!v.canPlaceElement(direction.getInt())) {
                if (player != null) player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("No room for it on that face"));
                return false;
            }
            if (!block.getIfOtherBlockIsSolid(world, pos, direction)) return false;
            String error = descriptor.checkCanPlace(new Coordonate(pos, world), direction, LRDU.Up);
            if (error != null) {
                if (player != null) Utils.sendMessage(player, error);
                return false;
            }
            return v.createSubBlock(stack, direction, player);
        } else if (old.getBlock() == block) {
            SixNodeEntity e = (SixNodeEntity) world.getBlockEntity(pos);
            SixNode sixNode = e == null ? null : (SixNode) e.getNode();
            if (sixNode == null) {
                // a block whose node this version could not load (kept as saved data): leave it, as for a kept face
                if (NodeManager.instance.hasOrphanAt(new Coordonate(pos, world))) {
                    if (player != null) Utils.sendMessage(player, "This block holds devices this version of Electrical Age cannot load; their data is kept. Break the block to discard them.");
                    return false;
                }
                world.removeBlock(pos, false);
                return false;
            }
            if (!sixNode.getSideEnable(direction) && block.getIfOtherBlockIsSolid(world, pos, direction)) {
                sixNode.createSubBlock(stack, direction, player);
                return true;
            }
            if (sixNode.isSideKept(direction.getInt())) return sixNode.createSubBlock(stack, direction, player);   // (refuses, and says why)
        }
        return false;
    }
}
