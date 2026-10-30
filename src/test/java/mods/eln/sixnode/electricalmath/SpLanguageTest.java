package mods.eln.sixnode.electricalmath;

import mods.eln.gui.code.LanguageAdapter.Kind;
import mods.eln.gui.code.LanguageAdapter.Token;
import mods.eln.gui.code.LanguageAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The signal processor's colouring and checking for the editor (board #45). */
public class SpLanguageTest {
    static Kind at(List<List<Token>> t, int line, int col) {
        for (Token k : t.get(line)) if (col >= k.start() && col < k.end()) return k.kind();
        return null;
    }

    @Test
    void tokens() {
        String text = "half = A * 0.5 # a half\nb2 = integrate(B, 0); C + half\nhalf + b2 - pi";
        var t = SpLanguage.INSTANCE.tokenize(text);
        assertEquals(3, t.size());
        assertEquals(Kind.DEFINITION, at(t, 0, 0));
        assertEquals(Kind.OPERATOR, at(t, 0, 5));
        assertEquals(Kind.INPUT_A, at(t, 0, 7));
        assertEquals(Kind.NUMBER, at(t, 0, 11));
        assertEquals(Kind.COMMENT, at(t, 0, 16));
        assertEquals(Kind.DEFINITION, at(t, 1, 0));
        assertEquals(Kind.FUNCTION, at(t, 1, 5));
        assertEquals(Kind.INPUT_B, at(t, 1, 15));
        assertEquals(Kind.INPUT_C, at(t, 1, 22));
        assertEquals(Kind.NAME, at(t, 1, 26), "a defined name");
        assertEquals(Kind.NAME, at(t, 2, 0));
        assertEquals(Kind.KEYWORD, at(t, 2, 12));
    }

    @Test
    void theOutputLineHasARuleAbove() {
        assertEquals(Set.of(2), SpLanguage.INSTANCE.rulesAbove("x = A\n\ny = x\n"));
        assertEquals(Set.of(), SpLanguage.INSTANCE.rulesAbove("A * 0.5"));
        // an old expression with = is all output: its = isn't a definition
        assertEquals(Kind.INPUT_A, at(SpLanguage.INSTANCE.tokenize("A = B"), 0, 0));
    }

    @Test
    void problemsPointAtTheirToken() {
        var d = SpLanguage.INSTANCE.validate("x = y + 1\nx");
        assertEquals(1, d.size());
        assertEquals(0, d.get(0).line());
        assertEquals(4, d.get(0).start());
        assertEquals(5, d.get(0).end());
        assertTrue(d.get(0).message().contains("y is not defined above"), d.get(0).message());
        assertTrue(SpLanguage.INSTANCE.validate("x = A\nx * 2").isEmpty());
    }

    @Test
    void hoverExplainsFunctionsInputsAndNamedParts() {
        String text = "avg = rc(A, 2)\non = rs(B > 0.5, C)\nif(on, avg, 0)";
        var h = SpLanguage.INSTANCE.hover(text, 1, 6); // rs
        assertEquals("rs(set, reset)", h.get(0));
        assertTrue(h.get(2).startsWith("e.g. "));
        assertEquals("Input B", SpLanguage.INSTANCE.hover(text, 1, 8).get(0));
        assertEquals(List.of("avg", "avg = rc(A, 2)"), SpLanguage.INSTANCE.hover(text, 2, 8));
        assertEquals(6, SpLanguage.INSTANCE.hover("pid(1, A, 1, 0, 0)", 0, 1).size()); // both forms
        assertTrue(SpLanguage.INSTANCE.hover(text, 0, 5).isEmpty()); // the = sign
        // a broken line above doesn't stop it
        assertEquals("rs(set, reset)", SpLanguage.INSTANCE.hover("smooth A, latch on B\n" + text, 2, 6).get(0));
    }

    @Test
    void anInputsDefaultKeepsTheInputsColour() {
        var t = SpLanguage.INSTANCE.tokenize("A = 0.5\nA * 2");
        assertEquals(LanguageAdapter.Kind.INPUT_A, t.get(0).get(0).kind());
        assertEquals(LanguageAdapter.Kind.INPUT_A, t.get(1).get(0).kind());
        assertTrue(SpLanguage.INSTANCE.validate("A = 0.5\nA * 2").isEmpty());
        assertTrue(SpLanguage.INSTANCE.hover("A = 0.5\nA * 2", 1, 0).get(2).contains("0.5"));
    }

    @Test
    void aCommentMayFollowCode() {
        String text = "sum = A + B\nhalf = sum / 2  # mean\nhalf";
        assertTrue(SpLanguage.INSTANCE.validate(text).isEmpty(), SpLanguage.INSTANCE.validate(text).toString());
        var line = SpLanguage.INSTANCE.tokenize(text).get(1);
        assertEquals(LanguageAdapter.Kind.COMMENT, line.get(line.size() - 1).kind());
        assertEquals(LanguageAdapter.Kind.DEFINITION, line.get(0).kind());
        assertTrue(SpLanguage.INSTANCE.hover(text, 1, 18).isEmpty());   // in the comment
        assertEquals(List.of("half", "half = sum / 2"), SpLanguage.INSTANCE.hover(text, 2, 1));
        assertEquals(java.util.Set.of(2), SpLanguage.INSTANCE.rulesAbove(text));
    }
}
