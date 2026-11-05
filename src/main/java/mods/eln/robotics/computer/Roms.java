package mods.eln.robotics.computer;

import mods.eln.robotics.computer.cpu.Rom;

/** The ROM chips' images (rom/ in the repository, built into resources), loaded once and shared by every computer. */
public final class Roms {
    private Roms() {
    }

    private static volatile Rom janet, boot;

    /** The Janet ROM, or null if the image is missing from the jar. */
    public static Rom janet() {
        Rom r = janet;
        if (r == null) {
            synchronized (Roms.class) {
                if (janet == null) janet = load("/eln/computer/rom/janet.rom");
                r = janet;
            }
        }
        return r;
    }

    /** The Floppy Boot ROM, or null if the image is missing from the jar. */
    public static Rom boot() {
        Rom r = boot;
        if (r == null) {
            synchronized (Roms.class) {
                if (boot == null) boot = load("/eln/computer/rom/boot.rom");
                r = boot;
            }
        }
        return r;
    }

    private static Rom load(String path) {
        try (var in = Roms.class.getResourceAsStream(path)) {
            return in == null ? null : new Rom(in.readAllBytes());
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
