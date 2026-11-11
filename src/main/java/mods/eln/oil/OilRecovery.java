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
 * How an oil lake gives up its last oil (todo #138; Baughn 2026-10-30, after Argon's a3; reworked 2026-11-08):
 * <ul>
 * <li>primary: a pump takes oil from the lake at its full rate, all of it if it can;</li>
 * <li>seepage: the lake refills from fractures at a rate lerped by its fill (Baughn 2026-11-11, "so it's easier to
 * tell what's going on"): none when full, SEEP_SHARE of a 230V oil pump's nominal rate at FULL_AT of its capacity and
 * below. The seepage goes into the lake, not to the pumps (Baughn 2026-11-08: more pumps mustn't mean more oil), so the
 * lake's pumps share it by draining what came in. It is caught up lazily, by the game time since the lake was last
 * looked at, so a lake left alone refills (ever more slowly as it nears full);</li>
 * <li>boosted: steam pumped down a second well (SteamInjectionElement) pays for oil at STEAM_KG_PER_BUCKET, which raises
 * the seepage, by the same lerp, to STEAM_SHARE of a pump's rate (100 %: a pump's full rate at FULL_AT and below).</li>
 * </ul>
 * Surface ponds (OilPonds, keys "oil-pond:") don't recover at all: they run dry (Baughn 2026-11-08: only the lake below
 * regenerates). Water lakes (Watershed) aren't touched either (Baughn: no infinite water). The steam's credit and each
 * lake's last seepage time are kept per lake (SavedData eln:oil_recovery).
 */
public final class OilRecovery extends SavedData {
    /** Config (ElnConfig oil.*): the fill (share of capacity) at and below which seepage is at its most; that most
     *  without steam and with it (shares of a pump's nominal rate); the steam injected per bucket of crude (kg). */
    public static volatile double fullAt = 0.25, seepShare = 0.25, steamShare = 1.0, steamKgPerBucket = 0.2;
    /** The most crude (mB) steam may have paid for ahead: one oil pump's tank. */
    public static final double BOOST_CAP_MB = OilPumpDescriptor.TANK_MB;

    public enum Regime { PRIMARY, SEEPAGE, BOOSTED }

    final Map<String, Double> boost;
    /** Per lake: the game time its seepage was last caught up to, and the fraction of a mB not given yet. */
    final Map<String, Long> seepTime;
    final Map<Integer, Double> seepCarry = new HashMap<>();

    /** A 230V oil pump's nominal rate from 40 blocks down (its tooltip's), mB/s. */
    static double pumpRate() {
        return 4000 / OilPumpDescriptor.energyPerMb(false, 40);
    }

    /** A lake's most seepage (mB/s), at FULL_AT of its capacity and below: SEEP_SHARE of a pump's nominal rate. */
    public static double seepRate() {
        return seepShare * pumpRate();
    }

    /** The lerp by fill: 0 when full, 1 at FULL_AT of its capacity and below. */
    public static double fillFactor(Reservoir r) {
        double c = Math.max(1, r.capacity()), span = c * (1 - fullAt);
        return span <= 0 ? (r.amount() < c ? 1 : 0) : Math.max(0, Math.min(1, (c - r.amount()) / span));
    }

    /** The lake's seepage now (mB/s), without steam. */
    public static double seepRate(Reservoir r) {
        return seepRate() * fillFactor(r);
    }

    /**
     * What a lake holding a of capacity c gains in dt seconds at most rate R (mB/s), lerped by fill: R up to FULL_AT,
     * then an exponential approach to full (the rate falls linearly with the room left).
     */
    public static double gain(double a, double c, double R, double dt) {
        if (R <= 0 || dt <= 0 || a >= c) return 0;
        double cf = fullAt * c, g = 0;
        if (a < cf) {
            if (R * dt <= cf - a) return R * dt;
            dt -= (cf - a) / R;
            g = cf - a;
            a = cf;
        }
        double span = c - cf;
        if (span <= 0) return g;
        return g + (c - a) * (1 - Math.exp(-dt * R / span));
    }

    OilRecovery(Map<String, Double> boost, Map<String, Long> seepTime) {
        this.boost = new HashMap<>(boost);
        this.seepTime = new HashMap<>(seepTime);
    }

    static final Codec<OilRecovery> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("boost").forGetter(r -> r.boost),
        // (saves before 2026-11-08 have none: lakes start seeping from when they're next looked at)
        Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("seep_time", Map.of()).forGetter(r -> r.seepTime)
    ).apply(i, OilRecovery::new));

    static final SavedDataType<OilRecovery> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath("eln", "oil_recovery"),
        () -> new OilRecovery(Map.of(), Map.of()), CODEC);

    public static OilRecovery get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Is it an oil lake or pond (not a water one)? */
    public static boolean oil(Reservoir r) {
        return r != null && r.kind().equals(OilReservoirs.KIND);
    }

    /** Reservoirs found to be surface ponds, by id (not saved: a pond's key never changes). */
    final Map<Integer, Boolean> pond = new HashMap<>();

    /** Does it recover (seepage, steam)? Oil lakes do; surface ponds don't (Baughn 2026-11-08). */
    public static boolean recovers(ServerLevel level, Reservoir r) {
        if (!oil(r)) return false;
        return !get(level).pond.computeIfAbsent(r.id(), id -> {
            String k = mods.eln.reservoir.ReservoirData.get(level).keyOf(id);
            return k != null && k.startsWith(OilPonds.POND);
        });
    }

    /** The amount (mB) at and below which an oil lake seeps at its most: FULL_AT of its capacity. */
    public static long tail(Reservoir r) {
        return (long) Math.floor(fullAt * r.capacity());
    }

    /** Crude (mB) the steam injected into the lake has paid for and pumps haven't taken yet. */
    public double boost(Reservoir r) {
        return boost.getOrDefault(Integer.toString(r.id()), 0.0);
    }

    public Regime regime(Reservoir r) {
        if (r.amount() > tail(r)) return Regime.PRIMARY;
        return boost(r) >= 1 ? Regime.BOOSTED : Regime.SEEPAGE;
    }

    /**
     * Catches the lake's seepage up to now, for the game time since it was last done: the lerped seepage, plus what the
     * steam paid for raises it towards STEAM_SHARE (that extra taken from the steam's credit). Returns the mB given.
     */
    public long seep(ServerLevel level, Reservoir r) {
        if (!recovers(level, r)) return 0;
        String k = Integer.toString(r.id());
        long now = level.getGameTime();
        Long last = seepTime.put(k, now);
        setDirty();
        if (last == null || now <= last) return 0;
        long room = r.capacity() - r.amount();
        if (room <= 0) {
            seepCarry.remove(r.id());
            return 0;
        }
        double dt = (now - last) / 20.0, a = r.amount(), c = r.capacity();
        double base = gain(a, c, seepRate(), dt), extra = 0, b = boost(r);
        if (b >= 1 && steamShare > seepShare) {
            extra = Math.min(b, gain(a, c, steamShare * pumpRate(), dt) - base);
            boost.put(k, b - extra);
        }
        double mb = base + extra + seepCarry.getOrDefault(r.id(), 0.0);
        long whole = (long) Math.min(Math.floor(mb), room);
        seepCarry.put(r.id(), whole < room ? mb - whole : 0.0);
        return whole > 0 ? Reservoirs.give(level, r, whole, false) : 0;
    }

    /** GameTests: pretend the lake's seepage was last caught up at this game time. */
    public void seepTimeForTests(Reservoir r, long time) {
        seepTime.put(Integer.toString(r.id()), time);
    }

    /** Steam (kg) into the lake: crude paid for (mB). Returns the steam it took (kg): none past BOOST_CAP_MB. */
    public double inject(ServerLevel level, Reservoir r, double steamKg) {
        if (!recovers(level, r)) return 0;
        double room = Math.max(0, BOOST_CAP_MB - boost(r));
        double mb = Math.min(room, steamKg / steamKgPerBucket * 1000);
        if (mb <= 0) return 0;
        boost.merge(Integer.toString(r.id()), mb, Double::sum);
        setDirty();
        return mb / 1000 * steamKgPerBucket;
    }

    /**
     * A pump takes up to `want` mB: the lake's seepage (and what steam paid for) caught up first, then from the lake.
     * Returns {mB taken, regime ordinal}.
     */
    public long[] take(ServerLevel level, Reservoir r, long want) {
        seep(level, r);
        Regime before = regime(r);
        long got = Reservoirs.take(level, r, want, false);
        return new long[]{got, before.ordinal()};
    }

    /** For Jade: the lake's state in a few words. */
    public static String describe(ServerLevel level, Reservoir r) {
        if (!recovers(level, r)) return String.format("pond #%d, %d%% full (ponds don't refill)", r.id(), (int) Math.round(100.0 * r.amount() / Math.max(1, r.capacity())));
        get(level).seep(level, r);
        long cap = Math.max(1, r.capacity());
        int pct = (int) Math.round(100.0 * r.amount() / cap);
        return switch (get(level).regime(r)) {
            case PRIMARY -> r.amount() >= cap ? String.format("lake #%d, full", r.id())
                : String.format("lake #%d, %d%% full, seeping %.1f mB/s", r.id(), pct, seepRate(r));
            case SEEPAGE -> String.format("lake #%d seeping %.1f mB/s, %d%% full", r.id(), seepRate(r), pct);
            case BOOSTED -> String.format("lake #%d seeping %.1f mB/s, boosted by steam (%.1f buckets paid), %d%% full", r.id(),
                steamShare * pumpRate() * fillFactor(r), get(level).boost(r) / 1000, pct);
        };
    }
}
