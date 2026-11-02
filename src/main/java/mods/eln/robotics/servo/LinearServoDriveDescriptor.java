package mods.eln.robotics.servo;

import mods.eln.Eln;
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
 * and fault (out). Model: mods.eln.cad.models.ServoParts.
 */
public class LinearServoDriveDescriptor extends TransparentNodeDescriptor implements mods.eln.introspect.DescribableSpec {
    /** Rail length it drives, in blocks (the car's travel). */
    public static final int MIN_TRAVEL = 2, MAX_TRAVEL = 32;
    /**
     * The motor: 2 m/s at 200 V (slower on a sagging line, stalled below 30 %), 1 m/s^2, 40 W idle, 80 % efficient. A
     * counterweight balances the car and COUNTERWEIGHT_KG of load, so an empty car going down lifts that much.
     */
    public static Drive drive = new Drive(Eln.MVU, 2.0, 1.0, 40, 0.8);
    public static double counterweightKg = 40, playerKg = 80, itemKg = 2;
    public static final double G = 9.81;
    /** Its motor's heat: capacity (J/K), to ambient (K/W); it faults this hot (K over ambient) and resumes at RESUME_K. */
    public static final double HEAT_C = 4000, HEAT_RP = 0.25, WARM_K = 80, RESUME_K = 50;
    /** A sagging line retargets the move to its new top speed when that differs by this share, at most every RETARGET_TICKS. */
    public static final double RETARGET_SHARE = 0.1;
    public static final int RETARGET_TICKS = 10;
    final Obj3D obj;

    public LinearServoDriveDescriptor(String name, String model) {
        super(name, LinearServoDriveElement.class, LinearServoDriveRender.class);
        obj = Eln.obj.getObj(model);
        voltageLevelColor = ElectricalCableDescriptor.voltageLevelColorOf(mods.eln.registry.ElnDeviceRegistry.meduimVoltageCableDescriptor);
    }

    /** Top speed (m/tick) at this line voltage. */
    public static double vmaxPerTick(double volts) {
        return drive.vmaxAt(volts) / 20;
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
        Collections.addAll(list, tr("Moves an elevator car along a Servo Rail\nset on its top, %1$ to %2$ blocks long.\nThe car rides on its front side.\n200V power on the back.\nSeen from the car's side, left flank:\nsetpoint in on a floor cable,\nin position out on a ceiling cable.\nRight flank: enable in on a floor cable,\nfault out on a ceiling cable.",
            MIN_TRAVEL, MAX_TRAVEL).split("\n"));
    }

    @Override
    public void describeSpec(mods.eln.introspect.Section spec) {
        spec.num(mods.eln.introspect.Keys.NOMINAL_VOLTAGE, drive.nominalVolts(), mods.eln.introspect.Unit.V);
        spec.num(mods.eln.introspect.Keys.MAX_SPEED, drive.vmaxNominal(), mods.eln.introspect.Unit.M_PER_S);
        spec.num(mods.eln.introspect.Keys.ACCELERATION, drive.accel(), mods.eln.introspect.Unit.M_PER_S2);
        spec.num(mods.eln.introspect.Keys.IDLE_POWER, drive.idleWatts(), mods.eln.introspect.Unit.W);
        spec.num(mods.eln.introspect.Keys.EFFICIENCY, drive.efficiency(), mods.eln.introspect.Unit.RATIO);
        spec.integer(mods.eln.introspect.Keys.TRAVEL_MIN, MIN_TRAVEL);
        spec.integer(mods.eln.introspect.Keys.TRAVEL_MAX, MAX_TRAVEL);
        spec.num("counterweight", counterweightKg, mods.eln.introspect.Unit.KG);
    }
}
