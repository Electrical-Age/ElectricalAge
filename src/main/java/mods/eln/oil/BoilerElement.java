package mods.eln.oil;

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
import mods.eln.sim.nbt.NbtThermalLoad;
import mods.eln.sim.process.destruct.Failure;
import mods.eln.sim.process.destruct.ThermalLoadWatchDog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** BoilerDescriptor: a heat-capacity thermal load on its sides that boils piped-in water to steam. */
public class BoilerElement extends TransparentNodeElement {
    final BoilerDescriptor desc;
    public final NbtThermalLoad thermal = new NbtThermalLoad("thermal");
    public final BoilerTanks tanks = new BoilerTanks();
    public double steamRate;   // mB/s, for WAILA
    double carry, steamCarry, meterSteam, meterTime;

    public BoilerElement(TransparentNode node, TransparentNodeDescriptor d) {
        super(node, d);
        desc = (BoilerDescriptor) d;
        thermalLoadList.add(thermal);
        thermalFastProcessList.add(new Boil());
        slowProcessList.add(new NodePeriodicPublishProcess(node, 2, 1));
        // dry and very hot: it fails (no explosion this round)
        slowProcessList.add(new ThermalLoadWatchDog().set(thermal).setTMax(400).set(new Failure(this).machine()));
    }

    class Boil implements IProcess {
        @Override
        public void process(double time) {
            // EA's thermal temperatures are relative to the ambient (Tref): boiling is at 100 degrees Celsius
            double celsius = thermal.Tc + mods.eln.sim.PhysicalConstant.Tref - mods.eln.sim.PhysicalConstant.TCelsius;
            double over = celsius - BoilerDescriptor.BOIL_C;
            double made = 0;
            if (over > 0 && tanks.water() > 0) {
                double energy = over * thermal.C;
                double water = energy / BoilerDescriptor.J_PER_MB_WATER + carry;
                int room = (int) ((BoilerTanks.STEAM_MB - tanks.steam()) / BoilerDescriptor.STEAM_PER_WATER);
                int w = (int) Math.min(Math.floor(water), Math.min(tanks.water(), room));
                carry = Math.min(1, water - Math.floor(water));
                if (w > 0) {
                    double st = w * BoilerDescriptor.STEAM_PER_WATER + steamCarry;   // 67.5 per mB: carry the half
                    tanks.boil(w, (int) Math.floor(st));
                    steamCarry = st - Math.floor(st);
                    thermal.Tc -= w * BoilerDescriptor.J_PER_MB_WATER / thermal.C;
                    made = w * BoilerDescriptor.STEAM_PER_WATER;
                }
            }
            // the meter: steam made over whole seconds (boiling comes in 67.5 mB lumps, too noisy per step)
            meterSteam += made;
            meterTime += time;
            if (meterTime >= 1) {
                steamRate = meterSteam / meterTime;
                meterSteam = 0;
                meterTime = 0;
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return thermal;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down || side == Direction.YN || side == Direction.YP) return 0;
        return NodeBase.maskThermal;
    }

    @Override
    public String multiMeterString(Direction side) {
        return "";
    }

    @Override
    public String thermoMeterString(Direction side) {
        return Utils.plotCelsius("T: ", thermal.Tc);
    }

    @Override
    public void initialize() {
        // a well-insulated kettle: 8 B of water is ~540 g in EA's scale (~2.3 kJ/K) plus the shell, so ~3 kJ/K;
        // ~80 W lost at 100 degrees. From cold, a 1 kW heater brings it to the boil in ~5 minutes.
        thermal.set(0.01, 1.0, 3000);
        connect();
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public Object getFluidHandler() {
        return tanks;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        tanks.writeToNBT(nbt, "boiler");
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        tanks.readFromNBT(nbt, "boiler");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<>();
        info.put(I18N.tr("Temperature"), Utils.plotCelsius("", thermal.Tc));
        info.put(I18N.tr("Water"), tanks.water() + " / " + BoilerTanks.WATER_MB + " mB");
        info.put(I18N.tr("Steam"), tanks.steam() + " / " + BoilerTanks.STEAM_MB + " mB");
        info.put(I18N.tr("Steam made"), String.format("%.0f mB/s", steamRate));
        return info;
    }
}
