package mods.eln.sixnode.hub;

import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.misc.Direction;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * Hub (P4b; layout gui/hub.json): the 80x80 connection grid between the four cable slots, as in 1.12. Each of the six
 * links (four corners, two straights) is a widget: a corner is clicked anywhere in its quadrant, a straight anywhere on
 * its arm. Connected links are dark, open ones light; the one under the mouse is highlighted.
 * <p>
 * The picture is the hub as the player sees it when the screen opens ({@link HubSlots}; AdventurAgent: on a floor the
 * fixed 1.12 picture was a quarter turn off for a player facing south): each slot sits at the place its edge is seen
 * at, each link is drawn between its two slots, and an empty slot's tooltip names its side of the world.
 */
public class HubScreen extends ElnScreen {
    final HubRender render;

    /** The face the hub is on and the way the player faced when the screen opened: what turns the picture. */
    final Direction side, facing;

    public HubScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (HubRender) render;
        this.side = this.render.side;
        this.facing = Direction.fromEnumFacing(inventory.player.getDirection());
        if (layout == null) return;
        for (HubSlots.Place place : HubSlots.Place.values()) {
            var r = layout.get("slot", String.valueOf(HubSlots.layoutId(place)));
            if (r == null) continue;
            menu.moveDeviceSlot(HubSlots.slot(side, facing, place), r.x() + 1, r.y() + 1,
                tr("Cable kit (1 block)"),
                tr("Connects %1$", tr(HubSlots.worldName(HubSlots.world(side, facing, place)))));
        }
    }

    // 1.12 HubGui geometry, relative to the grid: bars 5 wide, offsets h0..h7 / v0..v7
    static final int W = 5, W2 = (80 - 5 * W) / 2;
    static final int[] P = {0, W2, W2 + W, W2 + 2 * W, W2 + 3 * W, W2 + 4 * W, W2 + 5 * W, 80};

    /** The drawn rectangles of a link of that shape (HubSlots.RIGHT_BOTTOM...; x0, y0, x1, y1 in grid coordinates). */
    static int[][] rects(int shape) {
        return switch (shape) {
            case 0 -> new int[][]{{P[5], P[5], P[6], P[7]}, {P[5], P[5], P[7], P[6]}};
            case 1 -> new int[][]{{P[0], P[1], P[2], P[2]}, {P[1], P[0], P[2], P[2]}};
            case 2 -> new int[][]{{P[0], P[5], P[2], P[6]}, {P[1], P[5], P[2], P[7]}};
            case 3 -> new int[][]{{P[5], P[0], P[6], P[2]}, {P[5], P[1], P[7], P[2]}};
            case 4 -> new int[][]{{P[0], P[3], P[7], P[4]}};
            default -> new int[][]{{P[3], P[0], P[4], P[7]}};
        };
    }

    /** Click area: a corner's quadrant, or a straight's arm band without the centre square. */
    static int[] area(int shape) {
        return switch (shape) {
            case 0 -> new int[]{P[5], P[5], P[7], P[7]};
            case 1 -> new int[]{P[0], P[0], P[2], P[2]};
            case 2 -> new int[]{P[0], P[5], P[2], P[7]};
            case 3 -> new int[]{P[5], P[0], P[7], P[2]};
            case 4 -> new int[]{P[0], P[2], P[7], P[5]};
            default -> new int[]{P[2], P[0], P[5], P[7]};
        };
    }

    /** By shape. */
    static final String[] NAMES = {"right and bottom", "left and top", "left and bottom", "right and top", "left and right",
        "top and bottom"};

    class Link extends AbstractWidget {
        /** The hub's link (what a click toggles), and the shape it is drawn in: between its two slots' places. */
        final int id, shape, gx, gy;

        Link(int id, int shape, int gx, int gy) {
            super(gx + area(shape)[0], gy + area(shape)[1], area(shape)[2] - area(shape)[0], area(shape)[3] - area(shape)[1], Component.empty());
            this.id = id;
            this.shape = shape;
            this.gx = gx;
            this.gy = gy;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(tr("Connects %1$", tr(NAMES[shape])))));
        }

        boolean inCentre(double mx, double my) {
            return mx >= gx + P[2] && mx < gx + P[5] && my >= gy + P[2] && my < gy + P[5];
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            return super.isMouseOver(mx, my) && !inCentre(mx, my);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            render.clientSetByte(HubElement.clientConnectionGridToggle, (byte) id);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
            boolean hover = isMouseOver(mx, my) || isFocused();
            boolean on = render.connectionGrid[id];
            if (hover && id < 4) g.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
            int color = on ? (hover ? 0xFF4A4A4A : 0xFF202020) : (hover ? 0xFFEFEFEF : 0xFFB8B8B8);
            for (int[] q : rects(shape)) g.fill(gx + q[0], gy + q[1], gx + q[2], gy + q[3], color);
            // where the straights cross, a connected one stays on top
            int other = id == 4 ? 5 : id == 5 ? 4 : -1;
            if (other >= 0 && !on && render.connectionGrid[other])
                g.fill(gx + P[3], gy + P[3], gx + P[4], gy + P[4], 0xFF202020);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }

    @Override
    protected void init() {
        super.init();
        var r = rect("canvas", "grid");
        int gx = leftPos + r.x(), gy = topPos + r.y();
        canvas("grid", (g, q) -> g.fill(q.x(), q.y(), q.x() + q.w(), q.y() + q.h(), 0xFF8B8B8B));
        for (int id = 0; id < 6; id++) addRenderableWidget(new Link(id, HubSlots.shape(side, facing, id), gx, gy));
    }
}
