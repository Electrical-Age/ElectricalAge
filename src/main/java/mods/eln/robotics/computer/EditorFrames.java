package mods.eln.robotics.computer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The editor channel's framing, as the Janet ROM's editor task reads and writes it: "LENGTH\n" then the payload,
 * its fields joined by NUL bytes (op, id, path, revision, body), all UTF-8.
 */
public final class EditorFrames {
    public static final int FIELDS = 5;
    /** The longest file the editor sends or takes, in bytes. */
    public static final int MAX_BODY = 32 * 1024;

    private final java.io.ByteArrayOutputStream pending = new java.io.ByteArrayOutputStream();

    public static byte[] encode(String... fields) {
        byte[] payload = String.join("\0", fields).getBytes(StandardCharsets.UTF_8);
        byte[] head = (payload.length + "\n").getBytes(StandardCharsets.US_ASCII);
        byte[] b = new byte[head.length + payload.length];
        System.arraycopy(head, 0, b, 0, head.length);
        System.arraycopy(payload, 0, b, head.length, payload.length);
        return b;
    }

    /** Feed bytes; returns the whole frames they complete, each as its fields (frames without 5 fields are skipped). */
    public List<String[]> feed(byte[] bytes) {
        pending.writeBytes(bytes);
        List<String[]> out = new ArrayList<>();
        byte[] b = pending.toByteArray();
        int at = 0;
        while (true) {
            int nl = -1;
            for (int i = at; i < b.length && i < at + 12; i++) if (b[i] == '\n') { nl = i; break; }
            if (nl < 0) {
                if (b.length - at >= 12) at = b.length;   // not a length: drop what can't be framed
                break;
            }
            int n;
            try {
                n = Integer.parseInt(new String(b, at, nl - at, StandardCharsets.US_ASCII));
            } catch (NumberFormatException e) {
                at = nl + 1;
                continue;
            }
            if (n < 0 || n > 4 * MAX_BODY) {
                at = nl + 1;
                continue;
            }
            if (b.length < nl + 1 + n) break;
            String[] f = new String(b, nl + 1, n, StandardCharsets.UTF_8).split("\0", FIELDS);
            if (f.length == FIELDS) out.add(f);
            at = nl + 1 + n;
        }
        pending.reset();
        pending.write(b, at, b.length - at);
        return out;
    }
}
