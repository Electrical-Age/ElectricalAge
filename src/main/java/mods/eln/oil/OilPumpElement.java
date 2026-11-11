package mods.eln.oil;

import mods.eln.fluid.ElementFluidHandler;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The pump (OilPumpDescriptor): electrical load + resistor; a slow process finds its source and moves fluid into its
 * tank, and its discharge line on its right pushes the tank into whatever takes it there (Baughn 2026-11-03: the oil
 * devices' drawn outlets did nothing).
 */
public class OilPumpElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    final OilPumpDescriptor desc;
    final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor resistor = new Resistor(load, null);
    public final ElementFluidHandler tank = new ElementFluidHandler(OilPumpDescriptor.TANK_MB);
    final VoltageStateWatchDog watchdog = new VoltageStateWatchDog();

    /** Source found at the last look: a lake id (> 0), water (-1) or nothing (0); depth in blocks. */
    int source;
    int depth;
    double carry;
    double rate;   // mB/s, for WAILA
    /** Jade: the oil lake's state (OilRecovery.describe), or null. */
    String lake;
    String status = "idle";
    int look;

    public OilPumpElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        this.desc = (OilPumpDescriptor) d;
        electricalLoadList.add(load);
        electricalComponentList.add(resistor);
        slowProcessList.add(new Pumping());
        slowProcessList.add(time -> {
            // (an EA pipe on its flange takes it, PortBridge; else whatever takes fluid on that face)
            if (!dischargePort.piped() && node.coordonate.world() instanceof ServerLevel level)
                exportedForTests += mods.eln.fluid.FluidExport.push(tank, level, node.coordonate.pos(), discharge().toEnumFacing(), (int) Math.ceil(DISCHARGE_MB_PER_S * time));
        });
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(watchdog.set(load).setUNominal(desc.nominalU).set(new Failure(this).machine()));
    }

    /** How fast its discharge pushes out what it pumped (mB/s): far more than it pumps. */
    static final double DISCHARGE_MB_PER_S = 200;

    /** Its discharge on an EA pipe: up to DISCHARGE_MB_PER_S, against up to OilPumpDescriptor.DISCHARGE_HEAD_PA. */
    final mods.eln.fluid.PortBridge dischargePort = new mods.eln.fluid.PortBridge("discharge", mods.eln.fluid.PortBridge.Mode.OUT,
        DISCHARGE_MB_PER_S, OilPumpDescriptor.DISCHARGE_HEAD_PA);

    {
        // (the well's crude is live, its natural gas with it, Baughn 2026-11-10: a purge valve on the line lets it off)
        dischargePort.live = true;
    }

    long exportedForTests, bridgedForTests;

    static double crudeMb(mods.eln.sim.fluid.FluidNode n) {
        int id = mods.eln.sim.fluid.SpeciesRegistry.get().id("eln:crude_oil");
        return n.liquid(id) / mods.eln.fluid.FluidSpecies.kgPerMb(id, false);
    }

    static String edgesForTests(mods.eln.sim.fluid.FluidNode run) {
        StringBuilder b = new StringBuilder();
        for (var e : mods.eln.fluid.FluidWorld.edgesOf(run)) {
            var o = e.a == run ? e.b : e.a;
            b.append(String.format("; edge to node V %.6f z %.2f h %.2f crude %.1f mB P %.0f", o.volume, o.z, o.height, crudeMb(o), o.P()));
        }
        return b.toString();
    }

    /** GameTests: the discharge run's state (volume m3, liquid mB, pressure Pa, level, run size), or "unpiped". */
    public String dischargeRunForTests() {
        if (dischargePort.stub == null || !dischargePort.piped()) return "unpiped; exported " + exportedForTests + ", bridged " + bridgedForTests;
        var run = mods.eln.fluid.FluidWorld.nodeFor(dischargePort.stub);
        return String.format("run %.6f m3 z %.2f h %.2f, crude %.1f mB, P %.0f Pa, level %.3f, %d pieces; exported %d, bridged %d", run.volume, run.z, run.height,
            crudeMb(run), run.P(), run.level(), mods.eln.fluid.FluidWorld.runSize(dischargePort.stub), exportedForTests, bridgedForTests)
            + edgesForTests(run);
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    /** The flange at the end of its discharge line: the right face, 6 px up (mods.eln.cad.models.OilPump). */
    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (dischargePort.stub == null) return java.util.List.of();
        int face = discharge().toEnumFacing().get3DDataValue();
        return java.util.List.of(dischargePort.port(node.coordonate.pos().asLong(), face, TankElement.sideSpot(face, OilPumpDescriptor.DISCHARGE_Y)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        bridgedForTests += dischargePort.step(tank, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (dischargePort.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    /** Its discharge: the right side, where the model's discharge line ends in a flange (mods.eln.cad.models.OilPump). */
    public Direction discharge() {
        return front.right();
    }

    class Pumping implements IProcess {
        @Override
        public void process(double time) {
            if (!(node.coordonate.world() instanceof ServerLevel level)) return;
            if (look-- <= 0) {
                look = 40;
                findSource(level);
            }
            // (a water reservoir, a Watershed lake, is pumped like oil: from its record, Baughn: no infinite water)
            mods.eln.reservoir.Reservoir res = source > 0 ? mods.eln.reservoir.ReservoirData.get(level).byId(source) : null;
            boolean water = source < 0 || res != null && res.kind().equals(mods.eln.reservoir.ReservoirWater.KIND);
            boolean room = tank.getCapacity() - tank.getFluidAmount() >= 1
                && (tank.getFluidAmount() == 0 || tank.getFluid().getFluid().isSame(water ? Fluids.WATER : Oil.CRUDE.get()));
            if (source == 0 || !room) {
                resistor.setR(MnaConst.highImpedance);
                rate = 0;
                status = source == 0 ? "no well casing down to oil or water" : "tank full";
                return;
            }
            // an oil lake in its seepage tail gives its pumps only a trickle between them: a pump runs slowly, drawing a share
            OilRecovery rec = OilRecovery.recovers(level, res) ? OilRecovery.get(level) : null;
            if (rec != null) rec.seep(level, res);   // (the seepage goes into the lake first; Baughn 2026-11-08)
            // (seeping, an empty lake: run slow, as only the seepage comes in)
            boolean seeping = rec != null && rec.regime(res) == OilRecovery.Regime.SEEPAGE && res.amount() < 1000;
            double seepW = OilRecovery.seepRate() * OilPumpDescriptor.energyPerMb(false, depth);
            resistor.setR(seeping && seepW < desc.nominalP ? desc.resistorR * desc.nominalP / Math.max(1, seepW) : desc.resistorR);
            double e = resistor.getP() * time;
            double mb = e / OilPumpDescriptor.energyPerMb(water, depth) + carry;
            int whole = (int) Math.min(Math.floor(mb), tank.getCapacity() - tank.getFluidAmount());
            carry = mb - Math.floor(mb);
            long got = 0;
            if (whole > 0) {
                if (source < 0) got = whole;
                else if (rec != null) {
                    got = rec.take(level, res, whole)[0];
                } else {
                    if (res != null) got = mods.eln.reservoir.Reservoirs.take(level, res, whole, false);
                    if (got == 0) status = "the lake is empty";
                }
                if (got > 0) tank.fill(new FluidStack(water ? Fluids.WATER : Oil.CRUDE.get(), (int) got), true);
            }
            rate = 0.9 * rate + 0.1 * (got / Math.max(time, 1e-6));
            if (got > 0) status = source < 0 ? "pumping water" : water ? "pumping water from lake #" + source : "pumping crude";
            lake = OilRecovery.oil(res) ? OilRecovery.describe(level, res) : null;
        }
    }

    /** Follow the well casing down: a lake cell or water below its end (or beside its bottom block)? */
    void findSource(ServerLevel level) {
        int[] w = well(level, node.coordonate.pos());
        source = w[0];
        depth = w[1];
    }

    /**
     * The well under a device at `top`: {source, depth}: following the casing down from the block under it, a lake's id
     * (> 0) or water (-1) under its end or beside its bottom block, else 0; depth in blocks of casing.
     */
    static int[] well(ServerLevel level, BlockPos top) {
        BlockPos p = top.below();
        int d = 0;
        while (level.getBlockState(p).is(OilWorldgen.WELL_CASING.get()) && d < 400) {
            p = p.below();
            d++;
        }
        if (d == 0) return new int[]{0, 0};
        // the block under the casing, or around its bottom piece
        BlockPos bottom = p.above();
        for (BlockPos q : new BlockPos[]{p, bottom.north(), bottom.south(), bottom.east(), bottom.west()}) {
            mods.eln.reservoir.Reservoir r = mods.eln.reservoir.ReservoirData.get(level).at(level, q);
            if (r != null) return new int[]{r.id(), d};
            var fs = level.getFluidState(q);
            if (fs.is(Fluids.WATER) && fs.isSource()) return new int[]{-1, d};
        }
        return new int[]{0, d};
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return load;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP) return 0;
        return NodeBase.maskElectricalPower;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        resistor.setR(desc.resistorR);
        dischargePort.make(node.coordonate.y + OilPumpDescriptor.DISCHARGE_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "tank");
        nbt.putDouble("carry", carry);
        dischargePort.write(nbt);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        carry = nbt.getDoubleOr("carry", 0);
        dischargePort.read(nbt);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Status"), status);
        if (lake != null) info.put(I18N.tr("Lake"), lake);
        info.put(I18N.tr("Flow"), String.format("%.1f mB/s", rate));
        info.put(I18N.tr("Tank"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Discharge"), dischargePort.piped() ? I18N.tr("the flange on its right, into the pipe") : I18N.tr("the flange on its right"));
        info.put(I18N.tr("Power consumption"), Utils.plotPower("", resistor.getP()));
        return info;
    }
}
