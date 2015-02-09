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

	/*
	 * static public double getVirtualDistance(double distance,Coordonate txC,Coordonate rxC) { double virtualDistance = distance; if(distance > 2){ double vx,vy,vz; double dx,dy,dz; vx = rxC.x + 0.5; vy = rxC.y + 0.5; vz = rxC.z + 0.5;
	 * 
	 * dx = (txC.x - rxC.x)/distance; dy = (txC.y - rxC.y)/distance; dz = (txC.z - rxC.z)/distance; Coordonate c = new Coordonate(); c.setDimention(rxC.dimention);
	 * 
	 * for(int idx = 0;idx < distance - 1;idx++){ vx += dx; vy += dy; vz += dz; c.x = (int) vx; c.y = (int) vy; c.z = (int) vz; if(c.getBlockExist() == false) return Double.NaN; Block b = c.getBlock(); if(b != null && b.isOpaqueCube()){ virtualDistance += 2.0; }
	 * 
	 * } } return virtualDistance; }
	 * 
	 * 
	 * public static ArrayList<WirelessSignalInfo> getTxList(Coordonate rxC) { ArrayList<WirelessSignalInfo> list = new ArrayList<WirelessSignalInfo>();
	 * 
	 * for(ArrayList<IWirelessSignalTx> txList : WirelessSignalTxElement.channelMap.values()){ IWirelessSignalTx bestTx = null; float bestPower = -1; int bestGeneration = 1000;
	 * 
	 * if(txList != null) { int x = rxC.x; int y = rxC.y; int z = rxC.z; for(IWirelessSignalTx tx : txList){ Coordonate txC = tx.getCoordonate(); double distance = txC.trueDistanceTo(rxC);
	 * 
	 * if(txC.dimention == rxC.dimention && distance <= tx.getRange() && tx.getGeneration() < 100){ float power = 0f;
	 * 
	 * if(Double.isNaN(distance = getVirtualDistance(distance, txC, rxC))) continue; power = (float) (tx.getRange() - distance);
	 * 
	 * 
	 * 
	 * if(power < -0.1) continue;
	 * 
	 * 
	 * if(power > bestPower){ bestPower = power; bestTx = tx; bestGeneration = tx.getGeneration(); } } } }
	 * 
	 * 
	 * if(bestTx != null){ WirelessSignalInfo s = new WirelessSignalInfo(bestTx,bestGeneration,bestPower); list.add(s); } } return list; }
	 */

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
	}
}
