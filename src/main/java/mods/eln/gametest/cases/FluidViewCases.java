package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.fluid.FluidWorld;
import mods.eln.fluid.NodeFluidView;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidEdge;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * One source of truth for every fluid device (Baughn 2026-11-04: an overfilled boiler showed "8000 / 8000" because its
 * capability kept the 1.12 tanks' cap while its vessel filled past it; "make it structurally impossible"). Every
 * transparent device that is a FluidDevice is placed by enumeration, so a new one is covered without being listed:
 * <ul>
 * <li>its fluid capability is a NodeFluidView over one of its own nodes, or none, or an int tank on the explicit list
 *     below (devices whose store is that tank, filled from pipes through a PortBridge that respects it);</li>
 * <li>a NodeFluidView device is flooded through every port from water at 8 bar; its view then shows the node's real
 *     contents (no cap), and the capability takes no more than the node's real free volume, over all slots and faces
 *     at once (one budget: the tanks' and adapters' views had offered each slot the same room).</li>
 * </ul>
 * Gauges may clamp (a gauge glass reads full); quantities may not. The pipe adapter (a voxel part) and a tank holding
 * layers have their own cases.
 */
public final class FluidViewCases {
    private FluidViewCases() {
    }

    /**
     * Devices whose capability is an int tank that is their real store (no node behind it to disagree with): pipes fill
     * it through a PortBridge, which inserts only what the tank takes.
     */
    static final Set<String> INT_TANK_DEVICES = Set.of("IndustrialResinCollectorElement", "CompressorPortsElement", 
        "FuelHeatFurnaceElement", "GasTurbineElement", "FlareStackElement", "ColumnBaseElement", "ColumnSegmentElement",
        "OilSeparatorElement", "OilPumpElement", "FuelGeneratorElement",
        // (its water is a number, capped at its tank; the well node feeds it through a float valve a bucket short of full)
        "CompactTurbineElement");

    public static void addTo(List<TestCase> cases) {
        Set<Class<?>> seen = new java.util.HashSet<>();   // (one per element class: its sizes share the code)
        for (Map.Entry<Integer, TransparentNodeDescriptor> e : new TreeMap<>(Eln.transparentNodeItem.all()).entrySet()) {
            TransparentNodeDescriptor d = e.getValue();
            if (d.ElementClass == null || !FluidDevice.class.isAssignableFrom(d.ElementClass) || !seen.add(d.ElementClass)) continue;
            cases.add(new Device(e.getKey(), d.ElementClass.getSimpleName()));
        }
        cases.add(new Layers());
        cases.add(new Adapter());
    }

    /** Water at 8 bar behind a check valve into each of a device's ports (a feed pump on every flange). */
    static final class Flooder implements FluidDevice {
        final Object level;
        final List<Link> links = new ArrayList<>();

        Flooder(FluidDevice target, double y) {
            level = target.fluidLevel();
            FluidNode src = new FluidNode(1, y - 1, 1, true).fill(900, 8e5, Water.T_AMBIENT);
            for (FluidPort p : target.fluidPorts()) links.add(new Link(src, 0, p.node(), p.draw(), p.k()).kind(FluidEdge.Kind.CHECK));
        }

        public Object fluidLevel() {
            return level;
        }

        public List<FluidPort> fluidPorts() {
            return List.of();
        }

        @Override
        public List<Link> fluidLinks() {
            return links;
        }
    }

    /** The node's liquid shown in a view's LIQUID slot (mB), worked out from the node itself. */
    static int realLiquidMb(FluidNode n) {
        int top = mods.eln.fluid.FluidSpecies.topLiquid(n), id = top < 0 ? SpeciesRegistry.WATER : top;
        double kg = id == SpeciesRegistry.WATER ? (n.T() < Water.TB ? n.mWater : n.mLiquid()) : n.liquid(id);
        return (int) Math.round(kg / mods.eln.fluid.FluidSpecies.kgPerMb(id, false));
    }

    /** The node's real free liquid volume, in mB of liquid. */
    static int freeMb(FluidNode n) {
        return (int) Math.floor(Math.max(0, n.volume - n.liquidVolume()) * Water.RHO_L / EaUnits.KG_PER_MB_WATER);
    }

    /** All the water every face's capability takes at once (mB), not committed. */
    static int takesAtOnce(TestContext ctx, BlockPos p) {
        Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        int took = 0;
        try (var tx = Transaction.openRoot()) {
            for (var f : net.minecraft.core.Direction.values()) {
                var cap = ctx.level().getCapability(Capabilities.Fluid.BLOCK, p, f);
                if (cap == null || !seen.add(cap)) continue;
                took += cap.insert(FluidResource.of(net.minecraft.world.level.material.Fluids.WATER), 1_000_000, tx);
            }
        }
        return took;
    }

    static final class Device implements TestCase {
        final int damage;
        final String kind;
        TransparentNodeElement e;
        Flooder flood;
        String err;

        Device(int damage, String kind) {
            this.damage = damage;
            this.kind = kind;
        }

        public String id() {
            return "fluid_view_" + kind.replace("Element", "").toLowerCase();
        }

        public int width() {
            return 5;
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(2, 1, 2);
            try {
                try {
                    // (a submersible pump stands in water)
                    if (kind.startsWith("Submersible")) ctx.level().setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 2);
                    e = Wp10bCases.placeFacing(ctx, damage, p, Direction.XN);
                } catch (IllegalStateException noSuchFront) {
                    e = ctx.placeTransparent(damage, p);
                }
            } catch (RuntimeException ex) {
                err = ex.toString();
                return;
            }
            if (e.getFluidHandler() instanceof NodeFluidView && e instanceof FluidDevice d) {
                flood = new Flooder(d, e.node.coordonate.y);
                FluidWorld.add(flood);
            }
        }

        public void measure(TestContext ctx) {
            if (flood != null) FluidWorld.remove(flood);
            ctx.check(kind + ": placed", e != null, "" + err);
            if (e == null) return;
            Object h = e.getFluidHandler();
            boolean ok = h == null || h instanceof NodeFluidView || INT_TANK_DEVICES.contains(kind);
            ctx.check(kind + ": its fluid capability is a view of its node (NodeFluidView), none, or a listed int-tank store", ok,
                h == null ? "none" : h.getClass().getName());
            if (!(h instanceof NodeFluidView v) || !(e instanceof FluidDevice d)) return;
            FluidNode n = v.node();
            boolean own = false;
            for (var port : d.fluidPorts()) own |= n != null && (FluidWorld.nodeFor(port.node()) == FluidWorld.nodeFor(n) || port.node() == n);
            ctx.check(kind + ": its view watches one of its own nodes", own, "" + n);
            if (n == null) return;
            int shown = v.amount(NodeFluidView.LIQUID), real = realLiquidMb(n);
            ctx.check(kind + ": its view shows the node's real liquid (no cap)", Math.abs(shown - real) <= 1, shown + " shown, " + real + " in the node");
            int free = freeMb(n), took = takesAtOnce(ctx, e.node.coordonate.pos());
            ctx.check(kind + ": its capability takes no more than the node's free volume, every slot and face at once", took <= free + 1,
                took + " mB taken, " + free + " mB free");
        }
    }

    /**
     * An iron tank three-quarters water with gasoline floating on it, nearly full: its capability (LIQUID shows the
     * gasoline, INTAKE takes anything) takes no more than the vessel's free volume (it had offered LIQUID the whole
     * vessel less the gasoline, and INTAKE the room again).
     */
    static final class Layers implements TestCase {
        mods.eln.oil.TankElement t;
        int took, free;

        public String id() {
            return "fluid_view_tank_layers";
        }

        public int width() {
            return 3;
        }

        public void build(TestContext ctx) {
            t = PlumbingCases.tank(ctx, PlumbingCases.IRON_TANK, ctx.at(1, 1, 1), Direction.XN);
        }

        public void measure(TestContext ctx) {
            if (t.vessel == null) {
                ctx.check("tank layers: placed", false, "");
                return;
            }
            double v = t.vessel.volume;
            t.vessel.fill(0.75 * v * Water.RHO_L, Water.P_ATM, Water.T_AMBIENT);
            int gas = SpeciesRegistry.get().id("eln:gasoline");
            EaUnits.add(t.vessel, gas, 0.2 * v * 740, Water.T_AMBIENT, false);
            t.showWater();
            free = freeMb(t.vessel);
            took = takesAtOnce(ctx, t.node.coordonate.pos());
            ctx.check("tank layers: the capability takes no more than the free volume", took <= free + 1, took + " mB taken, " + free + " mB free");
        }
    }

    /** A pipe adapter on a short run: other mods' pipes can fill the run to 90 % at most, every slot at once. */
    static final class Adapter implements TestCase {
        BlockPos at;
        int took;

        public String id() {
            return "fluid_view_pipe_adapter";
        }

        public int width() {
            return 4;
        }

        public void build(TestContext ctx) {
            at = ctx.at(2, 1, 1);
            ctx.level().setBlock(ctx.at(1, 1, 1), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            var item = new net.minecraft.world.item.ItemStack(mods.eln.registry.ElnRegistry.PIPE_ADAPTER.get());
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ctx.at(1, 1, 1)),
                net.minecraft.core.Direction.EAST, ctx.at(1, 1, 1), false);
            item.useOn(new net.minecraft.world.item.context.UseOnContext(ctx.level(), null, net.minecraft.world.InteractionHand.MAIN_HAND, item, hit));
        }

        public void measure(TestContext ctx) {
            var n = PipeCases.node(ctx, at);
            mods.eln.node.voxel.PipePart stub = null;
            if (n != null) for (var p : n.parts) if (p instanceof mods.eln.node.voxel.PipePart pp) stub = pp;
            ctx.check("adapter: placed with its pipe stub", stub != null, "");
            if (stub == null) return;
            var run = FluidWorld.nodeFor(stub.fluid);
            int room = (int) Math.floor(Math.max(0, 0.9 * run.volume - run.liquidVolume()) * Water.RHO_L / EaUnits.KG_PER_MB_WATER);
            took = takesAtOnce(ctx, at);
            ctx.check("adapter: other mods fill the run to 90 % at most, every slot at once", took <= room + 1, took + " mB taken, room " + room);
        }
    }
}
