package mods.eln.transparentnode.transformer;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import mods.eln.sim.mna.component.VoltageSource;
import mods.eln.sim.mna.process.TransformerInterSystemProcess;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TransformerElement extends TransparentNodeElement {
    final NbtElectricalLoad primaryLoad = new NbtElectricalLoad("primaryLoad");
    final NbtElectricalLoad secondaryLoad = new NbtElectricalLoad("secondaryLoad");

    private final VoltageSource primaryVoltageSource = new VoltageSource("primaryVoltageSource");
    private final VoltageSource secondaryVoltageSource = new VoltageSource("secondaryVoltageSource");

    final TransformerInterSystemProcess interSystemProcess =
        new TransformerInterSystemProcess(primaryLoad, secondaryLoad, primaryVoltageSource, secondaryVoltageSource);
    final Transformer transformer = new Transformer();

    final TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);

    private float primaryMaxCurrent = 0;
    private float secondaryMaxCurrent = 0;
    private final TransformerDescriptor transformerDescriptor;

    boolean isIsolator = false;

    // ---- realistic converter (Baughn 2026-11-06: "like real-world transformers"): heat of the windings and the core

    /** The windings' and core's heat: a winding over its current, or a saturated core, heats it until it burns. */
    final NbtThermalLoad heat = new NbtThermalLoad("heat");
    private final ThermalLoadWatchDog heatWatchdog = new ThermalLoadWatchDog();
    /** Across the primary while the core saturates: the magnetising current, which heats the core. */
    final Resistor magnetizing = new Resistor();
    /** The core is saturated (switched with a margin: entered above its volts per turn, left below 97 % of it). */
    public boolean saturated;
    private int dwell;
    /**
     * Saved before the real-world voltages: it keeps working as it was (no saturation) until a player changes its windings
     * or core (legacyKey records them as loaded).
     */
    public boolean legacy;
    String legacyKey;
    /** Temperature it burns at (C), how far over ambient that is at 1.3 x its windings' rated current, its time constant (s). */
    static final double WARM_LIMIT = 100, BURN_RISE = 80, TAU = 30;
    /** A saturated core heats this many times as fast as windings at their burning current: it burns in about 10 s. */
    static final double SATURATION_HEAT = 4;
    /** Heat of the windings at their burning current (W), from the last inventory (0: incomplete). */
    double burnHeat;

    public TransformerElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);

        electricalLoadList.add(primaryLoad);
        electricalLoadList.add(secondaryLoad);
        Failure exp = new Failure(this).transformer();
        slowProcessList.add(voltagePrimaryWatchdog.set(primaryLoad).set(exp));
        slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(exp));

        thermalLoadList.add(heat);
        thermalFastProcessList.add(new ElectricalLoadHeatThermalLoad(primaryLoad, heat));
        thermalFastProcessList.add(new ElectricalLoadHeatThermalLoad(secondaryLoad, heat));
        thermalFastProcessList.add(new ElectricalResistorHeatThermalLoad(magnetizing, heat));
        magnetizing.connectTo(primaryLoad, null);
        electricalComponentList.add(magnetizing);
        slowProcessList.add(heatWatchdog.set(heat).setTMax(WARM_LIMIT).set(exp));
        slowProcessList.add(dt -> checkSaturation());

        transformerDescriptor = (TransformerDescriptor) descriptor;

        slowProcessList.add(new NodePeriodicPublishProcess(node, 1., .5));
    }

    private final VoltageStateWatchDog voltagePrimaryWatchdog = new VoltageStateWatchDog();
    private final VoltageStateWatchDog voltageSecondaryWatchdog = new VoltageStateWatchDog();

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        if (isIsolator)
            Eln.simulator.mna.removeProcess(interSystemProcess);
    }

    @Override
    public void connectJob() {
        if (isIsolator)
            Eln.simulator.mna.addProcess(interSystemProcess);
        super.connectJob();
    }

    /**
     * Both windings and the core are in (Baughn 2026-10-27: with any of the three slots empty no power flows). Cables
     * still attach to its terminals (Baughn, #164: so a voxel cable can be laid to an unconfigured one); only the
     * electrical link waits for it to be complete.
     */
    boolean complete() {
        return primaryWire() != null && secondaryWire() != null
            && !Utils.isEmpty(inventory.getItem(TransformerContainer.ferromagneticSlotId)) && !windowFull();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || !complete()) return null;
        if (side == front.left()) return primaryLoad;
        if (side == front.right()) return secondaryLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu == LRDU.Down) {
            if (side == front.left()) return NodeBase.maskElectricalPower;
            if (side == front.right()) return NodeBase.maskElectricalPower;
            if (side == front && !grounded) return NodeBase.maskElectricalPower;
            if (side == front.back() && !grounded) return NodeBase.maskElectricalPower;
        }
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        if (side == front.left())
            return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", -primaryLoad.getCurrent());
        if (side == front.right())
            return Utils.plotVolt("US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", -secondaryLoad.getCurrent());

        return Utils.plotVolt("UP+:", primaryLoad.getU()) + Utils.plotAmpere("IP+:", transformer.aCurrentState.state)
            + Utils.plotVolt("  US+:", secondaryLoad.getU()) + Utils.plotAmpere("IS+:", transformer.bCurrentState.state);

    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        applyIsolation();
        computeInventory();
        connect();
    }

    /** A winding: its turns, its wire's constants, its resistance (Rs as a load counts it) and cross-section, mm². */
    record Wire(int turns, ElectricalCableDescriptor d, double rs, double mm2) {
    }

    /**
     * The winding in a slot: a stack of six-node power cables (one a turn, each the whole cable's Rs, as in 1.12: so 1.12
     * converters keep their losses) or a machine-made winding (Wires stage 4: its wire's resistance for its length); null
     * if empty.
     */
    static Wire wire(ItemStack s) {
        if (Utils.isEmpty(s)) return null;
        if (mods.eln.node.voxel.WindingItem.is(s))
            return new Wire(mods.eln.node.voxel.WindingItem.turns(s), mods.eln.node.voxel.WindingItem.descriptor(s),
                mods.eln.node.voxel.WindingItem.rs(s), mods.eln.node.voxel.WindingItem.spec(s).squareMillimetres());
        if (!(Eln.sixNodeItem.getDescriptor(s) instanceof ElectricalCableDescriptor d)) return null;
        int tier = mods.eln.node.voxel.CablePart.tierOf(d.name);
        double mm2 = mods.eln.node.voxel.CableSpec.power(tier) ? mods.eln.node.voxel.CableSpec.preset(tier).squareMillimetres() : 0.5;
        return new Wire(s.getCount(), d, d.electricalRs, mm2);
    }

    Wire primaryWire() {
        return wire(inventory.getItem(TransformerContainer.primaryCableSlotId));
    }

    Wire secondaryWire() {
        return wire(inventory.getItem(TransformerContainer.secondaryCableSlotId));
    }

    int primaryTurns() {
        Wire w = primaryWire();
        return w == null ? 0 : w.turns();
    }

    /** Turns x mm² of both windings, against the core's window (Wires stage 4); 0 if a slot is empty. */
    double windowUsed() {
        Wire p = primaryWire(), s = secondaryWire();
        return p == null || s == null ? 0 : p.turns() * p.mm2() + s.turns() * s.mm2();
    }

    double window() {
        ItemStack core = inventory.getItem(TransformerContainer.ferromagneticSlotId);
        return Utils.isEmpty(core) ? 0 : ((FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core)).window;
    }

    /**
     * The windings don't fit the core: it disconnects until they do. Cables count as well as windings, and a converter
     * built before the window is held to it too (Baughn 2026-11-06: they disconnect until corrected).
     */
    public boolean windowFull() {
        return window() > 0 && windowUsed() > window();
    }

    private void computeInventory() {
        ItemStack primaryCable = inventory.getItem(TransformerContainer.primaryCableSlotId);
        ItemStack secondaryCable = inventory.getItem(TransformerContainer.secondaryCableSlotId);
        ItemStack core = inventory.getItem(TransformerContainer.ferromagneticSlotId);
        Wire pw = wire(primaryCable), sw = wire(secondaryCable);
        ElectricalCableDescriptor primaryCableDescriptor = pw == null ? null : pw.d(), secondaryCableDescriptor = sw == null ? null : sw.d();

        if (primaryCableDescriptor != null)
            voltagePrimaryWatchdog.setUNominal(primaryCableDescriptor.electricalNominalVoltage);
        else
            voltagePrimaryWatchdog.setUNominal(1000000);

        if (secondaryCableDescriptor != null)
            voltageSecondaryWatchdog.setUNominal(secondaryCableDescriptor.electricalNominalVoltage);
        else
            voltageSecondaryWatchdog.setUNominal(1000000);

        double coreFactor = 1;
        if (!Utils.isEmpty(core)) {
            FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

            coreFactor = coreDescriptor.cableMultiplicator;
        }

        if (pw == null || Utils.isEmpty(core)) {
            primaryLoad.highImpedance();
            primaryMaxCurrent = 0;
        } else {
            primaryLoad.setRs(pw.rs() * coreFactor);
            primaryMaxCurrent = (float) primaryCableDescriptor.electricalMaximalCurrent;
        }

        if (sw == null || Utils.isEmpty(core)) {
            secondaryLoad.highImpedance();
            secondaryMaxCurrent = 0;
        } else {
            secondaryLoad.setRs(sw.rs() * coreFactor);
            secondaryMaxCurrent = (float) secondaryCableDescriptor.electricalMaximalCurrent;
        }

        if (pw != null && sw != null) {
            transformer.setRatio(1.0 * secondaryTurns() / pw.turns());
            interSystemProcess.setRatio(1.0 * secondaryTurns() / pw.turns());
        } else {
            transformer.setRatio(1);
            interSystemProcess.setRatio(1);
        }

        // its heat: at 1.3 x the windings' rated current it settles BURN_RISE over ambient, with a TAU time constant
        burnHeat = 0;
        if (complete()) {
            double ip = 1.3 * primaryCableDescriptor.electricalMaximalCurrent, is = 1.3 * secondaryCableDescriptor.electricalMaximalCurrent;
            burnHeat = Math.max(ip * ip * primaryLoad.getRs() * 2, is * is * secondaryLoad.getRs() * 2);
        }
        if (burnHeat > 0) {
            heat.Rp = BURN_RISE / burnHeat;
            heat.C = TAU / heat.Rp;
        } else {
            heat.Rp = 1;
            heat.C = 100;
        }
        heat.setRsByTao(TAU);
        setSaturated(false);
    }

    /** Whether its screen's isolation toggle works (the variable converter is always isolated). */
    boolean canToggleIsolation() {
        return true;
    }

    /** The turns of the secondary in use (the variable converter taps it). */
    int secondaryTurns() {
        Wire w = secondaryWire();
        return w == null ? 0 : w.turns();
    }

    /** The core's volts per turn now: the larger of the two windings', 0 if it is incomplete. */
    double voltsPerTurn() {
        ItemStack p = inventory.getItem(TransformerContainer.primaryCableSlotId);
        int ns = secondaryTurns();
        if (!complete() || ns == 0) return 0;
        return Math.max(Math.abs(primaryLoad.getU()) / primaryTurns(), Math.abs(secondaryLoad.getU()) / ns);
    }

    /** Saturation starts this far over the core's rated volts per turn. */
    static final double SATURATES_AT = 1.1;

    double coreLimit() {
        ItemStack core = inventory.getItem(TransformerContainer.ferromagneticSlotId);
        return Utils.isEmpty(core) ? 0 : ((FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core)).voltsPerTurn;
    }

    String inventoryKey() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            ItemStack st = inventory.getItem(i);
            b.append(Utils.isEmpty(st) ? "-" : st.getItem() + "x" + st.getCount()).append(';');
        }
        return b.toString();
    }

    /** Every slow step: the core saturates past its volts per turn (not a legacy converter's, until it is changed). */
    void checkSaturation() {
        if (legacy && !inventoryKey().equals(legacyKey)) legacy = false;
        double limit = coreLimit(), vpt = voltsPerTurn();
        // (the core's rating is its working flux: it saturates 10 % over it, so a converter wound for 48 V doesn't saturate
        // on a line a volt high, as a real one has headroom)
        boolean want = !legacy && limit > 0 && burnHeat > 0 && (saturated ? vpt > 1.07 * limit : vpt > SATURATES_AT * limit);
        if (dwell > 0) dwell--;
        else if (want != saturated) {
            setSaturated(want);
            dwell = 20;   // (a second at least each way: a weak line sagging under the draw mustn't chatter)
        }
    }

    /** Switch the magnetising draw: SATURATION_HEAT x the windings' burning heat at the primary's voltage now. */
    void setSaturated(boolean on) {
        saturated = on;
        double u = Math.abs(primaryLoad.getU());
        if (on && u > 1e-3 && burnHeat > 0) magnetizing.setR(Math.max(0.01, u * u / (SATURATION_HEAT * burnHeat)));
        else magnetizing.highImpedance();
        needPublish();
    }

    private void applyIsolation() {
        electricalComponentList.remove(transformer);
        electricalComponentList.remove(primaryVoltageSource);
        electricalComponentList.remove(secondaryVoltageSource);
        primaryLoad.remove(primaryVoltageSource);
        secondaryLoad.remove(secondaryVoltageSource);
        primaryLoad.remove(transformer);
        secondaryLoad.remove(transformer);

        if (isIsolator) {
            primaryVoltageSource.connectTo(primaryLoad, null);
            secondaryVoltageSource.connectTo(secondaryLoad, null);
            electricalComponentList.add(primaryVoltageSource);
            electricalComponentList.add(secondaryVoltageSource);
        } else {
            transformer.connectTo(primaryLoad, secondaryLoad);
            electricalComponentList.add(transformer);
        }
    }

    public void inventoryChange(Container inventory) {
        disconnect();
        computeInventory();
        connect();
        needPublish();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    public float getLightOpacity() {
        return 1.0f;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public void onGroundedChangedByClient() {
        super.onGroundedChangedByClient();
        computeInventory();
        reconnect();
    }

    public static final byte toogleIsIsolator = 0x1;

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        switch (super.networkUnserialize(stream)) {
            case toogleIsIsolator:
                if (!canToggleIsolation()) break;
                disconnect();
                isIsolator = !isIsolator;
                applyIsolation();
                reconnect();
                needPublish();
                break;
        }
        return 0;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            if (Utils.isEmpty(inventory.getItem(0)))
                stream.writeByte(0);
            else
                stream.writeByte(inventory.getItem(0).getCount());
            if (Utils.isEmpty(inventory.getItem(1)))
                stream.writeByte(0);
            else
                stream.writeByte(inventory.getItem(1).getCount());

            Utils.serialiseItemStack(stream, inventory.getItem(TransformerContainer.ferromagneticSlotId));
            Utils.serialiseItemStack(stream, inventory.getItem(TransformerContainer.primaryCableSlotId));
            Utils.serialiseItemStack(stream, inventory.getItem(TransformerContainer.secondaryCableSlotId));

            node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
            stream.writeBoolean(isIsolator);

            float load = 0f;
            if (primaryMaxCurrent != 0 && secondaryMaxCurrent != 0) {
                load = Utils.limit((float) Math.max(primaryLoad.getI() / primaryMaxCurrent,
                    secondaryLoad.getI() / secondaryMaxCurrent), 0f, 1f);
            }
            stream.writeFloat(load);
            stream.writeBoolean(!Utils.isEmpty(inventory.getItem(3)));

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("isIsolated", isIsolator);
        nbt.putInt("tierGen", 1);
        if (legacy) nbt.putString("legacyCore", legacyKey);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        // (isolation first: its two sources must be in the component list when the base reads their saved states)
        isIsolator = nbt.getBooleanOr("isIsolated", false) || !canToggleIsolation();
        applyIsolation();
        super.readFromNBT(nbt);
        if (nbt.getIntOr("tierGen", 0) < 1) {
            rewindForRealVoltages();
            legacy = true;
            legacyKey = inventoryKey();
        } else if (nbt.contains("legacyCore")) {
            legacy = true;
            legacyKey = nbt.getStringOr("legacyCore", "");
        }
    }

    /**
     * Once, for a converter saved before the real-world voltages (no "tierGen"): one wound for 1.12's tiers is rewound for
     * today's (Baughn 2026-11-06). Its windings' cables say which tiers it joined: a converter whose turns ratio was exactly
     * the old ratio of its two windings' tiers (1:4 for 50 V -> 200 V, 4:1 for 800 V -> 200 V...) gets the turns closest to
     * the new ratio (5:24 for 48 V -> 230 V, 26:15 for 400 V -> 230 V, 2:55 for 400 V -> 11 kV). Windings of one tier wound 1:4 or 4:1 are taken
     * for 48 V <-> 230 V. Coils are only added, never taken; anything else is left as it is.
     */
    void rewindForRealVoltages() {
        ItemStack p = inventory.getItem(TransformerContainer.primaryCableSlotId);
        ItemStack s = inventory.getItem(TransformerContainer.secondaryCableSlotId);
        if (Utils.isEmpty(p) || Utils.isEmpty(s)) return;
        if (mods.eln.node.voxel.WindingItem.is(p) || mods.eln.node.voxel.WindingItem.is(s)) return;   // (windings are newer than the rewind)
        int[] turns = rewound(p.getCount(), s.getCount(), cableVolts(p), cableVolts(s));
        if (turns == null) return;
        p.setCount(turns[0]);
        s.setCount(turns[1]);
    }

    static double cableVolts(ItemStack cable) {
        return Eln.sixNodeItem.getDescriptor(cable) instanceof ElectricalCableDescriptor d ? d.electricalNominalVoltage : 0;
    }

    /** 1.12's voltage of today's tier (48 -> 50, 230 -> 200, 400 -> 800, 11000 -> 3200), or 0. */
    static double oldTier(double u) {
        if (u == Eln.LVU) return 50;
        if (u == Eln.MVU) return 200;
        if (u == Eln.HVU) return 800;
        if (u == Eln.VVU) return 3200;
        return 0;
    }

    /** The new turns [primary, secondary] for an old converter of turns p:s between windings rated up and us, or null: as is. */
    static int[] rewound(int p, int s, double up, double us) {
        double oldP = oldTier(up), oldS = oldTier(us);
        double want;
        if (oldP > 0 && oldS > 0 && oldP != oldS && Math.abs((double) s / p - oldS / oldP) < 1e-9) want = us / up;
        else if (s == 4 * p) want = Eln.MVU / Eln.LVU;
        else if (p == 4 * s) want = Eln.LVU / Eln.MVU;
        else return null;
        // the fewest turns within 0.5 % of the new ratio (5:24 is 0.17 % over 230 V), else the closest
        int[] fewest = null, closest = null;
        double err = Double.MAX_VALUE;
        for (int np = p; np <= 64; np++)
            for (int ns = s; ns <= 64; ns++) {
                double e = Math.abs((double) ns / np / want - 1);
                if (e <= 0.005 && (fewest == null || np + ns < fewest[0] + fewest[1])) fewest = new int[]{np, ns};
                if (e < err) {
                    err = e;
                    closest = new int[]{np, ns};
                }
            }
        return fewest != null ? fewest : closest;
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new java.util.LinkedHashMap<>();   // (in order: Jade shows the first lines unsneaked)
        info.put(I18N.tr("Ratio"), Utils.plotValue(transformer.getRatio()));
        if (window() > 0 && primaryWire() != null && secondaryWire() != null)
            info.put(I18N.tr("Window"), Math.round(windowUsed()) + " / " + Math.round(window()) + " mm² turns" + (windowFull() ? " " + I18N.tr("FULL: no room") : ""));
        double limit = coreLimit();
        if (limit > 0 && complete())
            info.put(I18N.tr("Volts per turn"), Utils.plotValue(voltsPerTurn()) + " / " + Utils.plotValue(limit)
                + (saturated ? " " + I18N.tr("SATURATED") : legacy ? " " + I18N.tr("(as built before 48 V)") : ""));
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", heat.Tc));
        info.put(I18N.tr("Isolated"), isIsolator ? I18N.tr("Yes") : I18N.tr("No"));
        if (Eln.wailaEasyMode) {
            FerromagneticCoreDescriptor core =
                (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(
                    inventory.getItem(TransformerContainer.ferromagneticSlotId));
            if (core != null) {
                info.put(I18N.tr("Core factor"), Utils.plotValue(core.cableMultiplicator));
            }
            info.put("Voltages", "\u00A7a" + Utils.plotVolt("", primaryLoad.getU()) + " " +
                "\u00A7e" + Utils.plotVolt("", secondaryLoad.getU()));
        }
        return info;
    }
}
