package mods.eln.sim;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import javax.swing.text.html.parser.Entity;


import com.google.common.primitives.Bytes;

import mods.eln.misc.Utils;
import mods.eln.node.NodeBlockEntity;
import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.process.destruct.IDestructable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;



public class Simulator /* ,IPacketHandler*/ {
	
	
	public RootSystem mna;
	
	private ArrayList<IProcess> slowProcessList;

	private ArrayList<IProcess> electricalProcessList;
	/*private ArrayList<ElectricalConnection> electricalConnectionList;
	private ArrayList<ElectricalLoad> electricalLoadList;*/
	

	private ArrayList<IProcess> thermalProcessList;
	private ArrayList<ThermalConnection> thermalConnectionList;
	private ArrayList<ThermalLoad> thermalLoadList;
	private HashSet<IDestructable> destructableSet;
	

	public ArrayList<IProcess> getElectricalProcessList()
	{
		return electricalProcessList;
	}
	

	
	boolean run;

	double electricalPeriod,thermalPeriod,callPeriod;
	int electricalInterSystemOverSampling;
	int nodeCount = 0;
	

	public double getMinimalThermalC(double Rs,double Rp)
	{
		return thermalPeriod*3/(  (1/(1/Rp + 1/Rs)));
	}

	
	
	public boolean checkThermalLoad(double thermalRs, double thermalRp, double thermalC)
	{
		if(thermalC < getMinimalThermalC(thermalRs,thermalRp))
		{
			Utils.println("checkThermalLoad ERROR");
			while(true);
			//return false;
		}
		return true;
	}
	
	public Simulator(double callPeriod,double electricalPeriod,int electricalInterSystemOverSampling,double thermalPeriod)
	{
		this.callPeriod = callPeriod;
		this.electricalPeriod = electricalPeriod;
		this.electricalInterSystemOverSampling = electricalInterSystemOverSampling;
		this.thermalPeriod = thermalPeriod;
		

		FMLCommonHandler.instance().bus().register(this);
		
		mna = new RootSystem(electricalPeriod,electricalInterSystemOverSampling);
	
		slowProcessList = new ArrayList<IProcess>();
	
		
		electricalProcessList = new ArrayList<IProcess>();
//		electricalConnectionList = new ArrayList<ElectricalConnection>();
//		electricalLoadList = new ArrayList<ElectricalLoad>();

	
		thermalProcessList = new ArrayList<IProcess>();
		thermalConnectionList = new ArrayList<ThermalConnection>();
		thermalLoadList = new ArrayList<ThermalLoad>();
		destructableSet = new HashSet<IDestructable>();

		run = false;
		

	}
	
	public void init()
	{
		nodeCount = 0;
		
		mna = new RootSystem(electricalPeriod,electricalInterSystemOverSampling);

		slowProcessList.clear();
		
		electricalProcessList.clear();
//		electricalConnectionList.clear();
//		electricalLoadList.clear();

		thermalProcessList.clear();
		thermalConnectionList.clear();
		thermalLoadList.clear();
		destructableSet.clear();
		

		
		run = true;
	}	
	public void stop()
	{
		nodeCount = 0;
		
		mna = null;
				
		slowProcessList.clear();
		
		electricalProcessList.clear();
//		electricalConnectionList.clear();
//		electricalLoadList.clear();

		thermalProcessList.clear();
		thermalConnectionList.clear();
		thermalLoadList.clear();
		destructableSet.clear();

		
		run = false;
	}
	
	public void addElectricalComponent(Component c)
	{
		if(c!=null){
			mna.addComponent(c);
//			electricalConnectionList.add(connection);
//			connection.L1.electricalConnections.add(connection);
//			connection.L2.electricalConnections.add(connection);
		}
	}
	public void removeElectricalComponent(Component c)
	{
		if(c!=null){
			mna.removeComponent(c);

			
//			electricalConnectionList.remove(connection);
//			connection.L1.electricalConnections.remove(connection);
//			connection.L2.electricalConnections.remove(connection);
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

	
	public void addElectricalLoad(State load)
	{
		if(load!=null){
			mna.addState(load);
			//electricalLoadList.add(load);
		}
		
	}
	public void removeElectricalLoad(State load)
	{
		if(load!=null){
			mna.removeState(load);
			//electricalLoadList.remove(load);
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
				addElectricalComponent(c);
			}
		}
	}
	public void removeAllElectricalConnection(ArrayList<ElectricalConnection> connection)
	{
		if(connection!=null){
			for (ElectricalConnection c : connection) {
				removeElectricalComponent(c);
			}
		}
	}
	
	public void addAllElectricalComponent(ArrayList<Component> cList)
	{
		if(cList!=null){
			for (Component c : cList) {
				addElectricalComponent(c);
			}
		}
	}
	public void removeAllElectricalComponent(ArrayList<Component> cList)
	{
		if(cList!=null){
			for (Component c : cList) {
				removeElectricalComponent(c);
			}
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
			for(ElectricalLoad l : load){
				addElectricalLoad(l);
			}
		}
	}
	public void removeAllElectricalLoad(ArrayList<ElectricalLoad> load)
	{
		if(load!=null){
			for(ElectricalLoad l : load){
				removeElectricalLoad(l);
			}
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
	
	

	int simplifyCMin = 0;
	int simplifyCMax = 100;

	

	//private ArrayList<Double> conectionSerialConductance = new ArrayList<Double>();

	double avgTickTime = 0;
	long electricalNsStack = 0,thermalNsStack = 0,slowNsStack = 0;

	double timeout = 0;
	double electricalTimeout = 0;
	double thermalTimeout = 0;
	
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if(event.phase != Phase.START) return;
	
	
		
		long stackStart;

		long startTime =  System.nanoTime();

		
		timeout += callPeriod;

		//Utils.print("+");
		while(timeout > 0){

			if(timeout < electricalTimeout && timeout < thermalTimeout){
				thermalTimeout -= timeout;
				electricalTimeout -= timeout;		
				timeout = 0;
				break;
			}
			
			double dt = 0;
			
			if(electricalTimeout <= thermalTimeout){
				dt = electricalTimeout;
				electricalTimeout += electricalPeriod;
				
				stackStart = System.nanoTime();
				
				mna.step();
				//Utils.print("-");
				for(IProcess p : electricalProcessList) {
					p.process(electricalPeriod);
				}
				
				electricalNsStack += System.nanoTime() - stackStart;
			}else{
				dt = thermalTimeout;
				thermalTimeout += thermalPeriod;

			    stackStart = System.nanoTime();
			///    Utils.print("*");

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
			    	process.process(thermalPeriod);
			    }
			    for (ThermalLoad load : thermalLoadList)
			    {
			    	load.PcTemp -= load.Tc/load.Rp;
			    				    	
			    	load.Tc += load.PcTemp*thermalPeriod/load.C;
			    	
			    	load.Pc = load.PcTemp;	
			    	load.Prs = load.PrsTemp;
			    	load.Psp = load.PspTemp;	
			    	load.PcTemp = 0;
			    	load.PrsTemp = 0;
			    	load.PspTemp = 0;
			    }

				thermalNsStack += System.nanoTime() - stackStart;
								
			}
			thermalTimeout -= dt;
			electricalTimeout -= dt;		
			timeout -= dt;
		}
		

		
	//	long slowProcessTime = System.nanoTime();
		stackStart = System.nanoTime();
		//String str = "";
	    for (Object o : slowProcessList.toArray())
	    {
	    	//long s =  System.nanoTime();
	    	IProcess process  = (IProcess) o;
	    	//str += System.nanoTime()-s + " ";
	    	process.process(1.0/20);
	    }	
	    
	    for(IDestructable d : destructableSet){
	    	d.destructImpl();
	    }
	    destructableSet.clear();
	    
	   // Utils.println(str);
	    slowNsStack += System.nanoTime() - stackStart;
	   // slowProcessTime = System.nanoTime() - slowProcessTime;
		avgTickTime += 1.0/20*((int)(System.nanoTime()-startTime)/1000);
		
		
		/*for (ElectricalLoad l : electricalLoadList) {
			if(Double.isNaN(l.Uc)){
				for(int i = 0;i < 1;i++){
					Utils.print("NAN");
				}
			/*	try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}*//*
			}
		}*/
		
		if(++printTimeCounter == 20){
			
			printTimeCounter = 0;
			electricalNsStack /= 20;
			thermalNsStack /= 20;
			slowNsStack /= 20;
			
			Utils.println(
					//"ticks " + new DecimalFormat("#").format((int)avgTickTime) + " us (" + (int)(slowProcessTime/1000) + "us SP) for "
					"ticks " + new DecimalFormat("#").format((int)avgTickTime) + " us" + "  E " + electricalNsStack/1000  + "  T " + thermalNsStack/1000  + "  S " + slowNsStack/1000 
/*
					+ "    " + electricalLoadList.size()  + " EL" 
					+ "    " + electricalConnectionList.size()  + " EC" */
				+ "    " + mna.getSubSystemCount() + " SS"	
				+ "    " + electricalProcessList.size()  + " EP" 
				+ "    " + thermalLoadList.size()  + " TL" 
				+ "    " + thermalConnectionList.size()  + " TC" 
				+ "    " + thermalProcessList.size()  + " TP" 
				+ "    " + slowProcessList.size()  + " SP" 			
				);
			avgTickTime = 0;
			
			electricalNsStack = 0;
			thermalNsStack = 0;
			slowNsStack = 0;
		}
		//Minecraft.getMinecraft().mcProfiler.endSection();
	}
	private int printTimeCounter = 0;

	
}
