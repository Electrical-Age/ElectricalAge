package mods.eln.robotics.computer.cpu;

/**
 * A ROM chip's image: up to one slot (1 MB), immutable, so every computer with the same chip shares one copy. Saves
 * refer to it by its hash, so a changed image (a mod update) is noticed.
 */
public final class Rom {
    final int[][] pages;
    public final String hash;
    public final int bytes;

    public Rom(byte[] image) {
        if (image.length > Memory.SLOT_BYTES) throw new IllegalArgumentException("ROM larger than a slot");
        bytes = image.length;
        int n = (image.length + Memory.PAGE_BYTES - 1) >> Memory.PAGE_SHIFT;
        pages = new int[n][Memory.PAGE_BYTES >> 2];
        for (int i = 0; i < image.length; i++)
            pages[i >> Memory.PAGE_SHIFT][(i & (Memory.PAGE_BYTES - 1)) >> 2] |= (image[i] & 0xFF) << ((i & 3) * 8);
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(image);
            hash = java.util.HexFormat.of().formatHex(d, 0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
