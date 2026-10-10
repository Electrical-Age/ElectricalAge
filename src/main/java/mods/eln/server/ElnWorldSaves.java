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
        loaded.add(Dimensions.id(l));
        Path main = file(l, ""), bak = file(l, ".bak");
        if (!Files.exists(main) && !Files.exists(bak)) return; // fresh world / dimension
        try {
            read(main);
        } catch (Exception ex) {
            Eln.LOG.error("Electrical Age: cannot read " + main + ", trying the backup", ex);
            try {
                read(bak);
            } catch (Exception ex2) {
                Eln.LOG.error("Electrical Age: cannot read the backup save " + bak, ex2);
            }
        }
    }

    static void read(Path p) throws IOException {
        CompoundTag nbt = NbtIo.readCompressed(p, NbtAccounter.unlimitedHeap());
        try {
            NodeManager.instance.loadFromNbt(nbt.getCompoundOrEmpty("nodes"));
        } catch (Exception e) {
            Eln.LOG.error("Electrical Age: loading nodes", e);
        }
        if (nbt.contains("config")) SaveConfig.instance.readFromNBT(nbt.getCompoundOrEmpty("config"));
        if (nbt.contains("wind")) Eln.wind.readFromNBT(nbt.getCompoundOrEmpty("wind"), "");
        try {
            Eln.ghostManager.loadFromNBT(nbt.getCompoundOrEmpty("ghost"));
        } catch (Exception e) {
            Eln.LOG.error("Electrical Age: loading ghosts", e);
        }
    }

    static void save(ServerLevel l) {
        int dim = Dimensions.id(l);
        if (!loaded.contains(dim)) return;
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
