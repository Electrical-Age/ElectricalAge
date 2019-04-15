package mods.eln.sixnode.modbusrtu

import mods.eln.Eln
import mods.eln.misc.Utils
import java.io.OutputStream
import java.net.*
import java.nio.ByteBuffer
import java.util.*

class ModbusTcpServer(port: Int = 1502) {
    companion object {
        private val InputBufferSize = 256
        private val OutputBufferSize = InputBufferSize
    }

    private val server = ServerSocket()
    private val connections: MutableList<ConnectionHandler> = ArrayList()
    private val slaves = TreeMap<Int, IModbusSlave>()

    init {
        if (Eln.modbusEnable) {
            try {
                server.bind(InetSocketAddress(port))
            } catch (e: BindException) {
                Utils.println("Exception while binding Modbus RTU Server. Modbus server disabled!")
                server.close()
                e.printStackTrace()
            }
            start()
        } else {
            server.close()
        }
    }

    val available: Boolean
        get() = server.isBound

    val host: String
        get() {
            val address = (server.localSocketAddress as? InetSocketAddress)?.address?.hostAddress ?: "-"
            return when (address) {
                "0.0.0.0" -> InetAddress.getLocalHost().hostAddress
                else -> address
            }
        }

    val port: Int
        get() = server.localPort

    private inner class ConnectionHandler(val socket: Socket) {
        private val inputBuffer = ByteBuffer.allocate(InputBufferSize)

        init {
            start()
        }

        fun start() = Thread(Runnable {
            while (!socket.isClosed) {
                inputBuffer.clear()
                val size = socket.inputStream.read(inputBuffer.array(), inputBuffer.position(), inputBuffer.remaining())
                if (size > 0) {
                    inputBuffer.position(inputBuffer.position() + size).flip()
                    handle(socket.outputStream)
                } else {
                    socket.close()
                }
            }
        }).start()

        private fun handle(output: OutputStream) {
            while (inputBuffer.hasRemaining()) {
                val transactionId = inputBuffer.short
                if (inputBuffer.short == 0.toShort()) {
                    val remaining = inputBuffer.short
                    if (inputBuffer.remaining() >= remaining) {
                        val slaveAddress = inputBuffer.get()
                        val functionCode = inputBuffer.get()
                        val response = ByteBuffer.allocate(OutputBufferSize)
                            .putShort(transactionId)
                            .putShort(0) // protocol
                            .putShort(0) // length, actually 0...
                            .put(slaveAddress) // start address
                        val slave = slaves[slaveAddress.toInt()]
                        if (slave != null) {
                            synchronized(slave) {
                                when (functionCode.toInt()) {
                                    0x01 -> readCoils(slave, response)
                                    0x02 -> readDiscreteInputs(slave, response)
                                    0x03 -> readHoldingRegisters(slave, response)
                                    0x04 -> readInputRegisters(slave, response)
                                    0x05 -> writeSingleCoil(slave, response)
                                    0x06 -> writeSingleRegister(slave, response)
                                    0x0f -> writeMultipleCoils(slave, response)
                                    0x10 -> writeMultipleRegisters(slave, response)
                                    else -> {
                                        response.put((0x80 + functionCode).toByte()).put(0x01.toByte())
                                        inputBuffer.position(inputBuffer.position() + remaining - 2)
                                    }
                                }
                            }
                        } else {
                            response.put((0x80 + functionCode).toByte()).put(0x0B.toByte())
                            inputBuffer.position(inputBuffer.position() + remaining - 2)
                        }
                        val position = response.position()
                        response.putShort(4, (position - 6).toShort()).flip()
                        output.write(response.array(), 0, position)
                        output.flush()
                    } else {
                        return
                    }
                } else {
                    return
                }
            }
        }

        private fun readCoils(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short

            try {
                // TODO: Support for multiple coils...
                if (quantity == 1.toShort()) {
                    val value = slave.getCoil(address.toInt())
                    response.put(0x01.toByte()).put(1.toByte()).put((if (value) 1 else 0).toByte())
                    return
                }
            } catch (e: IllegalAddressException) {
            }
            response.put((0x81.toByte())).put(0x02.toByte())
        }

        private fun readDiscreteInputs(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short

            try {
                // TODO: Support for multiple inputs...
                if (quantity == 1.toShort()) {
                    val value = slave.getInput(address.toInt())
                    response.put(0x02.toByte()).put(1.toByte()).put((if (value) 1 else 0).toByte())
                    return
                }
            } catch (e: IllegalAddressException) {
            }
            response.put((0x82.toByte())).put(0x02.toByte())
        }

        private fun readInputRegisters(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short

            try {
                val data = Array<Short>(quantity.toInt(), { 0 })
                for (i in 0..quantity - 1) {
                    data[i] = slave.getInputRegister(address.toInt() + i)
                }
                response.put(0x04.toByte()).put((quantity * 2).toByte())
                data.forEach { response.putShort(it) }
                return
            } catch (e: IllegalAddressException) {
            }
            response.put((0x84.toByte())).put(0x02.toByte())
        }

        private fun readHoldingRegisters(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short

            try {
                val data = Array<Short>(quantity.toInt(), { 0 })
                for (i in 0..quantity - 1) {
                    data[i] = slave.getHoldingRegister(address.toInt() + i)
                }
                response.put(0x03.toByte()).put((quantity * 2).toByte())
                data.forEach { response.putShort(it) }
                return
            } catch (e: IllegalAddressException) {
            }
            response.put((0x83.toByte())).put(0x02.toByte())
        }

        private fun writeSingleCoil(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val value = inputBuffer.short

            try {
                slave.setCoil(address.toInt(), value == 0xFF00.toShort())
                response.put(0x05.toByte()).putShort(address).putShort(value)
                return
            } catch (e: IllegalAddressException) {
            }
            response.put((0x85.toByte())).put(0x02.toByte())
        }

        private fun writeSingleRegister(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val value = inputBuffer.short

            try {
                slave.setHoldingRegister(address.toInt(), value)
                response.put(0x06.toByte()).putShort(address).putShort(value)
                return
            } catch (e: IllegalAddressException) {
            }
            response.put((0x86.toByte())).put(0x02.toByte())
        }

        private fun writeMultipleCoils(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short
            val byteCount = inputBuffer.get()

            // Check quantity
            var bCountChecked = (quantity/8).toByte()
            if (quantity % 8 != 0)
                bCountChecked++

            if ((quantity < 1 || quantity > 0x07b0) && (byteCount != bCountChecked)) {
                response.put((0x8f.toByte())).put(0x03.toByte())
                return
            }

            // check addresses
            if (!(address >= 0x0000 && (address + quantity) <= 0xffff)) {
                response.put((0x8f.toByte())).put(0x02.toByte())
                return
            }

            // check remaining amount of data and writing coils
            if (inputBuffer.remaining().toByte() >= byteCount) {
                // Wrinting registers...
                try {
                    var addr  = address
                    for (i in 1..quantity) {
                        slave.setHoldingRegister(addr.toInt(), inputBuffer.short)
                        addr++
                    }
                    response.put(0x0f.toByte()).putShort(address).putShort(quantity)
                    return

                } catch (e: IllegalAddressException) {
                }
            }
            response.put((0x8f.toByte())).put(0x04.toByte())
        }

        private fun writeMultipleRegisters(slave: IModbusSlave, response: ByteBuffer) {
            val address = inputBuffer.short
            val quantity = inputBuffer.short
            val byteCount = inputBuffer.get()

            // Check quantity
            if ((quantity < 1 || quantity > 0x007b) && (byteCount != (2 * quantity).toByte())) {
                response.put((0x90.toByte())).put(0x03.toByte())
                return
            }

            // check addresses
            if (!(address >= 0x0000 && (address + quantity) <= 0xffff)) {
                response.put((0x90.toByte())).put(0x02.toByte())
                return
            }

            // check remaining amount of data and writing coils
            if (inputBuffer.remaining().toByte() >= byteCount) {
                // Wrinting registers...
                try {
                    var addr  = address
                    for (i in 1..quantity) {
                        val value = inputBuffer.short
                        slave.setHoldingRegister(addr.toInt(), value)
                        addr++
                    }
                    response.put(0x10.toByte()).putShort(address).putShort(quantity)
                    return
                } catch (e: IllegalAddressException) {
                }
            }
            response.put((0x90.toByte())).put(0x04.toByte())
        }

        fun destroy() = socket.close()
    }

    fun start() = Thread(Runnable {
        while (!server.isClosed) {
            val socket = server.accept()
            if (socket != null) {
                connections.add(ConnectionHandler(socket))
            }
        }
    }).start()

    fun add(slave: IModbusSlave): Boolean {
        val id = slave.slaveId
        if (!slaves.containsKey(id)) {
            slaves.put(slave.slaveId, slave)
            return true
        } else {
            return false
        }
    }

    fun remove(slave: IModbusSlave) = slaves.remove(slave.slaveId)

    fun destroy() {
        server.close()
        connections.forEach { it.destroy() }
    }
}
