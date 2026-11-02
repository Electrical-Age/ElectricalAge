package mods.eln.node.voxel;

import mods.eln.misc.Coordonate;
import mods.eln.misc.LRDU;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A pipe sensor: places the pipe sensor device of its kind (PipeTransmitterDescriptor) on the floor, wall or ceiling
 * clicked (Baughn 2026-11-02: fitted on a pipe it ended up in mid-air, where signal cables can't go). The player then
 * lays pipe from its port to the pipe to measure. It no longer fits a sensor on a pipe; those already fitted
 * (PipeSensorPart) keep working.
 */
public class PipeSensorItem extends Item implements PipeTool {
    public final int sensing;

    public PipeSensorItem(Properties p, int sensing) {
        super(p);
        this.sensing = sensing;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        var level = ctx.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        String err = place(level, ctx.getClickedPos(), ctx.getClickedFace(), ctx.getClickLocation(), sensing, player);
        if (err != null) {
            if (player != null) player.sendOverlayMessage(Component.literal(err));
            return InteractionResult.FAIL;
        }
        if (player != null) player.sendOverlayMessage(Component.literal("Now lay pipe from its port to the pipe to measure"));
        if (player == null || !player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** The device descriptor of a kind of sensor. */
    public static mods.eln.node.six.SixNodeDescriptor device(int sensing) {
        return mods.eln.Eln.sixNodeItem.getDescriptor(mods.eln.sixnode.pipetransmitter.PipeTransmitterDescriptor.FIRST_ID + sensing);
    }

    /**
     * Place the device of a kind for a click on face `clicked` of the block at `at` (hit in world coordinates): on the
     * face behind the click in the block in front of it, or in the voxel block clicked. Its centre goes on the pipe
     * routers' lattice (4, 8 or 12 voxels across the face), nearest the click where it fits, so a pipe kit can end at
     * its port; its signal pin faces the player if it fits that way. Null if placed, else why not.
     */
    public static String place(Level level, BlockPos at, net.minecraft.core.Direction clicked, Vec3 hit, int sensing, Player player) {
        mods.eln.node.six.SixNodeDescriptor d = device(sensing);
        if (d == null) return "Can't place that";
        BlockPos pos = at.relative(clicked);
        mods.eln.misc.Direction face = mods.eln.misc.Direction.fromIntMinecraftSide(clicked.get3DDataValue()).getInverse();
        VoxelNode v = null;
        if (level.getBlockEntity(at) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n) {
            double hx = hit.x - at.getX(), hy = hit.y - at.getY(), hz = hit.z - at.getZ();
            int part = n.aimed(hx, hy, hz).part();
            if (part >= 0 && !(n.parts.get(part) instanceof CablePart))
                return "It goes on a floor, wall or ceiling: place it there and lay pipe to it";
            pos = at;
            face = n.placeFace(clicked, hx, hy, hz);
            v = n;
        } else if (level.getBlockEntity(pos) instanceof VoxelNodeEntity e && e.getNode() instanceof VoxelNode n) v = n;
        if (!mods.eln.node.six.SixNodeBlock.get().getIfOtherBlockIsSolid(level, pos, face)) return "It needs a solid floor, wall or ceiling behind it";
        boolean fresh = v == null;
        if (fresh) {
            var old = level.getBlockState(pos);
            if (!(old.isAir() || old.canBeReplaced())) return "Something is in the way there";
            v = new VoxelNode();
            v.onBlockPlacedBy(new Coordonate(pos, level), face, player, ItemStack.EMPTY);
        }
        String err = placeIn(v, d, face, new double[]{hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ()}, player);
        if (fresh) {
            if (err != null) v.onBreakBlock();
            else level.setBlock(pos, mods.eln.registry.ElnRegistry.VOXEL_NODE_BLOCK.get().stateFor(level, pos), 3);
        }
        return err;
    }

    /** Try the lattice centres on the face nearest the hit (block-local, 0..1) first, each front from the player's. */
    static String placeIn(VoxelNode v, mods.eln.node.six.SixNodeDescriptor d, mods.eln.misc.Direction face, double[] hit, Player player) {
        int n = CablePart.normalAxis(face.toEnumFacing().get3DDataValue());
        List<int[]> centres = new ArrayList<>();
        for (int a = 4; a <= 12; a += 4)
            for (int b = 4; b <= 12; b += 4) {
                int[] o = new int[3];
                int k = 0;
                for (int ax = 0; ax < 3; ax++) if (ax != n) o[ax] = (k++ == 0 ? a : b) - 8;
                centres.add(o);
            }
        centres.sort(Comparator.comparingDouble(o -> {
            double s = 0;
            for (int ax = 0; ax < 3; ax++) if (ax != n) s += Math.pow(hit[ax] * 16 - 8 - o[ax], 2);
            return s;
        }));
        LRDU first = player != null ? d.getFrontFromPlace(face, player) : LRDU.Up;
        ItemStack stack = new ItemStack(d.parentItem);
        String err = null;
        for (int[] o : centres) {
            LRDU f = first;
            for (int k = 0; k < 4; k++, f = f.getNextClockwise()) {
                String e = v.placeOffCentre(stack, face, o[0], o[1], o[2], f, player);
                if (e == null) return null;
                if (err == null) err = e;
            }
        }
        return err;
    }

    /** Its tooltip lines (client ItemTooltips). */
    public java.util.List<String> tooltip() {
        return java.util.List.of(sensing == PipeSensorPart.PRESSURE ? "Reads a pipe's pressure"
                : sensing == PipeSensorPart.TEMPERATURE ? "Reads a pipe's temperature" : "Reads how wet a pipe's steam is",
            "Goes on a floor, wall or ceiling: lay pipe from its port to the pipe to measure",
            "0-50 V out on its signal pin", "Right-click it with an empty hand to step its full scale");
    }
}
