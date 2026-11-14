package mods.eln.transparentnode.carbonblack;

import mods.eln.fluid.DeviceInlet;
import mods.eln.fluid.PortBridge;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** The Carbon Black Burner (CarbonBlackBurnerDescriptor). */
public class CarbonBlackBurnerElement extends TransparentNodeElement implements mods.eln.sim.fluid.FluidDevice {
    /** Its fuel vessel, filled by the line on its IN flange: residue, heavy oil or crude. */
    public final DeviceInlet inlet = new DeviceInlet("fuelIn", CarbonBlackBurnerDescriptor.TANK_MB,
        mods.eln.oil.Oil.Cut.RESIDUE.source.get(), mods.eln.oil.Oil.Cut.HEAVYOIL.source.get(), mods.eln.oil.Oil.CRUDE.get());
    /** Fuel burnt toward the next Carbon Black (mB), and those made but not yet taken. */
    double soot;
    int carbon;
    /** Fuel burnt in all (mB; tests, Jade). */
    double fuelBurnt;
    boolean burning;
    double sincePush;

    public CarbonBlackBurnerElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    void tick(double dt) {
        sincePush += dt;
        if (sincePush >= 1 && carbon > 0) {
            sincePush = 0;
            pushBelow();
        }
    }

    /** Burn a step's fuel out of the vessel, if it holds that much and there is room for the Carbon Black. */
    void burn(double dt) {
        boolean was = burning;
        burning = false;
        double want = CarbonBlackBurnerDescriptor.BURN_MB_PER_S * dt;
        if (carbon < CarbonBlackBurnerDescriptor.MAX_CARBON && inlet.feedMb() >= want) {
            double got = inlet.take(want);
            soot += got;
            fuelBurnt += got;
            burning = true;
            while (soot >= CarbonBlackBurnerDescriptor.MB_PER_CARBON) {
                soot -= CarbonBlackBurnerDescriptor.MB_PER_CARBON;
                carbon++;
            }
        }
        if (burning != was) needPublish();
    }

    /** Into a hopper under it (a transparent node stands on a full block or a hopper), as many as it takes. */
    void pushBelow() {
        var level = node.coordonate.world();
        if (level == null) return;
        var h = level.getCapability(Capabilities.Item.BLOCK, node.coordonate.pos().below(), net.minecraft.core.Direction.UP);
        if (h == null) return;
        try (Transaction tx = Transaction.openRoot()) {
            int got = ResourceHandlerUtil.insertStacking(h, ItemResource.of(new ItemStack(mods.eln.registry.ElnRegistry.CARBON_BLACK.get())), carbon, tx);
            tx.commit();
            carbon -= got;
        }
    }

    /** Carbon Black waiting in it (tests). */
    public int carbon() {
        return carbon;
    }

    public double burnt() {
        return fuelBurnt;
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
        if (carbon <= 0 || node.coordonate.world().isClientSide()) return carbon > 0;
        ItemStack s = new ItemStack(mods.eln.registry.ElnRegistry.CARBON_BLACK.get(), carbon);
        carbon = 0;
        if (!player.getInventory().add(s)) {
            var p = player.position();
            net.minecraft.world.Containers.dropItemStack(player.level(), p.x, p.y, p.z, s);
        }
        return true;
    }

    // ---- fluid: fuel in at the blue flange on its back

    Direction back() {
        return front.back();
    }

    @Override
    public Object fluidLevel() {
        return node.coordonate.dimention;
    }

    @Override
    public java.util.List<FluidPort> fluidPorts() {
        if (inlet.stub == null) return java.util.List.of();
        return java.util.List.of(inlet.port(node.coordonate.pos().asLong(), back().toEnumFacing().get3DDataValue(),
            PortBridge.endSpot(front, back(), CarbonBlackBurnerDescriptor.IN_Y, CarbonBlackBurnerDescriptor.IN_Z)));
    }

    @Override
    public java.util.List<Link> fluidLinks() {
        return inlet.links();
    }

    @Override
    public java.util.List<Vent> fluidVents() {
        return inlet.vents();
    }

    /** The burn is here, between the view's sync and show: a change in the tick would leave the view stale. */
    @Override
    public void beforeFluidStep(double dt) {
        inlet.sync(dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
        burn(dt);
        inlet.settle(dt);
    }

    @Override
    public void afterFluidStep(double dt) {
        inlet.after();
    }

    @Override
    public void initialize() {
        inlet.make(node.coordonate.y, node.coordonate.y + CarbonBlackBurnerDescriptor.IN_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    /** Put mB of fuel straight into its vessel (tests). */
    public void putFuel(net.minecraft.world.level.material.Fluid fluid, int mB) {
        inlet.put(fluid, mB, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inlet.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public Object getFluidHandler() {
        return inlet.view;
    }

    // ---- no electricity, no heat

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return null;
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeBoolean(burning);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        inlet.write(nbt);
        nbt.putDouble("soot", soot);
        nbt.putInt("carbon", carbon);
        nbt.putDouble("fuelBurnt", fuelBurnt);   // (not "burnt": the element's burnt-device flag)
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        inlet.read(nbt, "tank", CarbonBlackBurnerDescriptor.TANK_MB);
        soot = nbt.getDoubleOr("soot", 0);
        carbon = nbt.getIntOr("carbon", 0);
        fuelBurnt = nbt.getDoubleOr("fuelBurnt", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Fuel"), Math.round(inlet.feedMb()) + " / " + CarbonBlackBurnerDescriptor.TANK_MB + " mB");
        info.put(I18N.tr("Carbon Black"), carbon + (carbon >= CarbonBlackBurnerDescriptor.MAX_CARBON ? " " + I18N.tr("(full: take it)") : ""));
        String wrong = burning ? null : inlet.blockedBy();
        info.put(I18N.tr("State"), burning ? I18N.tr("burning") : wrong != null ? I18N.tr("blocked: it holds %1$, which it can't burn", wrong) : I18N.tr("out of fuel"));
        return info;
    }
}
