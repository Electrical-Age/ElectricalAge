package mods.eln.fluid;

import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * A device's own tank on an EA pipe (Baughn 2026-11-03: the oil devices on EA pipes, the fluids data): one small port
 * node on a flange, merged into the pipe that meets it (lumpable), and each step either OUT (what the device made goes
 * from its tank into the pipe: a pump with a shut-off head) or IN (what the pipe holds that the tank takes goes into
 * it). Works on any ResourceHandler of fluids, so the devices' own tank classes stay as they are. Not piped, it does
 * nothing (the device's outlet then pushes into the next block, FluidExport); it acts on the run the port is part of
 * (FluidWorld.nodeFor), never the stub alone.
 */
public final class PortBridge {
    public static final double STUB_VOLUME = 0.00002, PORT_K = 6e-11;

    public enum Mode { IN, OUT }

    final String key;
    public final Mode mode;
    /** mB per second it moves at most, and (OUT) the run pressure it stops at (Pa). */
    public final double ratePerS, headPa;
    /** The stub: its own small node (null until the device makes it). */
    public FluidNode stub;
    private double[] loaded;
    private double carry;

    /** OUT: what it pushes is live (crude with its natural gas: the oil pump's sealed line from the well). */
    public boolean live;

    public PortBridge(String key, Mode mode, double ratePerS, double headPa) {
        this.key = key;
        this.mode = mode;
        this.ratePerS = ratePerS;
        this.headPa = headPa;
    }

    /** Make the stub at height z (m) in a block, restoring what was saved. */
    public void make(double z, double ambientK) {
        stub = new FluidNode(STUB_VOLUME, z, 0.1, false);
        if (loaded != null) {
            FluidSaves.restore(stub, loaded);
            stub.update();
        } else stub.fill(0, Water.P_ATM, ambientK);
        stub.lumpable = true;   // (it merges into the pipe on its flange)
        stub.resistance = 1 / PORT_K;
    }

    /** Its port on face (MC 3D value) at spot at, of the block at pos (BlockPos.asLong). */
    public FluidDevice.FluidPort port(long pos, int face, int at) {
        return new FluidDevice.FluidPort(stub, pos, face, 0.5, PORT_K, at);
    }

    /**
     * The spot (FluidPort.at) on a device's front or back face of a point drawn at (y, z) px in its model's frame (front
     * XN, right z+: docs/cad.md), for the device facing front.
     */
    public static int endSpot(mods.eln.misc.Direction front, mods.eln.misc.Direction face, int y, int z) {
        mods.eln.misc.Direction r = front.right();
        boolean plus = r == mods.eln.misc.Direction.XP || r == mods.eln.misc.Direction.ZP;
        int w = plus ? z : 16 - z;
        int f = face.toEnumFacing().get3DDataValue();
        return f <= 3 ? w * 17 + y : y * 17 + w;   // (north/south: x then y; west/east: y then z)
    }

    /**
     * Whether a pipe meets it: its run reaches another node, or holds more than its stub. (A pipe from one device's
     * flange straight to another's is one run of both stubs and the pipe, with no edge out of it: the Steam Cracker's
     * butadiene into a Polymeriser never left, LabAssistant.)
     */
    public boolean piped() {
        return stub != null && (FluidWorld.runSize(stub) > 1 || !FluidWorld.edgesOf(stub).isEmpty());
    }

    /** Before a fluid step: move between the tank and the pipe (mB moved). T: what goes into the pipe is at T (K). */
    public int step(ResourceHandler<FluidResource> tank, double dt, double T) {
        if (!piped()) return 0;
        carry += ratePerS * dt;
        int budget = (int) Math.floor(carry);
        if (budget <= 0) return 0;
        carry -= budget;
        FluidNode run = FluidWorld.nodeFor(stub);
        return mode == Mode.OUT ? push(tank, run, budget, T) : pull(tank, run, budget);
    }

    private int push(ResourceHandler<FluidResource> tank, FluidNode run, int budget, double T) {
        if (run.P() > headPa) return 0;
        // Liquids until the run is full, and then a little more, as far as the liquid's give takes the run to the head
        // pressure (Water.K_LIQUID): a pump fills its pipe right up and pushes, so a pipe that climbs to a port high up
        // reaches it (Baughn 2026-11-09: 3 blocks up into a tank, nothing arrived; liquids stopped at 90 % of the run,
        // short of its top). In mB of any liquid: they go by volume.
        double roomM3 = Math.max(0, 0.98 * run.volume - run.liquidVolume()) + Math.max(0, headPa - run.P()) * run.volume / Water.K_LIQUID;
        // (the fluid core's liquid fills a run by squeezing its gas, not to a fixed 98 %: there the room is the gas the head
        // pressure would squeeze, else a run 98 % full stood at 1.2 bar, its top port above the liquid, and nothing climbed)
        if (FluidNode.coreOn())
            roomM3 = NodeFluidView.cushionRoom(run.volume - run.liquidVolume(), run.P(), run.pVapour(), headPa) + Math.max(0, headPa - run.P()) * run.volume / Water.K_LIQUID;
        int liquidRoom = (int) Math.floor(roomM3 * Water.RHO_L / EaUnits.KG_PER_MB_WATER);
        int moved = 0;
        for (int i = 0; i < tank.size() && moved < budget; i++) {
            FluidResource r = tank.getResource(i);
            if (r.isEmpty()) continue;
            var ph = FluidSpecies.of(r.getFluid());
            if (ph == null) continue;
            int want = Math.min(budget - moved, ph.vapour() ? Integer.MAX_VALUE : liquidRoom);
            if (want <= 0) continue;
            int got;
            try (var tx = Transaction.openRoot()) {
                got = tank.extract(i, r, want, tx);
                tx.commit();
            }
            if (got <= 0) continue;
            EaUnits.add(run, ph.id(), got * FluidSpecies.kgPerMb(ph.id(), ph.vapour()), T, ph.vapour(), live);
            moved += got;
            if (!ph.vapour()) liquidRoom -= got;
        }
        return moved;
    }

    private int pull(ResourceHandler<FluidResource> tank, FluidNode run, int budget) {
        int moved = 0;
        // (a device that takes a mixture alias, the column's crude: the run's layer of its parts, whole, as that alias, until
        // the column is a still; crude in a pipe is its five cuts now, Baughn 2026-11-10)
        var reg = mods.eln.sim.fluid.SpeciesRegistry.get();
        for (int id = 2; id < reg.size() && moved < budget; id++) {
            double[][] mix = reg.mixture(id, true);
            var f = mix == null ? null : FluidSpecies.fluid(id, false);
            if (f == null) continue;
            int part = (int) mix[0][0];
            double kg = EaUnits.layerLiquid(run, part);
            double per = FluidSpecies.kgPerMb(id, false);
            int want = Math.min(budget - moved, (int) Math.floor(kg / per));
            if (want <= 0) continue;
            int put;
            try (var tx = Transaction.openRoot()) {
                put = tank.insert(FluidResource.of(f), want, tx);
                tx.commit();
            }
            if (put <= 0) continue;
            EaUnits.drainLayer(run, part, put * per);
            moved += put;
        }
        for (int id = 0; id < run.speciesBound() && moved < budget; id++) {
            for (boolean vapour : new boolean[]{false, true}) {
                if (moved >= budget) break;
                double kg = vapour ? EaUnits.vapourAvailable(run, id) : run.liquid(id);
                var f = FluidSpecies.fluid(id, vapour);
                if (f == null || kg <= 0) continue;
                double per = FluidSpecies.kgPerMb(id, vapour);
                int have = (int) Math.floor(kg / per);
                int want = Math.min(budget - moved, have);
                if (want <= 0) continue;
                int put;
                try (var tx = Transaction.openRoot()) {
                    put = tank.insert(FluidResource.of(f), want, tx);
                    tx.commit();
                }
                if (put <= 0) continue;
                if (vapour) EaUnits.drainVapour(run, id, put * per);
                else EaUnits.drainLiquid(run, id, put * per);
                moved += put;
            }
        }
        return moved;
    }

    public void write(CompoundTag nbt) {
        if (stub != null) {
            FluidWorld.writeBackOf(stub);
            FluidSaves.put(nbt, key, stub);
        }
    }

    public void read(CompoundTag nbt) {
        loaded = FluidSaves.array(nbt, key);
    }
}
