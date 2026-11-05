package mods.eln.robotics.servo;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.CablePosition;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.robotics.motion.Drive;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The Linear Servo Drive (Baughn: the servo layer first, 200 V only): a motor and a drum at the foot of a Servo Rail
 * mast, which moves an elevator car up and down the mast beside it. Its front faces the car's shaft; the rail stands on
 * its top. Power on the back (200 V). Signals on the flanks: floor cables are inputs, ceiling cables outputs; seen from
 * the car's side (front.left()), the left flank has the setpoint (in) and in-position (out), the right flank enable (in)
 * and fault (out). Its electrics are a Machine API spec (spec()). Model: mods.eln.cad.models.ServoParts.
 */
public class LinearServoDriveDescriptor extends TransparentNodeDescriptor implements mods.eln.introspect.DescribableSpec {
    /** Rail length it drives, in blocks (the car's travel). */
    public static final int MIN_TRAVEL = 2, MAX_TRAVEL = 32;
    /**
     * The motor: 2 m/s at 200 V (slower on a sagging line, holding below 30 %), 1 m/s^2, 40 W idle, 80 % efficient,
     * rated for 120 kg net at full speed (about 3 kW). A counterweight balances the car and counterweightKg of load,
     * so an empty car going down lifts that much.
     */
    public static Drive drive = new Drive(Eln.MVU, 2.0, 1.0, 40, 0.8, 120);
    public static double counterweightKg = 40, playerKg = 80, itemKg = 2;
    /**
     * Its motor's heat (the old drive's): capacity (J/K), loss to ambient (W/K), the trip 80 K over its ambient (100 C
     * at 20 C, 170 C in the Nether; idle it sits 10 K over) and its reset 30 K under it (the old drive resumed at 50 K
     * over ambient).
     */
    public static final double HEAT_CAPACITY_J_PER_K = 4000, HEAT_LOSS_W_PER_K = 4, OVERHEAT_RISE_K = 80, OVERHEAT_RESET_K = 30;
    /**
     * A move's top speed follows the line when its limit differs by this share, at most every RETARGET_TICKS; it
     * speeds up only when that costs nothing or the machine's buffer is SPARE_SHARE full (energy piling up), so a
     * starved supply doesn't make it surge and fall back.
     */
    public static final double RETARGET_SHARE = 0.1, SPARE_SHARE = 0.75;
    public static final int RETARGET_TICKS = 10;
    /** Its signals (Machine API names). */
    public static final String SETPOINT = "setpoint", ENABLE = "enable", IN_POSITION = "in_position", FAULT = "fault",
        POSITION = "position";
    final Obj3D obj;

    public LinearServoDriveDescriptor(String name, String model) {
        super(name, LinearServoDriveElement.class, LinearServoDriveRender.class);
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor);
    }

    /**
     * Its electrics on the Machine API: a consumer at the drive's volts, rated at its idle plus its rated lift.
     * Brownout.SLOW: it runs on whatever its line gives, the energy rule making it slower (Baughn: a sagging line makes
     * the axis slow), and the API never pulls a weak line past its maximum power; the drive itself holds below the
     * stall share (with a band: LinearServoDriveElement.step). Brownout.STOP would stop it whenever the supply can't
     * carry its ask, a stop-and-go car on any weak line. Overvoltage trips rather than burns (a servo amplifier's
     * protection; the car holds and faults). Power on the back. Every pin is on a flank's floor edge (Baughn
     * 2026-11-05: a ceiling pin can't be wired, the rail is on top; several pins per edge, as the displays): the left
     * flank has the setpoint in the middle, in-position on its left and position on its right; the right flank enable
     * in the middle, fault on its left and the RS485 bus on its right (left and right as seen facing the flank). A
     * plain cable meets only the middles.
     */
    public static MachineSpec spec() {
        return MachineSpec.consumer(drive.nominalVolts()).ratedWatts(drive.ratedWatts()).idleWatts(drive.idleWatts())
            .onBrownout(Brownout.SLOW)
            .heatCapacityJPerK(HEAT_CAPACITY_J_PER_K).heatLossWPerK(HEAT_LOSS_W_PER_K).overheatRiseKelvin(OVERHEAT_RISE_K)
            .overheatResetKelvin(OVERHEAT_RESET_K)
            .power(RelativeFace.BACK)
            .signalIn(SETPOINT, RelativeFace.LEFT, CablePosition.FLOOR, 0)
            .signalOut(IN_POSITION, RelativeFace.LEFT, CablePosition.FLOOR_LEFT).signalOut(POSITION, RelativeFace.LEFT, CablePosition.FLOOR_RIGHT)
            .signalIn(ENABLE, RelativeFace.RIGHT, CablePosition.FLOOR, 1).signalOut(FAULT, RelativeFace.RIGHT, CablePosition.FLOOR_LEFT)
            .bus(RelativeFace.RIGHT, CablePosition.FLOOR_RIGHT)
            .build();
    }

    /** Acceleration (m/tick^2). */
    public static double accelPerTick() {
        return drive.accel() / 400;
    }

    void draw(byte status) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        var lamp = obj.getPart("lamp");
        if (lamp == null) return;
        // the status lamp: green in position, amber moving, red on a fault, dark unpowered
        switch (status) {
            case LinearServoDriveElement.IN_POSITION -> mods.eln.gl.GL11.glColor4f(0.3f, 1f, 0.3f, 1f);
            case LinearServoDriveElement.MOVING -> mods.eln.gl.GL11.glColor4f(1f, 0.7f, 0.1f, 1f);
            case LinearServoDriveElement.FAULT -> mods.eln.gl.GL11.glColor4f(1f, 0.15f, 0.1f, 1f);
            default -> mods.eln.gl.GL11.glColor4f(0.25f, 0.25f, 0.25f, 1f);
        }
        lamp.draw();
        mods.eln.gl.GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(LinearServoDriveElement.IDLE);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Moves an elevator car along a Servo Rail\nset on its top, %1$ to %2$ blocks long.\nThe car rides on its front side.\n200V power on the back.\nThree pins on each flank's floor edge\n(left to right, facing the flank):\nleft flank (seen from the car): in position,\nsetpoint, position; right flank: fault,\nenable, RS485 bus. Plain cables reach the middles.\nDriven over the bus by its registers:\n0 target, 1 position, 2 speed limit, 3 state, 4 load.",
            MIN_TRAVEL, MAX_TRAVEL).split("\n"));
    }

    @Override
    public void describeSpec(mods.eln.introspect.Section spec) {
        spec.num(mods.eln.introspect.Keys.NOMINAL_VOLTAGE, drive.nominalVolts(), mods.eln.introspect.Unit.V);
        spec.num(mods.eln.introspect.Keys.MAX_SPEED, drive.vmaxNominal(), mods.eln.introspect.Unit.M_PER_S);
        spec.num(mods.eln.introspect.Keys.ACCELERATION, drive.accel(), mods.eln.introspect.Unit.M_PER_S2);
        spec.num(mods.eln.introspect.Keys.IDLE_POWER, drive.idleWatts(), mods.eln.introspect.Unit.W);
        spec.num(mods.eln.introspect.Keys.RATED_POWER, drive.ratedWatts(), mods.eln.introspect.Unit.W);
        spec.num("stall_share", Drive.STALL_SHARE, mods.eln.introspect.Unit.RATIO);   // (the drive's own hold, not a machine floor: it is Brownout.SLOW)
        spec.num(mods.eln.introspect.Keys.EFFICIENCY, drive.efficiency(), mods.eln.introspect.Unit.RATIO);
        spec.integer(mods.eln.introspect.Keys.TRAVEL_MIN, MIN_TRAVEL);
        spec.integer(mods.eln.introspect.Keys.TRAVEL_MAX, MAX_TRAVEL);
        spec.num("counterweight", counterweightKg, mods.eln.introspect.Unit.KG);
    }
}
