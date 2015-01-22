package mods.eln.sixnode.wirelesssignal;

import java.util.ArrayList;
import java.util.HashMap;

import mods.eln.misc.Coordonate;

public interface IWirelessSignalSpot {
	public static ArrayList<IWirelessSignalSpot> spots = new ArrayList<IWirelessSignalSpot>();
	
	HashMap<String, ArrayList<IWirelessSignalTx>> getTx();
	ArrayList<IWirelessSignalSpot> getSpot();
	Coordonate getCoordonate();
	public int getRange();
}
