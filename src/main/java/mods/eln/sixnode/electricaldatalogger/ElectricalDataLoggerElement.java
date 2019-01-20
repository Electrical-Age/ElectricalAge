package mods.eln.sixnode.electricaldatalogger;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.i18n.I18N;
import mods.eln.item.BrushDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.six.SixNode;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.six.SixNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalDataLoggerElement extends SixNodeElement {

    public static final int logsSizeMax = 256;

    public int sampleStack, sampleStackNbr;

    NbtElectricalGateInput inputGate;
    ElectricalDataLoggerProcess slowProcess = new ElectricalDataLoggerProcess(this);
    public ElectricalDataLoggerDescriptor descriptor;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    public double timeToNextSample = 0;

    public byte color = 15;

    public DataLogs logs = new DataLogs(logsSizeMax);

    static final byte publishId = 1, dataId = 2;

    public static final byte resetId = 1;
    public static final byte setSamplingPeriodeId = 2, setMaxValue = 3;
    public static final byte setUnitId = 4;
    public static final byte newClientId = 5;
    public static final byte printId = 6;
    public static final byte tooglePauseId = 7;
    public static final byte setMinValue = 8;

    public static final byte toClientLogsClear = 1;
    public static final byte toClientLogsAdd = 2;

    boolean printToDo;
    boolean pause = false;

    public ElectricalDataLoggerElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
        super(sixNode, side, descriptor);

        this.descriptor = (ElectricalDataLoggerDescriptor) descriptor;

        inputGate = new NbtElectricalGateInput("inputGate");

        electricalLoadList.add(inputGate);
        electricalProcessList.add(slowProcess);
        sampleStackReset();
    }

    public static boolean canBePlacedOnSide(Direction side, SixNodeDescriptor descriptor) {
        if (((ElectricalDataLoggerDescriptor) descriptor).onFloor && side == Direction.YN) return true;
        return false;
    }

    public SixNodeElementInventory getInventory() {
        return inventory;
    }

    public static boolean canBePlacedOnSide(Direction side, int type) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByte("front");
        front = LRDU.fromInt((value >> 0) & 0x3);

        logs.readFromNBT(nbt, "logs");
        pause = nbt.getBoolean("pause");
        timeToNextSample = nbt.getDouble("timeToNextSample");
        sampleStack = nbt.getInteger("sampleStack");
        sampleStackNbr = nbt.getInteger("sampleStackNbr");
        color = nbt.getByte("color");
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setByte("front", (byte) (front.toInt() << 0));
        nbt.setDouble("timeToNextSample", timeToNextSample);
        nbt.setBoolean("pause", pause);
        nbt.setByte("color", color);

        logs.writeToNBT(nbt, "logs");
        nbt.setInteger("sampleStack", sampleStack);
        nbt.setInteger("sampleStackNbr", sampleStackNbr);
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu, int mask) {
        if (front.inverse() == lrdu) return inputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu, int mask) {
        return null;
    }

    @Override
    public int getConnectionMask(LRDU lrdu) {
        if (front.inverse() == lrdu) return NodeBase.maskElectricalInputGate;
        return 0;
    }

    @Override
    public String multiMeterString() {
        return inputGate.plot("In: ");
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = new HashMap<String, String>();
        info.put(I18N.tr("Input"), Utils.plotVolt("", inputGate.getU()));
        return info;
    }

    @Override
    public String thermoMeterString() {
        return "";
    }

    @Override
    public void networkSerialize(DataOutputStream stream) {
        super.networkSerialize(stream);
        try {
            stream.writeByte(logs.unitType);
            stream.writeBoolean(pause);
            stream.writeFloat((float) logs.samplingPeriod);
            stream.writeFloat((float) logs.maxValue);
            stream.writeFloat((float) logs.minValue);
            stream.writeByte(color);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        computeElectricalLoad();
    }

    @Override
    protected void inventoryChanged() {
        computeElectricalLoad();
    }

    public void computeElectricalLoad() {
    }

    @Override
    public void networkUnserialize(DataInputStream stream, EntityPlayerMP player) {
        super.networkUnserialize(stream);
        byte header;
        try {
            switch (header = stream.readByte()) {
                case setSamplingPeriodeId:
                    logs.reset();
                    sampleStackReset();
                    logs.samplingPeriod = stream.readFloat();
                    timeToNextSample = 0.1;
                    needPublish();
                    break;
                case setMaxValue:
                    logs.maxValue = stream.readFloat();
                    needPublish();
                    break;
                case setMinValue:
                    logs.minValue = stream.readFloat();
                    needPublish();
                    break;
                case setUnitId:
                    //sampleStackReset();
                    //logs.reset();
                    logs.unitType = stream.readByte();
                    needPublish();
                    break;
                case resetId:
                    sampleStackReset();
                    logs.reset();
                    break;
                case newClientId:
                    break;
                case printId:
                    printToDo = true;
                    break;
                case tooglePauseId:
                    pause = !pause;
                    needPublish();
                    break;
            }

            if (header == resetId || header == newClientId || header == setSamplingPeriodeId) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream(64);
                DataOutputStream packet = new DataOutputStream(bos);

                preparePacketForClient(packet);

                packet.writeByte(toClientLogsClear);
                int size = logs.size();
                for (int idx = size - 1; idx >= 0; idx--) {
                    packet.writeByte(logs.read(idx));
                }
                if (header == newClientId)
                    sendPacketToClient(bos, player);
                else
                    sendPacketToAllClient(bos);
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
    public Container newContainer(Direction side, EntityPlayer player) {
        return new ElectricalDataLoggerContainer(player, inventory);
    }

    public void sampleStackReset() {
        sampleStack = 0;
        sampleStackNbr = 0;
    }

    @Override
    public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
        ItemStack cur = entityPlayer.getCurrentEquippedItem();
        if(cur != null) {
            GenericItemUsingDamageDescriptor desc = BrushDescriptor.getDescriptor(cur);
            if(desc != null && desc instanceof BrushDescriptor) {
                BrushDescriptor brush = (BrushDescriptor) desc;
                int brushColor = brush.getColor(cur);
                if(brushColor != color && brush.use(cur, entityPlayer)) {
                    color = (byte) brushColor;
                    needPublish();
                }
                return true;
            }
        }

        return false;
    }
}
