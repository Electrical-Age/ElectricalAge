package mods.eln.sixnode.wirelesssignal;

import mods.eln.misc.Coordonate;

import java.util.ArrayList;
import java.util.HashMap;

public interface IWirelessSignalSpot {

    public static ArrayList<IWirelessSignalSpot> spots = new ArrayList<IWirelessSignalSpot>();

    HashMap<String, ArrayList<IWirelessSignalTx>> getTx();

    ArrayList<IWirelessSignalSpot> getSpot();

    Coordonate getCoordonate();

    public int getRange();
}
