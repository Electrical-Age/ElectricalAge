package mods.eln.sixnode.lampsupply;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.item.ConfigCopyToolDescriptor;
import mods.eln.item.IConfigurable;
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
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.WirelessUtils;
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.SmallerAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.ToogleAggregator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class LampSupplyElement extends SixNodeElement implements IConfigurable {

    public static class PowerSupplyChannelHandle {
        PowerSupplyChannelHandle(LampSupplyElement element, int id) {
            this.element = element;
            this.id = id;
        }

        public LampSupplyElement element;
        public int id;
    }

    public static final HashMap<String, ArrayList<PowerSupplyChannelHandle>> channelMap = new HashMap<String, ArrayList<PowerSupplyChannelHandle>>();

    public LampSupplyDescriptor descriptor;

    public NbtElectricalLoad powerLoad = new NbtElectricalLoad("powerLoad");
    public Resistor loadResistor;
    public IProcess lampSupplySlowProcess = new LampSupplySlowProcess();

    private AutoAcceptInventoryProxy inventory = (new AutoAcceptInventoryProxy(new SixNodeElementInventory(1, 64, this)))
        .acceptIfIncrement(0, 64, ElectricalCableDescriptor.class);


    static class Entry {
        Entry(String powerChannel, String wirelessChannel, int aggregator) {
            this.powerChannel = powerChannel;
            this.wirelessChannel = wirelessChannel;
            this.aggregator = aggregator;
        }

        public String powerChannel;
        public String wirelessChannel;
        public int aggregator;
    }

    public ArrayList<Entry> entries = new ArrayList<Entry>();

    VoltageStateWatchDog voltageWatchdog = new VoltageStateWatchDog();

    public static final byte setPowerName = 1;
    public static final byte setWirelessName = 2;
    public static final byte setSelectedAggregator = 3;
    double RpStack = 0;

    boolean[] channelStates;
    IWirelessSignalAggregator[][] aggregators;

    @Override
    public IInventory getInventory() {
        if (inventory != null)
            return inventory.getInventory();
        else
            return null;
    }

    @Override
    public Container newContainer(Direction side, EntityPlayer player) {
        return new LampSupplyContainer(player, inventory.getInventory());
    }

    public LampSupplyElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);
        this.descriptor = (LampSupplyDescriptor) descriptor;
        loadResistor = new Resistor(powerLoad, null);
        electricalComponentList.add(loadResistor);
        loadResistor.highImpedance();


        electricalLoadList.add(powerLoad);
        slowProcessList.add(lampSupplySlowProcess);


        slowProcessList.add(voltageWatchdog);
        voltageWatchdog
            .set(powerLoad)
            .set(new WorldExplosion(this).cableExplosion());
        channelStates = new boolean[this.descriptor.channelCount];
        aggregators = new IWirelessSignalAggregator[this.descriptor.channelCount][3];
        for (int idx = 0; idx < this.descriptor.channelCount; idx++) {
            channelStates[idx] = false;
            entries.add(new Entry("", "", 2));

            aggregators[idx][0] = new BiggerAggregator();
            aggregators[idx][1] = new SmallerAggregator();
            aggregators[idx][2] = new ToogleAggregator();
        }
    }

    class LampSupplySlowProcess implements IProcess {
        double sleepTimer = 0;

        HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
        HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();

        @Override
        public void process(double time) {
            loadResistor.setR(1 / RpStack);
            RpStack = 0;


            sleepTimer -= time;

            if (sleepTimer < 0) {
                sleepTimer += Utils.rand(1.2, 2);

                IWirelessSignalSpot spot = WirelessUtils.buildSpot(LampSupplyElement.this.getCoordonate(), null, 0);
                WirelessUtils.getTx(spot, txSet, txStrength);
            }


            for (int idx = 0; idx < LampSupplyElement.this.descriptor.channelCount; idx++) {
                Entry e = entries.get(idx);
                if (e.wirelessChannel.equals("")) {
                    channelStates[idx] = true;
                } else if (e.wirelessChannel.toLowerCase().equals("true")) {
                    channelStates[idx] = true;
                } else if (e.wirelessChannel.toLowerCase().equals("false")) {
                    channelStates[idx] = false;
                } else {
                    HashSet<IWirelessSignalTx> txs = txSet.get(e.wirelessChannel);
                    if (txs == null) {
                        channelStates[idx] = false;
                    } else {
                        channelStates[idx] = LampSupplyElement.this.aggregators[idx][e.aggregator].aggregate(txs) >= 0.5;
                    }
                }
            }

        }
    }

    static void channelRegister(LampSupplyElement tx, int id, String channel) {
        if (channel.equals("")) return;
        ArrayList<PowerSupplyChannelHandle> list = channelMap.get(channel);
        if (list == null)
            channelMap.put(channel, list = new ArrayList<PowerSupplyChannelHandle>());
        list.add(new PowerSupplyChannelHandle(tx, id));
    }

    static void channelRemove(LampSupplyElement tx, int id, String channel) {
        if (channel.equals("")) return;
        List<PowerSupplyChannelHandle> list = channelMap.get(channel);
        if (list == null) return;
        Iterator<PowerSupplyChannelHandle> i = list.iterator();
        while (i.hasNext()) {
            PowerSupplyChannelHandle e = i.next(); // must be called before you can call i.remove()
            // Do something
            if (e.element == tx && e.id == id) i.remove();
        }
        if (list.isEmpty())
            channelMap.remove(channel);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (getInventory().getStackInSlot(LampSupplyContainer.cableSlotId) == null) return null;
        if (front == lrdu) return powerLoad;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (getInventory().getStackInSlot(LampSupplyContainer.cableSlotId) == null) return 0;
        if (front == lrdu) return NodeBase.maskElectricalPower;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return Utils.plotUIP(powerLoad.getU(), powerLoad.getCurrent());
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        for (int i = 0; i < 3; ++i) {
            Entry e = entries.get(i);
            if (!e.powerChannel.isEmpty()) {
                info.put(I18N.tr("Channel") + " " + (i + 1), e.powerChannel + " = " +
                    (channelStates[i] ? "\u00A7aON" : "\u00A7cOFF"));
            }
        }
        info.put(I18N.tr("Total power"), Utils.plotPower("", powerLoad.getU() * powerLoad.getI()));
        if (Eln.wailaEasyMode) {
            info.put(I18N.tr("Voltage"), Utils.plotVolt("", powerLoad.getU()));
        }
        return info;
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
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (onBlockActivatedRotate(entityPlayer)) return true;

        return inventory.take(entityPlayer.getCurrentEquippedItem(), this, false, true);
    }

    @Override
    public void destroy(EntityPlayerMP entityPlayer) {
        super.destroy(entityPlayer);
        unregister();
    }

    @Override
    public void unload() {
        super.unload();
        unregister();
    }

    void unregister() {
        int idx = 0;
        for (Entry e : entries)
            channelRemove(this, idx++, e.powerChannel);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        int idx = 0;
        for (Entry e : entries) {
            nbt.setString("entry_p" + idx, e.powerChannel);
            nbt.setString("entry_w" + idx, e.wirelessChannel);
            nbt.setBoolean("channelStates" + idx, channelStates[idx]);

            nbt.setInteger("selectedAggregator" + idx, e.aggregator);
            ((ToogleAggregator) aggregators[idx][2]).writeToNBT(nbt, "toogleAggregator" + idx);

            idx++;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        int idx = 0;
        for (Entry e : entries) {
            channelRemove(this, idx++, e.powerChannel);
        }

        super.readFromNBT(nbt);
        if (nbt.hasKey("channel")) {
            entries.get(0).powerChannel = nbt.getString("channel");

        } else {
            idx = 0;
            while (nbt.hasKey("entry_p" + idx)) {
                entries.set(idx, new Entry(nbt.getString("entry_p" + idx), nbt.getString("entry_w" + idx), nbt.getInteger("selectedAggregator" + idx)));
                channelStates[idx] = nbt.getBoolean("channelStates" + idx);

                ((ToogleAggregator) aggregators[idx][2]).readFromNBT(nbt, "toogleAggregator" + idx);

                idx++;
            }
        }

        idx = 0;
        for (Entry e : entries) {
            channelRegister(this, idx++, e.powerChannel);
        }

    }

    void setupFromInventory() {
        ItemStack cableStack = getInventory().getStackInSlot(LampSupplyContainer.cableSlotId);
        if (cableStack != null) {
            ElectricalCableDescriptor desc = (ElectricalCableDescriptor) ElectricalCableDescriptor.getDescriptor(cableStack);
            desc.applyTo(powerLoad);
            voltageWatchdog.setUNominal(desc.electricalNominalVoltage);
        } else {
            voltageWatchdog.setUNominal(10000);
            powerLoad.highImpedance();
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {
            switch (stream.readByte()) {
                case setPowerName: {
                    int id = stream.readByte();
                    String newName = stream.readUTF();
                    channelRemove(this, id, entries.get(id).powerChannel);
                    entries.get(id).powerChannel = newName;
                    needPublish();
                    channelRegister(this, id, newName);
                    break;
                }
                case setWirelessName: {
                    int id = stream.readByte();
                    String newName = stream.readUTF();
                    channelRemove(this, id, entries.get(id).wirelessChannel);
                    entries.get(id).wirelessChannel = newName;
                    needPublish();
                    channelRegister(this, id, newName);
                    break;
                }
                case setSelectedAggregator:
                    entries.get(stream.readByte()).aggregator = stream.readByte();
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
            for (Entry e : entries) {
                stream.writeUTF(e.powerChannel);
                stream.writeUTF(e.wirelessChannel);
                stream.writeChar(e.aggregator);
            }

            Utils.serialiseItemStack(stream, getInventory().getStackInSlot(LampSupplyContainer.cableSlotId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToRp(double r) {
        RpStack += 1 / r;
    }

    public boolean getChannelState(int channel) {
        return channelStates[channel];
    }

    public int getRange() {
        return getRange(descriptor, inventory.getInventory());
    }

    private int getRange(LampSupplyDescriptor desc, IInventory inventory2) {
        ItemStack stack = getInventory().getStackInSlot(LampSupplyContainer.cableSlotId);
        if (stack == null) return desc.range;
        return desc.range + stack.stackSize;
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("powerChannels")) {
            NBTTagList list = compound.getTagList("powerChannel", 8);
            for(int idx = 0; idx < descriptor.channelCount && idx < list.tagCount(); idx++) {
                channelRemove(this, idx, entries.get(idx).powerChannel);
                entries.get(idx).powerChannel = list.getStringTagAt(idx);
                channelRegister(this, idx, entries.get(idx).powerChannel);
            }
            needPublish();
        }
        if(compound.hasKey("wirelessChannels")) {
            NBTTagList list = compound.getTagList("wirelessChannel", 8);
            for(int idx = 0; idx < descriptor.channelCount && idx < list.tagCount(); idx++) {
                channelRemove(this, idx, entries.get(idx).wirelessChannel);
                entries.get(idx).wirelessChannel = list.getStringTagAt(idx);
                channelRegister(this, idx, entries.get(idx).wirelessChannel);
            }
            needPublish();
        }
        if(compound.hasKey("aggregators")) {
            int[] aggregators = compound.getIntArray("aggregators");
            for(int idx = 0; idx < descriptor.channelCount && idx < aggregators.length; idx++) {
                entries.get(idx).aggregator = aggregators[idx];
            }
            needPublish();
        }
        if(ConfigCopyToolDescriptor.readCableType(compound, getInventory(), 0, invoker))
            needPublish();
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        NBTTagList powerList = new NBTTagList();
        NBTTagList wirelessList = new NBTTagList();
        int[] aggregators = new int[descriptor.channelCount];
        for(int idx = 0; idx < descriptor.channelCount; idx++) {
            powerList.appendTag(new NBTTagString(entries.get(idx).powerChannel));
            wirelessList.appendTag(new NBTTagString(entries.get(idx).wirelessChannel));
            aggregators[idx] = entries.get(idx).aggregator;
        }
        compound.setTag("powerChannels", powerList);
        compound.setTag("wirelessChannels", wirelessList);
        compound.setIntArray("aggregators", aggregators);
        ItemStack cables = getInventory().getStackInSlot(0);
        ConfigCopyToolDescriptor.writeCableType(compound, cables);
    }
}
