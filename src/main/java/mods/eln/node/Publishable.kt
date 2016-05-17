package mods.eln.node

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Publishable {
    fun needPublish()
}

class published<T>(var value: T) : ReadWriteProperty<Publishable, T> {
    override fun getValue(thisRef: Publishable, property: KProperty<*>): T = this.value
    override fun setValue(thisRef: Publishable, property: KProperty<*>, value: T) {
        this.value = value
        thisRef.needPublish()
    }
}
