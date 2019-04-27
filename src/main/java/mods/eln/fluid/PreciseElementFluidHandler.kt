package mods.eln.fluid

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing

class PreciseElementFluidHandler(tankSize: Int) : ElementFluidHandler(tankSize) {
    private var fixup = 0.0

    override fun readFromNBT(nbt: NBTTagCompound, str: String?) {
        super.readFromNBT(nbt, str)
        fixup = nbt.getDouble(str + "fixup")
    }

    override fun writeToNBT(nbt: NBTTagCompound, str: String?): NBTTagCompound? {
        super.writeToNBT(nbt, str)
        nbt.setDouble(str + "fixup", fixup)
        return nbt;
    }

    fun drain(demand: Double): Double {
        val drain = Math.ceil(demand - fixup)
        val drained = drain(drain.toInt(), true)?.amount?.toDouble() ?: 0.0
        val available = fixup + drained
        val actual = Math.min(demand, available)
        fixup = Math.max(0.0, available - demand)
        return actual
    }

    fun drainEnergy(energy: Double): Double {
        val heatValue = FuelRegistry.heatEnergyPerMilliBucket(tank.fluid?.getFluid())
        return if (heatValue > 0)
            heatValue * drain(energy / heatValue)
        else
            0.0
    }
}
