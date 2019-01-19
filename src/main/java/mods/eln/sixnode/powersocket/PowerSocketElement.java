package mods.eln.sixnode.powersocket;

import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.AutoAcceptInventoryProxy;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.mna.component.Resistor;
import mods.eln.sim.nbt.NbtElectricalLoad;
import mods.eln.sim.process.destruct.VoltageStateWatchDog;
import mods.eln.sim.process.destruct.WorldExplosion;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sixnode.lampsupply.LampSupplyElement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//TODO Copy-pasted from LampSupply. PowerSocket behavior must be implemented.
public class PowerSocketElement extends SixNodeElement {

    //NodeElectricalGateInput inputGate = new NodeElectricalGateInput("inputGate");
    public PowerSocketDescriptor descriptor;

    public NbtElectricalLoad outputLoad = new NbtElectricalLoad("outputLoad");
    public Resistor loadResistor = new Resistor(null, null);  // Connected in process()
    public IProcess PowerSocketSlowProcess = new PowerSocketSlowProcess();

    private AutoAcceptInventoryProxy acceptingInventory = new AutoAcceptInventoryProxy(
        new SixNodeElementInventory(1, 64, this)
    ).acceptIfEmpty(0, ElectricalCableDescriptor.class);

    public String channel = "Default channel";

    public int paintColor = 0;

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    public static final byte setChannelId = 1;

    @Override
    public IInventory getInventory() {
        return acceptingInventory.getInventory();
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new PowerSocketContainer(player, getInventory());
    }

    public PowerSocketElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        electricalLoadList.add(outputLoad);
        electricalComponentList.add(loadResistor);
        slowProcessList.add(PowerSocketSlowProcess);
        loadResistor.highImpedance();
        this.descriptor = (PowerSocketDescriptor) descriptor;

        slowProcessList.add(voltageWatchdog);
        voltageWatchdog
            .set(outputLoad)
            .set(new WorldExplosion(this).cableExplosion());
    }

    class PowerSocketSlowProcess implements IProcess {

        @Override
        public void process(double time) {
            Coordonate local = sixNode.coordonate;
            LampSupplyElement.PowerSupplyChannelHandle handle = null;
            float bestDist = 1e9f;
            List<LampSupplyElement.PowerSupplyChannelHandle> handles = LampSupplyElement.channelMap.get(channel);
            if(handles != null) {
                for(LampSupplyElement.PowerSupplyChannelHandle hdl : handles) {
                    float dist = (float) hdl.element.sixNode.coordonate.trueDistanceTo(local);
                    if(dist < bestDist && dist <= hdl.element.getRange()) {
                        bestDist = dist;
                        handle = hdl;
                    }
                }
            }

            loadResistor.breakConnection();
            loadResistor.highImpedance();
            if(handle != null && handle.element.getChannelState(handle.id)) {
                ItemStack cable = getInventory().getStackInSlot(PowerSocketContainer.cableSlotId);
                if (cable != null) {
                    ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cable);
                    loadResistor.connectTo(handle.element.powerLoad, outputLoad);
                    desc.applyTo(loadResistor);
                }
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (getInventory().getStackInSlot(PowerSocketContainer.cableSlotId) == null) return null;
        return outputLoad;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (getInventory().getStackInSlot(PowerSocketContainer.cableSlotId) == null) return 0;
        return NodeBase.maskElectricalPower + (1 << NodeBase.maskColorCareShift) + (paintColor << NodeBase.maskColorShift);
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(outputLoad.getU(), outputLoad.getCurrent());
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        setupFromInventory();
    }

    @Override
    protected void inventoryChanged() {
        super.inventoryChanged();
        sixNode.disconnect();
        setupFromInventory();
        sixNode.connect();
        needPublish();
    }

    @Override
    public void destroy(EntityPlayerMP entityPlayer) {
        super.destroy(entityPlayer);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("channel", channel);
        nbt.setInteger("color", paintColor);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        channel = nbt.getString("channel");
        paintColor = nbt.getInteger("color");
    }

    void setupFromInventory() {
        ItemStack cableStack = getInventory().getStackInSlot(PowerSocketContainer.cableSlotId);
        if (cableStack != null) {
            ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
            desc.applyTo(outputLoad);
            voltageWatchdog.setUNominal(desc.electricalNominalVoltage);
        } else {
            voltageWatchdog.setUNominal(10000);
            outputLoad.highImpedance();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {
            switch (stream.readByte()) {
                case setChannelId:
                    channel = stream.readUTF();
                    needPublish();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeUTF(channel);
            Utils.serialiseItemStack(stream, getInventory().getStackInSlot(PowerSocketContainer.cableSlotId));
            stream.writeInt(paintColor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack used = entityPlayer.getCurrentEquippedItem();
        if(used != null) {
            GenericItemUsingDamageDescriptor desc = GenericItemUsingDamageDescriptor.getDescriptor(used);
            if(desc != null && desc instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) desc;
                int color = brush.getColor(used);
                if(color != paintColor && brush.use(used, entityPlayer)) {
                    paintColor = color;
                    sixNode.reconnect();
                }
                return true;
            }
        }

        return acceptingInventory.take(used, this, true, true);
    }
}
