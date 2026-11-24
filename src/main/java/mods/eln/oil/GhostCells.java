package mods.eln.oil;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.FluidDevice.FluidPort;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The other cells of a multiblock whose node is its front bottom left block: the seven of a 2x2x2 (the Large Boiler,
 * the Still Pot), the one above the Condenser Coil.
 * Local frame as EA's (front = XN): x 0 front, 1 back; y 0 low, 1 high; z 0 left, 1 right (front.right()). Each cell is
 * a FaceNode, a ghost node (never saved) made in the device's initialize and dropped with it, as the Auto Miner's power
 * nodes are: its thermal faces hand out the device's shell, and the pipe tool finds the device's ports on it. Its ghost
 * group doesn't turn, so neither does the device.
 */
public final class GhostCells {
    /** The device behind the cells: which of its faces are thermal, its shell, its ports by block. */
    public interface Host extends mods.eln.fluid.GhostPortHost {
        boolean thermalFace(int x, int y, int z, Direction side);

        ThermalLoad shell();
    }

    private final List<FaceNode> faces = new ArrayList<>();

    /** The world coordinate of local block (x, y, z) of e. */
    public static Coordonate at(TransparentNodeElement e, int x, int y, int z) {
        Coordonate c = new Coordonate(x, y, z, e.node.coordonate.dimention);
        c.applyTransformation(e.front, e.node.coordonate);
        return c;
    }

    /** The heat faces both devices have: a lower block's back or flank (local block x, y, z; side in the world). */
    public static boolean lowerBackOrFlank(Direction front, int x, int y, int z, Direction side) {
        if (y != 0) return false;
        return (x == 1 && side == front.getInverse()) || (z == 0 && side == front.left()) || (z == 1 && side == front.right());
    }

    /** (Re)make the seven cells of e, a 2x2x2 (its initialize). */
    public <T extends TransparentNodeElement & Host> void make(T e) {
        List<int[]> cells = new ArrayList<>();
        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 1; y++)
                for (int z = 0; z <= 1; z++) if (x != 0 || y != 0 || z != 0) cells.add(new int[]{x, y, z});
        make(e, cells);
    }

    /** (Re)make these cells of e (local x, y, z each; the Condenser Coil's one above its node). */
    public <T extends TransparentNodeElement & Host> void make(T e, List<int[]> cells) {
        drop();
        for (int[] c : cells) {
            FaceNode n = new FaceNode(e, c[0], c[1], c[2]);
            n.onBlockPlacedBy(at(e, c[0], c[1], c[2]), e.front, null, null);
            faces.add(n);
        }
    }

    /** Drop the cells' nodes (the device is broken, or remakes them). */
    public void drop() {
        for (FaceNode n : faces) n.onBreakBlock();
        faces.clear();
    }

    /** The faces of one ghost block: its thermal faces hand out the device's shell; nothing else connects. */
    public static final class FaceNode extends GhostNode implements mods.eln.fluid.GhostPortHost {
        final Host host;
        final int x, y, z;

        FaceNode(Host host, int x, int y, int z) {
            this.host = host;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public List<FluidPort> portsAt(long pos) {
            return host.portsAt(pos);
        }

        @Override
        public void initializeFromThat(Direction front, LivingEntity entityLiving, ItemStack itemStack) {
            connect();
        }

        @Override
        public void initializeFromNBT() {
        }

        @Override
        public int getSideConnectionMask(Direction side, LRDU lrdu) {
            return lrdu == LRDU.Down && host.thermalFace(x, y, z, side) ? NodeBase.maskThermal : 0;
        }

        @Override
        public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
            return host.thermalFace(x, y, z, side) ? host.shell() : null;
        }

        @Override
        public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
            return null;
        }
    }
}
