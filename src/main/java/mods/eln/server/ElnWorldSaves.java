package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Dimensions;
import mods.eln.node.NodeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;

/**
 * EA's own save file per dimension, as in 1.12 (ServerEventListener): &lt;world&gt;/data/electricalAgeWorld&lt;dim&gt;.dat
 * holding {"nodes": NodeManager, "ghost": GhostManager}, written through a .tmp and with a .bak of the previous save.
 * Nodes live outside the chunk data, so they keep simulating whatever is loaded (1.12 semantics, P1-DESIGN.md).
 */
public final class ElnWorldSaves {
    private ElnWorldSaves() {
    }

    private static final Set<Integer> loaded = new HashSet<>();
    /** Dimensions that must not be saved this session (a save file could not be read and is still in place). */
    private static final Set<Integer> blocked = new HashSet<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, e -> {
            if (e.getLevel() instanceof ServerLevel l) load(l);
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Save.class, e -> {
            if (e.getLevel() instanceof ServerLevel l) save(l);
        });
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, e -> {
            if (e.getLevel() instanceof ServerLevel l) unload(l);
        });
    }

    static Path file(ServerLevel l, String suffix) {
        return l.getServer().getWorldPath(LevelResource.ROOT).resolve("data")
            .resolve("electricalAgeWorld" + Dimensions.id(l) + ".dat" + suffix);
    }

    static void load(ServerLevel l) {
        int dim = Dimensions.id(l);
        loaded(dim, load(file(l, ""), file(l, ".bak"), dim));
    }

    /** Records the outcome of loading dim: saved from now on, or blocked (see maySave). */
    static void loaded(int dim, boolean safe) {
        if (safe) {
            loaded.add(dim);
            blocked.remove(dim);
        } else {
            loaded.remove(dim);
            blocked.add(dim);
        }
    }

    /** Forgets dim's load outcome (tests; a world does this on LevelEvent.Unload). */
    static void unloadForTest(int dim) {
        loaded.remove(dim);
        blocked.remove(dim);
    }

    /** May dim be saved now? A blocked dimension logs an error at every save it skips, so the loss is not missed. */
    static boolean maySave(int dim) {
        if (blocked.contains(dim)) {
            Eln.LOG.error("Electrical Age: NOT saving dimension {}: its save file could not be read at load and is kept as it was. "
                + "EA changes in this dimension are lost when the server stops; fix the file (permissions, disk) and restart.", dim);
            return false;
        }
        return loaded.contains(dim);
    }

    /**
     * Is ex (or a cause) a sign that the file's content is bad (gzip or NBT format, cut short), rather than that it
     * could not be read at all (permissions, a disk or network error)? Only bad content is set aside.
     */
    static boolean isFormatError(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof java.util.zip.ZipException || t instanceof java.io.EOFException
                || t instanceof java.io.UTFDataFormatException || t instanceof java.io.CharConversionException) return true;
            if (t instanceof IOException) return false;
            if (t.getCause() == t) break;
        }
        return ex instanceof RuntimeException;   // NBT parse errors (ReportedNbtException, NbtAccounterException...)
    }

    /**
     * Loads a dimension's node file, else its backup. A file whose content is bad (isFormatError) is renamed aside
     * to &lt;name&gt;.corrupt-&lt;time&gt; first, so no later save can overwrite it (a save moves .dat over .dat.bak): the
     * player's EA world stays recoverable by hand. A file that can't be read at all (permissions, I/O error) is left
     * where it is and the next one is tried. Returns whether the dimension may be saved: false if a file could not be
     * read or could not be moved aside; EA then does not save this dimension at all this session (maySave).
     */
    static boolean load(Path main, Path bak, int dim) {
        Path tmp = main.resolveSibling(main.getFileName() + ".tmp");
        if (!Files.exists(main) && !Files.exists(tmp) && !Files.exists(bak)) return true; // fresh world / dimension
        boolean safe = true;
        CompoundTag nbt = null;
        // no .dat: a save crashed between its moves. A .tmp that is whole (checkWhole: gzip length and CRC) is then
        // the newest save; a .tmp next to a .dat is a write that never got moved, and is ignored.
        Path[] order = Files.exists(main) ? new Path[]{main, bak} : new Path[]{tmp, bak};
        for (Path p : order) {
            if (!Files.exists(p)) continue;
            try {
                if (p == tmp) checkWhole(p);
                nbt = NbtIo.readCompressed(p, NbtAccounter.unlimitedHeap());
                if (p == tmp) Eln.LOG.warn("Electrical Age: {} is missing; loaded the complete newer save {}", main, tmp);
                break;
            } catch (Exception ex) {
                if (!isFormatError(ex)) {
                    Eln.LOG.error("Electrical Age: cannot read the save file " + p + " (not a format error: left in place; "
                        + "EA will NOT save dimension " + dim + " this session)", ex);
                    safe = false;
                } else if (p == tmp) Eln.LOG.warn("Electrical Age: ignoring the half-written save file " + p, ex);
                else {
                    Eln.LOG.error("Electrical Age: cannot read the save file " + p, ex);
                    safe &= setAside(p);
                }
            }
        }
        if (nbt == null) {
            Eln.LOG.error("**************************************************************************************");
            Eln.LOG.error("Electrical Age: NO READABLE SAVE for dimension {}: {} and {} both failed to load.", dim, main, bak);
            Eln.LOG.error("Electrical Age: this dimension starts with no EA nodes; the unreadable files were kept (in place or as *.corrupt-*).");
            Eln.LOG.error("**************************************************************************************");
        } else {
            apply(nbt, dim);
        }
        if (!safe) Eln.LOG.error("Electrical Age: a save file of dimension {} could not be read or moved aside; "
            + "EA will NOT save this dimension until the server restarts, so that file is not overwritten", dim);
        return safe;
    }

    /**
     * Reads a gzip file to its end, so its trailer (length and CRC) is checked: NbtIo stops after the NBT data and
     * would take a file cut short in the trailer, or with a bad CRC, as good.
     */
    static void checkWhole(Path p) throws IOException {
        try (var in = new java.util.zip.GZIPInputStream(Files.newInputStream(p))) {
            in.transferTo(java.io.OutputStream.nullOutputStream());
        }
    }

    /** Renames an unreadable save file to &lt;name&gt;.corrupt-&lt;yyyyMMdd-HHmmss&gt; (never replacing anything). */
    static boolean setAside(Path p) {
        String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path to = p.resolveSibling(p.getFileName() + ".corrupt-" + stamp);
        for (int i = 1; Files.exists(to); i++) to = p.resolveSibling(p.getFileName() + ".corrupt-" + stamp + "-" + i);
        try {
            Files.move(p, to);
            Eln.LOG.error("Electrical Age: kept the unreadable save file as {}", to);
            return true;
        } catch (IOException e) {
            Eln.LOG.error("Electrical Age: cannot move the unreadable save file " + p + " aside", e);
            return false;
        }
    }

    static void apply(CompoundTag nbt, int dim) {
        try {
            NodeManager.instance.loadFromNbt(nbt.getCompoundOrEmpty("nodes"), dim);
        } catch (Exception e) {
            Eln.LOG.error("Electrical Age: loading nodes", e);
        }
        try {
            if (nbt.contains("config")) SaveConfig.instance.readFromNBT(nbt.getCompoundOrEmpty("config"));
            if (nbt.contains("wind")) Eln.wind.readFromNBT(nbt.getCompoundOrEmpty("wind"), "");
        } catch (Exception e) {
            Eln.LOG.error("Electrical Age: loading the save config and wind", e);
        }
        try {
            Eln.ghostManager.loadFromNBT(nbt.getCompoundOrEmpty("ghost"));
        } catch (Exception e) {
            Eln.LOG.error("Electrical Age: loading ghosts", e);
        }
    }

    static void save(ServerLevel l) {
        int dim = Dimensions.id(l);
        if (!maySave(dim)) return;
        try {
            CompoundTag nbt = new CompoundTag();
            CompoundTag nodes = new CompoundTag(), ghost = new CompoundTag();
            NodeManager.instance.saveToNbt(nodes, dim);
            Eln.ghostManager.saveToNBT(ghost, dim);
            nbt.put("nodes", nodes);
            nbt.put("ghost", ghost);
            if (dim == 0) nbt.put("config", SaveConfig.instance.writeToNBT(new CompoundTag()));
            if (dim == 0) {
                CompoundTag wind = new CompoundTag();
                Eln.wind.writeToNBT(wind, "");
                nbt.put("wind", wind);
            }
            Path main = file(l, ""), tmp = file(l, ".tmp"), bak = file(l, ".bak");
            Files.createDirectories(main.getParent());
            NbtIo.writeCompressed(nbt, tmp);
            if (Files.exists(main)) replace(main, bak);
            replace(tmp, main);
        } catch (Exception ex) {
            Eln.LOG.error("Electrical Age: saving dimension " + dim, ex);
        }
    }

    static void unload(ServerLevel l) {
        int dim = Dimensions.id(l);
        loaded.remove(dim);
        blocked.remove(dim);
        try {
            NodeManager.instance.unload(dim);
            Eln.ghostManager.unload(dim);
        } catch (Exception ex) {
            Eln.LOG.error("Electrical Age: unloading dimension " + dim, ex);
        }
    }

    private static void replace(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
