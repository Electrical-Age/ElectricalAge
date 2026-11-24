package mods.eln.robotics.computer.screen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * The screen as composed cells, for players who only watch it (Baughn 2026-11-24: a computer has one operator; the
 * monitor's onlookers and a second screen get the composed picture, not the scene): each cell's glyph, ink, paper
 * and owner, the pictures over them, and the pixmaps a watcher doesn't have yet (sent once, known by their version).
 * Deflated: a screen of 80 x 25 cells is a few KB.
 */
public final class CellsCodec {
    private CellsCodec() {
    }

    /**
     * Pictures a watcher's client keeps, at most. Both ends forget the one received longest ago, in the order they were
     * sent, so the server's set (insertion-ordered) and the client's map (insertion-ordered) always agree on what the
     * client has: one forgotten is sent again when next shown.
     */
    public static final int CACHE = 300;

    /** Encodes c; pixmaps whose version is in sent are only named (sent, insertion-ordered, gets the new ones). */
    public static byte[] encode(SceneCells c, Set<Integer> sent) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (DataOutputStream o = new DataOutputStream(new DeflaterOutputStream(bos))) {
                o.writeShort(c.w);
                o.writeShort(c.h);
                o.writeUTF(c.pens.name());
                for (int[] a : new int[][]{c.glyph, c.ink, c.paper, c.owner}) for (int v : a) o.writeInt(v);
                o.writeShort(c.deskOwners.size());
                for (int d : c.deskOwners) o.writeInt(d);
                o.writeShort(c.images.size());
                for (SceneCells.Image im : c.images) {
                    o.writeInt(im.owner());
                    for (int v : new int[]{im.x(), im.y(), im.cw(), im.ch(), im.x0(), im.y0(), im.x1(), im.y1()}) o.writeShort(v);
                    Scene.Pixmap p = im.pixmap();
                    o.writeInt(p.version());
                    boolean send = sent.add(p.version());
                    if (send && sent.size() > CACHE) {   // (the watcher's client forgets in the same order: see decode)
                        var it = sent.iterator();
                        it.next();
                        it.remove();
                    }
                    o.writeBoolean(send);
                    if (send) {
                        o.writeShort(p.w());
                        o.writeShort(p.h());
                        for (int v : p.argb()) o.writeInt(v);
                    }
                }
            }
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Decodes cells; pixmaps come from known (by version, insertion-ordered), new ones are added to it. Null if the data is bad. */
    public static SceneCells decode(byte[] data, Map<Integer, Scene.Pixmap> known) {
        try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
            int w = in.readShort(), h = in.readShort();
            if (w < 1 || h < 1 || w > 200 || h > 100) return null;
            SceneCells c = new SceneCells(w, h);
            c.pens = Pens.of(in.readUTF());
            for (int[] a : new int[][]{c.glyph, c.ink, c.paper, c.owner}) for (int i = 0; i < a.length; i++) a[i] = in.readInt();
            c.deskOwners.clear();
            for (int k = in.readShort(); k > 0; k--) c.deskOwners.add(in.readInt());
            for (int k = in.readShort(); k > 0; k--) {
                int owner = in.readInt();
                int[] v = new int[8];
                for (int i = 0; i < 8; i++) v[i] = in.readShort();
                int version = in.readInt();
                Scene.Pixmap p;
                if (in.readBoolean()) {
                    int pw = in.readShort(), ph = in.readShort();
                    if (pw < 1 || ph < 1 || pw > Scene.PIXMAP_W || ph > Scene.PIXMAP_H) return null;
                    int[] argb = new int[pw * ph];
                    for (int i = 0; i < argb.length; i++) argb[i] = in.readInt();
                    known.put(version, p = new Scene.Pixmap(pw, ph, argb, version));
                    if (known.size() > CACHE) known.remove(known.keySet().iterator().next());
                } else p = known.get(version);
                if (p != null) c.images.add(new SceneCells.Image(owner, v[0], v[1], v[2], v[3], p, v[4], v[5], v[6], v[7]));
            }
            return c;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
