package mods.eln.sim;

import mods.eln.misc.Recipe;
import mods.eln.misc.RecipesList;
import mods.eln.misc.Utils;
import mods.eln.sim.mna.component.Resistor;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class ElectricalStackMachineProcess implements IProcess {

    ElectricalStackMachineProcessObserver observer;

    public IInventory inventory;
    int inputSlotId, OutputSlotId, outputSlotNbr;
    Resistor electricalResistor;
    double resistorValue;
    RecipesList recipesList;
    int[] outSlotIdList;
    double efficiency = 1.0, speedUp = 1.0;

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

    public ElectricalStackMachineProcess(IInventory inventory, int inputSlotId, int OutputSlotId, int outputSlotNbr,
                                         Resistor electricalResistor, double resistorValue, RecipesList recipesList) {
        this.inventory = inventory;
        this.inputSlotId = inputSlotId;
        this.OutputSlotId = OutputSlotId;
        this.outputSlotNbr = outputSlotNbr;
        this.electricalResistor = electricalResistor;
        this.resistorValue = resistorValue;
        this.recipesList = recipesList;

        outSlotIdList = new int[outputSlotNbr];

        for (int idx = 0; idx < outputSlotNbr; idx++) {
            outSlotIdList[idx] = idx + OutputSlotId;
        }
    }

    public void setEfficiency(double efficiency) {
        this.efficiency = efficiency;
    }

    public void setSpeedUp(double speedUp) {
        this.speedUp = speedUp;
        setResistorValue(resistorValue);
    }

    @Override
    public void process(double time) {
        ItemStack itemStackIn = inventory.getStackInSlot(inputSlotId);

        boolean itemTypeChanged = itemStackIn == null && itemStackInOld != null ||
            itemStackIn != null && itemStackInOld == null ||
            itemStackIn != null && itemStackInOld != null && !itemStackIn.getUnlocalizedName().equals(itemStackInOld.getUnlocalizedName());

        if (itemTypeChanged || (!smeltCan()) || !smeltInProcess) {
            smeltInit();
            itemStackInOld = itemStackIn;
        }

        if (smeltInProcess) {
            energyCounter += getPower() * time;
            if (energyCounter > energyNeeded) {
                energyCounter -= energyNeeded;
                smeltItem();
                smeltInit();
            }
        }
    }

    public double getPower() {
        return electricalResistor.getP() * efficiency;
    }

    public void smeltInit() {
        smeltInProcess = smeltCan();
        if (!smeltInProcess) {
            smeltInProcess = false;
            energyNeeded = 1.0;
            energyCounter = 0.0;
            electricalResistor.highImpedance();
        } else {
            smeltInProcess = true;
            energyNeeded = recipesList.getRecipe(inventory.getStackInSlot(inputSlotId)).energy;
            energyCounter = 0.0;
            electricalResistor.setR(resistorValue / speedUp);
        }
    }

    public void setResistorValue(double value) {
        resistorValue = value;
        if (smeltInProcess) electricalResistor.setR(resistorValue / speedUp);
    }

    /**
     * Returns true if the furnace can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    public boolean smeltCan() {
        if (inventory.getStackInSlot(inputSlotId) == null) {
            return false;
        } else {
            ItemStack[] output = getSmeltResult();
            if (output == null) return false;
            return Utils.canPutStackInInventory(getSmeltResult(), inventory, outSlotIdList);
        }
    }

    public ItemStack[] getSmeltResult() {
        Recipe recipe = recipesList.getRecipe(inventory.getStackInSlot(inputSlotId));
        if (recipe == null) return null;
        return recipe.output;
    }

    /**
     * Turn one item from the furnace source stack into the appropriate smelted item in the furnace result stack
     */
    public void smeltItem() {
        if (this.smeltCan()) {
            Recipe recipe = recipesList.getRecipe(inventory.getStackInSlot(inputSlotId));
            Utils.tryPutStackInInventory(recipe.getOutputCopy(), inventory, outSlotIdList);
            inventory.decrStackSize(inputSlotId, recipe.input.stackSize);
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
