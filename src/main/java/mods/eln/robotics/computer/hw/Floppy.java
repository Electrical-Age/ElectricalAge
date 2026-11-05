package mods.eln.robotics.computer.hw;

/**
 * A 1.44 MB floppy's image: 2880 sectors of 512 bytes, with the sectors written since the last save marked. While
 * inserted it belongs to its computer; the host saves it between slices.
 */
public final class Floppy {
    public static final int SECTOR = 512, SECTORS = 2880, BYTES = SECTOR * SECTORS;

    public final java.util.UUID id;
    final byte[] data;
    final java.util.BitSet dirty = new java.util.BitSet(SECTORS);
    public boolean writeProtected;

    public Floppy(java.util.UUID id, byte[] image, boolean writeProtected) {
        this.id = id;
        this.data = new byte[BYTES];
        if (image != null) System.arraycopy(image, 0, data, 0, Math.min(image.length, BYTES));
        this.writeProtected = writeProtected;
    }

    /** The whole image (a copy), and the dirty marks cleared: what a save writes. */
    public byte[] snapshot() {
        dirty.clear();
        return data.clone();
    }

    public boolean dirty() {
        return !dirty.isEmpty();
    }
}
