package mods.eln.sixnode.wirelesssignal;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Coordinate;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.sixnode.wirelesssignal.WirelessUtils.WirelessSignalSpot;
import mods.eln.sixnode.wirelesssignal.aggregator.BiggerAggregator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class WirelessSignalAnalyserItemDescriptor extends GenericItemUsingDamageDescriptor {

    public WirelessSignalAnalyserItemDescriptor(String name) {
        super(name);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float vx, float vy, float vz) {
        if (world.isRemote) return EnumActionResult.PASS;
        Utils.addChatMessage(player, "-------------------");
        Direction dir = Direction.fromFacing(side);
        Coordinate c = new Coordinate(pos, world);
        c.move(dir);

        WirelessSignalSpot spot = WirelessUtils.buildSpot(c, null, 0);
        HashMap<String, HashSet<IWirelessSignalTx>> txSet = new HashMap<String, HashSet<IWirelessSignalTx>>();
        HashMap<IWirelessSignalTx, Double> txStrength = new HashMap<IWirelessSignalTx, Double>();
        WirelessUtils.getTx(spot, txSet, txStrength);

        BiggerAggregator aggregator = new BiggerAggregator();

        for (Entry<String, HashSet<IWirelessSignalTx>> entrySet : txSet.entrySet()) {
            HashSet<IWirelessSignalTx> set = entrySet.getValue();
            double strength = 100000;
            for (IWirelessSignalTx oneTx : set) {
                double temp = txStrength.get(oneTx);
                if (temp < strength) strength = temp;
            }
            Utils.addChatMessage(player, entrySet.getKey() + " Strength=" + String.format("%2.1f", strength) + " Value=" + String.format("%3.0f", aggregator.aggregate(set) * 100) + "%");
        }

        if (txSet.isEmpty()) {
            Utils.addChatMessage(player, "No wireless signal in area!");
        }
        /*ArrayList<WirelessSignalInfo> list = WirelessSignalRxProcess.getTxList(c);
		int idx = 0;
		for (WirelessSignalInfo e : list) {
			Utils.addChatMessage(player, e.tx.getChannel() + " Strength=" + String.format("%2.1f", e.power) + " Value=" + String.format("%2.1fV", e.tx.getValue() * Eln.instance.SVU));
			idx++;
		}
		if (list.size() == 0) {
			Utils.addChatMessage(player, "No wireless signal in area!");
		}*/
        return EnumActionResult.PASS;
    }
}
