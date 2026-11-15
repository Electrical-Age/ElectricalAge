package mods.eln.transparentnode.heatfurnace;

import mods.eln.item.ThermalIsolatorElement;
import mods.eln.misc.INBTTReady;
import mods.eln.misc.Utils;
import mods.eln.server.SaveConfig;
import mods.eln.sim.IProcess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class HeatFurnaceInventoryProcess implements IProcess, INBTTReady {

    HeatFurnaceElement furnace;
    //double combustibleEnergyMax = 40000 * 2;
    double combustibleBuffer = 0;

    public HeatFurnaceInventoryProcess(HeatFurnaceElement furnace) {
        this.furnace = furnace;
    }

    @Override
    public void process(double time) {
        ItemStack combustibleStack = furnace.inventory.getItem(HeatFurnaceContainer.combustibleId);
        ItemStack combustionChamberStack = furnace.inventory.getItem(HeatFurnaceContainer.combustrionChamberId);
        ItemStack isolatorChamberStack = furnace.inventory.getItem(HeatFurnaceContainer.isolatorId);

        double isolationFactor = 1;
        if (!Utils.isEmpty(isolatorChamberStack)) {
            if (furnace.thermalLoad.Tc > ((ThermalIsolatorElement)ThermalIsolatorElement.getDescriptor(isolatorChamberStack)).getTmax()) {
                furnace.inventory.decrStackSize(HeatFurnaceContainer.isolatorId, 1);
            } else {
                ThermalIsolatorElement iso = (ThermalIsolatorElement) Utils.getItemObject(isolatorChamberStack);
                isolationFactor = iso.getConductionFactor();
            }
        }
        furnace.thermalLoad.setRp(furnace.descriptor.thermal.Rp / isolationFactor);

        int combustionChamberNbr = 0;
        if (!Utils.isEmpty(combustionChamberStack)) {
            combustionChamberNbr = combustionChamberStack.getCount();
        }
        furnace.furnaceProcess.nominalPower = furnace.descriptor.nominalPower + furnace.descriptor.combustionChamberPower * combustionChamberNbr;

        if (furnace.getTakeFuel() && SaveConfig.instance != null) {
            if (!SaveConfig.instance.heatFurnaceFuel) {
                combustibleBuffer = furnace.furnaceProcess.nominalCombustibleEnergy;
            } else if (!Utils.isEmpty(combustibleStack)) {
                double itemEnergy = Utils.getItemEnergie(combustibleStack) * mods.eln.ElnConfig.heatFurnaceFuelFactor;   // (Baughn 2026-10-26: with turbines by the expansion)
                if (itemEnergy != 0) {
                    if (furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy) {
                        //	furnace.furnaceProcess.combustibleEnergy += itemEnergy;
                        combustibleBuffer += itemEnergy;
                        burnOne(combustibleStack);
                    }
                }
            }
        }

        if (furnace.furnaceProcess.combustibleEnergy + combustibleBuffer < furnace.furnaceProcess.nominalCombustibleEnergy) {
            furnace.furnaceProcess.combustibleEnergy += combustibleBuffer;
            combustibleBuffer = 0;
        } else {
            double delta = furnace.furnaceProcess.nominalCombustibleEnergy - furnace.furnaceProcess.combustibleEnergy;
            furnace.furnaceProcess.combustibleEnergy += delta;
            combustibleBuffer -= delta;
        }
    }

    /**
     * Takes one item of the fuel slot, and leaves what burning it leaves: the stack's own crafting remainder, as a
     * vanilla furnace does (a lava bucket's bucket; another mod's fuel that gives itself back). 1.12 knew only items
     * named "bucket" and ate the rest (nero 2026-11-14: Occultism's unbreakable core piece). With more of the stack
     * left in the slot, the remainder drops at the furnace.
     */
    void burnOne(ItemStack fuel) {
        net.minecraft.world.item.ItemStackTemplate remainder = fuel.getCraftingRemainder();
        furnace.inventory.decrStackSize(HeatFurnaceContainer.combustibleId, 1);
        if (remainder == null) return;
        ItemStack left = remainder.create();
        if (Utils.isEmpty(furnace.inventory.getItem(HeatFurnaceContainer.combustibleId))) {
            furnace.inventory.setItem(HeatFurnaceContainer.combustibleId, left);
        } else {
            furnace.node.dropItem(left);
        }
    }

    @Override
    public void readFromNBT(CompoundTag nbt, String str) {
        combustibleBuffer = nbt.getDoubleOr(str + "HFIP" + "combustribleBuffer", 0.0);
    }

    @Override
    public void writeToNBT(CompoundTag nbt, String str) {
        nbt.putDouble(str + "HFIP" + "combustribleBuffer", combustibleBuffer);
    }
}
