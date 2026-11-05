package mods.eln.robotics.computer;

import mods.eln.client.gui.CodeEditor;
import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The computer's screen, two tabs over the same chips and drives. Terminal: the terminal's text (TerminalText, fixed
 * 6 px cells as the code editor's) and an input line that sends what you type with Enter. Files: a path, a code editor
 * and Open / Save / Run / List, which ask the Janet ROM's editor task (it alone knows the floppies' file system); a save
 * over a file changed since it was opened is refused once, so two players don't overwrite each other unawares.
 */
public class ComputerScreen extends ElnScreen {
    public static final int WIDTH = 330, HEIGHT = 348, TERM_X = 5, TERM_Y = 24, ROWS = 15, CELL = 6, LINE = 10;
    public static final int SLOTS_Y = 236, INV_X = 84, INV_Y = 266;   // (348 px high: fits a 720 px window at GUI scale 2)
    static final int TERM_W = TerminalText.COLS * CELL + 8, TERM_H = ROWS * LINE + 6;
    final ComputerRender render;
    EditBox input;
    final List<String> history = new ArrayList<>();
    int historyAt = -1;
    /** Lines scrolled back from the end of the terminal (the wheel). */
    int scrollBack;
    // the Files tab (kept across re-inits: a resize mustn't lose an edit)
    static final JanetLanguage JANET = new JanetLanguage();
    boolean files;
    String path = "a:/main.janet", rev = "", text = "", status = "";
    CodeEditor editor;
    EditBox pathBox;
    final List<AbstractWidget> terminalWidgets = new ArrayList<>(), fileWidgets = new ArrayList<>();

    public ComputerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.render = (ComputerRender) render;
    }

    @Override
    protected void init() {
        if (editor != null) text = editor.buffer.text();
        if (pathBox != null) path = pathBox.getValue();
        terminalWidgets.clear();
        fileWidgets.clear();
        super.init();
        // (beside the title: inventory mods put their sort buttons at the top right)
        button(64, 2, 56, () -> tr("Terminal"), () -> setFiles(false));
        button(122, 2, 50, () -> tr("Files"), () -> setFiles(true),
            tr("Edit the files on the floppies (the computer must be running Janet)."));

        input = new EditBox(font, leftPos + TERM_X, topPos + TERM_Y + TERM_H + 4, TERM_W, 14, Component.literal("input"));
        input.setMaxLength(1000);
        input.setTextColor(0xFFE6E6E6);
        terminalWidgets.add(addRenderableWidget(input));

        pathBox = new EditBox(font, leftPos + TERM_X, topPos + TERM_Y, 146, 18, Component.literal("path"));
        pathBox.setMaxLength(200);
        pathBox.setValue(path);
        fileWidgets.add(addRenderableWidget(pathBox));
        int bx = TERM_X + 150;
        fileWidgets.add(button(bx, TERM_Y - 1, 42, () -> tr("Open"), this::open, tr("Load the file at the path into the editor.")));
        fileWidgets.add(button(bx + 43, TERM_Y - 1, 42, () -> tr("Save"), this::save, tr("Write the editor's text to the path.")));
        fileWidgets.add(button(bx + 86, TERM_Y - 1, 42, () -> tr("Run"), () -> ask("run", ""),
            tr("Run the saved file as a task of its own (its output is on the Terminal tab).")));
        fileWidgets.add(button(bx + 129, TERM_Y - 1, 42, () -> tr("List"), () -> ask("list", ""),
            tr("The files in the path's directory.")));
        editor = new CodeEditor(leftPos + TERM_X, topPos + TERM_Y + 22, TERM_W, TERM_H - 22, text, EditorFrames.MAX_BODY, JANET, t -> {
        });
        fileWidgets.add(addRenderableWidget(editor));

        int by = TERM_Y + TERM_H + 22;
        button(9, by, 60, () -> tr("Reset"), () -> render.clientSendId(ComputerElement.RESET_ID),
            tr("Start again from slot 1's chip (the RAM keeps what it held)."));
        button(9 + 62, by, 50, () -> tr("Break"), () -> render.clientSendId(ComputerElement.BREAK_ID),
            tr("Stop what the REPL is running (a loop that never ends, a long sleep)."));
        field(230, by, 36, () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(ComputerElement.SET_ADDRESS_ID, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its RS485 address, 17 to 255: the source of the packets it sends."));
        setFiles(files);
    }

    void setFiles(boolean f) {
        files = f;
        for (AbstractWidget w : terminalWidgets) w.visible = !f;
        for (AbstractWidget w : fileWidgets) w.visible = f;
        setFocused(f ? editor : input);
        if (f) editor.setFocused(true);
        else input.setFocused(true);
    }

    String dir() {
        String p = pathBox.getValue();
        int i = p.lastIndexOf('/');
        return i >= 0 ? p.substring(0, i + 1) : p.length() >= 2 && p.charAt(1) == ':' ? p.substring(0, 2) + "/" : "a:/";
    }

    void ask(String op, String body) {
        String p = op.equals("list") ? dir() : pathBox.getValue();
        render.editorSend(op, p, rev, body);
        status = tr("Waiting for the computer...");
    }

    void open() {
        rev = "";
        ask("open", "");
    }

    void save() {
        ask("save", editor.buffer.text());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (String[] f; (f = render.editorReplies.poll()) != null; ) {
            switch (f[0]) {
                case "file" -> {
                    pathBox.setValue(f[2]);
                    rev = f[3];
                    editor.buffer.setText(f[4]);
                    status = tr("Opened %1$", f[2]);
                }
                case "saved" -> {
                    rev = f[3];
                    status = tr("Saved %1$", f[2]);
                }
                case "conflict" -> {
                    rev = f[3];
                    status = tr("Changed on the disk since you opened it: Save again to overwrite.");
                }
                case "list" -> status = f[4].isEmpty() ? tr("No files in %1$", f[2]) : f[2] + ": " + f[4].replace("\n", "  ");
                case "ran" -> status = tr("Running %1$ (its output is on the Terminal tab)", f[2]);
                default -> status = f[4];
            }
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int k = event.key();
        if (files && editor != null && editor.isFocused() && k != com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            editor.keyPressed(event);
            return true;
        }
        if (!files && input != null && input.isFocused()) {
            if (k == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || k == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
                String line = input.getValue();
                render.clientSendString(ComputerElement.TYPE_ID, line + "\n");
                scrollBack = 0;
                if (!line.isEmpty()) history.add(line);
                historyAt = -1;
                input.setValue("");
                return true;
            }
            if ((k == com.mojang.blaze3d.platform.InputConstants.KEY_UP || k == com.mojang.blaze3d.platform.InputConstants.KEY_DOWN) && !history.isEmpty()) {
                if (k == com.mojang.blaze3d.platform.InputConstants.KEY_UP) historyAt = historyAt < 0 ? history.size() - 1 : Math.max(0, historyAt - 1);
                else historyAt = historyAt < 0 ? -1 : historyAt + 1 >= history.size() ? -1 : historyAt + 1;
                input.setValue(historyAt < 0 ? "" : history.get(historyAt));
                return true;
            }
        }
        return super.keyPressed(event);
    }

    /** The wheel over the terminal scrolls it back. */
    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!files && x >= leftPos + TERM_X && x < leftPos + TERM_X + TERM_W && y >= topPos + TERM_Y && y < topPos + TERM_Y + TERM_H) {
            scrollBack += scrollY > 0 ? 3 : -3;
            scrollBack = Math.max(0, scrollBack);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x0 = leftPos + TERM_X, y0 = topPos + TERM_Y;
        if (!files) {
            g.fill(x0, y0, x0 + TERM_W, y0 + TERM_H, 0xFF101418);
            int total = render.terminal.lineCount();
            scrollBack = Math.max(0, Math.min(scrollBack, total - ROWS));
            List<String> lines = new ArrayList<>();
            for (int i = Math.max(0, total - ROWS - scrollBack); i < total - scrollBack; i++) lines.add(render.terminal.line(i));
            for (int r = 0; r < lines.size(); r++) {
                String s = lines.get(r);
                int y = y0 + 4 + r * LINE;
                for (int c = 0; c < s.length(); c++) {
                    String ch = String.valueOf(s.charAt(c));
                    if (ch.equals(" ")) continue;
                    g.text(font, ch, x0 + 4 + c * CELL + (CELL - font.width(ch)) / 2, y, 0xFF9FE8A0, false);
                }
            }
        } else {
            String s = font.plainSubstrByWidth(status, TERM_W);
            g.text(font, s, x0, y0 + TERM_H + 7, LABEL, false);
        }
        label(g, 9, SLOTS_Y - 11, tr("Chips (slot 1 to 8)"));
        label(g, 9 + 8 * 18 + 14, SLOTS_Y - 11, tr("Drives A, B"));
        int sy = TERM_Y + TERM_H + 28;
        label(g, 128, sy, render.stateTextClient());
        label(g, 190, sy, tr("Address"));
    }
}
