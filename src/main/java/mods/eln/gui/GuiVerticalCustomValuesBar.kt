package mods.eln.gui

class GuiVerticalCustomValuesBar(x: Int, y:Int, width: Int, height: Int, helper: GuiHelper,
                                 val positions: Array<Float>) :
    GuiVerticalTrackBar(x, y, width, height, helper) {

    companion object {
        fun logarithmicScale(startDecade: Int, steps: Int) = Array(steps) {
            when(it.rem(3)) { 0 -> 1; 1 -> 2; else -> 5 } * Math.pow(10.0, startDecade + (it / 3).toDouble()).toFloat()
        }
    }

    init {
        setStepIdMax(positions.size - 1)
        setRange(0f, (positions.size - 1).toFloat())
    }

    override fun getValue() = positions.getOrElse(super.getValue().toInt(), { 0f })

    override fun setValue(value: Float) {
        val pos = positions.indexOfFirst { it >= value }
        when(pos) {
            -1 -> super.setValue(0f)
            else -> super.setValue(pos.toFloat())
        }
    }
}
