package mods.eln.oil;

import mods.eln.fluid.NodeFluidView;
import mods.eln.sim.fluid.EaUnits;
import mods.eln.sim.fluid.SpeciesRegistry;
import mods.eln.sim.fluid.Water;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The boiler's vessel as a fluid capability (NodeFluidView): water goes in (LIQUID) up to its rated fill, steam comes
 * out (GAS) once it boils. It holds nothing of its own; WATER_MB is the rating (the gauge glass's full mark), the same
 * mark its feed float closes at (BoilerElement.RATED_KG). The 1.12 boiler was two int tanks with these caps; saves from
 * before the vessel still carry their amounts (readFromNBT), which a new vessel is filled from.
 */
public class BoilerTanks extends NodeFluidView {
    public static final int WATER_MB = 8000;

    public BoilerTanks() {
        super(new Spec().liquids(id -> id == SpeciesRegistry.WATER).liquid(true, false).gas(false, true, 0, true)
            .liquidShare(WATER_MB * EaUnits.KG_PER_MB_WATER / Water.RHO_L / BoilerElement.VOLUME));
    }

    public int water() {
        return amount(LIQUID);
    }

    public int steam() {
        return amount(GAS);
    }

    /** Pour water in as a bucket or a test would (mB): what fits up to the rated fill; returns what went in. */
    public int fillWater(int mB) {
        try (var tx = Transaction.openRoot()) {
            int n = insert(LIQUID, FluidResource.of(Fluids.WATER), mB, tx);
            tx.commit();
            return n;
        }
    }

    /** Whether mB more water would fit (a bucket's worth). */
    public boolean fits(int mB) {
        try (var tx = Transaction.openRoot()) {
            return insert(LIQUID, FluidResource.of(Fluids.WATER), mB, tx) >= mB;
        }
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "water", water());
        nbt.putInt(prefix + "steam", steam());
    }

    /** The amounts saved (a save from before the vessel: what a new vessel is filled from). */
    public void readFromNBT(CompoundTag nbt, String prefix) {
        put(LIQUID, Fluids.WATER, nbt.getIntOr(prefix + "water", 0));
        put(GAS, Oil.STEAM.get(), nbt.getIntOr(prefix + "steam", 0));
    }
}
