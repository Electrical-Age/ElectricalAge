package mods.eln.node

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface IPublishable {
    fun needPublish()
}

class published<T>(var value: T) : ReadWriteProperty<IPublishable, T> {
    override fun getValue(thisRef: IPublishable, property: KProperty<*>): T = this.value
    override fun setValue(thisRef: IPublishable, property: KProperty<*>, value: T) {
        val changed = this.value != value
        this.value = value
        if (changed) thisRef.needPublish()
    }
}
