package mods.eln.oil;

import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.function.DoubleSupplier;

/**
 * A steam turbine's controls (Baughn 2026-10-26: a screen as the heat furnace's), shared by the 50V/200V Steam Turbines
 * and the shaft Steam Turbine (Baughn 2026-10-29): a regulator slot; internal control sets the throttle (the most the
 * steam valve may open) or, with a regulator, a target inlet pressure the regulator holds by closing the valve when the
 * inlet falls below it (so the boiler keeps its pressure); external control: a signal (0-50 V: 0-100%) sets the
 * throttle. The turbine's speed governor works within that limit. The screen is SteamGeneratorScreen, through
 * {@link Render}.
 */
public final class SteamValveControl {
    public static final int REGULATOR_SLOT = 0;
    public static final double TARGET_MIN_BAR = 1;
    public static final byte TOGGLE_EXTERNAL = 1, SET_THROTTLE = 2, SET_TARGET = 3;

    final TransparentNodeElement element;
    public final TransparentNodeElementInventory inventory;
    public final NbtElectricalGateInput control = new NbtElectricalGateInput("control");
    /** The inlet pressure (Pa) the regulator watches. */
    final DoubleSupplier inletPa;
    public final double targetMaxBar;
    public boolean controlExternal;
    /** Internal throttle (0..1), target inlet pressure (bar), and the valve's limit now (0..1). */
    public double throttle = 1, targetBar, limit = 1;
    double regulated = 1;
    /** The pressure regulator: its hit is minus the inlet pressure (bar), so it opens the valve as the pressure rises. */
    public final mods.eln.sim.RegulatorProcess pressure = new mods.eln.sim.RegulatorProcess("pressure") {
        @Override
        protected double getHit() {
            return -inletPa.getAsDouble() / 1e5;
        }

        @Override
        protected void setCmd(double cmd) {
            regulated = Math.max(0, Math.min(1, cmd));
        }
    };

    public SteamValveControl(TransparentNodeElement element, DoubleSupplier inletPa, double defaultTargetBar, double targetMaxBar) {
        this.element = element;
        this.inletPa = inletPa;
        this.targetBar = defaultTargetBar;
        this.targetMaxBar = targetMaxBar;
        inventory = new TransparentNodeElementInventory(1, 1, element);
    }

    public boolean hasRegulator() {
        return !Utils.isEmpty(inventory.getItem(REGULATOR_SLOT));
    }

    /** The regulator item in the slot sets the regulator's kind (as the heat furnace's; working point 10 bar). */
    public void computeInventory() {
        var stack = inventory.getItem(REGULATOR_SLOT);
        if (!Utils.isEmpty(stack) && Utils.getItemObject(stack) instanceof mods.eln.item.regulator.IRegulatorDescriptor r)
            r.applyTo(pressure, 10.0, 10.0, 2.0, 0.1);
        else pressure.setNone();
        pressure.setTarget(-targetBar);
    }

    /** The most the steam valve may open now (and remember it for the screen and meters). */
    public double limit() {
        limit = controlExternal ? control.getNormalized() : hasRegulator() ? regulated : throttle;
        return limit;
    }

    /** A screen event; true if it was one of these (the element then publishes). */
    public boolean unserialize(byte type, DataInputStream stream) throws IOException {
        switch (type) {
            case TOGGLE_EXTERNAL -> controlExternal = !controlExternal;
            case SET_THROTTLE -> throttle = Math.max(0, Math.min(1, stream.readFloat()));
            case SET_TARGET -> {
                targetBar = Math.max(TARGET_MIN_BAR, Math.min(targetMaxBar, stream.readFloat()));
                pressure.setTarget(-targetBar);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** For the screen: external, throttle, target, limit, inlet bar (Render.read reads them back). */
    public void serialize(DataOutputStream stream) throws IOException {
        stream.writeBoolean(controlExternal);
        stream.writeFloat((float) throttle);
        stream.writeFloat((float) targetBar);
        stream.writeFloat((float) limit);
        stream.writeFloat((float) (inletPa.getAsDouble() / 1e5));
    }

    public void writeToNBT(CompoundTag nbt) {
        nbt.putBoolean("controlExternal", controlExternal);
        nbt.putDouble("throttle", throttle);
        nbt.putDouble("targetBar", targetBar);
        pressure.writeToNBT(nbt, "pressure");
    }

    public void readFromNBT(CompoundTag nbt, double defaultTargetBar) {
        controlExternal = nbt.getBooleanOr("controlExternal", false);
        throttle = nbt.getDoubleOr("throttle", 1);
        targetBar = nbt.getDoubleOr("targetBar", defaultTargetBar);
        pressure.readFromNBT(nbt, "pressure");
    }

    /** A render that holds the controls' client side (what SteamGeneratorScreen needs). */
    public interface Rendered {
        Render controls();
    }

    /** The client side of the controls (a turbine's render), for SteamGeneratorScreen. */
    public static class Render {
        public boolean controlExternal, throttleNew, targetNew;
        public float throttle = -1, targetBar = -1, limit, inletBar;
        public final float targetMaxBar;
        final mods.eln.node.transparent.TransparentNodeElementRender render;

        public Render(mods.eln.node.transparent.TransparentNodeElementRender render, double targetMaxBar) {
            this.render = render;
            this.targetMaxBar = (float) targetMaxBar;
        }

        public void read(DataInputStream stream) throws IOException {
            controlExternal = stream.readBoolean();
            float t = stream.readFloat(), p = stream.readFloat();
            if (t != throttle) {
                throttle = t;
                throttleNew = true;
            }
            if (p != targetBar) {
                targetBar = p;
                targetNew = true;
            }
            limit = stream.readFloat();
            inletBar = stream.readFloat();
        }

        public void clientToggleControl() {
            render.clientSendId(TOGGLE_EXTERNAL);
        }

        public void clientSetThrottle(float v) {
            render.clientSendFloat(SET_THROTTLE, v);
        }

        public void clientSetTarget(float v) {
            render.clientSendFloat(SET_TARGET, v);
        }
    }
}
