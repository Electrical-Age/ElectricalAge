package mods.eln.sim;

import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.api.Machine;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * A machine's recipe work: each tick it runs it takes a tick's rated energy from its Machine (Machine API) and counts
 * it, times its efficiency, towards the recipe's energy; then the item is done. A machine that can't run takes
 * nothing, so it draws nothing. Run in the element's slow processes, after the machine's own.
 */
public class ElectricalStackMachineProcess implements IProcess {

    ElectricalStackMachineProcessObserver observer;

    public Container inventory;
    int inputSlotId, OutputSlotId, outputSlotNbr;
    final Machine machine;
    RecipesList recipesList;
    int[] outSlotIdList;
    double efficiency = 1.0;
    /** The power taken in the last tick, W. */
    double power;
    /** Progress loaded from a save, given back to the item in the input slot at the first tick (0: none). */
    double resumeJoules;

    ItemStack itemStackInOld = null;

    boolean smeltInProcess = false;
    double energyNeeded = 0;
    double energyCounter = 0;

    public interface ElectricalStackMachineProcessObserver {
        void done(ElectricalStackMachineProcess who);
    }

    public void setObserver(ElectricalStackMachineProcessObserver observer) {
        this.observer = observer;
    }

    public ElectricalStackMachineProcess(Container inventory, int inputSlotId, int OutputSlotId, int outputSlotNbr,
                                         Machine machine, RecipesList recipesList) {
        this.inventory = inventory;
        this.inputSlotId = inputSlotId;
        this.OutputSlotId = OutputSlotId;
        this.outputSlotNbr = outputSlotNbr;
        this.machine = machine;
        this.recipesList = recipesList;

        outSlotIdList = new int[outputSlotNbr];

        for (int idx = 0; idx < outputSlotNbr; idx++) {
            outSlotIdList[idx] = idx + OutputSlotId;
        }
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }

    @Override
    public void process(double time) {
        ItemStack itemStackIn = inventory.getItem(inputSlotId);

        boolean itemTypeChanged = Utils.isEmpty(itemStackIn) && !Utils.isEmpty(itemStackInOld) ||
            !Utils.isEmpty(itemStackIn) && Utils.isEmpty(itemStackInOld) ||
            !Utils.isEmpty(itemStackIn) && !Utils.isEmpty(itemStackInOld) && itemStackIn.getItem() != itemStackInOld.getItem() /* 1.12: translation keys (damage sub-items) */;

        if (itemTypeChanged || (!smeltCan()) || !smeltInProcess) {
            smeltInit();
            itemStackInOld = itemStackIn;
        }

        if (resumeJoules > 0) { // given back once the item can be worked on; dropped with the item
            if (smeltInProcess) {
                energyCounter = Math.min(resumeJoules, energyNeeded);
                resumeJoules = 0;
            } else if (Utils.isEmpty(itemStackIn)) {
                resumeJoules = 0;
            }
        }

        power = 0;
        if (smeltInProcess) {
            double got = machine.takeJoules(machine.ratedJoulesPerTick());
            power = got / time;
            energyCounter += got * efficiency;
            if (energyCounter > energyNeeded) {
                energyCounter -= energyNeeded;
                smeltItem();
                smeltInit();
            }
        }
    }

    /** The power that went into the work in the last tick (taken x efficiency), W. */
    public double getPower() {
        return power * efficiency;
    }

    /** The current item's progress, J (what a save keeps); a loaded one not yet given back counts. */
    public double progressJoules() {
        if (resumeJoules > 0) return resumeJoules;
        return smeltInProcess ? energyCounter : 0;
    }

    /** Gives a saved item's progress back when the item in the input slot can next be worked on (dropped if it goes). */
    public void resume(double joules) {
        resumeJoules = Double.isFinite(joules) && joules > 0 ? joules : 0;
    }

    public void smeltInit() {
        smeltInProcess = smeltCan();
        if (!smeltInProcess) {
            smeltInProcess = false;
            energyNeeded = 1.0;
            energyCounter = 0.0;
        } else {
            smeltInProcess = true;
            energyNeeded = recipesList.getRecipe(inventory.getItem(inputSlotId)).energy;
            energyCounter = 0.0;
        }
    }

    /**
     * Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    /** 26.x: an extra condition to run at all (e.g. the oil separator's tank has room). */
    public java.util.function.BooleanSupplier extraCondition = () -> true;

    public boolean smeltCan() {
        if (!extraCondition.getAsBoolean()) return false;
        if (Utils.isEmpty(inventory.getItem(inputSlotId))) {
            return false;
        } else {
            ItemStack[] output = getSmeltResult();
            if (output == null) return false;
            return Utils.canPutStackInInventory(getSmeltResult(), inventory, outSlotIdList);
        }
    }

    static final java.util.Random RANDOM = new java.util.Random();

    public ItemStack[] getSmeltResult() {
        Recipe recipe = recipesList.getRecipe(inventory.getItem(inputSlotId));
        if (recipe == null) return null;
        return recipe.maxOutput();
    }

    /**
     * Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack
     */
    public void smeltItem() {
        if (this.smeltCan()) {
            Recipe recipe = recipesList.getRecipe(inventory.getItem(inputSlotId));
            Utils.tryPutStackInInventory(recipe.getOutputCopy(RANDOM), inventory, outSlotIdList);
            inventory.removeItem(inputSlotId, recipe.input.getCount());
            if (observer != null) observer.done(this);
        }
    }

    public double processState() {
        if (!smeltInProcess) return 0.0;
        double state = energyCounter / energyNeeded;
        if (state > 1.0) state = 1.0;
        return state;
    }

    public double processStatePerSecond() {
        if (!smeltInProcess) return 0;
        double power = getPower() + 0.1;
        double ret = power / (energyNeeded);
        return ret;
    }
}
