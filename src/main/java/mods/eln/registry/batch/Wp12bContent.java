package mods.eln.registry.batch;

import mods.eln.ElnContent;
import mods.eln.item.electricalinterface.ItemEnergyInventoryProcess;
import mods.eln.item.electricalitem.ElectricalAxe;
import mods.eln.item.electricalitem.ElectricalLampItem;
import mods.eln.item.electricalitem.ElectricalPickaxe;
import mods.eln.item.electricalitem.TreeCapitation;
import net.minecraft.world.item.ItemStack;

import static mods.eln.Eln.*;
import static mods.eln.i18n.I18N.*;

/**
 * Device batch Wp12b (26.x port, split from 1.12 Wp12Content): electrical tools (flashlights, mining drill, chainsaw)
 * and the item energy process (EA batteries in the player inventory charge the electrical items).
 * Ids (sub-UIDs) unchanged. Armour, copper tools, ores, replicator: later Wp12 steps.
 */
@SuppressWarnings({"SameParameterValue", "PointlessArithmeticExpression", "unused"})
public class Wp12bContent implements ElnContent {
    public static ItemEnergyInventoryProcess itemEnergyInventoryProcess;

    @Override
    public void preInit() {
        registerElectricalTool(121);
    }

    @Override
    public void serverAboutToStart() {
        simulator.addSlowProcess(itemEnergyInventoryProcess = new ItemEnergyInventoryProcess());
    }

    @Override
    public void serverTick() {
        TreeCapitation.INSTANCE.process(0.05);
    }

    public static void registerElectricalTool(int id) {
        int subId, completId;
        ItemStack stack;
        String name;
        {
            subId = 0;
            name = TR_NAME(Type.NONE, "Small Flashlight");

            ElectricalLampItem desc = new ElectricalLampItem(
                name,
                10, 6, 20, 12, 8, 50,// int light,int range,
                6000, 100// , energyStorage, discharge, charge
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }
        {
            subId = 1;
            name = TR_NAME(Type.NONE, "Improved Flashlight");

            ElectricalLampItem desc = new ElectricalLampItem(
                name,
                15, 8, 20, 15, 12, 50,// int light,int range,
                24000, 400// , energyStorage, discharge, charge
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 8;
            name = TR_NAME(Type.NONE, "Portable Electrical Mining Drill");

            ElectricalPickaxe desc = new ElectricalPickaxe(
                name,
                8, 3,// float strengthOn,float strengthOff,
                120000, 200, 2400// double energyStorage,double
                // energyPerBlock,double chargePower
            );
            desc.setObj(mods.eln.Eln.obj.getObj("handdrill"));   // (board #44: the hand drill model, src/cadmodels HandDrill)
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

        {
            subId = 12;
            name = TR_NAME(Type.NONE, "Portable Electrical Axe");

            ElectricalAxe desc = new ElectricalAxe(
                name,
                8, 3,// float strengthOn,float strengthOff,
                40000, 200, 800// double energyStorage,double
                // energyPerBlock,double chargePower
            );
            sharedItemStackOne.addElement(subId + (id << 6), desc);
        }

    }
}
