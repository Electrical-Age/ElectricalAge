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
        button("scan", () -> tr("Scan"), render::scan, tr("Start ground scan (cost shown below)"));
        slider("range", 1, GroundRadarDescriptor.MAX_RANGE, 1, render.range,
            v -> Component.literal(tr("Range: %1$ m across", GroundRadarDescriptor.cellsAcross((int) Math.round(v)) * GroundScan.TOP_CELL)),
            v -> render.setRange((int) Math.round(v)));
        slider("detail", 1, GroundRadarDescriptor.MAX_DETAIL, 1, render.detail,
            v -> Component.literal(tr("Detail: %1$x", (int) Math.round(v))), v -> render.setDetail((int) Math.round(v)));
        text("addrl", () -> tr("Bus address"));
        field("addr", () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = mods.eln.client.gui.EaField.parse(v);
            render.clientSendFloat(GroundRadarElement.ADDRESS_ID, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("RS485 address (17–255)"));
        text("busstate", () -> render.address == 0 ? tr("No address (17–255)") : render.onBus ? tr("On a bus") : tr("No bus"),
            () -> render.address != 0 && render.onBus ? OK_TEXT : LABEL);
        if (render.desc.goggles) button("view", () -> tr("Enter view"), this::enterView,
            tr("3D radar view (Esc to exit)"));
        text("depth", this::depthText);
        text("status", this::statusText);
    }

    // ---- the goggles' view (GogglesView): a mode of this screen, so the server's menu stays open

    private GogglesView goggles;

    void enterView() {
        if (goggles != null) return;
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (render.picture == null || render.progress >= 0) {
            if (player != null) player.sendOverlayMessage(Component.literal(render.progress >= 0 ? tr("Wait for the scan to finish") : tr("No scan data yet")));
            return;
        }
        goggles = new GogglesView(render, render.picture);
    }

    void leaveView() {
        if (goggles == null) return;
        goggles.close();
        goggles = null;
        setFocused(null);   // (else Space or Enter on the console's focused Enter view opens it again)
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (goggles != null && goggles.hurt()) leaveView();
        if (goggles != null) {
            goggles.frame(g, width, height);
            return;
        }
        super.extractRenderState(g, mouseX, mouseY, a);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
        if (goggles != null) {
            if (e.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) leaveView();
            else goggles.key(e, true);
            return true;   // (nothing else in the view: no hotbar swaps, no closing on the inventory key)
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent e) {
        if (goggles != null) {
            goggles.key(e, false);
            return true;
        }
        return super.keyReleased(e);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (goggles != null) goggles.mouseMoved(mx, my);
        else super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        return goggles != null || super.mouseReleased(e);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        return goggles != null || super.mouseDragged(e, dx, dy);
    }

    @Override
    public void removed() {
        leaveView();
        render.viewLayer = layer;
        render.viewRow = row;
        for (int v = 0; v < 2; v++) release(v);
        super.removed();
    }

    // ---- the views as textures (a cell a texel), remade when the picture, the depth or the cut changes: a fill() a
    // cell cost 177 ms a frame for a 72-cell picture (AdventurAgent)

    private final mods.eln.client.gui.PixelTexture[] tex = {new mods.eln.client.gui.PixelTexture("ground_radar_top"),
        new mods.eln.client.gui.PixelTexture("ground_radar_side")};
    private final Object[] texFor = new Object[2];
    private final int[] texKey = {-2, -2};

    /** View v's texture (0: from above at layer k; 1: the side view along row r), or null where there is no GPU. */
    private net.minecraft.resources.Identifier texture(int v, GroundScan p, int k, int r) {
        int key = v == 0 ? k : r;
        if (tex[v].id() != null && texFor[v] == p && texKey[v] == key) return tex[v].id();
        var id = tex[v].paint(p.n, v == 0 ? p.n : p.layers.length,
            (x, y) -> v == 0 ? GroundScan.colour(p, k, x, y) : GroundScan.colour(p, y, x, r));
        texFor[v] = p;
        texKey[v] = key;
        return id;
    }

    private void release(int v) {
        tex[v].release();
        texFor[v] = null;
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
        return tr("%1$ m down (Y %2$), %3$ m across, %4$ m cells", l.depth() + 1, y, l.cell() * p.n, l.cell());
    }

    String statusText() {
        if (render.progress >= 0) return tr("Scanning... %1$", mods.eln.misc.Utils.plotPercent(render.progress));
        if (!render.fault.isEmpty()) return render.fault;
        if (render.battery < 0) return tr("No batteries");
        String cost = tr("Scan %1$, ~%2$ s.", mods.eln.api.Meters.joules(render.energy),
            (int) Math.ceil(Math.max(GroundRadarDescriptor.MIN_S, render.rate > 0 ? render.energy / render.rate : 0)));
        String b = cost + " " + tr("Batteries %1$", mods.eln.misc.Utils.plotPercent(render.battery));
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
        int k = Math.max(0, Math.min(p.layers.length - 1, layer)), n = p.n;
        row = Math.max(0, Math.min(n - 1, row < 0 ? n / 2 : row));
        var tex = texture(0, p, k, -1);
        if (tex != null) g.blit(tex, x, y, x + w, y + h, 0, 1, 0, 1);
        // the side view's cut, and the radar over the centre
        int ry = y + row * h / n + h / n / 2;
        g.fill(x, ry, x + w, ry + 1, 0x80FFFFFF);
        int cx = x + w / 2, cy = y + h / 2;
        g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
        g.text(font, "N", cx - 2, y + 1, 0xFFFFFFFF, true);
    }

    private void drawSide(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF373737);
        GroundScan p = render.picture;
        if (p == null) {
            g.fill(x, y, x + w, y + h, 0xFF101014);
            return;
        }
        int n = p.n, L = p.layers.length, r = Math.max(0, Math.min(n - 1, row < 0 ? n / 2 : row));
        var tex = texture(1, p, -1, r);
        if (tex != null) g.blit(tex, x, y, x + w, y + h, 0, 1, 0, 1);
        int k = Math.max(0, Math.min(L - 1, layer));
        int ly = y + k * h / L;
        g.fill(x - 3, ly, x, y + (k + 1) * h / L, 0xFFFFFFFF);
        g.fill(x + w, ly, x + w + 3, y + (k + 1) * h / L, 0xFFFFFFFF);
        g.text(font, "W", x + 1, y + 1, 0xFFFFFFFF, true);
        g.text(font, "E", x + w - 6, y + 1, 0xFFFFFFFF, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (goggles != null) return true;
        GroundScan p = render.picture;
        if (p != null && top != null && top.has(e.x(), e.y())) {
            row = Math.max(0, Math.min(p.n - 1, (int) ((e.y() - top.y) * p.n / top.h)));
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
        if (goggles != null) {
            goggles.scroll(dy);
            return true;
        }
        if (render.picture != null && (top != null && top.has(mx, my) || side != null && side.has(mx, my))) {
            layer = Math.max(0, Math.min(layers() - 1, layer - (int) Math.signum(dy)));
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

}
