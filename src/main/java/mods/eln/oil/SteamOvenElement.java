package mods.eln.oil;

import mods.eln.fluid.FluidWorld;
import mods.eln.fluid.PreciseElementFluidHandler;
import mods.eln.i18n.I18N;
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
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidDevice;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.Water;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The steam oven (SteamOvenDescriptor). Steam reaches its inlet node through the port on top (as the turbine's); while it
 * has food to cook and its fan has power, it condenses what it needs for MAX_COOK_W of cooking at EFFICIENCY (or what
 * the inlet holds above 1 atm, cooking slower), 1000 J an item as the electrical oven. The condensate is drained away.
 */
public class SteamOvenElement extends TransparentNodeElement implements FluidDevice {
    public static final int inSlotId = 0, outSlotId = 1;
    static final double ENERGY_PER_ITEM = mods.eln.misc.RecipesList.electricalFurnaceEnergyPerSmelt;
    /** The inlet holds 2 s of full-speed steam at ~5 bar (kg/m^3). */
    static final double INLET_RHO = 2.5;

    final TransparentNodeElementInventory inventory = new SteamOvenInventory(2, 64, this);
    final NbtElectricalLoad electricalLoad = new NbtElectricalLoad("electricalLoad");
    final Resistor fan = new Resistor(electricalLoad, null);
    public final PreciseElementFluidHandler tank = new PreciseElementFluidHandler(
        (int) Math.ceil(2 * SteamOvenDescriptor.maxSteamKgPerS() / EaUnits.KG_PER_MB_STEAM));
    public FluidNode inlet;
    private double[] loaded;
    private int shown;
    private List<FluidPort> ports = List.of();

    double energyCounter, cookW, steamRate, meterSteam, meterTime;
    boolean cooking, powered;
    ItemStack lastIn = ItemStack.EMPTY;

    public SteamOvenElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        tank.setFilter(new Fluid[]{Oil.STEAM.get()});
        electricalLoadList.add(electricalLoad);
        electricalComponentList.add(fan);
        slowProcessList.add(process);
        slowProcessList.add(new VoltageStateWatchDog().set(electricalLoad).setUNominal(mods.eln.Eln.LVU).set(new Failure(this).machine()));
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    /** What the input cooks to (food only: it never gets hot enough to char). */
    public ItemStack result() {
        ItemStack in = inventory.getItem(inSlotId);
        if (!mods.eln.transparentnode.electricalfurnace.ElectricalFurnaceProcess.charrable(in)) return ItemStack.EMPTY;
        return Utils.smeltResult(node.coordonate.world(), in);
    }

    boolean canCook() {
        ItemStack r = result();
        if (r.isEmpty()) return false;
        ItemStack out = inventory.getItem(outSlotId);
        if (out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(out, r) && out.getCount() + r.getCount() <= Math.min(out.getMaxStackSize(), inventory.getMaxStackSize());
    }

    final IProcess process = time -> {
        if (inlet == null) return;
        ItemStack in = inventory.getItem(inSlotId);
        if (!ItemStack.isSameItemSameComponents(in, lastIn)) {
            energyCounter = 0;
            lastIn = in.copy();
        }
        powered = electricalLoad.getU() > SteamOvenDescriptor.FAN_MIN * mods.eln.Eln.LVU;
        cooking = powered && canCook();
        double got = 0;
        if (cooking) {
            double jpk = Water.hVapour(inlet.T()) - Water.hLiquid(Water.TB);
            double want = Math.min(SteamOvenDescriptor.MAX_COOK_W / SteamOvenDescriptor.EFFICIENCY * time / Math.max(1, jpk), EaUnits.steamAvailable(inlet));
            double mv = inlet.mVapour(), mg = mv + inlet.mAir;
            got = want > 0 && mv > 0 ? EaUnits.drainGas(inlet, want * mg / mv) : 0;
            cookW = SteamOvenDescriptor.EFFICIENCY * got * jpk / time;
            energyCounter += cookW * time;
            if (energyCounter >= ENERGY_PER_ITEM) {
                energyCounter -= ENERGY_PER_ITEM;
                cookOne();
            }
        } else cookW = 0;
        meterSteam += got / EaUnits.KG_PER_MB_STEAM;
        meterTime += time;
        if (meterTime >= 1) {
            steamRate = meterSteam / meterTime;
            meterSteam = meterTime = 0;
        }
        if (got > 0) {
            syncTank();
            show();
        }
    };

    void cookOne() {
        ItemStack r = result();
        if (r.isEmpty() || !canCook()) return;
        ItemStack out = inventory.getItem(outSlotId);
        if (out.isEmpty()) inventory.setItem(outSlotId, r.copy());
        else out.grow(r.getCount());
        inventory.removeItem(inSlotId, 1);
        inventory.setChanged();
        needPublish();
    }

    public double progress() {
        return cooking ? Math.min(1, energyCounter / ENERGY_PER_ITEM) : 0;
    }

    // ---- fluid (as SteamGeneratorElement: the port on top, other mods' pipes through the tank)

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public List<FluidPort> fluidPorts() {
        return ports;
    }

    void syncTank() {
        int n = tank.getFluidAmount() - shown;
        if (n > 0) EaUnits.addWater(inlet, n * EaUnits.KG_PER_MB_STEAM, Water.TB, true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void beforeFluidStep(double dt) {
        syncTank();
    }

    @Override
    public void afterFluidStep(double dt) {
        show();
    }

    private void show() {
        int n = (int) Math.min(tank.getCapacity(), Math.floor(EaUnits.steamAvailable(inlet) / EaUnits.KG_PER_MB_STEAM));
        if (tank.getFluidAmount() > 0) tank.drain(tank.getFluidAmount(), true);
        if (n > 0) tank.fill(new net.neoforged.neoforge.fluids.FluidStack(Oil.STEAM.get(), n), true);
        shown = tank.getFluidAmount();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet != null) FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        FluidWorld.remove(this);
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    // ---- the rest

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
        return lrdu == LRDU.Down && (side == front.left() || side == front.right()) ? electricalLoad : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front.left() || side == front.right()) ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public void initialize() {
        ElnDeviceRegistry.lowVoltageCableDescriptor.applyTo(electricalLoad);
        fan.setR(mods.eln.Eln.LVU * mods.eln.Eln.LVU / SteamOvenDescriptor.FAN_W);
        var c = node.coordonate;
        inlet = new FluidNode(2 * SteamOvenDescriptor.maxSteamKgPerS() / INLET_RHO, c.y, 1.0, false);
        if (loaded != null) {
            inlet.mWater = loaded[0];
            inlet.mAir = loaded[1];
            inlet.energy = loaded[2];
            inlet.update();
        } else inlet.fill(0, Water.P_ATM, mods.eln.sim.Ambient.kelvin(node.coordonate));
        show();
        ports = List.of(new FluidPort(inlet, c.pos().asLong(), 1, 0.5, SteamGeneratorElement.PORT_K));   // the inlet on top
        connect();
    }

    @Override
    public void inventoryChange(Container inventory) {
        super.inventoryChange(inventory);
        needPublish();
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(electricalLoad.getU(), electricalLoad.getI());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return inlet == null ? null : Utils.plotCelsius("T:", inlet.T() - 273.15 - mods.eln.sim.PhysicalConstant.Tamb);
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeFloat((float) progress());
            stream.writeFloat((float) (cookW / ENERGY_PER_ITEM));   // (progress a second)
            stream.writeFloat((float) (inlet == null ? 1 : inlet.P() / 1e5));
            stream.writeFloat((float) (steamRate * EaUnits.KG_PER_MB_STEAM / SteamOvenDescriptor.maxSteamKgPerS()));   // (the vent's plume)
            stream.writeFloat((float) steamRate);   // (mB/s)
            stream.writeBoolean(powered);
            Utils.serialiseItemStack(stream, Utils.isEmpty(inventory.getItem(inSlotId)) ? null : inventory.getItem(inSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        if (inlet != null) {
            syncTank();
            show();
            nbt.putDouble("inletW", inlet.mWater);
            nbt.putDouble("inletA", inlet.mAir);
            nbt.putDouble("inletU", inlet.energy);
        }
        tank.writeToNBT(nbt, "tank");
        nbt.putDouble("energyCounter", energyCounter);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        energyCounter = nbt.getDoubleOr("energyCounter", 0);
        lastIn = inventory.getItem(inSlotId).copy();
        if (nbt.contains("inletW"))
            loaded = new double[]{nbt.getDoubleOr("inletW", 0), nbt.getDoubleOr("inletA", 0), nbt.getDoubleOr("inletU", 0)};
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        if (inlet != null) info.put(I18N.tr("Inlet pressure"), String.format("%.2f bar", inlet.P() / 1e5));
        info.put(I18N.tr("Steam used"), String.format("%.0f mB/s", steamRate));
        info.put(I18N.tr("Cooking"), cooking ? Utils.plotPower("", cookW) : I18N.tr("No"));
        return info;
    }
}
