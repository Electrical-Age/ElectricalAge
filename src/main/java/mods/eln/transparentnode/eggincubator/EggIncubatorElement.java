package mods.eln.transparentnode.eggincubator;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.Failure;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EggIncubatorElement extends TransparentNodeElement {

    public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    public Resistor powerResistor = new Resistor(powerLoad, null);
    TransparentNodeElementInventory inventory = new EggIncubatorInventory(1, 64, this);
    EggIncubatorProcess slowProcess = new EggIncubatorProcess();
    EggIncubatorDescriptor descriptor;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    double lastVoltagePublish;

    public EggIncubatorElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        electricalLoadList.add(powerLoad);
        electricalComponentList.add(powerResistor);
        slowProcessList.add(slowProcess);

        this.descriptor = (EggIncubatorDescriptor) descriptor;

        Failure exp = new Failure(this).machine();
        slowProcessList.add(voltageWatchdog.set(powerLoad).setUNominal(this.descriptor.nominalVoltage).set(exp));
    }

    class EggIncubatorProcess implements IProcess, INBTTReady {

        double energy = 5000;

        public EggIncubatorProcess() {
            resetEnergy();
        }

        void resetEnergy() {
            energy = 10000 + Math.random() * 10000;
        }

        @Override
        public void process(double time) {
            energy -= powerResistor.getP() * time;
            if (!Utils.isEmpty(inventory.getItem(EggIncubatorContainer.EggSlotId))) {
                descriptor.setState(powerResistor, true);
                if (energy <= 0) {
                    inventory.removeItem(EggIncubatorContainer.EggSlotId, 1);
                    Chicken chicken = net.minecraft.world.entity.EntityTypes.CHICKEN.create(node.coordonate.world(), EntitySpawnReason.TRIGGERED);
                    if (chicken != null) {
                        chicken.setAge(-24000);
                        chicken.snapTo(node.coordonate.x + 0.5, node.coordonate.y + 0.5, node.coordonate.z + 0.5, Mth.wrapDegrees(node.coordonate.world().getRandom().nextFloat() * 360.0F), 0.0F);
                        chicken.yHeadRot = chicken.getYRot();
                        chicken.yBodyRot = chicken.getYRot();
                        node.coordonate.world().addFreshEntity(chicken);
                        chicken.playAmbientSound();
                    }
                    //node.coordonate.world().spawnEntity());
                    resetEnergy();

                    needPublish();
                }
            } else {
                descriptor.setState(powerResistor, false);
                resetEnergy();
            }
            if (Math.abs(powerLoad.getU() - lastVoltagePublish) / descriptor.nominalVoltage > 0.1) needPublish();
        }

        @Override
        public void readFromNBT(CompoundTag nbt, String str) {
            energy = nbt.getDoubleOr(str + "energyCounter", 0.0);
        }

        @Override
        public void writeToNBT(CompoundTag nbt, String str) {
            nbt.putDouble(str + "energyCounter", energy);
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu != LRDU.Down) return null;
        return powerLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu == lrdu.Down) {
            return NodeBase.maskElectricalPower;
        }
        return 0;
    }

    @Override
    public String multiMeterString(Direction side) {
        return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return null;
    }

    @Override
    public void initialize() {
        descriptor.applyTo(powerLoad);
        connect();
    }

    public void inventoryChange(Container inventory) {
        needPublish();
    }

    @Override
    public boolean onBlockActivated(Player entityPlayer, Direction side, float vx, float vy, float vz) {
        return false;
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    public float getLightOpacity() {
        return 1.0f;
    }

    @Override
    public Container getInventory() {
        return inventory;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            if (Utils.isEmpty(inventory.getItem(EggIncubatorContainer.EggSlotId))) stream.writeByte(0);
            else stream.writeByte(inventory.getItem(EggIncubatorContainer.EggSlotId).getCount());

            node.lrduCubeMask.getTranslate(front.down()).serialize(stream);

            stream.writeFloat((float) powerLoad.getU());
        } catch (IOException e) {
            e.printStackTrace();
        }
        lastVoltagePublish = powerLoad.getU();
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Has egg"), !Utils.isEmpty(inventory.getItem(EggIncubatorContainer.EggSlotId)) ?
            I18N.tr("Yes") : I18N.tr("No"));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Power consumption"), Utils.plotPower("", powerResistor.getP()));
        }
        return info;
    }
}
