package mods.eln.gridnode.electricalpole;

import mods.eln.Eln;
import mods.eln.gridnode.GridElement;
import mods.eln.gridnode.GridLink;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.voxel.CableItems;
import mods.eln.node.voxel.CableSpec;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.mna.component.Transformer;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.heater.ElectricalLoadHeatThermalLoad;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A utility pole (Baughn 2026-11-08: one modular pole). It carries a line strung pole to pole with overhead cable
 * (Baughn 2026-11-09: bare copper, air insulates it), at 11 kV, set by its insulators; a line strung with cable kits
 * before then keeps their tier. Right-clicking it with equipment fits it:
 * <ul>
 * <li>a pole transformer (PoleTransformerItem): the cable socket at the foot then gives the transformer's low side,
 * at the line voltage x its ratio (low side / the line's nominal voltage);</li>
 * <li>up to four streetlights, one a side, on a beam towards the side the player stands on: each takes LAMP_WATTS from
 * the line while it's dark at the pole (the "buck converter" is built in, Baughn) and never wears out;</li>
 * <li>a stasis anchor: Stasis Delvers keeps an anchored pole running in stopped land (timeExempt).</li>
 * </ul>
 * Anything else (an empty hand) opens its screen, where equipment can be taken out.
 */
public class ElectricalPoleElement extends GridElement {
    /** Inventory slots: the transformer, the anchor, then the streetlights facing north, east, south and west. */
    public static final int TRANSFORMER = 0, ANCHOR = 1, LAMP0 = 2, SLOTS = 6;
    /** A streetlight's draw from the line, W (an LED street lamp's). */
    public static final double LAMP_WATTS = 100;
    /** Sky light (less the time of day's darkening) below which streetlights come on, and above which they go off. */
    static final int DARK_ON = 8, DARK_OFF = 11;

    public NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    public NbtThermalLoad thermalLoad = new NbtThermalLoad("thermalLoad");
    /** The transformer's low side (the socket at the foot) and the coupling to the line; idle without a transformer. */
    public final NbtElectricalLoad secondaryLoad = new NbtElectricalLoad("secondaryLoad");
    public final NbtThermalLoad transformerThermal = new NbtThermalLoad("transformerThermal");
    /** The fitted transformer, in the circuit only while one is fitted (line : low side; not isolated, as a converter). */
    public final Transformer transformerComponent = new Transformer();
    /** The streetlights' draw on the line. */
    public final Resistor lampLoad;
    ElectricalPoleDescriptor desc;
    ElectricalLoadHeatThermalLoad heater = new ElectricalLoadHeatThermalLoad(electricalLoad, thermalLoad);
    ElectricalLoadHeatThermalLoad transformerHeater = new ElectricalLoadHeatThermalLoad(secondaryLoad, transformerThermal);
    ThermalLoadWatchDog thermalWatchdog = new ThermalLoadWatchDog();
    ThermalLoadWatchDog transformerWatchdog = new ThermalLoadWatchDog();
    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();
    VoltageStateWatchDog voltageSecondaryWatchdog = new VoltageStateWatchDog();

    final TransparentNodeElementInventory inventory = new PoleInventory(this);
    /** Streetlights lit now (they share one dark sensor). */
    boolean lit;
    /** Seconds until the lights follow the sensor (NaN: they agree). */
    double switchIn = Double.NaN;
    /** Most a pole waits after its sensor changes, s. */
    static final double MAX_SWITCH_DELAY = 30;

    /** This pole's own switching delay: steady for the pole, spread over 0..MAX_SWITCH_DELAY by its position. */
    double switchDelay() {
        long h = coordonate().pos().asLong() * 0x9E3779B97F4A7C15L;
        return MAX_SWITCH_DELAY * ((h >>> 11) & 0xFFFF) / 65535.0;
    }

    /** GameTests: force the dark sensor (null: the sky decides). */
    public Boolean darkOverride;
    /** The line's tier when last applied (0: none yet). */
    int appliedTier;
    double secondaryMaxCurrent;

    public ElectricalPoleElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor, 24);
        desc = (ElectricalPoleDescriptor) descriptor;

        electricalLoad.setCanBeSimplifiedByLine(true);
        // Most of the resistance is in the cable, which is handled in GridLink.
        // We put some of it here, thereby allowing the thermal watchdog to work.
        desc.cableDescriptor.applyTo(electricalLoad);
        desc.cableDescriptor.applyTo(thermalLoad);
        electricalLoadList.add(electricalLoad);

        thermalLoadList.add(thermalLoad);
        slowProcessList.add(heater);
        thermalLoad.setAsSlow();
        slowProcessList.add(thermalWatchdog);
        thermalWatchdog
            .set(thermalLoad)
            .setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit)
            .set(new Failure(this).cable());

        slowProcessList.add(voltageWatchdog);
        voltageWatchdog.set(electricalLoad).set(new Failure(this).hvLine());

        // the transformer: an ideal one in the circuit while fitted (applyFitting). (Two poles' coupled sources, as the
        // old transformer pole had, each took the other's for an ideal source and crept towards the line's voltage.)
        electricalLoadList.add(secondaryLoad);
        thermalLoadList.add(transformerThermal);
        transformerThermal.setAsSlow();
        slowProcessList.add(transformerHeater);
        slowProcessList.add(transformerWatchdog.set(transformerThermal).set(new Failure(this).transformer()));
        slowProcessList.add(voltageSecondaryWatchdog.set(secondaryLoad).set(new Failure(this).hvLine()));

        lampLoad = new Resistor(electricalLoad, null);
        lampLoad.highImpedance();
        electricalComponentList.add(lampLoad);
        slowProcessList.add(lampProcess);

        // Publish load from time to time.
        slowProcessList.add(new NodePeriodicPublishProcess(node, 1.0, 0.5));
    }

    // ---- equipment

    public ItemStack transformer() {
        return inventory.getItem(TRANSFORMER);
    }

    public boolean hasTransformer() {
        return PoleTransformerItem.is(transformer());
    }

    public boolean hasAnchor() {
        return PoleGearItem.is(inventory.getItem(ANCHOR), PoleGearItem.Kind.ANCHOR);
    }

    /** The streetlights as a mask: bit k for the one facing LAMP_SIDES[k] (north, east, south, west). */
    public int lampMask() {
        int m = 0;
        for (int k = 0; k < 4; k++)
            if (PoleGearItem.is(inventory.getItem(LAMP0 + k), PoleGearItem.Kind.STREETLIGHT)) m |= 1 << k;
        return m;
    }

    /** Stasis Delvers: an anchored pole keeps running in stopped land. */
    @Override
    public boolean timeExempt() {
        return hasAnchor();
    }

    /** The streetlight slot facing the player: the cardinal direction from the pole to them (0 north .. 3 west). */
    int sideTowards(Player p) {
        double dx = p.getX() - (coordonate().x + 0.5), dz = p.getZ() - (coordonate().z + 0.5);
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? 1 : 3;
        return dz > 0 ? 2 : 0;
    }

    static final String[] SIDE_NAMES = {"north", "east", "south", "west"};

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        ItemStack s = player.getMainHandItem();
        int slot = -1;
        String what = null;
        if (PoleTransformerItem.is(s)) {
            slot = TRANSFORMER;
            what = "a transformer";
        } else if (PoleGearItem.is(s, PoleGearItem.Kind.ANCHOR)) {
            slot = ANCHOR;
            what = "a stasis anchor";
        } else if (PoleGearItem.is(s, PoleGearItem.Kind.STREETLIGHT)) {
            int k = sideTowards(player);
            slot = LAMP0 + k;
            what = "a streetlight facing " + SIDE_NAMES[k];
        }
        if (slot < 0) return super.onBlockActivated(player, side, vx, vy, vz);
        if (coordonate().world().isClientSide()) return true;
        if (!inventory.getItem(slot).isEmpty()) {
            Utils.sendMessage(player, "This pole already has " + what);
            return true;
        }
        inventory.setItem(slot, s.copyWithCount(1));
        if (!player.isCreative()) s.shrink(1);
        inventory.setChanged();   // (setItem alone doesn't tell the element)
        return true;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public void inventoryChange(Container inv) {
        super.inventoryChange(inv);
        disconnect();
        applyFitting();
        applyEquipment();
        connect();
        needPublish();
        // the span's halo is drawn by whichever pole draws the span: tell the neighbours too
        for (GridLink link : gridLinkList) {
            var other = link.getOtherElement(this);
            if (other != null) other.needPublish();
        }
    }

    /** Puts the transformer into the circuit, or takes it out, as one is fitted (between disconnect and connect). */
    void applyFitting() {
        electricalComponentList.remove(transformerComponent);
        electricalLoad.remove(transformerComponent);
        secondaryLoad.remove(transformerComponent);
        if (hasTransformer()) {
            transformerComponent.connectTo(electricalLoad, secondaryLoad);
            electricalComponentList.add(transformerComponent);
        }
    }

    // ---- the line

    /**
     * The line's tier: 11 kV, set by the pole's insulators for spans of overhead cable (Baughn 2026-11-09); a line
     * strung with cable kits before then keeps their tier.
     */
    public int lineTier() {
        for (GridLink link : gridLinkList) {
            int t = CableItems.tier(link.cableStack());
            if (CableSpec.power(t)) return t;
        }
        for (GridLink link : gridLinksBooting) {
            int t = CableItems.tier(link.cableStack());
            if (CableSpec.power(t)) return t;
        }
        return 4;
    }

    /** The line's nominal voltage. */
    public double lineVolts() {
        return CableSpec.volts(lineTier());
    }

    /**
     * The most the line takes before the pole burns. 11 kV lines: 30 % over, like a cable. Lower tiers keep 1.12's
     * allowance of 16 x 400 V (air insulation), so lines built before kits had tiers still hold.
     */
    public double lineMaxVolts() {
        int t = lineTier();
        return t >= 4 ? CableSpec.volts(t) * 1.3 : Eln.HVU * 16;
    }

    @Override
    protected void onLinksChanged() {
        applyEquipment();
        needPublish();
    }

    /** Sets the watchdogs, the ratio and the low side's wire from the line and the fitted transformer. */
    void applyEquipment() {
        voltageWatchdog.setUMaxMin(lineMaxVolts());
        ItemStack t = transformer();
        if (PoleTransformerItem.is(t)) {
            double low = PoleTransformerItem.volts(t);
            var wire = new CableSpec(PoleTransformerItem.mm2(t), CableSpec.preset(PoleTransformerItem.tier(t)).colour())
                .descriptor(PoleTransformerItem.tier(t));
            wire.applyTo(secondaryLoad, PoleTransformerItem.lossFactor(t));
            wire.applyTo(transformerThermal);
            transformerWatchdog.setLimit(wire.thermalWarmLimit, wire.thermalCoolLimit);
            voltageSecondaryWatchdog.setUNominal(low);
            secondaryMaxCurrent = PoleTransformerItem.amps(t);
            transformerComponent.setRatio(low / lineVolts());
        } else {
            desc.cableDescriptor.applyTo(secondaryLoad);
            desc.cableDescriptor.applyTo(transformerThermal);
            transformerWatchdog.setLimit(desc.cableDescriptor.thermalWarmLimit, desc.cableDescriptor.thermalCoolLimit);
            voltageSecondaryWatchdog.setUNominal(lineMaxVolts());
            secondaryMaxCurrent = 0;
            transformerComponent.setRatio(1);
        }
        appliedTier = lineTier();
    }

    // ---- streetlights

    /** Whether it's dark enough at the pole's top for streetlights (with some hysteresis, so dusk doesn't flicker). */
    boolean dark(Level level) {
        if (darkOverride != null) return darkOverride;
        BlockPos top = coordonate().pos().above(3);
        int sky = level.getBrightness(LightLayer.SKY, top) - level.getSkyDarken();
        return lit ? sky <= DARK_OFF : sky < DARK_ON;
    }

    final IProcess lampProcess = new IProcess() {
        @Override
        public void process(double time) {
            if (appliedTier != lineTier()) applyEquipment();
            int mask = lampMask();
            Level level = coordonate().world();
            boolean powered = electricalLoad.getU() > lineVolts() * 0.5;
            boolean want = mask != 0 && level != null && powered && dark(level);
            // each pole switches a little after the others (Baughn: not all at once at dusk): a delay of its own, 0..30 s
            if (want != lit) {
                switchIn = Double.isNaN(switchIn) ? switchDelay() : switchIn - time;
                if (switchIn <= 0 || !powered || darkOverride != null) {
                    lit = want;
                    switchIn = Double.NaN;
                    needPublish();
                }
            } else {
                switchIn = Double.NaN;
            }
            if (lit) {
                // constant power: the built-in converter draws LAMP_WATTS each, whatever the line's voltage
                double u = Math.max(electricalLoad.getU(), lineVolts() * 0.5);
                lampLoad.setR(u * u / (LAMP_WATTS * Integer.bitCount(mask)));
                for (int k = 0; k < 4; k++) {
                    if ((mask & (1 << k)) == 0) continue;
                    BlockPos p = lampLightPos(k);
                    LightBlockEntity.addLight(level, p.getX(), p.getY(), p.getZ(), 15, 5);
                }
            } else {
                lampLoad.highImpedance();
            }
        }
    };

    /** Where a streetlight's light goes: under its head, two blocks out and two below the pole's top. */
    BlockPos lampLightPos(int k) {
        int[] dx = {0, 1, 0, -1}, dz = {-1, 0, 1, 0};
        return coordonate().pos().offset(dx[k] * 2, 2, dz[k] * 2);
    }

    // ---- simulation glue

    @Override
    public String multiMeterString(Direction side) {
        String line = Utils.plotVolt("Line U:", electricalLoad.getU()) + Utils.plotAmpere("I:", electricalLoad.getCurrent());
        if (!hasTransformer()) return line;
        return line + Utils.plotVolt("  Low U:", secondaryLoad.getU()) + Utils.plotAmpere("I:", secondaryLoad.getCurrent());
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return secondaryLoad;
    }

    @Override
    public ElectricalLoad getGridElectricalLoad(Direction side) {
        return electricalLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermalLoad;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return hasTransformer() ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public void initialize() {
        applyFitting();
        applyEquipment();
        super.initialize();
        applyEquipment();
    }

    /** Old "Utility Pole w/DC-DC Converter" blocks: their built-in transformer becomes a fitted 400 V one. */
    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        if (desc.includeTransformer && !nbt.getBooleanOr("modular", false) && inventory.getItem(TRANSFORMER).isEmpty())
            inventory.setItem(TRANSFORMER, PoleTransformerItem.make(3, 8, 2));
        lit = nbt.getBooleanOr("lit", false);
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putBoolean("modular", true);
        nbt.putBoolean("lit", lit);
    }

    /** A freshly placed old-style transformer pole gets its transformer too. */
    @Override
    public void readItemStackNBT(CompoundTag nbt) {
        super.readItemStackNBT(nbt);
        if (desc.includeTransformer && inventory.getItem(TRANSFORMER).isEmpty())
            inventory.setItem(TRANSFORMER, PoleTransformerItem.make(3, 8, 2));
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        node.lrduCubeMask.getTranslate(front.down()).serialize(stream);
        try {
            stream.writeFloat(secondaryMaxCurrent != 0 ? (float) (secondaryLoad.getI() / secondaryMaxCurrent) : 0f);
            ItemStack t = transformer();
            stream.writeByte(PoleTransformerItem.is(t) ? PoleTransformerItem.tier(t) : 0);
            stream.writeByte(lampMask() | (lit ? 16 : 0) | (hasAnchor() ? 32 : 0));
            stream.writeByte(lineTier());
            stream.writeFloat((float) electricalLoad.getU());
            stream.writeFloat((float) (electricalLoad.getU() * electricalLoad.getI()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
