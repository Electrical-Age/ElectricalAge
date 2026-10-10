package mods.eln.sixnode.electricaldatalogger;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

public class DataLogsPrintDescriptor extends GenericItemUsingDamageDescriptor {

    public DataLogsPrintDescriptor(String name) {
        super(name);
    }

    public void initializeStack(ItemStack stack, DataLogs logs) {
        CompoundTag nbt = new CompoundTag();
        logs.writeToNBT(nbt, "");//.setByteArray("logs", logs.copyLog());
        mods.eln.generic.ItemNbt.set(stack, nbt);
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
}
