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
        if (processIn) Eln.simulator.mna.removeProcess(interSystemProcess);
        processIn = false;
    }

    /** The isolating sources' process is in the simulator (only while isolated and switched on: off, the sources aren't
     *  in the circuit, and their process would solve a subsystem they aren't in). */
    private boolean processIn;

    @Override
    public void connectJob() {
        if (isIsolator && switchedOn) {
            Eln.simulator.mna.addProcess(interSystemProcess);
            processIn = true;
        }
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
    record Wire(int turns, ElectricalCableDescriptor d, double rs, double mm2, int mm2Index, int colour, int tier) {
    }

    /**
     * The winding in a slot: a stack of six-node power cables (one a turn, each the whole cable's Rs, as in 1.12: so 1.12
     * converters keep their losses) or a machine-made winding (Wires stage 4: its wire's resistance for its length); null
     * if empty.
     */
    static Wire wire(ItemStack s) {
        if (Utils.isEmpty(s)) return null;
        if (mods.eln.node.voxel.WindingItem.is(s)) {
            var spec = mods.eln.node.voxel.WindingItem.spec(s);
            return new Wire(mods.eln.node.voxel.WindingItem.turns(s), mods.eln.node.voxel.WindingItem.descriptor(s),
                mods.eln.node.voxel.WindingItem.rs(s), spec.squareMillimetres(), spec.mm2(), spec.colour(), mods.eln.node.voxel.WindingItem.tier(s));
        }
        // a 1.12 cable item a turn, or a cable kit a block a turn (Baughn 2026-11-07: kits replace the items), each turn
        // the whole cable's Rs as the items were; a kit's spec gives its wire
        ElectricalCableDescriptor d = mods.eln.node.voxel.CableItems.descriptor(s);
        if (d == null) return null;
        int tier = mods.eln.node.voxel.CableItems.tier(s), turns = Math.min(64, mods.eln.node.voxel.CableItems.blocks(s));
        if (!mods.eln.node.voxel.CableSpec.power(tier)) return new Wire(turns, d, d.electricalRs, 0.5, 0, 7, 0);
        var spec = mods.eln.node.voxel.CableKitItem.specOf(s);
        if (spec == null) spec = mods.eln.node.voxel.CableSpec.preset(tier);
        return new Wire(turns, d, d.electricalRs, spec.squareMillimetres(), spec.mm2(), spec.colour(), tier);
    }

    Wire primaryWire() {
        return wire(inventory.getItem(TransformerContainer.primaryCableSlotId));
    }

    Wire secondaryWire() {
        return wire(inventory.getItem(TransformerContainer.secondaryCableSlotId));
    }

    public int primaryTurns() {
        Wire w = primaryWire();
        return w == null ? 0 : w.turns();
    }

    /** Turns x mm² of the windings in, against the core's window (Wires stage 4). */
    double windowUsed() {
        Wire p = primaryWire(), s = secondaryWire();
        // (both windings that are in: the screen shows the fill as they go in; AdventurAgent)
        return (p == null ? 0 : p.turns() * p.mm2()) + (s == null ? 0 : s.turns() * s.mm2());
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
    public int secondaryTurns() {
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
        boolean want = switchedOn && !legacy && limit > 0 && burnHeat > 0 && (saturated ? vpt > 1.07 * limit : vpt > SATURATES_AT * limit);
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

        if (!switchedOn) return;   // (off: both windings on their terminals, coupled to nothing)
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
        // a winding or the core changed: it comes up switched off, to be checked on its screen first (Baughn 2026-11-08)
        String k = windingsKey();
        if (!k.equals(lastWindings)) switchedOn = false;
        lastWindings = k;
        applyIsolation();
        computeInventory();
        connect();
        needPublish();
    }

    /**
     * Switched on: its windings are joined to their terminals (Baughn 2026-11-08: a converter comes up off when placed,
     * when a winding or the core changes, and after it burns out or is repaired, so a wrong winding doesn't burn it again
     * at once; its screen shows what it would do first). Saved converters from before this stay on.
     */
    public boolean switchedOn = true;
    String lastWindings = "";

    /** The windings and core as they are (items, counts and their data). */
    String windingsKey() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            ItemStack st = inventory.getItem(i);
            b.append(Utils.isEmpty(st) ? "-" : st.getItem() + "x" + st.getCount() + st.getComponentsPatch()).append(';');
        }
        return b.toString();
    }

    @Override
    public void readItemStackNBT(net.minecraft.nbt.CompoundTag nbt) {
        super.readItemStackNBT(nbt);
        switchedOn = false;   // (placed: off)
    }

    @Override
    public void burn(mods.eln.sim.process.destruct.FailureCause cause, mods.eln.sim.process.destruct.Failure.Kind kind) {
        super.burn(cause, kind);
        // (burnt, its electrical side is out of the simulation: the components may change now)
        switchedOn = false;
        applyIsolation();
    }

    @Override
    public void repair() {
        switchedOn = false;
        if (isBurnt()) applyIsolation();   // (still out of the simulation until super.repair connects it)
        super.repair();
    }

    /** Switch it on (its screen's switch; lab rigs and GameTests). */
    public void switchOn() {
        disconnect();
        switchedOn = true;
        applyIsolation();
        computeInventory();
        connect();
        needPublish();
    }

    public static final byte toggleSwitchedOn = 0x2;

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
            case toggleSwitchedOn:
                disconnect();
                switchedOn = !switchedOn;
                applyIsolation();
                computeInventory();
                connect();
                needPublish();
                break;
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
            // (the turns, for the drawing: a stack of 1.12 cables counts its items, a kit its blocks)
            Wire pw0 = primaryWire(), sw0 = secondaryWire();
            stream.writeByte(pw0 == null || mods.eln.node.voxel.WindingItem.is(inventory.getItem(0)) ? (Utils.isEmpty(inventory.getItem(0)) ? 0 : inventory.getItem(0).getCount()) : pw0.turns());
            stream.writeByte(sw0 == null || mods.eln.node.voxel.WindingItem.is(inventory.getItem(1)) ? (Utils.isEmpty(inventory.getItem(1)) ? 0 : inventory.getItem(1).getCount()) : sw0.turns());

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
            // the windings as drawn (Baughn 2026-11-07: see the configuration at a glance): turns, wire, colour, tier
            // a side, and the core's window
            for (Wire w : new Wire[]{primaryWire(), secondaryWire()}) {
                stream.writeShort(w == null ? 0 : w.turns());
                stream.writeByte(w == null ? 0 : w.mm2Index());
                stream.writeByte(w == null ? 0 : w.colour());
                stream.writeByte(w == null ? 0 : w.tier());
            }
            stream.writeFloat((float) window());
            // its screen: on or off, both sides' voltages, the core's volts a turn and the window used
            stream.writeBoolean(switchedOn);
            stream.writeFloat((float) primaryLoad.getU());
            stream.writeFloat((float) secondaryLoad.getU());
            stream.writeFloat((float) coreLimit());
            stream.writeFloat((float) windowUsed());
            stream.writeBoolean(saturated);

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("isIsolated", isIsolator);
        nbt.putBoolean("on", switchedOn);
        nbt.putInt("tierGen", 1);
        if (legacy) nbt.putString("legacyCore", legacyKey);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        // (isolation first: its two sources must be in the component list when the base reads their saved states)
        isIsolator = nbt.getBooleanOr("isIsolated", false) || !canToggleIsolation();
        switchedOn = nbt.getBooleanOr("on", true);
        applyIsolation();
        super.readFromNBT(nbt);
        switchedOn = nbt.getBooleanOr("on", true);
        lastWindings = windingsKey();
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
    public void rewindForRealVoltages() {
        ItemStack p = inventory.getItem(TransformerContainer.primaryCableSlotId);
        ItemStack s = inventory.getItem(TransformerContainer.secondaryCableSlotId);
        if (Utils.isEmpty(p) || Utils.isEmpty(s)) return;
        if (mods.eln.node.voxel.WindingItem.is(p) || mods.eln.node.voxel.WindingItem.is(s)) return;   // (windings are newer than the rewind)
        if (mods.eln.node.voxel.CableItems.kitTier(p) >= 0 || mods.eln.node.voxel.CableItems.kitTier(s) >= 0) return;   // (and kits)
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
