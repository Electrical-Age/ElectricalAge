package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TankDescriptor: one fluid-core vessel and its ports; it bursts over its rating. Blocks of a tier filling a box are one
 * vessel (TankGroup): `own` is this block's share, `vessel` the node in use (its own, or the box's).
 */
public class TankElement extends TransparentNodeElement implements FluidDevice {
    final TankDescriptor desc;
    /** Its ports' conductance (m^3): made for a large alloy pipe (they take any; the pipe's own end limits a smaller one). */
    public static final double PORT_K = mods.eln.node.voxel.PipePart.LARGE_END_K;
    /** Heat it loses to the air (W/K): a bare steel vessel's. */
    static final double LOSS_W_PER_K = 1.0;
    /** The low (liquid) and high (vapour) side ports' heights (voxels, on the pipe lattice). */
    public static final int LOW = 4, HIGH = 12;
    public FluidNode vessel, own;
    /** The box it is part of, or null alone. */
    public TankGroup group;
    private double[] loaded;
    private List<FluidPort> ports = List.of();
    /** The water's share of its volume, last published (the sight glass). */
    float shownFill = -1;
    /**
     * In a box: which of its ports have something coupled to them (Baughn 2026-10-29: a merged box draws a flange only
     * where a pipe meets it): bit 2 * face + (0 low, 1 high) for the side faces (Direction 3D values 2..5), bit 12 the
     * top. Published when it changes.
     */
    public int coupled;

    public TankElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (TankDescriptor) d;
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(time -> {
            if (vessel == null) return;
            if (vessel.P() > desc.burstPa)
                new mods.eln.sim.process.destruct.Failure(TankElement.this).pressureVessel(() -> vessel.energy - vessel.mWater * Water.uLiquid(Water.TB))
                    .fail(mods.eln.sim.process.destruct.FailureCause.OVERPRESSURE, vessel.P());
            if (Math.abs(fill() - shownFill) > 0.02) needPublish();
            int c = group == null ? 0 : coupledPorts();
            if (c != coupled) {
                coupled = c;
                needPublish();
            }
        });
    }

    /** The water's share of its volume. */
    public double fill() {
        return vessel == null ? 0 : Math.min(1, vessel.mLiquid() / Water.RHO_L / vessel.volume);
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    /** A side face's port spot at height y (voxels): the face's other axes in x, y, z order (FluidPort.at). */
    public static int sideSpot(int face, int y) {
        return face <= 3 ? 8 * 17 + y : y * 17 + 8;   // (north/south: x then y; west/east: y then z)
    }

    void makePorts() {
        long at = node.coordonate.pos().asLong();
        List<FluidPort> out = new ArrayList<>();
        if (group == null) {
            out.add(new FluidPort(vessel, at, 1, 1.0, PORT_K));   // top: vapour
            out.add(new FluidPort(vessel, at, 0, 0.0, PORT_K));   // bottom: liquid
            for (Direction d : new Direction[]{front.left(), front.right(), front.back()}) {
                int f = d.toEnumFacing().get3DDataValue();
                out.add(new FluidPort(vessel, at, f, 0.0, PORT_K, sideSpot(f, LOW)));    // low: liquid
                out.add(new FluidPort(vessel, at, f, 1.0, PORT_K, sideSpot(f, HIGH)));   // high: vapour
            }
        } else {
            // in a box: the ports on its outer faces, each drawing at its height in the whole vessel
            var c = node.coordonate;
            int j = c.y - group.y0, h = group.sy;
            if (j == h - 1) out.add(new FluidPort(vessel, at, 1, 1.0, PORT_K));
            if (j == 0) out.add(new FluidPort(vessel, at, 0, 0.0, PORT_K));
            for (Direction d : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                if (TankGroup.neighbour(this, d, null) != null && group.members.contains(TankGroup.neighbour(this, d, null))) continue;
                int f = d.toEnumFacing().get3DDataValue();
                out.add(new FluidPort(vessel, at, f, (j * 16 + LOW) / (16.0 * h), PORT_K, sideSpot(f, LOW)));
                out.add(new FluidPort(vessel, at, f, (j * 16 + HIGH) / (16.0 * h), PORT_K, sideSpot(f, HIGH)));
            }
        }
        ports = out;
    }

    /** Which of its ports meet a port of whatever is beside it (a pipe's end, a device's flange): see `coupled`. */
    int coupledPorts() {
        int mask = 0;
        var level = node.coordonate.world();
        for (FluidPort p : ports) {
            var f = net.minecraft.core.Direction.from3DDataValue(p.face());
            var there = net.minecraft.core.BlockPos.of(p.pos()).relative(f);
            var other = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(there, level));
            FluidDevice d = other instanceof FluidDevice fd ? fd
                : other instanceof mods.eln.node.transparent.TransparentNode t && t.element instanceof FluidDevice fe ? fe : null;
            if (d == null) continue;
            for (FluidPort q : d.fluidPorts())
                if (q.pos() == there.asLong() && q.face() == f.getOpposite().get3DDataValue() && q.at() == p.at()) {
                    if (p.face() == 1) mask |= 1 << 12;
                    else if (p.face() >= 2) mask |= 1 << (2 * p.face() + (p.at() == sideSpot(p.face(), HIGH) ? 1 : 0));
                }
        }
        return mask;
    }

    /** The tanks of its tier on its six sides (for regrouping). */
    List<TankElement> neighbours() {
        List<TankElement> out = new ArrayList<>();
        for (Direction d : TankGroup.DIRS) {
            TankElement o = TankGroup.neighbour(this, d, null);
            if (o != null) out.add(o);
        }
        return out;
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (vessel != null) {
            makePorts();
            FluidWorld.add(this);
            // (with its neighbours: a box may form)
            List<TankElement> seeds = neighbours();
            seeds.add(this);
            TankGroup.regroup(seeds, null);
        }
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
        if (group != null) {
            // (the box without it: its share leaves with it, the rest re-form)
            TankGroup g = group;
            g.writeBack();
            List<TankElement> rest = new ArrayList<>(g.members);
            rest.remove(this);
            group = null;
            vessel = own;
            TankGroup.regroup(rest, this);
        }
    }

    @Override
    public void initialize() {
        own = new FluidNode(desc.volume, node.coordonate.y, 1.0, false);
        own.lossWPerK = LOSS_W_PER_K;
        if (loaded != null) {
            own.mWater = loaded[0];
            own.mAir = loaded[1];
            own.energy = loaded[2];
            own.update();
        } else own.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        vessel = own;
        makePorts();
        connect();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return vessel == null ? "" : mods.eln.misc.Utils.plotCelsius("T:", vessel.T() - Water.T0);
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            shownFill = (float) fill();
            stream.writeFloat(shownFill);
            // the box: whether it draws it (its lowest corner), and its size (a lone tank: 1 1 1)
            boolean draws = group == null || group.master() == this;
            stream.writeBoolean(draws);
            stream.writeByte(group == null ? 1 : group.sx);
            stream.writeByte(group == null ? 1 : group.sy);
            stream.writeByte(group == null ? 1 : group.sz);
            stream.writeShort(group == null ? 0 : coupled);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (group != null) group.writeBack();   // (its share of the box)
        if (own != null) {
            // (to float precision: a box re-formed on load from these shares and written back differs in the last bits)
            nbt.putDouble("vesselW", (float) own.mWater);
            nbt.putDouble("vesselA", (float) own.mAir);
            nbt.putDouble("vesselU", (float) own.energy);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (nbt.contains("vesselW"))
            loaded = new double[]{nbt.getDoubleOr("vesselW", 0), nbt.getDoubleOr("vesselA", 0), nbt.getDoubleOr("vesselU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        if (vessel == null) return info;
        info.put(I18N.tr("Pressure"), String.format("%.2f bar of %s, %.0f °C", vessel.P() / 1e5, EaUnits.contents(vessel), vessel.T() - Water.T0));
        info.put(I18N.tr("Water"), String.format("%.0f mB (%.0f%% full)", vessel.mLiquid() / EaUnits.KG_PER_MB_WATER, fill() * 100));
        info.put(I18N.tr("Vessel"), group == null ? I18N.tr("one block") : String.format("%d x %d x %d blocks, %.0f buckets", group.sx, group.sy, group.sz,
            vessel.volume * Water.RHO_L / EaUnits.KG_PER_MB_WATER / 1000));
        if (vessel.mVapour() > 1e-7) info.put(I18N.tr("Steam"), EaUnits.steamGrams(vessel.mVapour() / EaUnits.KG_PER_MB_STEAM));
        if (vessel.P() > desc.workingPa) info.put(I18N.tr("Warning"), String.format("over its %.0f bar working pressure", desc.workingPa / 1e5));
        return info;
    }
}
