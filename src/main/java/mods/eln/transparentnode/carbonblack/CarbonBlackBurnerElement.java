package mods.eln.transparentnode.carbonblack;

import mods.eln.fluid.ElementFluidHandler;
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
import net.minecraft.world.level.material.Fluid;
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
    final ElementFluidHandler tank = new ElementFluidHandler(CarbonBlackBurnerDescriptor.TANK_MB);
    final PortBridge inPort = new PortBridge("fuelIn", PortBridge.Mode.IN, 200, 0);
    /** Fuel burnt toward the next Carbon Black (mB), and those made but not yet taken. */
    double soot;
    int carbon;
    /** Fuel burnt in all (mB; tests, Jade), and the fraction of a mB burnt but not yet drained. */
    double fuelBurnt, drainCarry;
    boolean burning;
    double sincePush;

    public CarbonBlackBurnerElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        tank.setFilter(new Fluid[]{mods.eln.oil.Oil.Cut.RESIDUE.source.get(), mods.eln.oil.Oil.Cut.HEAVYOIL.source.get(), mods.eln.oil.Oil.CRUDE.get()});
        slowProcessList.add(this::tick);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
    }

    void tick(double dt) {
        boolean was = burning;
        burning = false;
        double want = CarbonBlackBurnerDescriptor.BURN_MB_PER_S * dt;
        if (carbon < CarbonBlackBurnerDescriptor.MAX_CARBON && tank.getFluidAmount() >= Math.ceil(want)) {
            soot += want;
            fuelBurnt += want;
            drainCarry += want;
            int whole = (int) Math.floor(drainCarry);   // (whole mB drained; the fraction waits for the next tick)
            burning = true;
            if (whole > 0) {
                drainCarry -= whole;
                tank.drain(Math.min(whole, tank.getFluidAmount()), true);
            }
            while (soot >= CarbonBlackBurnerDescriptor.MB_PER_CARBON) {
                soot -= CarbonBlackBurnerDescriptor.MB_PER_CARBON;
                carbon++;
            }
        }
        if (burning != was) needPublish();
        sincePush += dt;
        if (sincePush >= 1 && carbon > 0) {
            sincePush = 0;
            pushBelow();
        }
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
        if (inPort.stub == null) return java.util.List.of();
        return java.util.List.of(inPort.port(node.coordonate.pos().asLong(), back().toEnumFacing().get3DDataValue(),
            PortBridge.endSpot(front, back(), CarbonBlackBurnerDescriptor.IN_Y, CarbonBlackBurnerDescriptor.IN_Z)));
    }

    @Override
    public void beforeFluidStep(double dt) {
        inPort.step(tank, dt, mods.eln.sim.Ambient.kelvin(node.coordonate));
    }

    @Override
    public void initialize() {
        inPort.make(node.coordonate.y + CarbonBlackBurnerDescriptor.IN_Y / 16.0, mods.eln.sim.Ambient.kelvin(node.coordonate));
        connect();
    }

    @Override
    public void connectJob() {
        super.connectJob();
        if (inPort.stub != null) mods.eln.fluid.FluidWorld.add(this);
    }

    @Override
    public void disconnectJob() {
        super.disconnectJob();
        mods.eln.fluid.FluidWorld.remove(this);
    }

    @Override
    public Object getFluidHandler() {
        return tank;
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
        tank.writeToNBT(nbt, "tank");
        inPort.write(nbt);
        nbt.putDouble("soot", soot);
        nbt.putInt("carbon", carbon);
        nbt.putDouble("fuelBurnt", fuelBurnt);   // (not "burnt": the element's burnt-device flag)
        nbt.putDouble("drainCarry", drainCarry);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
        inPort.read(nbt);
        soot = nbt.getDoubleOr("soot", 0);
        carbon = nbt.getIntOr("carbon", 0);
        fuelBurnt = nbt.getDoubleOr("fuelBurnt", 0);
        drainCarry = nbt.getDoubleOr("drainCarry", 0);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Fuel"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Carbon Black"), carbon + (carbon >= CarbonBlackBurnerDescriptor.MAX_CARBON ? " " + I18N.tr("(full: take it)") : ""));
        info.put(I18N.tr("State"), burning ? I18N.tr("burning") : I18N.tr("out of fuel"));
        return info;
    }
}
