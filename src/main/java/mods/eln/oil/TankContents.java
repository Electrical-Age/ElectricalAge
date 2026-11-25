package mods.eln.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mods.eln.fluid.FluidSaves;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.FluidNode;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Map;
import java.util.TreeMap;

/**
 * What a broken tank carries (item component eln:tank_contents; Baughn 2026-11-25: a broken tank keeps its contents,
 * any mix, partial fill or gas, and places back with them; frozen while carried). The contents as FluidSaves keeps them:
 * water, air (kg), energy (J), the other species by key (kg; keys, not ids, which are per session), the format they
 * were taken in, and the vessel's volume (m^3). Placing restores them through FluidSaves (so a corrupt item heals and a
 * species no datapack defines is kept). Also what it read when taken (temperature, pressure, the Jade contents and
 * composition), for its tooltip: species data lives on the server, and the contents don't change while carried. The
 * volume is informational: placed, the contents go into the tank's own volume.
 */
public record TankContents(double water, double air, double energy, Map<String, Double> species, int format, double volume,
                           double kelvin, double pascal, String contents, String composition) {

    public TankContents {
        species = Map.copyOf(species);
    }

    public static final Codec<TankContents> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.DOUBLE.fieldOf("W").forGetter(TankContents::water),
        Codec.DOUBLE.fieldOf("A").forGetter(TankContents::air),
        Codec.DOUBLE.fieldOf("U").forGetter(TankContents::energy),
        Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("S", Map.of()).forGetter(TankContents::species),
        Codec.INT.optionalFieldOf("Fv", 0).forGetter(TankContents::format),
        Codec.DOUBLE.fieldOf("V").forGetter(TankContents::volume),
        Codec.DOUBLE.optionalFieldOf("kelvin", Water.T_AMBIENT).forGetter(TankContents::kelvin),
        Codec.DOUBLE.optionalFieldOf("pascal", Water.P_ATM).forGetter(TankContents::pascal),
        Codec.STRING.optionalFieldOf("contents", "").forGetter(TankContents::contents),
        Codec.STRING.optionalFieldOf("composition", "").forGetter(TankContents::composition)
    ).apply(i, TankContents::new));

    private static final StreamCodec<io.netty.buffer.ByteBuf, Map<String, Double>> SPECIES_STREAM =
        ByteBufCodecs.<io.netty.buffer.ByteBuf, String, Double, Map<String, Double>>map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE);

    public static final StreamCodec<RegistryFriendlyByteBuf, TankContents> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE, TankContents::water,
        ByteBufCodecs.DOUBLE, TankContents::air,
        ByteBufCodecs.DOUBLE, TankContents::energy,
        SPECIES_STREAM, TankContents::species,
        ByteBufCodecs.VAR_INT, TankContents::format,
        ByteBufCodecs.DOUBLE, TankContents::volume,
        ByteBufCodecs.DOUBLE, TankContents::kelvin,
        ByteBufCodecs.DOUBLE, TankContents::pascal,
        ByteBufCodecs.STRING_UTF8, TankContents::contents,
        ByteBufCodecs.STRING_UTF8, TankContents::composition,
        TankContents::new);

    /** Liquid below this (m^3: half a mB of water) counts as none (EaUnits shows it as 0 mB). */
    static final double NO_LIQUID_M3 = 0.5 * EaUnits.KG_PER_MB_WATER / Water.RHO_L;
    /** Gas up to this pressure (Pa) is not pressurised: a fresh tank's air, warmed a little. */
    static final double NOT_PRESSURISED_PA = 1.1 * Water.P_ATM;

    /**
     * Whether n holds what a fresh tank does, so it drops as a plain, stackable tank (Baughn 2026-11-25: a tank holding
     * nothing but air and water vapour). Our reading of that: no liquid and no species but water and air, each under half
     * a mB, and its gas not over 1.1 atm, so a hot pressurised gas, steam included, is kept.
     */
    public static boolean empty(FluidNode n) {
        // (a species under half a mB, as liquid, counts as none either: a tank drained of oil keeps a trace, and Jade lists none)
        if (n.extra != null) {
            SpeciesRegistry reg = SpeciesRegistry.get();
            for (int i = 0; i < n.extra.length; i++) if (n.extra[i] / reg.byId(i + 2).rhoL >= NO_LIQUID_M3) return false;
        }
        return n.liquidVolume() < NO_LIQUID_M3 && n.P() <= NOT_PRESSURISED_PA;
    }

    /** n's contents (n updated), in full precision. */
    public static TankContents of(FluidNode n) {
        n.update();
        Map<String, Double> s = new TreeMap<>();
        if (n.extra != null) {
            SpeciesRegistry reg = SpeciesRegistry.get();
            for (int i = 0; i < n.extra.length; i++) if (n.extra[i] != 0) s.put(reg.byId(i + 2).key, n.extra[i]);
        }
        return new TankContents(n.mWater, n.mAir, n.energy, s, FluidSaves.FORMAT, n.volume, n.T(), n.P(), EaUnits.contents(n), EaUnits.composition(n));
    }

    /** The contents as FluidSaves.array gives them (water, air, energy, then the other species by id), to restore. */
    public double[] saved() {
        CompoundTag t = new CompoundTag();
        if (format > 0) t.putInt("vFv", format);
        t.putDouble("vW", water);
        t.putDouble("vA", air);
        t.putDouble("vU", energy);
        if (!species.isEmpty()) {
            CompoundTag s = new CompoundTag();
            species.forEach(s::putDouble);
            t.put("vS", s);
        }
        return FluidSaves.array(t, "v");
    }

    /** Its tooltip lines, as Jade reads a placed tank's (Pressure, Contents). */
    public java.util.List<String> lines() {
        return java.util.List.of(
            String.format("%s: %.2f bar of %s, %s", mods.eln.i18n.I18N.tr("Pressure"), pascal / 1e5, contents, mods.eln.api.Meters.celsius(kelvin - Water.T0)),
            mods.eln.i18n.I18N.tr("Contents") + ": " + composition);
    }
}
