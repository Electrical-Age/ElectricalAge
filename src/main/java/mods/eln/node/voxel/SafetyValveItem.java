package mods.eln.node.voxel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A safety valve, purge valve or breather (SafetyValvePart's modes): right-click a straight stretch of pipe; its outlet
 * points out of the pipe's most open side (up if it can). Or the upper end of a pipe that ends inside its block: it
 * stands on the end. Or a tank's top: on a stub over its top port (onTank).
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
            instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof mods.eln.oil.TankElement tank)
            // (only its top: a click meant for a thin pipe running past a tank that lands on the tank builds nothing)
            err = ctx.getClickedFace() == net.minecraft.core.Direction.UP ? onTank(level, pos, tank, player, mode)
                : mods.eln.i18n.I18N.tr("click the tank's top to fit it on a stub");
        else err = mods.eln.i18n.I18N.tr("must fit on a pipe, or on a tank's top");
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

    /** GameTests: the valve refuses once the stub is placed (the take-back, which no arrangement of parts reaches:
     *  stub and valve are checked together first). */
    public static boolean failValveForTests;

    /**
     * The valve clicked on a tank's top (Baughn 2026-11-28, after Lashtear 2026-11-28: a breather fitted only on a
     * pipe, and the stub to hold one over a tank could not be laid without scaffolding, there being no face over a
     * tank to click): a pipe STUB long is stood on the tank's free top port in the block above, and the valve on its
     * end (VoxelNode.addSafetyValveOnEnd). The pipe is the tank's own kind (TankDescriptor.pipeTier: iron pipe on an
     * Alloy Tank at its pressure would burst), in creative too, taken from a kit of it the player carries (in the
     * inventory or a Toolbag there; nothing is free, but in creative), and comes back when the stub is broken, as any
     * pipe. A safety valve is set no higher than the tank is rated. All or nothing. Null if done, else why not: the
     * port already coupled, a tank of a box's lower rows (no top port), what is in the way above, the tank over the
     * pipe's working pressure now, no such pipe carried.
     */
    static String onTank(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, mods.eln.oil.TankElement tank,
                         net.minecraft.world.entity.player.Player player, int mode) {
        mods.eln.sim.fluid.FluidDevice.FluidPort top = null;
        for (var p : mods.eln.fluid.FluidPlacement.portsAt(level, pos)) if (p.face() == 1) top = p;
        if (top == null) return mods.eln.i18n.I18N.tr("this tank has no top port (click one in the top row)");
        if (!mods.eln.fluid.FluidPlacement.partners(level, top).isEmpty()) return portTaken();
        var above = pos.above();
        if (level.isOutsideBuildHeight(above)) return mods.eln.i18n.I18N.tr("no room over the tank");
        var state = level.getBlockState(above);
        VoxelNode old = level.getBlockEntity(above) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n : null;
        if (old == null && !Mimic.allowed(state) && !(state.isAir() || state.canBeReplaced()))
            return mods.eln.i18n.I18N.tr("the %1$ over the tank is in the way", state.getBlock().getName().getString());
        if (old != null && old.safetyValveTaken() != null) return old.safetyValveTaken();
        int tier = tank.descriptor().pipeTier();
        int x = top.at() / 17, z = top.at() % 17;
        PipePart stub = new PipePart(tier, new int[]{x, 0, z, x, STUB, z});
        var vessel = tank.vessel;
        if (vessel != null && vessel.P() > stub.workingPa(vessel.T()))
            return mods.eln.i18n.I18N.tr("the tank is at %1$ bar, over the %2$ pipe's %3$ bar", String.format("%.0f", vessel.P() / 1e5), PipePart.TIER_NAME[tier],
                String.format("%.0f", stub.workingPa(vessel.T()) / 1e5));
        boolean free = player != null && player.isCreative();
        KitSupply kit = free ? null : KitSupply.pipeCarried(player, tier, STUB);
        if (!free && kit == null) return mods.eln.i18n.I18N.tr("needs half a block of %1$ pipe from a pipe kit in your inventory", PipePart.TIER_NAME[tier]);
        double setPa = mode == SafetyValvePart.SAFETY ? Math.min(stub.reliefPa(), tank.descriptor().workingPa()) : Double.NaN;
        // (both checked before either is placed: the pipe and the valve's body over its end)
        String err = Laying.check(level, java.util.List.of(above, above),
            java.util.List.of(stub, new SafetyValvePart(x, STUB, z, 1, stub.bore(), mods.eln.sim.fluid.Water.P_ATM, mode, true)));
        if (err == null) err = PipeKitItem.place(level, above, stub, player);
        if (err != null) return err;
        err = failValveForTests ? "refused for a test"
            : level.getBlockEntity(above) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n ? n.addSafetyValveOnEnd(stub, x, STUB, z, mode, setPa) : "no node here";
        if (err != null) {
            Laying.takeBack(level, above, stub);
            return err;
        }
        if (kit != null) kit.take(STUB);
        return null;
    }

    /** PipePlacer.PORT_TAKEN's words as they read after "Can't fit … valve: " (one wording for a port that has its pipe). */
    static String portTaken() {
        String s = mods.eln.i18n.I18N.tr(PipePlacer.PORT_TAKEN);
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
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
        return mods.eln.i18n.I18N.tr("Fits on a pipe, or click a tank's top to fit it on a stub over its top port (takes half a block of pipe from a kit).");
    }
}
