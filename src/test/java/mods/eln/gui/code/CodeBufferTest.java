package mods.eln.gui.code;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The code editor's text model (board #45): typing, lines, moves, selection, words, undo/redo, the limit. */
public class CodeBufferTest {
    static CodeBuffer type(CodeBuffer b, String s) {
        for (char c : s.toCharArray()) {
            if (c == '\n') b.newline();
            else b.insert(String.valueOf(c));
        }
        return b;
    }

    @Test
    void typingAndLines() {
        CodeBuffer b = type(new CodeBuffer("", 4096), "x = A\n  y = x\nx + y");
        assertEquals("x = A\n  y = x\n  x + y", b.text(), "a new line keeps the indent");
        assertEquals(3, b.lineCount());
        assertEquals(2, b.cursorLine());
    }

    @Test
    void movesAndSelection() {
        CodeBuffer b = new CodeBuffer("abc def\nghij", 4096);
        b.moveTo(0, 7, false);
        b.down(false);
        assertEquals(1, b.cursorLine());
        assertEquals(4, b.cursorCol(), "clamped to the shorter line");
        b.up(false);
        assertEquals(7, b.cursorCol(), "back to the column it moved in");
        b.moveTo(0, 0, false);
        b.right(true, true);
        assertEquals("abc", b.selectedText());
        b.down(true);
        assertEquals("abc def\nghi", b.selectedText());
        b.insert("Z");
        assertEquals("Zj", b.text());
        b.selectAll();
        assertEquals("Zj", b.selectedText());
    }

    @Test
    void deleting() {
        CodeBuffer b = new CodeBuffer("ab\ncd", 4096);
        b.moveTo(1, 0, false);
        b.backspace(false);
        assertEquals("abcd", b.text());
        b.moveTo(0, 4, false);
        b.backspace(true);
        assertEquals("", b.text());
        b = new CodeBuffer("one two", 4096);
        b.delete(true);
        assertEquals(" two", b.text());
    }

    @Test
    void undoAndRedo() {
        CodeBuffer b = type(new CodeBuffer("", 4096), "abc");
        b.insert(" ");
        type(b, "de");
        assertEquals("abc de", b.text());
        assertTrue(b.undo());
        assertEquals("abc ", b.text(), "a word undoes as one step");
        b.undo();
        assertEquals("abc", b.text());
        b.undo();
        assertEquals("", b.text());
        assertFalse(b.undo());
        b.redo();
        b.redo();
        assertEquals("abc ", b.text());
        b.insert("x");
        assertFalse(b.redo(), "an edit drops the redo");
    }

    @Test
    void cutPasteAndTheLimit() {
        CodeBuffer b = new CodeBuffer("hello world", 12);
        b.moveTo(0, 0, false);
        b.moveTo(0, 5, true);
        assertEquals("hello", b.cut());
        assertEquals(" world", b.text());
        b.moveTo(0, 6, false);
        assertTrue(b.insert("!\nok"));
        assertEquals(" world!\nok", b.text());
        assertFalse(b.insert("toolong"), "past the limit: refused");
        assertEquals(" world!\nok", b.text());
    }
}
