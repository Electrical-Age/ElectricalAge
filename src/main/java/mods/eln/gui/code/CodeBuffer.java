package mods.eln.gui.code;

import java.util.ArrayList;
import java.util.List;

/**
 * The text model of the code editor (Baughn 2026-10-30, board #45: a generic editor the signal processor uses first, and a
 * RISC-V assembler and a Lisp later): lines of text, a cursor and a selection anchor (line, column), editing, undo and
 * redo, word moves. No drawing and no game classes: the widget (client CodeEditor) draws it and feeds it keys.
 * Positions are clamped into the text. A character limit refuses edits that would pass it.
 */
public final class CodeBuffer {
    final List<StringBuilder> lines = new ArrayList<>();
    int line, col;
    /** The selection's other end, or -1 when nothing is selected. */
    int anchorLine = -1, anchorCol;
    /** Column the cursor tries to keep moving up and down. */
    int wantCol;
    final int maxChars;
    final List<Snapshot> undo = new ArrayList<>(), redo = new ArrayList<>();
    /** The kind of the last edit, so typing a word undoes as one step. */
    String lastEdit = "";
    static final int UNDO_DEPTH = 200;

    record Snapshot(String text, int line, int col) {
    }

    public CodeBuffer(String text, int maxChars) {
        this.maxChars = maxChars;
        setText(text);
    }

    public void setText(String text) {
        lines.clear();
        for (String l : text.replace("\r", "").split("\n", -1)) lines.add(new StringBuilder(l));
        line = col = 0;
        anchorLine = -1;
        wantCol = 0;
    }

    public String text() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) b.append('\n');
            b.append(lines.get(i));
        }
        return b.toString();
    }

    public int lineCount() {
        return lines.size();
    }

    public String line(int i) {
        return lines.get(i).toString();
    }

    public int cursorLine() {
        return line;
    }

    public int cursorCol() {
        return col;
    }

    public int length() {
        int n = lines.size() - 1;
        for (StringBuilder l : lines) n += l.length();
        return n;
    }

    // ---- selection

    public boolean hasSelection() {
        return anchorLine >= 0 && (anchorLine != line || anchorCol != col);
    }

    /** The selection as {line0, col0, line1, col1}, start before end, or null. */
    public int[] selection() {
        if (!hasSelection()) return null;
        boolean anchorFirst = anchorLine < line || anchorLine == line && anchorCol < col;
        return anchorFirst ? new int[]{anchorLine, anchorCol, line, col} : new int[]{line, col, anchorLine, anchorCol};
    }

    public String selectedText() {
        int[] s = selection();
        if (s == null) return "";
        if (s[0] == s[2]) return lines.get(s[0]).substring(s[1], s[3]);
        StringBuilder b = new StringBuilder(lines.get(s[0]).substring(s[1]));
        for (int i = s[0] + 1; i < s[2]; i++) b.append('\n').append(lines.get(i));
        return b.append('\n').append(lines.get(s[2]), 0, s[3]).toString();
    }

    public void selectAll() {
        anchorLine = 0;
        anchorCol = 0;
        line = lines.size() - 1;
        col = lines.get(line).length();
    }

    void startOrDropSelection(boolean select) {
        if (select) {
            if (anchorLine < 0) {
                anchorLine = line;
                anchorCol = col;
            }
        } else anchorLine = -1;
    }

    // ---- moves

    /** Put the cursor at (l, c), clamped; with select, the selection grows to it. */
    public void moveTo(int l, int c, boolean select) {
        startOrDropSelection(select);
        line = Math.max(0, Math.min(lines.size() - 1, l));
        col = Math.max(0, Math.min(lines.get(line).length(), c));
        wantCol = col;
        lastEdit = "";
    }

    public void left(boolean select, boolean word) {
        if (!select && hasSelection()) {
            int[] s = selection();
            moveTo(s[0], s[1], false);
            return;
        }
        if (word) {
            int[] p = wordLeft(line, col);
            moveTo(p[0], p[1], select);
        } else if (col > 0) moveTo(line, col - 1, select);
        else if (line > 0) moveTo(line - 1, lines.get(line - 1).length(), select);
        else moveTo(0, 0, select);
    }

    public void right(boolean select, boolean word) {
        if (!select && hasSelection()) {
            int[] s = selection();
            moveTo(s[2], s[3], false);
            return;
        }
        if (word) {
            int[] p = wordRight(line, col);
            moveTo(p[0], p[1], select);
        } else if (col < lines.get(line).length()) moveTo(line, col + 1, select);
        else if (line < lines.size() - 1) moveTo(line + 1, 0, select);
        else moveTo(line, col, select);
    }

    /** A line up, keeping the column it was moving in (on the first line: to its start). */
    public void up(boolean select) {
        int keep = wantCol;
        if (line > 0) {
            moveTo(line - 1, keep, select);
            wantCol = keep;
        } else moveTo(0, 0, select);
    }

    public void down(boolean select) {
        int keep = wantCol;
        if (line < lines.size() - 1) {
            moveTo(line + 1, keep, select);
            wantCol = keep;
        } else moveTo(line, lines.get(line).length(), select);
    }

    public void home(boolean select) {
        // to the first non-blank, then to column 0
        String l = lines.get(line).toString();
        int first = 0;
        while (first < l.length() && l.charAt(first) == ' ') first++;
        moveTo(line, col == first ? 0 : first, select);
    }

    public void end(boolean select) {
        moveTo(line, lines.get(line).length(), select);
    }

    static boolean wordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }

    int[] wordLeft(int l, int c) {
        if (c == 0) return l > 0 ? new int[]{l - 1, lines.get(l - 1).length()} : new int[]{0, 0};
        String s = lines.get(l).toString();
        int i = c;
        while (i > 0 && !wordChar(s.charAt(i - 1))) i--;
        while (i > 0 && wordChar(s.charAt(i - 1))) i--;
        return new int[]{l, i};
    }

    int[] wordRight(int l, int c) {
        String s = lines.get(l).toString();
        if (c >= s.length()) return l < lines.size() - 1 ? new int[]{l + 1, 0} : new int[]{l, c};
        int i = c;
        while (i < s.length() && !wordChar(s.charAt(i))) i++;
        while (i < s.length() && wordChar(s.charAt(i))) i++;
        return new int[]{l, i};
    }

    // ---- editing

    void remember(String kind) {
        if (!kind.equals(lastEdit) || kind.equals("other")) {
            undo.add(new Snapshot(text(), line, col));
            if (undo.size() > UNDO_DEPTH) undo.remove(0);
        }
        redo.clear();
        lastEdit = kind;
    }

    /** Remove the selection (if any); true if something went. */
    boolean deleteSelection() {
        int[] s = selection();
        if (s == null) return false;
        StringBuilder first = lines.get(s[0]);
        String tail = lines.get(s[2]).substring(s[3]);
        first.setLength(s[1]);
        first.append(tail);
        for (int i = s[2]; i > s[0]; i--) lines.remove(i);
        line = s[0];
        col = s[1];
        wantCol = col;
        anchorLine = -1;
        return true;
    }

    /** Type or paste text at the cursor (replacing the selection); refused (false) if it would pass the limit. */
    public boolean insert(String text) {
        text = text.replace("\r", "").replace("\t", "    ");
        String sel = selectedText();
        if (length() - sel.length() + text.length() > maxChars) return false;
        remember(text.length() == 1 && wordChar(text.charAt(0)) ? "type" : "other");
        deleteSelection();
        String[] parts = text.split("\n", -1);
        StringBuilder cur = lines.get(line);
        String tail = cur.substring(col);
        cur.setLength(col);
        cur.append(parts[0]);
        int l = line;
        for (int i = 1; i < parts.length; i++) lines.add(++l, new StringBuilder(parts[i]));
        lines.get(l).append(tail);
        line = l;
        col = lines.get(l).length() - tail.length();
        wantCol = col;
        return true;
    }

    /** Enter: a new line keeping the current line's indent. */
    public boolean newline() {
        String l = lines.get(line).toString();
        int indent = 0;
        while (indent < Math.min(col, l.length()) && l.charAt(indent) == ' ') indent++;
        return insert("\n" + " ".repeat(indent));
    }

    public void backspace(boolean word) {
        if (hasSelection()) {
            remember("other");
            deleteSelection();
            return;
        }
        if (line == 0 && col == 0) return;
        remember("back");
        int[] to = word ? wordLeft(line, col) : col > 0 ? new int[]{line, col - 1} : new int[]{line - 1, lines.get(line - 1).length()};
        anchorLine = to[0];
        anchorCol = to[1];
        deleteSelection();
    }

    public void delete(boolean word) {
        if (hasSelection()) {
            remember("other");
            deleteSelection();
            return;
        }
        if (line == lines.size() - 1 && col == lines.get(line).length()) return;
        remember("del");
        int[] to = word ? wordRight(line, col) : col < lines.get(line).length() ? new int[]{line, col + 1} : new int[]{line + 1, 0};
        anchorLine = to[0];
        anchorCol = to[1];
        deleteSelection();
    }

    /** Cut: the selected text, removed. */
    public String cut() {
        String s = selectedText();
        if (!s.isEmpty()) {
            remember("other");
            deleteSelection();
        }
        return s;
    }

    public boolean undo() {
        if (undo.isEmpty()) return false;
        redo.add(new Snapshot(text(), line, col));
        restore(undo.remove(undo.size() - 1));
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) return false;
        undo.add(new Snapshot(text(), line, col));
        restore(redo.remove(redo.size() - 1));
        return true;
    }

    void restore(Snapshot s) {
        setText(s.text());
        moveTo(s.line(), s.col(), false);
        lastEdit = "";
    }
}
