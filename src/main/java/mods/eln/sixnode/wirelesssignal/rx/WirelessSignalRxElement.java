package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.i18n.I18N;
import mods.eln.item.IConfigurable;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.SmallerAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.ToogleAggregator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WirelessSignalRxElement extends SixNodeElement implements IConfigurable {

    NbtElectricalGateOutput outputGate = new NbtElectricalGateOutput("outputGate");
    NbtElectricalGateOutputProcess outputGateProcess = new NbtElectricalGateOutputProcess("outputGateProcess", outputGate);

    public String channel = "Default channel";

    WirelessSignalRxProcess slowProcess = new WirelessSignalRxProcess(this);

    WirelessSignalRxDescriptor descriptor;

    ToogleAggregator toogleAggregator;

    boolean connection = false;

    public static final byte setChannelId = 1;
    public static final byte setSelectedAggregator = 2;
    IWirelessSignalAggregator[] aggregators;

    int selectedAggregator = 0;

    public WirelessSignalRxElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (WirelessSignalRxDescriptor) descriptor;

        electricalLoadList.add(outputGate);
        electricalComponentList.add(outputGateProcess);
        electricalProcessList.add(slowProcess);

        aggregators = new IWirelessSignalAggregator[3];
        aggregators[0] = new BiggerAggregator();
        aggregators[1] = new SmallerAggregator();
        aggregators[2] = toogleAggregator = new ToogleAggregator();
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front == lrdu) return outputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front == lrdu) return NodeBase.maskElectricalOutputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return outputGate.plot("Output gate");
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Channel"), (connection ? "\u00A7a" : "\u00A7c") + channel);
        info.put(I18N.tr("Output voltage"), Utils.plotVolt("", outputGate.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
    }

    void setConnection(boolean connection) {
        if (connection != this.connection) {
            this.connection = connection;
            needPublish();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setString("channel", channel);
        nbt.setBoolean("connection", connection);
        nbt.setInteger("selectedAggregator", selectedAggregator);
        toogleAggregator.writeToNBT(nbt, "toogleAggregator");
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        channel = nbt.getString("channel");
        connection = nbt.getBoolean("connection");
        selectedAggregator = nbt.getInteger("selectedAggregator");
        toogleAggregator.readFromNBT(nbt, "toogleAggregator");
    }

    @Override
    public Coordonate getCoordonate() {
        return sixNode.coordonate;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);

        try {
            switch (stream.readByte()) {
                case setChannelId:
                    channel = stream.readUTF();
                    slowProcess.sleepTimer = 0;
                    needPublish();
                    break;

                case setSelectedAggregator:
                    selectedAggregator = stream.readByte();
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
            stream.writeBoolean(connection);
            stream.writeByte(selectedAggregator);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public IWirelessSignalAggregator getAggregator() {
        if (selectedAggregator >= 0 && selectedAggregator < aggregators.length)
            return aggregators[selectedAggregator];
        return null;
    }

    @Override
    public void readConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        if(compound.hasKey("wirelessChannels")) {
            String newChannel = compound.getTagList("wirelessChannels", 8).getStringTagAt(0);
            if(newChannel != null && newChannel != "") {
                channel = newChannel;
                needPublish();
            }
        }
    }

    @Override
    public void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker) {
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString(channel));
        compound.setTag("wirelessChannels", list);
    }

    //	HashMap<String, ArrayList<IWirelessSignalTx>> wirelessTxInRange = new HashMap<String, ArrayList<IWirelessSignalTx>>();
//	ArrayList<IWirelessSignalSpot> wirelessSpotInRange = new ArrayList<IWirelessSignalSpot>();
}
