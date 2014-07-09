package mods.eln.sixnode.wirelesssignal.aggregator;

import java.util.Collection;

import mods.eln.sixnode.wirelesssignal.tx.IWirelessSignalTx;

public interface IWirelessSignalAggregator {
	IWirelessSignalTx aggregate(Collection<IWirelessSignalTx> txs);
}
