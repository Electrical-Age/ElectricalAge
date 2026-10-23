package mods.eln.client.gui

import mods.eln.gui.ElnNodeMenu
import mods.eln.i18n.I18N.tr
import mods.eln.mechanical.TachometerElement
import mods.eln.mechanical.TachometerRender
import mods.eln.misc.Utils
import mods.eln.sixnode.AmplifierElement
import mods.eln.sixnode.AmplifierRender
import mods.eln.sixnode.EmergencyLampElement
import mods.eln.sixnode.EmergencyLampRender
import mods.eln.sixnode.FilterElement
import mods.eln.sixnode.FilterRender
import mods.eln.sixnode.PIDRegulatorElement
import mods.eln.sixnode.PIDRegulatorRender
import mods.eln.sixnode.logicgate.PalElement
import mods.eln.sixnode.logicgate.PalRender
import mods.eln.sixnode.SummingUnitElement
import mods.eln.sixnode.SummingUnitRender
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/*
 * Screens of the Kotlin devices (P4b; their renders' fields are Kotlin-internal, so the screens are Kotlin too).
 * Layouts in assets/eln/gui; registered in client/ClientScreens.
 */

/** A packet to a six-node element: the header, then [body]. */
private fun send(render: mods.eln.node.six.SixNodeElementRender, body: (DataOutputStream) -> Unit) {
    val bos = ByteArrayOutputStream()
    val stream = DataOutputStream(bos)
    render.preparePacketForServer(stream)
    body(stream)
    render.sendPacketToServer(bos)
}

private fun parse(s: String): Float? = EaField.parse(s).takeUnless { it.isNaN() }

/** PID regulator (1.12 PIDRegulatorGui): Kp, Ki, Kd beside the 1.12 block diagram. */
class PIDRegulatorScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as PIDRegulatorRender

    override fun init() {
        super.init()
        canvas("diagram") { g, q -> g.blit(RenderPipelines.GUI_TEXTURED, PID, q.x(), q.y(), 91f, 17f, q.w(), q.h(), 256, 256) }
        fun k(id: String, name: String, max: Double, event: Int, value: () -> Float) =
            slider(id, 0.0, max, max / 50, value().toDouble(), { v -> Component.literal("$name = ${num(v)}") }) { v ->
                send(render) { it.writeByte(event); it.writeFloat(v.toFloat()) }
            }
        k("kp", "Kp", 50.0, PIDRegulatorElement.KpParameterChangedEvent) { render.Kp }
        k("ki", "Ki", 5.0, PIDRegulatorElement.KiParameterChangedEvent) { render.Ki }
        k("kd", "Kd", 5.0, PIDRegulatorElement.KdParameterChangerEvent) { render.Kd }
    }

    companion object {
        val PID: Identifier = Identifier.fromNamespaceAndPath("eln", "sprites/gui/pid.png")
    }
}

/** Amplifier (1.12 AmplifierGui): the gain. */
class AmplifierScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as AmplifierRender

    override fun init() {
        super.init()
        text("gainl") { tr("Gain") }
        field("gain", { num(render.gain.toDouble()) }, { v ->
            parse(v)?.let { g -> send(render) { it.writeByte(AmplifierElement.GainChangedEvent); it.writeFloat(g) } }
        }, tr("Gain"))
    }
}

/** Summing unit (1.12 SummingUnitGui): a gain per input, in the input colours. */
class SummingUnitScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as SummingUnitRender

    override fun init() {
        super.init()
        val colors = arrayOf("§4", "§2", "§1")
        for (i in 0..2) {
            text("g${i}l") { tr("Gain for input %1$", colors[i] + (i + 1)) }
            field("g$i", { num(render.gains[i].toDouble()) }, { v ->
                parse(v)?.let { g -> send(render) { it.writeByte(SummingUnitElement.GainChangedEvents[i]); it.writeFloat(g) } }
            })
        }
    }
}

/** Signal filter (1.12 FilterGui): the cut-off frequency on 1-2-5 steps from 1 mHz to 5 Hz. */
class FilterScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as FilterRender
    lateinit var cutoff: EaSlider

    override fun init() {
        super.init()
        cutoff = slider("cutoff", 0.0, (STEPS.size - 1).toDouble(), 1.0, index(render.cutOffFrequency.value).toDouble(),
            { v -> Component.literal(tr("Cut-off frequency %1$ Hz", String.format("%1.3f", STEPS[v.toInt()]))) }) { v ->
            render.clientSetFloat(FilterElement.Event.CUTOFF_FREQUENCY_CHANGED.value.toInt(), STEPS[v.toInt()])
        }
    }

    override fun update() {
        if (render.cutOffFrequency.pending) cutoff.setFromDevice(index(render.cutOffFrequency.value).toDouble())
    }

    companion object {
        /** 1.12 GuiVerticalCustomValuesBar.logarithmicScale(-3, 12). */
        val STEPS = FloatArray(12) { (when (it % 3) { 0 -> 1; 1 -> 2; else -> 5 }) * Math.pow(10.0, -3.0 + it / 3).toFloat() }

        fun index(f: Float): Int = STEPS.indices.minByOrNull { Math.abs(Math.log(STEPS[it] / Math.max(f, 1e-6f).toDouble())) } ?: 0
    }
}

/** Emergency lamp (1.12 EmergencyLampGui): supply, channel and the battery charge. */
class EmergencyLampScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as EmergencyLampRender
    lateinit var channel: EaField

    override fun init() {
        super.init()
        button("supply", { if (render.poweredByCable) tr("Powered by cable") else tr("Powered by Lamp Supply") }, {
            render.clientSend(EmergencyLampElement.Event.TOGGLE_POWERED_BY_CABLE.value.toInt())
        })
        channel = field("channel", { render.channel }, { v -> render.clientSetString(EmergencyLampElement.Event.SET_CHANNEL.value, v) },
            tr("Specify the supply channel"))
        text("status", {
            when {
                render.poweredByCable -> ""
                render.isConnectedToLampSupply -> tr("connected to %1$", render.channel)
                else -> tr("%1$ is not in range!", render.channel)
            }
        }, { if (render.isConnectedToLampSupply) 0xFF00AA00.toInt() else 0xFFAA0000.toInt() })
        gauge("charge", { render.charge.toDouble() }, { Component.literal(Utils.plotPercent(tr("Charge:"), render.charge.toDouble()).trim()) },
            2.0, 2.0)
        update()
    }

    override fun update() {
        channel.active = !render.poweredByCable
        channel.setEditable(!render.poweredByCable)
    }
}

/**
 * PAL (1.12 PalGui): the output for each of the 8 input combinations. 1.12's table index is A*4 + C*2 + (B xor C)
 * (inputs: A opposite the output, red; B left, green; C right, blue): each column is labelled with its A B C.
 */
class PalScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as PalRender

    override fun init() {
        super.init()
        canvas("diagram") { g, q -> g.blit(RenderPipelines.GUI_TEXTURED, PAL, q.x(), q.y(), 16f, 8f, q.w(), q.h(), 256, 256) }
        text("legend0") { "§4A: " + tr("back (red)") }
        text("legend1") { "§2B: " + tr("left (green)") }
        text("legend2") { "§1C: " + tr("right (blue)") }
        text("header") { tr("Inputs A B C, then the output for each:") }
        for (i in 0..7) {
            val a = (i shr 2) and 1
            val c = (i shr 1) and 1
            val b = c xor (i and 1)
            text("in$i") { "§4$a§2$b§1$c" }
            button("t$i", { if (render.truthTable[i]) "1" else "0" }, {
                send(render) { it.writeByte(PalElement.TruthTablePositionClickedEvent); it.writeInt(i) }
            })
        }
    }

    companion object {
        val PAL: Identifier = Identifier.fromNamespaceAndPath("eln", "sprites/gui/pal.png")
    }
}

/** Tachometer (1.12 TachometerGui): the rad/s range mapped to 0..100% output. */
class TachometerScreen(menu: ElnNodeMenu, inventory: Inventory, title: Component, r: Any) : ElnScreen(menu, inventory, title) {
    val render = r as TachometerRender
    lateinit var low: EaField
    lateinit var high: EaField

    override fun init() {
        super.init()
        text("hl") { "100%" }
        text("ll") { "0%" }
        high = field("high", { num(render.maxRads.toDouble()) }, { validate() }, *tr("Rads/s corresponding\nto 100% output").split("\n").toTypedArray())
        low = field("low", { num(render.minRads.toDouble()) }, { validate() }, *tr("Rads/s corresponding\nto 0% output").split("\n").toTypedArray())
        button("validate", { tr("Validate") }, { validate() })
    }

    fun validate() {
        val lo = parse(low.value) ?: return
        val hi = parse(high.value) ?: return
        val bos = ByteArrayOutputStream()
        val stream = DataOutputStream(bos)
        render.preparePacketForServer(stream)
        stream.writeByte(TachometerElement.SetRangeEventId)
        stream.writeFloat(lo)
        stream.writeFloat(hi)
        render.sendPacketToServer(bos)
    }
}
