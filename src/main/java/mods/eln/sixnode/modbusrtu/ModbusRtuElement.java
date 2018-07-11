package mods.eln.sixnode.modbusrtu;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInputOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.WirelessUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ModbusRtuElement extends SixNodeElement implements IModbusSlave {

    public NbtElectricalGateInputOutput[] ioGate = new NbtElectricalGateInputOutput[4];
    public NbtElectricalGateOutputProcess[] ioGateProcess = new NbtElectricalGateOutputProcess[4];

    HashMap<Integer, ServerWirelessTxStatus> wirelessTxStatusList = new HashMap<Integer, ServerWirelessTxStatus>();
    HashMap<Integer, ServerWirelessRxStatus> wirelessRxStatusList = new HashMap<Integer, ServerWirelessRxStatus>();

    ModbusRtuDescriptor descriptor;

    static final int ioStartOffset = 16;
    static final int ioRange = 8;
    int station = -1;

    String name = "";

    HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
    HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();

    static final byte setStation = 1;
    static final byte setName = 2;
    static final byte serverTxAdd = 3;
    static final byte serverRxAdd = 4;
    static final byte serverTxConfig = 5;
    static final byte serverRxConfig = 6;
    static final byte serverTxDelete = 7;
    static final byte serverRxDelete = 8;
    static final byte serverAllSyncronise = 9;

    static final byte clientAllSyncronise = 1;
    static final byte clientTx1Syncronise = 2;
    static final byte clientRx1Syncronise = 3;
    static final byte clientTxDelete = 4;
    static final byte clientRxDelete = 5;
    static final byte clientRx1Connected = 6;

    static final byte ClientModbusActivityEvent = 7;
    static final byte ClientModbusErrorEvent = 8;

    boolean addedOnServer = false;

    ArrayList<IModbusSlot> mapping = new ArrayList<IModbusSlot>();
    ModbusNullSlot nullSlot = new ModbusNullSlot();

    public ModbusRtuElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        for (int idx = 0; idx < 4; idx++) {
            ioGate[idx] = new NbtElectricalGateInputOutput("ioGate" + idx);
            ioGateProcess[idx] = new NbtElectricalGateOutputProcess("ioGateProcess" + idx, ioGate[idx]);

            electricalLoadList.add(ioGate[idx]);
            electricalComponentList.add(ioGateProcess[idx]);

            ioGateProcess[idx].setHighImpedance(true);

            mapping.add(new modbusAnalogIoSlot(ioStartOffset + idx * ioRange, ioRange, ioGate[idx], ioGateProcess[idx]));
        }

        slowProcessList.add(new ModbusRtuSlowProcess());

        this.descriptor = (ModbusRtuDescriptor) descriptor;
    }

    class ModbusRtuSlowProcess implements IProcess {
        double sleepTimer = 0;

        @Override
        public void process(double time) {
            sleepTimer -= time;
            if (sleepTimer < 0) {
                sleepTimer += Utils.rand(1.2, 2);

                IWirelessSignalSpot spot = WirelessUtils.buildSpot(getCoordonate(), null, 0);
                WirelessUtils.getTx(spot, txSet, txStrength);
            }

            for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
                if (rx.isConnected() != rx.connected) {
                    rx.connected = !rx.connected;
                    sendRx1Connected(rx);
                }
            }
        }
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        return ioGate[lrdu.toInt()];
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        return NodeBase.maskElectricalGate;
    }

    @Override
    public String multiMeterString() {
        return null; // Utils.plotUIP(powerLoad.Uc, powerLoad.getCurrent());
    }

    @Nullable
    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        if (Eln.modbusEnable) {
            info.put(I18N.tr("Modbus TCP"), Eln.modbusServer.getHost() + ":" + Eln.modbusServer.getPort());
            info.put(I18N.tr("Modbus Unit ID"), String.valueOf(station));
        } else {
            info.put("X_X", I18N.tr("Modbus is disabled, enable it in Eln.cfg"));
        }
        return info;
    }

    @Override
    public String thermoMeterString() {
        return null;
    }

    @Override
    public void initialize() {
        addToServer();
        // connect();
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
        removeFromServer();

        // Remove all TX signals.
        for (Integer key : wirelessTxStatusList.keySet()) {
            ServerWirelessTxStatus status = wirelessTxStatusList.get(key);
            status.delete();
            wirelessTxStatusList.remove(key);
        }
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        if (Utils.isPlayerUsingWrench(entityPlayer)) {
            if (side.isY()) {
                front = front.getNextClockwise();
                sixNode.reconnect();
                sixNode.setNeedPublish(true);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);

        try {
            stream.writeInt(station);
            stream.writeUTF(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendTx1Syncronise(WirelessTxStatus tx) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(clientTx1Syncronise);

            tx.writeTo(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO

        sendPacketToAllClient(bos);
    }

    void sendRx1Syncronise(WirelessRxStatus rx) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(clientRx1Syncronise);

            rx.writeTo(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO

        sendPacketToAllClient(bos);
    }

    void sendRx1Connected(WirelessRxStatus rx) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(clientRx1Connected);
            packet.writeInt(rx.uuid);
            packet.writeBoolean(rx.connected);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO

        sendPacketToAllClient(bos);
    }

    void onActivity() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(ClientModbusActivityEvent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendPacketToAllClient(bos);
    }

    void onError() {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
        DataOutputStream packet = new DataOutputStream(bos);

        preparePacketForClient(packet);

        try {
            packet.writeByte(ClientModbusErrorEvent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendPacketToAllClient(bos);
    }

    @Override
    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream, player);
        try {
            switch (stream.readByte()) {
                case setStation:
                    setStation(stream.readInt());
                    break;
                case setName:
                    setName(stream.readUTF());
                    break;
                case serverTxAdd: {
                    int idx = 0;
                    String name = stream.readUTF();

                    int uuid = 0;
                    /*
                     * do{ uuid = (int)(Math.random()*100000); }while(wirelessTxStatusList.containsKey(uuid));
                     */
                    for (ServerWirelessTxStatus tx : wirelessTxStatusList.values()) {
                        uuid = Math.max(uuid, tx.uuid);
                    }
                    uuid++;
                    ServerWirelessTxStatus tx;
                    wirelessTxStatusList.put(uuid, tx = new ServerWirelessTxStatus(name, -1, 0, sixNode.coordonate, uuid, this));

                    sendTx1Syncronise(tx);
                }
                break;
                case serverRxAdd: {
                    int idx = 0;
                    String name = stream.readUTF();

                    int uuid = 0;
                    /*
                     * do{ uuid = (int)(Math.random()*100000); }while(wirelessTxStatusList.containsKey(uuid));
                     */
                    for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
                        uuid = Math.max(uuid, rx.uuid);
                    }
                    uuid++;

                    ServerWirelessRxStatus rx;
                    wirelessRxStatusList.put(uuid, rx = new ServerWirelessRxStatus(name, -1, false, uuid, this));

                    sendRx1Syncronise(rx);
                }
                break;
                case serverTxConfig: {
                    int uuid = stream.readInt();
                    String name = stream.readUTF();
                    int id = stream.readInt();

                    WirelessTxStatus tx = wirelessTxStatusList.get(uuid);
                    if (tx != null) {
                        tx.setName(name);
                        tx.setId(id);
                        sendTx1Syncronise(tx);
                    }
                }
                break;
                case serverRxConfig: {
                    int uuid = stream.readInt();
                    String name = stream.readUTF();
                    int id = stream.readInt();

                    WirelessRxStatus rx = wirelessRxStatusList.get(uuid);
                    if (rx != null) {
                        rx.setName(name);
                        rx.setId(id);
                        sendRx1Syncronise(rx);
                    }
                }
                break;
                case serverTxDelete: {
                    int uuid = stream.readInt();
                    ServerWirelessTxStatus tx = wirelessTxStatusList.get(uuid);
                    if (tx != null) {
                        tx.delete();
                        wirelessTxStatusList.remove(tx.uuid);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
                        DataOutputStream packet = new DataOutputStream(bos);

                        preparePacketForClient(packet);

                        packet.writeByte(clientTxDelete);
                        packet.writeInt(uuid);
                        // TODO

                        sendPacketToAllClient(bos);
                    }
                }
                break;
                case serverRxDelete:
                    int uuid = stream.readInt();
                    ServerWirelessRxStatus rx = wirelessRxStatusList.get(uuid);
                    if (rx != null) {
                        rx.delete();
                        wirelessRxStatusList.remove(uuid);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
                        DataOutputStream packet = new DataOutputStream(bos);

                        preparePacketForClient(packet);

                        packet.writeByte(clientRxDelete);
                        packet.writeInt(uuid);
                        // TODO

                        sendPacketToAllClient(bos);
                    }
                    break;
                case serverAllSyncronise: {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
                    DataOutputStream packet = new DataOutputStream(bos);

                    preparePacketForClient(packet);

                    packet.writeByte(clientAllSyncronise);

                    packet.writeInt(wirelessTxStatusList.size());
                    for (ServerWirelessTxStatus e : wirelessTxStatusList.values()) {
                        e.writeTo(packet);
                    }

                    packet.writeInt(wirelessRxStatusList.size());
                    for (WirelessRxStatus e : wirelessRxStatusList.values()) {
                        e.writeTo(packet);
                    }

                    sendPacketToClient(bos, player);
                }
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	/*
     * public String getType() {
	 * 
	 * return "Probe"; }
	 */

    private void setName(String name) {
        this.name = name;
        needPublish();
    }

    private void setStation(int port) {
        removeFromServer();
        this.station = port;
        addToServer();
        needPublish();
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("station", station);
        nbt.setString("name", name);

        int idx;

        nbt.setInteger("txCnt", wirelessTxStatusList.size());
        idx = 0;
        for (ServerWirelessTxStatus tx : wirelessTxStatusList.values()) {
            tx.writeToNBT(nbt, "tx" + idx);
            idx++;
        }

        nbt.setInteger("rxCnt", wirelessRxStatusList.size());
        idx = 0;
        for (ServerWirelessRxStatus rx : wirelessRxStatusList.values()) {
            rx.writeToNBT(nbt, "rx" + idx);
            idx++;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        station = nbt.getInteger("station");
        name = nbt.getString("name");

        int cnt;

        cnt = nbt.getInteger("txCnt");
        for (int idx = 0; idx < cnt; idx++) {
            ServerWirelessTxStatus tx = new ServerWirelessTxStatus(nbt, "tx" + idx, this);
            wirelessTxStatusList.put(tx.uuid, tx);
        }
        cnt = nbt.getInteger("rxCnt");
        for (int idx = 0; idx < cnt; idx++) {
            ServerWirelessRxStatus rx = new ServerWirelessRxStatus(nbt, "rx" + idx, this);
            wirelessRxStatusList.put(rx.uuid, rx);
        }
    }

    @Override
    public boolean hasGui() {
        return true;
    }

    void addToServer() {
        if (station != -1) {
            addedOnServer = Eln.modbusServer.add(this);
        }
    }

    void removeFromServer() {
        if (addedOnServer)
            Eln.modbusServer.remove(this);
        addedOnServer = false;
    }

    IModbusSlot getModbusSlot(int id) {
        for (IModbusSlot slot : mapping) {
            if (id >= slot.getOffset() && id < slot.getOffset() + slot.getSize()) {
                onActivity();
                return slot;
            }
        }
        onError();
        return nullSlot;
    }

    @Override
    public boolean getCoil(int id) throws IllegalAddressException {
        IModbusSlot slot = getModbusSlot(id);
        id -= slot.getOffset();
        return slot.getCoil(id);
    }

    @Override
    public boolean getInput(int id) throws IllegalAddressException {
        IModbusSlot slot = getModbusSlot(id);
        id -= slot.getOffset();
        return slot.getInput(id);
    }

    @Override
    public short getInputRegister(int id) throws IllegalAddressException {
        IModbusSlot slot = getModbusSlot(id);
        id -= slot.getOffset();
        return slot.getInputRegister(id);
    }

    @Override
    public int getSlaveId() {
        return station;
    }

    @Override
    public void setCoil(int id, boolean value) {
        IModbusSlot slot = getModbusSlot(id);
        id -= slot.getOffset();
        slot.setCoil(id, value);
    }

    @Override
    public void setHoldingRegister(int id, short value) {
        IModbusSlot slot = getModbusSlot(id);
        id -= slot.getOffset();
        slot.setHoldingRegister(id, value);
    }
}
