package mods.eln.sixnode.wirelesssignal.rx;

import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.WirelessUtils;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashMap;
import java.util.HashSet;

public class WirelessSignalRxProcess implements IProcess, INBTTReady {

	private WirelessSignalRxElement rx;

    double sleepTimer = 0;

    HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
    HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();

	public WirelessSignalRxProcess(WirelessSignalRxElement rx) {
		this.rx = rx;
	}

	@Override
	public void process(double time) {
		double output = 0;
		sleepTimer -= time;

		if (sleepTimer < 0) {
			sleepTimer += Utils.rand(1.2, 2);

			IWirelessSignalSpot spot = WirelessUtils.buildSpot(rx.getCoordonate(), rx.channel, 0);
			WirelessUtils.getTx(spot, txSet, txStrength);
		}

		HashSet<IWirelessSignalTx> txs = txSet.get(rx.channel);
		if (txs == null) {
			output = 0;
			rx.setConnection(false);
		} else {
			output = rx.getAggregator().aggregate(txs);
			rx.setConnection(true);
		}

		rx.outputGateProcess.setOutputNormalized(output);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
	}
}
