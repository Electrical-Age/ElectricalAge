package mods.eln.robotics.computer;

import mods.eln.Eln;
import mods.eln.robotics.computer.screen.ScreenHost;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A screen protocol event from a player to the computer at pos in their dimension, handled on the network thread
 * (Baughn: latency): the computer takes it, runs and answers without waiting for the server thread's next tick, which
 * on a busy server is tens of milliseconds away. Computers list themselves here while loaded.
 */
public record ScreenEventPayload(BlockPos pos, String line) implements CustomPacketPayload {
    public static final Type<ScreenEventPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "screen_event"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScreenEventPayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ScreenEventPayload::pos,
        ByteBufCodecs.stringUtf8(ScreenHost.MAX_SAVE), ScreenEventPayload::line,
        ScreenEventPayload::new);

    @Override
    public Type<ScreenEventPayload> type() {
        return TYPE;
    }

    record Key(ResourceKey<Level> dimension, long pos) {
    }

    static final ConcurrentHashMap<Key, ComputerElement> LOADED = new ConcurrentHashMap<>();

    static void add(Level level, BlockPos pos, ComputerElement e) {
        LOADED.put(new Key(level.dimension(), pos.asLong()), e);
    }

    static void remove(Level level, BlockPos pos, ComputerElement e) {
        LOADED.remove(new Key(level.dimension(), pos.asLong()), e);
    }

    /** On the network thread. */
    public static void handle(ScreenEventPayload p, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) return;
        ComputerElement e = LOADED.get(new Key(player.level().dimension(), p.pos().asLong()));
        if (e != null) e.screenEvent(player, p.line());
    }
}
