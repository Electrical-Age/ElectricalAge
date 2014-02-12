package mods.eln.sim;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;

import javax.swing.text.html.parser.Entity;

import org.bouncycastle.crypto.tls.ByteQueue;

import com.google.common.primitives.Bytes;

import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;



public class Simulator implements ITickHandler/* ,IPacketHandler*/ {
	
	
	private ArrayList<IProcess> slowProcessList;

	private ArrayList<IProcess> electricalProcessList;
	private ArrayList<ElectricalConnection> electricalConnectionList;
	private ArrayList<ElectricalLoad> electricalLoadList;
	

	private ArrayList<IProcess> thermalProcessList;
	private ArrayList<ThermalConnection> thermalConnectionList;
	private ArrayList<ThermalLoad> thermalLoadList;
	

	public ArrayList<IProcess> getElectricalProcessList()
	{
		return electricalProcessList;
	}
	

	
	boolean run;
	int fps = 20;
	int commonOverSampling = 5;
	int electricalOverSampling = 10;
	int thermalOverSampling = 1;
	int nodeCount = 0;
	
	public double electricalHz;
	
	public double getMinimalElectricalC(double Rs,double Rp)
	{
		return 3/(electricalHz * Rs);
	}
	public double getMinimalThermalC(double Rs,double Rp)
	{
		return 3/(fps * commonOverSampling * thermalOverSampling *  (1/(1/Rp + 1/Rs)));
	}
	
	
	
	public boolean checkThermalLoad(double thermalRs, double thermalRp, double thermalC)
	{
		if(thermalC < getMinimalThermalC(thermalRs,thermalRp))
		{
			System.out.println("checkThermalLoad ERROR");
			while(true);
			//return false;
		}
		return true;
	}
	
	public Simulator(int fps,int commonOverSampling,int electricalOverSampling,int thermalOverSampling)
	{
		this.fps = fps;
		this.commonOverSampling = commonOverSampling;
		this.electricalOverSampling = electricalOverSampling;
		this.thermalOverSampling = thermalOverSampling;
		this.electricalHz = commonOverSampling*electricalOverSampling*fps;
		
		TickRegistry.registerTickHandler(this, Side.SERVER);
	
		slowProcessList = new ArrayList<IProcess>();
	
		
		electricalProcessList = new ArrayList<IProcess>();
		electricalConnectionList = new ArrayList<ElectricalConnection>();
		electricalLoadList = new ArrayList<ElectricalLoad>();

	
		thermalProcessList = new ArrayList<IProcess>();
		thermalConnectionList = new ArrayList<ThermalConnection>();
		thermalLoadList = new ArrayList<ThermalLoad>();

		run = false;
		
		setSimplify(true);
	}
	
	public void init()
	{
		nodeCount = 0;

		slowProcessList.clear();
		
		electricalProcessList.clear();
		electricalConnectionList.clear();
		electricalLoadList.clear();

		thermalProcessList.clear();
		thermalConnectionList.clear();
		thermalLoadList.clear();
		
		run = true;
	}	
	public void stop()
	{
		nodeCount = 0;
				
		slowProcessList.clear();
		
		electricalProcessList.clear();
		electricalConnectionList.clear();
		electricalLoadList.clear();

		thermalProcessList.clear();
		thermalConnectionList.clear();
		thermalLoadList.clear();
		
		run = false;
	}
	
	public void addElectricalConnection(ElectricalConnection connection)
	{
		if(connection!=null){
			electricalConnectionList.add(connection);
			connection.L1.electricalConnections.add(connection);
			connection.L2.electricalConnections.add(connection);
			workingGenerated = false;
		}
	}
	public void removeElectricalConnection(ElectricalConnection connection)
	{
		if(connection!=null){
			electricalConnectionList.remove(connection);
			connection.L1.electricalConnections.remove(connection);
			connection.L2.electricalConnections.remove(connection);
			workingGenerated = false;
		}


	}
	
	public void addThermalConnection(ThermalConnection connection)
	{
		if(connection!=null)thermalConnectionList.add(connection);
	}
	public void removeThermalConnection(ThermalConnection connection)
	{
		if(connection!=null)thermalConnectionList.remove(connection);
	}

	
	public void addElectricalLoad(ElectricalLoad load)
	{
		if(load!=null){
			electricalLoadList.add(load);
			workingGenerated = false;
		}
		
	}
	public void removeElectricalLoad(ElectricalLoad load)
	{
		if(load!=null){
			electricalLoadList.remove(load);
			workingGenerated = false;
		}
	}
	
	public void addThermalLoad(ThermalLoad load)
	{
		if(load!=null)thermalLoadList.add(load);
	}
	public void removeThermalLoad(ThermalLoad load)
	{
		if(load!=null)thermalLoadList.remove(load);
	}

	public void addSlowProcess(IProcess process)
	{
		if(process!=null)slowProcessList.add(process);
	}
	public void removeSlowProcess(IProcess process)
	{
		if(process!=null)slowProcessList.remove(process);
	}
	
	public void addElectricalProcess(IProcess process)
	{
		if(process!=null)electricalProcessList.add(process);
	}
	public void removeElectricalProcess(IProcess process)
	{
		if(process!=null)electricalProcessList.remove(process);
	}
	
	public void addThermalProcess(IProcess process)
	{
		if(process!=null)thermalProcessList.add(process);
	}
	public void removeThermalProcess(IProcess process)
	{
		if(process!=null)thermalProcessList.remove(process);
	}
	
	
	public void addAllElectricalConnection(ArrayList<ElectricalConnection> connection)
	{
		if(connection!=null){
			for (ElectricalConnection c : connection) {
				addElectricalConnection(c);
			}
			workingGenerated = false;
		}
	}
	public void removeAllElectricalConnection(ArrayList<ElectricalConnection> connection)
	{
		if(connection!=null){
			for (ElectricalConnection c : connection) {
				removeElectricalConnection(c);
			}
			workingGenerated = false;
		}
	}
	
	public void addAllThermalConnection(ArrayList<ThermalConnection> connection)
	{
		if(connection!=null)thermalConnectionList.addAll(connection);
	}
	public void removeAllThermalConnection(ArrayList<ThermalConnection> connection)
	{
		if(connection!=null)thermalConnectionList.removeAll(connection);
	}

	
	public void addAllElectricalLoad(ArrayList<ElectricalLoad> load)
	{
		if(load!=null){
			electricalLoadList.addAll(load);
			workingGenerated = false;
		}
	}
	public void removeAllElectricalLoad(ArrayList<ElectricalLoad> load)
	{
		if(load!=null){
			electricalLoadList.removeAll(load);
			workingGenerated = false;
		}
	}
	
	public void addAllThermalLoad(ArrayList<ThermalLoad> load)
	{
		if(load!=null)thermalLoadList.addAll(load);
	}
	public void removeAllThermalLoad(ArrayList<ThermalLoad> load)
	{
		if(load!=null)thermalLoadList.removeAll(load);
	}

	public void addAllSlowProcess(ArrayList<IProcess> process)
	{
		if(process!=null)slowProcessList.addAll(process);
	}
	public void removeAllSlowProcess(ArrayList<IProcess> process)
	{
		if(process!=null)slowProcessList.removeAll(process);
	}
	
	public void addAllElectricalProcess(ArrayList<IProcess> process)
	{
		if(process!=null)electricalProcessList.addAll(process);
	}
	public void removeAllElectricalProcess(ArrayList<IProcess> process)
	{
		if(process!=null)electricalProcessList.removeAll(process);
	}
	
	public void addAllThermalProcess(ArrayList<IProcess> process)
	{
		if(process!=null)thermalProcessList.addAll(process);
	}
	public void removeAllThermalProcess(ArrayList<IProcess> process)
	{
		if(process!=null)thermalProcessList.removeAll(process);
	}	
	
	
	private ArrayList<ElectricalConnection> workingElectricalConnectionList = new ArrayList<ElectricalConnection>();
	private ArrayList<ElectricalLoad> workingElectricalLoadList = new ArrayList<ElectricalLoad>();
	private ArrayList<SimplifiedElectricalBranch> simplifiedElectricalBranchList = new ArrayList<SimplifiedElectricalBranch>();
	
	int simplifyCMin = 0;
	int simplifyCMax = 100;
	
	void generateSimplify(){
		destroySimplify();
		
		for (ElectricalConnection c : electricalConnectionList) {
			c.resetTag();
		}
		for (ElectricalLoad l : electricalLoadList) {
			l.resetTag();
		}
		for (ElectricalLoad lStart : electricalLoadList) {
			if(!lStart.isTaged() && lStart.getSimplifyAuthorized() && lStart.electricalConnections.size() == 2){
				int cCount = 0;
				int lCount = 0;
				ElectricalConnection c/*,cA,cB*/;
				ElectricalLoad l = null,lA,lB;
				ArrayList<ElectricalLoad> lAList = new ArrayList<ElectricalLoad>(16);
				ArrayList<ElectricalLoad> lBList = new ArrayList<ElectricalLoad>(16);
				
				cCount = 2;
				
				c = lStart.electricalConnections.get(0);
				if(c.L1 == lStart)
					l = c.L2;
				else
					l = c.L1;

				while(cCount < simplifyCMax && l != lStart && l.electricalConnections.size() == 2 && l.getSimplifyAuthorized()){
					lAList.add(l);cCount++;
					if(l.electricalConnections.get(0) == c)
						c = l.electricalConnections.get(1);
					else
						c = l.electricalConnections.get(0);
					
					if(c.L1 == l)
						l = c.L2;
					else
						l = c.L1;									
				}
			//	cA = c;
				lA = l;
				
				if(l != lStart){
					c = lStart.electricalConnections.get(1);
					if(c.L1 == lStart)
						l = c.L2;
					else
						l = c.L1;
					
					while(cCount < simplifyCMax && l != lStart && l.electricalConnections.size() == 2 && l.getSimplifyAuthorized()){
						lBList.add(l);cCount++;
						if(l.electricalConnections.get(0) == c)
							c = l.electricalConnections.get(1);
						else
							c = l.electricalConnections.get(0);
						
						if(c.L1 == l)
							l = c.L2;
						else
							l = c.L1;
					}
				}
					//cB = c;
				lB = l;
				
				if(cCount >= simplifyCMin){
					ElectricalLoad[] lList = new ElectricalLoad[1+lAList.size() + lBList.size()];
					int idx;
					
					idx = 0;	
					for (;idx < lAList.size();idx++) {
						lList[idx] = lAList.get(lAList.size() - 1 - idx);
					}
					lList[idx++] = lStart;
					for (ElectricalLoad e : lBList) {
						lList[idx++] = e;
					}
					
					for (ElectricalLoad e : lList) {
						e.setTag();
						for (ElectricalConnection e2 : e.electricalConnections) {
							e2.setTag();
						}
					}
					
					SimplifiedElectricalBranch b = new SimplifiedElectricalBranch(lList,lA,lB);
					simplifiedElectricalBranchList.add(b);
					
					//System.out.println("Simplify    C:" + cCount);
					
					
				}
				
				
				
			}
		}
		
		//electricalProcessList.addAll(simplifiedElectricalBranchList);
		
		for (ElectricalConnection c : electricalConnectionList) {
			if(!c.isTaged()){
				workingElectricalConnectionList.add(c);
			}
		}
		
		
		for (ElectricalLoad l : electricalLoadList) {
			if(!l.isTaged()){
				workingElectricalLoadList.add(l);
			}
		}
	}
	
	void destroySimplify(){
		simplifiedElectricalBranchList.clear();
		workingElectricalConnectionList.clear();
		workingElectricalLoadList.clear();
	}

	//private ArrayList<Double> conectionSerialConductance = new ArrayList<Double>();
	boolean simplifyEnable;
	boolean workingGenerated;
	double avgTickTime = 0;
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData) {
		
		//Minecraft.getMinecraft().mcProfiler.startSection("Miaou !!");
		
/*
		for (ElectricalLoad load : electricalLoadList)
		{
			load.invC = 1/load.C;
			load.invRp = 1/load.Rp;
		}*/
	
		


		long startTime =  System.nanoTime();
		double commonTime = 1.0f/fps/commonOverSampling;
		double electricalTime = 1.0f/fps/commonOverSampling/electricalOverSampling;
		double thermalTime = 1.0f/fps/commonOverSampling/thermalOverSampling;
		
		

		if(!workingGenerated){
			destroySimplify();
			
			if(simplifyEnable){
				
				generateSimplify();
				double eOpt = 0;
				for (SimplifiedElectricalBranch b : simplifiedElectricalBranchList) {
					eOpt += b.energyStored();
				}
				double eNow = 0;
				for (ElectricalLoad l : workingElectricalLoadList) {
					eNow += l.energyStored();
				}
				
				System.out.println("Simplify! " + Utils.plotValue(eOpt,"J less ") + Utils.plotValue(eNow,"J remaine"));
			}
			else{
				System.out.println("NO simplify!");
				workingElectricalLoadList.addAll(electricalLoadList);
				workingElectricalConnectionList.addAll(electricalConnectionList);
			}
			workingGenerated = true;
		}
		
		for (SimplifiedElectricalBranch e : simplifiedElectricalBranchList) {
			e.preStep();
		}

		
		
		for(int idx2 = 0;idx2<commonOverSampling;idx2++)
		{
			
			for (ElectricalConnection c : electricalConnectionList)
			{
				c.serialConductance = 1/(c.L2.getRs() + c.L1.getRs());
			}	
				 
			for(int idx = 0;idx<electricalOverSampling-1;idx++)
			{
			    for (ElectricalConnection c : workingElectricalConnectionList)
			    {
			    	double i;
			    	ElectricalLoad L1 = c.L1,L2 = c.L2;
			    	
			    	i = (L2.Uc - L1.Uc)*c.serialConductance;
	    	
			    	L1.IcTemp += i;
			    	L2.IcTemp -= i;

			    }
			    
			    for (SimplifiedElectricalBranch b : simplifiedElectricalBranchList) {
					b.stepA();
				}
			    for (IProcess process : electricalProcessList)
			    {
			    	process.process(electricalTime);
			    }				   
			    for (ElectricalLoad load : workingElectricalLoadList)
			    {
			    	//load.IcTemp -= load.Uc*load.invRp;
			    	load.Uc += (load.IcTemp - load.Uc*load.invRp) * electricalTime*load.invC;
			    	load.IcTemp = 0;
			    }

			}
			
			
			
		    for (ElectricalLoad load : workingElectricalLoadList)
		    {
		    	load.Isp = 0;
		    }
		    for (ElectricalConnection c : workingElectricalConnectionList)
		    {
		    	double i,absi,iPow2;
		    	ElectricalLoad L1 = c.L1,L2 = c.L2;
		    	
		    	i = (L2.Uc - L1.Uc)*c.serialConductance;
		    	absi = Math.abs(i);
		    	iPow2 = i*i;
		    	
		    	L1.IcTemp += i;
		    	L2.IcTemp -= i;
		    	
		    	L1.IrsTemp += absi;
		    	L2.IrsTemp += absi;
		    	
		    	L1.IrsPow2Temp += iPow2;
		    	L2.IrsPow2Temp += iPow2;
		    }
		    for (SimplifiedElectricalBranch b : simplifiedElectricalBranchList) {
				b.stepB();
			}
		    for (IProcess process : electricalProcessList)
		    {
		    	process.process(electricalTime);
		    }				   
		    for (ElectricalLoad load : workingElectricalLoadList)
		    {
		    	load.IcTemp -= load.Uc*load.invRp;
		    	load.Uc += load.IcTemp*electricalTime*load.invC;

		    	load.Irs = load.IrsTemp;
		    	load.Ic = load.IcTemp;
		    	load.IrsPow2 = load.IrsPow2Temp;
		    	load.IrsTemp = 0;
		    	load.IcTemp = 0;
		    	load.IrsPow2Temp = 0;
		    }

			for(int idx = 0;idx<thermalOverSampling;idx++)
			{
			    for (ThermalConnection c : thermalConnectionList)
			    {
			    	double i;
			    	i = (c.L2.Tc - c.L1.Tc)/((c.L2.Rs + c.L1.Rs));
			    	c.L1.PcTemp += i;
			    	c.L2.PcTemp -= i;	
			    	
			    	c.L1.PrsTemp += Math.abs(i);
			    	c.L2.PrsTemp += Math.abs(i);
			    }
			    for (IProcess process : thermalProcessList)
			    {
			    	process.process(thermalTime);
			    }
			    for (ThermalLoad load : thermalLoadList)
			    {
			    	load.PcTemp -= load.Tc/load.Rp;
			    				    	
			    	load.Tc += load.PcTemp*thermalTime/load.C;
			    	
			    	load.Pc = load.PcTemp;	
			    	load.Prs = load.PrsTemp;
			    	load.Psp = load.PspTemp;	
			    	load.PcTemp = 0;
			    	load.PrsTemp = 0;
			    	load.PspTemp = 0;
			    }
		
			}
			
			

		}
		
		for (SimplifiedElectricalBranch e : simplifiedElectricalBranchList) {
			e.postStep();
		}
		
		long slowProcessTime = System.nanoTime();
	    for (Object o : slowProcessList.toArray())
	    {
	    	IProcess process  = (IProcess) o;
	    	process.process(1.0/20);
	    }	
	    slowProcessTime = System.nanoTime() - slowProcessTime;
		avgTickTime += 1.0/20*((int)(System.nanoTime()-startTime)/1000);
		
		if(++printTimeCounter == 20){
			printTimeCounter = 0;
			
			System.out.println("ticks " + new DecimalFormat("#").format((int)avgTickTime) + " us (" + (int)(slowProcessTime/1000) + "us SP) for " 
					+ "    " + simplifiedElectricalBranchList.size()  + " EB" 
					+ "    " + electricalLoadList.size()  + " EL" 
					+ "    " + workingElectricalLoadList.size()  + " WL" 
					+ "    " + electricalConnectionList.size()  + " EC" 
					+ "    " + workingElectricalConnectionList.size()  + " WC" 
				+ "    " + electricalProcessList.size()  + " EP" 
				+ "    " + thermalLoadList.size()  + " TL" 
				+ "    " + thermalConnectionList.size()  + " TC" 
				+ "    " + thermalProcessList.size()  + " TP" 
				+ "    " + slowProcessList.size()  + " SP" 			
				);
			avgTickTime = 0;
		}
		//Minecraft.getMinecraft().mcProfiler.endSection();
	}
	private int printTimeCounter = 0;
	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData) {
		// TODO Auto-generated method stub
		
		

	}
	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.SERVER);
		
	}
	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return "Miaou";
	}
	public void setSimplify(boolean b) {
		simplifyEnable = b;
		workingGenerated = false;
	}

	
}
