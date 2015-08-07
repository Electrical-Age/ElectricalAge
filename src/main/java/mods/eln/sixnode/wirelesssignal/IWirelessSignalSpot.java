package mods.eln.sixnode.wirelesssignal;

import java.util.ArrayList;
import java.util.HashMap;

import mods.eln.misc.Coordinate;

public interface IWirelessSignalSpot {
    
	public static ArrayList<IWirelessSignalSpot> spots = new ArrayList<IWirelessSignalSpot>();
	
	HashMap<String, ArrayList<IWirelessSignalTx>> getTx();
	ArrayList<IWirelessSignalSpot> getSpot();
	Coordinate getCoordinate();
	public int getRange();
}
