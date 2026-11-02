package mods.eln.introspect;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Wiring: the /eln describe command (ops) and the eln:describe payloads. The payloads are optional, so a client or
 * server without them still connects.
 */
public final class Introspect {
    private Introspect() {
    }

    /** Config introspect.remote: whether the server answers eln:describe (ops only even then). */
    public static volatile boolean remote = false;

    /** Client side: where answers go (set by the client setup). */
    public static BiConsumer<Integer, String> clientAnswer = (n, s) -> {
    };

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> e.registrar("1").optional()
            .playToServer(DescribePayload.Request.TYPE, DescribePayload.Request.CODEC, Introspect::onRequest)
            .playToClient(DescribePayload.Response.TYPE, DescribePayload.Response.CODEC,
                (DescribePayload.Response r, IPayloadContext ctx) -> clientAnswer.accept(r.nonce(), DescribePayload.inflate(r.json()))));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> e.getDispatcher().register(
            Commands.literal("eln").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("describe")
                    .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> block(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), false))
                        .then(Commands.literal("json").executes(c -> block(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "pos"), true))))
                    .then(Commands.literal("entity").then(Commands.argument("target", EntityArgument.entity())
                        .executes(c -> entity(c.getSource(), EntityArgument.getEntity(c, "target"), false))
                        .then(Commands.literal("json").executes(c -> entity(c.getSource(), EntityArgument.getEntity(c, "target"), true))))))));
    }

    /** The reports as {"reports":[...]} (server thread). */
    public static JsonObject describeBlock(ServerLevel level, BlockPos pos) {
        return wrap(Describe.at(level, pos));
    }

    static JsonObject wrap(List<Report> reports) {
        JsonObject o = new JsonObject();
        JsonArray a = new JsonArray();
        for (Report r : reports) a.add(r.toJson());
        o.add("reports", a);
        return o;
    }

    private static void onRequest(DescribePayload.Request q, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer p)) return;
        JsonObject answer;
        if (!remote) answer = error("introspect.remote is off on this server");
        else if (!p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) answer = error("ops only");
        else if (q.entityId() >= 0) {
            Entity e = p.level().getEntity(q.entityId());
            answer = e == null ? error("no entity " + q.entityId()) : wrap(List.of(Describe.entity(e)));
        } else if (!p.level().isLoaded(q.pos())) answer = error("not loaded: " + q.pos());
        else answer = describeBlock(p.level(), q.pos());
        PacketDistributor.sendToPlayer(p, new DescribePayload.Response(q.nonce(), DescribePayload.deflate(answer.toString())));
    }

    private static JsonObject error(String why) {
        JsonObject o = new JsonObject();
        o.addProperty("error", why);
        return o;
    }

    private static int block(CommandSourceStack src, BlockPos pos, boolean json) {
        List<Report> reports = Describe.at(src.getLevel(), pos);
        if (reports.isEmpty()) {
            src.sendFailure(Component.literal("no EA device at " + pos.toShortString()));
            return 0;
        }
        send(src, reports, json);
        return reports.size();
    }

    private static int entity(CommandSourceStack src, Entity e, boolean json) {
        send(src, List.of(Describe.entity(e)), json);
        return Command.SINGLE_SUCCESS;
    }

    private static void send(CommandSourceStack src, List<Report> reports, boolean json) {
        if (json) {
            String s = wrap(reports).toString();
            src.sendSuccess(() -> Component.literal(s), false);
            return;
        }
        var pretty = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        for (Report r : reports) {
            String s = pretty.toJson(Text.readable(r));
            src.sendSuccess(() -> Component.literal(s), false);
        }
    }
}
