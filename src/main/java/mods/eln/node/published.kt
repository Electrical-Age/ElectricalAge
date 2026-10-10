package mods.eln.node

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class published<T>(var value: T, val onChange: ((it: T) -> Unit)? = null, val triggerReconnect: Boolean = false)
    : ReadWriteProperty<INodeElement, T> {
    override fun getValue(thisRef: INodeElement, property: KProperty<*>): T = this.value
    override fun setValue(thisRef: INodeElement, property: KProperty<*>, value: T) {
        val changed = this.value != value
        this.value = value
        if (changed) {
            if (triggerReconnect) thisRef.reconnect()
            thisRef.needPublish()
            onChange?.invoke(this.value)
        }
    }
}
