package mods.eln.sixnode.electricalmath;

import mods.eln.client.gui.CodeEditor;
import mods.eln.client.gui.ElnScreen;
import mods.eln.client.gui.ReferencePane;
import mods.eln.gui.ElnNodeMenu;
import mods.eln.solver.Program;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The signal processor's screen (Baughn 2026-10-30, board #45): its program in the code editor in the middle, the quick
 * reference beside it on the right as a handbook page, on the left the logger it will carry (a frame for now), the chip
 * slot and Apply. The chips needed or the first problem show live, as the program is typed; Apply sends it. Sized for
 * GUI scale 4 at 1080p (480 x 270).
 */
public class ElectricalMathScreen extends ElnScreen {
    public static final int WIDTH = 466, HEIGHT = 266, INV_X = 132, INV_Y = 188, CHIP_X = 41, CHIP_Y = 202;
    static final int LEFT_X = 6, LEFT_W = 90, EDIT_X = 100, EDIT_W = 226, EDIT_Y = 16, EDIT_H = 148, REF_X = 332, REF_W = 128;
    final ElectricalMathRender render;
    CodeEditor editor;
    /** The guide's open categories, kept over a re-layout (all closed when the screen opens). */
    final java.util.Set<String> openCategories = new java.util.HashSet<>();
    Program draft;
    /** The logger channel whose scale and unit the fields show: A, B, C, out. */
    int selected;
    static final String[] TABS = {"\u00A7cA", "\u00A79B", "\u00A7aC", "\u00A7fout"};

    public ElectricalMathScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title);
        this.render = (ElectricalMathRender) render;
    }

    @Override
    protected void init() {
        super.init();
        String text = editor != null ? editor.buffer.text() : render.expression == null ? "" : render.expression;
        editor = addRenderableWidget(new CodeEditor(leftPos + EDIT_X, topPos + EDIT_Y, EDIT_W, EDIT_H, text, Program.MAX_CHARS, SpLanguage.INSTANCE,
            t -> draft = SpLanguage.compile(t)));
        addRenderableWidget(new ReferencePane(leftPos + REF_X, topPos + EDIT_Y, REF_W, HEIGHT - EDIT_Y - 6, openCategories, s -> editor.insert(s)));
        logControls();
        button(LEFT_X, 224, LEFT_W, () -> tr("Apply"), () -> render.clientSetString(ElectricalMathElement.setExpressionId, editor.buffer.text()),
            tr("Sends the program to the processor"));
        setInitialFocus(editor);
    }

    /** Graph mode, period, and the selected channel's scale and unit (the logger pane, board #45). */
    void logControls() {
        var lc = render.logClient;
        button(LEFT_X, 111, LEFT_W, () -> tr("Graph: %1$", tr(mods.eln.sixnode.electricaldatalogger.LogGraph.MODE_NAMES.get(Math.floorMod(render.log.mode, 3)))),
            () -> lc.mode((render.log.mode + 1) % 3), tr("Lines, stacked areas, or a lane each"));
        field(44, 134, LEFT_W - 38, () -> num(render.log.samplingPeriod), v -> {
            float p = mods.eln.client.gui.EaField.parse(v);
            if (!Float.isNaN(p)) lc.period(Math.max(0.05f, p));
        }, tr("Sampling period"), tr("in seconds, at least 0.05; changing it keeps the history"));
        for (int i = 0; i < 4; i++) {
            int c = i;
            button(LEFT_X + i * 23, 155, 21, () -> (selected == c ? "\u00A7n" : "") + TABS[c], () -> selected = c, tr("Show this channel's scale and unit"));
        }
        field(LEFT_X, 177, 28, () -> num(channel().min), v -> {
            float p = mods.eln.client.gui.EaField.parse(v);
            if (!Float.isNaN(p)) lc.min(selected, p);
        }, tr("Min"), tr("what a zero signal reads as"));
        field(LEFT_X + 30, 177, 28, () -> num(channel().max), v -> {
            float p = mods.eln.client.gui.EaField.parse(v);
            if (!Float.isNaN(p)) lc.max(selected, p);
        }, tr("Max"), tr("what a full signal (50 V) reads as"));
        field(LEFT_X + 60, 177, LEFT_W - 60, () -> channel().unit, v -> lc.unit(selected, v), tr("Unit"), tr("any text, up to 10 characters"));
    }

    mods.eln.sixnode.electricaldatalogger.LogChannel channel() {
        var ch = render.log.channels;
        return ch[Math.min(selected, ch.length - 1)];
    }

    static int colour(int c) {
        return switch (c) {
            case 0 -> CodeEditor.COLOURS.get(mods.eln.gui.code.LanguageAdapter.Kind.INPUT_A);
            case 1 -> CodeEditor.COLOURS.get(mods.eln.gui.code.LanguageAdapter.Kind.INPUT_B);
            case 2 -> CodeEditor.COLOURS.get(mods.eln.gui.code.LanguageAdapter.Kind.INPUT_C);
            default -> 0xFFE8E8E8;
        };
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        render.logClient.watch();
    }

    int chips() {
        var stack = menu.deviceInventory.getItem(ElectricalMathContainer.restoneSlotId);
        return ElectricalMathElement.isChip(stack) ? stack.getCount() : 0;
    }

    /** The status under the editor: the first problem, or the chips needed. */
    String status() {
        String t = editor.buffer.text();
        if (t.isBlank()) return tr("Program required");
        if (!editor.problems().isEmpty()) return editor.problems().get(0).message();
        if (draft == null || !draft.isValid()) return tr("Invalid equation!");
        int need = ElectricalMathElement.chipsFor(draft.getOperatorCount());
        return tr("%1$ Cheap Chip(s) required", need);
    }

    int statusColor() {
        String t = editor.buffer.text();
        if (t.isBlank()) return LABEL;
        if (!editor.problems().isEmpty() || draft == null || !draft.isValid()) return 0xFFC02020;
        return chips() >= ElectricalMathElement.chipsFor(draft.getOperatorCount()) ? 0xFF108F00 : 0xFFC02020;
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        // the logger: a device frame round its graph
        int x = leftPos + LEFT_X, y = topPos + EDIT_Y;
        g.fill(x, y, x + LEFT_W, y + 92, 0xFF4A4E58);
        g.fill(x + 1, y + 1, x + LEFT_W - 1, y + 91, 0xFF6A6F7A);
        mods.eln.sixnode.electricaldatalogger.LogGraph.draw(g, font, x + 3, y + 3, LEFT_W - 6, 86, render.log, ElectricalMathScreen::colour, true);
        g.text(font, tr("Period"), x, topPos + 139, LABEL, false);
        g.text(font, tr("Chips"), leftPos + CHIP_X - 4 - font.width(tr("Chips")), topPos + CHIP_Y + 5, LABEL, false);
        String s = status();
        var lines = font.split(Component.literal(s), EDIT_W);
        g.text(font, lines.isEmpty() ? Component.empty().getVisualOrderText() : lines.get(0), editor.textX(), topPos + EDIT_Y + EDIT_H + 3, statusColor(), false);
        if (lines.size() > 1 && mouseY >= topPos + EDIT_Y + EDIT_H + 2 && mouseY < topPos + EDIT_Y + EDIT_H + 12 && mouseX >= leftPos + EDIT_X
            && mouseX < leftPos + EDIT_X + EDIT_W)
            g.setTooltipForNextFrame(Component.literal(s), mouseX, mouseY);
    }

    /** Typing in the editor goes to the editor (the inventory key and the hotbar numbers don't act). */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (editor != null && editor.isFocused() && event.key() != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            editor.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
