package mods.eln.introspect;

import io.netty.buffer.ByteBuf;
import mods.eln.Eln;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * eln:describe, for tools on a lab client: the client asks for the server half of what is at a block (entityId -1) or of
 * an entity, and the server answers with the reports as deflated JSON. The server answers only an op (game-master
 * level) and only with config introspect.remote on, which is off unless a lab turns it on.
 */
public final class DescribePayload {
    private DescribePayload() {
    }

    /** Client -> server. */
    public record Request(int nonce, BlockPos pos, int entityId) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "describe"));
        public static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Request::nonce, BlockPos.STREAM_CODEC, Request::pos, ByteBufCodecs.VAR_INT, Request::entityId, Request::new);

        @Override
        public Type<Request> type() {
            return TYPE;
        }
    }

    /** Server -> client: the JSON (an object with "reports", or "error"), deflated. */
    public record Response(int nonce, byte[] json) implements CustomPacketPayload {
        public static final Type<Response> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Eln.MODID, "describe_result"));
        public static final StreamCodec<ByteBuf, Response> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Response::nonce, ByteBufCodecs.byteArray(1 << 20), Response::json, Response::new);

        @Override
        public Type<Response> type() {
            return TYPE;
        }
    }

    static byte[] deflate(String s) {
        Deflater d = new Deflater(Deflater.BEST_SPEED);
        d.setInput(s.getBytes(StandardCharsets.UTF_8));
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        return out.toByteArray();
    }

    public static String inflate(byte[] b) {
        Inflater i = new Inflater();
        i.setInput(b);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
            while (!i.finished()) {
                int n = i.inflate(buf);
                if (n == 0 && (i.needsInput() || i.needsDictionary())) break;
                out.write(buf, 0, n);
            }
        } catch (DataFormatException e) {
            return "{\"error\":\"bad reply: " + e.getMessage() + "\"}";
        } finally {
            i.end();
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
