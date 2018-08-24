package mods.eln.sixnode.wirelesssignal;

import mods.eln.misc.Coordinate;

import java.util.ArrayList;
import java.util.HashMap;

public interface IWirelessSignalSpot {

    public static ArrayList<IWirelessSignalSpot> spots = new ArrayList<IWirelessSignalSpot>();

    HashMap<String, ArrayList<IWirelessSignalTx>> getTx();

    ArrayList<IWirelessSignalSpot> getSpot();

    Coordinate getCoordinate();

    public int getRange();
}
