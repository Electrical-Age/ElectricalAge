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

/** The pump (OilPumpDescriptor): electrical load + resistor; a slow process finds its source and moves fluid. */
public class OilPumpElement extends TransparentNodeElement {
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
    String status = "idle";
    int look;

    public OilPumpElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        this.desc = (OilPumpDescriptor) d;
        electricalLoadList.add(load);
        electricalComponentList.add(resistor);
        slowProcessList.add(new Pumping());
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(watchdog.set(load).setUNominal(desc.nominalU).set(new Failure(this).machine()));
    }

    class Pumping implements IProcess {
        @Override
        public void process(double time) {
            if (!(node.coordonate.world() instanceof ServerLevel level)) return;
            if (look-- <= 0) {
                look = 40;
                findSource(level);
            }
            boolean water = source < 0;
            boolean room = tank.getCapacity() - tank.getFluidAmount() >= 1
                && (tank.getFluidAmount() == 0 || tank.getFluid().getFluid().isSame(water ? Fluids.WATER : Oil.CRUDE.get()));
            if (source == 0 || !room) {
                resistor.setR(MnaConst.highImpedance);
                rate = 0;
                status = source == 0 ? "no well casing down to oil or water" : "tank full";
                return;
            }
            resistor.setR(desc.resistorR);
            double e = resistor.getP() * time;
            double mb = e / OilPumpDescriptor.energyPerMb(water, depth) + carry;
            int whole = (int) Math.min(Math.floor(mb), tank.getCapacity() - tank.getFluidAmount());
            carry = mb - Math.floor(mb);
            long got = 0;
            if (whole > 0) {
                if (water) got = whole;
                else {
                    Reservoir r = ReservoirData.get(level).byId(source);
                    if (r != null) got = Reservoirs.take(level, r, whole, false);
                    if (got == 0) status = "the lake is empty";
                }
                if (got > 0) tank.fill(new FluidStack(water ? Fluids.WATER : Oil.CRUDE.get(), (int) got), true);
            }
            rate = 0.9 * rate + 0.1 * (got / Math.max(time, 1e-6));
            if (got > 0) status = water ? "pumping water" : "pumping crude from lake #" + source;
        }
    }

    /** Follow the well casing down: a lake cell or water below its end (or beside its bottom block)? */
    void findSource(ServerLevel level) {
        BlockPos p = node.coordonate.pos().below();
        int d = 0;
        while (level.getBlockState(p).is(OilWorldgen.WELL_CASING.get()) && d < 400) {
            p = p.below();
            d++;
        }
        source = 0;
        depth = d;
        if (d == 0) return;
        // the block under the casing, or around its bottom piece
        BlockPos bottom = p.above();
        for (BlockPos q : new BlockPos[]{p, bottom.north(), bottom.south(), bottom.east(), bottom.west()}) {
            Reservoir r = ReservoirData.get(level).at(q);
            if (r != null) {
                source = r.id();
                return;
            }
            var fs = level.getFluidState(q);
            if (fs.is(Fluids.WATER) && fs.isSource()) {
                source = -1;
                return;
            }
        }
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
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        carry = nbt.getDoubleOr("carry", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Status"), status);
        info.put(I18N.tr("Flow"), String.format("%.1f mB/s", rate));
        info.put(I18N.tr("Tank"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Power consumption"), Utils.plotPower("", resistor.getP()));
        return info;
    }
}
