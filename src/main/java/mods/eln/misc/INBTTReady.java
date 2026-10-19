package mods.eln.misc;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

public interface INBTTReady {
    public abstract void readFromNBT(CompoundTag nbt, String str);

    public abstract void writeToNBT(CompoundTag nbt, String str);

    /** readFromNBT(nbt, "") on every INBTTReady entry of the lists, in order; other entries are skipped. */
    static void readAll(CompoundTag nbt, List<?>... lists) {
        for (List<?> list : lists)
            for (Object o : list)
                if (o instanceof INBTTReady r) r.readFromNBT(nbt, "");
    }

    /** writeToNBT(nbt, "") on every INBTTReady entry of the lists, in order; other entries are skipped. */
    static void writeAll(CompoundTag nbt, List<?>... lists) {
        for (List<?> list : lists)
            for (Object o : list)
                if (o instanceof INBTTReady r) r.writeToNBT(nbt, "");
    }
}
