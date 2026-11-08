package mods.eln.transparentnode.wiredrawing;

import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.OreDictTags;
import mods.eln.misc.Utils;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.voxel.CableKitItem;
import mods.eln.node.voxel.CableSpec;
import mods.eln.node.voxel.WindingItem;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A wire-drawing machine (WireDrawingDescriptor). The screen sets what to make: a cable kit (a length in blocks) or a
 * winding (a number of turns), of a cross-section, insulation and colour within the machine's limits, or bare Copper
 * Cable (a number of them; no rubber, half again the hand recipe's yield). Started, it
 * draws the wire a little each tick at its rated power, taking copper (ingots or dust) and rubber from its slots a
 * whole item at a time into buffers that keep the fractions, and puts the kit or winding in its output slot when the
 * length is done, and stops: one item a Start. It waits while the output slot is full, and pauses (keeping its
 * progress) when copper or rubber runs out. Changing what to make scraps the wire drawn so far.
 */
public class WireDrawingElement extends TransparentNodeElement {
    public static final int COPPER = 0, RUBBER = 1, OUT = 2;
    /** What it makes: a cable kit, a converter winding, or bare Copper Cable (Baughn 2026-11-07). */
    public static final int KIT = 0, WINDING = 1, BARE = 2;
    public static final byte SET = 1, RUN = 2;
    public static final byte STOPPED = 0, RUNNING = 1, NO_COPPER = 2, NO_RUBBER = 3, OUTPUT_FULL = 4, NO_POWER = 5;

    final WireDrawingDescriptor descriptor;
    final WireDrawingInventory inventory = new WireDrawingInventory(this);
    public final ElementMachine machine;

    /** What to make. */
    public int mode = KIT, mm2, tier, colour, amount = 16;
    public boolean running;
    /** Blocks of wire drawn for the current item; copper (ingots) and rubber taken but not yet drawn. */
    public double done, copper, rubber;
    public byte status = STOPPED;
    double watts;

    public WireDrawingElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        descriptor = (WireDrawingDescriptor) d;
        // (each machine starts on its own voltage's preset)
        tier = descriptor.nominalU >= mods.eln.Eln.MVU ? 2 : 1;
        CableSpec preset = CableSpec.preset(tier);
        mm2 = Math.min(descriptor.maxMm2, preset.mm2());
        colour = descriptor.defaultColour;
        machine = Machines.forElement(this, descriptor.spec());
        slowProcessList.add(process);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    public static boolean isCopper(ItemStack s) {
        return !s.isEmpty() && (s.is(OreDictTags.tag("ingotCopper")) || s.is(OreDictTags.tag("dustCopper")));
    }

    public static boolean isRubber(ItemStack s) {
        return !s.isEmpty() && s.is(OreDictTags.tag("itemRubber"));
    }

    /** The current item's wire, blocks. */
    public double length() {
        return WireDrawingDescriptor.length(mode, amount);
    }

    public CableSpec spec() {
        return new CableSpec(mm2, colour);
    }

    /** Copper and rubber for the whole item. */
    public double copperNeeded() {
        return length() * WireDrawingDescriptor.copperPerBlock(mode, mm2);
    }

    public double rubberNeeded() {
        return length() * WireDrawingDescriptor.rubberPerBlock(mode, tier, mm2);
    }

    public double energyNeeded() {
        return length() * WireDrawingDescriptor.energyPerBlock(mode, mm2);
    }

    /** What it makes now, for the given length (a kit of the blocks, a winding of the turns, Copper Cables). */
    public ItemStack product() {
        if (mode == BARE) return mods.eln.Eln.findItemStack("Copper Cable", amount);
        return mode == KIT ? CableKitItem.kit(tier, spec(), amount * 16) : WindingItem.make(tier, spec(), amount);
    }

    public double progress() {
        double l = length();
        return l <= 0 ? 0 : Math.min(1, done / l);
    }

    /**
     * Sets what to make, clamped to the machine's limits (whatever the client sent); anything that changes the item
     * scraps the wire drawn for it.
     */
    public void configure(int mode, int mm2, int tier, int colour, int amount) {
        apply(mode, mm2, tier, colour, amount);
        needPublish();
    }

    void apply(int mode, int mm2, int tier, int colour, int amount) {
        mode = mode == WINDING || mode == BARE ? mode : KIT;
        mm2 = Math.max(0, Math.min(descriptor.maxMm2, mm2));
        tier = Math.max(1, Math.min(descriptor.maxTier, tier));
        colour = Math.max(0, Math.min(15, colour));
        amount = Math.max(1, Math.min(WireDrawingDescriptor.maxAmount(mode), amount));
        if (mode != this.mode || mm2 != this.mm2 || tier != this.tier || colour != this.colour || amount != this.amount) done = 0;
        this.mode = mode;
        this.mm2 = mm2;
        this.tier = tier;
        this.colour = colour;
        this.amount = amount;
    }

    /** Tops a buffer up from its slot, an item at a time, until it holds need (false: the slot ran out first). */
    boolean fill(int slot, boolean isCopper, double need) {
        while ((isCopper ? copper : rubber) < need - 1e-9) {
            ItemStack s = inventory.getItem(slot);
            if (s.isEmpty() || !(isCopper ? isCopper(s) : isRubber(s))) return false;
            inventory.removeItem(slot, 1);
            if (isCopper) copper += 1;
            else rubber += 1;
        }
        return true;
    }

    public final IProcess process = this::tick;

    /** A tick's drawing (public for GameTests). */
    public void tick(double dt) {
        watts = 0;
        if (!running) {
            status = STOPPED;
            return;
        }
        if (!inventory.getItem(OUT).isEmpty()) {
            status = OUTPUT_FULL;
            return;
        }
        double jpb = WireDrawingDescriptor.energyPerBlock(mode, mm2), cpb = WireDrawingDescriptor.copperPerBlock(mode, mm2),
            rpb = WireDrawingDescriptor.rubberPerBlock(mode, tier, mm2);
        double step = Math.min(machine.ratedJoulesPerTick() / jpb, length() - done);
        if (!fill(COPPER, true, step * cpb)) {
            status = NO_COPPER;
            return;
        }
        if (rpb > 0 && !fill(RUBBER, false, step * rpb)) {
            status = NO_RUBBER;
            return;
        }
        double got = machine.takeJoules(step * jpb);
        watts = got / Math.max(1e-6, dt);
        double d = got / jpb;
        done += d;
        copper = Math.max(0, copper - d * cpb);
        rubber = Math.max(0, rubber - d * rpb);
        status = got > 0 ? RUNNING : NO_POWER;
        if (done >= length() - 1e-9) {
            inventory.setItem(OUT, product());
            done = 0;
            // one item a Start (Baughn 2026-11-07: twice he made a second one he didn't mean to)
            running = false;
            status = STOPPED;
            inventory.setChanged();
            needPublish();
        }
    }

    // ---- the rest

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte type = super.networkUnserialize(stream);
        try {
            if (type == SET) {
                String[] f = stream.readUTF().split(",");
                // (not while drawing: the screen greys its settings then; a stale packet mustn't scrap the item)
                if (f.length == 5 && !running)
                    configure(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]));
            } else if (type == RUN) {
                running = stream.readBoolean();
                needPublish();
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return type;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(mode);
            stream.writeByte(mm2);
            stream.writeByte(tier);
            stream.writeByte(colour);
            stream.writeShort(amount);
            stream.writeBoolean(running);
            stream.writeByte(status);
            stream.writeFloat((float) progress());
            stream.writeFloat((float) copper);
            stream.writeFloat((float) rubber);
            stream.writeFloat((float) watts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putInt("mode", mode);
        nbt.putInt("mm2", mm2);
        nbt.putInt("tier", tier);
        nbt.putInt("col", colour);
        nbt.putInt("amount", amount);
        nbt.putBoolean("running", running);
        nbt.putDouble("done", done);
        nbt.putDouble("copper", copper);
        nbt.putDouble("rubber", rubber);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        double d = nbt.getDoubleOr("done", 0);
        apply(nbt.getIntOr("mode", mode), nbt.getIntOr("mm2", mm2), nbt.getIntOr("tier", tier), nbt.getIntOr("col", colour),
            nbt.getIntOr("amount", amount));
        done = d;
        running = nbt.getBooleanOr("running", false);
        copper = nbt.getDoubleOr("copper", 0);
        rubber = nbt.getDoubleOr("rubber", 0);
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.powerLoad();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return 0;
        return machine.connectionMask(side);
    }

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        needPublish();
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(machine.volts(), machine.amps());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    /** "Cable kit: 16 blocks of 2.5 mm², 48 V, brown", "Winding: 120 turns of ..." or "Bare wire: 16 Copper Cable". */
    public static String jobText(int mode, int mm2, int tier, int colour, int amount) {
        if (mode == BARE) return I18N.tr("Bare wire: %1$ Copper Cable", amount);
        String what = new CableSpec(mm2, colour).label(tier) + ", " + net.minecraft.world.item.DyeColor.byId(colour).getName().replace('_', ' ');
        return mode == KIT ? I18N.tr("Cable kit: %1$ blocks of %2$", amount, what) : I18N.tr("Winding: %1$ turns of %2$", amount, what);
    }

    public static String statusText(byte status) {
        return switch (status) {
            case RUNNING -> I18N.tr("Drawing wire");
            case NO_COPPER -> I18N.tr("Needs copper (ingots or dust)");
            case NO_RUBBER -> I18N.tr("Needs rubber");
            case OUTPUT_FULL -> I18N.tr("Take the finished item out");
            case NO_POWER -> I18N.tr("No power");
            default -> I18N.tr("Stopped");
        };
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>(machine.wailaLines());
        info.put(I18N.tr("Making"), jobText(mode, mm2, tier, colour, amount));
        info.put(I18N.tr("State"), statusText(status) + (running && done > 0 ? String.format(" (%.0f%%)", progress() * 100) : ""));
        return info;
    }
}
