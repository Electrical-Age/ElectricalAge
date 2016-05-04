package mods.eln.node

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Publishable {
    fun needPublish()
}

class published<T>(var value: T) : ReadWriteProperty<Publishable, T> {
    override fun getValue(receiver: Publishable, property: KProperty<*>): T = value
    override fun setValue(receiver: Publishable, property: KProperty<*>, newValue: T) {
        value = newValue
        receiver.needPublish()
    }
}
