package org.erisia.cad.tex;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * An atlas of 16 px cells, one material each (EA node models: one texture per model). Cells are laid out in order,
 * cols wide; a material's cell is (col, row), row 0 at the top of the image.
 */
public final class Atlas {
    public final int cols, rows;
    public final Map<String, int[]> cells = new LinkedHashMap<>();
    /** The palette entry each cell is painted with (a material may be a model's alias: its "paint" is a tier paint). */
    public final Map<String, String> entry = new LinkedHashMap<>();

    public Atlas(int cols, String... materials) {
        this.cols = cols;
        this.rows = (materials.length + cols - 1) / cols;
        for (int i = 0; i < materials.length; i++) {
            cells.put(materials[i], new int[]{i % cols, i / cols});
            entry.put(materials[i], materials[i]);
        }
    }

    /** Cells for the materials (in order), each painted with the palette entry it maps to. */
    public Atlas(int cols, Map<String, String> materialToEntry) {
        this(cols, materialToEntry.keySet().toArray(new String[0]));
        entry.putAll(materialToEntry);
    }

    /** Paint every cell from the palette. */
    public Sheet paint(long seed) {
        Sheet s = new Sheet(16 * cols, 16 * rows, seed);
        SplittableRandom rnd = new SplittableRandom(seed);
        for (var e : cells.entrySet()) Palette.paint(s, 16 * e.getValue()[0], 16 * e.getValue()[1], Palette.get(entry.get(e.getKey())), rnd);
        return s;
    }
}
