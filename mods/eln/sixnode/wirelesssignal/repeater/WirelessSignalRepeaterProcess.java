package mods.eln.sixnode.wirelesssignal.repeater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;


import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import mods.eln.misc.Coordonate;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;
import mods.eln.sixnode.wirelesssignal.WirelessUtils;
import mods.eln.sixnode.wirelesssignal.aggregator.IWirelessSignalAggregator;

public class WirelessSignalRepeaterProcess implements IProcess,IWirelessSignalSpot{

	private WirelessSignalRepeaterElement rx;

	public WirelessSignalRepeaterProcess(WirelessSignalRepeaterElement rx) {
		this.rx = rx;
	}
	
	double sleepTimer = 0;
	IWirelessSignalSpot spot;
	

	@Override
	public void process(double time) {
		sleepTimer -= time;
		if(sleepTimer < 0){
			sleepTimer += Utils.rand(1.2, 2);
	
			spot = WirelessUtils.buildSpot(rx.getCoordonate(), null, rx.descriptor.range);	
		}
		
		
	}


	@Override
	public HashMap<String, ArrayList<IWirelessSignalTx>> getTx() {
		// TODO Auto-generated method stub
		return spot.getTx();
	}

	@Override
	public ArrayList<IWirelessSignalSpot> getSpot() {
		// TODO Auto-generated method stub
		return spot.getSpot();
	}

	@Override
	public Coordonate getCoordonate() {
		// TODO Auto-generated method stub
		return rx.getCoordonate();
	}

	@Override
	public int getRange() {
		// TODO Auto-generated method stub
		return rx.descriptor.range;
	}

}





