package mods.eln.sixnode.powercapacitorsix;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.init.Cable;
import mods.eln.init.Config;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Capacitor;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.BipoleVoltageWatchdog;
import mods.eln.sim.process.destruct.WorldExplosion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PowerCapacitorSixElement extends SixNodeElement {

    PowerCapacitorSixDescriptor descriptor;
    NbtElectricalLoad positiveLoad = new NbtElectricalLoad("positiveLoad");
    NbtElectricalLoad negativeLoad = new NbtElectricalLoad("negativeLoad");

    Capacitor capacitor = new Capacitor(positiveLoad, negativeLoad);
    Resistor dischargeResistor = new Resistor(positiveLoad, negativeLoad);
    PunkProcess punkProcess = new PunkProcess();
    BipoleVoltageWatchdog watchdog = new BipoleVoltageWatchdog().set(capacitor);

    double stdDischargeResistor;

    boolean fromNbt = false;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public PowerCapacitorSixElement(SixNode SixNode, Direction side, SixNodeDescriptor descriptor) {
        super(SixNode, side, descriptor);
        this.descriptor = (PowerCapacitorSixDescriptor) descriptor;

        electricalLoadList.add(positiveLoad);
        electricalLoadList.add(negativeLoad);
        electricalComponentList.add(capacitor);
        electricalComponentList.add(dischargeResistor);
        electricalProcessList.add(punkProcess);
        slowProcessList.add(watchdog);

        watchdog.set(new WorldExplosion(this).machineExplosion());
        positiveLoad.setAsMustBeFarFromInterSystem();
    }

    class PunkProcess implements IProcess {
        double eLeft = 0;
        double eLegaliseResistor;

        @Override
        public void process(double time) {
            if (eLeft <= 0) {
                eLeft = 0;
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                eLeft -= dischargeResistor.getP() * time;
                dischargeResistor.setR(eLegaliseResistor);
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (lrdu == front.right()) return positiveLoad;
        if (lrdu == front.left()) return negativeLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (lrdu == front.right()) return NodeBase.maskElectricalPower;
        if (lrdu == front.left()) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotVolt("U", Math.abs(capacitor.getU())) + Utils.plotAmpere("I", capacitor.getCurrent());
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Capacity"), Utils.plotValue(capacitor.getC(), "F"));
        info.put(I18N.tr("Charge"), Utils.plotEnergy("", capacitor.getE()));
        if (Config.INSTANCE.getWailaEasyMode()) {
            info.put(I18N.tr("Voltage drop"), Utils.plotVolt("", Math.abs(capacitor.getU())));
            info.put(I18N.tr("Current"), Utils.plotAmpere("", Math.abs(capacitor.getCurrent())));

        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        Cable.applySmallRs(positiveLoad);
        Cable.applySmallRs(negativeLoad);

        setupPhysical();
    }

    @Override
    public void inventoryChanged() {
        super.inventoryChanged();
        setupPhysical();
    }

    public void setupPhysical() {
        double eOld = capacitor.getE();
        capacitor.setC(descriptor.getCValue(inventory));
        stdDischargeResistor = descriptor.dischargeTao / capacitor.getC();

        watchdog.setUNominal(descriptor.getUNominalValue(inventory));
        punkProcess.eLegaliseResistor = Math.pow(descriptor.getUNominalValue(inventory), 2) / 400;

        if (fromNbt) {
            dischargeResistor.setR(stdDischargeResistor);
            fromNbt = false;
        } else {
            double deltaE = capacitor.getE() - eOld;
            punkProcess.eLeft += deltaE;
            if (deltaE < 0) {
                dischargeResistor.setR(stdDischargeResistor);
            } else {
                dischargeResistor.setR(punkProcess.eLegaliseResistor);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setDouble("punkELeft", punkProcess.eLeft);
        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        punkProcess.eLeft = nbt.getDouble("punkELeft");
        if (Double.isNaN(punkProcess.eLeft)) punkProcess.eLeft = 0;
        fromNbt = true;
    }

    public void networkSerialize(java.io.DataOutputStream stream) {
        super.networkSerialize(stream);
        /*
		 * try {
		 * 
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */
    }

    @Override
    public IInventory getInventory() {
        return inventory;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new PowerCapacitorSixContainer(player, inventory);
    }
}
