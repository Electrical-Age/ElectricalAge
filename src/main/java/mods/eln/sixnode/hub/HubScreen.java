package mods.eln.sixnode.hub;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.gui.layout.Layout;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Hub (P4b; layout gui/hub.json): the 80x80 connection grid between the four cable slots, as in 1.12. Each of the
 * six links (four corners, two straights) is clicked to toggle; connected ones are dark, the one under the mouse lit.
 */
public class HubScreen extends ElnScreen {
    final HubRender render;
    Layout.Rect grid;

    public HubScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (HubRender) render;
    }

    // 1.12 HubGui geometry, relative to the grid: bars 5 wide, offsets h0..h7 / v0..v7
    static final int W = 5, W2 = (80 - 5 * W) / 2;
    static final int[] P = {0, W2, W2 + W, W2 + 2 * W, W2 + 3 * W, W2 + 4 * W, W2 + 5 * W, 80};

    /** The link's rectangles (x0, y0, x1, y1 in grid coordinates). */
    static int[][] rects(int id) {
        return switch (id) {
            case 0 -> new int[][]{{P[5], P[5], P[6], P[7]}, {P[5], P[5], P[7], P[6]}};
            case 1 -> new int[][]{{P[0], P[1], P[2], P[2]}, {P[1], P[0], P[2], P[2]}};
            case 2 -> new int[][]{{P[0], P[5], P[2], P[6]}, {P[1], P[5], P[2], P[7]}};
            case 3 -> new int[][]{{P[5], P[0], P[6], P[2]}, {P[5], P[1], P[7], P[2]}};
            case 4 -> new int[][]{{P[0], P[3], P[7], P[4]}};
            default -> new int[][]{{P[3], P[0], P[4], P[7]}};
        };
    }

    int linkAt(double mx, double my) {
        if (grid == null) return -1;
        int x = (int) Math.floor(mx - grid.x()), y = (int) Math.floor(my - grid.y());
        for (int id = 0; id < 6; id++)
            for (int[] r : rects(id)) if (x >= r[0] && x < r[2] && y >= r[1] && y < r[3]) return id;
        return -1;
    }

    int mouseX, mouseY;

    @Override
    protected void init() {
        super.init();
        canvas("grid", (g, r) -> {
            grid = r;
            g.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), 0xFF8B8B8B);
            int hover = linkAt(mouseX, mouseY);
            // unconnected first, so connected bars cover them where they cross
            for (int pass = 0; pass < 2; pass++)
                for (int id = 0; id < 6; id++) {
                    boolean on = render.connectionGrid[id];
                    if (on != (pass == 1)) continue;
                    int color = on ? (id == hover ? 0xFF505050 : 0xFF202020) : (id == hover ? 0xFFE0E0E0 : 0xFFB8B8B8);
                    for (int[] q : rects(id)) g.fill(r.x() + q[0], r.y() + q[1], r.x() + q[2], r.y() + q[3], color);
                }
        });
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float a) {
        mouseX = mx;
        mouseY = my;
        super.extractRenderState(g, mx, my, a);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int id = linkAt(event.x(), event.y());
        if (id >= 0 && event.button() == 0) {
            render.clientSetByte(HubElement.clientConnectionGridToggle, (byte) id);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
