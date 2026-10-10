package mods.eln.sixnode.wirelesssignal.aggregator;

import mods.eln.sixnode.wirelesssignal.IWirelessSignalTx;

import java.util.Collection;

public interface IWirelessSignalAggregator {
    double aggregate(Collection<IWirelessSignalTx> txs);
}
