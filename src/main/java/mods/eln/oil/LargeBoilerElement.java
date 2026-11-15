package mods.eln.oil;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.GhostNode;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The 2x2x2 Large Boiler (todo #472, Baughn 2026-11-15): a Boiler (one vessel, one shell, BoilerDescriptor.large()'s
 * ratings) whose node is the front-bottom-left block and whose other seven blocks are ghosts
 * (BoilerDescriptor.twoByTwo). Local frame as EA's (front = XN): x 0 front, 1 back; y 0 low, 1 high; z 0 left, 1
 * right (front.right()). Faces:
 * <ul>
 * <li>thermal (the one shell): the whole back (x 1, back) and both flanks (z 0 left, z 1 right): 12 faces;</li>
 * <li>steam out: the top of the two rear top blocks (1,1,0) and (1,1,1), drawn at the top of the vessel;</li>
 * <li>feedwater in: the front of (0,0,1), through the Boiler's check valve and float valve;</li>
 * <li>the level signal (the Boiler's gauge-glass signal): the front of the origin (0,0,0);</li>
 * <li>the rest plain (the front's upper blocks carry the gauge glass and dial in the model; underneath, the top front).</li>
 * </ul>
 * The faces on ghost blocks are FaceNodes, ghost nodes (never saved) made in initialize and dropped with the boiler,
 * as the Auto Miner's power nodes are. Its ghost group doesn't turn, so neither does it (no wrenchTurn).
 */
public class LargeBoilerElement extends BoilerElement {
    private final List<FaceNode> faces = new ArrayList<>();

    public LargeBoilerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
    }

    /** The world coordinate of local block (x, y, z). */
    Coordonate at(int x, int y, int z) {
        Coordonate c = new Coordonate(x, y, z, node.coordonate.dimention);
        c.applyTransformation(front, node.coordonate);
        return c;
    }

    /** Whether local block (x, y, z)'s face towards side (world) is a thermal face. */
    boolean thermalFace(int x, int y, int z, Direction side) {
        return (x == 1 && side == front.getInverse()) || (z == 0 && side == front.left()) || (z == 1 && side == front.right());
    }

    @Override
    protected FluidPort feedPort() {
        return new FluidPort(feed, at(0, 0, 1).pos().asLong(), front.toEnumFacing().get3DDataValue(), 0.5, PORT_K);
    }

    @Override
    protected List<FluidPort> steamPorts() {
        return List.of(new FluidPort(vessel, at(1, 1, 0).pos().asLong(), 1, 1.0, PORT_K),
            new FluidPort(vessel, at(1, 1, 1).pos().asLong(), 1, 1.0, PORT_K));
    }

    /** The ports on one of its blocks (for the pipe tool, which asks the node at a block for its ports). */
    public List<FluidPort> portsAt(long pos) {
        List<FluidPort> out = new ArrayList<>();
        for (FluidPort p : fluidPorts()) if (p.pos() == pos) out.add(p);
        return out;
    }

    @Override
    public void initialize() {
        super.initialize();
        for (FaceNode n : faces) n.onBreakBlock();
        faces.clear();
        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 1; y++)
                for (int z = 0; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    FaceNode n = new FaceNode(this, x, y, z);
                    n.onBlockPlacedBy(at(x, y, z), front, null, null);
                    faces.add(n);
                }
    }

    @Override
    public void onBreakElement() {
        super.onBreakElement();
        for (FaceNode n : faces) n.onBreakBlock();
        faces.clear();
    }

    // the origin block's own faces: the signal on its front, heat on its left flank

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return side == front && lrdu == LRDU.Down ? phaseOut : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalFace(0, 0, 0, side) ? thermal : null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        if (side == front) return NodeBase.maskElectricalOutputGate;
        return thermalFace(0, 0, 0, side) ? NodeBase.maskThermal : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return side == front ? mods.eln.misc.Utils.plotSignal(phaseOut.getU(), phaseOut.getCurrent()) : "";
    }

    /** The faces of one ghost block: its thermal faces hand out the boiler's shell; nothing else connects. */
    public static final class FaceNode extends GhostNode {
        final LargeBoilerElement boiler;
        final int x, y, z;

        FaceNode(LargeBoilerElement boiler, int x, int y, int z) {
            this.boiler = boiler;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public List<FluidPort> fluidPorts() {
            return boiler.portsAt(coordonate.pos().asLong());
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
            return lrdu == LRDU.Down && boiler.thermalFace(x, y, z, side) ? NodeBase.maskThermal : 0;
        }

        @Override
        public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
            return boiler.thermalFace(x, y, z, side) ? boiler.thermal : null;
        }

        @Override
        public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
            return null;
        }
    }
}
