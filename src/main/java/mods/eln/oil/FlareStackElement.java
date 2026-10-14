package mods.eln.oil;

import mods.eln.fluid.ElementFluidHandler;
import mods.eln.fluid.FuelRegistry;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** FlareStackDescriptor: burns its tank down at up to BURN_RATE; tells the client what burns, for the visuals. */
public class FlareStackElement extends TransparentNodeElement {
    public final ElementFluidHandler tank = new ElementFluidHandler(FlareStackDescriptor.TANK_MB);
    public double burnRate;   // mB/s, averaged over whole seconds
    double carry, meterBurnt, meterTime;
    String burning = "";

    public FlareStackElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 1, 0.5));
        slowProcessList.add(new Burn());
    }

    class Burn implements IProcess {
        @Override
        public void process(double time) {
            double want = FlareStackDescriptor.BURN_RATE * time + carry;
            int n = (int) Math.min(Math.floor(want), tank.getFluidAmount());
            carry = n > 0 ? want - Math.floor(want) : 0;
            String what = n > 0 ? FuelRegistry.INSTANCE.fluidName(tank.getFluid().getFluid()) : "";
            if (n > 0) tank.drain(n, true);
            meterBurnt += n;
            meterTime += time;
            if (meterTime >= 1) {
                burnRate = meterBurnt / meterTime;
                meterBurnt = 0;
                meterTime = 0;
            }
            if (!what.equals(burning)) {
                burning = what;
                needPublish();
            }
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(burning);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tank;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tank.writeToNBT(nbt, "tank");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tank.readFromNBT(nbt, "tank");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        var f = tank.getFluid();
        info.put(I18N.tr("Tank"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB" + (f.isEmpty() ? "" : " " + f.getHoverName().getString()));
        info.put(I18N.tr("Burning"), String.format("%.0f mB/s", burnRate));
        return info;
    }
}
