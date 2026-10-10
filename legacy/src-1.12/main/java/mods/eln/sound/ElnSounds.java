package mods.eln.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 1.12 sound events (WP13). 1.7.10 played sounds by name ("eln:Motor", "random.click"); 1.12 needs registered
 * SoundEvents. Every event in assets/eln/sounds.json is registered as eln:&lt;key&gt; (both sides; the registry is
 * synced) and EA's track names are resolved here, in one place:
 * <ul>
 * <li>EA tracks: "eln:Motor" -&gt; eln:motor (asset names are lower case, ResourceLocation lower-cases anyway);</li>
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

    private static final Map<ResourceLocation, SoundCategory> categories = new LinkedHashMap<>();

    private ElnSounds() {
    }

    static {
        try (InputStream in = ElnSounds.class.getResourceAsStream("/assets/eln/sounds.json")) {
            if (in != null) {
                JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                    JsonElement cat = e.getValue().getAsJsonObject().get("category");
                    SoundCategory category = cat == null ? null : SoundCategory.getByName(cat.getAsString());
                    categories.put(new ResourceLocation("eln", e.getKey()), category != null ? category : SoundCategory.BLOCKS);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Electrical Age: cannot read assets/eln/sounds.json", e);
        }
    }

    /** The registered EA sound event names (from sounds.json). */
    public static Iterable<ResourceLocation> names() {
        return Collections.unmodifiableSet(categories.keySet());
    }

    @SubscribeEvent
    public void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
        for (ResourceLocation name : categories.keySet())
            event.getRegistry().register(new SoundEvent(name).setRegistryName(name));
    }

    /** EA track name (1.7.10 style) -> sound event location. */
    public static ResourceLocation location(String track) {
        String legacy = LEGACY.get(track);
        return new ResourceLocation(legacy != null ? legacy : track.toLowerCase(Locale.ROOT));
    }

    /** The registered event for a track, or null (unknown name). */
    public static SoundEvent event(String track) {
        return SoundEvent.REGISTRY.getObject(location(track));
    }

    public static SoundCategory category(String track) {
        SoundCategory c = categories.get(location(track));
        return c != null ? c : SoundCategory.BLOCKS;
    }
}
