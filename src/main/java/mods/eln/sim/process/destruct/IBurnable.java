package mods.eln.sim.process.destruct;

import mods.eln.misc.Coordonate;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * A device that can burn out: it stays in the world but leaves the simulation (an open circuit) until repaired.
 * Implemented by SixNodeElement and TransparentNodeElement.
 */
public interface IBurnable {
    /** Why it burnt out, or null while it works. */
    @Nullable
    FailureCause getBurnt();

    /** The kind of device it failed as (picks the repair part); null while it works. */
    @Nullable
    Failure.Kind getBurntKind();

    default boolean isBurnt() {
        return getBurnt() != null;
    }

    void burn(FailureCause cause, Failure.Kind kind);

    /** Undo burn(): cooled down, watchdogs reset, back in the simulation. */
    void repair();

    Coordonate getBurnableCoordonate();

    ItemStack getDropItemStack();

    /** What a burnt device is repaired with (one item); see Repair.defaultPart. */
    default ItemStack getRepairPart() {
        return Repair.defaultPart(this);
    }
}
