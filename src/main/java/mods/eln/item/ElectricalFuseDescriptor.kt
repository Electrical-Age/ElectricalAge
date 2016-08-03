package mods.eln.item

import mods.eln.misc.VoltageLevelColor

class ElectricalFuseDescriptor(name: String, val maxCurrent: Double): GenericItemUsingDamageDescriptorUpgrade(name) {
    init {
        changeDefaultIcon("electricalfuse")
        voltageLevelColor = VoltageLevelColor.fromMaxCurrent(maxCurrent)
    }
}
