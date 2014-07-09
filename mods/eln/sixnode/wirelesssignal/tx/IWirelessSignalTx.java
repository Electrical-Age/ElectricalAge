package mods.eln.sixnode.wirelesssignal.tx;

import mods.eln.misc.Coordonate;

public interface IWirelessSignalTx {
	public Coordonate getCoordonate();
	
	public int getRange();
	public String getChannel();
	public double getValue();
}
