package mods.eln.transparentnode.groundradar;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The Ground Radar's screen (layout gui/ground_radar.json): the picture as a slice seen from above at the chosen depth
 * (click the side view, or the arrows, to choose it) and a side view down through the radar, its cut along the row
 * clicked in the top view; every layer of the side view is drawn the same height (deep layers are coarser, so this
 * reads as depth on a stretched scale; the depth label says where). The batteries, the Scan button, the state.
 */
public class GroundRadarScreen extends ElnScreen {
    final GroundRadarRender render;
    int layer, row;
    private Rect top, side;

    record Rect(int x, int y, int w, int h) {
        boolean has(double px, double py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }
    }

    public GroundRadarScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (GroundRadarRender) render;
        layer = this.render.viewLayer;   // (where the player left it)
        row = this.render.viewRow;
    }

    @Override
    protected void init() {
        super.init();
        canvas("top", (g, r) -> {
            top = new Rect(r.x(), r.y(), r.w(), r.h());
            drawTop(g, r.x(), r.y(), r.w(), r.h());
        });
        canvas("side", (g, r) -> {
            side = new Rect(r.x(), r.y(), r.w(), r.h());
            drawSide(g, r.x(), r.y(), r.w(), r.h());
        });
        button("up", () -> "▲", () -> layer = Math.max(0, layer - 1), tr("Shallower"));
        button("down", () -> "▼", () -> layer = Math.min(layers() - 1, layer + 1), tr("Deeper"));
        button("scan", () -> tr("Scan"), render::scan, tr("Scan the ground under the radar (%1$W for at least %2$ s)",
            (int) GroundRadarDescriptor.SCAN_W, (int) GroundRadarDescriptor.SCAN_S));
        text("depth", this::depthText);
        text("status", this::statusText);
    }

    @Override
    public void removed() {
        render.viewLayer = layer;
        render.viewRow = row;
        super.removed();
    }

    @Override
    protected boolean fitsGuiScale() {
        return true;
    }

    int layers() {
        return render.picture == null ? 1 : render.picture.layers.length;
    }

    String depthText() {
        GroundScan p = render.picture;
        if (p == null) return tr("No picture yet");
        layer = Math.max(0, Math.min(p.layers.length - 1, layer));
        GroundScan.Layer l = p.layers[layer];
        int y = p.cy - 1 - l.depth();
        return tr("%1$ m down (Y %2$), %3$ m across", l.depth() + 1, y, l.cell() * GroundScan.N);
    }

    String statusText() {
        if (render.progress >= 0) return tr("Scanning... %1$", mods.eln.misc.Utils.plotPercent(render.progress));
        if (!render.fault.isEmpty()) return render.fault;
        if (render.battery < 0) return tr("No batteries");
        String b = tr("Batteries %1$", mods.eln.misc.Utils.plotPercent(render.battery));
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (render.picture == null || level == null) return b;
        long ago = Math.max(0, level.getGameTime() - render.pictureAt) / 20;
        return b + ", " + tr("scanned %1$ ago", GroundRadarDescriptor.ago(ago));
    }

    private void drawTop(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF373737);
        GroundScan p = render.picture;
        if (p == null) {
            g.fill(x, y, x + w, y + h, 0xFF101014);
            return;
        }
        int k = Math.max(0, Math.min(p.layers.length - 1, layer));
        int cs = Math.max(1, Math.min(w, h) / GroundScan.N);
        for (int i = 0; i < GroundScan.N; i++)
            for (int j = 0; j < GroundScan.N; j++)
                g.fill(x + i * cs, y + j * cs, x + (i + 1) * cs, y + (j + 1) * cs, GroundScan.colour(p, k, i, j));
        // the side view's cut, and the radar over the centre
        g.fill(x, y + row * cs + cs / 2, x + GroundScan.N * cs, y + row * cs + cs / 2 + 1, 0x80FFFFFF);
        int c = GroundScan.N / 2 * cs;
        g.fill(x + c - 1, y + c - 1, x + c + 1, y + c + 1, 0xFFFFFFFF);
        g.text(font, "N", x + c - 2, y + 1, 0xFFFFFFFF, true);
    }

    private void drawSide(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF373737);
        GroundScan p = render.picture;
        if (p == null) {
            g.fill(x, y, x + w, y + h, 0xFF101014);
            return;
        }
        int n = p.layers.length;
        for (int k = 0; k < n; k++) {
            int y0 = y + k * h / n, y1 = y + (k + 1) * h / n;
            for (int i = 0; i < GroundScan.N; i++)
                g.fill(x + i * w / GroundScan.N, y0, x + (i + 1) * w / GroundScan.N, y1, GroundScan.colour(p, k, i, row));
        }
        int k = Math.max(0, Math.min(n - 1, layer));
        int ly = y + k * h / n;
        g.fill(x - 3, ly, x, y + (k + 1) * h / n, 0xFFFFFFFF);
        g.fill(x + w, ly, x + w + 3, y + (k + 1) * h / n, 0xFFFFFFFF);
        g.text(font, "W", x + 1, y + 1, 0xFFFFFFFF, true);
        g.text(font, "E", x + w - 6, y + 1, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        GroundScan p = render.picture;
        if (p != null && top != null && top.has(e.x(), e.y())) {
            int cs = Math.max(1, Math.min(top.w, top.h) / GroundScan.N);
            row = Math.max(0, Math.min(GroundScan.N - 1, (int) ((e.y() - top.y) / cs)));
            return true;
        }
        if (p != null && side != null && side.has(e.x(), e.y())) {
            layer = Math.max(0, Math.min(p.layers.length - 1, (int) ((e.y() - side.y) * p.layers.length / side.h)));
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (render.picture != null && (top != null && top.has(mx, my) || side != null && side.has(mx, my))) {
            layer = Math.max(0, Math.min(layers() - 1, layer - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

}
