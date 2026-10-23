package mods.eln.fluid

import net.minecraft.nbt.CompoundTag

class PreciseElementFluidHandler(tankSize: Int) : ElementFluidHandler(tankSize) {
    private var fixup = 0.0

    override fun readFromNBT(nbt: CompoundTag, str: String?) {
        super.readFromNBT(nbt, str)
        fixup = nbt.getDoubleOr(str + "fixup", 0.0)
    }

    override fun writeToNBT(nbt: CompoundTag, str: String?) {
        super.writeToNBT(nbt, str)
        nbt.putDouble(str + "fixup", fixup)
    }

    fun drain(demand: Double): Double {
        val drain = Math.ceil(demand - fixup)
        val drained = drain(drain.toInt(), true).amount.toDouble()
        val available = fixup + drained
        val actual = Math.min(demand, available)
        fixup = Math.max(0.0, available - demand)
        return actual
    }

    fun drainEnergy(energy: Double): Double {
        val f = fluid
        val heatValue = if (f.isEmpty) 0.0 else FuelRegistry.heatEnergyPerMilliBucket(f.fluid)
        return if (heatValue > 0)
            heatValue * drain(energy / heatValue)
        else
            0.0
    }
}
