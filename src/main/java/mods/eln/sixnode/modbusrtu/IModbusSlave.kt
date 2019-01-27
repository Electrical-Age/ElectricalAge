package mods.eln.sixnode.modbusrtu

interface IModbusSlave {
    val slaveId: Int

    @Throws(IllegalAddressException::class)
    fun getCoil(address: Int): Boolean

    @Throws(IllegalAddressException::class)
    fun setCoil(address: Int, value: Boolean)

    @Throws(IllegalAddressException::class)
    fun getInput(address: Int): Boolean

    @Throws(IllegalAddressException::class)
    fun getInputRegister(address: Int): Short

    @Throws(IllegalAddressException::class)
    fun getHoldingRegister(address: Int): Short

    @Throws(IllegalAddressException::class)
    fun setHoldingRegister(address: Int, value: Short)
}
