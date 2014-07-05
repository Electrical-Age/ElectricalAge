package mods.eln.sixnode.wirelesssignal;

import mods.eln.misc.Coordonate;

public interface IWirelessSignalTx {
	public Coordonate getCoordonate();
	
	public int getRange();
	public String getChannel();
	public int getGeneration();
	public double getValue();



}
