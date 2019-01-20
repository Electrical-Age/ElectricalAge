package mods.eln.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public interface IConfigurable {
    void readConfigTool(NBTTagCompound compound, EntityPlayer invoker);
    void writeConfigTool(NBTTagCompound compound, EntityPlayer invoker);
}
