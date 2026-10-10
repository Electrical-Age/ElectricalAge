package mods.eln.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.12 sound events (WP13); on 26.x registered through a DeferredRegister. 1.7.10 played sounds by name ("eln:Motor", "random.click"); 1.12 needs registered
 * SoundEvents. Every event in assets/eln/sounds.json is registered as eln:&lt;key&gt; (both sides; the registry is
 * synced) and EA's track names are resolved here, in one place:
 * <ul>
 * <li>EA tracks: "eln:Motor" -&gt; eln:motor (asset names are lower case, Identifier lower-cases anyway);</li>
 * <li>1.7.10 vanilla names that EA used -&gt; their 1.12 events ({@link #LEGACY}).</li>
 * </ul>
 * The category comes from the event's "category" in sounds.json (1.7.10 file; 1.12 ignores it there), default BLOCKS.
 */
public final class ElnSounds {
    public static final ElnSounds EVENTS = new ElnSounds();

    /** 1.7.10 vanilla sound names used by EA -> 1.12 sound event. */
    private static final Map<String, String> LEGACY = new HashMap<>();

    static {
        LEGACY.put("random.click", "minecraft:block.lever.click"); // 1.7.10 random.click = random/click.ogg
    }

    private static final Map<Identifier, SoundSource> categories = new LinkedHashMap<>();

    private ElnSounds() {
    }

    static {
        try (InputStream in = ElnSounds.class.getResourceAsStream("/assets/eln/sounds.json")) {
            if (in != null) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    JsonElement cat = e.getValue().getAsJsonObject().get("category");
                    SoundSource category = cat == null ? null : byName(cat.getAsString());
                    categories.put(Identifier.fromNamespaceAndPath("eln", e.getKey()), category != null ? category : SoundSource.BLOCKS);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Electrical Age: cannot read assets/eln/sounds.json", e);
        }
    }

    /** The registered EA sound event names (from sounds.json). */
    public static Iterable<Identifier> names() {
        return Collections.unmodifiableSet(categories.keySet());
    }

    /** Register every EA sound event on the mod bus (26.x: a DeferredRegister instead of RegistryEvent.Register). */
    public static void register(IEventBus modBus) {
        DeferredRegister<SoundEvent> reg = DeferredRegister.create(Registries.SOUND_EVENT, "eln");
        for (Identifier name : categories.keySet())
            reg.register(name.getPath(), () -> SoundEvent.createVariableRangeEvent(name));
        reg.register(modBus);
    }

    /** EA track name (1.7.10 style) -> sound event location. */
    public static Identifier location(String track) {
        String legacy = LEGACY.get(track);
        return Identifier.parse(legacy != null ? legacy : track.toLowerCase(Locale.ROOT));
    }

    /** The registered event for a track, or null (unknown name). */
    public static SoundEvent event(String track) {
        return BuiltInRegistries.SOUND_EVENT.getValue(location(track));
    }

    public static SoundSource category(String track) {
        SoundSource c = categories.get(location(track));
        return c != null ? c : SoundSource.BLOCKS;
    }

    private static SoundSource byName(String name) {
        for (SoundSource s : SoundSource.values()) if (s.getName().equals(name)) return s;
        return null;
    }
}
