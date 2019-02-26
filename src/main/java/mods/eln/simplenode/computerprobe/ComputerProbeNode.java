package mods.eln.simplenode.computerprobe;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.simple.SimpleNode;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInputOutput;
import mods.eln.sim.nbt.NbtElectricalGateOutputProcess;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.WirelessUtils;
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator;
import mods.eln.sixnode.wirelesssignal.aggregator.SmallerAggregator;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Optional;

import java.util.HashMap;
import java.util.HashSet;

@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = Other.modIdCc)
public class ComputerProbeNode extends SimpleNode /*implements IPeripheral */{

    public NbtElectricalGateInputOutput[] ioGate = new NbtElectricalGateInputOutput[6];
    public NbtElectricalGateOutputProcess[] ioGateProcess = new NbtElectricalGateOutputProcess[6];

    double spotTimeout = 0;
    IWirelessSignalSpot spot;
    HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
    HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();

    HashMap<String, WirelessTx> wirelessTxMap = new HashMap<String, ComputerProbeNode.WirelessTx>();

    @Override
    public void initialize() {
        slowProcessList.add(new SlowProcess());

        for (int idx = 0; idx < 6; idx++) {
            ioGate[idx] = new NbtElectricalGateInputOutput("ioGate" + idx);
            ioGateProcess[idx] = new NbtElectricalGateOutputProcess("ioGateProcess" + idx, ioGate[idx]);

            electricalLoadList.add(ioGate[idx]);
            electricalComponentList.add(ioGateProcess[idx]);

            ioGateProcess[idx].setHighImpedance(true);
        }
        connect();
    }

    class SlowProcess implements IProcess {
        @Override
        public void process(double time) {
            if (spot != null) {
                spotTimeout -= time;
                if (spotTimeout < 0) {
                    spot = null;
                    txSet.clear();
                    txStrength.clear();
                }
            }
        }
    }

    double wirelessRead(String channel, String aggregatorName) {
        if (spot == null) {
            spot = WirelessUtils.buildSpot(coordinate, null, 0);
            txSet.clear();
            txStrength.clear();
            WirelessUtils.getTx(spot, txSet, txStrength);
            spotTimeout = Utils.rand(1, 2);
        }

        IWirelessSignalAggregator aggregator = new BiggerAggregator();

        if (aggregatorName.equals("bigger")) aggregator = new BiggerAggregator();
        if (aggregatorName.equals("smaller")) aggregator = new SmallerAggregator();

        return aggregator.aggregate(txSet.get(channel));
    }

    @Override
    public void onBreakBlock() {
        super.onBreakBlock();

        unregister();
    }

    @Override
    public void unload() {
        super.unload();
        unregister();
    }

    void unregister() {
        for (WirelessTx tx : wirelessTxMap.values())
            WirelessSignalTxElement.channelRemove(tx);
    }

    @Override
    public int getSideConnectionMask(Direction side, LRDU lrduA) {
        return NodeBase.maskElectricalGate;
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrduA) {
        return null;
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrduB) {
        return ioGate[side.getInt()];
    }

    @Override
    public String getNodeUuid() {
        return getNodeUuidStatic();
    }

    public static String getNodeUuidStatic() {
        return "ElnComputerProbe";
    }

	/*
     * switch (method) { case 0: if(arguments.length < 2) return null; ioGateProcess[id].setHighImpedance(arguments[1].equals("in")); break; case 1: return new Object[]{ioGateProcess[id].isHighImpedance() ? "in" : "out"}; case 2: if(arguments.length < 2) return null; ioGateProcess[id].setOutputNormalized((Double) arguments[1]); break; case 3: return new Object[]{ioGateProcess[id].getOutputNormalized()}; case 4: return new Object[]{ioGate[id].getInputNormalized()}; default: break; }
	 */

    public Object[] signalSetDir(Direction side, boolean highImpedance) {
        ioGateProcess[side.getInt()].setHighImpedance(highImpedance);
        Utils.println(ioGateProcess[side.getInt()].isHighImpedance());
        return null;
    }

    public Object[] signalGetDir(Direction side) {
        return new Object[]{ioGateProcess[side.getInt()].isHighImpedance() ? "in" : "out"};
    }

    public Object[] signalSetOut(Direction side, double value) {
        ioGateProcess[side.getInt()].setOutputNormalized(value);
        return null;
    }

    public Object[] signalGetOut(Direction side) {
        return new Object[]{ioGateProcess[side.getInt()].getOutputNormalized()};
    }

    public Object[] signalGetIn(Direction side) {
        return new Object[]{ioGate[side.getInt()].getInputNormalized()};
    }

    public Object[] wirelessSet(String channel, double value) {
        WirelessTx tx = wirelessTxMap.get(channel);
        if (tx == null) {
            tx = new WirelessTx();
            tx.channel = channel;
            WirelessSignalTxElement.channelRegister(tx);
            wirelessTxMap.put(channel, tx);
        }

        tx.value = value;
        return null;
    }

    public Object[] wirelessRemove(String channel) {
        WirelessTx tx = wirelessTxMap.get(channel);
        if (tx != null) {
            WirelessSignalTxElement.channelRemove(tx);
            wirelessTxMap.remove(channel);
        }
        return null;
    }

    public Object[] wirelessRemoveAll() {
        for (WirelessTx tx : wirelessTxMap.values()) {
            WirelessSignalTxElement.channelRemove(tx);
        }
        wirelessTxMap.clear();
        return null;
    }

    public Object[] wirelessGet(String channel, String aggregation) {
        return new Object[]{wirelessRead(channel, aggregation)};
    }

    // ********************************** OC *************************

    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalSetDir(Context context, Arguments args) {
        Direction side = Direction.valueOf(args.checkString(0));
        boolean highImpedance = args.checkString(1).equals("in");
        return signalSetDir(side, highImpedance);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetDir(Context context, Arguments args) {
        Direction side = Direction.valueOf(args.checkString(0));
        return signalGetDir(side);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalSetOut(Context context, Arguments args) {
        Direction side = Direction.valueOf(args.checkString(0));
        double value = args.checkDouble(1);
        return signalSetOut(side, value);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetOut(Context context, Arguments args) {
        Direction side = Direction.valueOf(args.checkString(0));
        return signalGetOut(side);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] signalGetIn(Context context, Arguments args) {
        Direction side = Direction.valueOf(args.checkString(0));
        return signalGetIn(side);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessSet(Context context, Arguments args) {
        String channel = args.checkString(0);
        double value = args.checkDouble(1);
        return wirelessSet(channel, value);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessRemove(Context context, Arguments args) {
        String channel = args.checkString(0);
        return wirelessRemove(channel);
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessRemoveAll(Context context, Arguments args) {
        return wirelessRemoveAll();
    }

    @Optional.Method(modid = Other.modIdOc)
    public Object[] wirelessGet(Context context, Arguments args) {
        String channel = args.checkString(0);
        String aggregation = "bigger";
        if (args.count() == 2) aggregation = args.checkString(1);

        return wirelessGet(channel, aggregation);
    }

    // *************************** CC ********************
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public String getType() {
//        return "ElnProbe";
//    }
//
//    String[] functionNames = {"signalSetDir", "signalGetDir", "signalSetOut", "signalGetOut", "signalGetIn", "wirelessSet", "wirelessRemove", "wirelessRemoveAll", "wirelessGet"};
//
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public String[] getMethodNames() {
//        return functionNames;
//    }
//
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] args) throws LuaException, InterruptedException {
//        try {
//            if (method < 0 || method >= functionNames.length) return null;
//            switch (method) {
//                case 0:
//                    return signalSetDir(Direction.valueOf((String) args[0]), args[1].equals("in"));
//                case 1:
//                    return signalGetDir(Direction.valueOf((String) args[0]));
//                case 2:
//                    return signalSetOut(Direction.valueOf((String) args[0]), (Double) args[1]);
//                case 3:
//                    return signalGetOut(Direction.valueOf((String) args[0]));
//                case 4:
//                    return signalGetIn(Direction.valueOf((String) args[0]));
//                case 5:
//                    return wirelessSet((String) args[0], (Double) args[1]);
//                case 6:
//                    return wirelessRemove((String) args[0]);
//                case 7:
//                    return wirelessRemoveAll();
//                case 8: {
//                    String aggregation = "bigger";
//                    if (args.length == 2) aggregation = (String) args[1];
//                    return wirelessGet((String) args[0], aggregation);
//                }
//            }
//        } catch (Exception e) {
//            // TODO: handle exception
//        }
//        return null;
//    }
//
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public void attach(IComputerAccess computer) {
//        Utils.println("CC attache");
//    }
//
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public void detach(IComputerAccess computer) {
//        Utils.println("CC detach");
//    }
//
//    @Override
//    @Optional.Method(modid = Other.modIdCc)
//    public boolean equals(IPeripheral other) {
//        return this == other;
//    }

    // ********************** NBT *****************

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setInteger("wirelessTxCount", wirelessTxMap.size());
        int idx = 0;
        for (WirelessTx tx : wirelessTxMap.values()) {
            nbt.setString("wirelessTx" + idx + "channel", tx.channel);
            nbt.setDouble("wirelessTx" + idx + "value", tx.value);
        }
        return nbt;
    }

    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        int wirelessTxCount = nbt.getInteger("wirelessTxCount");
        for (int idx = 0; idx < wirelessTxCount; idx++) {
            WirelessTx tx = new WirelessTx();
            tx.channel = nbt.getString("wirelessTx" + idx + "channel");
            tx.value = nbt.getDouble("wirelessTx" + idx + "value");
            WirelessSignalTxElement.channelRegister(tx);
            wirelessTxMap.put(tx.channel, tx);
        }
    }

    class WirelessTx implements IWirelessSignalTx {
        String channel;
        double value;

        @Override
        public Coordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public int getRange() {
            return Eln.wirelessTxRange;
        }

        @Override
        public String getChannel() {
            return channel;
        }

        @Override
        public double getValue() {
            return value;
        }
    }
}
