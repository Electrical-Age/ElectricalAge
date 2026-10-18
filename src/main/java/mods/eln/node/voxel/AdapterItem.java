package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A pipe adapter: right-click the face of another mod's pipe or tank (or any block): it goes in the block in front,
 * its flange against that face, with a stub of pipe (iron) to the block's middle, where routes can join it.
 */
public class AdapterItem extends Item implements PipeTool {
    public AdapterItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var face = ctx.getClickedFace();
        var pos = ctx.getClickedPos().relative(face);
        int toward = face.getOpposite().get3DDataValue();   // (the flange faces the block clicked)
        var player = ctx.getPlayer();
        String err = PipeKitItem.place(level, pos, new PipePart(0, AdapterPart.stub(toward)), player);
        if (err == null && level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n) {
            err = n.addPart(new AdapterPart(toward));
            if (err != null) n.removePart(n.parts.size() - 1, false);
        }
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit an adapter here: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
