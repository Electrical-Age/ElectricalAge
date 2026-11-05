package mods.eln.robotics.computer;

import mods.eln.client.gui.EaField;
import mods.eln.client.gui.ElnScreen;
import mods.eln.gui.ElnNodeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import static mods.eln.i18n.I18N.tr;

/**
 * The computer's screen: the terminal (TerminalText, fixed 6 px cells as the code editor's), an input line that sends
 * what you type with Enter, the 8 chip slots and 2 floppy drives under it, its state, address and buttons.
 */
public class ComputerScreen extends ElnScreen {
    public static final int WIDTH = 330, HEIGHT = 366, TERM_X = 5, TERM_Y = 18, ROWS = 17, CELL = 6, LINE = 10;
    public static final int SLOTS_Y = 252, INV_X = 84, INV_Y = 284;
    static final int TERM_W = TerminalText.COLS * CELL + 8, TERM_H = ROWS * LINE + 6;
    final ComputerRender render;
    EditBox input;
    final java.util.List<String> history = new java.util.ArrayList<>();
    int historyAt = -1;

    public ComputerScreen(ElnNodeMenu menu, Inventory inventory, Component title, Object render) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        this.render = (ComputerRender) render;
    }

    @Override
    protected void init() {
        super.init();
        input = new EditBox(font, leftPos + TERM_X, topPos + TERM_Y + TERM_H + 4, TERM_W, 14, Component.literal("input"));
        input.setMaxLength(1000);
        input.setTextColor(0xFFE6E6E6);
        addRenderableWidget(input);
        setInitialFocus(input);
        int by = TERM_Y + TERM_H + 22;
        button(9, by, 60, () -> tr("Reset"), () -> render.clientSendId(ComputerElement.RESET_ID),
            tr("Start again from slot 1's chip (the RAM keeps what it held)."));
        button(9 + 62, by, 50, () -> tr("Break"), () -> render.clientSendId(ComputerElement.BREAK_ID),
            tr("Stop what the REPL is running (a loop that never ends, a long sleep)."));
        field(230, by, 36, () -> render.address == 0 ? "" : String.valueOf(render.address), v -> {
            float f = EaField.parse(v);
            render.clientSendFloat(ComputerElement.SET_ADDRESS_ID, Float.isNaN(f) ? 0 : Math.round(f));
        }, tr("Its RS485 address, 17 to 255: the source of the packets it sends."));
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (input != null && input.isFocused()) {
            int k = event.key();
            if (k == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || k == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
                String line = input.getValue();
                render.clientSendString(ComputerElement.TYPE_ID, line + "\n");
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

    @Override
    protected void extractDevice(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x0 = leftPos + TERM_X, y0 = topPos + TERM_Y;
        g.fill(x0, y0, x0 + TERM_W, y0 + TERM_H, 0xFF101418);
        java.util.List<String> lines = render.terminal.tail(ROWS);
        for (int r = 0; r < lines.size(); r++) {
            String s = lines.get(r);
            int y = y0 + 4 + r * LINE;
            for (int c = 0; c < s.length(); c++) {
                String ch = String.valueOf(s.charAt(c));
                if (ch.equals(" ")) continue;
                g.text(font, ch, x0 + 4 + c * CELL + (CELL - font.width(ch)) / 2, y, 0xFF9FE8A0, false);
            }
        }
        label(g, 9, SLOTS_Y - 11, tr("Chips (slot 1 to 8)"));
        label(g, 9 + 8 * 18 + 14, SLOTS_Y - 11, tr("Drives A, B"));
        int sy = TERM_Y + TERM_H + 28;
        label(g, 128, sy, render.stateTextClient());
        label(g, 190, sy, tr("Address"));
    }
}
