package mods.eln.anvil;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.electricalinterface.IItemEnergyBattery;
import mods.eln.item.electricalinterface.IItemEnergySelfManaged;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * The electrical anvil (ElectricalAnvilDescriptor; Baughn 2026-10-28). The tool sits in slot TOOL and is repaired in
 * place, a step at a time: each step restores a unit's worth of durability (a quarter, as vanilla's anvil) or what's
 * missing, takes joulesPerUnit for a whole unit (pro rata for less), and uses the material's credit first. The desk
 * keeps what's left of a consumed unit as credit for that material (so topping a tool up wastes nothing), saved with
 * it. A repair never raises the tool's anvil penalty (REPAIR_COST) and anneals it a step (AnvilRules.anneal). No XP.
 * Repair only: the material must be the tool's repair item or an unenchanted copy of the tool; books and enchanted
 * copies are refused, so it can't be used to combine enchantments. Renaming (engraving) is a small pulse.
 * <p>
 * Power: while it works it is a resistor drawing its rating at 800 V. It switches in only when the line is at RESUME
 * of 800 V and drops out below SAG, keeping its progress, so a weak supply with a capacitor bank charges up between
 * pulses (Baughn: max out the 800 V input to encourage capacitors and inductors).
 */
public class ElectricalAnvilElement extends TransparentNodeElement {
    public static final int TOOL = 0, MATERIAL = 1;
    public static final byte SET_NAME = 1;
    /** What it's doing, for the screen and Jade. */
    public static final byte EMPTY = 0, CANT = 1, DONE = 2, NO_MATERIAL = 3, WAITING = 4, SAGGING = 5, WELDING = 6, ENGRAVING = 7,
        RECOVERING = 8;
    static final int MAX_NAME = 50;   // (vanilla's anvil)

    final TransparentNodeElementInventory inventory = new ElectricalAnvilInventory(2, 64, this);
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    final Resistor arc = new Resistor(load, null);
    /** Credit per material (item id): fractions of a unit left from consumed material. */
    public final Map<String, Double> credit = new TreeMap<>();
    /** The name asked for in the screen, or null: engraved at the next pulse. */
    public String pendingName;
    /** Joules into the current step; whether the welder is switched in; what it's doing. */
    public double energy;
    public boolean welding;
    public byte status = EMPTY;
    /** Seconds before it may try again after a sag (a line that can't carry it would otherwise chatter on and off). */
    double holdOff;
    static final double HOLD_OFF_S = 1;
    /** Joules into the step when the current burst began; whether it's resting between bursts. */
    public double burstStart;
    /** The line's voltage last tick, and how long it has been level (not rising), for the plateau start. */
    double lastU, levelFor;
    public boolean recovering;
    String jobKey = "";
    Job current;

    /** A step: an engraving, or a repair restoring `restore` with `units` of `material` (null for a copy of the tool). */
    public record Job(boolean rename, int restore, double units, String material, boolean copy, double joules) {
    }

    public ElectricalAnvilElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        electricalLoadList.add(load);
        electricalComponentList.add(arc);
        slowProcessList.add(process);
        slowProcessList.add(new VoltageStateWatchDog().set(load).setUNominal(Eln.HVU).set(new Failure(this).machine()));
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    // ---- what to do

    static String key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    static Item item(String key) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(key));
    }

    /** Items whose bar is their charge, not wear (EA's tools and batteries, Stasis suits): not the desk's to mend. */
    public static boolean excluded(ItemStack s) {
        Object o = Utils.getItemObject(s);
        return s.getItem() instanceof IItemEnergyBattery || s.getItem() instanceof IItemEnergySelfManaged
            || o instanceof IItemEnergyBattery || o instanceof IItemEnergySelfManaged;
    }

    /**
     * What the material slot takes (the screen and hoppers): nothing enchanted, no books. The desk repairs and never
     * combines, so refusing them at the slot says so (AdventurAgent: they went in and were just ignored).
     */
    public static boolean materialMayGo(ItemStack s) {
        return !s.has(DataComponents.STORED_ENCHANTMENTS) && !EnchantmentHelper.hasAnyEnchantments(s);
    }

    /** An unenchanted copy of the tool counts as its material (vanilla's two-tools repair, without the enchantments). */
    public static boolean plainCopy(ItemStack tool, ItemStack m) {
        return !m.isEmpty() && m.is(tool.getItem()) && m.isDamageableItem() && !EnchantmentHelper.hasAnyEnchantments(m)
            && !m.has(DataComponents.STORED_ENCHANTMENTS);
    }

    static String nameOf(ItemStack s) {
        Component c = s.get(DataComponents.CUSTOM_NAME);
        return c == null ? "" : c.getString();
    }

    /** The next step for what's in the slots, or null (status says why). */
    public Job job() {
        ItemStack tool = inventory.getItem(TOOL);
        if (tool.isEmpty()) {
            pendingName = null;   // (a name typed for one tool isn't for the next)
            return idle(EMPTY);
        }
        if (excluded(tool)) return idle(CANT);
        if (pendingName != null && !pendingName.equals(nameOf(tool)))
            return new Job(true, 0, 0, null, false, ElectricalAnvilDescriptor.joulesPerName);
        if (!tool.isDamageableItem()) return idle(CANT);
        if (!tool.isDamaged()) return idle(DONE);
        int max = tool.getMaxDamage(), damage = tool.getDamageValue();
        ItemStack m = inventory.getItem(MATERIAL);
        if (plainCopy(tool, m)) {
            int restore = Math.min(damage, m.getMaxDamage() - m.getDamageValue() + max * 12 / 100);
            if (restore <= 0) return idle(NO_MATERIAL);
            double units = AnvilRules.unitsFor(restore, max);
            return new Job(false, restore, units, null, true, ElectricalAnvilDescriptor.joulesPerUnit * units);
        }
        String id = !m.isEmpty() && tool.isValidRepairItem(m) ? key(m.getItem()) : null;
        if (id == null)
            for (var e : credit.entrySet())
                if (e.getValue() > 1e-9 && tool.isValidRepairItem(new ItemStack(item(e.getKey())))) {
                    id = e.getKey();
                    break;
                }
        if (id == null) return idle(NO_MATERIAL);
        double have = credit.getOrDefault(id, 0.0);
        int inSlot = !m.isEmpty() && key(m.getItem()).equals(id) ? m.getCount() : 0;
        int restore = AnvilRules.stepRestore(damage, max);
        double units = AnvilRules.unitsFor(restore, max);
        if (have + inSlot < units) {   // (not enough for the whole step: as much as there is)
            restore = (int) Math.floor((have + inSlot) * AnvilRules.unit(max) + 1e-9);
            units = AnvilRules.unitsFor(restore, max);
        }
        if (restore <= 0) return idle(NO_MATERIAL);
        return new Job(false, restore, units, id, false, ElectricalAnvilDescriptor.joulesPerUnit * units);
    }

    private Job idle(byte why) {
        status = why;
        return null;
    }

    /** Finish a step (public for GameTests): repair or engrave, take the material, keep the change as credit, anneal. */
    public void complete(Job j) {
        ItemStack tool = inventory.getItem(TOOL);
        if (j.rename()) {
            if (pendingName == null || pendingName.isBlank()) tool.remove(DataComponents.CUSTOM_NAME);
            else tool.set(DataComponents.CUSTOM_NAME, Component.literal(pendingName));
            pendingName = null;
        } else {
            if (j.copy()) inventory.removeItem(MATERIAL, 1);
            else {
                double have = credit.getOrDefault(j.material(), 0.0);
                int take = AnvilRules.unitsToConsume(have, j.units());
                if (take > 0) inventory.removeItem(MATERIAL, take);
                double left = have + take - j.units();
                if (left > 1e-6) credit.put(j.material(), left);
                else credit.remove(j.material());
            }
            tool.setDamageValue(Math.max(0, tool.getDamageValue() - j.restore()));
            int ench = EnchantmentHelper.getEnchantmentsForCrafting(tool).size();
            tool.set(DataComponents.REPAIR_COST, AnvilRules.anneal(tool.getOrDefault(DataComponents.REPAIR_COST, 0), ench));
        }
        inventory.setItem(TOOL, tool);
        inventory.setChanged();
        needPublish();
    }

    // ---- the pulse

    public final IProcess process = dt -> {
        Job j = job();
        String k = j == null ? "" : (j.rename() ? "name:" : "fix:") + key(inventory.getItem(TOOL).getItem());
        if (!k.equals(jobKey)) {   // (another tool, or another kind of job: start the step afresh)
            jobKey = k;
            energy = burstStart = 0;
            recovering = false;
        }
        current = j;
        double u = load.getU();
        holdOff = Math.max(0, holdOff - dt);
        // (a line that has stopped rising: it won't get any higher, so start if there's room above the stop)
        double rate = (u - lastU) / Math.max(1e-6, dt);
        lastU = u;
        levelFor = welding || rate > ElectricalAnvilDescriptor.PLATEAU_V_PER_S ? 0 : levelFor + dt;
        boolean ready = u >= ElectricalAnvilDescriptor.resume * Eln.HVU
            || levelFor >= ElectricalAnvilDescriptor.PLATEAU_S && u >= ElectricalAnvilDescriptor.plateau * Eln.HVU;
        if (j == null) welding = false;
        else if (!welding && holdOff <= 0 && ready) {
            welding = true;
            recovering = false;
        }
        else if (welding && u < ElectricalAnvilDescriptor.sag * Eln.HVU) {
            welding = false;
            holdOff = HOLD_OFF_S;
        }
        double r = ElectricalAnvilDescriptor.resistance();
        arc.setR(welding ? r : mods.eln.sim.mna.misc.MnaConst.highImpedance);
        if (j == null) return;
        if (welding) {
            status = j.rename() ? ENGRAVING : WELDING;
            energy += u * u / r * dt;
            if (energy >= j.joules()) {
                complete(j);
                energy = burstStart = 0;
            } else if (energy - burstStart >= ElectricalAnvilDescriptor.burstJoules()) {
                // a unit's weld comes in bursts with a rest between, so a bank (or a battery bank) can recover (Baughn)
                welding = false;
                recovering = true;
                status = RECOVERING;
                holdOff = ElectricalAnvilDescriptor.burstGapS;
                burstStart = energy;
                arc.setR(mods.eln.sim.mna.misc.MnaConst.highImpedance);
            }
        } else status = recovering ? RECOVERING
            : holdOff > 0 || (energy > 0 && u < ElectricalAnvilDescriptor.sag * Eln.HVU) ? SAGGING : WAITING;
    };

    /** The current step's progress (0..1). */
    public double progress() {
        return current == null || current.joules() <= 0 ? 0 : Math.min(1, energy / current.joules());
    }

    /** Power the welder takes now (W). */
    public double powerNow() {
        double u = load.getU();
        return welding ? u * u / ElectricalAnvilDescriptor.resistance() : 0;
    }

    // ---- the rest

    @Override
    public byte networkUnserialize(DataInputStream stream) {
        byte type = super.networkUnserialize(stream);
        if (type == SET_NAME) {
            try {
                String n = stream.readUTF().strip();
                pendingName = n.length() > MAX_NAME ? n.substring(0, MAX_NAME) : n;
                needPublish();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return type;
        }
        return type;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front.back() ? load : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && side == front.back() ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public void initialize() {
        ElnDeviceRegistry.highVoltageCableDescriptor.applyTo(load);
        arc.highImpedance();
        connect();
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        needPublish();
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(status);
            stream.writeFloat((float) progress());
            stream.writeFloat((float) powerNow());
            stream.writeFloat((float) load.getU());
            stream.writeBoolean(welding);
            stream.writeUTF(pendingName == null ? "\u0000" : pendingName);
            stream.writeByte(credit.size());
            for (var e : credit.entrySet()) {
                stream.writeUTF(e.getKey());
                stream.writeFloat(e.getValue().floatValue());
            }
            ItemStack tool = inventory.getItem(TOOL);
            stream.writeInt(tool.getOrDefault(DataComponents.REPAIR_COST, 0));
            stream.writeInt(AnvilRules.penaltyFloor(EnchantmentHelper.getEnchantmentsForCrafting(tool).size()));
            Utils.serialiseItemStack(stream, tool.isEmpty() ? null : tool);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        CompoundTag c = new CompoundTag();
        credit.forEach(c::putDouble);
        nbt.put("credit", c);
        nbt.putDouble("energy", energy);
        nbt.putDouble("burstStart", burstStart);
        if (pendingName != null) nbt.putString("pendingName", pendingName);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        credit.clear();
        CompoundTag c = nbt.getCompoundOrEmpty("credit");
        for (String k : c.keySet()) credit.put(k, c.getDoubleOr(k, 0));
        energy = nbt.getDoubleOr("energy", 0);
        burstStart = nbt.getDoubleOr("burstStart", 0);
        pendingName = nbt.getString("pendingName").orElse(null);
        ItemStack tool = inventory.getItem(TOOL);
        Job j = job();
        jobKey = j == null ? "" : (j.rename() ? "name:" : "fix:") + key(tool.getItem());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("State"), statusText(status, load.getU()));
        if (welding) info.put(I18N.tr("Power"), Utils.plotPower("", powerNow()));
        if (!credit.isEmpty()) info.put(I18N.tr("Credit"), creditText(credit));
        return info;
    }

    /** The state in words (shared with the screen). */
    public static String statusText(byte status, double u) {
        return switch (status) {
            case EMPTY -> I18N.tr("Put a tool on the bench");
            case CANT -> I18N.tr("This can't be mended here");
            case DONE -> I18N.tr("Repaired");
            case NO_MATERIAL -> I18N.tr("Needs its repair material");
            case WAITING -> I18N.tr("Waiting for %1$V (now %2$V)", String.format("%.0f", ElectricalAnvilDescriptor.resume * Eln.HVU), String.format("%.0f", u));
            case SAGGING -> I18N.tr("Supply too weak (%1$V): paused", String.format("%.0f", u));
            case WELDING -> I18N.tr("Welding");
            case ENGRAVING -> I18N.tr("Engraving");
            case RECOVERING -> I18N.tr("Between bursts: waiting for %1$V (now %2$V)", String.format("%.0f", ElectricalAnvilDescriptor.resume * Eln.HVU), String.format("%.0f", u));
            default -> "";
        };
    }

    /** Credit per material, e.g. "Diamond 0.60" (item names in the reader's language where it can). */
    public static String creditText(Map<String, Double> credit) {
        StringBuilder b = new StringBuilder();
        for (var e : credit.entrySet()) {
            if (b.length() > 0) b.append(", ");
            b.append(new ItemStack(item(e.getKey())).getHoverName().getString()).append(String.format(" %.2f", e.getValue()));
        }
        return b.toString();
    }
}
