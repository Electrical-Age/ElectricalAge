package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A safety valve, purge valve or breather (SafetyValvePart's modes): right-click a straight stretch of pipe; its outlet
 * points out of the pipe's most open side (up if it can). Or the upper end of a pipe that ends inside its block: it
 * stands on the end. Or a tank: on a stub over its top port (onTank).
 */
public class SafetyValveItem extends Item implements PipeTool {
    /** SafetyValvePart.SAFETY, PURGE or BREATHER. */
    final int mode;

    public SafetyValveItem(Properties p) {
        this(p, SafetyValvePart.SAFETY);
    }

    public SafetyValveItem(Properties p, int mode) {
        super(p);
        this.mode = mode;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        var pos = ctx.getClickedPos();
        var player = ctx.getPlayer();
        var hit = ctx.getClickLocation();
        var look = ValveItem.lookedAt(level, pos, player);
        if (look != null) {
            pos = look.getBlockPos();
            hit = look.getLocation();
        }
        String err;
        if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n)
            err = n.addSafetyValveAimed(VoxelNode.eyeOf(player), hit, mode);
        else if (mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(pos, level))
            instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof mods.eln.oil.TankElement)
            err = onTank(level, pos, player, mode);
        else err = mods.eln.i18n.I18N.tr("must fit on a pipe");
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal(switch (mode) {
                case SafetyValvePart.PURGE -> mods.eln.i18n.I18N.tr("Can't fit purge valve: %1$", err);
                case SafetyValvePart.BREATHER -> mods.eln.i18n.I18N.tr("Can't fit breather valve: %1$", err);
                default -> mods.eln.i18n.I18N.tr("Can't fit safety valve: %1$", err);
            }));
            return InteractionResult.FAIL;
        }
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** The stub a valve clicked on a tank stands on: this far up out of its top port (voxels; half a block of pipe). */
    public static final int STUB = 8;

    /**
     * The valve clicked on a tank (anywhere on it; Baughn 2026-11-28, after Lashtear 2026-11-28: a breather fitted
     * only on a pipe, and the stub to hold one over a tank could not be laid without scaffolding, there being no
     * face over a tank to click): a pipe STUB long is stood on the tank's free top port in the block above, and the
     * valve on its end (VoxelNode.addSafetyValveOnEnd). The pipe is taken from a kit the player carries (in the
     * inventory or a Toolbag there, iron before alloy; nothing is free, but in creative), and comes back when the stub
     * is broken, as any pipe. All or nothing. Null if done, else why not: the port already coupled, a tank of a
     * box's lower rows (no top port), what is in the way above, no pipe carried.
     */
    static String onTank(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player, int mode) {
        mods.eln.sim.fluid.FluidDevice.FluidPort top = null;
        for (var p : mods.eln.fluid.FluidPlacement.portsAt(level, pos)) if (p.face() == 1) top = p;
        if (top == null) return mods.eln.i18n.I18N.tr("this tank has no top port (click one in the top row)");
        if (!mods.eln.fluid.FluidPlacement.partners(level, top).isEmpty()) return mods.eln.i18n.I18N.tr("its top port is already connected");
        var above = pos.above();
        var state = level.getBlockState(above);
        VoxelNode old = level.getBlockEntity(above) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
        if (old == null && !Mimic.allowed(state) && !(state.isAir() || state.canBeReplaced()))
            return mods.eln.i18n.I18N.tr("the %1$ over the tank is in the way", state.getBlock().getName().getString());
        if (old != null && old.safetyValveTaken() != null) return old.safetyValveTaken();
        boolean free = player != null && player.isCreative();
        KitSupply kit = free ? null : KitSupply.pipeCarried(player, STUB);
        if (!free && kit == null) return mods.eln.i18n.I18N.tr("needs half a block of pipe from a pipe kit in your inventory");
        int x = top.at() / 17, z = top.at() % 17;
        PipePart stub = new PipePart(kit == null ? 0 : kit.tier(), new int[]{x, 0, z, x, STUB, z});
        // (both checked before either is placed: the pipe and the valve's body over its end)
        String err = Laying.check(level, java.util.List.of(above, above),
            java.util.List.of(stub, new SafetyValvePart(x, STUB, z, 1, stub.bore(), mods.eln.sim.fluid.Water.P_ATM, mode)));
        if (err == null) err = PipeKitItem.place(level, above, stub, player);
        if (err != null) return err;
        err = level.getBlockEntity(above) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n
            ? n.addSafetyValveOnEnd(stub, x, STUB, z, mode) : "no node here";
        if (err != null) {
            Laying.takeBack(level, above, stub);
            return err;
        }
        if (kit != null) kit.take(STUB);
        return null;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        var list = new java.util.ArrayList<String>();
        if (mode == SafetyValvePart.BREATHER) {
            list.add(mods.eln.i18n.I18N.tr("Vents gas and admits air to hold atmospheric pressure (blocks liquid)."));
            mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Prevents a receiver tank from pressurising or pulling a vacuum."), onTankLine());
            return list;
        }
        if (mode == SafetyValvePart.PURGE) {
            list.add(mods.eln.i18n.I18N.tr("Vents gas above its set pressure (blocks liquid)."));
            mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Right-click with empty hand to set pressure."), onTankLine());
            return list;
        }
        list.add(mods.eln.i18n.I18N.tr("Relieves pipe pressure above its set point."));
        mods.eln.client.ItemTooltips.detail(list, mods.eln.i18n.I18N.tr("Right-click with empty hand to set pressure."), onTankLine());
        return list;
    }

    /** The tooltip's line on where it fits, the same for the three. */
    private static String onTankLine() {
        return mods.eln.i18n.I18N.tr("Fits on a pipe, or click a tank to fit it on a stub over its top port (takes half a block of pipe from a kit).");
    }
}
