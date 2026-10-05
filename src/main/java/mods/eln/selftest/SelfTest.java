package mods.eln.selftest;

import mods.eln.Eln;
import mods.eln.compat.BlockMeta;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.electricalsource.ElectricalSourceElement;
import mods.eln.sixnode.lampsocket.LampSocketElement;
import mods.eln.item.LampDescriptor;
import mods.eln.transparentnode.battery.BatteryDescriptor;
import mods.eln.transparentnode.battery.BatteryElement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-game regression test (porting guide "Tests"): `/eln selftest [ticks] [dim x y z]` builds, on a temporary stone
 * platform high above world spawn (or at the given position), the circuit
 * <pre>  electrical source (50 V) -> LV cable -> LV cable -> robust lamp socket (LV cable + small 50 V bulb)</pre>
 * all on the floor along +X, plus a cost oriented battery two blocks further, waits `ticks` server ticks (default
 * 40), checks the node voltages and the lamp current against Ohm's law with EA's own resistances (connection between
 * two loads = Rs(a) + Rs(b); source Rs = LV cable Rs; lamp R = U^2/P), checks the lamp's light block and the battery's
 * open-circuit voltage, then removes everything (no drops) and restores the area.
 *
 * RCON only returns what a command prints synchronously, so results go to the server log (logger "eln-selftest")
 * and are kept for `/eln selftest report`.
 */
public final class SelfTest implements SelfTestContext {
    private static final Logger LOG = LogManager.getLogger("eln-selftest");

    // damage values = subId + (registration id << 6), see ElnContentImpl / ElnDeviceRegistry
    static final int SOURCE = 0 + (3 << 6);       // Electrical Source
    static final int LV_CABLE = 4 + (32 << 6);    // Low Voltage Cable
    static final int LAMP_SOCKET = 4 + (64 << 6); // Robust Lamp Socket
    static final int LAMP_BULB = 0 + (4 << 6);    // Small 50V Incandescent Light Bulb (shared item)
    static final int BATTERY = 0 + (16 << 6);     // Cost Oriented Battery
    static final double SOURCE_U = 50.0;
    static final double TOLERANCE = 0.01; // relative

    /** Extra cases, one line per device batch (plain runs only; not keep/verify). */
    static List<SelfTestCase> cases() {
        List<SelfTestCase> c = new ArrayList<>();
        mods.eln.selftest.cases.Wp9aCases.addTo(c);
        mods.eln.selftest.cases.Wp9bCases.addTo(c);
        mods.eln.selftest.cases.Wp9cCases.addTo(c);
        mods.eln.selftest.cases.Wp10aCases.addTo(c);
        mods.eln.selftest.cases.Wp10bCases.addTo(c);
        mods.eln.selftest.cases.Wp11Cases.addTo(c);
        mods.eln.selftest.cases.Wp12Cases.addTo(c);
        return c;
    }

    /** Each extra case gets a platform row at origin + (0, 0, CASE_ROW * (index + 1)). */
    static final int CASE_ROW = 5;

    private static SelfTest running;
    private static final List<String> lastReport = Collections.synchronizedList(new ArrayList<String>());

    /** Cases whose build threw: their measure is skipped (it would only NPE on the missing devices). */
    private final java.util.Set<SelfTestCase> buildFailed = new java.util.HashSet<>();
    private final ICommandSender sender;
    private final WorldServer world;
    private final BlockPos origin; // platform start (x, y, z); elements at y + 1
    private final int ticks;
    private final Mode mode;
    private int tick = 0;
    private final List<String> report = new ArrayList<>();
    private final List<BlockPos> platform = new ArrayList<>();
    private final List<BlockPos> placed = new ArrayList<>();
    private int pass = 0, fail = 0;
    private final List<SelfTestCase> extra;
    private BlockPos base; // at() origin: the slice's platform, or the current extra case's row
    private FakePlayer player;

    private ElectricalSourceElement source;
    private SixNodeElement cable1, cable2;
    private LampSocketElement lamp;
    private BatteryElement battery;

    /** RUN: build, measure, clean up. KEEP: build, measure, leave it (for a restart). VERIFY: re-measure a kept circuit, clean up. */
    enum Mode {RUN, KEEP, VERIFY}

    private SelfTest(ICommandSender sender, WorldServer world, BlockPos origin, int ticks, Mode mode) {
        this.mode = mode;
        this.extra = mode == Mode.RUN ? cases() : new ArrayList<SelfTestCase>();
        this.base = origin;
        this.sender = sender;
        this.world = world;
        this.origin = origin;
        this.ticks = ticks;
    }

    // ------------------------------------------------------------------ command

    public static void command(MinecraftServer server, ICommandSender sender, String[] args) {
        // args[0] == "selftest"
        if (args.length >= 2 && args[1].equalsIgnoreCase("report")) {
            synchronized (lastReport) {
                if (lastReport.isEmpty()) say(sender, "eln selftest: no report yet" + (running != null ? " (running)" : ""));
                for (String l : lastReport) say(sender, l);
            }
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("help")) {
            say(sender, "/eln selftest [ticks] [dim x y z] : run (default 40 ticks, 24 blocks above world spawn in dim 0)");
            say(sender, "/eln selftest keep [ticks] [dim x y z] : same, but leave the circuit in place (prints its position)");
            say(sender, "/eln selftest verify dim x y z [ticks] : after a restart, re-measure a kept circuit, then clean up");
            say(sender, "/eln selftest report : print the last run's results (also in the server log, logger eln-selftest)");
            return;
        }
        if (running != null) {
            say(sender, "eln selftest: already running (tick " + running.tick + "/" + running.ticks + ")");
            return;
        }
        // tokens after "selftest": [keep|verify] then numbers
        Mode mode = Mode.RUN;
        int first = 1;
        if (args.length >= 2 && args[1].equalsIgnoreCase("keep")) {
            mode = Mode.KEEP;
            first = 2;
        } else if (args.length >= 2 && args[1].equalsIgnoreCase("verify")) {
            mode = Mode.VERIFY;
            first = 2;
        }
        int n = args.length - first;
        int ticks = 40;
        int dim = 0;
        BlockPos pos = null;
        String usage = "usage: /eln selftest [keep] [ticks] [dim x y z] | verify dim x y z [ticks] | report | help";
        try {
            if (mode == Mode.VERIFY) {
                if (n != 4 && n != 5) {
                    say(sender, usage);
                    return;
                }
                dim = Integer.parseInt(args[first]);
                pos = new BlockPos(Integer.parseInt(args[first + 1]), Integer.parseInt(args[first + 2]), Integer.parseInt(args[first + 3]));
                if (n == 5) ticks = Integer.parseInt(args[first + 4]);
            } else {
                if (n != 0 && n != 1 && n != 5) {
                    say(sender, usage);
                    return;
                }
                if (n >= 1) ticks = Integer.parseInt(args[first]);
                if (n == 5) {
                    dim = Integer.parseInt(args[first + 1]);
                    pos = new BlockPos(Integer.parseInt(args[first + 2]), Integer.parseInt(args[first + 3]), Integer.parseInt(args[first + 4]));
                }
            }
            ticks = Math.max(5, Math.min(1200, ticks));
        } catch (NumberFormatException e) {
            say(sender, usage);
            return;
        }
        WorldServer world = DimensionManager.getWorld(dim);
        if (world == null) {
            say(sender, "eln selftest: dimension " + dim + " is not loaded");
            return;
        }
        if (pos == null) {
            BlockPos spawn = world.getSpawnPoint();
            int y = Math.min(world.getHeight() - 12, Math.max(world.getHeight(spawn).getY() + 24, 100));
            pos = new BlockPos(spawn.getX() + 4, y, spawn.getZ() + 4);
        }
        SelfTest t = new SelfTest(sender, world, pos, ticks, mode);
        synchronized (lastReport) {
            lastReport.clear();
        }
        if (!(mode == Mode.VERIFY ? t.find() : t.setUp())) {
            if (mode == Mode.VERIFY) t.done(); // leave a broken kept circuit for inspection
            else {
                t.cleanup();
                t.done();
            }
            return;
        }
        running = t;
        MinecraftForge.EVENT_BUS.register(t);
        say(sender, "eln selftest " + mode.name().toLowerCase() + ": started at dim " + dim + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
            + ", results in " + ticks + " ticks: /eln selftest report (and the server log)");
    }

    private static void say(ICommandSender s, String msg) {
        s.sendMessage(new TextComponentString(msg));
    }

    // ------------------------------------------------------------------ steps

    @Override
    public WorldServer world() {
        return world;
    }

    @Override
    public void line(String s) {
        report.add(s);
        LOG.info(s);
    }

    @Override
    public void check(String what, boolean ok, String detail) {
        if (ok) pass++;
        else fail++;
        line((ok ? "PASS " : "FAIL ") + what + (detail.isEmpty() ? "" : ": " + detail));
    }

    @Override
    public void checkValue(String what, double measured, double expected) {
        double err = Math.abs(measured - expected) / Math.max(Math.abs(expected), 1e-9);
        boolean ok = !Double.isNaN(measured) && err <= TOLERANCE;
        check(what, ok, String.format("measured %.4f, expected %.4f (%.2f%%)", measured, expected, err * 100));
    }

    @Override
    public BlockPos at(int dx, int dy, int dz) {
        return base.add(dx, dy, dz);
    }

    /** z extent of the whole test area (slice row + extra case rows). */
    private int maxDz() {
        return 3 + CASE_ROW * extra.size();
    }

    /** Area: platform x 0..6, z 0..2 at origin.y; devices at y+1. */
    private boolean setUp() {
        line("eln selftest at dim " + world.provider.getDimension() + " " + origin.getX() + " " + origin.getY() + " "
            + origin.getZ() + ", " + ticks + " ticks");
        // the case rows reach maxDz() blocks along +Z (5 per case); load chunks nobody is watching (a run takes a few
        // seconds, unwatched chunks are only dropped at the next autosave)
        int loadedNow = 0;
        for (int dx = -1; dx <= 17; dx++)
            for (int dz = -1; dz <= maxDz(); dz++) {
                BlockPos p = at(dx, 0, dz);
                if (!world.isBlockLoaded(p)) {
                    world.getChunk(p);
                    loadedNow++;
                }
            }
        if (loadedNow > 0) line("loaded " + loadedNow + " chunk(s) of the test area");
        for (int dx = -1; dx <= 17; dx++)
            for (int dz = -1; dz <= maxDz(); dz++)
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos p = at(dx, dy, dz);
                    if (!world.isBlockLoaded(p)) {
                        check("setup", false, "area not loaded at " + p);
                        return false;
                    }
                    if (!world.isAirBlock(p)) {
                        check("setup", false, "area not empty at " + p + " (" + world.getBlockState(p) + "); pick another position");
                        return false;
                    }
                }
        for (int dx = 0; dx <= 6; dx++)
            for (int dz = 0; dz <= 2; dz++) {
                BlockPos p = at(dx, 0, dz);
                world.setBlockState(p, Blocks.STONE.getDefaultState(), 3);
                platform.add(p);
            }

        try {
            player = FakePlayerFactory.getMinecraft(world);
            source = (ElectricalSourceElement) placeSix(SOURCE, at(0, 1, 1));
            cable1 = placeSix(LV_CABLE, at(1, 1, 1));
            cable2 = placeSix(LV_CABLE, at(2, 1, 1));
            lamp = (LampSocketElement) placeSix(LAMP_SOCKET, at(3, 1, 1));
            battery = (BatteryElement) placeTransparent(BATTERY, at(6, 1, 1));
        } catch (RuntimeException e) {
            check("placement", false, e.toString());
            LOG.error("placement failed", e);
            return false;
        }
        check("placement", source != null && cable1 != null && cable2 != null && lamp != null && battery != null,
            "source " + (source != null) + ", cables " + (cable1 != null) + "/" + (cable2 != null) + ", lamp "
                + (lamp != null) + ", battery " + (battery != null));
        if (source == null || cable1 == null || cable2 == null || lamp == null || battery == null) return false;

        // source voltage through the GUI's packet path (ElectricalSourceElement.setVoltageId)
        source.networkUnserialize(stream(out -> {
            out.writeByte(ElectricalSourceElement.setVoltageId);
            out.writeFloat((float) SOURCE_U);
        }));
        // lamp: bulb + cable in the socket's slots (what the GUI slots do), then wired instead of lamp supply
        lamp.getInventory().setInventorySlotContents(0, Eln.sharedItem.getDescriptor(LAMP_BULB).newItemStack());
        lamp.getInventory().setInventorySlotContents(1, Eln.sixNodeItem.getDescriptor(LV_CABLE).newItemStack());
        lamp.getInventory().markDirty();
        lamp.networkUnserialize(stream(out -> out.writeByte(3))); // LampSocketElement.tooglePowerSupplyType

        for (int i = 0; i < extra.size(); i++) {
            SelfTestCase c = extra.get(i);
            base = origin.add(0, 0, CASE_ROW * (i + 1));
            try {
                int w = Math.max(1, Math.min(16, c.width()));
                for (int dx = 0; dx < w; dx++)
                    for (int dz = 0; dz <= 2; dz++) {
                        world.setBlockState(at(dx, 0, dz), Blocks.STONE.getDefaultState(), 3);
                        platform.add(at(dx, 0, dz));
                    }
                line("case " + c.name() + " at " + base);
                c.build(this);
            } catch (RuntimeException e) {
                check(c.name() + " build", false, e.toString());
                LOG.error("case " + c.name() + " build failed", e);
                buildFailed.add(c);
            }
        }
        base = origin;
        return true;
    }

    /** VERIFY: the circuit a KEEP run left at origin (after a save/restart): find its elements again. */
    private boolean find() {
        line("eln selftest verify at dim " + world.provider.getDimension() + " " + origin.getX() + " " + origin.getY()
            + " " + origin.getZ() + ", " + ticks + " ticks");
        world.getChunk(at(0, 1, 1));
        world.getChunk(at(6, 1, 1));
        for (int dx = 0; dx <= 6; dx++)
            for (int dz = 0; dz <= 2; dz++)
                if (world.getBlockState(at(dx, 0, dz)).getBlock() == Blocks.STONE) platform.add(at(dx, 0, dz));
        try {
            source = (ElectricalSourceElement) findSix(at(0, 1, 1));
            cable1 = findSix(at(1, 1, 1));
            cable2 = findSix(at(2, 1, 1));
            lamp = (LampSocketElement) findSix(at(3, 1, 1));
            NodeBase node = NodeManager.instance.getNodeFromCoordonate(coord(at(6, 1, 1)));
            if (node instanceof TransparentNode && ((TransparentNode) node).element instanceof BatteryElement) {
                battery = (BatteryElement) ((TransparentNode) node).element;
                placed.add(at(6, 1, 1));
            }
        } catch (RuntimeException e) {
            check("find kept circuit", false, e.toString());
            LOG.error("find failed", e);
            return false;
        }
        boolean ok = source != null && cable1 != null && cable2 != null && lamp != null && battery != null;
        check("find kept circuit", ok, "source " + (source != null) + ", cables " + (cable1 != null) + "/" + (cable2 != null)
            + ", lamp " + (lamp != null) + ", battery " + (battery != null) + ", platform blocks " + platform.size());
        return ok;
    }

    private static Coordonate coord(BlockPos p, WorldServer w) {
        return new Coordonate(p.getX(), p.getY(), p.getZ(), w);
    }

    private Coordonate coord(BlockPos p) {
        return coord(p, world);
    }

    private SixNodeElement findSix(BlockPos p) {
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(coord(p));
        if (!(node instanceof SixNode)) return null;
        placed.add(p);
        return ((SixNode) node).getElement(Direction.YN);
    }

    @Override
    public SixNodeElement placeSix(int damage, BlockPos p) {
        // as if clicking the top face of the platform block below: side 1 (up) -> element on the YN face
        return placeSix(damage, p, Direction.YN);
    }

    @Override
    public SixNodeElement placeSix(int damage, BlockPos p, Direction side) {
        SixNodeDescriptor d = Eln.sixNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no six node descriptor " + damage);
        ItemStack stack = d.newItemStack();
        EnumFacing clicked = side.getInverse().toEnumFacing(); // face of the neighbour block at p + side
        float hx = 0.5F + 0.5F * clicked.getXOffset(), hy = 0.5F + 0.5F * clicked.getYOffset(), hz = 0.5F + 0.5F * clicked.getZOffset();
        boolean ok = Eln.sixNodeItem.placeBlockAt(stack, player, world, p.getX(), p.getY(), p.getZ(), clicked.getIndex(), hx, hy, hz, damage);
        if (!ok) throw new IllegalStateException("placeBlockAt failed for " + d.name + " at " + p + " on " + side);
        if (!placed.contains(p)) placed.add(p);
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), world));
        if (!(node instanceof SixNode)) throw new IllegalStateException("no SixNode at " + p);
        return ((SixNode) node).getElement(side);
    }

    @Override
    public TransparentNodeElement placeTransparent(int damage, BlockPos p) {
        TransparentNodeDescriptor d = Eln.transparentNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no transparent node descriptor " + damage);
        ItemStack stack = d.newItemStack();
        IBlockState state = Eln.transparentNodeBlock.getDefaultState();
        // where TransparentNodeItem.placeBlockAt puts the node: p + the descriptor's spawn delta, rotated by the front
        // it computes from the side (UP) and the player's view (autominer: (2, 1, 0) -> the node is not at p)
        Direction front = d.getFrontFromPlace(Direction.fromIntMinecraftSide(EnumFacing.UP.getIndex()).getInverse(), player);
        int[] v = new int[]{d.getSpawnDeltaX(), d.getSpawnDeltaY(), d.getSpawnDeltaZ()};
        front.rotateFromXN(v);
        BlockPos np = p.add(v[0], v[1], v[2]);
        boolean ok = Eln.transparentNodeItem.placeBlockAt(stack, player, world, p, EnumFacing.UP, 0.5F, 1F, 0.5F, state);
        if (!ok) throw new IllegalStateException("placeBlockAt failed for " + d.name + " at " + p);
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(np.getX(), np.getY(), np.getZ(), world));
        if (node != null && !placed.contains(np)) placed.add(np);
        if (!(node instanceof TransparentNode)) throw new IllegalStateException("no TransparentNode at " + np + " (placed at " + p + ")");
        return ((TransparentNode) node).element;
    }

    public interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    @Override
    public DataInputStream stream(Writer w) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            w.write(new DataOutputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++tick < ticks) return;
        MinecraftForge.EVENT_BUS.unregister(this);
        try {
            measure();
        } catch (RuntimeException ex) {
            check("measure", false, ex.toString());
            LOG.error("measure failed", ex);
        }
        for (int i = 0; i < extra.size(); i++) {
            SelfTestCase c = extra.get(i);
            base = origin.add(0, 0, CASE_ROW * (i + 1));
            if (buildFailed.contains(c)) {
                line("SKIP " + c.name() + " measure (build failed)");
                continue;
            }
            try {
                c.measure(this);
            } catch (RuntimeException ex) {
                check(c.name() + " measure", false, ex.toString());
                LOG.error("case " + c.name() + " measure failed", ex);
            }
        }
        base = origin;
        finish();
    }

    private void measure() {
        double rsLv = ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs; // source Rs (applySmallRs) = LV cable Rs
        ElectricalCableDescriptor lv = (ElectricalCableDescriptor) Eln.sixNodeItem.getDescriptor(LV_CABLE);
        double rsCable = lv.electricalRs;
        LampDescriptor bulb = (LampDescriptor) Eln.sharedItem.getDescriptor(LAMP_BULB);
        double rLamp = bulb.getR();
        // connections: src-c1, c1-c2, c2-lamp; the lamp's positive load has the Rs of the LV cable in its slot
        double r1 = rsLv + rsCable, r2 = rsCable + rsCable, r3 = rsCable + rsCable;
        double i = SOURCE_U / (r1 + r2 + r3 + rLamp);
        line(String.format("model: Rs(LV cable) = %.6f ohm, R(lamp) = %.3f ohm, I = U / (R1+R2+R3+Rlamp) = %.5f A", rsCable, rLamp, i));

        ElectricalLoad src = source.getElectricalLoad(LRDU.Up);
        ElectricalLoad c1 = cable1.getElectricalLoad(LRDU.Up);
        ElectricalLoad c2 = cable2.getElectricalLoad(LRDU.Up);
        checkValue("source voltage [V]", src.getU(), SOURCE_U);
        checkValue("cable 1 voltage [V]", c1.getU(), SOURCE_U - i * r1);
        checkValue("cable 2 voltage [V]", c2.getU(), SOURCE_U - i * (r1 + r2));
        checkValue("lamp voltage [V]", lamp.positiveLoad.getU(), i * rLamp);
        checkValue("lamp current [A]", Math.abs(lamp.lampResistor.getCurrent()), i);
        checkValue("lamp power [W]", Math.abs(lamp.lampResistor.getCurrent() * lamp.lampResistor.getU()), i * i * rLamp);

        // light: LampSocketProcess turns the lamp voltage into a level (incandescent: nominalLight * 16 *
        // (U - Umin) / (Unom - Umin), capped at 14) and puts it either on the lamp's own block (node light value) or
        // on a LightBlock (META = level) up to `range` blocks away. Check the level and that the world lights it.
        double uLamp = Math.abs(lamp.lampResistor.getU());
        int expectedLight = (int) Math.max(0, Math.min(14,
            bulb.nominalLight * 16 * (uLamp - bulb.minimalU) / (bulb.nominalU - bulb.minimalU)));
        int level = lamp.getLightValue();
        BlockPos lightPos = at(3, 1, 1);
        int lightBlocks = 0;
        for (BlockPos p : BlockPos.getAllInBox(at(-12, -8, -12), at(28, 12, 14 + maxDz()))) {
            IBlockState s = world.getBlockState(p);
            if (s.getBlock() == ElnDeviceRegistry.lightBlock) {
                lightBlocks++;
                if (s.getValue(BlockMeta.META) > level) {
                    level = s.getValue(BlockMeta.META);
                    lightPos = p.toImmutable();
                }
            }
        }
        check("lamp light level", expectedLight > 0 && Math.abs(level - expectedLight) <= 1,
            "level " + level + " (expected " + expectedLight + "), " + lightBlocks + " light block(s), at " + lightPos);
        int worldLight = world.getLightFor(EnumSkyBlock.BLOCK, lightPos);
        check("world block light", worldLight >= level - 1 && level > 0, "block light " + worldLight + " at " + lightPos);

        // battery, open circuit: U(+) - U(-) = voltageFunction(charge) * U nominal
        BatteryDescriptor bd = battery.descriptor;
        double expectedU = battery.batteryProcess.voltageFunction.getValue(bd.startCharge) * battery.batteryProcess.uNominal;
        checkValue("battery open-circuit voltage [V]", battery.positiveLoad.getU() - battery.negativeLoad.getU(), expectedU);
        check("battery charge", Math.abs(battery.batteryProcess.getCharge() - bd.startCharge) < 0.01,
            String.format("%.4f (item start charge %.4f)", battery.batteryProcess.getCharge(), bd.startCharge));
    }

    /** Remove everything we placed, without drops; restore the platform to air. */
    private void finish() {
        if (mode == Mode.KEEP && !placed.isEmpty()) {
            line("kept: verify after a restart with /eln selftest verify " + world.provider.getDimension() + " "
                + origin.getX() + " " + origin.getY() + " " + origin.getZ());
            done();
            return;
        }
        cleanup();
        done();
    }

    private void cleanup() {
        boolean drops = world.getGameRules().getBoolean("doTileDrops");
        try {
            world.getGameRules().setOrCreateGameRule("doTileDrops", "false");
            for (BlockPos p : placed) world.setBlockToAir(p);
            for (BlockPos p : BlockPos.getAllInBox(at(-12, -8, -12), at(28, 12, 14 + maxDz()))) {
                if (world.getBlockState(p).getBlock() == ElnDeviceRegistry.lightBlock) world.setBlockToAir(p);
            }
            for (BlockPos p : platform) world.setBlockToAir(p);
            AxisAlignedBB box = new AxisAlignedBB(at(-2, -2, -2), at(19, 5, 3 + maxDz()));
            for (EntityItem item : world.getEntitiesWithinAABB(EntityItem.class, box)) item.setDead();
            boolean clean = true;
            for (BlockPos p : placed) {
                if (!world.isAirBlock(p)) clean = false;
                if (NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), world)) != null)
                    clean = false;
            }
            check("cleanup", clean, placed.size() + " node block(s) and " + platform.size() + " platform block(s) removed");
        } catch (RuntimeException e) {
            check("cleanup", false, e.toString());
            LOG.error("cleanup failed", e);
        } finally {
            world.getGameRules().setOrCreateGameRule("doTileDrops", Boolean.toString(drops));
        }
    }

    private void done() {
        line("eln selftest " + mode.name().toLowerCase() + " " + (fail == 0 ? "PASSED" : "FAILED") + ": " + pass + " pass, " + fail + " fail");
        synchronized (lastReport) {
            lastReport.clear();
            lastReport.addAll(report);
        }
        running = null;
        try {
            for (String l : report) say(sender, l);
        } catch (RuntimeException ignored) {
            // sender gone (player logged out)
        }
    }
}
