package mods.eln.transparentnode.groundradar;

import com.mojang.blaze3d.platform.InputConstants;
import mods.eln.client.ClientKeys;
import mods.eln.client.gui.PixelTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

import java.util.concurrent.atomic.AtomicReference;

import static mods.eln.i18n.I18N.tr;

/**
 * The Ground Radar w/Goggles' fly-through view (Baughn 2026-11-24), a mode of its screen: the server's menu stays open,
 * so walking off or the radar breaking ends it, and Esc goes back to the console. A free camera flies through the
 * picture with the player's movement keys (forward, back, left, right, jump up, sneak down, sprint faster; the wheel sets
 * the speed) and the mouse, which is captured while the view is open; the player doesn't move. A key cycles the
 * named bands (RadarBand) and four keys move the band-pass's edges (ClientKeys.RADAR_*); the radar keeps the band,
 * read once when the view opens. RadarView draws on a worker thread into a fixed-size buffer, blurred (the view
 * is meant to be blurry), shown full-screen under a goggle frame and a green dot-matrix HUD.
 */
final class GogglesView {
    /** The picture's width (texels); its height follows the window. */
    static final int W = 192;
    static final double FOV = 70, SPEED_MIN = 2, SPEED_MAX = 64;
    static final int GREEN = 0xFF55FF66, GREEN_DIM = 0x8033CC44;

    private final RadarView view;
    private final GroundScan picture;
    private final GroundRadarRender render;
    double x, y, z, yaw, pitch, speed = 8;
    RadarBand band;
    private boolean fwd, back, left, right, up, down, fast;
    private long last;
    private double mouseX = Double.NaN, mouseY;
    private int h = 108;
    private final PixelTexture tex = new PixelTexture("radar_goggles_view"), mask = new PixelTexture("radar_goggles_mask");
    private int maskW, maskH;

    /** The camera and band a frame is drawn from, and the frame drawn. */
    private record Shot(double x, double y, double z, double yaw, double pitch, RadarBand band, int w, int h) {
    }

    private final AtomicReference<Shot> wanted = new AtomicReference<>();
    private final AtomicReference<int[]> done = new AtomicReference<>();
    private volatile boolean running = true;
    private final Thread worker;

    GogglesView(GroundRadarRender render, GroundScan picture) {
        this.picture = picture;
        this.render = render;
        band = render.band;
        view = new RadarView(picture);
        // (start just behind and above the radar, looking down into the cone)
        x = picture.cx + 0.5;
        z = picture.cz + 0.5 - 12;
        y = picture.cy + 16;
        yaw = 0;
        pitch = 50;
        last = System.nanoTime();
        var mc = Minecraft.getInstance();
        hurt = mc.player == null ? 0 : mc.player.hurtTime;
        InputConstants.grabMouse(mc.getWindow(), mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
        worker = new Thread(this::work, "eln radar goggles");   // (last: nothing after it can throw and leave it running)
        worker.setDaemon(true);
        worker.start();
    }

    /** The worker: the latest wanted shot, drawn and blurred, into done. */
    private void work() {
        int[] a = null, b = null;
        Shot drawn = null;
        while (running) {
            Shot s = wanted.get();
            if (s == null || s.equals(drawn)) {
                try {
                    Thread.sleep(4);
                } catch (InterruptedException e) {
                    return;
                }
                continue;
            }
            if (a == null || a.length != s.w() * s.h()) {
                a = new int[s.w() * s.h()];
                b = new int[s.w() * s.h()];
            }
            view.render(a, s.w(), s.h(), s.x(), s.y(), s.z(), s.yaw(), s.pitch(), FOV, s.band(), 0, 1);
            blur(a, b, s.w(), s.h());
            done.set(b.clone());
            drawn = s;
        }
    }

    /** A 3 x 3 tent blur of src into dst (the view is meant to be blurry; the texture samples nearest). */
    static void blur(int[] src, int[] dst, int w, int h) {
        for (int py = 0; py < h; py++)
            for (int px = 0; px < w; px++) {
                int r = 0, g = 0, bl = 0, wt = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int qx = px + dx, qy = py + dy;
                        if (qx < 0 || qy < 0 || qx >= w || qy >= h) continue;
                        int k = (dx == 0 ? 2 : 1) * (dy == 0 ? 2 : 1), c = src[qy * w + qx];
                        r += k * ((c >> 16) & 255);
                        g += k * ((c >> 8) & 255);
                        bl += k * (c & 255);
                        wt += k;
                    }
                dst[py * w + px] = 0xFF000000 | (r / wt) << 16 | (g / wt) << 8 | (bl / wt);
            }
    }

    void close() {
        running = false;
        worker.interrupt();
        tex.release();
        mask.release();
        var mc = Minecraft.getInstance();
        InputConstants.releaseMouse(mc.getWindow(), mc.getWindow().getScreenWidth() / 2.0, mc.getWindow().getScreenHeight() / 2.0);
    }

    // ---- input

    /** A key; true if the view took it. Esc isn't taken here (the screen leaves the view on it). */
    boolean key(KeyEvent e, boolean down) {
        var o = Minecraft.getInstance().options;
        if (o.keyUp.matches(e)) fwd = down;
        else if (o.keyDown.matches(e)) back = down;
        else if (o.keyLeft.matches(e)) left = down;
        else if (o.keyRight.matches(e)) right = down;
        else if (o.keyJump.matches(e)) up = down;
        else if (o.keyShift.matches(e)) this.down = down;
        else if (o.keySprint.matches(e)) fast = down;
        else if (down && ClientKeys.RADAR_BAND.matches(e)) setBand(band.next());
        else if (down && ClientKeys.RADAR_LOW_DOWN.matches(e)) setBand(band.edges(-1, 0));
        else if (down && ClientKeys.RADAR_LOW_UP.matches(e)) setBand(band.edges(1, 0));
        else if (down && ClientKeys.RADAR_HIGH_DOWN.matches(e)) setBand(band.edges(0, -1));
        else if (down && ClientKeys.RADAR_HIGH_UP.matches(e)) setBand(band.edges(0, 1));
        else return false;
        return true;
    }

    /** Show b, and have the radar keep it. */
    private void setBand(RadarBand b) {
        if (b.equals(band)) return;
        band = b;
        render.setBand(b);
    }

    /** The mouse moved to (mx, my) (the cursor is captured, so it runs on unbounded): look around. */
    void mouseMoved(double mx, double my) {
        if (!Double.isNaN(mouseX)) {
            var mc = Minecraft.getInstance();
            // (in window pixels, as the game's own look: the GUI scale doesn't change the feel)
            double sens = (0.15 + 0.6 * mc.options.sensitivity().get()) * mc.getWindow().getGuiScale();
            yaw += (mx - mouseX) * sens;
            pitch = mods.eln.misc.Utils.limit(pitch + (my - mouseY) * sens * (mc.options.invertMouseY().get() ? -1 : 1), -89, 89);
        }
        mouseX = mx;
        mouseY = my;
    }

    void scroll(double dy) {
        speed = Math.max(SPEED_MIN, Math.min(SPEED_MAX, speed * Math.pow(1.4, Math.max(-3, Math.min(3, dy)))));
    }

    // ---- a frame

    /** The player's hurt time when the view opened (or last looked): a new hit ends the view. */
    private int hurt;

    /** True if the player was just hurt (the plan: flying blind, the view closes when they are). */
    boolean hurt() {
        var p = Minecraft.getInstance().player;
        int now = p == null ? 0 : p.hurtTime;
        boolean hit = now > hurt;
        hurt = now;
        return hit;
    }

    /** Fly by the frame's time, ask the worker for the next frame, and draw the last one with the goggles over it. */
    void frame(GuiGraphicsExtractor g, int sw, int sh) {
        long now = System.nanoTime();
        double dt = Math.min(0.1, (now - last) / 1e9);
        last = now;
        double ya = Math.toRadians(yaw), pa = Math.toRadians(pitch);
        double fx = -Math.sin(ya) * Math.cos(pa), fy = -Math.sin(pa), fz = Math.cos(ya) * Math.cos(pa);
        double rx = -Math.cos(ya), rz = -Math.sin(ya);
        double v = speed * (fast ? 3 : 1) * dt;
        int f = (fwd ? 1 : 0) - (back ? 1 : 0), s = (right ? 1 : 0) - (left ? 1 : 0), u = (up ? 1 : 0) - (down ? 1 : 0);
        x += (fx * f + rx * s) * v;
        y = Math.max(view.bottom() - 16, y + fy * f * v + u * v);   // (not far under the world's floor)
        z += (fz * f + rz * s) * v;
        h = Math.max(36, (int) Math.round(W * (double) sh / sw));
        wanted.set(new Shot(x, y, z, yaw, pitch, band, W, h));
        int[] img = done.getAndSet(null);
        if (img != null && img.length == W * h) tex.upload(W, h, img);
        g.fill(0, 0, sw, sh, 0xFF000000);
        if (tex.id() != null) g.blit(tex.id(), 0, 0, sw, sh, 0, 1, 0, 1);
        goggles(g, sw, sh);
        hud(g, sw, sh);
    }

    /** The goggle frame: two lenses' soft ovals, black outside, faint scanlines inside (made once a window size). */
    private void goggles(GuiGraphicsExtractor g, int sw, int sh) {
        int mw = 256, mh = Math.max(64, (int) Math.round(256.0 * sh / sw));
        if (mask.id() == null || mw != maskW || mh != maskH) {
            maskW = mw;
            maskH = mh;
            mask.paint(mw, mh, (px, py) -> {
                double nx = (px + 0.5) / mw, ny = (py + 0.5) / mh;
                // two overlapping ellipses, one a lens
                double e = Math.min(lens(nx, ny, 0.30), lens(nx, ny, 0.70));
                double a = Math.max(0, Math.min(1, (e - 0.92) / 0.10));   // (soft edge)
                int alpha = (int) Math.round(255 * a);
                if (py % 2 == 0) alpha = Math.max(alpha, 28);   // (scanlines)
                return alpha << 24;
            });
        }
        if (mask.id() != null) g.blit(mask.id(), 0, 0, sw, sh, 0, 1, 0, 1);
    }

    /** A lens's ellipse distance at (nx, ny): 1 on its rim. */
    static double lens(double nx, double ny, double cx) {
        double dx = (nx - cx) / 0.30, dy = (ny - 0.5) / 0.46;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** The HUD: green dot-matrix text, a soft second pass for the glow. */
    private void hud(GuiGraphicsExtractor g, int sw, int sh) {
        var font = Minecraft.getInstance().font;
        double ex = x - (picture.cx + 0.5), ez = z - (picture.cz + 0.5), depth = view.depthAt(y);
        String where = tr("%1$ m %2$, %3$ m %4$", (int) Math.round(Math.abs(ex)), ex >= 0 ? tr("E") : tr("W"),
            (int) Math.round(Math.abs(ez)), ez >= 0 ? tr("S") : tr("N"));
        String down = depth >= 0 ? tr("%1$ m down, Y %2$", (int) Math.round(depth), (int) Math.floor(y)) : tr("%1$ m up, Y %2$", (int) Math.round(-depth), (int) Math.floor(y));
        String band = bandLine(this.band);
        String here = tr("Here: %1$", at());
        String[] lines = {where, down, here, band, tr("Speed %1$ m/s", (int) Math.round(speed))};
        // (in the bottom left corner, outside the lenses: centred, it hid what lies straight under the radar from the
        // start, AdventurAgent 1575661910995763301)
        int ly = sh - 12 * lines.length - 10, wmax = 0;
        for (String l : lines) wmax = Math.max(wmax, font.width(l));
        g.fill(4, ly - 4, 16 + wmax, ly + 12 * lines.length, 0xA0000000);   // (a dark backing: legible over a bright band)
        for (String l : lines) {
            int lx = 10;
            g.text(font, l, lx + 1, ly + 1, GREEN_DIM, false);   // (the glow)
            g.text(font, l, lx, ly, GREEN, false);
            ly += 12;
        }
        String keys = tr("Esc: back   Wheel: speed   %1$: next band   %2$ %3$ / %4$ %5$: edges",
            ClientKeys.RADAR_BAND.getTranslatedKeyMessage().getString(), ClientKeys.RADAR_LOW_DOWN.getTranslatedKeyMessage().getString(), ClientKeys.RADAR_LOW_UP.getTranslatedKeyMessage().getString(),
            ClientKeys.RADAR_HIGH_DOWN.getTranslatedKeyMessage().getString(), ClientKeys.RADAR_HIGH_UP.getTranslatedKeyMessage().getString());
        g.text(font, keys, sw / 2 - font.width(keys) / 2, 8, GREEN_DIM | 0xFF000000, false);
        // the radar's heading: N at the top of a small compass, turning with the view
        double a = Math.toRadians(-yaw + 180);
        int cxp = sw - 40, cyp = 40;
        g.text(font, "N", (int) (cxp + Math.sin(a) * 18) - 2, (int) (cyp - Math.cos(a) * 18) - 4, GREEN, false);
        g.fill(cxp - 1, cyp - 1, cxp + 1, cyp + 1, GREEN);
    }

    /** The band's readout: a named band's name (with its densities, where it goes by them), or a custom band's. */
    static String bandLine(RadarBand b) {
        return switch (b.kind()) {
            case RadarBand.EVERYTHING -> tr("Band: everything");
            case RadarBand.CAVES -> tr("Band: caves (%1$ to %2$ g/cm3)", fmt(b.lo()), fmt(b.hi()));
            case RadarBand.WATER -> tr("Band: water");
            case RadarBand.OIL -> tr("Band: oil");
            case RadarBand.SOIL -> tr("Band: soil (%1$ to %2$ g/cm3)", fmt(b.lo()), fmt(b.hi()));
            case RadarBand.ROCK -> tr("Band: rock (%1$ to %2$ g/cm3)", fmt(b.lo()), fmt(b.hi()));
            case RadarBand.ORES -> tr("Band: ores (%1$ to %2$ g/cm3)", fmt(b.lo()), fmt(b.hi()));
            default -> tr("Band %1$ to %2$ g/cm3: %3$ to %4$", fmt(b.lo()), fmt(b.hi()), material(b.lo()), material(b.hi()));
        };
    }

    /** A density for the band's readout, one decimal (Meters.number puts SI prefixes on small values: "-300m"). */
    static String fmt(double d) {
        return String.format(java.util.Locale.ROOT, "%.1f", d);
    }

    /** A density's everyday name, for the band's readout. */
    static String material(double d) {
        if (d < 0.5) return tr("air");
        if (d < 1.3) return tr("water, oil, ice");
        if (d < 2.2) return tr("soil");
        if (d < 3.6) return tr("rock");
        if (d < 4.6) return tr("copper");
        if (d < 6.0) return tr("iron");
        if (d < 7.3) return tr("gold");
        if (d < 8.0) return tr("lead");
        return tr("uranium");
    }

    /** What is at the camera. */
    String at() {
        int c = view.cellAt(x, y, z);
        if (c < 0) return y > view.top() ? tr("above ground") : tr("outside the scan");
        int k = c / (picture.n * picture.n), i = c / picture.n % picture.n, j = c % picture.n;
        int f = picture.flagsAt(k, i, j);
        if ((f & GroundScan.UNKNOWN) != 0) return tr("not seen");
        if (picture.signalAt(k, i, j) < 0.2) return tr("faded");
        if ((f & GroundScan.OIL) != 0) return tr("oil");
        if ((f & GroundScan.LAVA) != 0) return tr("lava");
        if ((f & GroundScan.LIQUID) != 0) return tr("water");
        if ((f & GroundScan.BUILT) != 0) return tr("a structure");   // (its halls and walls average to nothing in particular)
        return material(picture.densityAt(k, i, j));
    }
}
