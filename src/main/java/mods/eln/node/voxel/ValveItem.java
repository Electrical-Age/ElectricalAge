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
            err = mods.eln.i18n.I18N.tr("must fit on a pipe");
            mods.eln.misc.Utils.println("Valve: no pipe at " + pos.toShortString() + ": " + level.getBlockState(pos));
        }
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal(mods.eln.i18n.I18N.tr("Can't fit valve: %1$", err)));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        var list = new java.util.ArrayList<String>();
        switch (mode) {
            case ValvePart.CONTROL -> {
                list.add(mods.eln.i18n.I18N.tr("Signal-controlled pipe valve (0–5 V)."));
                mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Driven by a signal cable in the same block."));
            }
            case ValvePart.THROTTLE -> list.add(mods.eln.i18n.I18N.tr("Manually adjustable pipe throttle valve."));
            case ValvePart.PRESSURE -> {
                list.add(mods.eln.i18n.I18N.tr("One-way pressure valve (relief or reducing)."));
                mods.eln.client.ItemTooltips.detail(list,
                    mods.eln.i18n.I18N.tr("Relief holds upstream pressure; reducing holds downstream pressure."),
                    mods.eln.i18n.I18N.tr("Optionally controlled by a signal cable in its block."));
            }
            default -> list.add(mods.eln.i18n.I18N.tr("Manual shut-off valve for pipes."));
        }
        return list;
    }
}
