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
public final class SelfTest {
    private static final Logger LOG = LogManager.getLogger("eln-selftest");

    // damage values = subId + (registration id << 6), see ElnContentImpl / ElnDeviceRegistry
    static final int SOURCE = 0 + (3 << 6);       // Electrical Source
    static final int LV_CABLE = 4 + (32 << 6);    // Low Voltage Cable
    static final int LAMP_SOCKET = 4 + (64 << 6); // Robust Lamp Socket
    static final int LAMP_BULB = 0 + (4 << 6);    // Small 50V Incandescent Light Bulb (shared item)
    static final int BATTERY = 0 + (16 << 6);     // Cost Oriented Battery
    static final double SOURCE_U = 50.0;
    static final double TOLERANCE = 0.01; // relative

    private static SelfTest running;
    private static final List<String> lastReport = Collections.synchronizedList(new ArrayList<String>());

    private final ICommandSender sender;
    private final WorldServer world;
    private final BlockPos origin; // platform start (x, y, z); elements at y + 1
    private final int ticks;
    private int tick = 0;
    private final List<String> report = new ArrayList<>();
    private final List<BlockPos> platform = new ArrayList<>();
    private final List<BlockPos> placed = new ArrayList<>();
    private int pass = 0, fail = 0;

    private ElectricalSourceElement source;
    private SixNodeElement cable1, cable2;
    private LampSocketElement lamp;
    private BatteryElement battery;

    private SelfTest(ICommandSender sender, WorldServer world, BlockPos origin, int ticks) {
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
            say(sender, "/eln selftest report : print the last run's results (also in the server log, logger eln-selftest)");
            return;
        }
        if (running != null) {
            say(sender, "eln selftest: already running (tick " + running.tick + "/" + running.ticks + ")");
            return;
        }
        int ticks = 40;
        int dim = 0;
        BlockPos pos = null;
        try {
            if (args.length >= 2) ticks = Math.max(5, Math.min(1200, Integer.parseInt(args[1])));
            if (args.length >= 6) {
                dim = Integer.parseInt(args[2]);
                pos = new BlockPos(Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            } else if (args.length != 2 && args.length != 1) {
                say(sender, "usage: /eln selftest [ticks] [dim x y z] | report | help");
                return;
            }
        } catch (NumberFormatException e) {
            say(sender, "usage: /eln selftest [ticks] [dim x y z] | report | help");
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
        SelfTest t = new SelfTest(sender, world, pos, ticks);
        synchronized (lastReport) {
            lastReport.clear();
        }
        if (!t.setUp()) {
            t.finish();
            return;
        }
        running = t;
        MinecraftForge.EVENT_BUS.register(t);
        say(sender, "eln selftest: started at dim " + dim + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
            + ", results in " + ticks + " ticks: /eln selftest report (and the server log)");
    }

    private static void say(ICommandSender s, String msg) {
        s.sendMessage(new TextComponentString(msg));
    }

    // ------------------------------------------------------------------ steps

    private void line(String s) {
        report.add(s);
        LOG.info(s);
    }

    private void check(String what, boolean ok, String detail) {
        if (ok) pass++;
        else fail++;
        line((ok ? "PASS " : "FAIL ") + what + (detail.isEmpty() ? "" : ": " + detail));
    }

    private void checkValue(String what, double measured, double expected) {
        double err = Math.abs(measured - expected) / Math.max(Math.abs(expected), 1e-9);
        boolean ok = !Double.isNaN(measured) && err <= TOLERANCE;
        check(what, ok, String.format("measured %.4f, expected %.4f (%.2f%%)", measured, expected, err * 100));
    }

    private BlockPos at(int dx, int dy, int dz) {
        return origin.add(dx, dy, dz);
    }

    /** Area: platform x 0..6, z 0..2 at origin.y; devices at y+1. */
    private boolean setUp() {
        line("eln selftest at dim " + world.provider.getDimension() + " " + origin.getX() + " " + origin.getY() + " "
            + origin.getZ() + ", " + ticks + " ticks");
        for (int dx = -1; dx <= 7; dx++)
            for (int dz = -1; dz <= 3; dz++)
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
            FakePlayer player = FakePlayerFactory.getMinecraft(world);
            source = (ElectricalSourceElement) placeSix(SOURCE, at(0, 1, 1), player);
            cable1 = placeSix(LV_CABLE, at(1, 1, 1), player);
            cable2 = placeSix(LV_CABLE, at(2, 1, 1), player);
            lamp = (LampSocketElement) placeSix(LAMP_SOCKET, at(3, 1, 1), player);
            battery = (BatteryElement) placeTransparent(BATTERY, at(6, 1, 1), player);
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
        return true;
    }

    private SixNodeElement placeSix(int damage, BlockPos p, FakePlayer player) {
        SixNodeDescriptor d = Eln.sixNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no six node descriptor " + damage);
        ItemStack stack = d.newItemStack();
        // as if clicking the top face of the platform block below: side 1 (up) -> element on the YN face
        boolean ok = Eln.sixNodeItem.placeBlockAt(stack, player, world, p.getX(), p.getY(), p.getZ(), 1, 0.5F, 1F, 0.5F, damage);
        if (!ok) throw new IllegalStateException("placeBlockAt failed for " + d.name + " at " + p);
        placed.add(p);
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), world));
        if (!(node instanceof SixNode)) throw new IllegalStateException("no SixNode at " + p);
        return ((SixNode) node).getElement(Direction.YN);
    }

    private Object placeTransparent(int damage, BlockPos p, FakePlayer player) {
        TransparentNodeDescriptor d = Eln.transparentNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no transparent node descriptor " + damage);
        ItemStack stack = d.newItemStack();
        IBlockState state = Eln.transparentNodeBlock.getDefaultState();
        boolean ok = Eln.transparentNodeItem.placeBlockAt(stack, player, world, p, EnumFacing.UP, 0.5F, 1F, 0.5F, state);
        if (!ok) throw new IllegalStateException("placeBlockAt failed for " + d.name + " at " + p);
        placed.add(p);
        NodeBase node = NodeManager.instance.getNodeFromCoordonate(new Coordonate(p.getX(), p.getY(), p.getZ(), world));
        if (!(node instanceof TransparentNode)) throw new IllegalStateException("no TransparentNode at " + p);
        return ((TransparentNode) node).element;
    }

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private static DataInputStream stream(Writer w) {
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
        for (BlockPos p : BlockPos.getAllInBox(at(-12, -8, -12), at(18, 12, 14))) {
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
        boolean drops = world.getGameRules().getBoolean("doTileDrops");
        try {
            world.getGameRules().setOrCreateGameRule("doTileDrops", "false");
            for (BlockPos p : placed) world.setBlockToAir(p);
            for (BlockPos p : BlockPos.getAllInBox(at(-12, -8, -12), at(18, 12, 14))) {
                if (world.getBlockState(p).getBlock() == ElnDeviceRegistry.lightBlock) world.setBlockToAir(p);
            }
            for (BlockPos p : platform) world.setBlockToAir(p);
            AxisAlignedBB box = new AxisAlignedBB(at(-2, -2, -2), at(9, 5, 5));
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
        line("eln selftest " + (fail == 0 ? "PASSED" : "FAILED") + ": " + pass + " pass, " + fail + " fail");
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
