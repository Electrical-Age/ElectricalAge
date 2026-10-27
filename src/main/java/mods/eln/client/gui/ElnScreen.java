package mods.eln.client.gui;

import mods.eln.gui.ElnNodeMenu;
import mods.eln.gui.SlotSpec;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The base of every EA screen (P4b), in the vanilla 26.x style: a container panel, the device's slots as vanilla slot
 * frames (big ones for 1.12's big skin), labels in the vanilla grey, the player inventory when the menu has one.
 * Subclasses add widgets in {@link #init()} and refresh them from the device's render in {@link #update()} (every
 * client tick). Without a subclass this is the generic screen: slots only.
 */
public class ElnScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<ElnNodeMenu> {
    public static final Identifier PANEL = Identifier.fromNamespaceAndPath("eln", "panel");
    public static final Identifier INSET = Identifier.fromNamespaceAndPath("eln", "inset");
    public static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    public static final int LABEL = 0xFF404040;

    /** The device's laid-out screen (assets/eln/gui), or null: then the screen places things itself. */
    protected final mods.eln.gui.layout.Layout.Result layout;

    public ElnScreen(ElnNodeMenu menu, Inventory inventory, Component title) {
        this(menu, inventory, title, widthFor(menu), sizeFor(menu));
    }

    static int widthFor(ElnNodeMenu menu) {
        return menu.spec.width();
    }

    static int sizeFor(ElnNodeMenu menu) {
        var l = ElnLayouts.layout(ElnScreens.descriptor(menu));
        if (l != null) return menu.spec.height();
        return menu.spec.playerInventory() ? Math.max(menu.spec.height(), menu.spec.inventoryY() + 82) : 40;
    }

    public ElnScreen(ElnNodeMenu menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
        this.layout = ElnLayouts.layout(ElnScreens.descriptor(menu));
        this.inventoryLabelY = menu.spec.inventoryY() - 11;
        this.inventoryLabelX = menu.spec.inventoryX();
    }

    // ---- builders by layout id (the layout file says where; the screen says what)

    private final java.util.List<Runnable> drawers = new java.util.ArrayList<>();
    private GuiGraphicsExtractor drawing;

    protected mods.eln.gui.layout.Layout.Rect rect(String kind, String id) {
        var r = layout == null ? null : layout.get(kind, id);
        if (r == null) throw new IllegalStateException("screen layout has no " + kind + " '" + id + "'");
        return r;
    }

    protected net.minecraft.client.gui.components.Button button(String id, java.util.function.Supplier<String> label, Runnable press,
                                                                String... tooltip) {
        var r = rect("button", id);
        return button(r.x(), r.y(), r.w(), label, press, tooltip);
    }

    protected EaField field(String id, java.util.function.Supplier<String> value, java.util.function.Consumer<String> commit, String... tooltip) {
        var r = rect("field", id);
        return field(r.x(), r.y(), r.w(), value, commit, tooltip);
    }

    protected EaSlider slider(String id, double min, double max, double step, double initial,
                              java.util.function.DoubleFunction<Component> label, java.util.function.DoubleConsumer commit) {
        var r = rect("slider", id);
        // a text line of the same id above the slider takes the label (the knob would cover it)
        var t = layout.get("text", id);
        EaSlider s = addRenderableWidget(new EaSlider(leftPos + r.x(), topPos + r.y(), r.w(), min, max, step, initial,
            t == null ? label : v -> Component.empty(), commit));
        if (t != null) drawers.add(() -> drawing.text(font, label.apply(s.get()), leftPos + t.x(), topPos + t.y(), LABEL, false));
        return s;
    }

    protected EaGauge gauge(String id, java.util.function.DoubleSupplier fraction, java.util.function.Supplier<Component> text,
                            double amberFrom, double redFrom) {
        var r = rect("gauge", id);
        return addRenderableWidget(new EaGauge(leftPos + r.x(), topPos + r.y(), r.w(), r.h(), fraction, text, amberFrom, redFrom));
    }

    /** An area drawn by the screen (screen coordinates passed to the painter). */
    protected void canvas(String id, java.util.function.BiConsumer<GuiGraphicsExtractor, mods.eln.gui.layout.Layout.Rect> painter) {
        var r = rect("canvas", id);
        var abs = new mods.eln.gui.layout.Layout.Rect(leftPos + r.x(), topPos + r.y(), r.w(), r.h());
        drawers.add(() -> painter.accept(drawing, abs));
    }

    /** A live text line at its layout place. */
    protected void text(String id, java.util.function.Supplier<String> s) {
        text(id, s, () -> LABEL);
    }

    protected void text(String id, java.util.function.Supplier<String> s, java.util.function.IntSupplier color) {
        var r = rect("text", id);
        drawers.add(() -> drawing.text(font, s.get(), leftPos + r.x(), topPos + r.y(), color.getAsInt(), false));
    }

    static final net.minecraft.resources.Identifier ARROW = net.minecraft.resources.Identifier.withDefaultNamespace("container/furnace/burn_progress");

    /** The furnace arrow, filled to the fraction. */
    protected void progress(String id, java.util.function.DoubleSupplier f) {
        var r = rect("progress", id);
        drawers.add(() -> {
            int ax = leftPos + r.x(), ay = topPos + r.y();
            drawing.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, ax, ay, 24, 16, 0.25f);
            int w = (int) Math.ceil(24 * Math.max(0, Math.min(1, f.getAsDouble())));
            if (w > 0) drawing.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, ax, ay, w, 16);
        });
    }

    /** The item in a device slot (its index in the device inventory), or EMPTY. */
    protected net.minecraft.world.item.ItemStack deviceItem(int index) {
        for (Slot s : menu.slots)
            if (s instanceof mods.eln.gui.ElnSlot && s.getContainerSlot() == index) return s.getItem();
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** Refresh labels and values from the device (client tick). */
    protected void update() {
    }

    private final java.util.List<Runnable> refreshers = new java.util.ArrayList<>();

    @Override
    protected void containerTick() {
        super.containerTick();
        for (Runnable r : refreshers) r.run();
        update();
    }

    @Override
    protected void init() {
        refreshers.clear();
        drawers.clear();
        super.init();
    }

    // ---- small builders for the common screens (positions relative to the panel)

    /** A button whose label follows the device (refreshed every tick). */
    protected net.minecraft.client.gui.components.Button button(int x, int y, int w, java.util.function.Supplier<String> label,
                                                                Runnable press, String... tooltip) {
        var b = net.minecraft.client.gui.components.Button.builder(Component.literal(label.get()), btn -> press.run())
            .bounds(leftPos + x, topPos + y, w, 20).build();
        if (tooltip.length > 0) b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(String.join("\n", tooltip))));
        addRenderableWidget(b);
        refreshers.add(() -> b.setMessage(Component.literal(label.get())));
        return b;
    }

    /** A text box committing on Enter / focus loss, following the device while not edited. */
    protected EaField field(int x, int y, int w, java.util.function.Supplier<String> value, java.util.function.Consumer<String> commit,
                            String... tooltip) {
        EaField f = new EaField(leftPos + x, topPos + y, w, value.get(), commit);
        if (tooltip.length > 0) f.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(String.join("\n", tooltip))));
        addRenderableWidget(f);
        refreshers.add(() -> f.setFromDevice(value.get()));
        return f;
    }

    /** A text line (panel coordinates) re-read every frame. */
    protected void label(GuiGraphicsExtractor g, int x, int y, String text) {
        g.text(font, text, leftPos + x, topPos + y, LABEL, false);
    }

    /** Number formatting for fields: no trailing zeros. */
    protected static String num(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.3f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, leftPos, topPos, imageWidth, imageHeight);
        for (Slot s : menu.slots) {
            if (s.x < -100 || s.y < -100) continue; // 1.12 hid some slots off-screen (thermal isolator)
            boolean big = s instanceof mods.eln.gui.ElnSlot es && SlotSpec.BIG.equals(es.spec.skin());
            int frame = s instanceof mods.eln.gui.ElnSlot es2 ? slotFrame(es2.getContainerSlot()) : 0;
            if (frame != 0) g.fill(leftPos + s.x - 3, topPos + s.y - 3, leftPos + s.x + 19, topPos + s.y + 19, frame);
            if (big) g.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, leftPos + s.x - 5, topPos + s.y - 5, 26, 26);
            else g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, leftPos + s.x - 1, topPos + s.y - 1, 18, 18);
        }
        drawing = g;
        for (Runnable d : drawers) d.run();
        extractDevice(g, mouseX, mouseY, a);
    }

    /** A coloured frame round a device slot (its index in the device inventory), ARGB; 0 for none. */
    protected int slotFrame(int index) {
        return 0;
    }

    /** Device graphics under the widgets (progress arrows, gauges' backgrounds). Screen coordinates. */
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        if (layout != null) {
            var t = layout.get("text", "title");
            if (t != null) g.text(font, title, t.x(), t.y(), LABEL, false);
            var i = layout.get("text", "inventory");
            if (i != null) g.text(font, playerInventoryTitle, i.x(), i.y(), LABEL, false);
            return;
        }
        // 1.12's small panels (inventory near the top) had no labels: there is no room for them
        if (menu.spec.playerInventory() && menu.spec.inventoryY() < 60) return;
        g.text(font, title, titleLabelX, titleLabelY, LABEL, false);
        if (menu.spec.playerInventory()) g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL, false);
    }

    /** An empty device slot says what goes in it (1.12's slot comments). */
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (hoveredSlot instanceof mods.eln.gui.ElnSlot es && !es.hasItem() && menu.getCarried().isEmpty()
            && es.spec.comment() != null && es.spec.comment().length > 0) {
            java.util.List<Component> lines = new java.util.ArrayList<>();
            for (String c : es.spec.comment()) lines.add(Component.literal(c));
            g.setTooltipForNextFrame(font, lines, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    /** Typing in a text box must not close the screen (the inventory key) or move hotbar items. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (getFocused() instanceof EditBox box && box.isFocused() && event.key() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)
            return box.keyPressed(event) || box.canConsumeInput();
        return super.keyPressed(event);
    }
}
