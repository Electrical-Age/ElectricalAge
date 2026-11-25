package mods.eln.gametest.cases;

import mods.eln.Eln;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.PvCurve;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.BuckConverterProcess;
import mods.eln.sim.mna.process.PvSourceBipole;
import mods.eln.sixnode.mppt.MpptElement;
import mods.eln.transparentnode.solarpanel.SolarPanelElement;

import java.util.ArrayList;
import java.util.List;

/**
 * MPPT charge controllers (Baughn 2026-11-25, board #79): a 2x3 panel at noon, a few blocks of cable (resistors) to
 * the controller's panel terminals, and on its battery terminals a stiff battery or a resistor. Rows: each tier on a
 * 48 V battery charged to 57.6 V (tracks the panel's peak, delivers it minus its efficiency); the plain one on a
 * resistor with a 60 V charge voltage (holds the output there); the plain one left at its default (48 V bank) on the
 * 96 V battery (passes nothing and says the battery is above the charge voltage); the advanced one on the 48 V battery
 * at its default 5 A charging current (holds it).
 */
public final class MpptCases {
    private MpptCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Row("mppt_advanced_battery", "Advanced MPPT Charge Controller", 48, 57.6, 0.97 * 0.97));
        cases.add(new Row("mppt_plain_battery", "MPPT Charge Controller", 48, 57.6, 0.94 * 0.93));
        cases.add(new Row("mppt_plain_charge_limit", "MPPT Charge Controller", 0, 60, 0));
        cases.add(new Row("mppt_default_on_96v", "MPPT Charge Controller", 96, 0, 0));
        cases.add(new Row("mppt_default_charge_current", "Advanced MPPT Charge Controller", 48, 57.6, -1));
        cases.add(new BurnCase(true));
        cases.add(new BurnCase(false));
    }

    /** batteryV > 0: a battery, else a 40 ohm resistor; chargeV: the charge voltage set. minShare: of the panel's peak. */
    static final class Row implements TestCase {
        final String id, device;
        final double batteryV, chargeV, minShare;
        SolarPanelElement panel;
        MpptElement mppt;
        final List<Component> parts = new ArrayList<>();
        final List<ElectricalLoad> loads = new ArrayList<>();

        Row(String id, String device, double batteryV, double chargeV, double minShare) {
            this.id = id;
            this.device = device;
            this.batteryV = batteryV;
            this.chargeV = chargeV;
            this.minShare = minShare;
        }

        public String id() {
            return id;
        }

        public int width() {
            return 8;
        }

        public int ticks() {
            return 240;
        }

        public void build(TestContext ctx) {
            var server = ctx.level().getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set noon");
            server.setWeatherParameters(100000, 0, false, false);
            panel = (SolarPanelElement) ctx.placeTransparent(Ids.transparent("2x3 Solar Panel"), ctx.at(3, 1, 1));
            mppt = (MpptElement) ctx.placeSix(Ids.six(device), ctx.at(6, 1, 1));
            parts.add(new Resistor(TestContext.field(panel, "positiveLoad"), TestContext.field(mppt, "pvPos")).setR(0.05));
            parts.add(new Resistor(TestContext.field(panel, "negativeLoad"), TestContext.field(mppt, "pvNeg")).setR(0.05));
            parts.add(new Resistor(TestContext.field(mppt, "batNeg"), null).setR(1e-3));
            if (batteryV > 0) {
                ElectricalLoad bat = new ElectricalLoad();
                bat.setRs(1e-3);
                loads.add(bat);
                parts.add(new VoltageSource("test battery", bat, null).setU(batteryV));
                parts.add(new Resistor(TestContext.field(mppt, "batPos"), bat).setR(0.02));
            } else {
                parts.add(new Resistor(TestContext.field(mppt, "batPos"), TestContext.field(mppt, "batNeg")).setR(40));
            }
            // the charge voltage, as the screen sends it (0: left at its default, a 48 V bank's)
            if (chargeV > 0) mppt.networkUnserialize(ctx.stream(o -> {
                o.writeByte(MpptElement.setChargeVoltsId);
                o.writeFloat((float) chargeV);
            }));
            // the charging current: left at its 5 A default (minShare -1), else opened up to 50 A
            if (minShare >= 0) mppt.networkUnserialize(ctx.stream(o -> {
                o.writeByte(MpptElement.setChargeAmpsId);
                o.writeFloat(50f);
            }));
            for (ElectricalLoad l : loads) Eln.simulator.addElectricalLoad(l);
            for (Component c : parts) Eln.simulator.addElectricalComponent(c);
        }

        public void measure(TestContext ctx) {
            BuckConverterProcess conv = TestContext.field(mppt, "conv");
            PvSourceBipole pv = TestContext.field(panel, "pv");
            PvCurve curve = pv.curve();
            double peak = curve.maxPowerPoint()[1];
            String got = String.format("panel %.1f W at %.1f V (peak %.1f W at %.1f V), out %.1f W at %.1f V, ratio %.3f, %s",
                pv.getP(), pv.getU(), peak, curve.maxPowerPoint()[0], conv.getOutputP(), conv.getOutputU(), conv.getRatio(),
                mppt.getWaila().get(mods.eln.i18n.I18N.tr("State")));
            if (minShare < 0) {
                ctx.check(id + ": the default 5 A charging current holds", conv.isFlowing() && conv.getOutputI() <= 5.05 && conv.getOutputI() > 4.5, got);
            } else if (chargeV <= 0) {
                ctx.check(id + ": a bank above the default charge voltage: nothing flows, and it says why",
                    !conv.isFlowing() && mods.eln.i18n.I18N.tr("Battery above charge voltage").equals(mppt.getWaila().get(mods.eln.i18n.I18N.tr("State"))), got);
            } else if (batteryV > 0) {
                ctx.check(id + ": tracks the panel's peak, minus its efficiency", conv.getOutputP() >= minShare * peak && peak > 100, got);
                ctx.check(id + ": power balances (out = efficiency x in)",
                    Math.abs(conv.getOutputP() - mppt.efficiency() * conv.getInputP()) <= 0.01 * conv.getOutputP(), got);
            } else {
                ctx.check(id + ": holds the output at the charge voltage", conv.isLimited() && conv.getOutputU() <= chargeV * 1.01
                    && conv.getOutputU() >= chargeV * 0.95, got);
            }
            for (Component c : parts) Eln.simulator.removeElectricalComponent(c);
            for (ElectricalLoad l : loads) Eln.simulator.removeElectricalLoad(l);
        }
    }

    /**
     * What burns: the plain controller with 450 V across its panel terminals (rated 350 V open circuit), and a small
     * panel with a 48 V battery across it backwards (its bypass and terminals heat it past 150 C).
     */
    static final class BurnCase implements TestCase {
        final boolean controller;
        mods.eln.node.six.SixNodeElement mppt;
        SolarPanelElement panel;
        final List<Component> parts = new ArrayList<>();
        final ElectricalLoad x = new ElectricalLoad();

        BurnCase(boolean controller) {
            this.controller = controller;
        }

        public String id() {
            return controller ? "mppt_input_overvoltage_burns" : "solar_reversed_battery_burns";
        }

        public int ticks() {
            return 240;
        }

        public void build(TestContext ctx) {
            x.setRs(1e-3);
            ElectricalLoad pos, neg;
            if (controller) {
                mppt = ctx.placeSix(Ids.six("MPPT Charge Controller"), ctx.at(1, 1, 1));
                pos = TestContext.field(mppt, "pvPos");
                neg = TestContext.field(mppt, "pvNeg");
                parts.add(new VoltageSource("over", x, null).setU(450));
            } else {
                var server = ctx.level().getServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "time set noon");
                panel = (SolarPanelElement) ctx.placeTransparent(Ids.transparent("Small Solar Panel"), ctx.at(1, 1, 1));
                pos = TestContext.field(panel, "positiveLoad");
                neg = TestContext.field(panel, "negativeLoad");
                parts.add(new VoltageSource("reversed battery", x, null).setU(-48));
            }
            Eln.simulator.addElectricalLoad(x);   // (after placing: a failed build leaks nothing)
            parts.add(new Resistor(x, pos).setR(0.01));
            parts.add(new Resistor(neg, null).setR(0.01));
            for (Component c : parts) Eln.simulator.addElectricalComponent(c);
            // once it burns, its loads leave the simulation: take the test's parts out first thing the next tick (a
            // resistor left on a removed load breaks the solver's generate)
            Eln.simulator.addSlowPreProcess(watch);
        }

        boolean removed;
        final mods.eln.sim.IProcess watch = this::watchStep;

        void watchStep(double dt) {
            Object burnt = controller ? mppt.getBurnt() : panel.getBurnt();
            if (burnt != null) unwire();
        }

        void unwire() {
            if (removed) return;
            removed = true;
            for (Component c : parts) Eln.simulator.removeElectricalComponent(c);
            Eln.simulator.removeElectricalLoad(x);
        }

        public void measure(TestContext ctx) {
            Eln.simulator.removeSlowPreProcess(watch);
            if (controller) ctx.check("mppt: 450 V on its 350 V input burns it", mppt.getBurnt() != null, String.valueOf(mppt.getBurnt()));
            else ctx.check("solar: a battery across it backwards burns it", panel.getBurnt() != null,
                String.format("%s, panel %.1f C", panel.getBurnt(), panel.panelCelsius()));
            unwire();
        }
    }
}
