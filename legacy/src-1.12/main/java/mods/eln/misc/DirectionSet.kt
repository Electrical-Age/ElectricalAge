package mods.eln.misc

import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

class DirectionSet : TreeSet<Direction>() {
    private fun Int.setBit(position: Int): Int = this or (1 shl position)
    private fun Int.isBitSet(position: Int): Boolean = this and (1 shl position) != 0

    fun toInt(): Int {
        var value = 0
        if (contains(Direction.XN)) value = value.setBit(0)
        if (contains(Direction.XP)) value = value.setBit(1)
        if (contains(Direction.YN)) value = value.setBit(2)
        if (contains(Direction.YP)) value = value.setBit(3)
        if (contains(Direction.ZN)) value = value.setBit(4)
        if (contains(Direction.ZP)) value = value.setBit(5)
        return value
    }

    fun fromInt(value: Int) {
        clear()
        if (value.isBitSet(0)) add(Direction.XN)
        if (value.isBitSet(1)) add(Direction.XP)
        if (value.isBitSet(2)) add(Direction.YN)
        if (value.isBitSet(3)) add(Direction.YP)
        if (value.isBitSet(4)) add(Direction.ZN)
        if (value.isBitSet(5)) add(Direction.ZP)
    }

    fun serialize(stream: DataOutputStream) {
        stream.writeInt(toInt())
    }

    fun deserialize(stream: DataInputStream) {
        clear()
        fromInt(stream.readInt())
    }

    fun writeToNBT(nbt: NBTTagCompound, name: String) {
        nbt.setInteger(name, toInt())
    }

    fun readFromNBT(nbt: NBTTagCompound, name: String) {
        fromInt(nbt.getInteger(name))
    }
}
