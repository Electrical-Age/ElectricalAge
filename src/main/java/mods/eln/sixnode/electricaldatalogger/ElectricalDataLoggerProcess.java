package mods.eln.sixnode.electricaldatalogger;



import mods.eln.misc.Utils;
import mods.eln.sim.IProcess;
import net.minecraft.world.item.ItemStack;


public class ElectricalDataLoggerProcess implements IProcess {

    ElectricalDataLoggerElement e;

    public ElectricalDataLoggerProcess(ElectricalDataLoggerElement e) {
        this.e = e;
    }

    /** Records the input every step (a mean per tick) unless paused, sends each point as it is due, prints. */
    @Override
    public void process(double time) {
        if (!e.pause && e.store.record(new float[]{(float) e.inputGate.getNormalized()}, time)) e.sendPoint(e.store.emit());
        if (e.printToDo) {
            ItemStack paperStack = e.inventory.getItem(ElectricalDataLoggerContainer.paperSlotId);
            ItemStack printStack = e.inventory.getItem(ElectricalDataLoggerContainer.printSlotId);
            if (!Utils.isEmpty(paperStack) && Utils.isEmpty(printStack)) {
                e.inventory.decrStackSize(ElectricalDataLoggerContainer.paperSlotId, 1);
                ItemStack print = mods.eln.registry.batch.Wp9cContent.dataLogsPrintDescriptor.newItemStack(1);
                mods.eln.registry.batch.Wp9cContent.dataLogsPrintDescriptor.initializeStack(print, e.view());
                e.inventory.setItem(ElectricalDataLoggerContainer.printSlotId, print);
            }
            e.printToDo = false;
        }
    }
}
