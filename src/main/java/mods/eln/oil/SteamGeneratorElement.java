package mods.eln.oil;

import mods.eln.fluid.FuelRegistry;
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
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.PowerSource;
import mods.eln.sim.nbt.NbtElectricalLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

/**
 * SteamGeneratorDescriptor: steam in through the fluid capability, electricity out of the front and back (like the
 * fuel generator). The power source offers what the steam in the tank can give over the next second, up to nominal;
 * the steam actually used follows the power the load really takes, so an idle turbine keeps its steam.
 */
public class SteamGeneratorElement extends TransparentNodeElement {
    final SteamGeneratorDescriptor desc;
    public final NbtElectricalLoad load = new NbtElectricalLoad("load");
    public final PowerSource source = new PowerSource("source", load);
    public final PreciseElementFluidHandler tank;
    final double jPerMb = FuelRegistry.INSTANCE.heatEnergyPerMilliBucket("steam");
    public double steamRate;   // mB/s used, averaged over whole seconds
    double meterSteam, meterTime;

    public SteamGeneratorElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (SteamGeneratorDescriptor) d;
        tank = new PreciseElementFluidHandler((int) Math.ceil(desc.nominalSteam() * SteamGeneratorDescriptor.TANK_SECONDS));
        tank.setFilter(new Fluid[]{Oil.STEAM.get()});
        electricalLoadList.add(load);
        electricalComponentList.add(source);
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        slowProcessList.add(new Run());
    }

    class Run implements IProcess {
        @Override
        public void process(double time) {
            // last step's output came from the tank
            double used = source.getEffectiveP() * time / (SteamGeneratorDescriptor.EFFICIENCY * jPerMb);
            if (used > 0) used = tank.drain(used);
            meterSteam += used;
            meterTime += time;
            if (meterTime >= 1) {
                steamRate = meterSteam / meterTime;
                meterSteam = 0;
                meterTime = 0;
            }
            double offer = tank.getFluidAmount() * SteamGeneratorDescriptor.EFFICIENCY * jPerMb / SteamGeneratorDescriptor.RESPONSE_SECONDS;
            source.setP(Math.min(desc.nominalP, offer));
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? load : null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return lrdu == LRDU.Down && (side == front || side == front.back()) ? NodeBase.maskElectricalPower : 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(load.getU(), load.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        desc.cable.applyTo(load);
        source.setUmax(desc.maxU);
        source.setImax(desc.nominalP * 5 / desc.maxU);
        connect();
    }

    @Override
    public boolean onBlockActivated(net.minecraft.world.entity.player.Player player, Direction side, float vx, float vy, float vz) {
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
        info.put(I18N.tr("Steam"), tank.getFluidAmount() + " / " + tank.getCapacity() + " mB");
        info.put(I18N.tr("Steam used"), String.format("%.0f mB/s", steamRate));
        info.put(I18N.tr("Generated power"), Utils.plotPower("", source.getEffectiveP()));
        info.put(I18N.tr("Voltage"), Utils.plotVolt("", load.getU()));
        return info;
    }
}
