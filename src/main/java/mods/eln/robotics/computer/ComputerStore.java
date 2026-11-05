package mods.eln.robotics.computer;

import mods.eln.robotics.computer.hw.Floppy;
import mods.eln.robotics.computer.hw.Machine;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Computers' RAM and floppies' images, kept as files beside the world's data rather than in chunk NBT (a few MB each):
 * data/eln_computers/ID.bin and data/eln_floppies/ID.img, both compressed. Writes go to one background thread in
 * order (a tmp file, then a move), so the server thread only takes the copy-on-write snapshot. Reads are on the
 * caller's thread. flush() waits for the writes (the server stopping).
 * <p>
 * It also keeps the set of disks mounted in some computer, so a cloned floppy (a creative copy, /give) can't be in two
 * drives at once (Argon's review).
 */
public final class ComputerStore {
    private ComputerStore() {
    }

    private static final java.util.concurrent.ExecutorService WRITER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "eln-computer-saves");
        t.setDaemon(true);
        return t;
    });
    private static final java.util.Set<UUID> MOUNTED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("eln.computer");

    static Path dir(MinecraftServer server, String kind) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve(kind);
    }

    static Path machineFile(MinecraftServer s, UUID id) {
        return dir(s, "eln_computers").resolve(id + ".bin");
    }

    static Path floppyFile(MinecraftServer s, UUID id) {
        return dir(s, "eln_floppies").resolve(id + ".img");
    }

    private interface Writer {
        void write(OutputStream o) throws IOException;
    }

    private static void writeAsync(Path file, Writer w) {
        WRITER.execute(() -> {
            try {
                Files.createDirectories(file.getParent());
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                try (OutputStream o = new java.io.BufferedOutputStream(Files.newOutputStream(tmp))) {
                    w.write(o);
                }
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                LOG.error("could not write {}", file, e);
            }
        });
    }

    public static void saveMachine(MinecraftServer s, UUID id, Machine.Saved saved) {
        writeAsync(machineFile(s, id), saved::write);
    }

    /** The saved machine, or null (none, or unreadable). Waits for pending writes of it first. */
    public static Machine.Saved loadMachine(MinecraftServer s, UUID id) {
        flush();
        Path f = machineFile(s, id);
        if (!Files.exists(f)) return null;
        try (InputStream in = new java.io.BufferedInputStream(Files.newInputStream(f))) {
            return Machine.Saved.read(in);
        } catch (IOException e) {
            LOG.error("could not read {}", f, e);
            return null;
        }
    }

    public static void deleteMachine(MinecraftServer s, UUID id) {
        Path f = machineFile(s, id);
        WRITER.execute(() -> {
            try {
                Files.deleteIfExists(f);
            } catch (IOException e) {
                LOG.error("could not delete {}", f, e);
            }
        });
    }

    public static void saveFloppy(MinecraftServer s, UUID id, byte[] image) {
        writeAsync(floppyFile(s, id), o -> {
            var d = new java.util.zip.DeflaterOutputStream(o);
            d.write(image);
            d.finish();
        });
    }

    /** A disk's image (all zeros if it has none yet). */
    public static byte[] loadFloppy(MinecraftServer s, UUID id) {
        flush();
        Path f = floppyFile(s, id);
        if (!Files.exists(f)) return new byte[Floppy.BYTES];
        try (InputStream in = new java.util.zip.InflaterInputStream(new java.io.BufferedInputStream(Files.newInputStream(f)))) {
            byte[] b = in.readNBytes(Floppy.BYTES);
            return b.length == Floppy.BYTES ? b : java.util.Arrays.copyOf(b, Floppy.BYTES);
        } catch (IOException e) {
            LOG.error("could not read {}", f, e);
            return new byte[Floppy.BYTES];
        }
    }

    /** Copy a disk's image to a new id (a cloned floppy put in a second drive). */
    public static void copyFloppy(MinecraftServer s, UUID from, UUID to) {
        saveFloppy(s, to, loadFloppy(s, from));
    }

    /** Claim a disk for a drive; false: it is already in one. */
    public static boolean mount(UUID id) {
        return MOUNTED.add(id);
    }

    public static void unmount(UUID id) {
        MOUNTED.remove(id);
    }

    /** Wait until every write so far is on disk. */
    public static void flush() {
        try {
            WRITER.submit(() -> {
            }).get(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("computer saves did not finish", e);
        }
    }

    /** The server stopped: writes finished, mounts forgotten. */
    public static void serverStopped() {
        flush();
        MOUNTED.clear();
    }
}
