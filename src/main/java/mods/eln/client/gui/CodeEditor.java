package mods.eln.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import mods.eln.gui.code.CodeBuffer;
import mods.eln.gui.code.LanguageAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The code editor (Baughn 2026-10-30, board #45; the signal processor's first, a RISC-V assembler and a Lisp later): a
 * CodeBuffer drawn in fixed character cells (the game's font, each glyph centred in a cell: a monospace grid that scales
 * with the GUI scale), a line-number gutter, colours from the language's tokens, a rule above the lines it marks,
 * problems underlined and marked in the gutter with their message on hover. Keys: arrows (ctrl: by word, shift: select),
 * Home, End, Page Up/Down, Backspace/Delete (ctrl: a word), Enter keeps the indent, Tab indents, ctrl-A/C/X/V,
 * ctrl-Z/Y (ctrl-shift-Z). Mouse: click to place, drag to select, double-click a word, the wheel scrolls.
 */
public class CodeEditor extends AbstractWidget {
    public static final int CELL = 6, LINE = 10, PAD = 3;
    static final int BACKGROUND = 0xFF1C1D22, GUTTER = 0xFF26272E, GUTTER_TEXT = 0xFF70727C, CURRENT_LINE = 0xFF24262E, SELECTION = 0xFF3A4A6E,
        CURSOR = 0xFFE8E8E8, RULE = 0xFF6A6C78, ERROR = 0xFFE04848;
    /** The colours of the token kinds (A red, B blue, C green: Baughn 2026-10-30). */
    public static final Map<LanguageAdapter.Kind, Integer> COLOURS = new EnumMap<>(LanguageAdapter.Kind.class);

    static {
        COLOURS.put(LanguageAdapter.Kind.TEXT, 0xFFE6E6E6);
        COLOURS.put(LanguageAdapter.Kind.COMMENT, 0xFF7C8C7C);
        COLOURS.put(LanguageAdapter.Kind.NUMBER, 0xFFD8B880);
        COLOURS.put(LanguageAdapter.Kind.OPERATOR, 0xFFA8A8B0);
        COLOURS.put(LanguageAdapter.Kind.FUNCTION, 0xFFE8D870);
        COLOURS.put(LanguageAdapter.Kind.NAME, 0xFFC890E0);
        COLOURS.put(LanguageAdapter.Kind.DEFINITION, 0xFFE4B0FF);
        COLOURS.put(LanguageAdapter.Kind.INPUT_A, 0xFFFF5A5A);
        COLOURS.put(LanguageAdapter.Kind.INPUT_B, 0xFF5A9CFF);
        COLOURS.put(LanguageAdapter.Kind.INPUT_C, 0xFF5AD85A);
        COLOURS.put(LanguageAdapter.Kind.KEYWORD, 0xFFF0A050);
        COLOURS.put(LanguageAdapter.Kind.ERROR, ERROR);
    }

    public final CodeBuffer buffer;
    final LanguageAdapter language;
    final Consumer<String> changed;
    int scrollLine, scrollCol;
    String analysed;
    List<List<LanguageAdapter.Token>> tokens = List.of();
    List<LanguageAdapter.Diagnostic> problems = List.of();
    Set<Integer> rules = Set.of();
    long lastKey;

    public CodeEditor(int x, int y, int w, int h, String text, int maxChars, LanguageAdapter language, Consumer<String> changed) {
        super(x, y, w, h, Component.literal("code"));
        this.buffer = new CodeBuffer(text, maxChars);
        this.language = language;
        this.changed = changed;
        analyse();
    }

    /** Re-colour and re-check after a change (and tell the screen). */
    void analyse() {
        String t = buffer.text();
        if (t.equals(analysed)) return;
        analysed = t;
        tokens = language.tokenize(t);
        problems = language.validate(t);
        rules = language.rulesAbove(t);
        changed.accept(t);
    }

    public void setText(String text) {
        buffer.setText(text);
        scrollLine = scrollCol = 0;
        analyse();
    }

    public List<LanguageAdapter.Diagnostic> problems() {
        return problems;
    }

    int gutter() {
        return CELL * Math.max(2, String.valueOf(buffer.lineCount()).length()) + 2 * PAD;
    }

    int rows() {
        return (height - 2 * PAD) / LINE;
    }

    int cols() {
        return (width - gutter() - 2 * PAD) / CELL;
    }

    public int textX() {
        return getX() + gutter() + PAD;
    }

    int textY() {
        return getY() + PAD;
    }

    /** Keep the cursor in view. */
    void follow() {
        int l = buffer.cursorLine(), c = buffer.cursorCol();
        if (l < scrollLine) scrollLine = l;
        if (l >= scrollLine + rows()) scrollLine = l - rows() + 1;
        if (c < scrollCol) scrollCol = Math.max(0, c - 4);
        if (c >= scrollCol + cols()) scrollCol = c - cols() + 4;
        lastKey = Util.getMillis();
    }

    // ---- drawing

    /** Typed text only arrives while some widget has claimed text input (SDL's), as EditBox does. */
    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        Minecraft.getInstance().onTextInputFocusChange(this, focused);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Font font = Minecraft.getInstance().font;
        int x0 = getX(), y0 = getY(), gw = gutter();
        g.fill(x0, y0, x0 + width, y0 + height, BACKGROUND);
        g.fill(x0, y0, x0 + gw, y0 + height, GUTTER);
        if (isFocused()) g.outline(x0, y0, width, height, 0xFF5A6080);
        g.enableScissor(x0, y0, x0 + width, y0 + height);
        int tx = textX(), ty = textY();
        int[] sel = buffer.selection();
        for (int r = 0; r < rows() + 1; r++) {
            int l = scrollLine + r;
            if (l >= buffer.lineCount()) break;
            int y = ty + r * LINE;
            if (l == buffer.cursorLine() && isFocused()) g.fill(x0 + gw, y - 1, x0 + width, y + LINE - 1, CURRENT_LINE);
            if (rules.contains(l)) g.fill(x0 + gw, y - 1, x0 + width, y, RULE);
            // selection
            if (sel != null && l >= sel[0] && l <= sel[2]) {
                int c0 = l == sel[0] ? sel[1] : 0, c1 = l == sel[2] ? sel[3] : buffer.line(l).length() + 1;
                int sx0 = tx + (c0 - scrollCol) * CELL, sx1 = tx + (c1 - scrollCol) * CELL;
                g.fill(Math.max(tx, sx0), y - 1, Math.max(tx, sx1), y + LINE - 1, SELECTION);
            }
            // line number
            String n = String.valueOf(l + 1);
            boolean bad = false;
            for (var p : problems) bad |= p.line() == l;
            g.text(font, n, x0 + gw - PAD - font.width(n), y, bad ? ERROR : GUTTER_TEXT, false);
            if (bad) g.fill(x0 + 1, y + 2, x0 + 3, y + 6, ERROR);
            // text
            String s = buffer.line(l);
            List<LanguageAdapter.Token> ts = l < tokens.size() ? tokens.get(l) : List.of();
            int ti = 0;
            for (int c = scrollCol; c < Math.min(s.length(), scrollCol + cols() + 1); c++) {
                while (ti < ts.size() && ts.get(ti).end() <= c) ti++;
                LanguageAdapter.Kind k = ti < ts.size() && ts.get(ti).start() <= c ? ts.get(ti).kind() : LanguageAdapter.Kind.TEXT;
                String ch = String.valueOf(s.charAt(c));
                if (ch.equals(" ")) continue;
                int cx = tx + (c - scrollCol) * CELL + (CELL - font.width(ch)) / 2;
                g.text(font, ch, cx, y, COLOURS.getOrDefault(k, 0xFFE6E6E6), false);
            }
            // problems: a dotted red underline
            for (var p : problems)
                if (p.line() == l) {
                    int c0 = p.start(), c1 = Math.max(p.end(), p.start() + 1);
                    for (int c = c0; c < c1; c++) {
                        int ux = tx + (c - scrollCol) * CELL;
                        if (ux < tx) continue;
                        g.fill(ux, y + 8, ux + 2, y + 9, ERROR);
                        g.fill(ux + 3, y + 8, ux + 5, y + 9, ERROR);
                    }
                }
        }
        // the cursor, blinking; the input method's window goes by it
        if (isFocused()) {
            int r = buffer.cursorLine() - scrollLine;
            int cx = tx + (buffer.cursorCol() - scrollCol) * CELL;
            if (r >= 0 && r <= rows() && cx >= tx) {
                if ((Util.getMillis() - lastKey) / 500 % 2 == 0) g.fill(cx, ty + r * LINE - 1, cx + 1, ty + r * LINE + LINE - 1, CURSOR);
                Minecraft.getInstance().textInputManager().setTextInputArea(cx, ty + r * LINE, cx + 1, ty + r * LINE + LINE);
            }
        }
        if (isHovered()) g.requestCursor(com.mojang.blaze3d.platform.cursor.CursorTypes.IBEAM);
        g.disableScissor();
        // on hover: a problem's message (its underline or its line number), else what the language says of the word
        if (isMouseOver(mouseX, mouseY) && mouseY >= ty) {
            int l = scrollLine + (mouseY - ty) / LINE;
            int c = mouseX < tx ? -1 : (mouseX - tx) / CELL + scrollCol;
            boolean shown = false;
            for (var p : problems)
                if (p.line() == l && (mouseX < x0 + gw || c >= p.start() && c <= Math.max(p.end(), p.start() + 1))) {
                    g.setTooltipForNextFrame(Component.literal(p.message()), mouseX, mouseY);
                    shown = true;
                }
            if (!shown && c >= 0 && l < buffer.lineCount()) {
                List<String> info = hover(l, c);
                if (!info.isEmpty()) g.setTooltipForNextFrame(font, tooltip(font, info), mouseX, mouseY);
            }
        }
    }

    String hoverText;
    int hoverLine = -1, hoverCol = -1;
    List<String> hoverInfo = List.of();

    /** The language's word on (line, col), kept while neither the text nor the place changes. */
    List<String> hover(int line, int col) {
        String text = buffer.text();
        if (line != hoverLine || col != hoverCol || !text.equals(hoverText)) {
            hoverText = text;
            hoverLine = line;
            hoverCol = col;
            hoverInfo = language.hover(text, line, col);
        }
        return hoverInfo;
    }

    /** Hover lines as a tooltip: headings (the first, and any after an example) gold, examples grey, wrapped. */
    static List<net.minecraft.util.FormattedCharSequence> tooltip(Font font, List<String> info) {
        List<net.minecraft.util.FormattedCharSequence> out = new java.util.ArrayList<>();
        boolean heading = true;
        for (String s : info) {
            boolean example = s.startsWith("e.g. ");
            var style = net.minecraft.network.chat.Style.EMPTY.withColor(heading ? 0xF0C860 : example ? 0xA0A0A0 : 0xFFFFFF);
            out.addAll(font.split(Component.literal(s).withStyle(style), 200));
            heading = example;
        }
        return out;
    }

    // ---- input

    /** The text position under a point. */
    int[] at(double mx, double my) {
        int l = scrollLine + (int) Math.floor((my - textY()) / LINE);
        int c = scrollCol + (int) Math.round((mx - textX()) / CELL);
        l = Math.max(0, Math.min(buffer.lineCount() - 1, l));
        return new int[]{l, Math.max(0, c)};
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int[] p = at(event.x(), event.y());
        if (doubleClick) {
            buffer.moveTo(p[0], p[1], false);
            buffer.left(false, true);
            buffer.right(true, true);
        } else buffer.moveTo(p[0], p[1], event.hasShiftDown());
        lastKey = Util.getMillis();
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        int[] p = at(event.x(), event.y());
        buffer.moveTo(p[0], p[1], true);
        follow();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (!isMouseOver(x, y)) return false;
        if (sx != 0 || Minecraft.getInstance().hasShiftDown()) scrollCol = Math.max(0, scrollCol - (int) Math.signum(sx != 0 ? sx : sy) * 4);
        else scrollLine = Math.max(0, Math.min(Math.max(0, buffer.lineCount() - rows()), scrollLine - (int) Math.signum(sy) * 3));
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused() || !event.isAllowedChatCharacter()) return false;
        buffer.insert(event.codepointAsString());
        edited();
        return true;
    }

    void edited() {
        analyse();
        follow();
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!isFocused()) return false;
        boolean shift = e.hasShiftDown(), ctrl = e.hasControlDownWithQuirk();
        var clip = Minecraft.getInstance().keyboardHandler;
        if (e.isSelectAll()) buffer.selectAll();
        else if (e.isCopy()) clip.setClipboard(buffer.selectedText());
        else if (e.isCut()) {
            clip.setClipboard(buffer.cut());
            edited();
        } else if (e.isPaste()) {
            buffer.insert(clip.getClipboard());
            edited();
        } else if (ctrl && e.key() == InputConstants.KEY_Z && !shift) {
            buffer.undo();
            edited();
        } else if (ctrl && (e.key() == InputConstants.KEY_Y || e.key() == InputConstants.KEY_Z && shift)) {
            buffer.redo();
            edited();
        } else switch (e.key()) {
            case InputConstants.KEY_LEFT -> buffer.left(shift, ctrl);
            case InputConstants.KEY_RIGHT -> buffer.right(shift, ctrl);
            case InputConstants.KEY_UP -> buffer.up(shift);
            case InputConstants.KEY_DOWN -> buffer.down(shift);
            case InputConstants.KEY_HOME -> {
                if (ctrl) buffer.moveTo(0, 0, shift);
                else buffer.home(shift);
            }
            case InputConstants.KEY_END -> {
                if (ctrl) buffer.moveTo(buffer.lineCount() - 1, Integer.MAX_VALUE, shift);
                else buffer.end(shift);
            }
            case InputConstants.KEY_PAGEUP -> {
                for (int i = 0; i < rows(); i++) buffer.up(shift);
            }
            case InputConstants.KEY_PAGEDOWN -> {
                for (int i = 0; i < rows(); i++) buffer.down(shift);
            }
            case InputConstants.KEY_BACKSPACE -> {
                buffer.backspace(ctrl);
                edited();
            }
            case InputConstants.KEY_DELETE -> {
                buffer.delete(ctrl);
                edited();
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                buffer.newline();
                edited();
            }
            case InputConstants.KEY_TAB -> {
                buffer.insert("    ");
                edited();
            }
            default -> {
                return false;
            }
        }
        follow();
        return true;
    }

    /** Insert text at the cursor (the reference's click). */
    public void insert(String s) {
        buffer.insert(s);
        edited();
        setFocused(true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
