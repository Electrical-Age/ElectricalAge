package mods.eln.gametest.cases;

import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.misc.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/** The shaft redo's art pass (Baughn 2026-10-29): hitboxes that follow each model and turn with the block's front. */
public final class ShaftArtCases {
    private ShaftArtCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new HitboxPerFacing());
        cases.add(new PortsMeetWhenPlaced());
        cases.add(new MotorSpinsUnloadedGenerators());
        cases.add(new SpeedKeptWhenPartsChange());
        cases.add(new TerminalsWhereDrawn());
        cases.add(new TrippedSeals());
        cases.add(new TankBoxReloads());
        cases.add(new TankBoxReformsLate());
        cases.add(new InlinePumpAlongView());
        cases.add(new TankBuckets());
    }

    static final int HP = 9 + (4 << 6), GEN4 = 10 + (4 << 6), GAS = 11 + (4 << 6), JOINT = 12 + (4 << 6), HUB = 13 + (4 << 6),
        FLYWHEEL = 14 + (4 << 6), TACHO = 15 + (4 << 6), GEN8 = 16 + (4 << 6), MOTOR = 17 + (4 << 6), LP = 18 + (4 << 6),
        PLANT_CONDENSER = 52 + (82 << 6);

    /**
     * Every shaft part's hitbox, for each of the four fronts: the model's (not the full cell), with its shaft at both
     * shaft faces (left and right of its front); the turbines' inlet on top and exhaust at the back; the plant
     * condenser's exhaust inlets on top, at its back and on its right.
     */
    static final class HitboxPerFacing implements TestCase {
        final List<String> bad = new ArrayList<>();
        int checked;

        public String id() {
            return "shaft_hitbox_per_facing";
        }

        public int ticks() {
            return 2;
        }

        static boolean has(VoxelShape s, double x, double y, double z) {
            for (var b : s.toAabbs()) if (b.contains(new Vec3(x, y, z))) return true;
            return false;
        }

        /** The point 0.47 of a block from the centre toward D, at height Y. */
        static boolean toward(VoxelShape s, Direction d, double y) {
            var f = d.toEnumFacing();
            return has(s, 0.5 + 0.47 * f.getStepX(), y, 0.5 + 0.47 * f.getStepZ());
        }

        public void build(TestContext ctx) {
            BlockPos p = ctx.at(4, 1, 1);
            int[] ids = {HP, LP, GAS, GEN4, GEN8, MOTOR, JOINT, HUB, FLYWHEEL, TACHO, PLANT_CONDENSER};
            String[] names = {"HP turbine", "LP turbine", "gas turbine", "4 kW generator", "8 kW generator", "shaft motor", "joint", "joint hub",
                "flywheel", "tachometer", "plant condenser"};
            for (int i = 0; i < ids.length; i++)
                for (Direction f : new Direction[]{Direction.XN, Direction.XP, Direction.ZN, Direction.ZP}) {
                    var e = ctx.placeTransparent(ids[i], p);
                    if (ids[i] != HUB) {   // (the hub has no front: it faces XP)
                        e.front = f;
                        e.reconnect();
                    }
                    Direction front = e.front;
                    VoxelShape s = ctx.level().getBlockState(p).getShape(ctx.level(), p);
                    String n = names[i] + " facing " + front;
                    var b = s.isEmpty() ? null : s.bounds();
                    if (b == null || (b.minX == 0 && b.minY == 0 && b.minZ == 0 && b.maxX == 1 && b.maxY == 1 && b.maxZ == 1 && s.toAabbs().size() == 1))
                        bad.add(n + ": not the model's hitbox");
                    else if (ids[i] == PLANT_CONDENSER) {
                        if (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5) || !toward(s, front.right(), 0.5))
                            bad.add(n + ": exhaust inlets (top, back, right)");
                    } else {
                        if (!toward(s, front.left(), 0.5) || !toward(s, front.right(), 0.5)) bad.add(n + ": shaft at its left and right");
                        if (ids[i] == HUB && (!toward(s, front, 0.5) || !toward(s, front.back(), 0.5))) bad.add(n + ": shaft on all four sides");
                        if ((ids[i] == HP || ids[i] == LP || ids[i] == GAS) && (!has(s, 0.5, 0.97, 0.5) || !toward(s, front.back(), 0.5)))
                            bad.add(n + ": inlet on top, exhaust at its back");
                    }
                    checked++;
                    ctx.level().removeBlock(p, false);
                }
        }

        public void measure(TestContext ctx) {
            ctx.check("shaft hitboxes: each model's, turned with its front (" + checked + " placements)", bad.isEmpty() && checked == 44, String.join("; ", bad));
        }
    }

    /** Place by the item path as a player looking toward YAW (degrees: 0 south, 90 west, 180 north, 270 east). */
    static mods.eln.node.transparent.TransparentNodeElement placeLooking(TestContext ctx, int damage, BlockPos p, float yaw) {
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(damage);
        // (the fake player is shared by every test: its view is put back, or other tests' placements turn with it)
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
        float y0 = player.getYRot(), x0 = player.getXRot();
        player.setYRot(yaw);
        player.setXRot(0);
        try {
            var node = ((mods.eln.node.transparent.TransparentNodeItem) d.parentItem).placeAt(d.newItemStack(), player, ctx.level(), p, Direction.YN);
            return node == null ? null : node.element;
        } finally {
            player.setYRot(y0);
            player.setXRot(x0);
        }
    }

    static boolean piped(mods.eln.sim.fluid.FluidNode n) {
        return n != null && !mods.eln.fluid.FluidWorld.edgesOf(n).isEmpty();
    }

    static final float EAST = 270, WEST = 90, NORTH = 180, SOUTH = 0;

    /**
     * AdventurAgent's client check (2026-10-29): an LP turbine and a plant condenser placed side by side by a player never
     * coupled. Placed by the item path with the player's view (no front set by the test): a shaft part's shaft runs
     * along the view; a turbine, gas turbine, tank, heat exchanger or plant condenser turns to meet the ports already
     * beside it. LP then condenser (the condenser in each of 4 facings) and condenser then LP; an HP's and a gas
     * turbine's exhaust and a large pipe, both orders; a tank and a pipe on its low spot; a heat exchanger and a pipe
     * at its end; a plant condenser between two cooling-water pipes.
     */
    static final class PortsMeetWhenPlaced extends PlumbingCases.Big {
        final List<String> bad = new ArrayList<>();
        final List<Runnable> checks = new ArrayList<>();

        public String id() {
            return "fluid_ports_meet_when_placed_by_a_player";
        }

        public int ticks() {
            return 10;
        }

        void expect(String what, java.util.function.BooleanSupplier ok) {
            checks.add(() -> {
                if (!ok.getAsBoolean()) bad.add(what);
            });
        }

        public void build(TestContext ctx) {
            // LP looking east (shaft east-west, exhaust south), then a condenser south of it, in each facing
            for (int k = 0; k < 4; k++) {
                BlockPos a = ctx.at(1 + 2 * k, 1, 0);
                var lp = (mods.eln.mechanical.ShaftSteamTurbineElement) placeLooking(ctx, LP, a, EAST);
                boolean along = lp.front == Direction.ZN || lp.front == Direction.ZP;
                expect("LP looking east: its shaft runs east-west (front " + lp.front + ")", () -> along);
                float yaw = 90 * k;
                placeLooking(ctx, PLANT_CONDENSER, a.south(), yaw);
                expect("LP then condenser (placed looking " + yaw + "): coupled", () -> piped(lp.exhaust));
            }
            // a condenser first, then the LP north of it looking west (its back would face north: it turns)
            for (int k = 0; k < 4; k++) {
                BlockPos a = ctx.at(1 + 2 * k, 1, 3);
                placeLooking(ctx, PLANT_CONDENSER, a.south(), 90 * k);
                var lp = (mods.eln.mechanical.ShaftSteamTurbineElement) placeLooking(ctx, LP, a, WEST);
                int kk = k;
                expect("condenser (placed looking " + 90 * kk + ") then LP: coupled", () -> piped(lp.exhaust));
            }
            // an HP and a gas turbine, then a large pipe at the back; a large pipe, then the turbine (it turns)
            int[] turbines = {HP, GAS};
            for (int t = 0; t < 2; t++) {
                BlockPos a = ctx.at(1 + 4 * t, 1, 6), b = ctx.at(3 + 4 * t, 1, 6);
                var e1 = placeLooking(ctx, turbines[t], a, EAST);
                PipeCases.lay(ctx, a.relative(e1.front.back().toEnumFacing()), 2, 8, 8, 0, 8, 8, 16);
                PipeCases.lay(ctx, b.south(), 2, 8, 8, 0, 8, 8, 16);
                var e2 = placeLooking(ctx, turbines[t], b, WEST);
                String n = t == 0 ? "HP" : "gas turbine";
                mods.eln.sim.fluid.FluidNode x1 = e1 instanceof mods.eln.mechanical.ShaftSteamTurbineElement s1 ? s1.exhaust : ((mods.eln.mechanical.GasTurbineElement) e1).exhaust;
                mods.eln.sim.fluid.FluidNode x2 = e2 instanceof mods.eln.mechanical.ShaftSteamTurbineElement s2 ? s2.exhaust : ((mods.eln.mechanical.GasTurbineElement) e2).exhaust;
                expect(n + " then a large pipe at its back: coupled", () -> piped(x1));
                expect("a large pipe, then the " + n + " (turned to it): coupled", () -> piped(x2));
            }
            // a tank looking at a pipe on its east (its front, the sight glass, would face it: it turns)
            PipeCases.lay(ctx, ctx.at(1, 1, 9), 1, 0, 4, 8, 16, 4, 8);
            var tank = (mods.eln.oil.TankElement) placeLooking(ctx, PlumbingCases.ALLOY_TANK, ctx.at(0, 1, 9), WEST);
            expect("a pipe, then a tank looking at it: coupled", () -> piped(tank.vessel));
            // a heat exchanger looking east (its ends east and west), a pipe at its east end's centre
            var hx = (mods.eln.oil.HeatExchangerElement) placeLooking(ctx, GasPlantCases.HX, ctx.at(4, 1, 9), EAST);
            PipeCases.lay(ctx, ctx.at(5, 1, 9), 2, 0, 8, 8, 16, 8, 8);
            expect("a heat exchanger, then a large pipe at its end: coupled", () -> piped(hx.a));
            // a plant condenser between two pipes on its low spots (north and south): its cooling ports take both
            PipeCases.lay(ctx, ctx.at(8, 1, 7), 1, 8, 4, 0, 8, 4, 16);
            PipeCases.lay(ctx, ctx.at(8, 1, 9), 1, 8, 4, 0, 8, 4, 16);
            var c = (mods.eln.oil.PlantCondenserElement) placeLooking(ctx, PLANT_CONDENSER, ctx.at(8, 1, 8), NORTH);
            expect("two pipes, then a plant condenser between them: both on its cooling-water ports",
                () -> mods.eln.fluid.FluidWorld.edgesOf(c.tubes).size() >= 2);
        }

        public void measure(TestContext ctx) {
            checks.forEach(Runnable::run);
            ctx.check("fluid ports meet when placed by a player (" + checks.size() + " checks)", bad.isEmpty(), String.join("; ", bad));
        }
    }

    static mods.eln.mechanical.ShaftNetwork shaftOf(mods.eln.node.transparent.TransparentNodeElement e) {
        return ((mods.eln.mechanical.ShaftElement) e).getShaft();
    }

    /** A 200V Shaft Motor at P (placed looking east: front north) fed 200 V on its front. */
    static mods.eln.mechanical.ShaftMotorElement poweredMotor(TestContext ctx, BlockPos p) {
        var m = (mods.eln.mechanical.ShaftMotorElement) placeLooking(ctx, MOTOR, p, EAST);
        BlockPos cable = p.relative(m.front.toEnumFacing());
        ctx.placeSix(Ids.six("Medium Voltage Cable"), cable);
        var s = (mods.eln.sixnode.electricalsource.ElectricalSourceElement) ctx.placeSix(Wp11Cases.SOURCE, cable.relative(m.front.toEnumFacing()));
        s.networkUnserialize(ctx.stream(out -> {
            out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
            out.writeFloat(200f);
        }));
        return m;
    }

    /**
     * AdventurAgent's client check (2026-10-29): a 200V Shaft Motor spinning a line with unloaded generators held it at
     * 0.5 rad/s (the generators' drag took 4 W even at a standstill: an unbounded torque). Placed by the player's view: a
     * motor and four unloaded 4 kW generators reach over 90 % of 800 rad/s within 30 s; with a flywheel (200 kJ at 800,
     * two minutes of the motor's 2 kW) the line is well on its way, speeding up.
     */
    static final class MotorSpinsUnloadedGenerators extends PlumbingCases.Big {
        mods.eln.node.transparent.TransparentNodeElement a, b;
        IProcessHolder trace = new IProcessHolder();
        double bAt20;

        static final class IProcessHolder {
            mods.eln.sim.IProcess p;
            int tick;
        }

        public String id() {
            return "shaft_motor_spins_unloaded_generators";
        }

        public int ticks() {
            return 600;
        }

        public void build(TestContext ctx) {
            for (int line = 0; line < 2; line++) {
                int z = line == 0 ? 2 : 7;
                var m = poweredMotor(ctx, ctx.at(1, 1, z));
                for (int x = 2; x <= 5; x++) placeLooking(ctx, GEN4, ctx.at(x, 1, z), EAST);
                placeLooking(ctx, line == 0 ? TACHO : FLYWHEEL, ctx.at(6, 1, z), EAST);
                if (line == 0) a = m;
                else b = m;
            }
            mods.eln.Eln.simulator.addSlowProcess(trace.p = dt -> {
                if (++trace.tick == 400) bAt20 = shaftOf(b).getRads();
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(trace.p);
            double ra = shaftOf(a).getRads(), rb = shaftOf(b).getRads();
            ctx.line(String.format("motor + 4 unloaded 4 kW generators: %.0f rad/s after 30 s; with a flywheel: %.0f at 20 s, %.0f at 30 s (inertia %.3f / %.3f kg m^2)",
                ra, bAt20, rb, shaftOf(a).getInertia(), shaftOf(b).getInertia()));
            ctx.check("motor + 4 unloaded generators: one shaft", shaftOf(a).getElements().size() == 6, String.valueOf(shaftOf(a).getElements().size()));
            ctx.check("motor + 4 unloaded generators: over 90 % of 800 rad/s within 30 s", ra > 720, String.format("%.0f rad/s", ra));
            ctx.check("with a flywheel too: under way and speeding up (over 250 rad/s at 30 s)", rb > 250 && rb > bAt20 + 20, String.format("%.0f -> %.0f rad/s", bAt20, rb));
        }
    }

    /**
     * Placing or breaking a shaft part keeps the line's speed (AdventurAgent: 800 -> 108 rad/s after a removal; a merge
     * took the slower side's speed): a cut keeps it on both sides; a light part placed at rest joins at nearly the line's
     * speed; a flywheel placed at rest takes its share of the angular momentum.
     */
    static final class SpeedKeptWhenPartsChange extends PlumbingCases.Big {
        String bad = "";
        String line = "";

        public String id() {
            return "shaft_speed_kept_when_parts_change";
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext ctx) {
            int[] ids = {JOINT, GEN8, GEN4, JOINT, TACHO};
            var parts = new mods.eln.node.transparent.TransparentNodeElement[ids.length];
            for (int i = 0; i < ids.length; i++) parts[i] = placeLooking(ctx, ids[i], ctx.at(1 + i, 1, 4), EAST);
            if (shaftOf(parts[0]).getElements().size() != 5) bad += "not one shaft; ";
            shaftOf(parts[0]).setRads(800);
            // a cut: the 4 kW generator in the middle goes
            ctx.level().removeBlock(ctx.at(3, 1, 4), false);
            double left = shaftOf(parts[0]).getRads(), right = shaftOf(parts[4]).getRads();
            if (Math.abs(left - 800) > 1 || Math.abs(right - 800) > 1) bad += String.format("cut: %.0f / %.0f rad/s; ", left, right);
            // a joint placed at rest on the right end: nearly the line's speed
            var i0 = shaftOf(parts[4]).getInertia();
            var j = placeLooking(ctx, JOINT, ctx.at(6, 1, 4), EAST);
            double want = 800 * i0 / (i0 + ((mods.eln.mechanical.ShaftElement) j).getShaftInertia()), got = shaftOf(j).getRads();
            if (Math.abs(got - want) > 1) bad += String.format("joint placed: %.0f rad/s (momentum: %.0f); ", got, want);
            // a flywheel at rest on the left end: the momentum shared
            var i1 = shaftOf(parts[0]).getInertia();
            var f = placeLooking(ctx, FLYWHEEL, ctx.at(0, 1, 4), EAST);
            double wantF = 800 * i1 / (i1 + ((mods.eln.mechanical.ShaftElement) f).getShaftInertia()), gotF = shaftOf(f).getRads();
            if (Math.abs(gotF - wantF) > 1) bad += String.format("flywheel placed: %.0f rad/s (momentum: %.0f); ", gotF, wantF);
            line = String.format("cut %.0f / %.0f; joint placed %.0f (of %.0f); flywheel placed %.0f (of %.0f)", left, right, got, want, gotF, wantF);
        }

        public void measure(TestContext ctx) {
            ctx.line("shaft speed: " + line);
            ctx.check("shaft speed kept when parts are cut, added, or a flywheel joins (momentum)", bad.isEmpty(), bad);
        }
    }

    /**
     * The motor's and generators' terminals are where they are drawn (AdventurAgent: a cable on the motor's other face
     * found no connector): placed looking east, the motor takes power on its front (the terminal plate under its
     * terminal box) and a signal on its back (the small plate); a generator power on its front and back; each face's
     * plate is in its hitbox, the plates' faces the connecting ones.
     */
    static final class TerminalsWhereDrawn extends PlumbingCases.Big {
        String bad = "";

        public String id() {
            return "shaft_terminals_where_drawn";
        }

        public int ticks() {
            return 2;
        }

        public void build(TestContext ctx) {
            var m = placeLooking(ctx, MOTOR, ctx.at(2, 1, 2), EAST);
            if (m.front != Direction.ZN) bad += "motor looking east faces " + m.front + "; ";
            if (m.getConnectionMask(m.front, mods.eln.misc.LRDU.Down) != mods.eln.node.NodeBase.maskElectricalPower) bad += "motor: no power on its front; ";
            if (m.getConnectionMask(m.front.back(), mods.eln.misc.LRDU.Down) != mods.eln.node.NodeBase.maskElectricalInputGate) bad += "motor: no signal on its back; ";
            for (Direction d : new Direction[]{m.front.left(), m.front.right()})
                if (m.getConnectionMask(d, mods.eln.misc.LRDU.Down) != 0) bad += "motor: a connection on its side " + d + "; ";
            VoxelShape s = ctx.level().getBlockState(m.node.coordonate.pos()).getShape(ctx.level(), m.node.coordonate.pos());
            if (!HitboxPerFacing.toward(s, m.front, 0.1) || !HitboxPerFacing.toward(s, m.front.back(), 0.1)) bad += "motor: its terminal plates aren't on its front and back; ";
            for (int id : new int[]{GEN4, GEN8}) {
                var g = placeLooking(ctx, id, ctx.at(id == GEN4 ? 5 : 8, 1, 2), EAST);
                for (Direction d : new Direction[]{g.front, g.front.back()})
                    if (g.getConnectionMask(d, mods.eln.misc.LRDU.Down) != mods.eln.node.NodeBase.maskElectricalPower) bad += "generator: no power on " + d + "; ";
                VoxelShape gs = ctx.level().getBlockState(g.node.coordonate.pos()).getShape(ctx.level(), g.node.coordonate.pos());
                if (!HitboxPerFacing.toward(gs, g.front, 0.1) || !HitboxPerFacing.toward(gs, g.front.back(), 0.1)) bad += "generator: its terminal plates aren't on its front and back; ";
            }
        }

        public void measure(TestContext ctx) {
            ctx.check("shaft terminals: where they are drawn, on the faces that connect", bad.isEmpty(), bad);
        }
    }

    /**
     * A trip seals (AdventurAgent 2026-10-29: a tripped LP behind an HP on manual took 1.5 g/s through its governor
     * valve's leak and kept the line at 916 rad/s): an HP and an LP on 20 bar steam, their shafts overdriven at 900
     * rad/s (past the 880 trip, as a manual HP on the line would): tripped, they pass no steam and make no power.
     */
    static final class TrippedSeals extends PlumbingCases.Big {
        final mods.eln.mechanical.ShaftSteamTurbineElement[] t = new mods.eln.mechanical.ShaftSteamTurbineElement[2];
        final double[] kg0 = new double[2], w0 = new double[2], kg1 = new double[2], w1 = new double[2];
        mods.eln.sim.IProcess drive;
        int tick;

        public String id() {
            return "shaft_turbine_tripped_seals";
        }

        public int ticks() {
            return 80;
        }

        public void build(TestContext ctx) {
            t[0] = (mods.eln.mechanical.ShaftSteamTurbineElement) ShaftSteamCases.orient(ctx.placeTransparent(HP, ctx.at(2, 1, 2)));
            t[1] = (mods.eln.mechanical.ShaftSteamTurbineElement) ShaftSteamCases.orient(ctx.placeTransparent(LP, ctx.at(2, 1, 6)));
            mods.eln.Eln.simulator.addSlowProcess(drive = dt -> {
                tick++;
                for (int i = 0; i < 2; i++) {
                    ShaftSteamCases.steam(t[i]);
                    shaftOf(t[i]).setRads(900);
                    if (tick == 20) {
                        kg0[i] = t[i].totalKg;
                        w0[i] = t[i].totalWork;
                    }
                    kg1[i] = t[i].totalKg;
                    w1[i] = t[i].totalWork;
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(drive);
            for (int i = 0; i < 2; i++) {
                String n = i == 0 ? "HP" : "LP";
                ctx.line(String.format("%s overdriven at 900 rad/s on 20 bar: tripped %s, %.4f g steam, %.1f J of work over 3 s; inlet %.1f bar",
                    n, t[i].tripped, (kg1[i] - kg0[i]) * 1000, w1[i] - w0[i], t[i].inlet.P() / 1e5));
                ctx.check(n + " tripped: no steam through it, no power from it", t[i].tripped && kg1[i] - kg0[i] < 1e-7 && w1[i] - w0[i] < 1e-3,
                    String.format("%.4f g, %.1f J", (kg1[i] - kg0[i]) * 1000, w1[i] - w0[i]));
            }
        }
    }

    /**
     * Unload these nodes and load them back as a world load does (NodeManager.loadFromNbt: each read, added and
     * initialized in turn, then all booted), in the given order.
     */
    static void reloadAll(TestContext ctx, List<BlockPos> at) throws ReflectiveOperationException {
        var w = ctx.level();
        List<net.minecraft.nbt.CompoundTag> tags = new ArrayList<>();
        List<Class<?>> classes = new ArrayList<>();
        for (BlockPos p : at) {
            var old = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, w));
            var t = new net.minecraft.nbt.CompoundTag();
            t.putString("tag", old.getNodeUuid());
            old.writeToNBT(t);
            tags.add(t);
            classes.add(old.getClass());
        }
        for (BlockPos p : at) {
            var old = mods.eln.node.NodeManager.instance.getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, w));
            old.unload();
            mods.eln.node.NodeManager.instance.removeNode(old);
        }
        List<mods.eln.node.NodeBase> fresh = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            var n = (mods.eln.node.NodeBase) classes.get(i).getConstructor().newInstance();
            n.readFromNBT(tags.get(i).copy());
            mods.eln.node.NodeManager.instance.addNode(n);
            n.initializeFromNBT();
            fresh.add(n);
        }
        for (var n : fresh) n.globalBoot();
        for (BlockPos p : at) if (w.getBlockEntity(p) instanceof mods.eln.node.NodeBlockEntity be) be.forgetNode();
    }

    /**
     * AdventurAgent (2026-10-29): a 2 x 2 x 2 iron tank with pipes on its west and east faces came back from a server
     * restart as eight one-block tanks. Built by a player's view, filled, piped; unloaded and loaded back (in placing order
     * and the reverse), it is one box again with all its water.
     */
    static final class TankBoxReloads extends PlumbingCases.Big {
        String bad = "", line = "";

        public String id() {
            return "tank_box_reforms_after_a_reload";
        }

        public int ticks() {
            return 2;
        }

        static mods.eln.oil.TankElement at(TestContext ctx, BlockPos p) {
            return (mods.eln.oil.TankElement) ((mods.eln.node.transparent.TransparentNode) mods.eln.node.NodeManager.instance
                .getNodeFromCoordonate(new mods.eln.misc.Coordonate(p, ctx.level()))).element;
        }

        public void build(TestContext ctx) {
            List<BlockPos> at = new ArrayList<>();
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 3; x++)
                    for (int z = 2; z <= 3; z++) {
                        placeLooking(ctx, PlumbingCases.IRON_TANK, ctx.at(x, y, z), EAST);
                        at.add(ctx.at(x, y, z));
                    }
            var g = at(ctx, at.get(0)).group;
            if (g == null || g.members.size() != 8) {
                bad = "not a box when built";
                return;
            }
            PlumbingCases.fill(g.node, 0.6 * g.node.volume * mods.eln.sim.fluid.Water.RHO_L, mods.eln.sim.fluid.Water.P_ATM, 293.15);
            double water = g.node.mWater;
            PipeCases.lay(ctx, ctx.at(1, 1, 2), 0, 0, 8, 8, 16, 8, 8);    // (an iron pipe to its west face's centre)
            PipeCases.lay(ctx, ctx.at(4, 1, 2), 2, 0, 8, 8, 16, 8, 8);    // (a large pipe to its east face's centre)
            for (int order = 0; order < 2; order++) {
                List<BlockPos> o = new ArrayList<>(at);
                o.add(ctx.at(1, 1, 2));   // (the pipes too, last; reversed: first)
                o.add(ctx.at(4, 1, 2));
                if (order == 1) java.util.Collections.reverse(o);
                try {
                    reloadAll(ctx, o);
                } catch (ReflectiveOperationException e) {
                    bad += e + "; ";
                    return;
                }
                var g2 = at(ctx, at.get(0)).group;
                boolean one = g2 != null && g2.members.size() == 8;
                for (BlockPos p : at) one &= at(ctx, p).group == g2;
                double w2 = one ? g2.node.mWater : 0;
                line += String.format("%s order: %s, %.3f of %.3f kg; ", order == 0 ? "placing" : "reverse", one ? "one box" : "NOT a box", w2, water);
                if (!one) bad += (order == 0 ? "placing" : "reverse") + " order: not one box; ";
                else if (Math.abs(w2 - water) > 1e-3 * water) bad += String.format("water %.3f of %.3f kg; ", w2, water);
            }
        }

        public void measure(TestContext ctx) {
            ctx.line("tank box reload: " + line);
            ctx.check("a tank box re-forms after a reload, with its water", bad.isEmpty(), bad);
        }
    }

    /**
     * Whatever leaves a full box of tanks as lone blocks (AdventurAgent's restart, not reproduced), the box re-forms within
     * a couple of seconds, with all its water: a 2 x 2 x 2 box broken into lone tanks behind its back (shares written,
     * groups dropped) is one box again.
     */
    static final class TankBoxReformsLate extends PlumbingCases.Big {
        final List<BlockPos> at = new ArrayList<>();
        double water;
        String bad = "";

        public String id() {
            return "tank_box_reforms_late";
        }

        public int ticks() {
            return 60;
        }

        public void build(TestContext ctx) {
            for (int y = 1; y <= 2; y++)
                for (int x = 2; x <= 3; x++)
                    for (int z = 2; z <= 3; z++) {
                        placeLooking(ctx, PlumbingCases.IRON_TANK, ctx.at(x, y, z), EAST);
                        at.add(ctx.at(x, y, z));
                    }
            var g = TankBoxReloads.at(ctx, at.get(0)).group;
            if (g == null) {
                bad = "not a box when built";
                return;
            }
            PlumbingCases.fill(g.node, 0.5 * g.node.volume * mods.eln.sim.fluid.Water.RHO_L, mods.eln.sim.fluid.Water.P_ATM, 293.15);
            water = g.node.mWater;
            try {
                var wb = mods.eln.oil.TankGroup.class.getDeclaredMethod("writeBack");
                wb.setAccessible(true);
                wb.invoke(g);
            } catch (ReflectiveOperationException e) {
                bad = e.toString();
                return;
            }
            for (BlockPos p : at) {
                var t = TankBoxReloads.at(ctx, p);
                t.group = null;
                t.vessel = t.own;
                mods.eln.fluid.FluidWorld.changed(t);
            }
        }

        public void measure(TestContext ctx) {
            if (bad.isEmpty()) {
                var g = TankBoxReloads.at(ctx, at.get(0)).group;
                boolean one = g != null && g.members.size() == 8;
                for (BlockPos p : at) one &= TankBoxReloads.at(ctx, p).group == g;
                if (!one) bad = "still lone tanks after 3 s";
                else if (Math.abs(g.node.mWater - water) > 1e-3 * water) bad = String.format("water %.3f of %.3f kg", g.node.mWater, water);
            }
            ctx.check("lone tanks that fill a box become one box again, with their water", bad.isEmpty(), bad);
        }
    }

    /**
     * The 200V Inline Pump placed looking east (AdventurAgent 2026-10-29: its line came out across the view): its line runs
     * east-west, in on the west (behind the player), out on the east; a pipe laid to its west face's low spot couples.
     */
    static final class InlinePumpAlongView extends PlumbingCases.Big {
        mods.eln.oil.InlinePumpElement p;
        String bad = "";

        public String id() {
            return "inline_pump_placed_along_the_view";
        }

        public int ticks() {
            return 4;
        }

        public void build(TestContext ctx) {
            p = (mods.eln.oil.InlinePumpElement) placeLooking(ctx, PlumbingCases.PUMP, ctx.at(4, 1, 4), EAST);
            int in = -1, out = -1;
            for (var port : p.fluidPorts()) {
                if (port.node() == p.inlet) in = port.face();
                if (port.node() == p.outlet) out = port.face();
            }
            if (in != 4 || out != 5) bad += "in on face " + in + ", out on " + out + " (want west 4, east 5); ";
            PipeCases.lay(ctx, ctx.at(3, 1, 4), 1, 0, 4, 8, 16, 4, 8);   // (to its west face's low spot)
        }

        public void measure(TestContext ctx) {
            if (mods.eln.fluid.FluidWorld.edgesOf(p.inlet).isEmpty()) bad += "the pipe at its west doesn't reach its inlet; ";
            ctx.check("inline pump placed looking east: its line east-west, in behind, out ahead; a pipe couples", bad.isEmpty(), bad);
        }
    }

    /**
     * Tanks take and give buckets of water (AdventurAgent 2026-10-29: a water bucket on an alloy tank did nothing): a water
     * bucket on any block of a 3 x 1 x 2 alloy box puts 1000 mB in, an empty bucket takes 1000 mB out; the same on a lone
     * iron tank; and a tank's water is its fluid capability.
     */
    static final class TankBuckets extends PlumbingCases.Big {
        final List<BlockPos> box = new ArrayList<>();
        BlockPos lone;
        double box0, lone0;
        mods.eln.sim.IProcess step;
        int tick;
        String bad = "";
        double[] after = new double[4];

        public String id() {
            return "tank_takes_and_gives_buckets";
        }

        public int ticks() {
            return 30;
        }

        static double liquidMb(mods.eln.oil.TankElement t) {
            return t.vessel.mLiquid() / mods.eln.sim.fluid.EaUnits.KG_PER_MB_WATER;
        }

        static boolean bucket(TestContext ctx, mods.eln.oil.TankElement t, net.minecraft.world.item.Item item) {
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(ctx.level());
            var held = player.getMainHandItem().copy();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
            boolean used = mods.eln.fluid.FluidBuckets.use(player, t.getFluidHandler());
            boolean filled = player.getMainHandItem().is(net.minecraft.world.item.Items.WATER_BUCKET);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
            return used && (item == net.minecraft.world.item.Items.BUCKET ? filled : true);
        }

        public void build(TestContext ctx) {
            for (int x = 1; x <= 3; x++)
                for (int z = 1; z <= 2; z++) {
                    placeLooking(ctx, PlumbingCases.ALLOY_TANK, ctx.at(x, 1, z), EAST);
                    box.add(ctx.at(x, 1, z));
                }
            lone = ctx.at(7, 1, 7);
            placeLooking(ctx, PlumbingCases.IRON_TANK, lone, EAST);
            var b = TankBoxReloads.at(ctx, box.get(4));
            if (b.group == null) bad += "the alloy tanks aren't a box; ";
            box0 = liquidMb(b);
            lone0 = liquidMb(TankBoxReloads.at(ctx, lone));
            mods.eln.Eln.simulator.addSlowProcess(step = dt -> {
                tick++;
                var bt = TankBoxReloads.at(ctx, box.get(4));
                var lt = TankBoxReloads.at(ctx, lone);
                if (tick == 2) {
                    if (!bucket(ctx, bt, net.minecraft.world.item.Items.WATER_BUCKET)) bad += "box: water bucket not taken; ";
                    if (!bucket(ctx, lt, net.minecraft.world.item.Items.WATER_BUCKET)) bad += "lone: water bucket not taken; ";
                }
                if (tick == 10) {
                    after[0] = liquidMb(bt);
                    after[1] = liquidMb(lt);
                    if (!bucket(ctx, TankBoxReloads.at(ctx, box.get(0)), net.minecraft.world.item.Items.BUCKET)) bad += "box: an empty bucket got no water; ";
                    if (!bucket(ctx, lt, net.minecraft.world.item.Items.BUCKET)) bad += "lone: an empty bucket got no water; ";
                }
                if (tick == 20) {
                    after[2] = liquidMb(bt);
                    after[3] = liquidMb(lt);
                }
            });
        }

        public void measure(TestContext ctx) {
            mods.eln.Eln.simulator.removeSlowProcess(step);
            ctx.line(String.format("tank buckets: box %.0f -> %.0f -> %.0f mB; lone %.0f -> %.0f -> %.0f mB", box0, after[0], after[2], lone0, after[1], after[3]));
            if (Math.abs(after[0] - box0 - 1000) > 5 || Math.abs(after[1] - lone0 - 1000) > 5) bad += "a water bucket didn't put 1000 mB in; ";
            if (Math.abs(after[0] - after[2] - 1000) > 5 || Math.abs(after[1] - after[3] - 1000) > 5) bad += "an empty bucket didn't take 1000 mB out; ";
            var t = TankBoxReloads.at(ctx, lone);
            var cap = ctx.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, lone, null);
            if (cap != t.water) bad += "the tank's water isn't its fluid capability (" + cap + "); ";
            ctx.check("tanks take and give buckets of water, lone or in a box", bad.isEmpty(), bad);
        }
    }
}
