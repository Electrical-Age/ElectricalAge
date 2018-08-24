package mods.eln.sixnode.wirelesssignal;

import mods.eln.misc.Coordinate;
import mods.eln.sixnode.wirelesssignal.tx.WirelessSignalTxElement;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;
import java.util.Map.Entry;

public class WirelessUtils {

    private WirelessUtils() {
    }

    public static void getTx(IWirelessSignalSpot root, HashMap<String, HashSet<IWirelessSignalTx>> txSet, HashMap<IWirelessSignalTx, Double> txStrength) {
        HashSet<IWirelessSignalSpot> spotSet = new HashSet<IWirelessSignalSpot>();
        txSet.clear();
        txStrength.clear();
        getTx(root, txSet, txStrength, spotSet, true, 0);
    }

    private static void getTx(IWirelessSignalSpot from, HashMap<String, HashSet<IWirelessSignalTx>> txSet, HashMap<IWirelessSignalTx, Double> txStrength, HashSet<IWirelessSignalSpot> spotSet, boolean isRoot, double strength) {
        if (spotSet.contains(from)) return;

        spotSet.add(from);

        if (!isRoot) {
            for (List<IWirelessSignalTx> txs : from.getTx().values()) {
                for (IWirelessSignalTx tx : txs) {
                    if (isRoot)
                        strength = tx.getRange() - getVirtualDistance(tx.getCoordinate(), from.getCoordinate(), tx.getCoordinate().trueDistanceTo(from.getCoordinate()));
                    addTo(tx, strength, txSet, txStrength);
                }
            }
            for (IWirelessSignalSpot spot : from.getSpot()) {
                if (isRoot)
                    strength = spot.getRange() - getVirtualDistance(spot.getCoordinate(), from.getCoordinate(), spot.getCoordinate().trueDistanceTo(from.getCoordinate()));
                getTx(spot, txSet, txStrength, spotSet, false, strength);
            }
        } else {
            LinkedList<IWirelessSignalSpot> spots = new LinkedList<IWirelessSignalSpot>();
            spots.addAll(from.getSpot());

            LinkedList<IWirelessSignalTx> txs = new LinkedList<IWirelessSignalTx>();
            for (ArrayList<IWirelessSignalTx> txss : from.getTx().values()) {
                txs.addAll(txss);
            }

            double bestScore;
            Object best = null;
            while (!spots.isEmpty() || !txs.isEmpty()) {
                bestScore = Double.MAX_VALUE;
                for (IWirelessSignalSpot spot : spots) {
                    double temp = spot.getCoordinate().trueDistanceTo(from.getCoordinate());
                    if (temp < bestScore) {
                        bestScore = temp;
                        best = spot;
                    }
                }

                for (IWirelessSignalTx tx : txs) {
                    double temp = tx.getCoordinate().trueDistanceTo(from.getCoordinate());
                    if (temp < bestScore) {
                        bestScore = temp;
                        best = tx;
                    }
                }

                if (best instanceof IWirelessSignalSpot) {
                    IWirelessSignalSpot b = (IWirelessSignalSpot) best;
                    if (isRoot)
                        strength = b.getRange() - getVirtualDistance(b.getCoordinate(), from.getCoordinate(), b.getCoordinate().trueDistanceTo(from.getCoordinate()));
                    getTx(b, txSet, txStrength, spotSet, false, strength);
                    spots.remove(best);
                } else if (best == null) {
                    break;
                } else {
                    IWirelessSignalTx tx = (IWirelessSignalTx) best;

                    if (isRoot)
                        strength = tx.getRange() - getVirtualDistance(tx.getCoordinate(), from.getCoordinate(), tx.getCoordinate().trueDistanceTo(from.getCoordinate()));
                    addTo(tx, strength, txSet, txStrength);
                    txs.remove(best);
                }
            }
        }
    }

    private static void addTo(IWirelessSignalTx tx, double strength, Map<String, HashSet<IWirelessSignalTx>> reg, Map<IWirelessSignalTx, Double> txStrength) {
        String channel = tx.getChannel();
        HashSet<IWirelessSignalTx> ch = reg.get(channel);
        if (ch != null && ch.contains(tx)) return;
        if (ch == null)
            reg.put(channel, ch = new HashSet<IWirelessSignalTx>());
        ch.add(tx);
        txStrength.put(tx, strength);
    }

	/*
     *
	 * public static HashSet<IWirelessSignalTx> getTx(String channel,IWirelessSignalSpot root){ HashSet<IWirelessSignalTx> txSet = new HashSet<IWirelessSignalTx>(); getTx(channel, root,txSet); return txSet; }
	 * 
	 * private static void getTx(String channel,IWirelessSignalSpot root,HashSet<IWirelessSignalTx> txSet){ for(IWirelessSignalSpot spot : root.getSpot()){ getTx(channel, spot, txSet); }
	 * 
	 * if(channel != null){ ArrayList<IWirelessSignalTx> txs = root.getTx().get(channel); if(txs != null) txSet.addAll(txs); }else{ for(ArrayList<IWirelessSignalTx> txs : root.getTx().values()){ txSet.addAll(txs); } } }
	 */

    public static WirelessSignalSpot buildSpot(Coordinate c, String channel, int range) {
        HashMap<String, ArrayList<IWirelessSignalTx>> txs = new HashMap<String, ArrayList<IWirelessSignalTx>>();
        ArrayList<IWirelessSignalSpot> spots = new ArrayList<IWirelessSignalSpot>();

        for (IWirelessSignalSpot spot : IWirelessSignalSpot.spots) {
            if (isInRange(spot.getCoordinate(), c, spot.getRange())) {
                spots.add(spot);
            }
        }

        if (channel != null) {
            ArrayList<IWirelessSignalTx> inRangeTx = new ArrayList<IWirelessSignalTx>();

            List<IWirelessSignalTx> sameChannelTx = WirelessSignalTxElement.channelMap.get(channel);
            if (sameChannelTx != null) {
                for (IWirelessSignalTx tx : sameChannelTx) {
                    if (isInRange(tx.getCoordinate(), c, tx.getRange())) {
                        inRangeTx.add(tx);
                    }
                }
            }
            if (!inRangeTx.isEmpty())
                txs.put(channel, inRangeTx);
        } else {
            for (Entry<String, ArrayList<IWirelessSignalTx>> entryTxs : WirelessSignalTxElement.channelMap.entrySet()) {
                ArrayList<IWirelessSignalTx> inRangeTx = new ArrayList<IWirelessSignalTx>();

                for (IWirelessSignalTx tx : entryTxs.getValue()) {
                    if (isInRange(tx.getCoordinate(), c, tx.getRange())) {
                        inRangeTx.add(tx);
                    }
                }

                if (!inRangeTx.isEmpty())
                    txs.put(entryTxs.getKey(), inRangeTx);
            }
        }

        return new WirelessSignalSpot(txs, spots, c, range);
    }

    static private boolean isInRange(Coordinate txC, Coordinate rxC, double range) {
        double distance = txC.trueDistanceTo(rxC);
        if (distance > range) return false;
        if (getVirtualDistance(txC, rxC, distance) > range) return false;
        return true;
    }

    static private double getVirtualDistance(Coordinate txC, Coordinate rxC, double distance) {
        double virtualDistance = distance;
        if (distance > 2) {
            double vx, vy, vz;
            double dx, dy, dz;
            vx = rxC.pos.getX() + 0.5;
            vy = rxC.pos.getY() + 0.5;
            vz = rxC.pos.getZ() + 0.5;

            dx = (txC.pos.getX() - rxC.pos.getX()) / distance;
            dy = (txC.pos.getY() - rxC.pos.getY()) / distance;
            dz = (txC.pos.getZ() - rxC.pos.getZ()) / distance;
            Coordinate c = new Coordinate();
            c.setDimension(rxC.getDimension());

            for (int idx = 0; idx < distance - 1; idx++) {
                vx += dx;
                vy += dy;
                vz += dz;
                c.setPosition(new Vec3d(vx, vy, vz));
                if (c.doesBlockExist()) {
                    Block b = c.getBlockState().getBlock();
                    World w = c.world();

                    virtualDistance +=
                        b.isOpaqueCube(b.getDefaultState()) &&
                        !b.isAir(b.getDefaultState(), w,new BlockPos(c.pos.getX(), c.pos.getY(), c.pos.getZ())) ?
                        2.0 : 0.0;
                }
            }
        }
        return virtualDistance;
    }

    public static class WirelessSignalSpot implements IWirelessSignalSpot {

        HashMap<String, ArrayList<IWirelessSignalTx>> txs;
        ArrayList<IWirelessSignalSpot> spots;
        Coordinate coordinate;
        int range;

        public WirelessSignalSpot(HashMap<String, ArrayList<IWirelessSignalTx>> txs, ArrayList<IWirelessSignalSpot> spots, Coordinate coordinate, int range) {
            this.txs = txs;
            this.spots = spots;
            this.coordinate = coordinate;
            this.range = range;
        }

        @Override
        public HashMap<String, ArrayList<IWirelessSignalTx>> getTx() {
            return txs;
        }

        @Override
        public ArrayList<IWirelessSignalSpot> getSpot() {
            return spots;
        }

        @Override
        public Coordinate getCoordinate() {
            return coordinate;
        }

        @Override
        public int getRange() {
            return range;
        }
    }
}
