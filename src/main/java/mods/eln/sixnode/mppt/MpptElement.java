package mods.eln.sixnode.mppt;

import mods.eln.Eln;
import mods.eln.api.Meters;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.network.ClientValues;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.BatteryProcess;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.MpptTracker;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.BuckConverterProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.DifferentialVoltageWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The MPPT charge controller (board #79). Four terminals, as the real thing, since EA's panels and batteries float:
 * panel + on the top edge (front), battery + on the bottom; panel - on the player's left, battery - on the player's
 * right (the element's own right and left). Inside, a
 * {@link BuckConverterProcess} between the two ports and a {@link MpptTracker} setting its ratio each electrical step;
 * the battery's charge voltage (a screen slider) is the converter's output limit. Above its rated power it moves the
 * panels off their peak (up the curve, within the converter's solve); beyond its input rating across the panel
 * terminals it burns (a watchdog).
 * <p>
 * Wired backwards it does nothing: the converter only passes power forward, from a positive input.
 */
public class MpptElement extends SixNodeElement {
    final MpptDescriptor descriptor;
    final NbtElectricalLoad pvPos = new NbtElectricalLoad("pvPos"), pvNeg = new NbtElectricalLoad("pvNeg");
    final NbtElectricalLoad batPos = new NbtElectricalLoad("batPos"), batNeg = new NbtElectricalLoad("batNeg");
    final VoltageSource pvPosSrc = new VoltageSource("pvPosSrc", pvPos, null), pvNegSrc = new VoltageSource("pvNegSrc", pvNeg, null);
    final VoltageSource batPosSrc = new VoltageSource("batPosSrc", batPos, null), batNegSrc = new VoltageSource("batNegSrc", batNeg, null);
    final BuckConverterProcess conv = new BuckConverterProcess(pvPos, pvNeg, pvPosSrc, pvNegSrc, batPos, batNeg, batPosSrc, batNegSrc);
    final MpptTracker tracker;
    private boolean processIn;

    /** The battery's charge voltage (V): a 48 V lead-acid bank's absorption voltage to start with. */
    float chargeVolts = (float) (Eln.LVU * BatteryProcess.ABSORPTION);
    public static final byte setChargeVoltsId = 1, setChargeAmpsId = 2;
    /** The most charging current (A), as a real controller's setting: a small bank takes far less than the panels give
     *  (AdventurAgent: a 250 W battery cooked at 470 W). 5 A to start: a 48 V bank's 250 W. */
    float chargeAmps = 5;
    public static final float MIN_CHARGE_AMPS = 0.5f, MAX_CHARGE_AMPS = 400;
    public static final float MIN_CHARGE_VOLTS = 5;

    enum State {OFF, TRACKING, CHARGED, POWER_LIMIT, BATTERY_HIGH, PANELS_LOW, NO_BATTERY, CURRENT_LIMIT}

    State state = State.OFF;
    private double publishIn = Math.random();
    /** The panels' voltage across its input within its rating (the open-circuit voltage counts: rate strings cold). */
    final DifferentialVoltageWatchDog inputWatchdog = new DifferentialVoltageWatchDog();

    public MpptElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (MpptDescriptor) descriptor;
        this.tracker = this.descriptor.newTracker();
        electricalLoadList.add(pvPos);
        electricalLoadList.add(pvNeg);
        electricalLoadList.add(batPos);
        electricalLoadList.add(batNeg);
        electricalComponentList.add(pvPosSrc);
        electricalComponentList.add(pvNegSrc);
        electricalComponentList.add(batPosSrc);
        electricalComponentList.add(batNegSrc);
        electricalProcessList.add(this::trackStep);
        slowProcessList.add(this::slowStep);
        slowProcessList.add(inputWatchdog.set(pvPos, pvNeg).setUMax(this.descriptor.maxInputVolts).set(new Failure(this).electronics()));
    }

    /** Each electrical step: the tracker's next ratio (it also holds the rated power). */
    private void trackStep(double dt) {
        // the charging current as an input power limit, from the battery side's voltage now (the converter applies it
        // within its solve, as its rating)
        double uBat = Math.max(conv.getOutputU(), 1);
        double ampsLimit = chargeAmps * uBat / descriptor.efficiency;
        conv.setPowerLimit(Math.min(descriptor.maxPower, ampsLimit));
        boolean byCurrent = ampsLimit < descriptor.maxPower;
        conv.setRatio(tracker.next(conv.getInputU(), conv.getInputI(), conv.getOutputU(), conv.getRatio(), conv.getRatioUsed(),
            conv.isLimited() || conv.isPowerLimited(), conv.isFlowing(), dt));
        double uIn = conv.getInputU(), uOut = conv.getOutputU();
        if (conv.isFlowing()) state = conv.isLimited() ? State.CHARGED : conv.isPowerLimited() ? (byCurrent ? State.CURRENT_LIMIT : State.POWER_LIMIT) : State.TRACKING;
        else if (uIn < 1) state = State.OFF;
        else if (Math.abs(uOut) < 1) state = State.NO_BATTERY;   // (lit panels, nothing on the battery side)
        // lit panels but nothing flows: the battery side above the charge voltage (a bank of more cells than it is set
        // for; resting at it is "at charge voltage"), or the panels below the battery (it only steps down)
        else if (uOut > chargeVolts * 1.002) state = State.BATTERY_HIGH;
        else if (uOut >= chargeVolts * 0.998) state = State.CHARGED;
        // (below by a margin: after dusk the dark input can sit at the battery's voltage, as both ends of the cable follow
        // each other; that reads "No input")
        else if (uIn < uOut * 0.99) state = State.PANELS_LOW;
        // nothing on the battery side (a cable off: the converter sees no path there); else, after dusk, a dark input
        // that sits at the battery's voltage (both ends of the cable follow each other): "No input"
        else state = conv.isOutputFloating() ? State.NO_BATTERY : State.OFF;
    }

    /** Each tick: the screen's readings, twice a second. */
    private void slowStep(double dt) {
        publishIn -= dt;
        if (publishIn < 0) {
            publishIn += 0.5;
            needPublish();
        }
    }

    @Override
    public void connectJob() {
        if (!sixNode.isDestructing() && !processIn) {
            Eln.simulator.mna.addProcess(conv);
            processIn = true;
        }
        super.connectJob();
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        if (processIn) {
            Eln.simulator.mna.removeProcess(conv);
            processIn = false;
        }
    }

    @Override
    public void initialize() {
        conv.setEfficiency(descriptor.efficiency);
        conv.setOutputLimit(chargeVolts);
        conv.setPowerLimit(descriptor.maxPower);
        // the terminals' own resistance, as a short 230 V cable's
        for (NbtElectricalLoad l : new NbtElectricalLoad[]{pvPos, pvNeg, batPos, batNeg})
            ElnDeviceRegistry.meduimVoltageCableDescriptor.applyTo(l);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front) return pvPos;
        // (a wall element's left and right are its own, mirrored for the player facing it: AdventurAgent found the
        // minus pins swapped against the tooltip, which names them as the player sees them)
        if (lrdu == front.right()) return pvNeg;
        if (lrdu == front.inverse()) return batPos;
        if (lrdu == front.left()) return batNeg;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return getElectricalLoad(lrdu) != null ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("PV:", conv.getInputU()) + Utils.plotAmpere("", conv.getInputI())
            + Utils.plotVolt("Bat:", conv.getOutputU()) + Utils.plotAmpere("", conv.getOutputI());
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    /** Output over input power (the tier's). */
    public double efficiency() {
        return descriptor.efficiency;
    }

    /** The state as players read it (Jade, screen). */
    static String stateText(int ordinal) {
        State[] all = State.values();
        return switch (ordinal >= 0 && ordinal < all.length ? all[ordinal] : State.OFF) {
            case OFF -> I18N.tr("No input");
            case TRACKING -> I18N.tr("Tracking");
            case CHARGED -> I18N.tr("At charge voltage");
            case POWER_LIMIT -> I18N.tr("At rated power");
            case BATTERY_HIGH -> I18N.tr("Battery above charge voltage");
            case PANELS_LOW -> I18N.tr("Panels below battery voltage");
            case NO_BATTERY -> I18N.tr("No battery");
            case CURRENT_LIMIT -> I18N.tr("At charge current");
        };
    }

    @Override
    public Map<String, String> getWaila() {
        return mods.eln.compat.jade.ProbeLines.of()
            .headline(I18N.tr("Panels"), Meters.watts(conv.getInputP()) + " @ " + Meters.volts(conv.getInputU()))
            .headline(I18N.tr("Battery"), Meters.watts(conv.getOutputP()) + " @ " + Meters.volts(conv.getOutputU()))
            .headline(I18N.tr("State"), stateText(state.ordinal()))
            .detail(I18N.tr("Charge voltage"), Meters.volts(chargeVolts))
            .detail(I18N.tr("Charge current"), Meters.amps(chargeAmps));
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat(chargeVolts);
            stream.writeFloat(chargeAmps);
            stream.writeFloat((float) conv.getInputU());
            stream.writeFloat((float) conv.getInputI());
            stream.writeFloat((float) conv.getOutputU());
            stream.writeFloat((float) conv.getOutputI());
            stream.writeByte(state.ordinal());
            stream.writeBoolean(conv.isFlowing());   // (the display: lit while it converts)
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            byte id = stream.readByte();
            if (id == setChargeVoltsId) {
                chargeVolts = ClientValues.clamp(stream.readFloat(), MIN_CHARGE_VOLTS, (float) descriptor.maxInputVolts, chargeVolts);
                conv.setOutputLimit(chargeVolts);
                needPublish();
            } else if (id == setChargeAmpsId) {
                chargeAmps = ClientValues.clamp(stream.readFloat(), MIN_CHARGE_AMPS, MAX_CHARGE_AMPS, chargeAmps);
                needPublish();
            }
        } catch (IOException e) {
            // a short packet: nothing changes
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putFloat("chargeVolts", chargeVolts);
        nbt.putFloat("chargeAmps", chargeAmps);
        nbt.putDouble("ratio", conv.getRatio());
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        chargeVolts = (float) ClientValues.load(nbt.getFloatOr("chargeVolts", chargeVolts), MIN_CHARGE_VOLTS, descriptor.maxInputVolts, chargeVolts);
        chargeAmps = (float) ClientValues.load(nbt.getFloatOr("chargeAmps", chargeAmps), MIN_CHARGE_AMPS, MAX_CHARGE_AMPS, chargeAmps);
        conv.setRatio(nbt.getDoubleOr("ratio", 0.5));
    }
}
