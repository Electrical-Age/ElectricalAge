package mods.eln.sixnode.electricaldatalogger;

import mods.eln.node.six.SixNodeElementRender;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** A logger's packets from its screen to the server ({@link Logger#handle}), behind the device's own header if any. */
public final class LogClient {
    public interface Body {
        void write(DataOutputStream o) throws IOException;
    }

    final SixNodeElementRender render;
    final int prefix;
    final LogWatch watch = new LogWatch();

    public LogClient(SixNodeElementRender render, int prefix) {
        this.render = render;
        this.prefix = prefix;
    }

    void send(byte header, Body body) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bos);
            render.preparePacketForServer(o);
            if (prefix >= 0) o.writeByte(prefix);
            o.writeByte(header);
            body.write(o);
            render.sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Keeps the graph coming: call while it can be seen (asks once a second). */
    public void watch() {
        if (watch.due()) send(Logger.WATCH, o -> {
        });
    }

    public void period(float seconds) {
        send(Logger.PERIOD, o -> o.writeFloat(seconds));
    }

    public void min(int channel, float v) {
        send(Logger.MIN, o -> {
            o.writeByte(channel);
            o.writeFloat(v);
        });
    }

    public void max(int channel, float v) {
        send(Logger.MAX, o -> {
            o.writeByte(channel);
            o.writeFloat(v);
        });
    }

    public void unit(int channel, String unit) {
        send(Logger.UNIT, o -> {
            o.writeByte(channel);
            o.writeUTF(LogChannel.sanitize(unit));
        });
    }

    public void mode(int mode) {
        send(Logger.MODE, o -> o.writeByte(mode));
    }

    public void reset() {
        send(Logger.RESET, o -> {
        });
    }
}
