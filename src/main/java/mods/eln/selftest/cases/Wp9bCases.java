package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.compat.WorldCompat;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sixnode.ScanMode;
import mods.eln.sixnode.ScannerElement;
import mods.eln.sixnode.electricalalarm.ElectricalAlarmElement;
import mods.eln.sixnode.electricalfiredetector.ElectricalFireDetectorElement;
import mods.eln.sixnode.electricallightsensor.ElectricalLightSensorElement;
import mods.eln.sixnode.electricalredstoneinput.ElectricalRedstoneInputElement;
import mods.eln.sixnode.electricalredstoneoutput.ElectricalRedstoneOutputElement;
import mods.eln.sixnode.electricalsensor.ElectricalSensorElement;
import mods.eln.sixnode.electricalvumeter.ElectricalVuMeterElement;
import mods.eln.sixnode.electricalwatch.ElectricalWatchElement;
import mods.eln.sixnode.electricalweathersensor.ElectricalWeatherSensorElement;
import mods.eln.sixnode.thermalcable.ThermalCableElement;
import mods.eln.sixnode.thermalsensor.ThermalSensorElement;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Selftest cases of device batch Wp9b (registry/batch/Wp9bContent). Every gate output is loaded by a signal cable
 * (a lone gate load is not measured). Devices are placed on the floor; the fake player's view is chosen so that the
 * device's pin points at the intended neighbour (the "geometry" check fails otherwise). Vanilla blocks a case adds
 * (redstone block, stone, chest, netherrack, fire) are removed at the end of its measure().
 * Not tested server-side: anemometer (wall only: refuses floor/ceiling placement), entity sensor (needs a moving
 * living entity: output = speed-weighted), watches/fire buzzer (battery slot needs wp12's BatteryItem; only placement
 * is checked for the watch), LED vuMeter / standard alarm / thermal probe / electrical probe / light sensor
 * (same code paths as their tested variants).
 */
public final class Wp9bCases {
    private Wp9bCases() {
    }

    // damage = subId + (id << 6), 1.7.10 ids
    static final int SIGNAL_CABLE = 0 + (32 << 6);
    static final int LV_CABLE = 4 + (32 << 6);
    static final int SOURCE = 0 + (3 << 6);
    static final int THERMAL_CABLE = 1 + (48 << 6);
    static final int VOLTAGE_PROBE = 1 + (100 << 6);
    static final int TEMPERATURE_PROBE = 1 + (101 << 6);
    static final int VUMETER = 0 + (102 << 6);
    static final int NUCLEAR_ALARM = 0 + (103 << 6);
    static final int DAYLIGHT_SENSOR = 0 + (104 << 6);
    static final int WEATHER_SENSOR = 4 + (104 << 6);
    static final int FIRE_DETECTOR = 13 + (104 << 6);
    static final int SCANNER = 15 + (104 << 6);
    static final int REDSTONE_IN = 0 + (108 << 6);
    static final int REDSTONE_OUT = 1 + (108 << 6);
    static final int ANALOG_WATCH = 4 + (117 << 6);

    static final UnaryOperator<LRDU> FRONT = f -> f;
    static final UnaryOperator<LRDU> FRONT_RIGHT = LRDU::right; // pins declared as `front == lrdu.left()`
    static final UnaryOperator<LRDU> FRONT_INVERSE = LRDU::inverse;

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new RedstoneChain());
        cases.add(new Scanner());
        cases.add(new Probes());
        cases.add(new Environment());
    }

    /**
     * Place a six-node device on the floor of p with a view (fake player yaw) such that the pin
     * pin(front) points to `pinTo`. The front is what the descriptor computes for that view.
     */
    static SixNodeElement placeToward(SelfTestContext ctx, int damage, BlockPos p, UnaryOperator<LRDU> pin, EnumFacing pinTo) {
        FakePlayer fp = FakePlayerFactory.getMinecraft(ctx.world());
        SixNodeDescriptor d = Eln.sixNodeItem.getDescriptor(damage);
        if (d == null) throw new IllegalStateException("no six node descriptor " + damage);
        float oldYaw = fp.rotationYaw;
        try {
            for (float yaw = 0; yaw < 360; yaw += 90) {
                fp.rotationYaw = yaw;
                LRDU front = d.getFrontFromPlace(Direction.YN, fp);
                if (Direction.YN.applyLRDU(pin.apply(front)).toEnumFacing() == pinTo) {
                    SixNodeElement e = ctx.placeSix(damage, p);
                    EnumFacing actual = e.side.applyLRDU(pin.apply(e.front)).toEnumFacing();
                    int mask = e.getConnectionMask(pin.apply(e.front));
                    ctx.check(d.name + " geometry", actual == pinTo && mask != 0,
                        "pin " + actual + " (wanted " + pinTo + "), mask " + mask);
                    return e;
                }
            }
        } finally {
            fp.rotationYaw = oldYaw;
        }
        throw new IllegalStateException("no view puts the pin of " + d.name + " toward " + pinTo);
    }

    static ElectricalLoad pinLoad(SixNodeElement e, UnaryOperator<LRDU> pin) {
        return e.getElectricalLoad(pin.apply(e.front));
    }

    /** Vanilla blocks set by a case; removed (inventory cleared first) at the end of its measure(). */
    static abstract class Base implements SelfTestCase {
        final List<BlockPos> extra = new ArrayList<>();

        void setVanilla(SelfTestContext ctx, BlockPos p, IBlockState s) {
            extra.add(p);
            ctx.world().setBlockState(p, s, 3);
        }

        @Override
        public final void measure(SelfTestContext ctx) {
            try {
                check(ctx);
            } finally {
                WorldServer w = ctx.world();
                for (int i = extra.size() - 1; i >= 0; i--) {
                    BlockPos p = extra.get(i);
                    TileEntity te = w.getTileEntity(p);
                    if (te instanceof TileEntityChest) ((TileEntityChest) te).clear();
                    // platform positions go back to stone; the selftest cleanup removes the platform afterwards
                    w.setBlockState(p, p.getY() == ctx.at(0, 0, 0).getY() ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState(), 3);
                }
            }
        }

        abstract void check(SelfTestContext ctx);
    }

    /**
     * Row (z=1): redstone block | Redstone-to-Voltage | signal cable | Voltage-to-Redstone | (air: probe position)
     * plus a Nuclear Alarm (z=0) and an analog vuMeter (z=2) on the cable.
     * Expected: converter input = 15 (strongest indirect power next to a redstone block) -> output SVU*15/15 = 50 V;
     * the cable, the vuMeter, the alarm and the V-to-R input all at 50 V (gate inputs draw ~no current);
     * V-to-R redstone = round(U*15/SVU) = 15 and the world sees power 15 next to it (NodeBlock.getWeakPower);
     * alarm warm (U > SVU/2) -> light value 7 (descriptor).
     */
    static final class RedstoneChain extends Base {
        ElectricalRedstoneInputElement in;
        SixNodeElement cable;
        ElectricalRedstoneOutputElement out;
        ElectricalAlarmElement alarm;
        ElectricalVuMeterElement vu;

        @Override
        public String name() {
            return "wp9b redstone";
        }

        @Override
        public int width() {
            return 6;
        }

        @Override
        public void build(SelfTestContext ctx) {
            setVanilla(ctx, ctx.at(0, 1, 1), Blocks.REDSTONE_BLOCK.getDefaultState());
            in = (ElectricalRedstoneInputElement) placeToward(ctx, REDSTONE_IN, ctx.at(1, 1, 1), FRONT_RIGHT, EnumFacing.EAST);
            cable = ctx.placeSix(SIGNAL_CABLE, ctx.at(2, 1, 1));
            out = (ElectricalRedstoneOutputElement) placeToward(ctx, REDSTONE_OUT, ctx.at(3, 1, 1), FRONT_RIGHT, EnumFacing.WEST);
            alarm = (ElectricalAlarmElement) placeToward(ctx, NUCLEAR_ALARM, ctx.at(2, 1, 0), FRONT, EnumFacing.SOUTH);
            vu = (ElectricalVuMeterElement) placeToward(ctx, VUMETER, ctx.at(2, 1, 2), FRONT, EnumFacing.NORTH);
        }

        @Override
        void check(SelfTestContext ctx) {
            double svu = Eln.SVU;
            ctx.line("redstone: model U = SVU * level/15, level 15 (redstone block), SVU = " + svu + " V");
            ctx.checkValue("R-to-V output [V] (SVU*15/15)", in.outputGate.getU(), svu);
            ctx.checkValue("signal cable [V]", cable.getElectricalLoad(LRDU.Up).getU(), svu);
            ctx.checkValue("V-to-R input [V]", pinLoad(out, FRONT_RIGHT).getU(), svu);
            ctx.checkValue("vuMeter input [V]", vu.inputGate.getU(), svu);
            ctx.checkValue("alarm input [V]", pinLoad(alarm, FRONT).getU(), svu);
            ctx.check("V-to-R redstone = 15", out.isProvidingWeakPower() == 15, "redstone " + out.isProvidingWeakPower());
            int world = ctx.world().getRedstonePowerFromNeighbors(ctx.at(4, 1, 1));
            ctx.check("world power next to V-to-R = 15", world == 15, "getRedstonePowerFromNeighbors " + world);
            ctx.check("alarm warm -> light 7", alarm.getLightValue() == 7, "light " + alarm.getLightValue());
        }
    }

    /**
     * Scanner A faces stone: opaque cube -> 1.0 -> 50 V. Scanner B faces a chest holding 16 cobblestone (SIMPLE
     * mode, plain IInventory): 16 / 64 / 27 slots = 0.009259 -> 0.463 V. Each output on a signal cable.
     */
    static final class Scanner extends Base {
        ScannerElement a, b;
        SixNodeElement ca, cb;

        @Override
        public String name() {
            return "wp9b scanner";
        }

        @Override
        public void build(SelfTestContext ctx) {
            // pin = front.inverse() toward -X, so the scanner looks at +X
            ca = ctx.placeSix(SIGNAL_CABLE, ctx.at(0, 1, 1));
            a = (ScannerElement) placeToward(ctx, SCANNER, ctx.at(1, 1, 1), FRONT_INVERSE, EnumFacing.WEST);
            setVanilla(ctx, ctx.at(2, 1, 1), Blocks.STONE.getDefaultState());
            cb = ctx.placeSix(SIGNAL_CABLE, ctx.at(4, 1, 1));
            b = (ScannerElement) placeToward(ctx, SCANNER, ctx.at(5, 1, 1), FRONT_INVERSE, EnumFacing.WEST);
            setVanilla(ctx, ctx.at(6, 1, 1), Blocks.CHEST.getDefaultState());
            TileEntity te = ctx.world().getTileEntity(ctx.at(6, 1, 1));
            if (!(te instanceof TileEntityChest)) throw new IllegalStateException("no chest TE");
            ((TileEntityChest) te).setInventorySlotContents(0, new ItemStack(Blocks.COBBLESTONE, 16));
        }

        @Override
        void check(SelfTestContext ctx) {
            double svu = Eln.SVU;
            ctx.check("scanner mode SIMPLE", a.getMode() == ScanMode.SIMPLE && b.getMode() == ScanMode.SIMPLE, "");
            ctx.checkValue("scanner on stone [V] (1.0*SVU)", ca.getElectricalLoad(LRDU.Up).getU(), svu);
            double chest = 16.0 / 64 / 27;
            ctx.checkValue("scanner on chest, 16 items [V] (16/64/27*SVU)", cb.getElectricalLoad(LRDU.Up).getU(), chest * svu);
        }
    }

    /**
     * Voltage probe: source 30 V -> LV cable -> Voltage Probe (LV cable in its slot, range 0..50 V via the GUI packet)
     * -> signal cable. The probe draws no current: U(probe) = 30 V; output = 30/50*SVU = 30 V.
     * Temperature probe: thermal cable item in its slot, a Copper Thermal Cable on its thermal pin, range -50..50 C
     * via the GUI packet; nothing heats: T = 0 (ambient) -> output (0+50)/100*SVU = 25 V.
     */
    static final class Probes extends Base {
        SixNodeElement src, lv, sig1, sig2;
        ElectricalSensorElement probe;
        ThermalSensorElement temp;
        ThermalCableElement tcable;

        @Override
        public String name() {
            return "wp9b probes";
        }

        @Override
        public void build(SelfTestContext ctx) {
            src = ctx.placeSix(SOURCE, ctx.at(0, 1, 1));
            src.networkUnserialize(ctx.stream(out -> {
                out.writeByte(mods.eln.sixnode.electricalsource.ElectricalSourceElement.setVoltageId);
                out.writeFloat(30f);
            }));
            lv = ctx.placeSix(LV_CABLE, ctx.at(1, 1, 1));
            // geometry on the output pin (front, toward +X): the input pin (front.inverse()) only connects once a cable is in the slot
            probe = (ElectricalSensorElement) placeToward(ctx, VOLTAGE_PROBE, ctx.at(2, 1, 1), FRONT, EnumFacing.EAST);
            probe.getInventory().setInventorySlotContents(0, Eln.sixNodeItem.getDescriptor(LV_CABLE).newItemStack());
            probe.getInventory().markDirty();
            probe.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ElectricalSensorElement.setValueId);
                out.writeFloat(0f);
                out.writeFloat(50f);
            }));
            sig1 = ctx.placeSix(SIGNAL_CABLE, ctx.at(3, 1, 1));

            tcable = (ThermalCableElement) ctx.placeSix(THERMAL_CABLE, ctx.at(5, 1, 1));
            temp = (ThermalSensorElement) placeToward(ctx, TEMPERATURE_PROBE, ctx.at(6, 1, 1), FRONT, EnumFacing.EAST);
            temp.getInventory().setInventorySlotContents(0, Eln.sixNodeItem.getDescriptor(THERMAL_CABLE).newItemStack());
            temp.getInventory().markDirty();
            temp.networkUnserialize(ctx.stream(out -> {
                out.writeByte(ThermalSensorElement.setValueId);
                out.writeFloat(-50f);
                out.writeFloat(50f);
            }));
            sig2 = ctx.placeSix(SIGNAL_CABLE, ctx.at(7, 1, 1));
        }

        @Override
        void check(SelfTestContext ctx) {
            double svu = Eln.SVU;
            ctx.checkValue("voltage probe input [V] (source 30 V, no current)", probe.aLoad.getU(), 30.0);
            ctx.checkValue("voltage probe output [V] (30/50*SVU)", sig1.getElectricalLoad(LRDU.Up).getU(), 30.0 / 50.0 * svu);
            double t = temp.thermalLoad.Tc;
            ctx.check("temperature probe T = 0 C (ambient, no heat)", Math.abs(t) < 0.01, String.format("T = %.4f C", t));
            ctx.check("thermal cable T = 0 C", Math.abs(tcable.getThermalLoad(LRDU.Up).Tc) < 0.01, String.format("T = %.4f C", tcable.getThermalLoad(LRDU.Up).Tc));
            ctx.checkValue("temperature probe output [V] ((0+50)/100*SVU)", sig2.getElectricalLoad(LRDU.Up).getU(), 0.5 * svu);
        }
    }

    /**
     * Daylight sensor: output = SVU * light/15 with EA's formula re-evaluated here from the world at measure time
     * (sky light at the node - skylightSubtracted, times cos of the damped celestial angle; 0 without sky light):
     * checks the 1.12 sky API mapping. Weather sensor: 0 V when it is not raining (else > 0, RC tau 3 s).
     * Fire detector: fire on netherrack 3 blocks away, in sight -> firePresent, output rising toward SVU through
     * an RC (tau 0.6 s, first scan after 0.5 s): > SVU/2 after the run. Analog watch: placement only.
     */
    static final class Environment extends Base {
        ElectricalLightSensorElement day;
        ElectricalWeatherSensorElement weather;
        ElectricalFireDetectorElement fire;
        SixNodeElement sDay, sWeather, sFire;
        ElectricalWatchElement watch;

        @Override
        public String name() {
            return "wp9b environment";
        }

        @Override
        public int width() {
            return 12;
        }

        @Override
        public void build(SelfTestContext ctx) {
            day = (ElectricalLightSensorElement) placeToward(ctx, DAYLIGHT_SENSOR, ctx.at(0, 1, 1), FRONT_RIGHT, EnumFacing.SOUTH);
            sDay = ctx.placeSix(SIGNAL_CABLE, ctx.at(0, 1, 2));
            weather = (ElectricalWeatherSensorElement) placeToward(ctx, WEATHER_SENSOR, ctx.at(2, 1, 1), FRONT_RIGHT, EnumFacing.SOUTH);
            sWeather = ctx.placeSix(SIGNAL_CABLE, ctx.at(2, 1, 2));
            watch = (ElectricalWatchElement) ctx.placeSix(ANALOG_WATCH, ctx.at(4, 1, 1));
            fire = (ElectricalFireDetectorElement) placeToward(ctx, FIRE_DETECTOR, ctx.at(6, 1, 1), FRONT_RIGHT, EnumFacing.SOUTH);
            sFire = ctx.placeSix(SIGNAL_CABLE, ctx.at(6, 1, 2));
            setVanilla(ctx, ctx.at(9, 0, 1), Blocks.NETHERRACK.getDefaultState());
            setVanilla(ctx, ctx.at(9, 1, 1), Blocks.FIRE.getDefaultState());
        }

        @Override
        void check(SelfTestContext ctx) {
            double svu = Eln.SVU;
            WorldServer w = ctx.world();
            BlockPos p = ctx.at(0, 1, 1);
            int light = 0;
            if (w.provider.hasSkyLight()) {
                int i1 = WorldCompat.getSavedLightValue(w, EnumSkyBlock.SKY, p.getX(), p.getY(), p.getZ()) - w.getSkylightSubtracted();
                i1 = Math.max(0, i1);
                float f = w.getCelestialAngleRadians(1.0F);
                if (f < (float) Math.PI) f += (0.0F - f) * 0.2F;
                else f += (((float) Math.PI * 2F) - f) * 0.2F;
                light = MathHelper.clamp(Math.round((float) i1 * MathHelper.cos(f)), 0, 15);
            }
            ctx.line("daylight: sky light " + WorldCompat.getSavedLightValue(w, EnumSkyBlock.SKY, p.getX(), p.getY(), p.getZ())
                + ", skylightSubtracted " + w.getSkylightSubtracted() + ", world time " + w.getWorldTime() + " -> level " + light);
            ctx.checkValue("daylight sensor [V] (SVU*level/15)", sDay.getElectricalLoad(LRDU.Up).getU(), svu * light / 15.0);
            double uw = sWeather.getElectricalLoad(LRDU.Up).getU();
            if (w.isRaining()) ctx.check("weather sensor > 0 V (raining)", uw > 0, String.format("U = %.3f V", uw));
            else ctx.check("weather sensor 0 V (clear)", Math.abs(uw) < 1e-3, String.format("U = %.4f V", uw));
            ctx.check("watch placed", watch != null, "");
            ctx.check("fire detector sees the fire", fire.firePresent, "fire block " + w.getBlockState(ctx.at(9, 1, 1)).getBlock());
            double uf = sFire.getElectricalLoad(LRDU.Up).getU();
            ctx.check("fire detector output > SVU/2 (RC step)", uf > svu / 2 && uf <= svu * 1.01, String.format("U = %.3f V", uf));
        }
    }
}
