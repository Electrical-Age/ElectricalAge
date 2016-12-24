package mods.eln.fluid

import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class PreciseElementFluidHandler(tankSize: Int) : ElementFluidHandler(tankSize) {
    private var fixup = 0.0

    override fun readFromNBT(nbt: NBTTagCompound, str: String?) {
        super.readFromNBT(nbt, str)
        fixup = nbt.getDouble(str + "fixup")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String?) {
        super.writeToNBT(nbt, str)
        nbt.setDouble(str + "fixup", fixup)
    }

    fun drain(demand: Double) : Double {
        val drain = Math.ceil(demand - fixup)
        val drained = drain(ForgeDirection.DOWN, drain.toInt(), true)?.amount?.toDouble() ?: 0.0
        val available = fixup + drained
        val actual = Math.min(demand, available)
        fixup = Math.max(0.0, available - demand)
        return actual
    }
}
