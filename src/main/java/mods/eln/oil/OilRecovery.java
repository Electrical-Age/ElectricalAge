package mods.eln.oil;

import com.mojang.serialization.Codec;
import mods.eln.reservoir.Reservoir;
import mods.eln.reservoir.Reservoirs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * How an oil lake gives up its last oil (todo #138; Baughn 2026-10-30, after Argon's a3):
 * <ul>
 * <li>primary: down to TAIL_SHARE of its capacity, a pump takes oil from the lake at its full rate;</li>
 * <li>seepage: below that the lake never runs dry: a pump gets SEEP_SHARE of its own nominal rate, from fractures, and
 * the lake stays at its tail level;</li>
 * <li>boosted: steam pumped down a second well (SteamInjectionElement) pays for oil at STEAM_KG_PER_BUCKET, and while
 * that lasts the lake's pumps run at their full rate again.</li>
 * </ul>
 * The steam's credit is kept per lake (SavedData eln:oil_recovery). Water lakes (Watershed) are not touched: they run
 * dry (Baughn: no infinite water).
 */
public final class OilRecovery extends SavedData {
    /** Config (ElnConfig oil.*): the tail's start (share of capacity), seepage (share of a pump's nominal rate), and
     *  the steam injected per bucket of crude recovered (kg). */
    public static volatile double tailShare = 0.2, seepShare = 0.18, steamKgPerBucket = 0.2;
    /** The most crude (mB) steam may have paid for ahead: one oil pump's tank. */
    public static final double BOOST_CAP_MB = OilPumpDescriptor.TANK_MB;

    public enum Regime { PRIMARY, SEEPAGE, BOOSTED }

    final Map<String, Double> boost;

    OilRecovery(Map<String, Double> boost) {
        this.boost = new HashMap<>(boost);
    }

    static final Codec<OilRecovery> CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).xmap(OilRecovery::new, r -> r.boost)
        .fieldOf("boost").codec();

    static final SavedDataType<OilRecovery> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("eln", "oil_recovery"),
        () -> new OilRecovery(Map.of()), CODEC);

    public static OilRecovery get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Is it an oil lake (not a water one)? */
    public static boolean oil(Reservoir r) {
        return r != null && r.kind().equals(OilReservoirs.KIND);
    }

    /** The amount (mB) below which an oil lake seeps: TAIL_SHARE of its capacity. */
    public static long tail(Reservoir r) {
        return (long) Math.floor(tailShare * r.capacity());
    }

    /** Crude (mB) the steam injected into the lake has paid for and pumps haven't taken yet. */
    public double boost(Reservoir r) {
        return boost.getOrDefault(Integer.toString(r.id()), 0.0);
    }

    public Regime regime(Reservoir r) {
        if (r.amount() > tail(r)) return Regime.PRIMARY;
        return boost(r) >= 1 ? Regime.BOOSTED : Regime.SEEPAGE;
    }

    /** Steam (kg) into the lake: crude paid for (mB). Returns the steam it took (kg): none past BOOST_CAP_MB. */
    public double inject(Reservoir r, double steamKg) {
        double room = Math.max(0, BOOST_CAP_MB - boost(r));
        double mb = Math.min(room, steamKg / steamKgPerBucket * 1000);
        if (mb <= 0) return 0;
        boost.merge(Integer.toString(r.id()), mb, Double::sum);
        setDirty();
        return mb / 1000 * steamKgPerBucket;
    }

    /**
     * A pump takes up to `want` mB: from the lake down to its tail, then what steam has paid for, then up to `seep` mB
     * of seepage (its share of the pump's nominal rate this step). Returns {mB taken, regime ordinal, seepage taken}.
     */
    public long[] take(ServerLevel level, Reservoir r, long want, long seep) {
        long got = Reservoirs.take(level, r, Math.min(want, Math.max(0, r.amount() - tail(r))), false);
        if (got >= want) return new long[]{got, Regime.PRIMARY.ordinal(), 0};
        long rest = want - got;
        double b = boost(r);
        if (b >= 1) {
            long fromBoost = (long) Math.min(rest, Math.floor(b));
            boost.put(Integer.toString(r.id()), b - fromBoost);
            setDirty();
            return new long[]{got + fromBoost, Regime.BOOSTED.ordinal(), 0};
        }
        long s = Math.min(rest, seep);
        return new long[]{got + s, (got > 0 ? Regime.PRIMARY : Regime.SEEPAGE).ordinal(), s};
    }

    /** For Jade: the lake's state in a few words. */
    public static String describe(ServerLevel level, Reservoir r) {
        long cap = Math.max(1, r.capacity());
        int pct = (int) Math.round(100.0 * r.amount() / cap);
        return switch (get(level).regime(r)) {
            case PRIMARY -> String.format("lake #%d, %d%% full", r.id(), pct);
            case SEEPAGE -> String.format("lake #%d seeping: %.0f%% of a pump's rate", r.id(), seepShare * 100);
            case BOOSTED -> String.format("lake #%d boosted by steam: %.1f buckets paid", r.id(), get(level).boost(r) / 1000);
        };
    }
}
