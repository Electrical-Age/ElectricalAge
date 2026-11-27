package mods.eln.sixnode.wirelesssignal;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.sixnode.wirelesssignal.WirelessUtils.WirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import static mods.eln.i18n.I18N.tr;

public class WirelessSignalAnalyserItemDescriptor extends GenericItemUsingDamageDescriptor {

    public WirelessSignalAnalyserItemDescriptor(String name) {
        super(name);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Scans nearby wireless signal channels on right-click."));
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, Level world, int x, int y, int z, int side, float vx, float vy, float vz) {
        if (world.isClientSide()) return true;
        Direction dir = Direction.fromIntMinecraftSide(side);
        Coordonate c = new Coordonate(x, y, z, world);
        c.move(dir);

        WirelessSignalSpot spot = WirelessUtils.buildSpot(c, null, 0);
        HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
        HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();
        WirelessUtils.getTx(spot, txSet, txStrength);

        BiggerAggregator aggregator = new BiggerAggregator();

        if (txSet.isEmpty()) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.literal(tr("No wireless signals in range.")));
            return true;
        }

        for (Entry<String, HashSet<IWirelessSignalTx>> entrySet : txSet.entrySet()) {
            HashSet<IWirelessSignalTx> set = entrySet.getValue();
            double strength = 100000;
            for (IWirelessSignalTx oneTx : set) {
                double temp = txStrength.get(oneTx);
                if (temp < strength) strength = temp;
            }
            Utils.sendMessage(player, tr("%1$: %2$% (strength %3$)",
                entrySet.getKey(),
                String.format("%.0f", aggregator.aggregate(set) * 100),
                String.format("%.1f", strength)));
        }
        return true;
    }
}
