package mods.eln.introspect.client;

import com.google.gson.JsonObject;
import mods.eln.introspect.ClientDescribable;
import mods.eln.introspect.DescribePayload;
import mods.eln.introspect.Introspect;
import mods.eln.introspect.Keys;
import mods.eln.introspect.Report;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client entry points: this client's half of what is at a block or of an entity (no network), and requests for the
 * server half over eln:describe. Client thread only, except {@link #answer} which is thread-safe.
 */
public final class ClientIntrospect {
    private ClientIntrospect() {
    }

    private static final AtomicInteger NONCE = new AtomicInteger();
    private static final Map<Integer, String> ANSWERS = new ConcurrentHashMap<>();

    public static void register() {
        Introspect.clientAnswer = (n, json) -> {
            if (ANSWERS.size() > 64) ANSWERS.clear();
            ANSWERS.put(n, json);
        };
    }

    /** What this client has at pos: the transparent render, or each six-node render (by slot). */
    public static List<Report> local(BlockPos pos) {
        List<Report> out = new ArrayList<>();
        var level = Minecraft.getInstance().level;
        if (level == null) return out;
        var be = level.getBlockEntity(pos);
        if (be instanceof TransparentNodeEntity t && t.elementRender != null) out.add(transparent(t.elementRender, pos));
        if (be instanceof SixNodeEntity s)
            for (SixNodeElementRender r : s.elementRenderList) if (r != null) out.add(six(r, pos));
        return out;
    }

    public static Report entity(Entity e) {
        Report r = new Report(Report.Half.CLIENT);
        r.id().text(Keys.KIND, String.valueOf(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())));
        r.id().text(Keys.NODE, "entity");
        r.id().ref("uuid", e.getStringUUID());
        r.id().ref(Keys.POS, e.getX() + "," + e.getY() + "," + e.getZ());
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        if (e instanceof ClientDescribable d) own(r, d);
        return r;
    }

    static Report transparent(TransparentNodeElementRender e, BlockPos pos) {
        Report r = new Report(Report.Half.CLIENT);
        id(r, e.transparentNodedescriptor == null ? null : e.transparentNodedescriptor.newItemStack(), "transparent", pos);
        if (e.front != null) r.id().text("front", e.front.name().toLowerCase(Locale.ROOT));
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        r.client().bool("burnt", e.burnt);
        r.client().bool("grounded", e.grounded);
        if (e instanceof ClientDescribable d) own(r, d);
        return r;
    }

    static Report six(SixNodeElementRender e, BlockPos pos) {
        Report r = new Report(Report.Half.CLIENT);
        id(r, e.sixNodeDescriptor == null ? null : e.sixNodeDescriptor.newItemStack(), "six", pos);
        if (e.side != null) r.id().text(Keys.FACE, e.side.name().toLowerCase(Locale.ROOT));
        r.id().integer(Keys.SLOT, e.slot);
        r.id().text(Keys.ELEMENT, e.getClass().getSimpleName());
        r.client().bool("burnt", e.burnt);
        if (e instanceof ClientDescribable d) own(r, d);
        return r;
    }

    private static void id(Report r, ItemStack stack, String node, BlockPos pos) {
        r.id().text(Keys.KIND, stack == null || stack.isEmpty() ? "unknown" : String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        r.id().text(Keys.NODE, node);
        r.id().ref(Keys.POS, pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    private static void own(Report r, ClientDescribable d) {
        try {
            d.describeClient(r);
        } catch (RuntimeException ex) {
            r.fault("describe_failed", ex.toString());
        }
    }

    /** Asks the server for its half at pos (entityId -1) or of an entity; returns the nonce to {@link #answer} with. */
    public static int request(BlockPos pos, int entityId) {
        int n = NONCE.incrementAndGet();
        ClientPacketDistributor.sendToServer(new DescribePayload.Request(n, pos, entityId));
        return n;
    }

    /** The server's answer to a request (JSON), or null while it hasn't come. Taken once. */
    public static String answer(int nonce) {
        return ANSWERS.remove(nonce);
    }

    static JsonObject wrap(List<Report> reports) {
        JsonObject o = new JsonObject();
        com.google.gson.JsonArray a = new com.google.gson.JsonArray();
        for (Report r : reports) a.add(r.toJson());
        o.add("reports", a);
        return o;
    }
}
