package mods.eln.misc

import net.minecraft.nbt.NBTTagCompound
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.*

class DirectionSet: TreeSet<Direction>() {
    fun toInt(): Int {
        var value = 0
        if (contains(Direction.XN)) value += 1
        if (contains(Direction.XP)) value += 2
        if (contains(Direction.YN)) value += 4
        if (contains(Direction.YP)) value += 8
        if (contains(Direction.ZN)) value += 16
        if (contains(Direction.ZP)) value += 32
        return value
    }

    fun fromInt(value: Int) {
        clear()
        if (value and 1 != 0) add(Direction.XN)
        if (value and 2 != 0) add(Direction.XP)
        if (value and 4 != 0) add(Direction.YN)
        if (value and 8 != 0) add(Direction.YP)
        if (value and 16 != 0) add(Direction.ZN)
        if (value and 32 != 0) add(Direction.ZP)
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
