package mods.eln.sixnode.electricaldatalogger;

import mods.eln.i18n.I18N;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ElectricalDataLoggerElement extends SixNodeElement {

    NbtElectricalGateInput inputGate;
    ElectricalDataLoggerProcess slowProcess = new ElectricalDataLoggerProcess(this);
    public ElectricalDataLoggerDescriptor descriptor;

    SixNodeElementInventory inventory = new SixNodeElementInventory(2, 64, this);

    /** The history (board #45): one channel here, the input; shown per channel scale and unit. */
    public final Logger logger = new Logger(this, -1, new LogChannel());
    public final LogStore store = logger.store;
    public final LogChannel[] channels = logger.channels;

    static final byte publishId = 1, dataId = 2;

    public static final byte resetId = 1;
    public static final byte setSamplingPeriodeId = 2, setMaxValue = 3;
    public static final byte setUnitId = 4;
    /** A client that can see the graph asks for it (LogWatchers), every second while it can. */
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
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        byte value = nbt.getByteOr("front", (byte) 0);
        front = LRDU.fromInt((value >> 0) & 0x3);

        pause = nbt.getBooleanOr("pause", false);
        if (!logger.readFromNBT(nbt) && nbt.contains("logssamplingPeriod")) {
            migrate(nbt);
        }
    }

    /**
     * A logger saved before 2026-10-30 kept a byte per sample (newest first) at its period and one scale: its samples
     * are replayed into the store as recorded, and the scale becomes channel 0's.
     */
    void migrate(CompoundTag nbt) {
        byte[] log = nbt.getByteArray("logslog").orElse(new byte[0]);
        float period = nbt.getFloatOr("logssamplingPeriod", 0.5f);
        channels[0].min = nbt.getFloatOr("logsminValue", 0f);
        channels[0].max = nbt.getFloatOr("logsmaxValue", 100f);
        channels[0].unit = LogChannel.oldUnit(nbt.getByteOr("logsunitType", DataLogs.percentType));
        float[][] samples = new float[log.length][];
        for (int i = 0; i < log.length; i++) samples[i] = new float[]{DataLogs.oldSample(log[log.length - 1 - i])};
        store.clear();
        store.setPeriod(period > 0 ? period : 0.5);
        store.replay(samples, store.period());
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putByte("front", (byte) (front.toInt() << 0));
        nbt.putBoolean("pause", pause);
        logger.writeToNBT(nbt);
    }

    /** The graph as shown: the store's view at its period, with the channels' scales. */
    public DataLogs view() {
        return logger.view();
    }

    @Override
    public ElectricalLoad getElectricalLoad(LRDU lrdu) {
        if (front.inverse() == lrdu) return inputGate;
        return null;
    }

    @Override
    public ThermalLoad getThermalLoad(LRDU lrdu) {
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
            stream.writeBoolean(pause);
            logger.publish(stream);
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
    public void networkUnserialize(DataInputStream stream, ServerPlayer player) {
        super.networkUnserialize(stream);
        try {
            byte header = stream.readByte();
            if (logger.handle(header, stream, player)) return;
            switch (header) {
                case printId:
                    printToDo = true;
                    break;
                case tooglePauseId:
                    pause = !pause;
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

    /** The whole graph, to one player (who just started watching) or to every watcher (null). */
    public void sendView(ServerPlayer player) {
        logger.sendView(player);
    }
}
