package mods.eln.sim;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.RootSystem;
import mods.eln.sim.mna.component.Component;
import mods.eln.sim.mna.state.State;
import mods.eln.sim.process.destruct.IDestructable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Simulator /* ,IPacketHandler */{

	public RootSystem mna;

	private ArrayList<IProcess> slowProcessList;
	private List<IProcess> slowPreProcessList;

	private ArrayList<IProcess> electricalProcessList;

	private ArrayList<IProcess> thermalFastProcessList, thermalSlowProcessList;
	private ArrayList<ThermalConnection> thermalFastConnectionList, thermalSlowConnectionList;
	private ArrayList<ThermalLoad> thermalFastLoadList, thermalSlowLoadList;
	private Set<IDestructable> destructableSet;

	boolean run;

	public double electricalPeriod, thermalPeriod, callPeriod;
	int electricalInterSystemOverSampling;
	int nodeCount = 0;

	int simplifyCMin = 0;
	int simplifyCMax = 100;

	double avgTickTime = 0;
	long electricalNsStack = 0, thermalFastNsStack = 0, slowNsStack = 0,thermalSlowNsStack = 0;

	double timeout = 0;
	double electricalTimeout = 0;
	double thermalTimeout = 0;

	private int printTimeCounter = 0;

	public ArrayList<IProcess> getElectricalProcessList() {
		return electricalProcessList;
	}

	public double getMinimalThermalC(double Rs, double Rp) {
		return thermalPeriod * 3 / ((1 / (1 / Rp + 1 / Rs)));
	}

	public boolean checkThermalLoad(double thermalRs, double thermalRp, double thermalC) {
		if (thermalC < getMinimalThermalC(thermalRs, thermalRp)) {
			Utils.println("checkThermalLoad ERROR");
			while (true)
				;
			// return false;
		}
		return true;
	}

	public Simulator(double callPeriod, double electricalPeriod, int electricalInterSystemOverSampling, double thermalPeriod) {
		this.callPeriod = callPeriod;
		this.electricalPeriod = electricalPeriod;
		this.electricalInterSystemOverSampling = electricalInterSystemOverSampling;
		this.thermalPeriod = thermalPeriod;

		FMLCommonHandler.instance().bus().register(this);

		mna = new RootSystem(electricalPeriod, electricalInterSystemOverSampling);

		slowProcessList = new ArrayList<IProcess>();
		slowPreProcessList = new ArrayList<IProcess>();

		electricalProcessList = new ArrayList<IProcess>();
		// electricalConnectionList = new ArrayList<ElectricalConnection>();
		// electricalLoadList = new ArrayList<ElectricalLoad>();

		thermalFastProcessList = new ArrayList<IProcess>();
		thermalSlowProcessList = new ArrayList<IProcess>();
		thermalFastConnectionList = new ArrayList<ThermalConnection>();
		thermalFastLoadList = new ArrayList<ThermalLoad>();
		thermalSlowConnectionList = new ArrayList<ThermalConnection>();
		thermalSlowLoadList = new ArrayList<ThermalLoad>();
		destructableSet = new HashSet<IDestructable>();

		run = false;
	}

	public void init() {
		nodeCount = 0;

		mna = new RootSystem(electricalPeriod, electricalInterSystemOverSampling);

		slowProcessList.clear();
		slowPreProcessList.clear();

		electricalProcessList.clear();
		// electricalConnectionList.clear();
		// electricalLoadList.clear();

		thermalFastProcessList.clear();
		thermalSlowProcessList.clear();
		thermalFastConnectionList.clear();
		thermalFastLoadList.clear();
		thermalSlowConnectionList.clear();
		thermalSlowLoadList.clear();
		destructableSet.clear();

		run = true;
	}

	public void stop() {
		nodeCount = 0;

		mna = null;

		slowProcessList.clear();
		slowPreProcessList.clear();

		electricalProcessList.clear();
		// electricalConnectionList.clear();
		// electricalLoadList.clear();

		thermalFastProcessList.clear();
		thermalSlowProcessList.clear();
		thermalFastConnectionList.clear();
		thermalFastLoadList.clear();
		thermalSlowConnectionList.clear();
		thermalSlowLoadList.clear();
		destructableSet.clear();

		run = false;
	}

	public void addElectricalComponent(Component c) {
		if (c != null) {
			mna.addComponent(c);
			// electricalConnectionList.add(connection);
			// connection.L1.electricalConnections.add(connection);
			// connection.L2.electricalConnections.add(connection);
		}
	}

	public void removeElectricalComponent(Component c) {
		if (c != null) {
			mna.removeComponent(c);

			// electricalConnectionList.remove(connection);
			// connection.L1.electricalConnections.remove(connection);
			// connection.L2.electricalConnections.remove(connection);
		}
	}

	public void addThermalConnection(ThermalConnection connection) {
		if (connection != null) {
			if (connection.L1.isSlow() == connection.L2.isSlow()) {
				if (connection.L1.isSlow())
					thermalSlowConnectionList.add(connection);
				else
					thermalFastConnectionList.add(connection);

			} else {
				Utils.println("***** addThermalConnection ERROR ****");
			}
		}
	}

	public void removeThermalConnection(ThermalConnection connection) {
		if (connection != null) {
			thermalSlowConnectionList.remove(connection);
			thermalFastConnectionList.remove(connection);
		}
	}

	public void addElectricalLoad(State load) {
		if (load != null) {
			mna.addState(load);
		}
	}

	public void removeElectricalLoad(State load) {
		if (load != null) {
			mna.removeState(load);
		}
	}

	public void addThermalLoad(ThermalLoad load) {
		if (load != null) {
			if (load.isSlow())
				thermalSlowLoadList.add(load);
			else
				thermalFastLoadList.add(load);
		}
	}

	public void removeThermalLoad(ThermalLoad load) {
		if (load != null) {
			thermalSlowLoadList.remove(load);
			thermalFastLoadList.remove(load);
		}
	}

	public void addSlowProcess(IProcess process) {
		if (process != null) slowProcessList.add(process);
	}

	public void removeSlowProcess(IProcess process) {
		if (process != null) slowProcessList.remove(process);
	}

	public void addSlowPreProcess(IProcess process) {
		if (process != null) slowPreProcessList.add(process);
	}

	public void removeSlowPreProcess(IProcess process) {
		if (process != null) slowPreProcessList.remove(process);
	}

	public void addElectricalProcess(IProcess process) {
		if (process != null) electricalProcessList.add(process);
	}

	public void removeElectricalProcess(IProcess process) {
		if (process != null) electricalProcessList.remove(process);
	}

	public void addThermalFastProcess(IProcess process) {
		if (process != null) thermalFastProcessList.add(process);
	}

	public void removeThermalFastProcess(IProcess process) {
		if (process != null) thermalFastProcessList.remove(process);
	}

	public void addThermalSlowProcess(IProcess process) {
		if (process != null) thermalSlowProcessList.add(process);
	}

	public void removeThermalSlowProcess(IProcess process) {
		if (process != null) thermalSlowProcessList.remove(process);
	}

	public void addAllElectricalConnection(Iterable<ElectricalConnection> connection) {
		if (connection != null) {
			for (ElectricalConnection c : connection) {
				addElectricalComponent(c);
			}
		}
	}

	public void removeAllElectricalConnection(Iterable<ElectricalConnection> connection) {
		if (connection != null) {
			for (ElectricalConnection c : connection) {
				removeElectricalComponent(c);
			}
		}
	}

	public void addAllElectricalComponent(Iterable<Component> cList) {
		if (cList != null) {
			for (Component c : cList) {
				addElectricalComponent(c);
			}
		}
	}

	public void removeAllElectricalComponent(Iterable<Component> cList) {
		if (cList != null) {
			for (Component c : cList) {
				removeElectricalComponent(c);
			}
		}
	}

	public void addAllThermalConnection(Iterable<ThermalConnection> connection) {
		if (connection != null) {
			for (ThermalConnection c : connection) {
				addThermalConnection(c);
			}
		}
	}

	public void removeAllThermalConnection(Iterable<ThermalConnection> connection) {
		if (connection != null) {
			for (ThermalConnection c : connection) {
				removeThermalConnection(c);
			}
		}
	}

	public void addAllElectricalLoad(Iterable<ElectricalLoad> load) {
		if (load != null) {
			for (ElectricalLoad l : load) {
				addElectricalLoad(l);
			}
		}
	}

	public void removeAllElectricalLoad(Iterable<ElectricalLoad> load) {
		if (load != null) {
			for (ElectricalLoad l : load) {
				removeElectricalLoad(l);
			}
		}
	}

	public void addAllThermalLoad(Iterable<ThermalLoad> load) {
		if (load != null) {
			for (ThermalLoad c : load) {
				addThermalLoad(c);
			}
		}
	}

	public void removeAllThermalLoad(Iterable<ThermalLoad> load) {
		if (load != null) {
			for (ThermalLoad c : load) {
				removeThermalLoad(c);
			}
		}
	}

	public void addAllSlowProcess(ArrayList<IProcess> process) {
		if (process != null) slowProcessList.addAll(process);
	}

	public void removeAllSlowProcess(ArrayList<IProcess> process) {
		if (process != null) slowProcessList.removeAll(process);
	}

	public void addAllElectricalProcess(ArrayList<IProcess> process) {
		if (process != null) electricalProcessList.addAll(process);
	}

	public void removeAllElectricalProcess(ArrayList<IProcess> process) {
		if (process != null) electricalProcessList.removeAll(process);
	}

	public void addAllThermalFastProcess(ArrayList<IProcess> process) {
		if (process != null) thermalFastProcessList.addAll(process);
	}

	public void removeAllThermalFastProcess(ArrayList<IProcess> process) {
		if (process != null) thermalFastProcessList.removeAll(process);
	}

	public void addAllThermalSlowProcess(ArrayList<IProcess> process) {
		if (process != null) thermalSlowProcessList.addAll(process);
	}

	public void removeAllThermalSlowProcess(ArrayList<IProcess> process) {
		if (process != null) thermalSlowProcessList.removeAll(process);
	}

	// private ArrayList<Double> conectionSerialConductance = new ArrayList<Double>();

	public boolean pleaseCrash = false;
	@SubscribeEvent
	public void tick(ServerTickEvent event) {
		if (event.phase != Phase.START) return;
		if(pleaseCrash) throw new StackOverflowError();
		long stackStart;

		long startTime = System.nanoTime();

		for (Object o : slowPreProcessList.toArray()) {
			IProcess process = (IProcess) o;
			process.process(1.0 / 20);
		}

		timeout += callPeriod;

		while (timeout > 0) {
			if (timeout < electricalTimeout && timeout < thermalTimeout) {
				thermalTimeout -= timeout;
				electricalTimeout -= timeout;
				timeout = 0;
				break;
			}

			double dt;

			if (electricalTimeout <= thermalTimeout) {
				dt = electricalTimeout;
				electricalTimeout += electricalPeriod;

				stackStart = System.nanoTime();

				mna.step();
				for (IProcess p : electricalProcessList) {
					p.process(electricalPeriod);
				}

				electricalNsStack += System.nanoTime() - stackStart;
			} else {
				dt = thermalTimeout;
				thermalTimeout += thermalPeriod;

				stackStart = System.nanoTime();
				// / Utils.print("*");

				thermalStep(thermalPeriod, thermalFastConnectionList, thermalFastProcessList, thermalFastLoadList);

				thermalFastNsStack += System.nanoTime() - stackStart;
			}
			thermalTimeout -= dt;
			electricalTimeout -= dt;
			timeout -= dt;
		}

		{
			stackStart = System.nanoTime();
			thermalStep(0.05, thermalSlowConnectionList, thermalSlowProcessList, thermalSlowLoadList);
			thermalSlowNsStack += System.nanoTime() - stackStart;
		}

		stackStart = System.nanoTime();

		for (Object o : slowProcessList.toArray()) {
			IProcess process = (IProcess) o;
			process.process(0.05);
		}

		for (IDestructable d : destructableSet) {
			d.destructImpl();
		}
		destructableSet.clear();

		slowNsStack += System.nanoTime() - stackStart;
		avgTickTime += 1.0 / 20 * ((int) (System.nanoTime() - startTime) / 1000);
		
		/*stackStart = System.nanoTime();
		for (int idx = 0; idx < 100; idx++) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
			DataOutputStream stream = new DataOutputStream(bos);

			try {
				for (int idx2 = 0; idx2 < 10; idx2++)
					stream.writeInt(idx2+0xAAAAAAAA);
			} catch (IOException e) {
				e.printStackTrace();
			}
			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

			for (Object obj : server.getConfigurationManager().playerEntityList) {
				EntityPlayerMP player = (EntityPlayerMP) obj;
				WorldServer worldServer = (WorldServer) MinecraftServer.getServer().worldServerForDimension(player.dimension);
				PlayerManager playerManager = worldServer.getPlayerManager();
				Utils.sendPacketToClient(bos, player);
			}
			//S3FPacketCustomPayload packet = new S3FPacketCustomPayload(Eln.channelName, bos.toByteArray());
			//Eln.instance.eventChannel.sendToAll(new FMLProxyPacket(packet));
		}
		
		Utils.println((System.nanoTime() - stackStart) / 1000);*/

		if (++printTimeCounter == 20) {
			printTimeCounter = 0;
			electricalNsStack /= 20;
			thermalFastNsStack /= 20;
			thermalSlowNsStack /= 20;
			slowNsStack /= 20;

			Utils.println("ticks " + new DecimalFormat("#").format((int) avgTickTime) + " us" + "  E " + electricalNsStack / 1000 + "  TF " + thermalFastNsStack / 1000 + "  TS " + thermalSlowNsStack / 1000 + "  S " + slowNsStack / 1000

					+ "    " + mna.getSubSystemCount() + " SS"
					+ "    " + electricalProcessList.size() + " EP"
					+ "    " + thermalFastLoadList.size() + " TFL"
					+ "    " + thermalFastConnectionList.size() + " TFC"
					+ "    " + thermalFastProcessList.size() + " TFP"
					+ "    " + thermalSlowLoadList.size() + " TSL"
					+ "    " + thermalSlowConnectionList.size() + " TSC"
					+ "    " + thermalSlowProcessList.size() + " TSP"
					+ "    " + slowProcessList.size() + " SP"
					);

			avgTickTime = 0;

			electricalNsStack = 0;
			thermalFastNsStack = 0;
			slowNsStack = 0;
			thermalSlowNsStack = 0;
		}
	}

	public boolean isRegistred(ElectricalLoad load) {
		return mna.isRegistred(load);
	}

	void thermalStep(double dt, Iterable<ThermalConnection> connectionList, Iterable<IProcess> processList, Iterable<ThermalLoad> loadList) {
		for (ThermalConnection c : connectionList) {
			double i;
			i = (c.L2.Tc - c.L1.Tc) / ((c.L2.Rs + c.L1.Rs));
			c.L1.PcTemp += i;
			c.L2.PcTemp -= i;

			c.L1.PrsTemp += Math.abs(i);
			c.L2.PrsTemp += Math.abs(i);
		}
		if (processList != null) {
			for (IProcess process : processList) {
				process.process(dt);
			}
		}
		for (ThermalLoad load : loadList) {
			load.PcTemp -= load.Tc / load.Rp;

			load.Tc += load.PcTemp * dt / load.C;

			load.Pc = load.PcTemp;
			load.Prs = load.PrsTemp;
			load.Psp = load.PspTemp;
			load.PcTemp = 0;
			load.PrsTemp = 0;
			load.PspTemp = 0;
		}
	}
}
