package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** A pipe valve: right-click a straight stretch of pipe to fit it there (at the lattice point nearest the click). Of a
 *  kind (ValvePart.ON_OFF, CONTROL, THROTTLE). */
public class ValveItem extends Item implements PipeTool {
    final int mode;

    public ValveItem(Properties p) {
        this(p, ValvePart.ON_OFF);
    }

    public ValveItem(Properties p, int mode) {
        super(p);
        this.mode = mode;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        var player = ctx.getPlayer();
        // (thin pipes are easy to miss: if the click went past, the pipe the player looks at)
        var hit = ctx.getClickLocation();
        if (!(level.getBlockEntity(pos) instanceof VoxelNodeEntity) && player != null
            && player.pick(player.blockInteractionRange(), 1f, false) instanceof net.minecraft.world.phys.BlockHitResult bh
            && level.getBlockEntity(bh.getBlockPos()) instanceof VoxelNodeEntity) {
            pos = bh.getBlockPos();
            hit = bh.getLocation();   // (AdventurAgent: with the other block's click spot, no part was found there)
        }
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n)
            err = n.addValveNear(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ(), mode);
        else {
            err = "fit it on a pipe";
            mods.eln.misc.Utils.println("Valve: no pipe at " + pos.toShortString() + ": " + level.getBlockState(pos));
        }
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal("Can't fit a valve: " + err));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Its tooltip lines (client ItemTooltips), or null. */
    public java.util.List<String> tooltip() {
        if (mode != ValvePart.PRESSURE) return null;
        return java.util.List.of("A pressure valve, one way along its arrow; right-click it to set it.",
            "Relief holds the pressure before it, reducing the pressure after it.",
            "A signal cable in its block sets it.");
    }
}
