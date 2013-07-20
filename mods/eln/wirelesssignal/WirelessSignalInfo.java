package mods.eln.wirelesssignal;

public class WirelessSignalInfo {
	WirelessSignalInfo(IWirelessSignalTx tx,int generation,double power)
	{
		this.tx = tx;
		this.generation = generation;
		this.power = power;
	}
	IWirelessSignalTx tx;
	int generation;
	double power;	
}
