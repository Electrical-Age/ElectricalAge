package mods.eln.sixnode.wirelesssignal.aggregator;

import java.util.Collection;

import mods.eln.sixnode.wirelesssignal.tx.IWirelessSignalTx;

public interface IWirelessSignalAggregator {
	double aggregate(Collection<IWirelessSignalTx> txs);
}
