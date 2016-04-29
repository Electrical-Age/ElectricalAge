package mods.eln.sim.process.destruct

import mods.eln.mechanical.ShaftElement

class ShaftSpeedWatchdog(shaftElement: ShaftElement, max: Double): ValueWatchdog() {
    val shaftElement = shaftElement

    init {
        this.max = max
    }

    override fun getValue(): Double = shaftElement.shaft.rads
}
