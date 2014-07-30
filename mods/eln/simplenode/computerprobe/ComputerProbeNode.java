package mods.eln.simplenode.computerprobe;

import java.util.HashMap;
import java.util.HashSet;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import mods.eln.Eln;
import mods.eln.Other;
import mods.eln.misc.Coordonate;
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
import cpw.mods.fml.common.Optional;

public class ComputerProbeNode extends SimpleNode{

	public NbtElectricalGateInputOutput[] ioGate = new NbtElectricalGateInputOutput[6];
	public NbtElectricalGateOutputProcess[] ioGateProcess = new NbtElectricalGateOutputProcess[6];

	
	@Override
	public void initialize() {
		slowProcessList.add(new SlowProcess());
		
		for(int idx = 0; idx < 6; idx++){
			ioGate[idx] = new NbtElectricalGateInputOutput("ioGate" + idx);
			ioGateProcess[idx] = new NbtElectricalGateOutputProcess("ioGateProcess" + idx, ioGate[idx]);
			
			electricalLoadList.add(ioGate[idx]);
			electricalComponentList.add(ioGateProcess[idx]);
			
			ioGateProcess[idx].setHighImpedance(true);
		}		
		
		connect();
	}

	class SlowProcess implements IProcess{
		@Override
		public void process(double time) {
			if(spot != null){
				spotTimeout -= time;
				if(spotTimeout < 0){
					spot = null;
					txSet.clear();
					txStrength.clear();
				}
			}
		}	
	}
	
	double spotTimeout = 0;
	IWirelessSignalSpot spot;
	HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
	HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();
	
	double wirelessRead(String channel,String aggregatorName){
		if(spot == null){
			spot = WirelessUtils.buildSpot(coordonate, null,0);
			txSet.clear();
			txStrength.clear();
			WirelessUtils.getTx(spot, txSet, txStrength);
			spotTimeout = Utils.rand(1, 2);
		}


		IWirelessSignalAggregator aggregator = new BiggerAggregator();
		
		if(aggregatorName.equals("bigger")) aggregator = new BiggerAggregator();
		if(aggregatorName.equals("smaller")) aggregator = new SmallerAggregator();
		
		return aggregator.aggregate(txSet.get(channel));
		
	}
	
	@Override
	public void onBreakBlock() {
		super.onBreakBlock();
		
		
		for(WirelessTx tx : wirelessTxMap.values())
			WirelessSignalTxElement.channelRemove(tx);
	}
	@Override
	public int getSideConnectionMask(Direction side, LRDU lrduA) {
		return NodeBase.maskElectricalGate;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrduA) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrduB) {
		// TODO Auto-generated method stub
		return ioGate[side.getInt()];
	}

	
	@Override
	public String getNodeUuid() {
		return getNodeUuidStatic();
	}
	public static String getNodeUuidStatic() {
		return "ElnComputerProbe";
	}
	/*switch (method) {
	case 0:
		if(arguments.length < 2) return null;
		ioGateProcess[id].setHighImpedance(arguments[1].equals("in"));
		break;
	case 1:
		return new Object[]{ioGateProcess[id].isHighImpedance() ? "in" : "out"};
	case 2:
		if(arguments.length < 2) return null;
		ioGateProcess[id].setOutputNormalized((Double) arguments[1]);
		break;
	case 3:
		return new Object[]{ioGateProcess[id].getOutputNormalized()};
	case 4:
		return new Object[]{ioGate[id].getInputNormalized()};
	default:
		break;
	}*/
    @Optional.Method(modid = Other.modIdOc)
	public Object[] setDir(Context context, Arguments args) {
		Direction side = Direction.valueOf(args.checkString(0));
		boolean highImpedance = args.checkString(1).equals("in");
		ioGateProcess[side.getInt()].setHighImpedance(highImpedance);
		Utils.println(ioGateProcess[side.getInt()].isHighImpedance() );
		return null;
	}

    @Optional.Method(modid = Other.modIdOc)
	public Object[] getDir(Context context, Arguments args) {
		Direction side = Direction.valueOf(args.checkString(0));
		Utils.println(ioGateProcess[side.getInt()].isHighImpedance() );
		return new Object[]{ioGateProcess[side.getInt()].isHighImpedance() ? "in" : "out"};
	}

    @Optional.Method(modid = Other.modIdOc)
	public Object[] setOut(Context context, Arguments args) {
		Direction side = Direction.valueOf(args.checkString(0));
		double value = args.checkDouble(1);
		ioGateProcess[side.getInt()].setOutputNormalized(value);
		return null;
	}

    @Optional.Method(modid = Other.modIdOc)
	public Object[] getOut(Context context, Arguments args) {
    	Direction side = Direction.valueOf(args.checkString(0));
		return new Object[]{ioGateProcess[side.getInt()].getOutputNormalized()};
	}

    @Optional.Method(modid = Other.modIdOc)
	public Object[] getIn(Context context, Arguments args) {
    	Direction side = Direction.valueOf(args.checkString(0));
		return new Object[]{ioGate[side.getInt()].getInputNormalized()};
	}


    @Optional.Method(modid = Other.modIdOc)
	public Object[] wirelessTx(Context context, Arguments args) {
    	String channel = args.checkString(0);
    	double value = args.checkDouble(1);
    	
    	WirelessTx tx = wirelessTxMap.get(channel);
    	if(tx == null){
    		tx = new WirelessTx();
    		tx.channel = channel;
    		WirelessSignalTxElement.channelRegister(tx);
    		wirelessTxMap.put(channel, tx);
    	}
    	
    	tx.value = value;
		return null;
	}
    
    @Optional.Method(modid = Other.modIdOc)
	public Object[] wirelessTxRemove(Context context, Arguments args) {
    	String channel = args.checkString(0);
    	
    	WirelessTx tx = wirelessTxMap.get(channel);
    	if(tx != null){
    		WirelessSignalTxElement.channelRemove(tx);
    		wirelessTxMap.remove(channel);
    	}
		return null;
	}
       
    @Optional.Method(modid = Other.modIdOc)
	public Object[] wirelessTxRemoveAll(Context context, Arguments args) {   	
    	for(WirelessTx tx : wirelessTxMap.values()){
    		WirelessSignalTxElement.channelRemove(tx);
    	}
    	wirelessTxMap.clear();
		return null;
	}
    
    @Optional.Method(modid = Other.modIdOc)
	public Object[] wirelessRx(Context context, Arguments args) {
    	String channel = args.checkString(0);
    	String aggregation = "bigger";
    	if(args.count() == 2) aggregation = args.checkString(1);
    	

		return new Object[]{ wirelessRead(channel, aggregation)};
	}      
    
    public void writeToNBT(NBTTagCompound nbt) {
    	super.writeToNBT(nbt);
    	nbt.setInteger("wirelessTxCount", wirelessTxMap.size());
    	int idx = 0;
    	for(WirelessTx tx : wirelessTxMap.values()){
    		nbt.setString("wirelessTx"+idx+"channel", tx.channel);
    		nbt.setDouble("wirelessTx"+idx+"value", tx.value);
    	}
    }
    
    public void readFromNBT(NBTTagCompound nbt) {
    	super.readFromNBT(nbt);
    	int wirelessTxCount = nbt.getInteger("wirelessTxCount");
    	for(int idx = 0;idx < wirelessTxCount;idx++){
    		WirelessTx tx = new WirelessTx();
    		tx.channel = nbt.getString("wirelessTx"+idx+"channel");
    		tx.value = nbt.getDouble("wirelessTx"+idx+"value");
    		WirelessSignalTxElement.channelRegister(tx);
    		wirelessTxMap.put(tx.channel, tx);
    	}
    }
    
    
  
    
    HashMap<String, WirelessTx> wirelessTxMap = new HashMap<String, ComputerProbeNode.WirelessTx>();
    
    class WirelessTx implements IWirelessSignalTx{
    	String channel;
    	double value;
		@Override
		public Coordonate getCoordonate() {
			// TODO Auto-generated method stub
			return coordonate;
		}

		@Override
		public int getRange() {
			// TODO Auto-generated method stub
			return Eln.wirelessTxRange;
		}

		@Override
		public String getChannel() {
			// TODO Auto-generated method stub
			return channel;
		}

		@Override
		public double getValue() {
			// TODO Auto-generated method stub
			return value;
		}
    }
}
