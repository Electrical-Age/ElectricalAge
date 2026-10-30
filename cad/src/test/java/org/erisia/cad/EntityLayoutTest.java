package org.erisia.cad;

import org.erisia.cad.entity.EntityLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityLayoutTest {
    @Test
    void flipsToYDown() {
        Model m = new Model();
        Part root = m.part("frame", null, 1, 13, -7, 0.3, 0.1, 0.2);
        Part child = m.part("wheel", root, 0, 2, 0, 0, 0, 0);
        m.add(new Box(-1, 1.1, 0, 2, 3.7, 5)).in(root).tex(28, 23);
        m.add(new Box(0, 0, 0, 1, 1, 1)).in(child);
        var parts = EntityLayout.layout(m, 24);
        var p = parts.get(0);
        assertEquals(11, p.oy(), 1e-6);
        assertEquals(-0.3f, p.rx(), 1e-6);
        assertEquals(0.1f, p.ry(), 1e-6);
        assertEquals(-0.2f, p.rz(), 1e-6);
        var b = p.boxes().get(0);
        assertEquals(-3.7f, b.y(), 1e-6);
        assertEquals(2.6f, b.h(), 1e-6);
        assertEquals(-2, parts.get(1).oy(), 1e-6);
        assertEquals("frame", parts.get(1).parent());
    }

    @Test
    void netAsVanilla() {
        Box b = new Box(0, 0, 0, 3, 2.6, 5.48);
        b.tex(28, 23);
        float[][] n = EntityLayout.net(b);
        assertArrayEquals(new float[]{33.48f, 23, 36.48f, 28.48f}, n[3], 1e-4f);   // the top: vanilla DOWN
        assertArrayEquals(new float[]{28, 28.48f, 33.48f, 31.08f}, n[0], 1e-4f);
        assertArrayEquals(new float[]{28, 23, 44.96f, 31.08f}, EntityLayout.netBounds(b), 1e-4f);
    }
}
