package mods.eln.introspect.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mods.eln.introspect.SyncDiff;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * For agent tools in the client (called by reflection, so they need no build dependency on EA): JSON strings in and out,
 * client thread. Ask with {@link #request}, poll {@link #answer} on later ticks, read this client's half with
 * {@link #local} / {@link #localEntity}, and compare with {@link #compare}.
 */
public final class AgentHook {
    private AgentHook() {
    }

    /** The version of this interface; it only grows. */
    public static int version() {
        return 1;
    }

    public static int request(int x, int y, int z) {
        return ClientIntrospect.request(new BlockPos(x, y, z), -1);
    }

    public static int requestEntity(int entityId) {
        return ClientIntrospect.request(BlockPos.ZERO, entityId);
    }

    /** The server's JSON for a request, or null while it hasn't come. */
    public static String answer(int nonce) {
        return ClientIntrospect.answer(nonce);
    }

    public static String local(int x, int y, int z) {
        return ClientIntrospect.wrap(ClientIntrospect.local(new BlockPos(x, y, z))).toString();
    }

    public static String localEntity(int entityId) {
        var level = Minecraft.getInstance().level;
        var e = level == null ? null : level.getEntity(entityId);
        return e == null ? "{\"error\":\"no entity " + entityId + " on this client\"}" : ClientIntrospect.wrap(List.of(ClientIntrospect.entity(e))).toString();
    }

    /**
     * Pairs the server's reports with this client's (by node and slot) and lists their sync differences:
     * {"pairs":[{"id":..., "diff":[{key,server,client,why}]}], "unpaired":[ids], "server_only":[ids]} (server_only: reports
     * marked so, which no client has).
     */
    public static String compare(String serverJson, String clientJson) {
        JsonArray s = reports(serverJson), c = reports(clientJson);
        JsonObject out = new JsonObject();
        JsonArray pairs = new JsonArray(), unpaired = new JsonArray(), alone = new JsonArray();
        boolean[] used = new boolean[c.size()];
        for (JsonElement se : s) {
            JsonObject so = se.getAsJsonObject();
            int match = -1;
            for (int i = 0; i < c.size() && match < 0; i++)
                if (!used[i] && key(so).equals(key(c.get(i).getAsJsonObject()))) match = i;
            if (match < 0) {
                JsonObject id = so.getAsJsonObject("id");
                boolean serverOnly = id != null && id.get("server_only") instanceof JsonObject b && b.get("bool").getAsBoolean();
                (serverOnly ? alone : unpaired).add(so.get("id"));
                continue;
            }
            used[match] = true;
            JsonObject p = new JsonObject();
            p.add("id", so.get("id"));
            JsonArray d = new JsonArray();
            for (SyncDiff.Difference x : SyncDiff.of(so, c.get(match).getAsJsonObject())) {
                JsonObject e = new JsonObject();
                e.addProperty("key", x.key());
                e.addProperty("server", x.server());
                e.addProperty("client", x.client());
                e.addProperty("why", x.why());
                d.add(e);
            }
            p.add("diff", d);
            pairs.add(p);
        }
        for (int i = 0; i < c.size(); i++) if (!used[i]) unpaired.add(c.get(i).getAsJsonObject().get("id"));
        out.add("pairs", pairs);
        out.add("unpaired", unpaired);
        out.add("server_only", alone);
        return out.toString();
    }

    private static JsonArray reports(String json) {
        try {
            JsonElement e = JsonParser.parseString(json);
            if (e.isJsonObject() && e.getAsJsonObject().get("reports") instanceof JsonArray a) return a;
        } catch (RuntimeException ignored) {
            // not a report list: nothing to pair
        }
        return new JsonArray();
    }

    /** Node kind, slot and kind name: the same on both halves. */
    private static String key(JsonObject r) {
        JsonObject id = r.getAsJsonObject("id");
        if (id == null) return "";
        return text(id, "node") + "/" + text(id, "slot") + "/" + text(id, "kind") + "/" + text(id, "uuid");
    }

    private static String text(JsonObject id, String k) {
        return id.get(k) instanceof JsonObject o ? String.valueOf(o.entrySet().iterator().next().getValue()) : "";
    }
}
